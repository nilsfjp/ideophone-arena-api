# Backend-konventioner

Riktlinjer för Java Spring Boot-projekt. Syftet är att säkerställa god separation of concerns, läsbar kod och ett konsekvent API.

---

## Controller

- Returnerar alltid `ResponseEntity` med korrekt HTTP-statuskod

<details>
<summary>Kodexempel</summary>

```java
@GetMapping("/{id}")
public ResponseEntity<ProductResponseDto> getById(@PathVariable Long id) {
    return ResponseEntity.ok(productService.getById(id));
}

@PostMapping
public ResponseEntity<ProductResponseDto> create(@RequestBody ProductRequestDto dto) {
    return ResponseEntity.status(201).body(productService.create(dto));
}

@DeleteMapping("/{id}")
public ResponseEntity<Void> delete(@PathVariable Long id) {
    productService.delete(id);
    return ResponseEntity.noContent().build();
}
```

| Statuskod | Används vid |
|---|---|
| `200 OK` | Lyckad hämtning eller uppdatering |
| `201 Created` | Ny resurs skapad |
| `204 No Content` | Resurs borttagen |
| `404 Not Found` | Resurs hittades inte |

</details>

- Tar emot request-DTOs och returnerar response-DTOs – aldrig entiteter direkt

<details>
<summary>Kodexempel</summary>

```java
// Rätt – controller känner bara till DTOs
@PostMapping
public ResponseEntity<ProductResponseDto> create(@RequestBody ProductRequestDto dto) {
    return ResponseEntity.status(201).body(productService.create(dto));
}

// Fel – entitet exponeras direkt i API-svaret
@PostMapping
public ResponseEntity<Product> create(@RequestBody Product product) {
    return ResponseEntity.status(201).body(productService.create(product));
}
```

</details>

- Delegerar allt arbete till service-lagret – ingen logik i controllern

<details>
<summary>Kodexempel</summary>

```java
// Rätt – controllern delegerar
@PutMapping("/{id}")
public ResponseEntity<ProductResponseDto> update(@PathVariable Long id, @RequestBody ProductRequestDto dto) {
    return ResponseEntity.ok(productService.update(id, dto));
}

// Fel – affärslogik läcker in i controllern
@PutMapping("/{id}")
public ResponseEntity<ProductResponseDto> update(@PathVariable Long id, @RequestBody ProductRequestDto dto) {
    Product product = productRepository.findById(id).orElseThrow();
    product.setName(dto.getName());
    productRepository.save(product);
    return ResponseEntity.ok(productMapper.toDto(product));
}
```

</details>

- Validerar att inkommande request-data har rätt format och att obligatoriska fält finns – inte affärsregler

<details>
<summary>Kodexempel</summary>

```java
// Rätt – kontrollera att nödvändig data faktiskt finns innan den skickas vidare
@PostMapping
public ResponseEntity<ProductResponseDto> create(@RequestBody ProductRequestDto dto) {
    if (dto.getName() == null || dto.getName().isBlank()) {
        return ResponseEntity.badRequest().build();
    }
    return ResponseEntity.status(201).body(productService.create(dto));
}
```

> Controllern validerar format och närvaro. Om ett namn måste vara unikt i systemet är det service-lagrets ansvar.

</details>

- Anropar aldrig repository direkt, känner inte till entiteter och innehåller ingen affärslogik

---

## Service

- Innehåller all affärslogik

<details>
<summary>Kodexempel</summary>

```java
// Rätt – affärslogiken bor i service
public OrderResponseDto placeOrder(OrderRequestDto dto) {
    Product product = productRepository.findById(dto.getProductId())
            .orElseThrow(() -> new ProductNotFoundException(dto.getProductId()));

    if (product.getStock() < dto.getQuantity()) {
        throw new InsufficientStockException("Inte tillräckligt med lager");
    }

    product.setStock(product.getStock() - dto.getQuantity());
    productRepository.save(product);

    Order order = new Order(product, dto.getQuantity());
    return orderMapper.toDto(orderRepository.save(order));
}
```

</details>

- Använder transaktionshantering där det är relevant

<details>
<summary>Kodexempel</summary>

```java
// @Transactional säkerställer att hela operationen lyckas eller rullas tillbaka
@Transactional
public OrderResponseDto placeOrder(OrderRequestDto dto) {
    // flera databasoperationer som måste lyckas tillsammans
}

// Läsoperationer kan markeras som readOnly för bättre prestanda
@Transactional(readOnly = true)
public List<ProductResponseDto> getAll() {
    return productRepository.findAll().stream()
            .map(productMapper::toDto)
            .toList();
}
```

</details>

- Delegerar mappning till dedikerade mappers

<details>
<summary>Kodexempel</summary>

