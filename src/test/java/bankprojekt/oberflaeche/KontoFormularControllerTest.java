package bankprojekt.oberflaeche;

import bankprojekt.basisdaten.Geldbetrag;
import bankprojekt.basisdaten.Waehrung;
import bankprojekt.verwaltung.Bankverwaltung;
import bankprojekt.verwaltung.Kontoart;
import bankprojekt.verwaltung.Kundenakte;
import bankprojekt.verwaltung.Testdaten;
import bankprojekt.verwaltung.VerstellbareUhr;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testklasse für das Formular zum Eröffnen eines Kontos ({@link KontoFormularController}).
 */
@ExtendWith(JavaFxErweiterung.class)
class KontoFormularControllerTest {

    private Parent formular;
    private KontoFormularController controller;
    private Kundenakte anna;
    private Kundenakte bert;

    @BeforeEach
    void setUp() {
        Dialoge.Geladen<KontoFormularController> geladen = FxHilfe.laden("kontoFormular.fxml");
        formular = geladen.wurzel();
        controller = geladen.controller();
        Bankverwaltung verwaltung = Testdaten.verwaltung(new VerstellbareUhr());
        anna = verwaltung.kundeAnlegen(Testdaten.admin(verwaltung), Testdaten.kunde("Anna", "Schmidt"));
        bert = verwaltung.kundeAnlegen(Testdaten.admin(verwaltung), Testdaten.kunde("Bert", "Müller"));
    }

    private ComboBox<Kontoart> kontoart() {
        return FxHilfe.suche(formular, "kontoart");
    }

    private ComboBox<Waehrung> waehrung() {
        return FxHilfe.suche(formular, "waehrung");
    }

    @Test
    void vorgaben_euroUndSaldoNull_alleKontoartenUndWaehrungen() {
        assertEquals(List.of(Kontoart.values()), kontoart().getItems());
        assertEquals(List.of(Waehrung.values()), waehrung().getItems());
        assertEquals(Waehrung.EURO, controller.getWaehrung());
        assertNull(controller.getKontoart());
        assertEquals(new Geldbetrag(0).toString(), FxHilfe.text(formular, "saldo"));
        assertSame(FxHilfe.suche(formular, "fehler"), controller.getFehlerfeld());
    }

    @Test
    void saldo_folgtDerWaehrung_undIstLeerOhneWaehrung() {
        waehrung().setValue(Waehrung.DOBRA);
        assertEquals(new Geldbetrag(0, Waehrung.DOBRA).toString(), FxHilfe.text(formular, "saldo"));
        waehrung().setValue(null);
        assertEquals("", FxHilfe.text(formular, "saldo"));
    }

    @Test
    void dispo_nurBeimGirokonto() {
        assertTrue(FxHilfe.suche(formular, "dispo").isDisabled());
        assertTrue(FxHilfe.suche(formular, "dispoBeschriftung").isDisabled());
        assertNull(controller.getDispo(), "ohne Kontoart kein Dispo");

        kontoart().setValue(Kontoart.SPARBUCH);
        assertTrue(FxHilfe.suche(formular, "dispo").isDisabled());
        FxHilfe.tippe(formular, "dispo", "abc");
        assertNull(controller.getDispo(), "Sparbuch ignoriert den Dispo");

        kontoart().setValue(Kontoart.GIROKONTO);
        assertFalse(FxHilfe.suche(formular, "dispo").isDisabled());
        assertFalse(FxHilfe.suche(formular, "dispoBeschriftung").isDisabled());
        assertThrows(IllegalArgumentException.class, controller::getDispo);
        FxHilfe.tippe(formular, "dispo", "500,50");
        waehrung().setValue(Waehrung.DENAR);
        assertEquals(new Geldbetrag(500.5, Waehrung.DENAR), controller.getDispo());

        waehrung().setValue(null);
        assertNull(controller.getDispo(), "ohne Währung kein Betrag");
    }

    @Test
    void setKunden_mitUndOhneVorauswahl() {
        controller.setKunden(List.of(anna, bert), null);
        assertEquals(List.of(anna, bert), FxHilfe.<ComboBox<Kundenakte>>suche(formular, "kunde").getItems());
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, controller::getKunde);
        assertEquals("Bitte einen Kunden auswählen.", e.getMessage());

        controller.setKunden(List.of(anna, bert), bert);
        assertSame(bert, controller.getKunde());
    }

    @Test
    void vorauswahl_dieNichtInDerListeIst_wirdIgnoriert() {
        controller.setKunden(List.of(anna), bert);
        assertThrows(IllegalArgumentException.class, controller::getKunde);
    }
}
