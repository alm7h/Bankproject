package bankprojekt.oberflaeche;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testklasse für die Darstellungshilfen der Oberfläche ({@link Darstellung}).
 */
@ExtendWith(JavaFxErweiterung.class)
class DarstellungTest {

    @Nested
    class FormatierenTests {
        @Test
        void datum_undZeitpunkt_formatieren_undNullAlsStrich() {
            assertEquals("01.05.1990", Darstellung.datum(LocalDate.of(1990, 5, 1)));
            assertEquals("–", Darstellung.datum(null));
            assertEquals("14.10.2026 09:05:03", Darstellung.zeitpunkt(LocalDateTime.of(2026, 10, 14, 9, 5, 3)));
            assertEquals("–", Darstellung.zeitpunkt(null));
        }

        @Test
        void cssName_ersetztUmlaute_undEntferntSonderzeichen() {
            assertEquals("aufgeloest", Darstellung.cssName("aufgelöst"));
            assertEquals("aeoeuess", Darstellung.cssName("Ä Ö Ü ß"));
            assertEquals("aktiv", Darstellung.cssName("Aktiv!"));
        }
    }

    @Nested
    class DatumLesenTests {
        @Test
        void leereEingabe_liefertNull() {
            assertNull(Darstellung.datumLesen(null));
            assertNull(Darstellung.datumLesen("   "));
        }

        @Test
        void gueltigeDaten_einOderZweistellig() {
            assertEquals(LocalDate.of(1990, 5, 1), Darstellung.datumLesen("01.05.1990"));
            assertEquals(LocalDate.of(1990, 5, 1), Darstellung.datumLesen(" 1.5.1990 "));
            assertEquals(LocalDate.of(2024, 2, 29), Darstellung.datumLesen("29.02.2024"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"31.02.1990", "29.02.2023", "1990-05-01", "01.13.1990", "abc", "01.05.90"})
        void ungueltigeDaten_werfenMitVerstaendlicherMeldung(String eingabe) {
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                    () -> Darstellung.datumLesen(eingabe));
            assertTrue(e.getMessage().contains(eingabe) && e.getMessage().contains("TT.MM.JJJJ"), e.getMessage());
        }
    }

    @Nested
    class BetragLesenTests {
        @Test
        void gueltigeBetraege_mitKommaOderPunkt() {
            assertEquals(500.0, Darstellung.betragLesen("500", "Dispo"));
            assertEquals(500.5, Darstellung.betragLesen("500,5", "Dispo"));
            assertEquals(99.95, Darstellung.betragLesen(" 99.95 ", "Dispo"));
        }

        @Test
        void leererBetrag_istPflicht() {
            assertTrue(assertThrows(IllegalArgumentException.class, () -> Darstellung.betragLesen(null, "Dispo"))
                    .getMessage().equals("Dispo ist ein Pflichtfeld."));
            assertThrows(IllegalArgumentException.class, () -> Darstellung.betragLesen(" ", "Dispo"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"1.000", "1,234", "-5", "abc", "5,", ",5", "1 000", "1234567890123"})
        void ungueltigeBetraege_werden_abgelehnt(String eingabe) {
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                    () -> Darstellung.betragLesen(eingabe, "Dispo"));
            assertTrue(e.getMessage().startsWith("Dispo \""), e.getMessage());
        }
    }

    @Nested
    class TabellenTests {
        /**
         * Baut eine ausgelegte Tabelle mit einer Statusspalte.
         * @param eintraege die Zeilen
         * @return die Tabelle
         */
        private TableView<String> tabelle(String... eintraege) {
            TableView<String> tabelle = new TableView<>(FXCollections.observableArrayList(eintraege));
            TableColumn<String, String> spalte = new TableColumn<>("Status");
            spalte.setCellValueFactory(z -> new ReadOnlyStringWrapper(z.getValue()));
            spalte.setCellFactory(s -> Darstellung.statusZelle());
            tabelle.getColumns().add(spalte);
            new Scene(tabelle, 400, 300);
            FxHilfe.auslegen(tabelle);
            return tabelle;
        }

        /**
         * Liefert die Tabellenzeilen mit Inhalt.
         * @param tabelle die Tabelle
         * @return die gefüllten Zeilen
         */
        @SuppressWarnings("unchecked")
        private List<TableRow<String>> zeilen(TableView<String> tabelle) {
            List<TableRow<String>> zeilen = new ArrayList<>();
            for (Node n : tabelle.lookupAll(".table-row-cell"))
                zeilen.add((TableRow<String>) n);
            return zeilen;
        }

