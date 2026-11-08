package dev.stash.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QueryTextTest {
    @Test
    void foldsCaseAndYo() {
        assertEquals("елитра", QueryText.normalize("  Ёлитра  ", 64));
    }

    @Test
    void escapesLikeWildcards() {
        assertEquals("%\\%\\_%", QueryText.displayPattern("%_"));
        assertEquals("%elytra%", QueryText.itemPattern("elytra"));
        assertEquals("%gold\\_ingot%", QueryText.itemPattern("gold ingot"));
    }
}
