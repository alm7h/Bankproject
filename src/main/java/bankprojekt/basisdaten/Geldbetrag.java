package bankprojekt.basisdaten;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamField;
import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;

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
     * Anzahl der Nachkommastellen, auf die Ergebnisse gerundet werden (Cent).
     */
    private static final int NACHKOMMASTELLEN = 2;

    /**
     * Rundungsart für alle Ergebnisse: kaufmännisch (HALF_UP). Ab einem halben Cent wird
     * vom Betrag her aufgerundet, z. B. 0,005 zu 0,01 und -0,005 zu -0,01.
     */
    private static final RoundingMode RUNDUNG = RoundingMode.HALF_UP;

    /**
     * 0 €
     */
    public static final Geldbetrag NULL_EURO = new Geldbetrag(0);

    /**
     * Betrag in der in waehrung angegebenen Währung, exakt als Dezimalzahl.
     * <p>
     * Das Feld heißt wert und nicht betrag, weil der Name auch im Speicherformat steht:
     * Ältere Versionen haben den Betrag als double unter dem Namen betrag gespeichert;
     * hieße das neue Feld genauso, ließen sich solche Dateien wegen des anderen Typs nicht
     * mehr lesen. {@code readObject} rechnet sie beim Lesen um. Nicht final, weil
     * {@code readObject} das Feld setzen muss; es gibt aber keine Setter, der Geldbetrag
     * bleibt unveränderlich.
     */
    private BigDecimal wert;

    /**
     * Repräsentiert die Währung eines Geldbetrags.
     */
    private Waehrung waehrung;

    /**
     * erstellt einen Geldbetrag in der Währung Euro. Gespeichert wird der Dezimalwert,
     * mit dem Java den double ausgibt ({@link BigDecimal#valueOf(double)}), also z. B.
     * genau 0,1 und nicht die Binärdarstellung 0,1000000000000000055…; gerundet wird nicht.
     * @param betrag Betrag in €
     * @throws IllegalArgumentException wenn betrag unendlich oder NaN ist
     */
    public Geldbetrag(double betrag)
    {
        if(!Double.isFinite(betrag))
            throw new IllegalArgumentException("Der Betrag ist nicht finit");
        this.wert = BigDecimal.valueOf(betrag);
        this.waehrung = Waehrung.EURO;
    }

    /**
     * Konstruktor zur Erstellung eines Geldbetrags mit einem spezifischen Betrag und einer angegebenen Währung.
     * Wie bei {@link #Geldbetrag(double)} wird der Dezimalwert des double ungerundet gespeichert.
     *
     * @param betrag Der Betrag in der angegebenen Währung.
     * @param waehrung Die Währung des Betrags.
     * @throws IllegalArgumentException Wenn der Betrag unendlich, NaN ist oder waehrung null ist.
     */
    public Geldbetrag(double betrag, Waehrung waehrung) throws IllegalArgumentException {
        if(!Double.isFinite(betrag) || waehrung == null)
            throw new IllegalArgumentException("Der Betrag ist nicht finit oder die angegebene Waehrung ist null");
        this.wert = BigDecimal.valueOf(betrag);
        this.waehrung = waehrung;
    }

    /**
     * erstellt einen Geldbetrag in der Währung Euro
     * @param betrag Betrag in €; er wird exakt (ungerundet) übernommen
     * @throws IllegalArgumentException wenn betrag null ist
     */
    public Geldbetrag(BigDecimal betrag) {
        this(betrag, Waehrung.EURO);
    }

    /**
     * Konstruktor zur Erstellung eines Geldbetrags mit einem exakten Betrag und einer angegebenen Währung.
     *
     * @param betrag Der Betrag in der angegebenen Währung; er wird exakt (ungerundet) übernommen.
     * @param waehrung Die Währung des Betrags.
     * @throws IllegalArgumentException Wenn betrag oder waehrung null ist.
     */
    public Geldbetrag(BigDecimal betrag, Waehrung waehrung) {
        if(betrag == null || waehrung == null)
            throw new IllegalArgumentException("Der Betrag oder die angegebene Waehrung ist null");
        this.wert = betrag;
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
     * <p>
     * Gerechnet wird exakt mit den Kursen als Dezimalzahlen (Betrag × Zielkurs ÷ Ausgangskurs);
     * gerundet wird nur das Ergebnis, einmal und kaufmännisch. Z. B. sind 0,01 € genau 0,615 MKD,
     * das ergibt 0,62 MKD. (Mit double war 0,615 binär 0,61499999… und wurde zu 0,61 abgerundet.)
     *
     * @param zielwaehrung Die Währung, in die der Betrag umgerechnet werden soll.
     *                     Die Zielwährung darf nicht null sein.
     * @return Ein neuer Geldbetrag in der Zielwährung, wobei der Betrag kaufmännisch auf zwei Dezimalstellen gerundet ist.
     * @throws IllegalArgumentException Falls die Zielwährung null ist.
     */
    public Geldbetrag umrechnen(Waehrung zielwaehrung) {
        // null ausdrücklich prüfen, damit wie dokumentiert eine IllegalArgumentException
        // statt einer zufälligen NullPointerException entsteht
        if (zielwaehrung == null)
            throw new IllegalArgumentException("Die Zielwährung darf nicht null sein");
        if (this.waehrung == zielwaehrung) return this;
        // erst multiplizieren (exakt), dann teilen und dabei einmal auf Cent runden
        BigDecimal zielBetrag = this.wert.multiply(zielwaehrung.getUmrechnungskursExakt())
                .divide(this.waehrung.getUmrechnungskursExakt(), NACHKOMMASTELLEN, RUNDUNG);
        return new Geldbetrag(zielBetrag, zielwaehrung);
    }

    /**
     * Betrag von this als double
     * @return Betrag in der Währung von this, als nächstgelegener double-Wert (für exakte
     *         Rechnungen {@link #getBetragExakt()} verwenden)
     */
    public double getBetrag() {
        return wert.doubleValue();
    }

    /**
     * Betrag von this, exakt als Dezimalzahl
     * @return Betrag in der Währung von this
     */
    public BigDecimal getBetragExakt() {
        return wert;
    }

    /**
     * prüft, ob this einen negativen Betrag darstellt
     * @return true, wenn this negativ ist
     */
    public boolean isNegativ()
    {
        return this.wert.signum() < 0;
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
        return new Geldbetrag(aufCentRunden(this.wert.add(konvertiert.getBetragExakt())), this.waehrung);
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
        return new Geldbetrag(aufCentRunden(this.wert.subtract(konvertiert.getBetragExakt())), this.waehrung);
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
        // der Faktor zählt wie im Konstruktor mit seinem Dezimalwert
        return new Geldbetrag(aufCentRunden(this.wert.multiply(BigDecimal.valueOf(faktor))), this.waehrung);
    }

    /**
     * Rundet ein Rechenergebnis kaufmännisch (HALF_UP) auf 2 Nachkommastellen (Cent).
     * <p>
     * Früher wurde mit double gerechnet. Dabei entstanden Rundungsfehler, die sich über
     * mehrere Buchungen aufsummierten: z. B. ergab 1,00 - 0,90 den Wert
     * 0,09999999999999998. Mit BigDecimal rechnen plus, minus und mal exakt. Gerundet
     * werden muss trotzdem: bei mal mit einem Faktor wie 1/3 und bei Operanden mit mehr
     * als zwei Nachkommastellen (z. B. dem ungerundeten Aktienkurs). Gerundet wird der
     * Dezimalwert, 1,005 ergibt also 1,01.
     *
     * @param wert das ungerundete Ergebnis
     * @return der auf 2 Nachkommastellen gerundete Wert
     */
    private static BigDecimal aufCentRunden(BigDecimal wert) {
        return wert.setScale(NACHKOMMASTELLEN, RUNDUNG);
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
     * Vergleich und Hashcode garantiert auf demselben Wert beruhen. Gerechnet wird
     * bewusst noch mit double, damit Vergleich und Hashcode genau wie bisher bleiben.
     *
     * @return der Betrag von this in Euro
     */
    private double inEuro() {
        return this.wert.doubleValue() / this.waehrung.getUmrechnungskursZuEuro();
    }

    /**
     * Liefert die Textdarstellung des Geldbetrags: den Betrag mit zwei Nachkommastellen
     * (im Format des Standard-Locales), gefolgt vom Kürzel der Währung.
     *
     * @return der Geldbetrag in der Form "{betrag} {waehrung}", z. B. "1.234,50 EUR"
     */
    @Override
    public String toString() {
        return String.format("%,.2f %s", this.wert, this.waehrung);
    }

    /**
     * Liest einen Geldbetrag im aktuellen oder im alten Speicherformat. Fehlt das Feld
     * wert, ist es das alte Format mit dem double im Feld betrag; es wird wie im
     * Konstruktor mit {@link BigDecimal#valueOf(double)} umgerechnet, sodass
     * {@link #getBetrag()} genau den bisherigen Wert liefert.
     * @param ein der Eingabestrom
     * @throws IOException bei einem Fehler beim Lesen
     * @throws InvalidObjectException wenn Betrag oder Währung fehlen oder ungültig sind
     * @throws ClassNotFoundException falls eine Klasse nicht gefunden wird
     */
    @Serial
    private void readObject(ObjectInputStream ein) throws IOException, ClassNotFoundException {
        ObjectInputStream.GetField felder = ein.readFields();
        Object gelesenerBetrag = felder.defaulted("wert") ? alterBetrag(felder) : felder.get("wert", null);
        Object gelesenWaehrung = felder.get("waehrung", null);
        if (!(gelesenerBetrag instanceof BigDecimal b) || !(gelesenWaehrung instanceof Waehrung w))
            throw new InvalidObjectException("Der gespeicherte Geldbetrag ist ungültig");
        this.wert = b;
        this.waehrung = w;
    }

    /**
     * Liest den Betrag aus dem alten Speicherformat, in dem er als double im Feld betrag steht.
     * @param felder die gelesenen Felder
     * @return der Betrag als BigDecimal
     * @throws IOException bei einem Fehler beim Lesen
     * @throws InvalidObjectException wenn das Feld fehlt, kein double oder nicht finit ist
     */
    private static BigDecimal alterBetrag(ObjectInputStream.GetField felder) throws IOException {
        ObjectStreamField feld = felder.getObjectStreamClass().getField("betrag");
        if (feld == null || feld.getType() != double.class)
            throw new InvalidObjectException("Der gespeicherte Geldbetrag enthält keinen Betrag");
        double alt = felder.get("betrag", 0.0);
        if (!Double.isFinite(alt))
            throw new InvalidObjectException("Der gespeicherte Betrag ist nicht finit");
        return BigDecimal.valueOf(alt);
    }
}
