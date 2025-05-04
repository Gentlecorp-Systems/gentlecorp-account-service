package com.gentlecorp.account.exceptions;

import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * RuntimeException, falls kein Customer gefunden wurde.
 */
@Getter
public final class NotFoundException extends RuntimeException {
    /**
     * Nicht-vorhandene ID.
     */
    private final UUID id;

    /**
     * Suchkriterien, zu denen nichts gefunden wurde.
     */
    private final Map<String, List<String>> suchkriterien;

    public NotFoundException(final UUID id) {
        super(String.format("Kein Bankkonto mit der ID %s gefunden.", id));
        this.id = id;
        suchkriterien = null;
    }

    public NotFoundException(final Map<String, List<String>> searchCriteria) {
        super("Keine Kunden gefunden mit diesen Suchkriterien gefunden.");
        id = null;
        this.suchkriterien = searchCriteria;
    }

    public NotFoundException() {
        super("Keine Kunden gefunden.");
        id = null;
        suchkriterien = null;
    }
}
