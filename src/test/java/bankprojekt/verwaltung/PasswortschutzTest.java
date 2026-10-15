package bankprojekt.verwaltung;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testklasse für das Hashen und Prüfen von Passwörtern ({@link Passwortschutz}).
 */
class PasswortschutzTest {

    /** einmal berechneter Hash mit voller Iterationszahl (das Hashen kostet Zeit) */
    private static final String HASH = Passwortschutz.hashen("geheim".toCharArray());

    @Test
    void hashen_enthaeltKeinenKlartext_undHatDasErwarteteFormat() {
        assertFalse(HASH.contains("geheim"), "Der Hash darf das Passwort nicht enthalten");
        String[] teile = HASH.split("\\$");
        assertEquals(4, teile.length, "Format: Kennung$Iterationen$Salz$Hash");
        assertEquals("pbkdf2-sha256", teile[0]);
        assertEquals(String.valueOf(Passwortschutz.ITERATIONEN), teile[1]);
    }

    @Test
    void pruefen_akzeptiertDasRichtigePasswort() {
        assertTrue(Passwortschutz.pruefen("geheim".toCharArray(), HASH));
    }

    @Test
    void pruefen_lehntEinFalschesPasswortAb() {
        assertFalse(Passwortschutz.pruefen("Geheim".toCharArray(), HASH));
    }

    @Test
    void pruefen_lehntEinLeeresPasswortAb_ohneAusnahme() {
        assertFalse(Passwortschutz.pruefen(new char[0], HASH));
    }

    @Test
    void hashen_liefertFuerGleichesPasswortVerschiedeneHashes_wegenDesSalzes() {
        String a = Passwortschutz.hashen("1234".toCharArray(), 1_000);
        String b = Passwortschutz.hashen("1234".toCharArray(), 1_000);
        assertNotEquals(a, b, "Jeder Hash braucht ein eigenes zufälliges Salz");
        assertTrue(Passwortschutz.pruefen("1234".toCharArray(), a));
        assertTrue(Passwortschutz.pruefen("1234".toCharArray(), b));
    }

    @Test
    void pruefen_verwendetDieGespeicherteIterationszahl() {
        String schnell = Passwortschutz.hashen("abcd".toCharArray(), 1_000);
        assertTrue(schnell.startsWith("pbkdf2-sha256$1000$"));
        assertTrue(Passwortschutz.pruefen("abcd".toCharArray(), schnell));
    }

    @Test
    void pruefen_mitNull_liefertFalse() {
        assertFalse(Passwortschutz.pruefen(null, HASH));
        assertFalse(Passwortschutz.pruefen("geheim".toCharArray(), null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "geheim", "md5$1$abc$def", "pbkdf2-sha256$x$AAAA$AAAA",
            "pbkdf2-sha256$0$AAAA$AAAA", "pbkdf2-sha256$1000$%%%%$AAAA", "pbkdf2-sha256$1000$$AAAA",
            "pbkdf2-sha256$1000$AAAA"})
    void pruefen_mitBeschaedigtemHash_liefertFalse(String kaputt) {
        assertFalse(Passwortschutz.pruefen("geheim".toCharArray(), kaputt));
    }

    @Test
    void hashen_mitNull_wirftNullPointerException() {
        assertThrows(NullPointerException.class, () -> Passwortschutz.hashen(null));
    }
}
