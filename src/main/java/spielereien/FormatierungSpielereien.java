package spielereien;

// Übung 10: neues Hauptprogramm zur Übung 10 (Formatierung mit
// PrintWriter.printf). Es werden ausschließlich Formatstrings verwendet -
// keine eigene String-Umwandlung und keine Formatierungsobjekte (DateTimeFormatter o.ä.).
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Locale;
import java.util.Scanner;

/**
 * Liest vom Benutzer eine ganze Zahl und eine Kommazahl ein und schreibt diese
 * sowie das aktuelle Datum und die aktuelle Uhrzeit auf verschiedene Arten
 * formatiert in eine Textdatei. Zur Formatierung wird durchgängig
 * {@link PrintWriter#printf} (bzw. dessen Formatstrings) benutzt.
 *
 * @author Claude
 */
public class FormatierungSpielereien {

    /**
     * Name der Datei, in die die formatierten Werte geschrieben werden.
     */
    private static final String DATEINAME = "formatierung.txt";

    /**
     * Startet das Programm.
     * @param args werden nicht verwendet
     * @throws IOException wenn die Datei nicht geschrieben werden kann
     */
    public static void main(String[] args) throws IOException {
        // Eingaben vom Benutzer einlesen
        Scanner tastatur = new Scanner(System.in);

        System.out.print("Bitte eine ganze Zahl eingeben: ");
        long ganzeZahl = tastatur.nextLong();

        System.out.print("Bitte eine Zahl mit Nachkommaanteil eingeben (mit Punkt, z.B. 3.14): ");
        double kommaZahl = Double.parseDouble(tastatur.next());

        // aktuelles Datum und aktuelle Uhrzeit
        LocalDate heute = LocalDate.now();
        LocalTime jetzt = LocalTime.now();

        // PrintWriter über FileWriter stülpen und alle Zeilen formatiert schreiben
        try (PrintWriter aus = new PrintWriter(new FileWriter(DATEINAME))) {
            // 1. ganze Zahl mit Standardformatierung
            aus.printf("%d%n", ganzeZahl);

            // 2. ganze Zahl mit 10 Stellen, vorne mit Nullen aufgefüllt
            aus.printf("%010d%n", ganzeZahl);

            // 3. mit Tausendertrennzeichen (,), negative Zahlen in Klammern (()
            aus.printf("%(,d%n", ganzeZahl);

            // 4. hexadezimal, kleine Ziffern (x), alternatives Format (#) -> Präfix 0x
            aus.printf("%#x%n", ganzeZahl);

            // 5. Kommazahl mit Standardformatierung
            aus.printf("%f%n", kommaZahl);

            // 6. mit Vorzeichen (+) und 3 Nachkommastellen
            aus.printf("%+.3f%n", kommaZahl);

            // 7. in wissenschaftlicher Darstellung (Exponent)
            aus.printf("%e%n", kommaZahl);

            // 8. 2 Nachkommastellen mit US-Punkt als Dezimaltrennzeichen (Locale als 1. Parameter)
            aus.printf(Locale.US, "%.2f%n", kommaZahl);

            // 9. wie 8., zusätzlich mit Prozentzeichen (%% ergibt ein literales %)
            aus.printf(Locale.US, "%.2f%%%n", kommaZahl);

            // 10. heutiges Datum: Tag ohne führende 0 (te), Monatsname ausgeschrieben (tB),
            //     vierstelliges Jahr (tY), in Klammern abgekürzter Wochentag (ta).
            //     Über den Argumentindex 1$ wird dasselbe Datum mehrfach verwendet.
            aus.printf("%1$te. %1$tB %1$tY (%1$ta)%n", heute);

            // 11. heutiges Datum französisch: zweistelliger Tag (td), Monat (tm) und Jahr (ty),
            //     voll ausgeschriebener Wochentag (tA) in französischer Sprache (Locale.FRANCE).
            aus.printf(Locale.FRANCE, "%1$td.%1$tm.%1$ty (%1$tA)%n", heute);

            // 12. aktuelle Uhrzeit englisch: Stunde im 12h-Format ohne führende 0 (tl),
            //     Minute (tM), am/pm (tp) - in englischer Schreibweise (Locale.US).
            aus.printf(Locale.US, "%1$tl:%1$tM %1$tp%n", jetzt);
        }

        System.out.println("Die formatierten Werte wurden in die Datei \"" + DATEINAME + "\" geschrieben.");
    }
}
