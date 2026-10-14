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
 * @author m.alharethi
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
     * @throws NullPointerException wenn der inhaber null ist
     * @throws IllegalArgumentException wenn der angegebene dispo null, negativ bzw. NaN oder 0 ist
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
     * @throws IllegalArgumentException wenn dispo null, negativ bzw. NaN oder 0 ist
     */
    public void setDispo(Geldbetrag dispo) {
        // Ein Dispo von 0 wurde bisher entgegen der Dokumentation ("größer als 0") akzeptiert.
        // equals vergleicht den Eurowert, daher wird 0 in jeder Währung abgelehnt.
        if(dispo == null || dispo.isNegativ() || dispo.equals(Geldbetrag.NULL_EURO))
            throw new IllegalArgumentException("Der Dispo ist nicht gültig!");
        this.dispo = dispo;
    }

    /**
     * Bucht eine Überweisung von this an den angegebenen Empfänger ab. Die Überweisung
     * wird nur ausgeführt, wenn das Konto nicht gesperrt ist, alle Parameter gültig sind
     * und die Deckung einschließlich Dispo ausreicht. Die Sperre wird vor den Parametern
     * geprüft.
     *
     * @param betrag der zu überweisende Betrag, darf nicht null oder negativ sein
     * @param empfaenger Name des Empfängers, darf nicht null sein
     * @param nachKontonr Kontonummer des Empfängers
     * @param nachBlz Bankleitzahl der Bank des Empfängers
     * @param verwendungszweck Verwendungszweck, darf nicht null sein
     * @return true, wenn die Überweisung abgebucht wurde, false, wenn die Deckung nicht reicht
     * @throws GesperrtException wenn das Konto gesperrt ist
     * @throws IllegalArgumentException wenn einer der Parameter null, ungültig oder negativ ist
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
     * Einschubmethode der Template-Methode {@link Konto#abheben(Geldbetrag)}.
     * Beim Girokonto darf bis zur Höhe des Dispos überzogen werden, d. h. die Abhebung
     * ist erlaubt, solange der Kontostand danach nicht unter {@code -dispo} fällt.
     *
     * @param betrag der abzuhebende Betrag (gültig, Konto nicht gesperrt)
     * @return true, wenn der Kontostand abzüglich des Betrags den Dispo nicht überschreitet
     */
    @Override
    protected boolean pruefeAbhebung(Geldbetrag betrag) {
        return !getKontostand().plus(dispo).minus(betrag).isNegativ();
    }

    /**
     * Bucht eine eingehende Überweisung auf this. Ein gesperrtes Konto darf keine
     * Überweisungen empfangen; die Sperre wird – wie beim Absenden – vor den Parametern geprüft.
     * Sind alle Parameter gültig, wird der Betrag dem Kontostand gutgeschrieben.
     *
     * @param betrag der empfangene Betrag, darf nicht null oder negativ sein
     * @param vonName Name des Senders, darf nicht null sein
     * @param vonKontonr Kontonummer des Senders
     * @param vonBlz Bankleitzahl des Senderkontos
     * @param verwendungszweck Verwendungszweck, darf nicht null sein
     * @throws GesperrtException wenn this gesperrt ist
     * @throws IllegalArgumentException wenn einer der Parameter null, ungültig oder negativ ist
     */
    @Override
    public void ueberweisungEmpfangen(Geldbetrag betrag, String vonName, long vonKontonr, long vonBlz, String verwendungszweck)
            throws GesperrtException
    {
        // Bisher wurde auch auf einem gesperrten Konto gutgeschrieben
        if (this.isGesperrt())
            throw new GesperrtException(this.getKontonummer());
        if (betrag == null || betrag.isNegativ()|| vonName == null || verwendungszweck == null)
            throw new IllegalArgumentException("Parameter fehlerhaft");
        setKontostand(getKontostand().plus(betrag));
    }

    /**
     * Liefert die Textdarstellung des Girokontos: die Kontoart, die allgemeinen
     * Kontodaten aus {@link Konto#toString()} und den Dispo.
     *
     * @return Kontoart, Kontodaten und Dispo als Text
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
