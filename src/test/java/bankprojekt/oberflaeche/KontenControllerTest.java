package bankprojekt.oberflaeche;

import bankprojekt.basisdaten.Geldbetrag;
import bankprojekt.basisdaten.Waehrung;
import bankprojekt.exceptions.GesperrtException;
import bankprojekt.verwaltung.Bankverwaltung;
import bankprojekt.verwaltung.Benutzer;
import bankprojekt.verwaltung.Kontoakte;
import bankprojekt.verwaltung.Kontoart;
import bankprojekt.verwaltung.Kontostatus;
import bankprojekt.verwaltung.Kundenakte;
import bankprojekt.verwaltung.Testdaten;
import bankprojekt.verwaltung.VerstellbareUhr;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testklasse für die Kontenansicht ({@link KontenController}).
 */
@ExtendWith(JavaFxErweiterung.class)
class KontenControllerTest {

    private Bankverwaltung verwaltung;
    private Benutzer admin;
    private Kundenakte anna;
    private Kundenakte bert;
    private Kontoakte giro;
    private Kontoakte spar;
    private Kontoakte bertSpar;
    private Parent ansicht;
    private KontenController controller;
    private TableView<Kontoakte> tabelle;

    @BeforeEach
    void setUp() {
        verwaltung = Testdaten.verwaltung(new VerstellbareUhr());
        admin = Testdaten.admin(verwaltung);
        anna = verwaltung.kundeAnlegen(admin, Testdaten.kunde("Anna", "Schmidt"));
        bert = verwaltung.kundeAnlegen(admin, Testdaten.kunde("Bert", "Müller"));
        giro = verwaltung.kontoEroeffnen(admin, anna.getKundennummer(), Kontoart.GIROKONTO, Waehrung.EURO,
                new Geldbetrag(500));
        spar = verwaltung.kontoEroeffnen(admin, anna.getKundennummer(), Kontoart.SPARBUCH, Waehrung.DENAR, null);
        bertSpar = verwaltung.kontoEroeffnen(admin, bert.getKundennummer(), Kontoart.SPARBUCH, Waehrung.EURO, null);

        Dialoge.Geladen<KontenController> geladen = FxHilfe.laden("konten.fxml");
        ansicht = geladen.wurzel();
        controller = geladen.controller();
        controller.setSitzung(new Sitzung(verwaltung, admin));
        controller.aktualisieren();
        FxHilfe.auslegen(ansicht);
        tabelle = FxHilfe.tabelle(ansicht);
    }

    private Button knopf(String id) {
        return FxHilfe.suche(ansicht, id);
    }

    private ComboBox<Kundenakte> kundeFilter() {
        return FxHilfe.suche(ansicht, "kundeFilter");
    }

    private ComboBox<Kontostatus> statusFilter() {
        return FxHilfe.suche(ansicht, "statusFilter");
    }

    private void waehle(Kontoakte konto) {
        tabelle.getSelectionModel().select(konto);
    }

    @Nested
    class TabelleTests {
        @Test
        void zeigtAlleKontenMitIhrenDaten() {
            assertEquals(List.of(giro, spar, bertSpar), tabelle.getItems());
            assertEquals(giro.getIbanFormatiert(), FxHilfe.zelle(tabelle, "IBAN", giro));
            assertEquals("Girokonto", FxHilfe.zelle(tabelle, "Kontoart", giro));
            assertEquals("Schmidt, Anna", FxHilfe.zelle(tabelle, "Inhaber", giro));
            assertEquals("K000001", FxHilfe.zelle(tabelle, "Kundennr.", giro));
            assertEquals(new Geldbetrag(0, Waehrung.DENAR), FxHilfe.zelle(tabelle, "Kontostand", spar));
            assertEquals("aktiv", FxHilfe.zelle(tabelle, "Status", giro));
            assertEquals("3 Konten", FxHilfe.text(ansicht, "anzahl"));
        }

        @Test
        void kontostand_wirdRechtsbuendigUndLiveAngezeigt() throws GesperrtException {
            Testdaten.einzahlen(verwaltung, giro.getKontonummer(), new Geldbetrag(125.5));
            FxHilfe.auslegen(ansicht);
            List<String> staende = ansicht.lookupAll(".table-cell").stream()
                    .filter(n -> n instanceof TableCell<?, ?> z && z.getItem() instanceof Geldbetrag)
                    .map(n -> (TableCell<?, ?>) n)
                    .peek(z -> assertEquals(javafx.geometry.Pos.CENTER_RIGHT, z.getAlignment()))
                    .map(TableCell::getText)
                    .toList();
            assertTrue(staende.contains(new Geldbetrag(125.5).toString()), staende.toString());
        }

