package bankprojekt.oberflaeche;

import bankprojekt.basisdaten.Waehrung;
import bankprojekt.verwaltung.Bankverwaltung;
import bankprojekt.verwaltung.Benutzer;
import bankprojekt.verwaltung.Kontoart;
import bankprojekt.verwaltung.Kundenakte;
import bankprojekt.verwaltung.Kundenportal;
import bankprojekt.verwaltung.Kundenzugang;
import bankprojekt.verwaltung.Testdaten;
import bankprojekt.verwaltung.VerstellbareUhr;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testklasse für Begrüßung und Navigation im Kundenportal ({@link KundenportalController}).
 */
@ExtendWith(JavaFxErweiterung.class)
class KundenportalControllerTest {

    private Bankverwaltung verwaltung;
    private Benutzer admin;
    private Kundenportal portal;
    private Parent fenster;
    private KundenportalController controller;
    private int abgemeldet;

    @BeforeEach
    void setUp() {
        verwaltung = Testdaten.verwaltung(new VerstellbareUhr());
        admin = Testdaten.admin(verwaltung);
        portal = new Kundenportal(verwaltung);
        Dialoge.Geladen<KundenportalController> geladen = FxHilfe.laden("kundenportal.fxml");
        fenster = geladen.wurzel();
        controller = geladen.controller();
    }

    private void anmelden(Kundenzugang zugang) {
        controller.setSitzung(new Kundensitzung(portal, zugang), () -> abgemeldet++);
        FxHilfe.auslegen(fenster);
    }

    private ToggleButton nav(String id) {
        return FxHilfe.suche(fenster, id);
    }

    private Node ansicht() {
        return FxHilfe.<StackPane>suche(fenster, "inhalt").getChildren().get(0);
    }

    @Test
    void kunde_wirdMitVornamenBegruesst_undSiehtSeineKonten() {
        Kundenakte anna = verwaltung.kundeAnlegen(admin, Testdaten.kunde("Anna", "Schmidt"));
        verwaltung.kontoEroeffnen(admin, anna.getKundennummer(), Kontoart.SPARBUCH, Waehrung.EURO, null);
        anmelden(Testdaten.kundenzugang(verwaltung, "anna.s", anna.getKundennummer()));

        String gruss = FxHilfe.text(fenster, "begruessung");
        assertTrue(gruss.matches("Guten (Morgen|Tag|Abend), Anna!"), gruss);
        assertTrue(FxHilfe.text(fenster, "unterzeile").startsWith("Schön, dass Sie da sind."));
        assertEquals("Angemeldet als\nanna.s", FxHilfe.text(fenster, "angemeldetAls"));
        assertTrue(nav("kontenKnopf").isSelected());
        assertTrue(nav("kontenKnopf").isVisible() && nav("datenKnopf").isVisible());
        assertNotNull(ansicht().lookup("#ueberweisenKnopf"), "Meine Konten");
    }

    @Test
    void navigation_zwischenDenAnsichten() {
        Kundenakte anna = verwaltung.kundeAnlegen(admin, Testdaten.kunde("Anna", "Schmidt"));
        anmelden(Testdaten.kundenzugang(verwaltung, "anna.s", anna.getKundennummer()));
        Node konten = ansicht();

        nav("antraegeKnopf").fire();
        assertNotNull(ansicht().lookup("#antragTabelle"));
        nav("datenKnopf").fire();
        assertNotNull(ansicht().lookup("#speichernKnopf"));
        nav("datenKnopf").fire();
        assertTrue(nav("datenKnopf").isSelected(), "aktiver Knopf bleibt ausgewählt");
        nav("kontenKnopf").fire();
        assertSame(konten, ansicht(), "Ansicht wird wiederverwendet");
    }

    @Test
    void neukundeMitOffenemAntrag_siehtNurDieAntraege() {
        Testdaten.neukunde(verwaltung, "carla.k", Testdaten.kunde("Carla", "Klein"), Kontoart.GIROKONTO);
        anmelden(verwaltung.findeKundenzugang("carla.k").orElseThrow());

        assertTrue(FxHilfe.text(fenster, "begruessung").endsWith(", Carla!"));
        assertTrue(FxHilfe.text(fenster, "unterzeile").startsWith("Ihr Antrag wird"));
        assertFalse(nav("kontenKnopf").isVisible());
        assertFalse(nav("datenKnopf").isManaged());
        assertTrue(nav("antraegeKnopf").isSelected());
        assertNotNull(ansicht().lookup("#antragTabelle"));
    }

    @Test
    void abgelehnterNeukunde_erfaehrtVonDerEntscheidung() {
        Testdaten.neukunde(verwaltung, "carla.k", Testdaten.kunde("Carla", "Klein"), Kontoart.GIROKONTO);
        verwaltung.antragAblehnen(admin, "A000001", "Gehaltsnachweis fehlt");
        anmelden(verwaltung.findeKundenzugang("carla.k").orElseThrow());
        assertTrue(FxHilfe.text(fenster, "unterzeile").startsWith("Zu Ihrem Antrag gibt es eine Entscheidung"));
    }

    @Test
    void zugangOhneAntrag_wirdMitBenutzernamenBegruesst() {
        anmelden(Testdaten.kundenzugang(verwaltung, "neu", null));
        assertTrue(FxHilfe.text(fenster, "begruessung").endsWith(", neu!"));
    }

    @Test
    void abmelden_ruftDieAbmeldeaktion() {
        anmelden(Testdaten.kundenzugang(verwaltung, "neu", null));
        FxHilfe.druecke(fenster, "abmeldenKnopf");
        assertEquals(1, abgemeldet);
    }

    @Test
    void setSitzung_mitNull_wirft() {
        Kundenzugang zugang = Testdaten.kundenzugang(verwaltung, "neu", null);
        assertThrows(NullPointerException.class, () -> controller.setSitzung(null, () -> { }));
        assertThrows(NullPointerException.class,
                () -> controller.setSitzung(new Kundensitzung(portal, zugang), null));
        assertThrows(NullPointerException.class, () -> new Kundensitzung(null, zugang));
        assertThrows(NullPointerException.class, () -> new Kundensitzung(portal, null));
    }
}
