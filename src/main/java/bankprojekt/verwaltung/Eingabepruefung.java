package bankprojekt.verwaltung;

import java.time.LocalDate;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Prüft und bereinigt Eingaben für die {@link Bankverwaltung}. Jede Methode
 * entfernt führende und folgende Leerzeichen und wirft bei ungültigen Werten
 * eine {@link IllegalArgumentException} mit einer verständlichen deutschen Meldung,
 * die die Oberfläche direkt anzeigen kann.
 * @author m.alharethi
 */
final class Eingabepruefung {
    /**
     * Einfache Prüfung einer E-Mail-Adresse: etwas@etwas.tld, ohne Leerzeichen
     */
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$");
    /**
     * Telefonnummer: optional mit "+", danach Ziffern, Leerzeichen, Klammern, "/" oder "-"
     */
    private static final Pattern TELEFON = Pattern.compile("^\\+?[0-9][0-9 ()/-]{4,24}$");
    /**
     * Benutzername: 3 bis 30 Kleinbuchstaben, Ziffern, Punkte, Unter- oder Bindestriche
     */
    private static final Pattern BENUTZERNAME = Pattern.compile("^[a-z0-9._-]{3,30}$");
    /**
     * Frühestes zulässiges Geburtsdatum (Plausibilitätsgrenze)
     */
    private static final LocalDate FRUEHESTES_GEBURTSDATUM = LocalDate.of(1900, 1, 1);
    /**
     * Mindestlänge eines Passworts
     */
    static final int MIN_PASSWORTLAENGE = 4;

    /**
     * Nicht instanziierbar: Die Klasse enthält nur statische Prüfmethoden.
     */
    private Eingabepruefung() {
    }

    /**
     * Prüft ein Pflichtfeld.
     * @param wert eingegebener Wert
     * @param feldname Name des Feldes für die Fehlermeldung
     * @return der Wert ohne führende und folgende Leerzeichen
     * @throws IllegalArgumentException wenn der Wert null oder leer ist
     */
    static String pflichtfeld(String wert, String feldname) {
        if (wert == null || wert.isBlank())
            throw new IllegalArgumentException(feldname + " ist ein Pflichtfeld.");
        return wert.strip();
    }

    /**
     * Bereinigt ein freiwilliges Feld.
     * @param wert eingegebener Wert, darf null sein
     * @return der Wert ohne führende und folgende Leerzeichen, "" für null
     */
    static String freiwillig(String wert) {
        return wert == null ? "" : wert.strip();
    }

    /**
     * Prüft eine (verpflichtende) E-Mail-Adresse.
     * @param wert eingegebene E-Mail-Adresse
     * @return die bereinigte E-Mail-Adresse
     * @throws IllegalArgumentException wenn sie fehlt oder kein gültiges Format hat
     */
    static String email(String wert) {
        String email = pflichtfeld(wert, "E-Mail");
        if (!EMAIL.matcher(email).matches())
            throw new IllegalArgumentException("Die E-Mail-Adresse \"" + email + "\" ist ungültig.");
        return email;
    }

    /**
     * Prüft eine (freiwillige) Telefonnummer.
     * @param wert eingegebene Telefonnummer, darf leer sein
     * @return die bereinigte Telefonnummer oder ""
     * @throws IllegalArgumentException wenn eine Nummer angegeben, aber ungültig ist
     */
    static String telefon(String wert) {
        String telefon = freiwillig(wert);
        if (!telefon.isEmpty() && !TELEFON.matcher(telefon).matches())
            throw new IllegalArgumentException("Die Telefonnummer \"" + telefon
                    + "\" ist ungültig (erlaubt: Ziffern, Leerzeichen, + ( ) / -).");
        return telefon;
    }

    /**
     * Prüft ein Geburtsdatum.
     * @param datum eingegebenes Datum
     * @param heute das heutige Datum
     * @return das Datum
     * @throws IllegalArgumentException wenn es fehlt, in der Zukunft oder vor 1900 liegt
     */
    static LocalDate geburtsdatum(LocalDate datum, LocalDate heute) {
        if (datum == null)
            throw new IllegalArgumentException("Geburtsdatum ist ein Pflichtfeld.");
        if (datum.isAfter(heute))
            throw new IllegalArgumentException("Das Geburtsdatum darf nicht in der Zukunft liegen.");
        if (datum.isBefore(FRUEHESTES_GEBURTSDATUM))
            throw new IllegalArgumentException("Das Geburtsdatum darf nicht vor 1900 liegen.");
        return datum;
    }

    /**
     * Prüft einen Benutzernamen und bringt ihn in Kleinschreibung.
     * @param wert eingegebener Benutzername
     * @return der Benutzername in Kleinbuchstaben
     * @throws IllegalArgumentException wenn er fehlt oder unzulässige Zeichen enthält
     */
    static String benutzername(String wert) {
        String name = pflichtfeld(wert, "Benutzername").toLowerCase(Locale.ROOT);
        if (!BENUTZERNAME.matcher(name).matches())
            throw new IllegalArgumentException("Der Benutzername muss 3 bis 30 Zeichen lang sein "
                    + "und darf nur Buchstaben, Ziffern, Punkt, Unter- und Bindestrich enthalten.");
        return name;
    }

    /**
     * Prüft ein neues Passwort.
     * @param passwort eingegebenes Passwort
     * @throws IllegalArgumentException wenn es fehlt oder zu kurz ist
     */
    static void passwort(char[] passwort) {
        if (passwort == null || passwort.length < MIN_PASSWORTLAENGE)
            throw new IllegalArgumentException("Das Passwort muss mindestens "
                    + MIN_PASSWORTLAENGE + " Zeichen lang sein.");
    }
}
