package bankprojekt.basisdaten;

import java.math.BigDecimal;

/**
 * Repräsentiert verschiedene Währungen mit festem Wechselkurs zum Euro.
 */
public enum Waehrung {
    /** Euro (EUR), die Bezugswährung mit dem Kurs 1 */
    EURO("1", "EUR"),
    /** Mazedonischer Denar (MKD): 61,5 Denar entsprechen 1 Euro */
    DENAR("61.5", "MKD"),
    /** Komoren-Franc (KMF): 491,96775 Franc entsprechen 1 Euro */
    FRANC("491.96775", "KMF"),
    /** São-Tomé-Dobra (STN): 24,5 Dobra entsprechen 1 Euro */
    DOBRA("24.5", "STN");

    /**
     * Umrechnungskurs der Währung zum Euro: gibt an, wie viele Einheiten dieser
     * Währung 1 Euro entsprechen. Der Kurs wird als BigDecimal gespeichert, damit er
     * genau dem festgelegten Dezimalwert entspricht (491,96775 ist als double nicht
     * exakt darstellbar).
     */
    private final BigDecimal umrechnungskursZuEuro;

    /**
     * Kürzel der Währung, also der übliche Code, an dem man die Währung erkennt,
     * z. B. "EUR" für den Euro.
     */
    private final String kuerzel;

    /**
     * Legt eine Währung mit ihrem Umrechnungskurs zum Euro und ihrem Kürzel an.
     *
     * @param kurs    Umrechnungskurs der Währung zum Euro als Dezimaltext, z. B. "61.5"
     * @param kuerzel Kürzel der Währung
     */
     Waehrung(String kurs, String kuerzel) {
        this.umrechnungskursZuEuro = new BigDecimal(kurs);
        this.kuerzel = kuerzel;
    }

    /**
     * Liefert den Umrechnungskurs der Währung zum Euro als double. Der Wert wird aus dem
     * exakten Kurs abgeleitet und ist der nächstgelegene double-Wert.
     *
     * @return wie viele Einheiten dieser Währung 1 Euro entsprechen
     */
    public double getUmrechnungskursZuEuro() {
        return umrechnungskursZuEuro.doubleValue();
    }

    /**
     * Liefert den Umrechnungskurs der Währung zum Euro exakt.
     *
     * @return wie viele Einheiten dieser Währung 1 Euro entsprechen, genau als Dezimalzahl
     */
    public BigDecimal getUmrechnungskursExakt() {
        return umrechnungskursZuEuro;
    }

    /**
     * Liefert das Kürzel der Währung.
     *
     * @return das Kürzel der Währung, z. B. "EUR" für den Euro
     */
    public String getKuerzel() {
        return kuerzel;
    }

    /**
     * Liefert das Kürzel der Währung als Textdarstellung.
     *
     * @return das Kürzel der Währung
     */
    @Override
    public String toString() {
        return this.kuerzel;
    }
}