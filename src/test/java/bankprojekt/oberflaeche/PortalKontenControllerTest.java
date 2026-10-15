package bankprojekt.oberflaeche;

import bankprojekt.basisdaten.Geldbetrag;
import bankprojekt.basisdaten.Waehrung;
import bankprojekt.verwaltung.Bankverwaltung;
import bankprojekt.verwaltung.Benutzer;
import bankprojekt.verwaltung.Buchung;
import bankprojekt.verwaltung.Kontoakte;
import bankprojekt.verwaltung.Kontoart;
import bankprojekt.verwaltung.Kontostatus;
import bankprojekt.verwaltung.Kundenakte;
import bankprojekt.verwaltung.Kundenportal;
import bankprojekt.verwaltung.Kundenzugang;
import bankprojekt.verwaltung.Testdaten;
import bankprojekt.verwaltung.VerstellbareUhr;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testklasse für „Meine Konten" im Kundenportal ({@link PortalKontenController}).
 */
@ExtendWith(JavaFxErweiterung.class)
class PortalKontenControllerTest {

    private Bankverwaltung verwaltung;
    private Benutzer admin;
    private Kundenzugang anna;
    private Kontoakte giro;
    private Kontoakte spar;
    private Kontoakte bertGiro;
    private Parent ansicht;
    private PortalKontenController controller;
    private TableView<Kontoakte> tabelle;

    @BeforeEach
    void setUp() {
        verwaltung = Testdaten.verwaltung(new VerstellbareUhr());
        admin = Testdaten.admin(verwaltung);
        Kundenakte a = verwaltung.kundeAnlegen(admin, Testdaten.kunde("Anna", "Schmidt"));
        Kundenakte b = verwaltung.kundeAnlegen(admin, Testdaten.kunde("Bert", "Müller"));
        anna = Testdaten.kundenzugang(verwaltung, "anna.s", a.getKundennummer());
        giro = verwaltung.kontoEroeffnen(admin, a.getKundennummer(), Kontoart.GIROKONTO, Waehrung.EURO,
                new Geldbetrag(100));
        spar = verwaltung.kontoEroeffnen(admin, a.getKundennummer(), Kontoart.SPARBUCH, Waehrung.DENAR, null);
        bertGiro = verwaltung.kontoEroeffnen(admin, b.getKundennummer(), Kontoart.GIROKONTO, Waehrung.EURO,
                new Geldbetrag(100));
        verwaltung.bareinzahlung(admin, giro.getKontonummer(), new Geldbetrag(200), null);
        verwaltung.bareinzahlung(admin, spar.getKontonummer(), new Geldbetrag(123, Waehrung.DENAR), null);

        Dialoge.Geladen<PortalKontenController> geladen = FxHilfe.laden("portalKonten.fxml");
        ansicht = geladen.wurzel();
        controller = geladen.controller();
        controller.setSitzung(new Kundensitzung(new Kundenportal(verwaltung), anna));
        controller.aktualisieren();
        FxHilfe.auslegen(ansicht);
        tabelle = FxHilfe.tabelle(ansicht);
    }

    private Button knopf(String id) {
        return FxHilfe.suche(ansicht, id);
    }

    @SuppressWarnings("unchecked")
    private TableView<Buchung> auszug() {
        return (TableView<Buchung>) ansicht.lookup("#auszugTabelle");
    }

    @Nested
    class AnzeigeTests {
        @Test
        void zeigtNurEigeneKonten_mitGesamtsaldoInEuro_undWaehltDasErste() {
            assertEquals(List.of(giro, spar), tabelle.getItems());
            assertEquals(giro.toString(), FxHilfe.zelle(tabelle, "Konto", giro));
            assertEquals(new Geldbetrag(123, Waehrung.DENAR), FxHilfe.zelle(tabelle, "Kontostand", spar));
            assertEquals("aktiv", FxHilfe.zelle(tabelle, "Status", spar));
            assertSame(giro, tabelle.getSelectionModel().getSelectedItem());
            assertEquals("Gesamtsaldo (in EUR umgerechnet): " + new Geldbetrag(202), FxHilfe.text(ansicht, "gesamt"));
        }