        @Test
        @SuppressWarnings("unchecked")
        void kontostandzelle_ohneWert_bleibtLeer() {
            // dieselbe Zellfabrik wie die Kontostandspalte, aber mit fehlendem Wert
            TableColumn<Kontoakte, Geldbetrag> original = (TableColumn<Kontoakte, Geldbetrag>) tabelle.getColumns()
                    .stream().filter(s -> "Kontostand".equals(s.getText())).findFirst().orElseThrow();
            TableColumn<Kontoakte, Geldbetrag> ohneWert = new TableColumn<>("Kontostand");
            ohneWert.setCellValueFactory(z -> new ReadOnlyObjectWrapper<>(null));
            ohneWert.setCellFactory(original.getCellFactory());
            TableView<Kontoakte> probe = new TableView<>(FXCollections.observableArrayList(giro));
            probe.getColumns().add(ohneWert);
            new Scene(probe, 300, 200);
            FxHilfe.auslegen(probe);
            assertTrue(probe.lookupAll(".table-cell").stream()
                    .allMatch(n -> ((TableCell<?, ?>) n).getText() == null));
        }

        @Test
        void auswahl_dieWegfiltertWird_istDanachLeer_undDieDetailsVerschwinden() {
            waehle(giro);
            kundeFilter().setValue(bert);
            assertNull(tabelle.getSelectionModel().getSelectedItem());
            assertFalse(FxHilfe.suche(ansicht, "detailInhalt").isVisible());
        }

        @Test
        void einzigesKonto_wirdImSingularGezaehlt() {
            Bankverwaltung eine = Testdaten.verwaltung(new VerstellbareUhr());
            Benutzer chef = Testdaten.admin(eine);
            eine.kundeAnlegen(chef, Testdaten.kunde("Carla", "Klein"));
            eine.kontoEroeffnen(chef, "K000001", Kontoart.SPARBUCH, Waehrung.EURO, null);
            controller.setSitzung(new Sitzung(eine, chef));
            controller.aktualisieren();
            assertEquals("1 Konto", FxHilfe.text(ansicht, "anzahl"));
        }

        @Test
        void kundenfilter_enthaeltAlleKunden_undNullFuerAlle() {
            assertEquals(java.util.Arrays.asList(null, anna, bert), kundeFilter().getItems());
            kundeFilter().setValue(anna);
            assertEquals(List.of(giro, spar), tabelle.getItems());
            assertEquals("2 von 3 Konten", FxHilfe.text(ansicht, "anzahl"));
        }

        @Test
        void statusfilter_undSuche() {
            verwaltung.kontoSperren(admin, spar.getKontonummer());
            controller.aktualisieren();
            statusFilter().setValue(Kontostatus.GESPERRT);
            assertEquals(List.of(spar), tabelle.getItems());
            statusFilter().setValue(null);

            FxHilfe.tippe(ansicht, "suche", "müller");
            assertEquals(List.of(bertSpar), tabelle.getItems());
            FxHilfe.tippe(ansicht, "suche", bertSpar.getIbanFormatiert());
            assertEquals(List.of(bertSpar), tabelle.getItems());
        }

        @Test
        void filterZuruecksetzen_zeigtWiederAlles() {
            kundeFilter().setValue(bert);
            statusFilter().setValue(Kontostatus.AKTIV);
            FxHilfe.tippe(ansicht, "suche", "DE");
            FxHilfe.knopf(ansicht, "Filter zurücksetzen").fire();
            assertNull(kundeFilter().getValue());
            assertNull(statusFilter().getValue());
            assertEquals("", FxHilfe.<TextField>suche(ansicht, "suche").getText());
            assertEquals(3, tabelle.getItems().size());
        }

        @Test
        void aktualisieren_behaeltDenKundenfilter_undDieAuswahl() {
            kundeFilter().setValue(anna);
            waehle(spar);
            verwaltung.kundeAnlegen(admin, Testdaten.kunde("Carla", "Klein"));
            controller.aktualisieren();
            assertSame(anna, kundeFilter().getValue());
            assertEquals(4, kundeFilter().getItems().size());
            assertSame(spar, tabelle.getSelectionModel().getSelectedItem());
        }

        @Test
        void kontenVonKundeZeigen_setztDenKundenfilter() {
            FxHilfe.tippe(ansicht, "suche", "xyz");
            statusFilter().setValue(Kontostatus.GESPERRT);
            controller.kontenVonKundeZeigen(bert);
            assertSame(bert, kundeFilter().getValue());
            assertNull(statusFilter().getValue());
            assertEquals(List.of(bertSpar), tabelle.getItems());
        }

