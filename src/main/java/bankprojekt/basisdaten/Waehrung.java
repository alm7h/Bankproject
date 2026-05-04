package bankprojekt.basisdaten;

/**
 * Repräsentiert verschiedene Währungen mit festem Wechselkurs zum Euro.
 */
public enum Waehrung {
    EURO(1.0, "EUR"),
    DENAR(61.5, "MKD"),
    FRANC(491.96775, "KMF"),
    DOBRA(24.5, "STN");

    /**
     * Represents the exchange rate of the currency relative to the Euro.
     * This value indicates how much of the specific currency corresponds to 1 Euro.
     */
    private final double umrechnungskursZuEuro;

    /**
     * Represents the abbreviation or symbol of the currency.
     * This is a standard code or shorthand used to identify the currency,
     * such as "EUR" for Euro or "USD" for US Dollar.
     */
    private final String kuerzel;

    /**
     * Constructs a Waehrung instance with a specific exchange rate to Euro and a currency abbreviation.
     *
     * @param kurs    the exchange rate of the currency relative to the Euro
     * @param kuerzel the abbreviation or symbol of the currency
     */
     Waehrung(double kurs, String kuerzel) {
        this.umrechnungskursZuEuro = kurs;
        this.kuerzel = kuerzel;
    }

    /**
     * Retrieves the exchange rate of the currency relative to the Euro.
     *
     * @return the value representing how much of this currency corresponds to 1 Euro
     */
    public double getUmrechnungskursZuEuro() {
        return umrechnungskursZuEuro;
    }

    /**
     * Retrieves the abbreviation or symbol of the currency.
     *
     * @return the abbreviation or shorthand used to identify the currency, such as "EUR" for Euro
     */
    public String getKuerzel() {
        return kuerzel;
    }

    /**
     * Returns the abbreviation or symbol of the currency.
     *
     * @return the abbreviation or shorthand used to identify the currency
     */
    @Override
    public String toString() {
        return this.kuerzel;
    }
}