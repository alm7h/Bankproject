package bankprojekt.oberflaeche;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseButton;

/**
 * Einheitliche Darstellung von Datum, Zeitpunkt und Status in der Oberfläche.
 * @author m.alharethi
 */
final class Darstellung {
    /**
     * Datumsformat der Oberfläche, unabhängig vom eingestellten Locale
     */
    static final DateTimeFormatter DATUM = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    /**
     * Format für Zeitpunkte (Protokoll, letzte Anmeldung)
     */
    static final DateTimeFormatter ZEITPUNKT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
    /**
     * Format für eingegebene Daten: Tag und Monat ein- oder zweistellig, streng
     * geprüft (kein 31.02.)
     */
    private static final DateTimeFormatter EINGABE_DATUM =
            DateTimeFormatter.ofPattern("d.M.uuuu").withResolverStyle(ResolverStyle.STRICT);
    /**
     * Eingegebener Betrag: bis zu 12 Ziffern, optional Komma oder Punkt mit 1–2 Nachkommastellen
     */
    private static final Pattern BETRAG = Pattern.compile("^\\d{1,12}([.,]\\d{1,2})?$");

    /**
     * Nicht instanziierbar: Die Klasse enthält nur statische Methoden.
     */
    private Darstellung() {
    }

    /**
     * Formatiert ein Datum.
     * @param datum das Datum, darf null sein
     * @return z. B. "14.10.2026", "–" für null
     */
    static String datum(LocalDate datum) {
        return datum == null ? "–" : DATUM.format(datum);
    }

    /**
     * Formatiert einen Zeitpunkt.
     * @param zeitpunkt der Zeitpunkt, darf null sein
     * @return z. B. "14.10.2026 10:15:30", "–" für null
     */
    static String zeitpunkt(LocalDateTime zeitpunkt) {
        return zeitpunkt == null ? "–" : ZEITPUNKT.format(zeitpunkt);
    }

    /**
     * Liest ein eingegebenes Datum im Format TT.MM.JJJJ (auch T.M.JJJJ). Unmögliche
     * Daten wie der 31.02. werden abgelehnt.
     * @param text der eingegebene Text
     * @return das Datum oder null, wenn nichts eingegeben wurde
     * @throws IllegalArgumentException wenn der Text kein gültiges Datum ist
     */
    static LocalDate datumLesen(String text) {
        if (text == null || text.isBlank())
            return null;
        try {
            return LocalDate.parse(text.strip(), EINGABE_DATUM);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Das Datum \"" + text.strip()
                    + "\" ist ungültig. Bitte im Format TT.MM.JJJJ eingeben.", e);
        }
    }

    /**
     * Liest einen eingegebenen Betrag: Ziffern, optional mit Komma oder Punkt und
     * höchstens zwei Nachkommastellen (z. B. "500", "500,00", "99.5"). Tausenderpunkte
     * werden bewusst nicht akzeptiert, weil "1.000" sonst mehrdeutig wäre.
     * @param text der eingegebene Text
     * @param feldname Name des Feldes für die Fehlermeldung
     * @return der Betrag
     * @throws IllegalArgumentException wenn nichts oder kein gültiger Betrag eingegeben wurde
     */
    static double betragLesen(String text, String feldname) {
        if (text == null || text.isBlank())
            throw new IllegalArgumentException(feldname + " ist ein Pflichtfeld.");
        String betrag = text.strip();
        if (!BETRAG.matcher(betrag).matches())
            throw new IllegalArgumentException(feldname + " \"" + betrag
                    + "\" ist ungültig. Bitte einen Betrag wie 500 oder 500,00 eingeben.");
        return Double.parseDouble(betrag.replace(',', '.'));
    }

    /**
     * Erzeugt eine Tabellenzelle, die einen Status als farbige Markierung zeigt.
     * Die CSS-Klasse ergibt sich aus dem Text, z. B. "status-aufgeloest" für "aufgelöst".
     * @param <S> Typ der Tabellenzeilen
     * @return die Zelle
     */
    static <S> TableCell<S, String> statusZelle() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean leer) {
                super.updateItem(status, leer);
                if (leer || status == null) {
                    setGraphic(null);
                } else {
                    Label markierung = new Label(status);
                    markierung.getStyleClass().addAll("status", "status-" + cssName(status));
                    setGraphic(markierung);
                }
                setText(null);
            }
        };
    }

    /**
     * Macht aus einem Statustext einen CSS-Klassennamen (Kleinbuchstaben, ohne Umlaute).
     * @param text der Statustext
     * @return z. B. "aufgeloest" für "aufgelöst"
     */
    static String cssName(String text) {
        return text.toLowerCase(Locale.ROOT)
                .replace("ä", "ae").replace("ö", "oe").replace("ü", "ue").replace("ß", "ss")
                .replaceAll("[^a-z0-9-]", "");
    }

    /**
     * Richtet die Zeilen einer Tabelle ein: Zeilen, auf die eine Bedingung zutrifft
     * (z. B. gesperrte Kunden), werden ausgegraut, und ein Doppelklick auf eine
     * gefüllte Zeile löst eine Aktion aus (z. B. Bearbeiten).
     * @param tabelle die Tabelle
     * @param inaktiv die Bedingung für ausgegraute Zeilen
     * @param doppelklick Aktion für den Doppelklick, bekommt die Zeile; darf null sein
     * @param <S> Typ der Tabellenzeilen
     */
    static <S> void zeilenEinrichten(TableView<S> tabelle, Predicate<S> inaktiv, Consumer<S> doppelklick) {
        tabelle.setRowFactory(t -> {
            TableRow<S> zeile = new TableRow<>() {
                @Override
                protected void updateItem(S eintrag, boolean leer) {
                    super.updateItem(eintrag, leer);
                    getStyleClass().remove("inaktiv");
                    if (!leer && eintrag != null && inaktiv.test(eintrag))
                        getStyleClass().add("inaktiv");
                }
            };
            if (doppelklick != null) {
                zeile.setOnMouseClicked(e -> {
                    if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2 && !zeile.isEmpty())
                        doppelklick.accept(zeile.getItem());
                });
            }
            return zeile;
        });
    }

    /**
     * Erzeugt eine Listenzelle für Auswahlfelder, die für "keine Auswahl" (null)
     * einen Text wie "Alle Kunden" zeigt.
     * @param leertext Text für null
     * @param <T> Typ der Einträge
     * @return die Zelle
     */
    static <T> ListCell<T> auswahlZelle(String leertext) {
        return new ListCell<>() {
            @Override
            protected void updateItem(T eintrag, boolean leer) {
                super.updateItem(eintrag, leer);
                setText(leer || eintrag == null ? leertext : eintrag.toString());
            }
        };
    }
}