```java
// Rätt – mappning delegeras
public ProductResponseDto getById(Long id) {
    Product product = productRepository.findById(id)
            .orElseThrow(() -> new ProductNotFoundException(id));
    return productMapper.toDto(product);
}

// Fel – mappning sker inline i service
public ProductResponseDto getById(Long id) {
    Product product = productRepository.findById(id).orElseThrow();
    return new ProductResponseDto(product.getId(), product.getName(), product.getPrice());
}
```

</details>

- Kastar egna namngivna exceptions vid felfall

<details>
<summary>Kodexempel</summary>

```java
// Rätt – egen exception med tydligt namn
public ProductResponseDto getById(Long id) {
    return productRepository.findById(id)
            .map(productMapper::toDto)
            .orElseThrow(() -> new ProductNotFoundException(id));
}

// Fel – generiskt undantag utan kontext
public ProductResponseDto getById(Long id) {
    return productRepository.findById(id)
            .map(productMapper::toDto)
            .orElseThrow(() -> new RuntimeException("Hittades inte"));
}
```

</details>

- Validerar affärsregler – inte format

<details>
<summary>Kodexempel</summary>

```java
// Rätt – affärsrelaterad validering sker i service
public ProductResponseDto create(ProductRequestDto dto) {
    if (productRepository.existsByName(dto.getName())) {
        throw new DuplicateProductException("En produkt med det namnet finns redan");
    }
    Product product = productMapper.toEntity(dto);
    return productMapper.toDto(productRepository.save(product));
}
```

> Service validerar affärsregler. Om ett fält saknas eller har fel format ska det ha fastnat i controllern redan.

</details>

- Returnerar alltid DTOs – entiteter lämnar aldrig service-lagret
- Har ingen kännedom om HTTP, statuskoder eller `ResponseEntity`

---

## Repository

- Använder derived query methods där det är möjligt

<details>
<summary>Kodexempel</summary>

```java
// Spring Data härleder SQL automatiskt från metodnamnet
List<Todo> findByCompletedTrue();
List<Product> findByNameContainingIgnoreCase(String name);
List<Order> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
```

</details>

- Använder JPQL med bundna parametrar för custom queries – aldrig strängkonkatenering

<details>
<summary>Kodexempel</summary>

```java
// Rätt – JPQL med bundna parametrar
@Query("SELECT p FROM Product p WHERE p.price < :maxPrice AND p.category = :category")
List<Product> findAffordableByCategory(@Param("maxPrice") double maxPrice, @Param("category") String category);

// Fel – strängkonkatenering öppnar för SQL injection
@Query("SELECT p FROM Product p WHERE p.category = '" + category + "'")
List<Product> findByCategory(String category);
```

</details>

- Innehåller ingen logik, anropas aldrig direkt från controllern

---

## Entitet / Model

- Representerar databasens struktur och ingenting annat
- Databasrelaterade begränsningar definieras här

<details>
<summary>Kodexempel</summary>

```java
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)           // fältet får inte vara null i databasen
    private String name;

    @Column(nullable = false, unique = true)  // måste vara unikt
    private String sku;

    @Column(length = 500)               // maxlängd
    private String description;

    public Product() {}

    public Product(String name, String sku) {
        this.name = name;
        this.sku = sku;
    }

    // getters och setters
}
```

> Det här är databasregler, inte affärsregler. "Namn får inte vara tomt" är en affärsregel och hör hemma i service-lagret.

</details>

- Innehåller inga DTOs, ingen affärslogik och exponeras aldrig utanför service-lagret

---

## DTO

- Innehåller endast den information som är relevant för det specifika flödet – varken mer eller mindre

<details>
<summary>Kodexempel</summary>

```java
// Request-DTO vid POST – inget id eftersom det genereras av databasen
public class ProductRequestDto {
    private String name;
    private double price;
}

// Response-DTO – innehåller id som klienten behöver
public class ProductResponseDto {
    private Long id;
    private String name;
    private double price;
}

// Fel – response-DTO exponerar intern information som klienten inte behöver
public class ProductResponseDto {
    private Long id;
    private String name;
    private double price;
    private String internalCostPrice;   // intern data
    private String createdByAdminUser;  // intern data
}
```

</details>

- Har separata request- och response-DTOs där det är motiverat

<details>
<summary>Kodexempel</summary>

```java
// Vid registrering skickar klienten in lösenord
public class RegisterRequestDto {
    private String username;
    private String password;  // tas emot, hashas och sparas – skickas aldrig tillbaka
    private String email;
}

// Svaret tillbaka innehåller aldrig lösenord
public class UserResponseDto {
    private Long id;
    private String username;
    private String email;
}
```

</details>

- Innehåller inga JPA-annotations, ingen databaslogik och är aldrig samma klass som entiteten

---

## Mapper

- Har ett enda ansvar: mappning mellan entitet och DTO

<details>
<summary>Kodexempel</summary>

