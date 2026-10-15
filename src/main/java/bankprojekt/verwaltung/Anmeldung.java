package bankprojekt.verwaltung;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Prüft die Anmeldung von Benutzern an der {@link Bankverwaltung}.
 * <p>
 * Nach {@value #MAX_FEHLVERSUCHE} aufeinanderfolgenden Fehlversuchen wird die
 * Anmeldung für {@link #SPERRDAUER} gesperrt; in dieser Zeit wird kein Passwort
 * geprüft. Ein erfolgreicher Versuch setzt den Zähler zurück. Jeder Versuch, jede
 * Sperre und jede Abmeldung wird protokolliert.
 * <p>
 * Der Zähler gilt für die laufende Anwendung (alle Benutzernamen gemeinsam), damit
 * auch das Durchprobieren verschiedener Benutzernamen gebremst wird. Bei einem
 * unbekannten Benutzernamen wird trotzdem ein Passwort-Hash berechnet, damit die
 * Antwortzeit nicht verrät, ob es den Benutzer gibt.
 * @author m.alharethi
 */
public final class Anmeldung {
    /**
     * Anzahl der Fehlversuche, nach der die Anmeldung gesperrt wird
     */
    public static final int MAX_FEHLVERSUCHE = 5;
    /**
     * Dauer der Sperre nach zu vielen Fehlversuchen
     */
    public static final Duration SPERRDAUER = Duration.ofSeconds(60);

    /**
     * die Verwaltung mit den Benutzern und dem Protokoll
     */
    private final Bankverwaltung verwaltung;
    /**
     * Uhr für die Sperrzeit (in Tests austauschbar)
     */
    private final Clock uhr;
    /**
     * Anzahl der Fehlversuche seit der letzten erfolgreichen Anmeldung bzw. Sperre
     */
    private int fehlversuche;
    /**
     * Ende der aktuellen Sperre, null wenn nicht gesperrt
     */
    private Instant gesperrtBis;

    /**
     * Erstellt die Anmeldung für eine Verwaltung; die Sperrzeit läuft nach deren Uhr.
     * @param verwaltung die Bankverwaltung
     * @throws NullPointerException wenn die Verwaltung null ist
     */
    public Anmeldung(Bankverwaltung verwaltung) {
        this(verwaltung, verwaltung.getUhr());
    }

    /**
     * Erstellt die Anmeldung für eine Verwaltung mit eigener Uhr.
     * @param verwaltung die Bankverwaltung
     * @param uhr die Uhr für die Sperrzeit
     * @throws NullPointerException wenn Verwaltung oder Uhr null ist
     */
    Anmeldung(Bankverwaltung verwaltung, Clock uhr) {
        this.verwaltung = Objects.requireNonNull(verwaltung, "Verwaltung darf nicht null sein");
        this.uhr = Objects.requireNonNull(uhr, "Uhr darf nicht null sein");
    }

    /**
     * Versucht, einen Benutzer anzumelden.
     * @param benutzername der eingegebene Benutzername (Groß-/Kleinschreibung egal)
     * @param passwort das eingegebene Passwort
     * @return das Ergebnis; bei Erfolg mit dem angemeldeten Benutzer
     */
    public Anmeldeergebnis anmelden(String benutzername, char[] passwort) {
        Duration rest = getRestsperre();
        if (!rest.isZero())
            return new Anmeldeergebnis(Anmeldeergebnis.Status.GESPERRT, null, 0, rest);

        String name = benutzername == null ? "" : benutzername.strip();
        Optional<Benutzer> gefunden = verwaltung.findeBenutzer(name);
        boolean passwortRichtig;
        if (gefunden.isPresent()) {
            passwortRichtig = gefunden.get().passwortPruefen(passwort);
        } else {
            // gleich viel Rechenzeit wie bei einem bekannten Benutzer
            Passwortschutz.pruefen(passwort, Platzhalter.HASH);
            passwortRichtig = false;
        }

        if (passwortRichtig && gefunden.get().isAktiv()) {
            fehlversuche = 0;
            verwaltung.anmeldungVermerken(gefunden.get());
            return new Anmeldeergebnis(Anmeldeergebnis.Status.ERFOLGREICH, gefunden.get(), MAX_FEHLVERSUCHE,
                    Duration.ZERO);
        }

        // Fehlversuch: falsche Zugangsdaten oder deaktivierter Benutzer
        fehlversuche++;
        String grund = passwortRichtig ? "Benutzer ist deaktiviert" : "Benutzername oder Passwort falsch";
        verwaltung.protokollieren(name.isEmpty() ? "(leer)" : name, Protokollaktion.ANMELDUNG_FEHLGESCHLAGEN,
                grund + " (Fehlversuch " + fehlversuche + " von " + MAX_FEHLVERSUCHE + ")");
        if (fehlversuche >= MAX_FEHLVERSUCHE) {
            fehlversuche = 0;
            gesperrtBis = uhr.instant().plus(SPERRDAUER);
            verwaltung.protokollieren(Bankverwaltung.SYSTEM, Protokollaktion.ANMELDUNG_GESPERRT,
                    MAX_FEHLVERSUCHE + " Fehlversuche – Anmeldung für "
                            + SPERRDAUER.toSeconds() + " Sekunden gesperrt");
            return new Anmeldeergebnis(Anmeldeergebnis.Status.GESPERRT, null, 0, SPERRDAUER);
        }
        Anmeldeergebnis.Status status = passwortRichtig
                ? Anmeldeergebnis.Status.DEAKTIVIERT : Anmeldeergebnis.Status.FALSCHE_ZUGANGSDATEN;
        return new Anmeldeergebnis(status, null, MAX_FEHLVERSUCHE - fehlversuche, Duration.ZERO);
    }

    /**
     * Meldet einen Benutzer ab (wird protokolliert).
     * @param benutzer der abzumeldende Benutzer; bei null geschieht nichts
     */
    public void abmelden(Benutzer benutzer) {
        if (benutzer != null)
            verwaltung.protokollieren(benutzer.getBenutzername(), Protokollaktion.ABMELDUNG, "Abgemeldet");
    }

    /**
     * Liefert, wie lange die Anmeldung noch gesperrt ist.
     * @return die Restdauer der Sperre; {@link Duration#ZERO}, wenn nicht gesperrt
     */
    public Duration getRestsperre() {
        if (gesperrtBis == null)
            return Duration.ZERO;
        Duration rest = Duration.between(uhr.instant(), gesperrtBis);
        if (rest.isNegative() || rest.isZero()) {
            gesperrtBis = null;
            return Duration.ZERO;
        }
        return rest;
    }

    /**
     * Hält einen Platzhalter-Hash für unbekannte Benutzernamen. Er wird erst beim
     * ersten Bedarf berechnet (Holder-Idiom), weil das Hashen Zeit kostet.
     */
    private static final class Platzhalter {
        /**
         * Hash eines zufälligen, nie verwendeten Passworts
         */
        static final String HASH = Passwortschutz.hashen(java.util.UUID.randomUUID().toString().toCharArray());

        /**
         * Nicht instanziierbar.
         */
        private Platzhalter() {
        }
    }
}
