package bankprojekt.basisdaten;

import bankprojekt.exceptions.GesperrtException;
import bankprojekt.exceptions.UngueltigeKontonummerException;

/**
 * Ein Girokonto, d.h. ein Konto mit einem Dispo und der Fähigkeit,
 * Überweisungen zu senden und zu empfangen.
 * Grundsätzlich sind Überweisungen und Abhebungen möglich bis
 * zu einem Kontostand von -this.dispo
 * @author Doro
 *
 */
public class Girokonto extends UeberweisungsfaehigesKonto{
    /**
     * Wert, bis zu dem das Konto überzogen werden darf
     */
    private Geldbetrag dispo;

    /**
     * erzeugt ein Girokonto mit den angegebenen Werten
     * @param inhaber Kontoinhaber
     * @param kontonummer Kontonummer
     * @param dispo Dispo
     * @throws IllegalArgumentException wenn der inhaber null ist
     * 				    oder der angegebene dispo negativ bzw. NaN ist
     * @throws UngueltigeKontonummerException wenn kontonummer ungültig ist
     */
    public Girokonto(Kunde inhaber, long kontonummer, Geldbetrag dispo)
    {
        super(inhaber, kontonummer);
        setDispo(dispo);
    }

    /**
     * erzeugt ein leeres, nicht gesperrtes Standard-Girokonto
     * von Max MUSTERMANN
     */
    public Girokonto()
    {
        this(Kunde.MUSTERMANN, 99887766, new Geldbetrag(500));
    }

    /**
     * liefert den Dispo
     * @return Dispo von this
     */
    public Geldbetrag getDispo() {
        return dispo;
    }

    /**
     * setzt den Dispo neu
     * @param dispo muss größer sein als 0
     * @throws IllegalArgumentException wenn dispo negativ bzw. NaN ist
     */
    public void setDispo(Geldbetrag dispo) {
        if(dispo == null || dispo.isNegativ())
            throw new IllegalArgumentException("Der Dispo ist nicht gültig!");
        this.dispo = dispo;
    }

    /**
     * Sends a bank transfer from the current account to the specified recipient's account.
     * The transfer is only executed if the account is not locked, the provided parameters are valid,
     * and sufficient funds including overdraft (dispo) are available.
     *
     * @param betrag the amount to be transferred. Must not be null or negative.
     * @param empfaenger the recipient of the transfer. Must not be null.
     * @param nachKontonr the account number of the recipient.
     * @param nachBlz the bank code (BLZ) of the recipient's bank.
     * @param verwendungszweck the purpose or reason for the transfer. Must not be null.
     * @return true if the transfer was successful, false if there are insufficient funds.
     * @throws GesperrtException if the account is locked and the transfer cannot be processed.
     * @throws IllegalArgumentException if any of the provided parameters are null, invalid, or negative.
     */

    @Override
    public boolean ueberweisungAbsenden(Geldbetrag betrag,
                                        String empfaenger, long nachKontonr,
                                        long nachBlz, String verwendungszweck)
            throws GesperrtException
    {
        if (this.isGesperrt())
            throw new GesperrtException(this.getKontonummer());
        if (betrag == null || betrag.isNegativ()|| empfaenger == null || verwendungszweck == null)
            throw new IllegalArgumentException("Parameter fehlerhaft");
        if (!getKontostand().plus(dispo).minus(betrag).isNegativ())
        {
            setKontostand(getKontostand().minus(betrag));
            return true;
        }
        else
            return false;
    }

    /**
     * Attempts to withdraw the specified amount from the account.
     * The withdrawal is only successful if the account is not locked
     * and sufficient funds, including the overdraft limit (dispo), are available.
     *
     * @param betrag the amount to withdraw. Must not be null or negative.
     * @return true if the withdrawal was successful, false if there are insufficient funds.
     * @throws GesperrtException if the account is locked and the withdrawal cannot be processed.
     * @throws IllegalArgumentException if the specified amount is null or negative.
     */
    @Override
    public boolean abheben(Geldbetrag betrag) throws GesperrtException{
        if (betrag == null || betrag.isNegativ()) {
            throw new IllegalArgumentException("Betrag ungültig");
        }
        if(this.isGesperrt())
            throw new GesperrtException(this.getKontonummer());
        if (!getKontostand().plus(dispo).minus(betrag).isNegativ())
        {
            setKontostand(getKontostand().minus(betrag));
            return true;
        }
        else
            return false;
    }

    /**
     * Processes the receipt of a bank transfer into the account. The method validates the input parameters,
     * and if valid, adds the transferred amount to the account balance.
     *
     * @param betrag the amount of money received. Must not be null or negative.
     * @param vonName the name of the sender. Must not be null.
     * @param vonKontonr the account number of the sender.
     * @param vonBlz the bank code (BLZ) of the sender's bank.
     * @param verwendungszweck the purpose or reason for the transfer. Must not be null.
     * @throws IllegalArgumentException if any of the provided parameters are null, invalid, or negative.
     */
    @Override
    public void ueberweisungEmpfangen(Geldbetrag betrag, String vonName, long vonKontonr, long vonBlz, String verwendungszweck)
    {
        if (betrag == null || betrag.isNegativ()|| vonName == null || verwendungszweck == null)
            throw new IllegalArgumentException("Parameter fehlerhaft");
        setKontostand(getKontostand().plus(betrag));
    }

    /**
     * Returns a string representation of the Girokonto object, including the account details
     * and the overdraft limit (dispo). The account details are retrieved from the superclass's
     * toString method.
     *
     * @return a formatted string containing the account type, details from the superclass,
     *         and the overdraft limit (dispo).
     */
    @Override
    public String toString()
    {
        String ausgabe = "-- GIROKONTO --" + System.lineSeparator() +
                super.toString() + System.lineSeparator()
                + "Dispo: " + this.dispo;
        return ausgabe;
    }

    /**
     * Wechselt die Währung des Kontos und rechnet den Kontostand sowie den Dispo um.
     * @param neu die neue Währung
     */
    @Override
    public void waehrungswechsel(Waehrung neu) {
        // 1. Kontostand über die Basisklasse umrechnen
        super.waehrungswechsel(neu);
        // 2. Den Dispo speziell für das Girokonto umrechnen
        if (this.dispo != null) {
            this.dispo = this.dispo.umrechnen(neu);
        }
    }


}
