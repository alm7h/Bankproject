package bankprojekt.verwaltung;

import java.time.Clock;
import java.time.LocalDate;

import bankprojekt.basisdaten.Geldbetrag;
import bankprojekt.exceptions.GesperrtException;

/**
 * Testdaten für Tests außerhalb dieses Pakets (z. B. der Oberfläche), die dafür
 * paketinterne Methoden der Verwaltung brauchen.
 */
public final class Testdaten {
    /** Passwort des Testbenutzers "admin" */
    public static final String PASSWORT = "geheim";
    /** schneller Hash des Passworts (wenige Iterationen) */
    private static final String HASH = Passwortschutz.hashen(PASSWORT.toCharArray(), 1_000);

    private Testdaten() {
    }

    /**
     * Erzeugt eine leere Verwaltung mit dem aktiven Benutzer "admin" (Passwort {@link #PASSWORT}).
     * @param uhr die Uhr der Verwaltung
     * @return die Verwaltung
     */
    public static Bankverwaltung verwaltung(Clock uhr) {
        Bankverwaltung verwaltung = new Bankverwaltung(new Bank(12345678L), uhr);
        verwaltung.erstenBenutzerEinrichten("admin", "Admin", HASH);
        return verwaltung;
    }

    /**
     * Liefert den Benutzer "admin" einer mit {@link #verwaltung(Clock)} erzeugten Verwaltung.
     * @param verwaltung die Verwaltung
     * @return der Benutzer
     */
    public static Benutzer admin(Bankverwaltung verwaltung) {
        return verwaltung.findeBenutzer("admin").orElseThrow();
    }

    /**
     * Gültige Kundendaten.
     * @param vorname Vorname (auch für die E-Mail-Adresse)
     * @param nachname Nachname
     * @return die Kundendaten
     */
    public static Kundendaten kunde(String vorname, String nachname) {
        return new Kundendaten(vorname, nachname, LocalDate.of(1990, 5, 1), "Berlin", "",
                vorname.toLowerCase() + "@example.org");
    }

    /**
     * Zahlt über die Bank auf ein Konto ein (in der Oberfläche gibt es noch keine Buchungen).
     * @param verwaltung die Verwaltung
     * @param kontonummer die Kontonummer
     * @param betrag der Betrag
     * @throws GesperrtException wenn das Konto gesperrt ist
     */
    public static void einzahlen(Bankverwaltung verwaltung, long kontonummer, Geldbetrag betrag)
            throws GesperrtException {
        verwaltung.getBank().geldEinzahlen(kontonummer, betrag);
    }

    /**
     * Hebt über die Bank von einem Konto ab.
     * @param verwaltung die Verwaltung
     * @param kontonummer die Kontonummer
     * @param betrag der Betrag
     * @throws GesperrtException wenn das Konto gesperrt ist
     */
    public static void abheben(Bankverwaltung verwaltung, long kontonummer, Geldbetrag betrag)
            throws GesperrtException {
        verwaltung.getBank().geldAbheben(kontonummer, betrag);
    }
}
