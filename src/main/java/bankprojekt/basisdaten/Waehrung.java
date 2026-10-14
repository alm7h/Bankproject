package bankprojekt.basisdaten;

/**
 * Repräsentiert verschiedene Währungen mit festem Wechselkurs zum Euro.
 */
public enum Waehrung {
    /** Euro (EUR), die Bezugswährung mit dem Kurs 1 */
    EURO(1.0, "EUR"),
    /** Mazedonischer Denar (MKD): 61,5 Denar entsprechen 1 Euro */
    DENAR(61.5, "MKD"),
    /** Komoren-Franc (KMF): 491,96775 Franc entsprechen 1 Euro */
    FRANC(491.96775, "KMF"),
    /** São-Tomé-Dobra (STN): 24,5 Dobra entsprechen 1 Euro */
    DOBRA(24.5, "STN");

    /**
     * Umrechnungskurs der Währung zum Euro: gibt an, wie viele Einheiten dieser
     * Währung 1 Euro entsprechen.
     */
    private final double umrechnungskursZuEuro;

    /**
     * Kürzel der Währung, also der übliche Code, an dem man die Währung erkennt,
     * z. B. "EUR" für den Euro.
     */
    private final String kuerzel;

    /**
     * Legt eine Währung mit ihrem Umrechnungskurs zum Euro und ihrem Kürzel an.
     *
     * @param kurs    Umrechnungskurs der Währung zum Euro
     * @param kuerzel Kürzel der Währung
     */
     Waehrung(double kurs, String kuerzel) {
        this.umrechnungskursZuEuro = kurs;
        this.kuerzel = kuerzel;
    }

    /**
     * Liefert den Umrechnungskurs der Währung zum Euro.
     *
     * @return wie viele Einheiten dieser Währung 1 Euro entsprechen
     */
    public double getUmrechnungskursZuEuro() {
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