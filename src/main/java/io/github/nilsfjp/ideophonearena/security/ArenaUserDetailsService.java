package io.github.nilsfjp.ideophonearena.security;

import io.github.nilsfjp.ideophonearena.repository.AppUserRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class ArenaUserDetailsService implements UserDetailsService {

    private final AppUserRepository appUserRepository;

    public ArenaUserDetailsService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return appUserRepository.findByUsername(username)
                .map(appUser -> User.withUsername(appUser.getUsername())
                        .password(appUser.getPasswordHash())
                        .authorities(appUser.getRole().name())
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}