```java
@Component
public class ProductMapper {

    public ProductResponseDto toDto(Product product) {
        return new ProductResponseDto(
                product.getId(),
                product.getName(),
                product.getPrice()
        );
    }

    public Product toEntity(ProductRequestDto dto) {
        return new Product(
                dto.getName(),
                dto.getPrice()
        );
    }
}
```

</details>

- Anropas från service-lagret
- Innehåller ingen affärslogik, gör inga databasanrop och anropas aldrig från controller eller repository

---

## Exception-hantering

- Använder egna namngivna exception-klasser för olika typer av felfall

<details>
<summary>Kodexempel</summary>

```java
public class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException(Long id) {
        super("Produkt med id " + id + " hittades inte");
    }
}

public class DuplicateProductException extends RuntimeException {
    public DuplicateProductException(String message) {
        super(message);
    }
}
```

</details>

- Har en global exception handler via `@ControllerAdvice` som fångar alla exceptions på ett ställe

<details>
<summary>Kodexempel</summary>

```java
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<String> handleNotFound(ProductNotFoundException ex) {
        return ResponseEntity.status(404).body(ex.getMessage());
    }

    @ExceptionHandler(DuplicateProductException.class)
    public ResponseEntity<String> handleDuplicate(DuplicateProductException ex) {
        return ResponseEntity.status(409).body(ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGeneric(Exception ex) {
        return ResponseEntity.status(500).body("Något gick fel");
    }
}
```

</details>

- Returnerar meningsfulla felmeddelanden med rätt statuskod – aldrig stacktrace eller intern systeminformation
- Hanterar inte exceptions med try/catch utspritt i controllers eller services

---

## Security

- Använder JWT för autentisering – token genereras vid inloggning och skickas med i efterföljande anrop

<details>
<summary>Kodexempel</summary>

```java
// Inloggningsendpoint returnerar en JWT-token
@PostMapping("/auth/login")
public ResponseEntity<AuthResponseDto> login(@RequestBody LoginRequestDto dto) {
    return ResponseEntity.ok(authService.login(dto));
}

// AuthResponseDto innehåller token som klienten sparar och skickar med i Authorization-headern
public class AuthResponseDto {
    private String token;
}
```

```
// Klienten skickar med token i varje anrop
Authorization: Bearer <token>
```

</details>

- Krypterar lösenord innan de sparas i databasen – lösenord lagras aldrig i klartext

<details>
<summary>Kodexempel</summary>

```java
// Rätt – lösenord hashas med BCrypt innan det sparas
@Service
public class AuthService {

    private final PasswordEncoder passwordEncoder;

    public UserResponseDto register(RegisterRequestDto dto) {
        String hashedPassword = passwordEncoder.encode(dto.getPassword());
        User user = new User(dto.getUsername(), hashedPassword);
        return userMapper.toDto(userRepository.save(user));
    }

    public AuthResponseDto login(LoginRequestDto dto) {
        User user = userRepository.findByUsername(dto.getUsername())
                .orElseThrow(() -> new InvalidCredentialsException("Felaktiga inloggningsuppgifter"));

        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Felaktiga inloggningsuppgifter");
        }

        String token = jwtService.generateToken(user);
        return new AuthResponseDto(token);
    }
}
```

</details>

- Konfigurerar vilka endpoints som kräver autentisering och vilka som är öppna

<details>
<summary>Kodexempel</summary>

```java
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/**").permitAll()   // öppna endpoints
                .anyRequest().authenticated()              // allt annat kräver inloggning
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            );

        return http.build();
    }
}
```

</details>

- Använder roller för åtkomstkontroll om projektet kräver det – annars räcker autentisering

<details>
<summary>Kodexempel</summary>

```java
// Roller används när olika användare ska ha olika behörigheter
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(auth -> auth
        .requestMatchers("/auth/**").permitAll()
        .requestMatchers("/admin/**").hasRole("ADMIN")   // endast admin
        .requestMatchers(HttpMethod.GET, "/products/**").hasAnyRole("USER", "ADMIN")
        .anyRequest().authenticated()
    );
    return http.build();
}

// Roller kan även sättas direkt på metodnivå
@PreAuthorize("hasRole('ADMIN')")
public void deleteUser(Long id) {
    userRepository.deleteById(id);
}
```

</details>

- Kopplar Spring Security mot databasen för att ladda användare vid autentisering

<details>
<summary>Kodexempel</summary>

```java
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Användare hittades inte: " + username));
    }
}
```

</details>

---

## Good to have

- Tester – enhetstester på service-lagret och integrationstester på controller-lagret
- Automatiserad mappning med MapStruct istället för manuell mappning
- API-dokumentation med Swagger / OpenAPI
- Loggning på lämpliga nivåer genom applikationen
- Paginering på endpoints som returnerar listor
- Validering av inkommande data med Bean Validation (`@NotNull`, `@NotBlank` m.fl.)
- Miljöspecifik konfiguration med profiler (dev, prod)
