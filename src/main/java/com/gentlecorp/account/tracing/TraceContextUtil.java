package com.gentlecorp.account.tracing;

import com.gentlecorp.account.security.CustomUserDetails;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Utility-Klasse zum Extrahieren des aktuellen Benutzernamens aus dem Security-Kontext.
 */
public class TraceContextUtil {

    private static Environment springEnv;

    /**
     * Gibt den aktuellen Benutzernamen zurück – falls verfügbar.
     *
     * @return z.B. „gentlecg99“ oder null
     */
    public static String getUsernameOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;

        Object principal = auth.getPrincipal();
        if (principal instanceof CustomUserDetails user) {
            return user.getUsername();
        }
        return auth.getName();
    }

    public static String getTraceId() {
        SpanContext ctx = Span.current().getSpanContext();
        return ctx.isValid() ? ctx.getTraceId() : null;
    }

    public static String getSpanId() {
        SpanContext ctx = Span.current().getSpanContext();
        return ctx.isValid() ? ctx.getSpanId() : null;
    }

    public static String getEnvironment() {
        return springEnv != null
            ? springEnv.getProperty("app.env", "dev")
            : "dev";
    }
}

