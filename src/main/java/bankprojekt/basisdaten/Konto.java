package bankprojekt.basisdaten;

import java.io.Serializable;
import java.math.BigInteger;

import bankprojekt.exceptions.GesperrtException;
import bankprojekt.exceptions.UngueltigeKontonummerException;

/**
 * Stellt ein allgemeines Bank-Konto dar
 */
// Übung 10: Serializable an der Oberklasse ergänzt. Dadurch sind alle
// Kontotypen (Girokonto, Sparbuch und künftige wie Aktienkonto) automatisch
// serialisierbar, und die Bank kann beliebige Kontotypen speichern, ohne dass
// die Speicher-/Lese-Methoden die konkreten Typen kennen müssen.
public abstract class Konto implements Comparable<Konto>, Serializable
{
    /**
     * Versionsnummer für die Serialisierung.
     */
    private static final long serialVersionUID = 1L;

    /**
     * die Kontonummer
     */
    private final long nummer;

    /**
     * Wenn das Konto gesperrt ist (gesperrt = true), können keine Aktionen daran mehr vorgenommen werden,
     * die zum Schaden des Kontoinhabers wären (abheben, Inhaberwechsel)
     */
    private boolean gesperrt;

    /**
     * der Kontoinhaber
     */
    private Kunde inhaber;

    /**
     * der aktuelle Kontostand
     */
    private Geldbetrag kontostand = Geldbetrag.NULL_EURO;

    /**
     * Setzt die beiden Eigenschaften Kontoinhaber und Kontonummer auf die angegebenen Werte,
     * der anfängliche Kontostand wird auf 0 gesetzt.
     *
     * @param inhaber der Inhaber
     * @param kontonummer die gewünschte Kontonummer
     * @throws NullPointerException Wenn der Inhaber null ist
     * @throws UngueltigeKontonummerException Wenn Kontonummer ungültig ist
     */
    public Konto(Kunde inhaber, long kontonummer) throws UngueltigeKontonummerException, NullPointerException {
        if(inhaber == null)
            throw new NullPointerException("Inhaber darf nicht null sein!");
        if(kontonummer < 0 || kontonummer > 9_999_999_999L)
            throw new UngueltigeKontonummerException();
        this.inhaber = inhaber;
        this.nummer = kontonummer;
        this.gesperrt = false;
    }

    /**
     * Setzt alle Eigenschaften des Kontos auf Standardwerte
     */
    public Konto() {
        this(Kunde.MUSTERMANN, 1234567);
    }

    /**
     * Liefert die Kontonummer zurück
     * @return   Kontonummer
     */
    public long getKontonummer() {
        return nummer;
    }

    /**
     * Liefert zurück, ob das Konto gesperrt ist oder nicht
     * @return true, wenn das Konto gesperrt ist
     */
    public boolean isGesperrt() {
        return gesperrt;
    }

    /**
     * Liefert den Kontoinhaber zurück
     * @return der Inhaber
     */
    public Kunde getInhaber() {
        return this.inhaber;
    }

    /**
     * Setzt den Kontoinhaber
     * @param kinh neuer Kontoinhaber
     * @throws GesperrtException Wenn das Konto gesperrt ist
     * @throws NullPointerException wenn kinh null ist
     */
    public void setInhaber(Kunde kinh) throws GesperrtException, NullPointerException {
        if (kinh == null)
            throw new NullPointerException("Der Inhaber darf nicht null sein!");
        if(this.isGesperrt())
            throw new GesperrtException(this.nummer);
        this.inhaber = kinh;

    }

    /**
     * Liefert den aktuellen Kontostand
     * @return   Kontostand
     */
    public Geldbetrag getKontostand() {
        return kontostand;
    }

    /**
     * Setzt den aktuellen Kontostand
     * @param kontostand Ein neuer Kontostand darf nicht null sein
     */
    protected void setKontostand(Geldbetrag kontostand) {
        if(kontostand != null)
            this.kontostand = kontostand;
    }

