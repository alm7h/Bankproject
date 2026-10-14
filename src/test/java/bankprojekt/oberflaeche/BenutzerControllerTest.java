package bankprojekt.oberflaeche;

import bankprojekt.verwaltung.Anmeldung;
import bankprojekt.verwaltung.Bankverwaltung;
import bankprojekt.verwaltung.Benutzer;
import bankprojekt.verwaltung.Testdaten;
import bankprojekt.verwaltung.VerstellbareUhr;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testklasse für die Benutzerverwaltung ({@link BenutzerController}).
 */
@ExtendWith(JavaFxErweiterung.class)
class BenutzerControllerTest {

    private Bankverwaltung verwaltung;
    private Benutzer admin;
    private Benutzer bert;
    private Parent ansicht;
    private BenutzerController controller;
    private TableView<Benutzer> tabelle;

    @BeforeEach
    void setUp() {
        verwaltung = Testdaten.verwaltung(new VerstellbareUhr());
        admin = Testdaten.admin(verwaltung);
        bert = verwaltung.benutzerAnlegen(admin, "bert", "Bert Beispiel", "abcd".toCharArray());
        new Anmeldung(verwaltung).anmelden("admin", Testdaten.PASSWORT.toCharArray());

        Dialoge.Geladen<BenutzerController> geladen = FxHilfe.laden("benutzer.fxml");
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

    /**
     * Füllt die Passwortfelder eines Benutzerformulars.
     * @param formular der Inhalt des Dialogs
     * @param passwort das Passwort
     * @param wiederholung die Wiederholung
     */
    private static void passwoerter(Parent formular, String passwort, String wiederholung) {
        FxHilfe.tippe(formular, "passwort", passwort);
        FxHilfe.tippe(formular, "wiederholung", wiederholung);
    }

    @Nested
    class TabelleTests {
        @Test
        void zeigtAlleBenutzer_undMarkiertDenAngemeldeten() {
            assertEquals(List.of(admin, bert), tabelle.getItems());
            assertEquals("admin  (Sie)", FxHilfe.zelle(tabelle, "Benutzername", admin));
            assertEquals("bert", FxHilfe.zelle(tabelle, "Benutzername", bert));
            assertEquals("Bert Beispiel", FxHilfe.zelle(tabelle, "Anzeigename", bert));
            assertEquals("aktiv", FxHilfe.zelle(tabelle, "Status", bert));
            assertEquals("14.10.2026 10:00:00", FxHilfe.zelle(tabelle, "Angelegt am", bert));
            assertEquals("14.10.2026 10:00:00", FxHilfe.zelle(tabelle, "Letzte Anmeldung", admin));
            assertEquals("–", FxHilfe.zelle(tabelle, "Letzte Anmeldung", bert));
            assertEquals("2 Benutzer, davon 2 aktiv", FxHilfe.text(ansicht, "anzahl"));
        }

        @Test
        void deaktivierteBenutzer_sindAusgegraut_undGezaehlt() {
            verwaltung.benutzerDeaktivieren(admin, "bert");
            controller.aktualisieren();
            FxHilfe.auslegen(ansicht);
            assertEquals("deaktiviert", FxHilfe.zelle(tabelle, "Status", bert));
            assertEquals("2 Benutzer, davon 1 aktiv", FxHilfe.text(ansicht, "anzahl"));
            assertEquals(1, ansicht.lookupAll(".table-row-cell").stream()
                    .filter(z -> z.getStyleClass().contains("inaktiv")).count());
        }

        @Test
        void aktualisieren_behaeltDieAuswahl_undOhneSitzungPassiertNichts() {
            tabelle.getSelectionModel().select(bert);
            controller.aktualisieren();
            assertSame(bert, tabelle.getSelectionModel().getSelectedItem());

            Dialoge.Geladen<BenutzerController> leer = FxHilfe.laden("benutzer.fxml");
            leer.controller().aktualisieren();
            assertTrue(FxHilfe.<Benutzer>tabelle(leer.wurzel()).getItems().isEmpty());
        }
    }

    @Nested
    class KnoepfeTests {
        @Test
        void ohneAuswahl_allesDeaktiviert() {
            assertTrue(knopf("bearbeitenKnopf").isDisabled());
            assertTrue(knopf("passwortKnopf").isDisabled());
            assertTrue(knopf("statusKnopf").isDisabled());
        }

        @Test
        void sichSelbst_kannManNichtDeaktivieren() {
            tabelle.getSelectionModel().select(admin);
            assertFalse(knopf("passwortKnopf").isDisabled());
            assertTrue(knopf("statusKnopf").isDisabled());
            assertEquals("Sie können sich nicht selbst deaktivieren.", knopf("statusKnopf").getTooltip().getText());

            tabelle.getSelectionModel().select(bert);
            assertFalse(knopf("statusKnopf").isDisabled());
            assertNull(knopf("statusKnopf").getTooltip());
            assertEquals("Deaktivieren", knopf("statusKnopf").getText());
            assertTrue(knopf("statusKnopf").getStyleClass().contains("gefahr"));
        }
    }

    @Nested
    class DialogTests {
        @Test
        void neuerBenutzer_mitAbweichenderWiederholung_dannGueltig() {
            Dialogantworten antworten = new Dialogantworten().dann(d -> {
                Parent formular = Dialogantworten.inhalt(d);
                FxHilfe.tippe(formular, "benutzername", "Erika.M");
                FxHilfe.tippe(formular, "anzeigename", "Erika Mustermann");
                passwoerter(formular, "geheim1", "geheim2");
                Dialogantworten.knopf(d, "Speichern").fire();
                assertEquals("Die beiden Passwörter stimmen nicht überein.", FxHilfe.text(formular, "fehler"));
                passwoerter(formular, "geheim1", "geheim1");
                Dialogantworten.knopf(d, "Speichern").fire();
            });

            FxHilfe.knopf(ansicht, "Neuer Benutzer").fire();

            assertEquals("Neuer Benutzer", antworten.letzter().getHeaderText());
            Benutzer erika = verwaltung.findeBenutzer("erika.m").orElseThrow();
            assertSame(erika, tabelle.getSelectionModel().getSelectedItem());
            assertTrue(new Anmeldung(verwaltung).anmelden("erika.m", "geheim1".toCharArray()).isErfolgreich());
        }

        @Test
        void neuerBenutzer_vergebenerName_bleibtImFormular_abbrechenLegtNichtsAn() {
            new Dialogantworten().dann(d -> {
                Parent formular = Dialogantworten.inhalt(d);
                FxHilfe.tippe(formular, "benutzername", "bert");
                FxHilfe.tippe(formular, "anzeigename", "Noch ein Bert");
                passwoerter(formular, "abcd", "abcd");
                Dialogantworten.knopf(d, "Speichern").fire();
                assertTrue(FxHilfe.text(formular, "fehler").contains("bereits vergeben"));
                Dialogantworten.knopf(d, "Abbrechen").fire();
            });
            FxHilfe.knopf(ansicht, "Neuer Benutzer").fire();
            assertEquals(2, verwaltung.getBenutzer().size());
        }

        @Test
        void bearbeiten_aendertDenAnzeigenamen() {
            tabelle.getSelectionModel().select(bert);
            Dialogantworten antworten = new Dialogantworten().dann(d -> {
                Parent formular = Dialogantworten.inhalt(d);
                assertTrue(FxHilfe.suche(formular, "benutzername").isDisabled());
                FxHilfe.tippe(formular, "anzeigename", "Bert B.");
                Dialogantworten.knopf(d, "Speichern").fire();
            });
            knopf("bearbeitenKnopf").fire();
            assertEquals("Benutzer bert bearbeiten", antworten.letzter().getHeaderText());
            assertEquals("Bert B.", bert.getAnzeigename());
            assertEquals("Bert B.", FxHilfe.zelle(tabelle, "Anzeigename", bert));
        }

        @Test
        void bearbeiten_abgebrochen_undPerDoppelklick() {
            tabelle.getSelectionModel().select(bert);
            Dialogantworten antworten = new Dialogantworten().klicke("Abbrechen");
            Node zeile = ansicht.lookupAll(".table-row-cell").stream()
                    .filter(z -> ((TableRow<?>) z).getItem() == bert).findFirst().orElseThrow();
            zeile.fireEvent(new MouseEvent(MouseEvent.MOUSE_CLICKED, 1, 1, 1, 1, MouseButton.PRIMARY, 2,
                    false, false, false, false, true, false, false, false, false, true, null));
            antworten.allesBeantwortet();
            assertEquals("Bert Beispiel", bert.getAnzeigename());
        }

        @Test
        void passwortZuruecksetzen_mitZuKurzemPasswort_dannGueltig() {
            tabelle.getSelectionModel().select(bert);
            Dialogantworten antworten = new Dialogantworten().dann(d -> {
                Parent formular = Dialogantworten.inhalt(d);
                passwoerter(formular, "abc", "abc");
                Dialogantworten.knopf(d, "Speichern").fire();
                assertTrue(FxHilfe.text(formular, "fehler").contains("mindestens 4 Zeichen"));
                passwoerter(formular, "neu-1234", "neu-1234");
                Dialogantworten.knopf(d, "Speichern").fire();
            });
            knopf("passwortKnopf").fire();
            assertEquals("Passwort von bert zurücksetzen", antworten.letzter().getHeaderText());
            Anmeldung anmeldung = new Anmeldung(verwaltung);
            assertTrue(anmeldung.anmelden("bert", "neu-1234".toCharArray()).isErfolgreich());
            assertFalse(anmeldung.anmelden("bert", "abcd".toCharArray()).isErfolgreich());
        }

        @Test
        void passwortZuruecksetzen_abgebrochen() {
            tabelle.getSelectionModel().select(bert);
            new Dialogantworten().klicke("Abbrechen");
            knopf("passwortKnopf").fire();
            assertTrue(new Anmeldung(verwaltung).anmelden("bert", "abcd".toCharArray()).isErfolgreich());
        }

        @Test
        void deaktivieren_nurNachBestaetigung_aktivierenOhne() {
            tabelle.getSelectionModel().select(bert);
            Dialogantworten antworten = new Dialogantworten().klicke("Abbrechen").klicke("Deaktivieren");
            knopf("statusKnopf").fire();
            assertTrue(bert.isAktiv());
            assertEquals("Benutzer deaktivieren", antworten.letzter().getHeaderText());
            knopf("statusKnopf").fire();
            assertFalse(bert.isAktiv());
            assertEquals("Aktivieren", knopf("statusKnopf").getText());

            knopf("statusKnopf").fire();
            assertTrue(bert.isAktiv());
        }

        @Test
        void aktionen_ohneAuswahl_tunNichts() {
            for (String id : List.of("bearbeitenKnopf", "passwortKnopf", "statusKnopf")) {
                knopf(id).setDisable(false);
                knopf(id).fire();   // ein unerwarteter Dialog ließe den Test scheitern
            }
            assertTrue(bert.isAktiv());
        }
    }
}