        @Test
        void details_mitKontoauszug_neuesteBuchungZuerst() {
            assertEquals("Girokonto · " + giro.getIbanFormatiert(), FxHilfe.text(ansicht, "detailTitel"));
            assertEquals(new Geldbetrag(200).toString(), FxHilfe.text(ansicht, "detailStand"));
            assertEquals("aktiv", FxHilfe.text(ansicht, "detailStatus"));
            assertEquals(1, auszug().getItems().size());
            assertEquals("", FxHilfe.text(ansicht, "aktionHinweis"));

            tabelle.getSelectionModel().select(spar);
            assertEquals("Überweisungen sind nur vom Girokonto aus möglich.", FxHilfe.text(ansicht, "aktionHinweis"));
            assertTrue(knopf("ueberweisenKnopf").isDisabled());
            assertFalse(knopf("umbuchenKnopf").isDisabled());
        }

        @Test
        void ohneKonten_bleibtAllesLeer() {
            Kundenakte c = verwaltung.kundeAnlegen(admin, Testdaten.kunde("Carla", "Klein"));
            controller.setSitzung(new Kundensitzung(new Kundenportal(verwaltung),
                    Testdaten.kundenzugang(verwaltung, "carla.k", c.getKundennummer())));
            controller.aktualisieren();
            assertTrue(tabelle.getItems().isEmpty());
            assertEquals("", FxHilfe.text(ansicht, "gesamt"));
            assertFalse(FxHilfe.suche(ansicht, "detailInhalt").isVisible());
            assertTrue(FxHilfe.suche(ansicht, "keineAuswahl").isVisible());
        }

        @Test
        void negativerKontostand_istRot_undAufgeloesteZaehlenNichtZumGesamtsaldo() {
            verwaltung.barauszahlung(admin, giro.getKontonummer(), new Geldbetrag(250), null);
            Kontoakte leer = verwaltung.kontoEroeffnen(admin, giro.getKundennummer(), Kontoart.FESTGELDKONTO,
                    Waehrung.EURO, null);
            verwaltung.kontoAufloesen(admin, leer.getKontonummer());
            controller.aktualisieren();
            FxHilfe.auslegen(ansicht);
            assertTrue(FxHilfe.<Label>suche(ansicht, "detailStand").getStyleClass().contains("negativ"));
            assertEquals("Gesamtsaldo (in EUR umgerechnet): " + new Geldbetrag(-48), FxHilfe.text(ansicht, "gesamt"));
            assertEquals(1, ansicht.lookupAll(".table-row-cell").stream()
                    .filter(z -> z.getStyleClass().contains("inaktiv")).count());

            tabelle.getSelectionModel().select(leer);
            assertEquals("Dieses Konto ist aufgelöst.", FxHilfe.text(ansicht, "aktionHinweis"));
            assertTrue(knopf("sperrenKnopf").isDisabled());
            tabelle.getSelectionModel().select(spar);
            assertFalse(FxHilfe.<Label>suche(ansicht, "detailStand").getStyleClass().contains("negativ"));
        }

        @Test
        void gesperrtesKonto_nurHinweis() {
            verwaltung.kontoSperren(admin, giro.getKontonummer());
            controller.aktualisieren();
            assertTrue(knopf("ueberweisenKnopf").isDisabled() && knopf("umbuchenKnopf").isDisabled()
                    && knopf("sperrenKnopf").isDisabled());
            assertTrue(FxHilfe.text(ansicht, "aktionHinweis").startsWith("Dieses Konto ist gesperrt."));
            assertTrue(FxHilfe.<Label>suche(ansicht, "detailStatus").getStyleClass().contains("status-gesperrt"));
            tabelle.getSelectionModel().select(spar);
            assertTrue(knopf("umbuchenKnopf").isDisabled(), "kein anderes aktives Konto als Ziel");
        }