    /**
     * Liefert eine String-Ausgabe, wenn das Konto gesperrt ist
     * @return "GESPERRT", wenn das Konto gesperrt ist, ansonsten "".
     */
    public String getGesperrtText()
    {
        if (this.isGesperrt())
            return "GESPERRT";
        else
            return "";
    }

    /**
     * Liefert die ordentlich formatierte Kontonummer
     * @return auf 10 Stellen formatierte Kontonummer
     */
    public String getKontonummerFormatiert()
    {
        return String.format("%10d", this.nummer);
    }

    /**
     * Deutsche IBAN aus der eigenen Kontonummer und der BLZ
     * @param blz höchstens 8-stellige Bankleitzahl
     * @return Deutsche IBAN, "", wenn blz zu groß oder negativ ist
     */
    public String getIban(long blz)
    {
        if(blz > 99999999L || blz < 0)
            return "";
        String zusammen = String.format("%08d", blz)
                + String.format("%010d", this.nummer)
                + "131400";
        BigInteger alsZahl = new BigInteger(zusammen);
        BigInteger rest = alsZahl.remainder(new BigInteger("97"));
        int pruefziffern = 98 - rest.intValue();
        return String.format("DE%02d%08d%010d",
                pruefziffern, blz, this.nummer);
    }


    /**
     * Der Betrag wird in bar auf das Konto eingezahlt.
     * @param betrag double
     * @throws IllegalArgumentException Wenn der Betrag negativ oder null ist
     */
    public void einzahlen(Geldbetrag betrag) {
        if (betrag == null || betrag.isNegativ()) {
            throw new IllegalArgumentException("Falscher Betrag");
        }
        setKontostand(getKontostand().plus(betrag));
    }

    /** Übung 11:
     * Mit dieser Methode wird der geforderte Betrag vom Konto in bar ausgezahlt, wenn es nicht gesperrt ist
     * und die speziellen Abheberegeln des jeweiligen Kontotyps die Abhebung erlauben.
     * <p>
     * Übung 11: Diese Methode ist die <b>Template-Methode</b>. Sie legt den für alle
     * Kontoarten gleichen Ablauf einer Abhebung ein für alle Mal fest:
     * <ol>
     *   <li>Prüfung der Parameter (nicht null, nicht negativ),</li>
     *   <li>Prüfung der Sperre,</li>
     *   <li>kontotyp-spezifische Prüfung über die Einschubmethode {@link #pruefeAbhebung(Geldbetrag)},</li>
     *   <li>Abbuchung vom Kontostand,</li>
     *   <li>kontotyp-spezifische Nachbearbeitung über {@link #nachAbhebung(Geldbetrag)}.</li>
     * </ol>
     * Sie ist deshalb {@code final} – die Unterklassen können den Ablauf nicht
     * verändern, sondern nur ihre Regeln über die beiden Einschubmethoden einbringen.
     *
     * @param betrag abzuhebender Betrag
     * @throws GesperrtException Wenn das Konto gesperrt ist
     * @throws IllegalArgumentException Wenn der Betrag negativ oder null ist
     * @return True, wenn die Abhebung geklappt hat,
     * 		   false, wenn sie abgelehnt wurde
     */
    public final synchronized boolean abheben(Geldbetrag betrag)
            throws GesperrtException {
        if (betrag == null || betrag.isNegativ())
            throw new IllegalArgumentException("Betrag ungültig");
        if (this.isGesperrt())
            throw new GesperrtException(this.getKontonummer());
        if (!pruefeAbhebung(betrag))
            return false;
        setKontostand(getKontostand().minus(betrag));
        nachAbhebung(betrag);
        return true;
    }

