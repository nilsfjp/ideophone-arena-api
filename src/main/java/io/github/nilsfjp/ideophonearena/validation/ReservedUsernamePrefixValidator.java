package io.github.nilsfjp.ideophonearena.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.List;
import java.util.Locale;

// Reserved for the thesis ingestion cohort (thesis_p*) and browser-loop automation
// accounts (browser_loop_*). A live player registering under either prefix would pollute
// the research aggregates that fence on these prefixes (live divergence, position bias),
// so registration rejects them. The check is trim + Locale.ROOT lowercase to match the
// case-insensitive collation the read-side LIKE fences run under -- 'THESIS_P37' must be
// rejected too. Locale.ROOT (not the default locale) keeps the fold ASCII, so a Turkish-
// locale JVM cannot dot-fold the 'I' and slip a reserved prefix past this guard.
public class ReservedUsernamePrefixValidator implements ConstraintValidator<NotReservedUsername, String> {

    private static final List<String> RESERVED_PREFIXES = List.of("thesis_p", "browser_loop_");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;  // @NotBlank owns null/blank
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return RESERVED_PREFIXES.stream().noneMatch(normalized::startsWith);
    }
}
