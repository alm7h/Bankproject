package bankprojekt.oberflaeche;

import bankprojekt.verwaltung.Bankverwaltung;
import bankprojekt.verwaltung.Kundenakte;
import bankprojekt.verwaltung.Kundendaten;
import bankprojekt.verwaltung.Testdaten;
import bankprojekt.verwaltung.VerstellbareUhr;
import javafx.scene.Parent;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testklasse für das Kundenformular ({@link KundeFormularController}).
 */
@ExtendWith(JavaFxErweiterung.class)
class KundeFormularControllerTest {

    private Parent formular;
    private KundeFormularController controller;

    @BeforeEach
    void setUp() {
        Dialoge.Geladen<KundeFormularController> geladen = FxHilfe.laden("kundeFormular.fxml");
        formular = geladen.wurzel();
        controller = geladen.controller();
    }

    private void ausfuellen(String geburtsdatum, String email) {
        FxHilfe.tippe(formular, "vorname", " Carla ");
        FxHilfe.tippe(formular, "nachname", "Klein");
        FxHilfe.tippe(formular, "geburtsdatum", geburtsdatum);
        FxHilfe.tippe(formular, "adresse", "Hauptstr. 1");
        FxHilfe.tippe(formular, "telefon", "");
        FxHilfe.tippe(formular, "email", email);
    }

    @Test
    void neuesFormular_hatNochKeineKundennummer() {
        assertEquals("wird automatisch vergeben", FxHilfe.text(formular, "kundennummer"));
        assertSame(FxHilfe.suche(formular, "fehler"), controller.getFehlerfeld());
    }

    @Test
    void auslesen_liefertGepruefteKundendaten() {
        ausfuellen("01.05.1990", "carla@example.org");
        Kundendaten daten = controller.auslesen();
        assertEquals("Carla", daten.vorname());
        assertEquals(LocalDate.of(1990, 5, 1), daten.geburtsdatum());
        assertEquals("", daten.telefon());
    }

    @Test
    void auslesen_mitUngueltigemDatumOderEmail_wirft() {
        ausfuellen("31.02.1990", "carla@example.org");
        assertTrue(assertThrows(IllegalArgumentException.class, controller::auslesen).getMessage().contains("31.02.1990"));
        ausfuellen("", "carla@example.org");
        assertTrue(assertThrows(IllegalArgumentException.class, controller::auslesen).getMessage()
                .contains("Geburtsdatum"));
        ausfuellen("01.05.1990", "carla");
        assertTrue(assertThrows(IllegalArgumentException.class, controller::auslesen).getMessage().contains("E-Mail"));
    }

    @Test
    void setKunde_fuelltDasFormular() {
        Bankverwaltung verwaltung = Testdaten.verwaltung(new VerstellbareUhr());
        Kundenakte anna = verwaltung.kundeAnlegen(Testdaten.admin(verwaltung), Testdaten.kunde("Anna", "Schmidt"));
        controller.setKunde(anna);
        assertEquals("K000001", FxHilfe.text(formular, "kundennummer"));
        assertEquals("Anna", FxHilfe.<TextField>suche(formular, "vorname").getText());
        assertEquals("Schmidt", FxHilfe.<TextField>suche(formular, "nachname").getText());
        assertEquals("01.05.1990", FxHilfe.<TextField>suche(formular, "geburtsdatum").getText());
        assertEquals("Berlin", FxHilfe.<TextArea>suche(formular, "adresse").getText());
        assertEquals("anna@example.org", FxHilfe.<TextField>suche(formular, "email").getText());
        assertEquals(anna.getKundendaten(), controller.auslesen(), "unverändert ausgelesen = gleiche Daten");
    }
}
