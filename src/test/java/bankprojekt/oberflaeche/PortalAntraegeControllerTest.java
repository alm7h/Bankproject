package bankprojekt.oberflaeche;

import bankprojekt.basisdaten.Waehrung;
import bankprojekt.verwaltung.Antragsart;
import bankprojekt.verwaltung.Bankverwaltung;
import bankprojekt.verwaltung.Benutzer;
import bankprojekt.verwaltung.Kontoakte;
import bankprojekt.verwaltung.Kontoantrag;
import bankprojekt.verwaltung.Kontoart;
import bankprojekt.verwaltung.Kundenakte;
import bankprojekt.verwaltung.Kundenportal;
import bankprojekt.verwaltung.Kundenzugang;
import bankprojekt.verwaltung.Testdaten;
import bankprojekt.verwaltung.VerstellbareUhr;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testklasse für „Anträge" im Kundenportal ({@link PortalAntraegeController}).
 */
@ExtendWith(JavaFxErweiterung.class)
class PortalAntraegeControllerTest {

    private Bankverwaltung verwaltung;
    private Benutzer admin;
    private Kundenportal portal;
    private Parent ansicht;
    private PortalAntraegeController controller;
    private TableView<Kontoantrag> tabelle;

    @BeforeEach
    void setUp() {
        verwaltung = Testdaten.verwaltung(new VerstellbareUhr());
        admin = Testdaten.admin(verwaltung);
        portal = new Kundenportal(verwaltung);
        Dialoge.Geladen<PortalAntraegeController> geladen = FxHilfe.laden("portalAntraege.fxml");
        ansicht = geladen.wurzel();
        controller = geladen.controller();
        tabelle = FxHilfe.suche(ansicht, "antragTabelle");
    }

    private void anmelden(Kundenzugang zugang) {
        controller.setSitzung(new Kundensitzung(portal, zugang));
        controller.aktualisieren();
    }

    private Kundenzugang neukunde() {
        Testdaten.neukunde(verwaltung, "carla.k", Testdaten.kunde("Carla", "Klein"), Kontoart.GIROKONTO);
        return verwaltung.findeKundenzugang("carla.k").orElseThrow();
    }

    private boolean sichtbar(String id) {
        return FxHilfe.suche(ansicht, id).isVisible() && FxHilfe.suche(ansicht, id).isManaged();
    }

    @Test
    void offenerNeukundenantrag_wirdGeprueft_ohneAktionen() {
        Kundenzugang carla = neukunde();
        anmelden(carla);
        Kontoantrag antrag = tabelle.getItems().get(0);
        assertEquals("A000001", FxHilfe.zelle(tabelle, "Antrag", antrag));
        assertEquals("Neukunde", FxHilfe.zelle(tabelle, "Art", antrag));
        assertEquals("Girokonto", FxHilfe.zelle(tabelle, "Kontoart", antrag));
        assertEquals("EUR", FxHilfe.zelle(tabelle, "Währung", antrag));
        assertEquals("14.10.2026 10:00:00", FxHilfe.zelle(tabelle, "Eingereicht", antrag));
        assertEquals("offen", FxHilfe.zelle(tabelle, "Status", antrag));
        assertEquals("wird geprüft", FxHilfe.zelle(tabelle, "Bemerkung", antrag));
        assertTrue(FxHilfe.text(ansicht, "hinweis").startsWith("Ihr Antrag wird geprüft."));
        assertFalse(sichtbar("zusatzKnopf"));
        assertFalse(sichtbar("erneutKnopf"));
    }

    @Test
    void abgelehnterAntrag_zeigtDieBegruendung_undErlaubtEinenNeuen() {
        Kundenzugang carla = neukunde();
        verwaltung.antragAblehnen(admin, "A000001", "Gehaltsnachweis fehlt");
        anmelden(carla);
        assertEquals("Begründung: Gehaltsnachweis fehlt", FxHilfe.zelle(tabelle, "Bemerkung", tabelle.getItems().get(0)));
        assertTrue(FxHilfe.text(ansicht, "hinweis").startsWith("Ihr Antrag wurde leider abgelehnt."));
        assertTrue(sichtbar("erneutKnopf"));

        new Dialogantworten().dann(d -> {
            Parent formular = Dialogantworten.inhalt(d);
            assertTrue(FxHilfe.suche(formular, "datenBereich").isVisible());
            assertEquals("Carla", FxHilfe.<TextField>suche(formular, "vorname").getText(), "vorbelegt");
            Dialogantworten.knopf(d, "Speichern").fire();
            assertEquals("Bitte eine Kontoart auswählen.", FxHilfe.text(formular, "antragFehler"));
            FxHilfe.<ComboBox<Kontoart>>suche(formular, "kontoart").setValue(Kontoart.SPARBUCH);
            Dialogantworten.knopf(d, "Speichern").fire();
        });
        FxHilfe.druecke(ansicht, "erneutKnopf");

        assertEquals(2, tabelle.getItems().size());
        assertEquals("A000002", tabelle.getItems().get(0).getAntragsnummer(), "neuester zuerst");
        assertFalse(sichtbar("erneutKnopf"), "jetzt ist wieder ein Antrag offen");
    }

