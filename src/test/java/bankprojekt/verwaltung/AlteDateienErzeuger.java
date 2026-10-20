package bankprojekt.verwaltung;

import bankprojekt.basisdaten.Festgeldkonto;
import bankprojekt.basisdaten.Geldbetrag;
import bankprojekt.basisdaten.Waehrung;
import bankprojekt.exceptions.GesperrtException;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Erzeugt die Testdateien im alten Speicherformat, in dem {@link Geldbetrag} seinen
 * Betrag noch als {@code double} gespeichert hat. Mit ihnen prüfen {@code GeldbetragTest}
 * und {@link DatenspeicherTest}, dass solche Dateien auch nach der Umstellung auf
 * {@code BigDecimal} unverändert gelesen werden.
 * <p>
 * Die Klasse ist kein Test und wird vom Build nicht ausgeführt (ihr Name passt zu keinem
 * Testmuster). Sie muss auf Commit {@value #COMMIT} laufen, der letzten Version mit dem
 * double-Feld; in einer neueren Version bricht sie ab, statt Dateien im neuen Format zu
 * schreiben. Neu erzeugen, im Wurzelverzeichnis des Repositorys:
 * <pre>
 * REPO=$(pwd); ALT=/tmp/bankprojekt-d7ba47f
 * git worktree add "$ALT" d7ba47f
 * cp src/test/java/bankprojekt/verwaltung/AlteDateienErzeuger.java "$ALT/src/test/java/bankprojekt/verwaltung/"
 * cd "$ALT"
 * ./mvnw -q test-compile exec:java -Dexec.classpathScope=test \
 *     -Dexec.mainClass=bankprojekt.verwaltung.AlteDateienErzeuger -Dexec.args="$REPO/src/test/resources"
 * rm src/test/java/bankprojekt/verwaltung/AlteDateienErzeuger.java
 * cd "$REPO"
 * git worktree remove "$ALT"
 * </pre>
 * Der Pfad in {@code exec.args} darf keine Leerzeichen enthalten. Die Passwort-Hashes
 * enthalten ein zufälliges Salz, daher unterscheiden sich neu erzeugte Dateien byteweise
 * von den alten; die gespeicherten Beträge bleiben gleich.
 *
 * @author m.alharethi
 */
public final class AlteDateienErzeuger {
    /** Commit der letzten Version, die Beträge als double speichert */
    static final String COMMIT = "d7ba47f";

    /** Datei mit einzelnen Geldbeträgen, relativ zum Ressourcenverzeichnis */
    private static final String GELDBETRAEGE = "bankprojekt/basisdaten/geldbetraege-double.ser";

    /** Datei mit einer ganzen Verwaltung, relativ zum Ressourcenverzeichnis */
    private static final String VERWALTUNG = "bankprojekt/verwaltung/verwaltung-double.ser";

    /**
     * Nur statische Methoden, keine Objekte.
     */
    private AlteDateienErzeuger() {
    }

    /**
     * Schreibt beide Testdateien.
     * @param args optional das Ressourcenverzeichnis, in das geschrieben wird
     *             (Standard: {@code src/test/resources})
     * @throws IOException wenn nicht geschrieben werden kann
     * @throws GesperrtException wenn eine Buchung wider Erwarten abgelehnt wird
     * @throws IllegalStateException wenn Geldbetrag den Betrag nicht mehr als double speichert
     */
    public static void main(String[] args) throws IOException, GesperrtException {
        doubleVersionPruefen();
        Path ressourcen = Path.of(args.length > 0 ? args[0] : "src/test/resources");
        geldbetraegeSchreiben(ressourcen.resolve(GELDBETRAEGE));
        new Datenspeicher(ressourcen.resolve(VERWALTUNG)).speichern(verwaltung());
    }

    /**
     * Bricht ab, wenn Geldbetrag seinen Betrag nicht mehr als double speichert – dann
     * würden die Dateien im neuen Format geschrieben.
     * @throws IllegalStateException wenn das Feld betrag kein double ist
     */
    private static void doubleVersionPruefen() {
        try {
            if (Geldbetrag.class.getDeclaredField("betrag").getType() == double.class)
                return;
        } catch (NoSuchFieldException e) {
            // ohne Feld betrag ist es ebenfalls nicht die alte Version
        }
        throw new IllegalStateException("Geldbetrag speichert den Betrag nicht mehr als double. "
                + "Die Testdateien im alten Format lassen sich nur auf Commit " + COMMIT + " erzeugen.");
    }

    /**
     * Schreibt eine Liste von Geldbeträgen in allen Währungen, darunter ein negativer Betrag,
     * einer mit drei Nachkommastellen und ein ungerundetes double. {@code GeldbetragTest}
     * erwartet genau diese Werte in dieser Reihenfolge.
     * @param datei die Zieldatei
     * @throws IOException wenn nicht geschrieben werden kann
     */
    private static void geldbetraegeSchreiben(Path datei) throws IOException {
        List<Geldbetrag> betraege = new ArrayList<>(List.of(
                Geldbetrag.NULL_EURO,
                new Geldbetrag(12.34),
                new Geldbetrag(-5),
                new Geldbetrag(61.5, Waehrung.DENAR),
                new Geldbetrag(491.97, Waehrung.FRANC),
                new Geldbetrag(24.5, Waehrung.DOBRA),
                new Geldbetrag(1234.567, Waehrung.DENAR),
                new Geldbetrag(0.1 + 0.2),          // 0.30000000000000004
                new Geldbetrag(9_999_999.99)));
        Files.createDirectories(datei.getParent());
        try (ObjectOutputStream aus = new ObjectOutputStream(Files.newOutputStream(datei))) {
            aus.writeObject(betraege);
        }
    }

    /**
     * Baut eine kleine Verwaltung mit erfundenen Kunden auf, in der Geldbeträge an allen
     * gespeicherten Stellen vorkommen: Kontostand (auch negativ und in Fremdwährung), Dispo,
     * Monatssumme des Sparbuchs, gekündigter Festgeldbetrag und Kontoauszug. Die Uhr steht
     * fest, damit Zeitstempel und Protokoll immer gleich sind.
     * @return die Verwaltung (Kontonummern 1 bis 4)
     * @throws GesperrtException wenn eine Buchung wider Erwarten abgelehnt wird
     */
    private static Bankverwaltung verwaltung() throws GesperrtException {
        Bankverwaltung verwaltung = Testdaten.verwaltung(
                Clock.fixed(Instant.parse("2026-10-20T08:00:00Z"), ZoneId.of("Europe/Berlin")));
        Benutzer admin = Testdaten.admin(verwaltung);
        String lena = verwaltung.kundeAnlegen(admin, Testdaten.kunde("Lena", "Hoffmann")).getKundennummer();
        String jonas = verwaltung.kundeAnlegen(admin, Testdaten.kunde("Jonas", "Becker")).getKundennummer();

        // Girokonto mit Dispo und Guthaben
        long giro = verwaltung.kontoEroeffnen(admin, lena, Kontoart.GIROKONTO, Waehrung.EURO,
                new Geldbetrag(500)).getKontonummer();
        verwaltung.bareinzahlung(admin, giro, new Geldbetrag(1234.56), null);

        // Girokonto im Minus
        long imMinus = verwaltung.kontoEroeffnen(admin, jonas, Kontoart.GIROKONTO, Waehrung.EURO,
                new Geldbetrag(300)).getKontonummer();
        verwaltung.barauszahlung(admin, imMinus, new Geldbetrag(20.5), null);

        // Sparbuch in Denar, auch mit einer Einzahlung in Euro (umgerechnet: 615 MKD)
        long sparbuch = verwaltung.kontoEroeffnen(admin, lena, Kontoart.SPARBUCH, Waehrung.DENAR,
                null).getKontonummer();
        verwaltung.bareinzahlung(admin, sparbuch, new Geldbetrag(6150, Waehrung.DENAR), null);
        verwaltung.bareinzahlung(admin, sparbuch, new Geldbetrag(10), null);
        verwaltung.barauszahlung(admin, sparbuch, new Geldbetrag(615.5, Waehrung.DENAR), null);

        // Festgeldkonto mit gekündigtem Teilbetrag
        Kontoakte festgeld = verwaltung.kontoEroeffnen(admin, jonas, Kontoart.FESTGELDKONTO, Waehrung.EURO, null);
        verwaltung.bareinzahlung(admin, festgeld.getKontonummer(), new Geldbetrag(10_000), null);
        ((Festgeldkonto) festgeld.getKonto()).kuendigen(new Geldbetrag(2500));
        return verwaltung;
    }
}