        @Test
        void auswahl_bleibtNachDemAktualisierenErhalten() {
            tabelle.getSelectionModel().select(spar);
            controller.aktualisieren();
            assertSame(spar, tabelle.getSelectionModel().getSelectedItem());
        }
    }

    @Nested
    class AktionenTests {
        @Test
        void ueberweisen_mitFehlerImFormular_dannGueltig() {
            Dialogantworten antworten = new Dialogantworten().dann(d -> {
                Parent formular = Dialogantworten.inhalt(d);
                assertTrue(FxHilfe.text(formular, "von").startsWith(giro.getIbanFormatiert()));
                assertEquals("EUR", FxHilfe.text(formular, "waehrung"));
                FxHilfe.tippe(formular, "iban", bertGiro.getIbanFormatiert());
                FxHilfe.tippe(formular, "betrag", "999");
                Dialogantworten.knopf(d, "Speichern").fire();
                assertTrue(FxHilfe.text(formular, "fehler").startsWith("Die Überweisung ist nicht gedeckt"));
                FxHilfe.tippe(formular, "betrag", "75,50");
                FxHilfe.tippe(formular, "zweck", "Miete");
                Dialogantworten.knopf(d, "Speichern").fire();
            });
            knopf("ueberweisenKnopf").fire();
            assertEquals("Überweisung", antworten.letzter().getHeaderText());
            assertEquals(new Geldbetrag(124.5), giro.getKontostand());
            assertEquals(new Geldbetrag(75.5), bertGiro.getKontostand());
            assertEquals(new Geldbetrag(124.5).toString(), FxHilfe.text(ansicht, "detailStand"));
            assertEquals(2, auszug().getItems().size(), "Kontoauszug aufgefrischt");
        }

        @Test
        void umbuchen_aufDasSparbuch() {
            new Dialogantworten().dann(d -> {
                Parent formular = Dialogantworten.inhalt(d);
                ComboBox<Kontoakte> ziel = FxHilfe.suche(formular, "ziel");
                assertSame(spar, ziel.getValue(), "einziges Ziel vorausgewählt");
                FxHilfe.tippe(formular, "betrag", "10");
                FxHilfe.tippe(formular, "text", "Sparen");
                Dialogantworten.knopf(d, "Speichern").fire();
            });
            knopf("umbuchenKnopf").fire();
            assertEquals(new Geldbetrag(190), giro.getKontostand());
            assertEquals(new Geldbetrag(123 + 615, Waehrung.DENAR), spar.getKontostand());
        }

        @Test
        void abgebrocheneDialoge_aendernNichts() {
            new Dialogantworten().klicke("Abbrechen").klicke("Abbrechen").klicke("Abbrechen");
            knopf("ueberweisenKnopf").fire();
            knopf("umbuchenKnopf").fire();
            knopf("sperrenKnopf").fire();
            assertEquals(new Geldbetrag(200), giro.getKontostand());
            assertEquals(Kontostatus.AKTIV, giro.getStatus());
        }

        @Test
        void kontoSperren_nachRueckfrage() {
            Dialogantworten antworten = new Dialogantworten().klicke("Sperren");
            knopf("sperrenKnopf").fire();
            assertEquals(Kontostatus.GESPERRT, giro.getStatus());
            assertTrue(antworten.letzter().getContentText().contains("Entsperren kann das Konto nur Ihre Bank"));
            assertEquals("gesperrt", FxHilfe.text(ansicht, "detailStatus"));
        }

        @Test
        void aktionen_ohneAuswahl_tunNichts() {
            tabelle.getSelectionModel().clearSelection();
            for (String id : List.of("ueberweisenKnopf", "umbuchenKnopf", "sperrenKnopf")) {
                knopf(id).setDisable(false);
                knopf(id).fire();
            }
            assertEquals(Kontostatus.AKTIV, giro.getStatus());
        }
    }
}
