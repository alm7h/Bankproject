package bankprojekt.oberflaeche;

import bankprojekt.verwaltung.Bankverwaltung;
import bankprojekt.verwaltung.Benutzer;
import bankprojekt.verwaltung.Protokollaktion;
import bankprojekt.verwaltung.Protokolleintrag;
import bankprojekt.verwaltung.Testdaten;
import bankprojekt.verwaltung.VerstellbareUhr;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testklasse für die Protokollansicht ({@link ProtokollController}).
 */
@ExtendWith(JavaFxErweiterung.class)
class ProtokollControllerTest {

    private Bankverwaltung verwaltung;
    private Parent ansicht;
    private ProtokollController controller;
    private TableView<Protokolleintrag> tabelle;

    @BeforeEach
    void setUp() {
        verwaltung = Testdaten.verwaltung(new VerstellbareUhr());
        Benutzer admin = Testdaten.admin(verwaltung);
        verwaltung.kundeAnlegen(admin, Testdaten.kunde("Anna", "Schmidt"));
        verwaltung.kundeAnlegen(admin, Testdaten.kunde("Bert", "Müller"));
        verwaltung.kundeDeaktivieren(admin, "K000002");

        Dialoge.Geladen<ProtokollController> geladen = FxHilfe.laden("protokoll.fxml");
        ansicht = geladen.wurzel();
        controller = geladen.controller();
        controller.setSitzung(new Sitzung(verwaltung, admin));
        controller.aktualisieren();
        tabelle = FxHilfe.tabelle(ansicht);
    }

    private ComboBox<Protokollaktion> aktionFilter() {
        return FxHilfe.suche(ansicht, "aktionFilter");
    }

    @Test
    void zeigtAlleEintraege_neuesteZuerst() {
        List<Protokolleintrag> alle = verwaltung.getProtokoll();
        assertEquals(4, tabelle.getItems().size());
        assertSame(alle.get(alle.size() - 1), tabelle.getItems().get(0));
        Protokolleintrag neuester = tabelle.getItems().get(0);
        assertEquals("14.10.2026 10:00:00", FxHilfe.zelle(tabelle, "Zeitpunkt", neuester));
        assertEquals("admin", FxHilfe.zelle(tabelle, "Benutzer", neuester));
        assertEquals("Kunde deaktiviert", FxHilfe.zelle(tabelle, "Aktion", neuester));
        assertTrue(((String) FxHilfe.zelle(tabelle, "Details", neuester)).contains("K000002"));
        assertEquals("4 Einträge", FxHilfe.text(ansicht, "anzahl"));
    }

    @Test
    void filterNachAktion_mitNullFuerAlle() {
        assertNull(aktionFilter().getItems().get(0), "erster Eintrag steht für alle Aktionen");
        assertEquals(Protokollaktion.values().length + 1, aktionFilter().getItems().size());
        aktionFilter().setValue(Protokollaktion.KUNDE_ANGELEGT);
        assertEquals(2, tabelle.getItems().size());
        assertEquals("2 von 4 Einträgen", FxHilfe.text(ansicht, "anzahl"));
        aktionFilter().getSelectionModel().select(0);
        assertEquals(4, tabelle.getItems().size());
    }

    @Test
    void suche_inBenutzerUndDetails() {
        FxHilfe.tippe(ansicht, "suche", "MÜLLER");
        assertEquals(2, tabelle.getItems().size(), "angelegt + deaktiviert");
        FxHilfe.tippe(ansicht, "suche", "system");
        assertEquals(1, tabelle.getItems().size(), "Ersteinrichtung durch System");
        FxHilfe.tippe(ansicht, "suche", "  ");
        assertEquals(4, tabelle.getItems().size());
    }

    @Test
    void suchtextNull_findetAlles() {
        FxHilfe.<javafx.scene.control.TextField>suche(ansicht, "suche").setText(null);
        controller.aktualisieren();
        assertEquals(4, tabelle.getItems().size());
    }

    @Test
    void neueEintraege_erscheinenNachDemAktualisieren() {
        verwaltung.kundeAktivieren(Testdaten.admin(verwaltung), "K000002");
        controller.aktualisieren();
        assertEquals(Protokollaktion.KUNDE_AKTIVIERT, tabelle.getItems().get(0).aktion());
    }

    @Test
    void ohneSitzung_bleibtDieTabelleLeer() {
        Dialoge.Geladen<ProtokollController> leer = FxHilfe.laden("protokoll.fxml");
        FxHilfe.tippe(leer.wurzel(), "suche", "x");
        leer.controller().aktualisieren();
        assertTrue(FxHilfe.<Protokolleintrag>tabelle(leer.wurzel()).getItems().isEmpty());
    }
}
