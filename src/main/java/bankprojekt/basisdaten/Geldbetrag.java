package bankprojekt.basisdaten;

import java.io.Serializable;
import org.decimal4j.util.DoubleRounder;

/**
 * Ein Geldbetrag mit Währung
 * @author m.alharethi
 *Serializable ergänzt, damit ein Geldbetrag als Teil der
 *Bank (Kontostände, Dispo usw.) über die Java-Serialisierung gespeichert werden kann.
 *
 */
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
        // null ausdrücklich prüfen, damit wie dokumentiert eine IllegalArgumentException
        // statt einer zufälligen NullPointerException entsteht
        if (zielwaehrung == null)
            throw new IllegalArgumentException("Die Zielwährung darf nicht null sein");
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
     * @return this + summand in der Währung von this, auf 2 Nachkommastellen (Cent) gerundet
     * @throws IllegalArgumentException wenn summand null ist
     */
    public Geldbetrag plus(Geldbetrag summand) {
        if(summand == null || summand.isNegativ()) throw new IllegalArgumentException("Summand ist null oder negativ");
        // Den Summanden in die Währung von 'this' umrechnen
        Geldbetrag konvertiert = summand.umrechnen(this.waehrung);
        return new Geldbetrag(aufCentRunden(this.betrag + konvertiert.getBetrag()), this.waehrung);
    }
    /**
     * rechnet this - subtrahend
     * @param subtrahend abzuziehender Betrag
     * @return this - subtrahend in der Währung von this, auf 2 Nachkommastellen (Cent) gerundet
     * @throws IllegalArgumentException wenn subtrahend null ist
     */
    public Geldbetrag minus(Geldbetrag subtrahend) {
        if(subtrahend == null || subtrahend.isNegativ()) throw new IllegalArgumentException("Subtrahend ist null oder negativ");
        // Den Subtrahenden in die Währung von 'this' umrechnen
        Geldbetrag konvertiert = subtrahend.umrechnen(this.waehrung);
        return new Geldbetrag(aufCentRunden(this.betrag - konvertiert.getBetrag()), this.waehrung);
    }

    /**
     * multipliziert this mit faktor
     * @param faktor Faktor der Multiplikation
     * @return das faktor-Fache von this, auf 2 Nachkommastellen (Cent) gerundet
     * @throws IllegalArgumentException wenn Faktor nicht finit ist
     */
    public Geldbetrag mal(double faktor)
    {
        if(!Double.isFinite(faktor))
            throw new IllegalArgumentException("Faktor ist nicht finit");
        return new Geldbetrag(aufCentRunden(this.betrag * faktor), this.waehrung);
    }

    /**
     * Rundet ein Rechenergebnis auf 2 Nachkommastellen (Cent).
     * <p>
     * Bisher wurde mit den rohen double-Werten weitergerechnet. Dabei entstehen
     * Rundungsfehler, die sich über mehrere Buchungen aufsummieren: z. B. ergab
     * 1,00 - 0,90 den Wert 0,09999999999999998, sodass die restlichen 0,10 € nicht mehr
     * abgehoben werden konnten, und 0,10 + 0,20 war nicht gleich 0,30. Deshalb wird jedes
     * Ergebnis von plus, minus und mal – wie schon bei {@link #umrechnen(Waehrung)} –
     * auf Cent gerundet.
     *
     * @param wert das ungerundete Ergebnis
     * @return der auf 2 Nachkommastellen gerundete Wert
     */
    private static double aufCentRunden(double wert) {
        return DoubleRounder.round(wert, 2);
    }

    /**
     * Vergleicht this mit einem anderen Geldbetrag. Verglichen wird der Wert in Euro,
     * der über die Umrechnungskurse der jeweiligen Währungen berechnet wird.
     *
     * @param o der Geldbetrag, mit dem this verglichen wird
     * @return eine negative Zahl, 0 oder eine positive Zahl, je nachdem, ob this
     *         kleiner, gleich oder größer als o ist
     * @throws NullPointerException wenn o null ist
     */
    @Override
    public int compareTo(Geldbetrag o) throws NullPointerException {
        if(o == null) throw new NullPointerException();
        if(this ==  o) return 0;
        return Double.compare(this.inEuro(), o.inEuro());
    }

    /**
     * Prüft, ob this gleich dem übergebenen Objekt ist. Das ist der Fall, wenn o ein
     * Geldbetrag mit demselben Wert in Euro ist (Vergleich über compareTo).
     *
     * @param o das Vergleichsobjekt
     * @return true, wenn o ein gleichwertiger Geldbetrag ist, sonst false
     */

    @Override
    public boolean equals(Object o)
    {
        if(!(o instanceof Geldbetrag)) return false;
        return this.compareTo((Geldbetrag) o) == 0;
    }

    /**
     * Berechnet den Hashcode aus dem Wert des Geldbetrags in Euro.
     * <p>
     * {@link #equals(Object)} vergleicht über den Eurowert, d. h. z. B. 1 EUR und
     * 61,5 MKD sind gleich. Damit der Vertrag von {@link Object#hashCode()} erfüllt ist
     * (gleiche Objekte liefern den gleichen Hashcode), muss deshalb auch der Hashcode
     * aus genau demselben Eurowert berechnet werden – und nicht aus dem reinen Betrag
     * in der jeweiligen Währung.
     *
     * @return Hashcode des Eurowerts
     */
    @Override
    public int hashCode() {
        return Double.hashCode(inEuro());
    }

    /**
     * Rechnet den Betrag von this in Euro um (ohne Rundung). Wird von
     * {@link #compareTo(Geldbetrag)} und {@link #hashCode()} gemeinsam genutzt, damit
     * Vergleich und Hashcode garantiert auf demselben Wert beruhen.
     *
     * @return der Betrag von this in Euro
     */
    private double inEuro() {
        return this.betrag / this.waehrung.getUmrechnungskursZuEuro();
    }

    /**
     * Liefert die Textdarstellung des Geldbetrags: den Betrag mit zwei Nachkommastellen
     * (im Format des Standard-Locales), gefolgt vom Kürzel der Währung.
     *
     * @return der Geldbetrag in der Form "{betrag} {waehrung}", z. B. "1.234,50 EUR"
     */
    @Override
    public String toString() {
        return String.format("%,.2f %s", this.betrag, this.waehrung);
    }
}
