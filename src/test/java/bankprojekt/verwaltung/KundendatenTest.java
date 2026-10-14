package bankprojekt.verwaltung;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testklasse für die Prüfung der Kundenstammdaten ({@link Kundendaten}).
 */
class KundendatenTest {

    private static final LocalDate GEBURTSDATUM = LocalDate.of(1990, 5, 1);

    /**
     * Erzeugt gültige Kundendaten, bei denen nur die E-Mail-Adresse variiert.
     * @param email die E-Mail-Adresse
     * @return die Kundendaten
     */
    private static Kundendaten mitEmail(String email) {
        return new Kundendaten("Anna", "Schmidt", GEBURTSDATUM, "Berlin", "", email);
    }

    @Test
    void konstruktor_entferntLeerzeichenAmRand() {
        Kundendaten daten = new Kundendaten("  Anna ", " Schmidt", GEBURTSDATUM,
                " Hauptstr. 1, Berlin ", " +49 30 123456 ", " anna@example.org ");
        assertEquals("Anna", daten.vorname());
        assertEquals("Schmidt", daten.nachname());
        assertEquals("Hauptstr. 1, Berlin", daten.adresse());
        assertEquals("+49 30 123456", daten.telefon());
        assertEquals("anna@example.org", daten.email());
    }

    @Test
    void telefon_istFreiwillig() {
        assertEquals("", new Kundendaten("Anna", "Schmidt", GEBURTSDATUM, "Berlin", null, "a@b.de").telefon());
        assertEquals("", new Kundendaten("Anna", "Schmidt", GEBURTSDATUM, "Berlin", "   ", "a@b.de").telefon());
    }

    @Test
    void pflichtfelder_fehlen_wirftMitFeldnamenInDerMeldung() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> new Kundendaten(" ", "Schmidt", GEBURTSDATUM, "Berlin", "", "a@b.de"));
        assertTrue(e.getMessage().contains("Vorname"));
        e = assertThrows(IllegalArgumentException.class,
                () -> new Kundendaten("Anna", null, GEBURTSDATUM, "Berlin", "", "a@b.de"));
        assertTrue(e.getMessage().contains("Nachname"));
        e = assertThrows(IllegalArgumentException.class,
                () -> new Kundendaten("Anna", "Schmidt", null, "Berlin", "", "a@b.de"));
        assertTrue(e.getMessage().contains("Geburtsdatum"));
        e = assertThrows(IllegalArgumentException.class,
                () -> new Kundendaten("Anna", "Schmidt", GEBURTSDATUM, "", "", "a@b.de"));
        assertTrue(e.getMessage().contains("Adresse"));
        e = assertThrows(IllegalArgumentException.class, () -> mitEmail(""));
        assertTrue(e.getMessage().contains("E-Mail"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"anna", "anna@", "@example.org", "anna@example", "anna@example.o",
            "an na@example.org", "anna@@example.org"})
    void email_ungueltig_wirftIllegalArgumentException(String email) {
        assertThrows(IllegalArgumentException.class, () -> mitEmail(email));
    }

    @ParameterizedTest
    @ValueSource(strings = {"anna@example.org", "anna.schmidt+bank@mail.example.de", "a@b.de"})
    void email_gueltig_wirdUebernommen(String email) {
        assertEquals(email, mitEmail(email).email());
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "123", "+49 abc 123", "0049-30-12345678901234567890"})
    void telefon_ungueltig_wirftIllegalArgumentException(String telefon) {
        assertThrows(IllegalArgumentException.class,
                () -> new Kundendaten("Anna", "Schmidt", GEBURTSDATUM, "Berlin", telefon, "a@b.de"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"030 123456", "+49 (30) 123-456", "0171/1234567"})
    void telefon_gueltig_wirdUebernommen(String telefon) {
        assertEquals(telefon,
                new Kundendaten("Anna", "Schmidt", GEBURTSDATUM, "Berlin", telefon, "a@b.de").telefon());
    }

    @Test
    void geburtsdatum_inDerZukunft_wirftIllegalArgumentException() {
        LocalDate morgen = LocalDate.now().plusDays(1);
        assertThrows(IllegalArgumentException.class,
                () -> new Kundendaten("Anna", "Schmidt", morgen, "Berlin", "", "a@b.de"));
    }

    @Test
    void geburtsdatum_heute_istErlaubt() {
        LocalDate heute = LocalDate.now();
        assertEquals(heute, new Kundendaten("Anna", "Schmidt", heute, "Berlin", "", "a@b.de").geburtsdatum());
    }

    @Test
    void geburtsdatum_vor1900_wirftIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> new Kundendaten("Anna", "Schmidt",
                LocalDate.of(1899, 12, 31), "Berlin", "", "a@b.de"));
        assertEquals(LocalDate.of(1900, 1, 1), new Kundendaten("Anna", "Schmidt",
                LocalDate.of(1900, 1, 1), "Berlin", "", "a@b.de").geburtsdatum());
    }
}
