package bankprojekt.basisdaten;

import bankprojekt.exceptions.UngueltigeKontonummerException;

/**
 * Übung 11: Ein Festgeldkonto. Abhebungen sind nur bis zu der Höhe erlaubt, die
 * zuvor über {@link #kuendigen(Geldbetrag)} gekündigt wurde. Ein einmal gekündigter
 * und abgehobener Betrag kann nicht erneut abgehoben werden.
 * <p>
 * Die gemeinsame Ablauflogik der Abhebung steckt in der Template-Methode
 * {@link Konto#abheben(Geldbetrag)}; das Festgeldkonto bringt seine Regel über
 * {@link #pruefeAbhebung(Geldbetrag)} ein und verringert in {@link #nachAbhebung(Geldbetrag)}
 * den noch gekündigten Betrag.
 */
public class Festgeldkonto extends Konto {
    /**
     * Versionsnummer für die Serialisierung.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Betrag, der gekündigt wurde und daher (noch) abgehoben werden darf.
     */
    private Geldbetrag gekuendigt = Geldbetrag.NULL_EURO;

    /**
     * erstellt ein Festgeldkonto für den angegebenen Inhaber mit der angegebenen Kontonummer
     * @param inhaber Kontoinhaber
     * @param kontonummer Kontonummer
     * @throws NullPointerException wenn der Inhaber null ist
     * @throws UngueltigeKontonummerException wenn die Kontonummer ungültig ist
     */
    public Festgeldkonto(Kunde inhaber, long kontonummer) {
        super(inhaber, kontonummer);
    }

    /**
     * erstellt ein Standard-Festgeldkonto (Max Mustermann).
     */
    public Festgeldkonto() {
        super();
    }

    /**
     * Markiert den angegebenen Betrag als gekündigt, sodass er anschließend abgehoben
     * werden darf. Es kann höchstens der aktuelle Kontostand gekündigt werden; ein
     * größerer Wunschbetrag wird auf den Kontostand begrenzt.
     *
     * @param betrag der zu kündigende Betrag, darf nicht null oder negativ sein
     * @throws IllegalArgumentException wenn der Betrag null oder negativ ist
     */
    public void kuendigen(Geldbetrag betrag) {
        if (betrag == null || betrag.isNegativ())
            throw new IllegalArgumentException("Betrag ungültig");
        if (betrag.compareTo(getKontostand()) > 0)
            this.gekuendigt = getKontostand();
        else
            this.gekuendigt = betrag;
    }

    /**
     * Liefert den Betrag, der aktuell gekündigt und damit abhebbar ist.
     * @return der noch gekündigte Betrag
     */
    public Geldbetrag getGekuendigterBetrag() {
        return gekuendigt;
    }

    /**
     * Übung 11: Einschubmethode der Template-Methode {@link Konto#abheben(Geldbetrag)}.
     * Beim Festgeldkonto darf höchstens der zuvor gekündigte Betrag abgehoben werden.
     * (Da nie mehr gekündigt wird, als auf dem Konto liegt, kann der Kontostand dabei
     * nicht überzogen werden.)
     *
     * @param betrag der abzuhebende Betrag (gültig, Konto nicht gesperrt)
     * @return true, wenn der Betrag den gekündigten Betrag nicht überschreitet
     */
    @Override
    protected boolean pruefeAbhebung(Geldbetrag betrag) {
        return betrag.compareTo(gekuendigt) <= 0;
    }

    /**
     * Übung 11: Nachbearbeitung der Template-Methode {@link Konto#abheben(Geldbetrag)}.
     * Verringert nach einer erfolgreichen Abhebung den noch gekündigten Betrag, damit
     * derselbe gekündigte Betrag nicht erneut abgehoben werden kann.
     *
     * @param betrag der soeben abgehobene Betrag
     */
    @Override
    protected void nachAbhebung(Geldbetrag betrag) {
        this.gekuendigt = this.gekuendigt.minus(betrag);
    }

    @Override
    public String toString() {
        return "-- FESTGELDKONTO --" + System.lineSeparator()
                + super.toString() + System.lineSeparator()
                + "Gekündigt: " + this.gekuendigt;
    }
}
