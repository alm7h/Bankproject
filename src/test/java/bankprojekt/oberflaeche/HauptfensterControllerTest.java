package bankprojekt.oberflaeche;

import bankprojekt.basisdaten.Waehrung;
import bankprojekt.verwaltung.Bankverwaltung;
import bankprojekt.verwaltung.Benutzer;
import bankprojekt.verwaltung.Kontoart;
import bankprojekt.verwaltung.Kundenakte;
import bankprojekt.verwaltung.Testdaten;
import bankprojekt.verwaltung.VerstellbareUhr;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testklasse für Navigation und Abmelden im Hauptfenster ({@link HauptfensterController}).
 */
@ExtendWith(JavaFxErweiterung.class)
class HauptfensterControllerTest {

    private Bankverwaltung verwaltung;
    private Benutzer admin;
    private Parent fenster;
    private HauptfensterController controller;
    private int abgemeldet;

    @BeforeEach
    void setUp() {
        verwaltung = Testdaten.verwaltung(new VerstellbareUhr());
        admin = Testdaten.admin(verwaltung);
        Dialoge.Geladen<HauptfensterController> geladen = FxHilfe.laden("hauptfenster.fxml");
        fenster = geladen.wurzel();
        controller = geladen.controller();
    }

    private void anmelden() {
        controller.setSitzung(new Sitzung(verwaltung, admin), () -> abgemeldet++);
        FxHilfe.auslegen(fenster);
    }

    private Node aktuelleAnsicht() {
        return FxHilfe.<StackPane>suche(fenster, "inhalt").getChildren().get(0);
    }

    private ToggleButton nav(String id) {
        return FxHilfe.suche(fenster, id);
    }

    @Test
    void setSitzung_zeigtBenutzer_undStartetMitDenKunden() {
        anmelden();
        assertEquals("Angemeldet als\nAdmin (admin)", FxHilfe.<Label>suche(fenster, "angemeldetAls").getText());
        assertTrue(nav("kundenKnopf").isSelected());
        assertNotNull(aktuelleAnsicht().lookup("#kontenAnzeigenKnopf"), "Kundenansicht");
    }

    @Test
    void navigation_wechseltDieAnsicht_undLaedtJedeNurEinmal() {
        anmelden();
        Node kunden = aktuelleAnsicht();

        nav("kontenKnopf").fire();
        assertTrue(nav("kontenKnopf").isSelected());
        assertNotNull(aktuelleAnsicht().lookup("#detailInhalt"), "Kontenansicht");

        nav("benutzerKnopf").fire();
        assertTrue(nav("benutzerKnopf").isSelected());
        assertNotNull(aktuelleAnsicht().lookup("#passwortKnopf"), "Benutzeransicht");

        nav("protokollKnopf").fire();
        assertTrue(nav("protokollKnopf").isSelected());
        assertNotNull(aktuelleAnsicht().lookup("#aktionFilter"), "Protokollansicht");

        nav("kundenKnopf").fire();
        assertSame(kunden, aktuelleAnsicht(), "die Kundenansicht wird wiederverwendet");
    }

    @Test
    void klickAufDenAktivenKnopf_laesstIhnAusgewaehlt() {
        anmelden();
        nav("kundenKnopf").fire();
        assertTrue(nav("kundenKnopf").isSelected());
        assertNotNull(nav("kundenKnopf").getToggleGroup().getSelectedToggle());
    }

    @Test
    @SuppressWarnings("unchecked")
    void kontenAnzeigen_inDerKundenansicht_wechseltZuDenKontenDesKunden() {
        Kundenakte anna = verwaltung.kundeAnlegen(admin, Testdaten.kunde("Anna", "Schmidt"));
        verwaltung.kundeAnlegen(admin, Testdaten.kunde("Bert", "Müller"));
        verwaltung.kontoEroeffnen(admin, anna.getKundennummer(), Kontoart.SPARBUCH, Waehrung.EURO, null);
        verwaltung.kontoEroeffnen(admin, "K000002", Kontoart.SPARBUCH, Waehrung.EURO, null);
        anmelden();

        TableView<Kundenakte> kunden = (TableView<Kundenakte>) aktuelleAnsicht().lookup("#tabelle");
        kunden.getSelectionModel().select(anna);
        FxHilfe.druecke((Parent) aktuelleAnsicht(), "kontenAnzeigenKnopf");

        assertTrue(nav("kontenKnopf").isSelected());
        ComboBox<Kundenakte> filter = FxHilfe.suche((Parent) aktuelleAnsicht(), "kundeFilter");
        assertSame(anna, filter.getValue());
        assertEquals(1, ((TableView<?>) aktuelleAnsicht().lookup("#tabelle")).getItems().size());
    }

    @Test
    void abmelden_ruftDieAbmeldeaktion() {
        anmelden();
        FxHilfe.druecke(fenster, "abmeldenKnopf");
        assertEquals(1, abgemeldet);
    }

    @Test
    void setSitzung_mitNull_wirftNullPointerException() {
        assertThrows(NullPointerException.class, () -> controller.setSitzung(null, () -> { }));
        assertThrows(NullPointerException.class,
                () -> controller.setSitzung(new Sitzung(verwaltung, admin), null));
    }
}
