package bankprojekt.basisdaten;

import java.time.LocalDate;

import bankprojekt.exceptions.UngueltigeKontonummerException;
import bankprojekt.nuetzliches.Kalender;

/**
 * ein Sparbuch, d.h. ein Konto, das nur recht eingeschränkt genutzt
 * werden kann. Insbesondere darf man monatlich nur höchstens 2000€
 * abheben, wobei der Kontostand nie unter 0,50€ fallen darf. Zur Abfrage
 * des heutigen Datums (was die Zuordnung zweier Abhebungen zu einem gemeinsamen
 * Monat bestimmt) kann ein Kalender-Objekt angegeben werden. 
 * @author Doro
 */
public class Sparbuch extends Konto {
    /**
     * Monatlich erlaubter Gesamtbetrag für Abhebungen
     */
    public static final Geldbetrag ABHEBESUMME = new Geldbetrag(2000);

    /**
     * Betrag, der nach einer Abhebung mindestens auf dem Konto bleiben muss
     */
    public static final Geldbetrag MINIMUM = new Geldbetrag(0.5);

    /**
     * Zinssatz, mit dem das Sparbuch verzinst wird. 0,03 entspricht 3%
     */
    private double zinssatz;

    /**
     * Betrag, der im aktuellen Monat bereits abgehoben wurde
     */
    private Geldbetrag bereitsAbgehoben = Geldbetrag.NULL_EURO;

    /**
     * Kalender zur Abfrage des heutigen Datums
     */
    private Kalender kalender;

    /**
     * Monat und Jahr der letzten Abhebung
     */
    private LocalDate zeitpunkt;

    /**
     * ein Standard-Sparbuch
     */
    public Sparbuch() {
        this.zinssatz = 0.03;
        this.kalender = new Kalender();
        zeitpunkt = kalender.getHeutigesDatum();
    }

    /**
     * ein Standard-Sparbuch, das inhaber gehört und die angegebene Kontonummer hat
     * Das heutige Datum wird an allen Stellen, wo es benötigt wird, vom Betriebssystem
     * abgefragt.
     * @param inhaber der Kontoinhaber
     * @param kontonummer die Wunsch-Kontonummer
     * @throws IllegalArgumentException wenn inhaber null ist
     * @throws UngueltigeKontonummerException wenn kontonummer ungültig ist
     */
    public Sparbuch(Kunde inhaber, long kontonummer) {
        this(inhaber, kontonummer, new Kalender());
    }

    /**
     * ein Standard-Sparbuch, das inhaber gehört und die angegebene Kontonummer hat
     * @param inhaber der Kontoinhaber
     * @param kontonummer die Wunsch-Kontonummer
     * @param kalender der Kalender, der für die Abfrage des heutigen Datums benutzt wird
     * @throws IllegalArgumentException wenn inhaber oder kalender null ist
     * @throws UngueltigeKontonummerException wenn kontonummer ungültig ist
     */
    public Sparbuch(Kunde inhaber, long kontonummer, Kalender kalender) {
        super(inhaber, kontonummer);
        this.zinssatz = 0.03;
        if(kalender == null)
            throw new IllegalArgumentException();
        this.kalender = kalender;
        zeitpunkt = kalender.getHeutigesDatum();
    }

    /**
     * Übung 11: Einschubmethode der Template-Methode {@link Konto#abheben(Geldbetrag)}.
     * Prüft die Abhebe-Bedingungen des Sparbuchs: Der Kontostand darf nach der Abhebung
     * nicht unter das {@link #MINIMUM} fallen und die im laufenden Monat insgesamt
     * abgehobene Summe darf {@link #ABHEBESUMME} nicht überschreiten. Beim Übergang in
     * einen neuen Monat wird die bereits abgehobene Summe zuvor zurückgesetzt.
     *
     * @param betrag der abzuhebende Geldbetrag (gültig, Konto nicht gesperrt)
     * @return true, wenn Mindestguthaben und Monatslimit eingehalten werden
     */
    @Override
    protected boolean pruefeAbhebung(Geldbetrag betrag) {
        LocalDate heute = kalender.getHeutigesDatum();
        if (heute.getMonth() != zeitpunkt.getMonth() || heute.getYear() != zeitpunkt.getYear()) {
            bereitsAbgehoben = Geldbetrag.NULL_EURO;

            // BUGFIX (Übung 06): zeitpunkt MUSS hier sofort auf 'heute' gesetzt werden.
            // Ohne dieses Update blieb zeitpunkt immer auf dem alten Datum (z.B. dem
            // Initialmonat). Dadurch wurde bereitsAbgehoben bei JEDER Abhebung in einem
            // anderen Monat erneut auf null zurückgesetzt – das monatliche Limit von 2000€
            // ließ sich so beliebig umgehen.
            // Der Test SparbuchMockitoTest#testAbhebelimitResetNurEinmalProMonat
            // scheiterte vor dieser Korrektur.
            zeitpunkt = heute;
        }

        Geldbetrag neu = getKontostand().minus(betrag);
        return neu.compareTo(Sparbuch.MINIMUM) >= 0
                && bereitsAbgehoben.plus(betrag).compareTo(Sparbuch.ABHEBESUMME) <= 0;
    }

    /**
     * Übung 11: Nachbearbeitung der Template-Methode {@link Konto#abheben(Geldbetrag)}.
     * Schreibt nach einer erfolgreichen Abhebung die im laufenden Monat bereits
     * abgehobene Summe fort und merkt sich den Zeitpunkt der Abhebung.
     *
     * @param betrag der soeben abgehobene Betrag
     */
    @Override
    protected void nachAbhebung(Geldbetrag betrag) {
        bereitsAbgehoben = bereitsAbgehoben.plus(betrag);
        zeitpunkt = kalender.getHeutigesDatum();
    }

    /**
     * Returns a string representation of the savings account. The string includes the account details
     * from the superclass and additional information specific to the savings account, such as the interest rate.
     *
     * @return a string containing details about the savings account, including superclass details and the interest rate
     */

    @Override
    public String toString()
    {
        String ausgabe = "-- SPARBUCH --" + System.lineSeparator() +
                super.toString()+ System.lineSeparator()
                + "Zinssatz: " + this.zinssatz * 100 +"%";
        return ausgabe;
    }
}