        @Test
        void aufgeloesteKonten_sindAusgegraut() {
            verwaltung.kontoAufloesen(admin, bertSpar.getKontonummer());
            controller.aktualisieren();
            FxHilfe.auslegen(ansicht);
            long grau = ansicht.lookupAll(".table-row-cell").stream()
                    .filter(z -> z.getStyleClass().contains("inaktiv")).count();
            assertEquals(1, grau);
        }

        @Test
        void ohneSitzung_wirdNichtGesucht() {
            Dialoge.Geladen<KontenController> leer = FxHilfe.laden("konten.fxml");
            FxHilfe.tippe(leer.wurzel(), "suche", "DE");   // löst die Suche aus
            leer.controller().aktualisieren();
            assertTrue(FxHilfe.<Kontoakte>tabelle(leer.wurzel()).getItems().isEmpty());
        }
    }

    @Nested
    class DetailTests {
        @Test
        void ohneAuswahl_nurDerHinweis() {
            assertFalse(FxHilfe.suche(ansicht, "detailInhalt").isVisible());
            assertTrue(FxHilfe.suche(ansicht, "keineAuswahl").isVisible());
        }

        @Test
        void auswahl_zeigtAlleDetails() {
            waehle(spar);
            assertTrue(FxHilfe.suche(ansicht, "detailInhalt").isVisible());
            assertFalse(FxHilfe.suche(ansicht, "keineAuswahl").isVisible());
            assertEquals(spar.getIbanFormatiert(), FxHilfe.text(ansicht, "detailIban"));
            assertEquals(new Geldbetrag(0, Waehrung.DENAR).toString(), FxHilfe.text(ansicht, "detailStand"));
            assertEquals("2", FxHilfe.text(ansicht, "detailNummer"));
            assertEquals("Sparbuch", FxHilfe.text(ansicht, "detailArt"));
            assertEquals("Schmidt, Anna (K000001)", FxHilfe.text(ansicht, "detailInhaber"));
            assertEquals("MKD", FxHilfe.text(ansicht, "detailWaehrung"));
            assertEquals("aktiv", FxHilfe.text(ansicht, "detailStatus"));
            assertTrue(FxHilfe.<Label>suche(ansicht, "detailStatus").getStyleClass().contains("status-aktiv"));
            assertEquals("14.10.2026", FxHilfe.text(ansicht, "detailEroeffnet"));
            assertEquals("–", FxHilfe.text(ansicht, "detailAufgeloest"));
        }

        @Test
        void kontostand_wirdLiveAktualisiert_undNegativRot() throws GesperrtException {
            waehle(giro);
            Testdaten.einzahlen(verwaltung, giro.getKontonummer(), new Geldbetrag(10));
            assertEquals(new Geldbetrag(10).toString(), FxHilfe.text(ansicht, "detailStand"));

            Testdaten.abheben(verwaltung, giro.getKontonummer(), new Geldbetrag(60));
            waehle(spar);
            waehle(giro);
            Label stand = FxHilfe.suche(ansicht, "detailStand");
            assertTrue(stand.getStyleClass().contains("negativ"));
            waehle(spar);
            assertFalse(stand.getStyleClass().contains("negativ"));
        }

        @Test
        void knoepfe_passenZumStatus() throws GesperrtException {
            waehle(giro);
            assertFalse(knopf("sperrenKnopf").isDisabled());
            assertTrue(knopf("entsperrenKnopf").isDisabled());
            assertFalse(knopf("aufloesenKnopf").isDisabled());
            assertEquals("", FxHilfe.text(ansicht, "aufloesenHinweis"));

            Testdaten.einzahlen(verwaltung, giro.getKontonummer(), new Geldbetrag(1));
            waehle(spar);
            waehle(giro);
            assertTrue(knopf("aufloesenKnopf").isDisabled(), "Guthaben -> nicht auflösbar");
            assertEquals("Auflösen ist nur bei einem Kontostand von 0 möglich.", FxHilfe.text(ansicht, "aufloesenHinweis"));

            verwaltung.kontoSperren(admin, spar.getKontonummer());
            controller.aktualisieren();
            waehle(spar);
            assertTrue(knopf("sperrenKnopf").isDisabled());
            assertFalse(knopf("entsperrenKnopf").isDisabled());

            verwaltung.kontoAufloesen(admin, spar.getKontonummer());
            controller.aktualisieren();
            assertTrue(knopf("sperrenKnopf").isDisabled() && knopf("entsperrenKnopf").isDisabled()
                    && knopf("aufloesenKnopf").isDisabled());
            assertEquals("", FxHilfe.text(ansicht, "aufloesenHinweis"));
            assertEquals("14.10.2026", FxHilfe.text(ansicht, "detailAufgeloest"));
        }
    }