        @Test
        void statusZelle_zeigtFarbigeMarkierung_undLeereZellenNichts() {
            TableView<String> tabelle = tabelle("aktiv", "aufgelöst", null);
            List<Label> markierungen = tabelle.lookupAll(".status").stream().map(n -> (Label) n).toList();
            assertEquals(2, markierungen.size(), "nur für Zeilen mit Status");
            assertTrue(markierungen.stream().anyMatch(l -> l.getText().equals("aktiv")
                    && l.getStyleClass().contains("status-aktiv")));
            assertTrue(markierungen.stream().anyMatch(l -> l.getText().equals("aufgelöst")
                    && l.getStyleClass().contains("status-aufgeloest")));
        }

        @Test
        void zeilenEinrichten_grautInaktiveZeilenAus() {
            TableView<String> tabelle = tabelle("aktiv", "gesperrt", null);
            Darstellung.zeilenEinrichten(tabelle, "gesperrt"::equals, null);
            tabelle.refresh();
            FxHilfe.auslegen(tabelle);
            long grau = zeilen(tabelle).stream().filter(z -> z.getStyleClass().contains("inaktiv")).count();
            assertEquals(1, grau);

            // ändert sich der Inhalt, verschwindet die Markierung wieder
            tabelle.getItems().set(1, "aktiv");
            tabelle.refresh();
            FxHilfe.auslegen(tabelle);
            assertTrue(zeilen(tabelle).stream().noneMatch(z -> z.getStyleClass().contains("inaktiv")));
            assertTrue(zeilen(tabelle).stream().allMatch(z -> z.getOnMouseClicked() == null),
                    "ohne Doppelklick-Aktion kein Klick-Handler");
        }

        @Test
        void zeilenEinrichten_doppelklickAufGefuellteZeile_loestDieAktionAus() {
            TableView<String> tabelle = tabelle("eins", "zwei");
            List<String> geoeffnet = new ArrayList<>();
            Darstellung.zeilenEinrichten(tabelle, s -> false, geoeffnet::add);
            tabelle.refresh();
            FxHilfe.auslegen(tabelle);
            TableRow<String> zweite = zeilen(tabelle).stream().filter(z -> "zwei".equals(z.getItem()))
                    .findFirst().orElseThrow();
            TableRow<String> leer = zeilen(tabelle).stream().filter(TableRow::isEmpty).findFirst().orElseThrow();

            klicke(zweite, MouseButton.PRIMARY, 1);
            klicke(zweite, MouseButton.SECONDARY, 2);
            klicke(leer, MouseButton.PRIMARY, 2);
            assertEquals(List.of(), geoeffnet, "Einfachklick, rechte Taste, leere Zeile: nichts");

            klicke(zweite, MouseButton.PRIMARY, 2);
            assertEquals(List.of("zwei"), geoeffnet);
        }

        /**
         * Schickt einen Mausklick an einen Knoten.
         * @param ziel der Knoten
         * @param taste die Maustaste
         * @param anzahl Anzahl der Klicks
         */
        private void klicke(Node ziel, MouseButton taste, int anzahl) {
            ziel.fireEvent(new MouseEvent(MouseEvent.MOUSE_CLICKED, 1, 1, 1, 1, taste, anzahl,
                    false, false, false, false, taste == MouseButton.PRIMARY, false, taste == MouseButton.SECONDARY,
                    false, false, true, null));
        }
    }

    @Test
    void auswahlZelle_zeigtFuerNullDenLeertext() {
        ListView<String> liste = new ListView<>(FXCollections.observableArrayList(Arrays.asList(null, "aktiv")));
        ListCell<String> zelle = Darstellung.auswahlZelle("Alle Status");
        zelle.updateListView(liste);
        zelle.updateIndex(0);
        assertEquals("Alle Status", zelle.getText());
        zelle.updateIndex(1);
        assertEquals("aktiv", zelle.getText());
        zelle.updateIndex(7);   // jenseits der Liste: leere Zelle
        assertTrue(zelle.isEmpty());
        assertEquals("Alle Status", zelle.getText());
    }
}
