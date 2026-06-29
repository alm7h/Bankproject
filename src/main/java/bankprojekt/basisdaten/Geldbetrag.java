package bankprojekt.basisdaten;

import java.io.Serializable;
import java.util.Objects;
import org.decimal4j.util.DoubleRounder;

/**
 * Ein Geldbetrag mit Währung
 */
// Übung 10: Serializable ergänzt, damit ein Geldbetrag als Teil der
// Bank (Kontostände, Dispo usw.) über die Java-Serialisierung gespeichert werden kann.
public class Geldbetrag implements Comparable<Geldbetrag>, Serializable {
    /**
     * Versionsnummer für die Serialisierung.
     */
    private static final long serialVersionUID = 1L;

    /**
     * 0 €
     */
    public static final Geldbetrag NULL_EURO = new Geldbetrag(0);

    /**
     * Betrag in der in waehrung angegebenen Währung
     */
    private final double betrag;

    /**
     * Repräsentiert die Währung eines Geldbetrags.
     */
    private Waehrung waehrung;

    /**
     * erstellt einen Geldbetrag in der Währung Euro
     * @param betrag Betrag in €
     * @throws IllegalArgumentException wenn betrag unendlich oder NaN ist
     */
    public Geldbetrag(double betrag)
    {
        if(!Double.isFinite(betrag))
            throw new IllegalArgumentException("Der Betrag ist nicht finit");
        this.betrag = betrag;
        this.waehrung = Waehrung.EURO;
    }

    /**
     * Konstruktor zur Erstellung eines Geldbetrags mit einem spezifischen Betrag und einer angegebenen Währung.
     *
     * @param betrag Der Betrag in der angegebenen Währung.
     * @param waehrung Die Währung des Betrags.
     * @throws IllegalArgumentException Wenn der Betrag unendlich, NaN ist oder waehrung null ist.
     */
    public Geldbetrag(double betrag, Waehrung waehrung) throws IllegalArgumentException {
        if(!Double.isFinite(betrag) || waehrung == null)
            throw new IllegalArgumentException("Der Betrag ist nicht finit oder die angegebene Waehrung ist null");
        this.betrag = betrag;
        this.waehrung = waehrung;
    }

    /**
     * Liefert die Währung des Geldbetrags.
     *
     * @return die Währung, in der der Betrag vorliegt
     */
    public Waehrung getWaehrung() {
        return waehrung;
    }

    /**
     * Konvertiert den aktuellen Geldbetrag in eine andere Zielwährung.
     * Wenn die Zielwährung identisch mit der aktuellen Währung ist, wird der ursprüngliche Geldbetrag zurückgegeben.
     *
     * @param zielwaehrung Die Währung, in die der Betrag umgerechnet werden soll.
     *                     Die Zielwährung darf nicht null sein.
     * @return Ein neuer Geldbetrag in der Zielwährung, wobei der Betrag auf zwei Dezimalstellen gerundet ist.
     * @throws IllegalArgumentException Falls die Zielwährung null ist.
     */
    public Geldbetrag umrechnen(Waehrung zielwaehrung) {
        if (this.waehrung == zielwaehrung) return this;
        double inEuro = this.betrag / this.waehrung.getUmrechnungskursZuEuro();
        // Umrechnung und Rundung auf 2 Stellen
        double zielBetragGerundet = DoubleRounder.round(inEuro * zielwaehrung.getUmrechnungskursZuEuro(), 2);
        return new Geldbetrag(zielBetragGerundet, zielwaehrung);
    }

    /**
     * Betrag von this
     * @return Betrag in der Währung von this
     */
    public double getBetrag() {
        return betrag;
    }

    /**
     * prüft, ob this einen negativen Betrag darstellt
     * @return true, wenn this negativ ist
     */
    public boolean isNegativ()
    {
        return this.betrag < 0;
    }

    /**
     * rechnet this + summand
     * @param summand zu addierender Betrag
     * @return this + summand in der Währung von this
     * @throws IllegalArgumentException wenn summand null ist
     */
    public Geldbetrag plus(Geldbetrag summand) {
        if(summand == null || summand.isNegativ()) throw new IllegalArgumentException("Summand ist null oder negativ");
        // Den Summanden in die Währung von 'this' umrechnen
        Geldbetrag konvertiert = summand.umrechnen(this.waehrung);
        return new Geldbetrag(this.betrag + konvertiert.getBetrag(), this.waehrung);
    }
    /**
     * rechnet this - subtrahend
     * @param subtrahend abzuziehender Betrag
     * @return this - subtrahend in der Währung von this
     * @throws IllegalArgumentException wenn subtrahend null ist
     */
    public Geldbetrag minus(Geldbetrag subtrahend) {
        if(subtrahend == null || subtrahend.isNegativ()) throw new IllegalArgumentException("Subtrahend ist null oder negativ");
        // Den Subtrahenden in die Währung von 'this' umrechnen
        Geldbetrag konvertiert = subtrahend.umrechnen(this.waehrung);
        return new Geldbetrag(this.betrag - konvertiert.getBetrag(), this.waehrung);
    }

    /**
     * multipliziert this mit faktor
     * @param faktor Faktor der Multiplikation
     * @return das faktor-Fache von this
     * @throws IllegalArgumentException wenn Faktor nicht finit ist
     */
    public Geldbetrag mal(double faktor)
    {
        if(!Double.isFinite(faktor))
            throw new IllegalArgumentException("Faktor ist nicht finit");
        return new Geldbetrag(this.betrag * faktor, this.waehrung);
    }

    /**
     * Compares this Geldbetrag instance with another Geldbetrag instance for order.
     * The comparison is based on the equivalent value of the amounts in Euros,
     * calculated using the conversion rates of their respective currencies.
     *
     * @param o the Geldbetrag instance to compare to this instance
     * @return a negative integer, zero, or a positive integer as this instance
     *         is less than, equal to, or greater than the specified instance
     * @throws NullPointerException if the specified Geldbetrag is null
     */
    @Override
    public int compareTo(Geldbetrag o) throws NullPointerException {
        if(o == null) throw new NullPointerException();
        if(this ==  o) return 0;
        double dieseInEuro = this.betrag / this.waehrung.getUmrechnungskursZuEuro();
        double andereInEuro = o.betrag / o.waehrung.getUmrechnungskursZuEuro();
        return Double.compare(dieseInEuro, andereInEuro);
    }

    /**
     * Determines whether this instance is equal to another specified object.
     * The comparison is based on whether the other object is an instance of Geldbetrag
     * and has the same equivalent value as this instance, determined using the compareTo method.
     *
     * @param o the object to compare for equality with this instance
     * @return true if the specified object is equal to this instance, false otherwise
     */

    @Override
    public boolean equals(Object o)
    {
        if(!(o instanceof Geldbetrag)) return false;
        return this.compareTo((Geldbetrag) o) == 0;
    }

    /**
     * Generates a hash code for this instance based on its `betrag` value.
     *
     * @return an integer hash code derived from the `betrag` property
     */
    @Override
    public int hashCode() {
        return Objects.hash(this.getBetrag());
    }

    /**
     * Converts this Geldbetrag instance to its string representation.
     * The string contains the monetary amount formatted to two decimal places,
     * followed by the currency symbol.
     *
     * @return a string representation of the Geldbetrag in the format "{betrag} {waehrung}"
     *         where {betrag} is the amount formatted with two decimal places and
     *         {waehrung} is the currency abbreviation.
     */
    @Override
    public String toString() {
        return String.format("%,.2f %s", this.betrag, this.waehrung);
    }
}
