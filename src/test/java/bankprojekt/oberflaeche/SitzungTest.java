package bankprojekt.oberflaeche;

import bankprojekt.verwaltung.Bankverwaltung;
import bankprojekt.verwaltung.Benutzer;
import bankprojekt.verwaltung.Testdaten;
import bankprojekt.verwaltung.VerstellbareUhr;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testklasse für die Sitzung eines angemeldeten Benutzers ({@link Sitzung}).
 */
class SitzungTest {

    private final Bankverwaltung verwaltung = Testdaten.verwaltung(new VerstellbareUhr());
    private final Benutzer admin = Testdaten.admin(verwaltung);

    @Test
    void sitzung_haeltVerwaltungUndBenutzer() {
        Sitzung sitzung = new Sitzung(verwaltung, admin);
        assertSame(verwaltung, sitzung.verwaltung());
        assertSame(admin, sitzung.benutzer());
    }

    @Test
    void sitzung_ohneVerwaltungOderBenutzer_wirftNullPointerException() {
        assertThrows(NullPointerException.class, () -> new Sitzung(null, admin));
        assertThrows(NullPointerException.class, () -> new Sitzung(verwaltung, null));
    }
}
