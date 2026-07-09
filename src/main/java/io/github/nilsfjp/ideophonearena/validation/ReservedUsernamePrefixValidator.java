package io.github.nilsfjp.ideophonearena.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;

// Reserved for the thesis ingestion cohort (thesis_p*) and browser-loop automation
// accounts (browser_loop_*). A live player registering under either prefix would pollute
// the research aggregates that fence on these prefixes (live divergence, position bias),
// so registration rejects them. The check is trim + Locale.ROOT lowercase to match the
// case-insensitive collation the read-side LIKE fences run under -- 'THESIS_P37' must be
// rejected too. Locale.ROOT (not the default locale) keeps the fold ASCII, so a Turkish-
// locale JVM cannot dot-fold the 'I' and slip a reserved prefix past this guard.
//
// The browser-loop harness must register a browser_loop_* account precisely to be fenced out
// of those aggregates, so the `automation` profile lifts the rejection -- for that prefix
// alone (NIL-90). thesis_p* is rejected under every profile: it is the one prefix whose rows
// are READ BACK as data (/api/research/thesis/divergence includes the cohort rather than
// excluding it), so a stray thesis_p row is silent corruption, not just noise. The property
// defaults to false and its only source is application-automation.properties, which is
// dockerignored and so absent from the image.
public class ReservedUsernamePrefixValidator implements ConstraintValidator<NotReservedUsername, String> {

    private static final List<String> RESERVED_PREFIXES = List.of("thesis_p", "browser_loop_");
    private static final String AUTOMATION_PREFIX = "browser_loop_";

    private final boolean allowBrowserLoopRegistration;

    public ReservedUsernamePrefixValidator(
            @Value("${app.automation.allow-browser-loop-registration:false}") boolean allowBrowserLoopRegistration) {
        this.allowBrowserLoopRegistration = allowBrowserLoopRegistration;
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;  // @NotBlank owns null/blank
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (allowBrowserLoopRegistration && normalized.startsWith(AUTOMATION_PREFIX)) {
            return true;
        }
        return RESERVED_PREFIXES.stream().noneMatch(normalized::startsWith);
    }
}
