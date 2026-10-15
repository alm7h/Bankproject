package bankprojekt.oberflaeche;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.UncheckedIOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testklasse für die Dialog- und Ladehilfen der Oberfläche ({@link Dialoge}).
 */
@ExtendWith(JavaFxErweiterung.class)
class DialogeTest {

    @Nested
    class LadenTests {
        @Test
        void laden_liefertWurzelUndController() {
            Dialoge.Geladen<KundeFormularController> geladen = Dialoge.laden("kundeFormular.fxml");
            assertNotNull(geladen.wurzel());
            assertInstanceOf(KundeFormularController.class, geladen.controller());
        }

        @Test
        void laden_unbekannteDatei_wirftIllegalArgumentException() {
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                    () -> Dialoge.laden("gibtsnicht.fxml"));
            assertTrue(e.getMessage().contains("gibtsnicht.fxml"));
        }

        @Test
        void laden_fehlerhafteDatei_wirftUncheckedIOException() {
            UncheckedIOException e = assertThrows(UncheckedIOException.class, () -> Dialoge.laden("kaputt.fxml"));
            assertTrue(e.getMessage().contains("kaputt.fxml"));
        }

        @Test
        void fenster_liefertDasFensterDesKnotens_oderNull() {
            Label ohneSzene = new Label();
            assertNull(Dialoge.fenster(ohneSzene));

            StackPane wurzel = new StackPane(new Label());
            Scene szene = new Scene(wurzel);
            assertNull(Dialoge.fenster(wurzel), "Szene ohne Fenster");

            Stage buehne = new Stage();
            buehne.setScene(szene);
            assertSame(buehne, Dialoge.fenster(wurzel));
        }
    }

    @Nested
    class BestaetigenTests {
        @Test
        void bestaetigen_mitAktionsknopf_liefertTrue() {
            Dialogantworten antworten = new Dialogantworten().klicke("Sperren");
            assertTrue(Dialoge.bestaetigen(null, "Konto sperren", "Wirklich sperren?", "Sperren"));

            DialogPane dialog = antworten.letzter();
            assertEquals("Konto sperren", dialog.getHeaderText());
            assertEquals("Wirklich sperren?", dialog.getContentText());
            assertTrue(dialog.getStylesheets().get(0).endsWith(Dialoge.STIL));
            antworten.allesBeantwortet();
        }

        @Test
        void bestaetigen_mitAbbrechen_liefertFalse_undAbbrechenIstVorausgewaehlt() {
            Dialogantworten antworten = new Dialogantworten().dann(d -> {
                assertTrue(Dialogantworten.knopf(d, "Abbrechen").isDefaultButton());
                assertFalse(Dialogantworten.knopf(d, "Auflösen").isDefaultButton());
                Dialogantworten.knopf(d, "Abbrechen").fire();
            });
            assertFalse(Dialoge.bestaetigen(null, "Konto auflösen", "Wirklich?", "Auflösen"));
            antworten.allesBeantwortet();
        }

        @Test
        void bestaetigen_ohneKnopfGeschlossen_liefertFalse() {
            new Dialogantworten().schliesse();
            assertFalse(Dialoge.bestaetigen(null, "Titel", "Frage", "Ja"));
        }
    }

    @Nested
    class FehlerTests {
        @Test
        void fehler_zeigtStandardueberschriftUndText() {
            Dialogantworten antworten = new Dialogantworten().klicke("OK");
            Dialoge.fehler(null, "Etwas ging schief.");
            assertEquals("Die Aktion konnte nicht ausgeführt werden", antworten.letzter().getHeaderText());
            assertEquals("Etwas ging schief.", antworten.letzter().getContentText());
        }

        @Test
        void fehler_mitEigenerUeberschrift() {
            Dialogantworten antworten = new Dialogantworten().klicke("OK");
            Dialoge.fehler(null, "Start nicht möglich", "Datei kaputt");
            assertEquals("Start nicht möglich", antworten.letzter().getHeaderText());
        }

        @Test
        void unerwarteterDialog_laesstDenTestScheitern() {
            // ohne Dialogantworten gilt die Vorgabe der Erweiterung
            AssertionError e = assertThrows(AssertionError.class, () -> Dialoge.fehler(null, "x"));
            assertTrue(e.getMessage().startsWith("Unerwarteter Dialog"));
        }
    }

    @Nested
    class FormularTests {
        private final Label fehlerfeld = new Label("alter Fehler");
        private int gespeichert;

        @Test
        void speichern_mitGueltigenDaten_fuehrtDieAktionAus_undLiefertTrue() {
            Dialogantworten antworten = new Dialogantworten().dann(d -> {
                assertEquals("", fehlerfeld.getText(), "Fehlerfeld wird beim Öffnen geleert");
                Button speichern = Dialogantworten.knopf(d, "Speichern");
                assertTrue(speichern.getStyleClass().contains("primaer"));
                Dialogantworten.knopf(d, "Abbrechen");
                speichern.fire();
            });
            assertTrue(Dialoge.formular(null, "Neuer Kunde", new StackPane(), fehlerfeld, () -> gespeichert++));
            assertEquals(1, gespeichert);
            assertEquals("Neuer Kunde", antworten.letzter().getHeaderText());
        }

        @Test
        void eingabefehler_haeltDenDialogOffen_undZeigtDieMeldung() {
            boolean[] gueltig = {false};
            Dialogantworten antworten = new Dialogantworten().dann(d -> {
                Dialogantworten.knopf(d, "Speichern").fire();
                assertEquals("Vorname ist ein Pflichtfeld.", fehlerfeld.getText());
                gueltig[0] = true;
                Dialogantworten.knopf(d, "Speichern").fire();
            });
            assertTrue(Dialoge.formular(null, "Neuer Kunde", new StackPane(), fehlerfeld, () -> {
                if (!gueltig[0])
                    throw new IllegalArgumentException("Vorname ist ein Pflichtfeld.");
                gespeichert++;
            }));
            assertEquals(1, gespeichert);
            antworten.allesBeantwortet();
        }

        @Test
        void unzulaessigeAktion_haeltDenDialogOffen() {
            new Dialogantworten().dann(d -> {
                Dialogantworten.knopf(d, "Speichern").fire();
                assertEquals("Kunde ist gesperrt", fehlerfeld.getText());
                Dialogantworten.knopf(d, "Abbrechen").fire();
            });
            assertFalse(Dialoge.formular(null, "Neues Konto", new StackPane(), fehlerfeld, () -> {
                throw new IllegalStateException("Kunde ist gesperrt");
            }));
        }

        @Test
        void eigeneKnopfbeschriftung() {
            new Dialogantworten().klicke("Genehmigen");
            assertTrue(Dialoge.formular(null, "Antrag", new StackPane(), fehlerfeld, "Genehmigen", () -> gespeichert++));
            assertEquals(1, gespeichert);
        }

        @Test
        void abbrechen_fuehrtDieAktionNichtAus() {
            new Dialogantworten().klicke("Abbrechen");
            assertFalse(Dialoge.formular(null, "Titel", new StackPane(), fehlerfeld, () -> gespeichert++));
            assertEquals(0, gespeichert);
        }

        @Test
        void schliessenOhneKnopf_liefertFalse() {
            new Dialogantworten().schliesse();
            assertFalse(Dialoge.formular(null, "Titel", new StackPane(), fehlerfeld, () -> gespeichert++));
        }

        @Test
        void speicherfehler_wirdGemeldet_undDerDialogSchliesst() {
            Dialogantworten antworten = new Dialogantworten().klicke("Speichern").klicke("OK");
            assertTrue(Dialoge.formular(null, "Titel", new StackPane(), fehlerfeld, () -> {
                throw new UncheckedIOException(new java.io.IOException("Platte voll"));
            }));
            assertEquals(2, antworten.gezeigt().size(), "Formular und Fehlermeldung");
            assertTrue(antworten.letzter().getContentText().contains("Platte voll"));
        }
    }

    @Test
    void anzeigen_zeigtInhaltMitSchliessenKnopf() {
        Label inhalt = new Label("Kontoauszug");
        Dialogantworten antworten = new Dialogantworten().klicke("Schließen");
        Dialoge.anzeigen(null, "Kontoauszug DE..", inhalt);
        assertSame(inhalt, antworten.letzter().getContent());
        assertEquals("Kontoauszug DE..", antworten.letzter().getHeaderText());
    }

    @Nested
    class AusfuehrenTests {
        @Test
        void ausfuehren_ohneFehler_liefertTrue() {
            int[] aufrufe = {0};
            assertTrue(Dialoge.ausfuehren(null, () -> aufrufe[0]++));
            assertEquals(1, aufrufe[0]);
        }

        @Test
        void ausfuehren_meldetFehler_undLiefertFalse() {
            Dialogantworten antworten = new Dialogantworten().klicke("OK").klicke("OK").klicke("OK");
            assertFalse(Dialoge.ausfuehren(null, () -> {
                throw new IllegalArgumentException("ungültig");
            }));
            assertFalse(Dialoge.ausfuehren(null, () -> {
                throw new IllegalStateException("nicht erlaubt");
            }));
            assertFalse(Dialoge.ausfuehren(null, () -> {
                throw new UncheckedIOException(new java.io.IOException("nicht gespeichert"));
            }));
            assertEquals("ungültig", antworten.gezeigt().get(0).getContentText());
            assertEquals("nicht erlaubt", antworten.gezeigt().get(1).getContentText());
            assertTrue(antworten.gezeigt().get(2).getContentText().contains("nicht gespeichert"));
        }
    }

    @Test
    void setAnzeige_ersetztDieAnzeige_undNullStelltDieStandardanzeigeWiederHer() {
        int[] angezeigt = {0};
        Dialoge.setAnzeige(dialog -> {
            angezeigt[0]++;
            return java.util.Optional.of(ButtonType.OK);
        });
        Dialoge.fehler(null, "x");
        assertEquals(1, angezeigt[0]);

        // null stellt showAndWait wieder her; danach sofort wieder eine Testanzeige,
        // damit kein Fenster aufgeht
        Dialoge.setAnzeige(null);
        Dialogantworten antworten = new Dialogantworten().klicke("OK");
        Dialoge.fehler(null, "y");
        assertEquals(1, angezeigt[0], "die alte Anzeige wird nicht mehr benutzt");
        antworten.allesBeantwortet();
    }
}
