package bankprojekt.oberflaeche;

import bankprojekt.basisdaten.Waehrung;
import bankprojekt.verwaltung.Bankverwaltung;
import bankprojekt.verwaltung.Benutzer;
import bankprojekt.verwaltung.Kontoart;
import bankprojekt.verwaltung.Kundenakte;
import bankprojekt.verwaltung.Kundenstatus;
import bankprojekt.verwaltung.Kundenzugang;
import bankprojekt.verwaltung.Testdaten;
import bankprojekt.verwaltung.VerstellbareUhr;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testklasse für die Kundenansicht ({@link KundenController}).
 */
@ExtendWith(JavaFxErweiterung.class)
class KundenControllerTest {

    private Bankverwaltung verwaltung;
    private Benutzer admin;
    private Kundenakte anna;
    private Kundenakte bert;
    private Parent ansicht;
    private KundenController controller;
    private TableView<Kundenakte> tabelle;

    @BeforeEach
    void setUp() {
        verwaltung = Testdaten.verwaltung(new VerstellbareUhr());
        admin = Testdaten.admin(verwaltung);
        anna = verwaltung.kundeAnlegen(admin, Testdaten.kunde("Anna", "Schmidt"));
        bert = verwaltung.kundeAnlegen(admin, Testdaten.kunde("Bert", "Müller"));
        verwaltung.kontoEroeffnen(admin, anna.getKundennummer(), Kontoart.SPARBUCH, Waehrung.EURO, null);
        verwaltung.kontoEroeffnen(admin, anna.getKundennummer(), Kontoart.FESTGELDKONTO, Waehrung.EURO, null);
        verwaltung.kundeDeaktivieren(admin, bert.getKundennummer());

        Dialoge.Geladen<KundenController> geladen = FxHilfe.laden("kunden.fxml");
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

    @SuppressWarnings("unchecked")
    private ComboBox<Kundenstatus> statusFilter() {
        return FxHilfe.suche(ansicht, "statusFilter");
    }

    /**
     * Füllt das Kundenformular eines Dialogs.
     * @param formular der Inhalt des Dialogs
     * @param vorname Vorname
     * @param nachname Nachname
     */
    private static void formularFuellen(Parent formular, String vorname, String nachname) {
        FxHilfe.tippe(formular, "vorname", vorname);
        FxHilfe.tippe(formular, "nachname", nachname);
        FxHilfe.tippe(formular, "geburtsdatum", "1.5.1990");
        FxHilfe.tippe(formular, "adresse", "Hauptstr. 1\n10115 Berlin");
        FxHilfe.tippe(formular, "telefon", "030 123456");
        FxHilfe.tippe(formular, "email", "carla@example.org");
    }

    @Nested
    class TabelleTests {
        @Test
        void zeigtAlleKundenMitIhrenDaten() {
            assertEquals(List.of(anna, bert), tabelle.getItems());
            assertEquals("K000001", FxHilfe.zelle(tabelle, "Kundennummer", anna));
            assertEquals("Schmidt, Anna", FxHilfe.zelle(tabelle, "Name", anna));
            assertEquals("01.05.1990", FxHilfe.zelle(tabelle, "Geburtsdatum", anna));
            assertEquals("anna@example.org", FxHilfe.zelle(tabelle, "E-Mail", anna));
            assertEquals("", FxHilfe.zelle(tabelle, "Telefon", anna));
            assertEquals(2, FxHilfe.zelle(tabelle, "Konten", anna));
            assertEquals(0, FxHilfe.zelle(tabelle, "Konten", bert));
            assertEquals("aktiv", FxHilfe.zelle(tabelle, "Status", anna));
            assertEquals("gesperrt", FxHilfe.zelle(tabelle, "Status", bert));
            assertEquals("2 Kunden", FxHilfe.text(ansicht, "anzahl"));
        }

        @Test
        void gesperrteKunden_sindAusgegraut() {
            long grau = ansicht.lookupAll(".table-row-cell").stream()
                    .filter(z -> z.getStyleClass().contains("inaktiv")).count();
            assertEquals(1, grau);
        }

        @Test
        void suche_undStatusfilter_wirkenSofort() {
            FxHilfe.tippe(ansicht, "suche", "schmi");
            assertEquals(List.of(anna), tabelle.getItems());
            assertEquals("1 von 2 Kunden", FxHilfe.text(ansicht, "anzahl"));
            FxHilfe.tippe(ansicht, "suche", "");

            statusFilter().setValue(Kundenstatus.GESPERRT);
            assertEquals(List.of(bert), tabelle.getItems());
            statusFilter().getSelectionModel().select(0);   // "Alle Status"
            assertNull(statusFilter().getValue());
            assertEquals(2, tabelle.getItems().size());
        }

        @Test
        void einzigerKunde_wirdImSingularGezaehlt() {
            FxHilfe.tippe(ansicht, "suche", "anna");
            assertEquals("1 von 2 Kunden", FxHilfe.text(ansicht, "anzahl"));
            Bankverwaltung eine = Testdaten.verwaltung(new VerstellbareUhr());
            eine.kundeAnlegen(Testdaten.admin(eine), Testdaten.kunde("Carla", "Klein"));
            controller.setSitzung(new Sitzung(eine, Testdaten.admin(eine)));
            FxHilfe.tippe(ansicht, "suche", "");
            assertEquals("1 Kunde", FxHilfe.text(ansicht, "anzahl"));
        }

        @Test
        void auswahl_bleibtNachDemAktualisierenErhalten() {
            tabelle.getSelectionModel().select(bert);
            controller.aktualisieren();
            assertSame(bert, tabelle.getSelectionModel().getSelectedItem());
        }

        @Test
        void auswahl_dieWegfiltertWird_istDanachLeer() {
            tabelle.getSelectionModel().select(bert);
            FxHilfe.tippe(ansicht, "suche", "anna");
            assertNull(tabelle.getSelectionModel().getSelectedItem());
            assertTrue(knopf("bearbeitenKnopf").isDisabled());
        }

        @Test
        void aktualisieren_ohneSitzung_tutNichts() {
            Dialoge.Geladen<KundenController> leer = FxHilfe.laden("kunden.fxml");
            leer.controller().aktualisieren();
            assertTrue(FxHilfe.<Kundenakte>tabelle(leer.wurzel()).getItems().isEmpty());
        }
    }

    @Nested
    class KnoepfeTests {
        @Test
        void ohneAuswahl_sindDieZeilenaktionenDeaktiviert() {
            assertTrue(knopf("bearbeitenKnopf").isDisabled());
            assertTrue(knopf("statusKnopf").isDisabled());
            assertTrue(knopf("kontenAnzeigenKnopf").isDisabled());
        }

        @Test
        void aktiverKunde_deaktivieren_inRot_gesperrterKunde_aktivieren() {
            controller.setKontenAnzeigen(k -> { });
            tabelle.getSelectionModel().select(anna);
            assertFalse(knopf("bearbeitenKnopf").isDisabled());
            assertFalse(knopf("kontenAnzeigenKnopf").isDisabled());
            assertEquals("Deaktivieren", knopf("statusKnopf").getText());
            assertTrue(knopf("statusKnopf").getStyleClass().contains("gefahr"));

            tabelle.getSelectionModel().select(bert);
            assertEquals("Aktivieren", knopf("statusKnopf").getText());
            assertFalse(knopf("statusKnopf").getStyleClass().contains("gefahr"));
        }

        @Test
        void kontenAnzeigen_ohneZiel_bleibtDeaktiviert() {
            tabelle.getSelectionModel().select(anna);
            assertTrue(knopf("kontenAnzeigenKnopf").isDisabled());
        }
    }

    @Nested
    class AnlegenUndBearbeitenTests {
        @Test
        void neuerKunde_mitFehlerImFormular_dannGueltig_wirdAngelegtUndAusgewaehlt() {
            FxHilfe.tippe(ansicht, "suche", "gibtsnicht");
            Dialogantworten antworten = new Dialogantworten().dann(d -> {
                Parent formular = Dialogantworten.inhalt(d);
                assertEquals("wird automatisch vergeben", FxHilfe.text(formular, "kundennummer"));
                Dialogantworten.knopf(d, "Speichern").fire();
                assertEquals("Vorname ist ein Pflichtfeld.", FxHilfe.text(formular, "fehler"));
                formularFuellen(formular, "Carla", "Klein");
                Dialogantworten.knopf(d, "Speichern").fire();
            });

            FxHilfe.knopf(ansicht, "Neuer Kunde").fire();

            antworten.allesBeantwortet();
            assertEquals("Neuer Kunde", antworten.letzter().getHeaderText());
            Kundenakte carla = verwaltung.findeKunde("K000003").orElseThrow();
            assertEquals("Klein, Carla", carla.getName());
            assertEquals("", FxHilfe.<javafx.scene.control.TextField>suche(ansicht, "suche").getText(),
                    "Suche zurückgesetzt, damit der neue Kunde sichtbar ist");
            assertSame(carla, tabelle.getSelectionModel().getSelectedItem());
        }

        @Test
        void neuerKunde_abgebrochen_legtNichtsAn() {
            new Dialogantworten().klicke("Abbrechen");
            FxHilfe.knopf(ansicht, "Neuer Kunde").fire();
            assertEquals(2, verwaltung.getKunden().size());
        }

        @Test
        void bearbeiten_zeigtDieDatenVor_undSpeichertAenderungen() {
            tabelle.getSelectionModel().select(anna);
            Dialogantworten antworten = new Dialogantworten().dann(d -> {
                Parent formular = Dialogantworten.inhalt(d);
                assertEquals("K000001", FxHilfe.text(formular, "kundennummer"));
                assertEquals("Schmidt", FxHilfe.<javafx.scene.control.TextField>suche(formular, "nachname").getText());
                FxHilfe.tippe(formular, "nachname", "Meier");
                Dialogantworten.knopf(d, "Speichern").fire();
            });
            knopf("bearbeitenKnopf").fire();
            assertEquals("Kunde K000001 bearbeiten", antworten.letzter().getHeaderText());
            assertEquals("Meier, Anna", anna.getName());
            assertEquals("Meier, Anna", FxHilfe.zelle(tabelle, "Name", anna));
        }

        @Test
        void bearbeiten_abgebrochen_aendertNichts() {
            tabelle.getSelectionModel().select(anna);
            new Dialogantworten().dann(d -> {
                FxHilfe.tippe(Dialogantworten.inhalt(d), "nachname", "Meier");
                Dialogantworten.knopf(d, "Abbrechen").fire();
            });
            knopf("bearbeitenKnopf").fire();
            assertEquals("Schmidt, Anna", anna.getName());
        }

        @Test
        void doppelklickAufEineZeile_oeffnetDasBearbeiten() {
            tabelle.getSelectionModel().select(anna);
            Dialogantworten antworten = new Dialogantworten().klicke("Abbrechen");
            Node zeile = ansicht.lookupAll(".table-row-cell").stream()
                    .filter(z -> ((TableRow<?>) z).getItem() == anna).findFirst().orElseThrow();
            zeile.fireEvent(new MouseEvent(MouseEvent.MOUSE_CLICKED, 1, 1, 1, 1, MouseButton.PRIMARY, 2,
                    false, false, false, false, true, false, false, false, false, true, null));
            antworten.allesBeantwortet();
        }

        @Test
        void bearbeiten_ohneAuswahl_oeffnetKeinFormular() {
            knopf("bearbeitenKnopf").setDisable(false);
            knopf("bearbeitenKnopf").fire();   // ein unerwarteter Dialog ließe den Test scheitern
            assertNull(tabelle.getSelectionModel().getSelectedItem());
        }
    }

    @Nested
    class StatusTests {
        @Test
        void deaktivieren_mitAbbrechen_aendertNichts() {
            tabelle.getSelectionModel().select(anna);
            Dialogantworten antworten = new Dialogantworten().klicke("Abbrechen");
            knopf("statusKnopf").fire();
            assertTrue(anna.isAktiv());
            assertEquals("Kunde deaktivieren", antworten.letzter().getHeaderText());
            assertTrue(antworten.letzter().getContentText().contains("K000001"));
        }

        @Test
        void deaktivieren_bestaetigt_sperrtDenKunden() {
            tabelle.getSelectionModel().select(anna);
            new Dialogantworten().klicke("Deaktivieren");
            knopf("statusKnopf").fire();
            assertFalse(anna.isAktiv());
            assertEquals("gesperrt", FxHilfe.zelle(tabelle, "Status", anna));
            assertEquals("Aktivieren", knopf("statusKnopf").getText());
        }

        @Test
        void aktivieren_ohneRueckfrage() {
            tabelle.getSelectionModel().select(bert);
            knopf("statusKnopf").fire();
            assertTrue(bert.isAktiv());
        }

        @Test
        void statusWechseln_ohneAuswahl_tutNichts() {
            knopf("statusKnopf").setDisable(false);
            knopf("statusKnopf").fire();
            assertTrue(anna.isAktiv());
            assertFalse(bert.isAktiv());
        }
    }

    @Nested
    class PortalzugangTests {
        @Test
        void spalteUndKnopf_zeigenDenStandDesPortalzugangs() {
            assertEquals("–", FxHilfe.zelle(tabelle, "Portal", anna));
            tabelle.getSelectionModel().select(anna);
            assertEquals("Portalzugang einrichten", knopf("portalKnopf").getText());
            Testdaten.kundenzugang(verwaltung, "anna.s", anna.getKundennummer());
            controller.aktualisieren();
            assertEquals("anna.s", FxHilfe.zelle(tabelle, "Portal", anna));
            assertEquals("Portalpasswort zurücksetzen", knopf("portalKnopf").getText());
        }

        @Test
        void portalzugangEinrichten_imDialog() {
            tabelle.getSelectionModel().select(anna);
            Dialogantworten antworten = new Dialogantworten().dann(d -> {
                Parent formular = Dialogantworten.inhalt(d);
                FxHilfe.tippe(formular, "benutzername", "admin");
                FxHilfe.tippe(formular, "passwort", "start1");
                FxHilfe.tippe(formular, "wiederholung", "start1");
                Dialogantworten.knopf(d, "Speichern").fire();
                assertTrue(FxHilfe.text(formular, "fehler").contains("bereits vergeben"));
                FxHilfe.tippe(formular, "benutzername", "anna.s");
                Dialogantworten.knopf(d, "Speichern").fire();
            });
            knopf("portalKnopf").fire();
            assertEquals("Portalzugang einrichten", antworten.letzter().getHeaderText());
            Kundenzugang zugang = verwaltung.getKundenzugangVon(anna.getKundennummer()).orElseThrow();
            assertTrue(zugang.mussPasswortAendern());
            assertEquals("anna.s", FxHilfe.zelle(tabelle, "Portal", anna));
        }

        @Test
        void portalpasswortZuruecksetzen_imDialog() {
            Kundenzugang zugang = Testdaten.kundenzugang(verwaltung, "anna.s", anna.getKundennummer());
            controller.aktualisieren();
            tabelle.getSelectionModel().select(anna);
            Dialogantworten antworten = new Dialogantworten().dann(d -> {
                Parent formular = Dialogantworten.inhalt(d);
                FxHilfe.tippe(formular, "passwort", "neu-123");
                FxHilfe.tippe(formular, "wiederholung", "neu-123");
                Dialogantworten.knopf(d, "Speichern").fire();
            });
            knopf("portalKnopf").fire();
            assertEquals("Portalpasswort zurücksetzen", antworten.letzter().getHeaderText());
            assertTrue(zugang.mussPasswortAendern());
        }

        @Test
        void portalzugang_abgebrochen_oderOhneAuswahl() {
            tabelle.getSelectionModel().select(anna);
            new Dialogantworten().klicke("Abbrechen");
            knopf("portalKnopf").fire();
            assertTrue(verwaltung.getKundenzugangVon(anna.getKundennummer()).isEmpty());
            tabelle.getSelectionModel().clearSelection();
            knopf("portalKnopf").setDisable(false);
            knopf("portalKnopf").fire();
        }
    }

    @Nested
    class KontenAnzeigenTests {
        @Test
        void kontenAnzeigen_gibtDenAusgewaehltenKundenWeiter() {
            List<Kundenakte> gezeigt = new ArrayList<>();
            controller.setKontenAnzeigen(gezeigt::add);
            tabelle.getSelectionModel().select(anna);
            knopf("kontenAnzeigenKnopf").fire();
            assertEquals(List.of(anna), gezeigt);
        }

        @Test
        void kontenAnzeigen_ohneAuswahlOderZiel_tutNichts() {
            knopf("kontenAnzeigenKnopf").setDisable(false);
            knopf("kontenAnzeigenKnopf").fire();   // weder Auswahl noch Ziel

            List<Kundenakte> gezeigt = new ArrayList<>();
            controller.setKontenAnzeigen(gezeigt::add);
            knopf("kontenAnzeigenKnopf").setDisable(false);
            knopf("kontenAnzeigenKnopf").fire();   // Ziel, aber keine Auswahl
            assertTrue(gezeigt.isEmpty());

            controller.setKontenAnzeigen(null);
            tabelle.getSelectionModel().select(anna);
            knopf("kontenAnzeigenKnopf").setDisable(false);
            knopf("kontenAnzeigenKnopf").fire();   // Auswahl, aber kein Ziel
            assertTrue(gezeigt.isEmpty());
        }
    }
}