    @Test
    void genehmigterAntrag_zeigtDasKonto() {
        Kundenzugang carla = neukunde();
        Kontoakte konto = verwaltung.antragGenehmigen(admin, "A000001", new bankprojekt.basisdaten.Geldbetrag(200));
        anmelden(carla);
        assertEquals("Konto eröffnet: " + konto, FxHilfe.zelle(tabelle, "Bemerkung", tabelle.getItems().get(0)));
        assertTrue(FxHilfe.text(ansicht, "hinweis").startsWith("Hier sehen Sie Ihre Kontoanträge."));
        assertTrue(sichtbar("zusatzKnopf"));
    }

    @Test
    void genehmigterAntrag_eines_inzwischen_aufgeloestenKontos_bleibtLesbar() {
        Kundenzugang carla = neukunde();
        Kontoakte konto = verwaltung.antragGenehmigen(admin, "A000001", new bankprojekt.basisdaten.Geldbetrag(200));
        verwaltung.kontoAufloesen(admin, konto.getKontonummer());
        anmelden(carla);
        assertTrue(((String) FxHilfe.zelle(tabelle, "Bemerkung", tabelle.getItems().get(0))).startsWith("Konto eröffnet"));
    }

    @Test
    void kunde_beantragtEinWeiteresKonto() {
        Kundenakte anna = verwaltung.kundeAnlegen(admin, Testdaten.kunde("Anna", "Schmidt"));
        Kundenzugang zugang = Testdaten.kundenzugang(verwaltung, "anna.s", anna.getKundennummer());
        anmelden(zugang);
        assertTrue(tabelle.getItems().isEmpty());

        Dialogantworten antworten = new Dialogantworten().dann(d -> {
            Parent formular = Dialogantworten.inhalt(d);
            assertFalse(FxHilfe.suche(formular, "datenBereich").isVisible());
            FxHilfe.<ComboBox<Kontoart>>suche(formular, "kontoart").setValue(Kontoart.FESTGELDKONTO);
            FxHilfe.<ComboBox<Waehrung>>suche(formular, "waehrung").setValue(Waehrung.FRANC);
            Dialogantworten.knopf(d, "Speichern").fire();
        });
        FxHilfe.druecke(ansicht, "zusatzKnopf");

        assertEquals("Weiteres Konto beantragen", antworten.letzter().getHeaderText());
        Kontoantrag antrag = tabelle.getItems().get(0);
        assertEquals(Antragsart.ZUSATZKONTO, antrag.getArt());
        assertEquals(Waehrung.FRANC, antrag.getWaehrung());
    }

    @Test
    void abgebrocheneAntraege_legenNichtsAn() {
        Kundenakte anna = verwaltung.kundeAnlegen(admin, Testdaten.kunde("Anna", "Schmidt"));
        anmelden(Testdaten.kundenzugang(verwaltung, "anna.s", anna.getKundennummer()));
        new Dialogantworten().klicke("Abbrechen");
        FxHilfe.druecke(ansicht, "zusatzKnopf");
        assertTrue(verwaltung.getAntraege(null).isEmpty());
    }

    @Test
    void neuerAntrag_ohneFruehereDaten_mitLeeremFormular() {
        Kundenzugang neu = Testdaten.kundenzugang(verwaltung, "neu", null);
        anmelden(neu);
        assertTrue(sichtbar("erneutKnopf"), "ohne offenen Antrag kann beantragt werden");
        new Dialogantworten().dann(d -> {
            assertEquals("", FxHilfe.<TextField>suche(Dialogantworten.inhalt(d), "vorname").getText());
            Dialogantworten.knopf(d, "Abbrechen").fire();
        });
        FxHilfe.druecke(ansicht, "erneutKnopf");
        assertEquals(List.of(), portal.getAntraege(neu));
    }
}