    /** Übung 11:
     * Einschubmethode (primitive Operation) der Template-Methode {@link #abheben(Geldbetrag)}.
     * Eine Unterklasse entscheidet hier nach ihren eigenen Regeln, ob die Abhebung
     * des angegebenen Betrags erlaubt ist. Wenn diese Methode aufgerufen wird, ist
     * der Betrag bereits als gültig geprüft (nicht null, nicht negativ) und das Konto
     * ist nicht gesperrt; die Unterklasse muss diese Fälle also nicht erneut behandeln.
     * <p>
     * Die Methode ist {@code protected}: Sie gehört zum internen Mechanismus der
     * Template-Methode und darf nicht von beliebigem fremden Code aufgerufen werden,
     * sondern nur innerhalb der Vererbungshierarchie.
     *
     * @param betrag der abzuhebende Betrag (gültig, Konto nicht gesperrt)
     * @return true, wenn die Abhebung nach den Regeln der Unterklasse erlaubt ist
     */
    protected abstract boolean pruefeAbhebung(Geldbetrag betrag);

    /** Übung 11:
     * Einschubmethode (Haken) der Template-Methode {@link #abheben(Geldbetrag)}.
     * Sie wird nach einer erfolgreichen Abbuchung aufgerufen, damit eine Unterklasse
     * zusätzliche Buchführung erledigen kann (z. B. das Sparbuch die im Monat bereits
     * abgehobene Summe fortschreiben oder das Festgeldkonto den noch gekündigten
     * Betrag verringern). Die Standardimplementierung tut nichts.
     *
     * @param betrag der soeben erfolgreich abgehobene Betrag
     */
    protected void nachAbhebung(Geldbetrag betrag) {
        // Standardverhalten: keine zusätzliche Buchführung nötig.
    }

    /**
     * Sperrt das Konto, sind Aktionen zum Schaden des Benutzers nicht mehr möglich.
     */
    public void sperren() {
        this.gesperrt = true;
    }

    /**
     * Entsperrt das Konto, alle Kontoaktionen sind wieder möglich.
     */
    public void entsperren() {
        this.gesperrt = false;
    }

    @Override
    public String toString() {
        return "Kontonummer: " + this.getKontonummerFormatiert()
                + " " + this.getGesperrtText()
                + System.lineSeparator()
                + "Inhaber: " + this.inhaber + System.lineSeparator()
                + "Aktueller Kontostand: " + getKontostand() + " ";
    }

    /**
     * Gibt das this-Konto auf der Konsole aus
     */
    public void ausgebenAufDerKonsole() {
        System.out.println(this);
    }


    /**
     * Vergleich von this mit other; Zwei Konten gelten als gleich,
     * wen sie die gleiche Kontonummer haben
     *
     * @param other das Vergleichskonto
     * @return True, wenn beide Konten die gleiche Nummer haben
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Konto otherKonto)) {
            return false;
        }
        return this.nummer == otherKonto.nummer;
    }

    /**
     * Computes the hash code for the account object. The hash code is based on the
     * account number's value.
     *
     * @return an integer representing the hash code for the account
     */

    @Override
    public int hashCode()
    {
        return 31 + Long.hashCode(this.nummer);
    }


    /**
     * Compares this Konto object with another Konto object for order. The comparison
     * is based on the kontonummer of the accounts.
     *
     * @param other the other Konto object to be compared.
     * @return a negative integer, zero, or a positive integer if this Konto's
     *         kontonummer is less than, equal to, or greater than the specified Konto's kontonummer, respectively.
     */
    @Override
    public int  compareTo(Konto other)
    {
        return Long.compare(this.nummer, other.nummer);
    }

    /**
     * Wechselt die Währung des Kontos und rechnet den Kontostand um
     * @param neu die neue Währung
     */
    public void waehrungswechsel(Waehrung neu) throws NullPointerException {
        if (neu == null) {
            throw new NullPointerException("Die neue Währung darf nicht null");
        }
        this.kontostand = this.kontostand.umrechnen(neu);
    }
}


