package bankprojekt.basisdaten;

import bankprojekt.exceptions.GesperrtException;
import bankprojekt.exceptions.UngueltigeKontonummerException;

/**
 * Ein Bank-Konto, das Absender und Ziel von Überweisungen
 * sein kann.
 */
public abstract class UeberweisungsfaehigesKonto extends Konto {

    /**
     * Setzt alle Eigenschaften des Kontos auf Standardwerte
     */
    public UeberweisungsfaehigesKonto()
    {
        super();
    }

    /**
     * Setzt die beiden Eigenschaften Kontoinhaber und Kontonummer auf die angegebenen Werte,
     * der anfängliche Kontostand wird auf 0 gesetzt.
     *
     * @param inhaber der Inhaber
     * @param kontonummer die gewünschte Kontonummer
     * @throws NullPointerException Wenn der Inhaber null ist
     * @throws UngueltigeKontonummerException Wenn Kontonummer ungültig ist
     */
    public UeberweisungsfaehigesKonto(Kunde inhaber, long kontonummer) {
        super(inhaber, kontonummer);
    }

    /**
     * Bucht den angegebenen Betrag von this als Überweisung ab,
     * falls es nicht gesperrt ist und alle kontospezifischen 
     * Regeln für die Überweisung eingehalten werden.
     * Am Empfängerkonto wird keine Änderung vorgenommen, da davon ausgegangen wird, dass dieses sich
     * bei einer anderen Bank befindet.
     * @param betrag der zu überweisende Betrag
     * @param empfaenger Name des Empfängers der Überweisung
     * @param nachKontonr Kontonummer des Zielkontos
     * @param nachBlz Bankleitzahl des Zielkontos
     * @param verwendungszweck Verwendungszweck
     * @return boolean true, wenn die Überweisungsabbuchung ausgeführt wurde,
     *                 false, wenn nicht (weil kontospezifische Regeln verletzt wurden, 
     *                 	     z.B. Kontostand reicht nicht aus)
     * @throws GesperrtException wenn das Konto gesperrt ist
     * @throws IllegalArgumentException wenn der Betrag negativ bzw. NaN bzw. unendlich ist oder
     * 					empfaenger oder verwendungszweck null ist
     */
    public abstract boolean ueberweisungAbsenden(Geldbetrag betrag,
                                                 String empfaenger, long nachKontonr,
                                                 long nachBlz, String verwendungszweck)
            throws GesperrtException, NullPointerException;

    /**
     * this empfängt den angegebenen betrag per Überweisung
     * @param betrag der empfangene Betrag
     * @param vonName Name des Senders der Überweisung
     * @param vonKontonr Kontonummer des Senders
     * @param vonBlz Bankleitzahl des Senderkontos
     * @param verwendungszweck Verwendungszweck
     * @throws IllegalArgumentException wenn der Betrag negativ bzw. NaN bzw. unendlich ist oder
     * 									vonName oder verwendungszweck null ist
     */
    public abstract void ueberweisungEmpfangen(Geldbetrag betrag, String vonName, long vonKontonr, long vonBlz, String verwendungszweck);
}