    @Nested
    class AktionenTests {
        @Test
        void neuesKonto_mitFehlendemDispo_dannGueltig_wirdEroeffnetUndAusgewaehlt() {
            kundeFilter().setValue(bert);
            Dialogantworten antworten = new Dialogantworten().dann(d -> {
                Parent formular = Dialogantworten.inhalt(d);
                ComboBox<Kundenakte> kunde = FxHilfe.suche(formular, "kunde");
                assertSame(bert, kunde.getValue(), "Kunde aus dem Filter vorausgewählt");
                assertFalse(kunde.getItems().contains(null));
                FxHilfe.<ComboBox<Kontoart>>suche(formular, "kontoart").setValue(Kontoart.GIROKONTO);
                Dialogantworten.knopf(d, "Speichern").fire();
                assertEquals("Dispo ist ein Pflichtfeld.", FxHilfe.text(formular, "fehler"));
                FxHilfe.tippe(formular, "dispo", "750");
                Dialogantworten.knopf(d, "Speichern").fire();
            });

            FxHilfe.knopf(ansicht, "Neues Konto").fire();

            assertEquals("Neues Konto eröffnen", antworten.letzter().getHeaderText());
            Kontoakte neu = verwaltung.findeKonto(4L).orElseThrow();
            assertEquals(Kontoart.GIROKONTO, neu.getKontoart());
            assertEquals("K000002", neu.getKundennummer());
            assertSame(neu, tabelle.getSelectionModel().getSelectedItem());
            assertSame(bert, kundeFilter().getValue(), "Kundenfilter bleibt");
        }

        @Test
        void neuesKonto_gesperrteKundenSindNichtWaehlbar_undAbbrechenEroeffnetNichts() {
            verwaltung.kundeDeaktivieren(admin, bert.getKundennummer());
            new Dialogantworten().dann(d -> {
                ComboBox<Kundenakte> kunde = FxHilfe.suche(Dialogantworten.inhalt(d), "kunde");
                assertEquals(List.of(anna), kunde.getItems());
                Dialogantworten.knopf(d, "Abbrechen").fire();
            });
            FxHilfe.knopf(ansicht, "Neues Konto").fire();
            assertEquals(3, verwaltung.getKonten().size());
        }

        @Test
        void sperren_nurNachBestaetigung() {
            waehle(giro);
            Dialogantworten antworten = new Dialogantworten().klicke("Abbrechen").klicke("Sperren");
            knopf("sperrenKnopf").fire();
            assertEquals(Kontostatus.AKTIV, giro.getStatus());
            assertTrue(antworten.letzter().getContentText().contains(giro.getIbanFormatiert()));
            knopf("sperrenKnopf").fire();
            assertEquals(Kontostatus.GESPERRT, giro.getStatus());
            assertEquals("gesperrt", FxHilfe.text(ansicht, "detailStatus"));
        }

        @Test
        void entsperren_ohneRueckfrage() {
            verwaltung.kontoSperren(admin, giro.getKontonummer());
            controller.aktualisieren();
            waehle(giro);
            knopf("entsperrenKnopf").fire();
            assertEquals(Kontostatus.AKTIV, giro.getStatus());
        }

        @Test
        void aufloesen_nurNachBestaetigung() {
            waehle(spar);
            Dialogantworten antworten = new Dialogantworten().klicke("Abbrechen").klicke("Auflösen");
            knopf("aufloesenKnopf").fire();
            assertEquals(Kontostatus.AKTIV, spar.getStatus());
            assertEquals("Konto auflösen", antworten.letzter().getHeaderText());
            knopf("aufloesenKnopf").fire();
            assertEquals(Kontostatus.AUFGELOEST, spar.getStatus());
        }

        @Test
        void fehlerDerVerwaltung_werdenGemeldet() throws GesperrtException {
            // Guthaben entsteht, nachdem der Knopf schon freigegeben war
            waehle(spar);
            Testdaten.einzahlen(verwaltung, spar.getKontonummer(), new Geldbetrag(5, Waehrung.DENAR));
            Dialogantworten antworten = new Dialogantworten().klicke("Auflösen").klicke("OK");
            knopf("aufloesenKnopf").fire();
            assertTrue(antworten.letzter().getContentText().contains("Kontostand von 0"));
            assertEquals(Kontostatus.AKTIV, spar.getStatus());
        }

        @Test
        void aktionen_ohneAuswahl_tunNichts() {
            for (String id : List.of("sperrenKnopf", "entsperrenKnopf", "aufloesenKnopf")) {
                Node knopf = knopf(id);
                knopf.setDisable(false);
                ((Button) knopf).fire();   // ein unerwarteter Dialog ließe den Test scheitern
            }
            assertTrue(verwaltung.getKonten().stream().allMatch(k -> k.getStatus() == Kontostatus.AKTIV));
        }
    }
}
