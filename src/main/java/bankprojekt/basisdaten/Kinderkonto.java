package bankprojekt.basisdaten;

import bankprojekt.exceptions.UngueltigeKontonummerException;

/**
 * Übung 11: Ein Kinderkonto. Hier darf pro Abhebung höchstens 50,- € abgehoben
 * werden, und der Kontostand darf dabei nicht überzogen werden (kein Dispo).
 * <p>
 * Die gemeinsame Ablauflogik der Abhebung (Parameter- und Sperrprüfung, Abbuchung)
 * steckt in der Template-Methode {@link Konto#abheben(Geldbetrag)}; das Kinderkonto
 * steuert nur seine eigene Regel über die Einschubmethode {@link #pruefeAbhebung(Geldbetrag)} bei.
 */
public class Kinderkonto extends Konto {
    /**
     * Versionsnummer für die Serialisierung.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Höchstbetrag, der bei einer einzelnen Abhebung erlaubt ist.
     */
    public static final Geldbetrag MAX_ABHEBUNG = new Geldbetrag(50);

    /**
     * erstellt ein Kinderkonto für den angegebenen Inhaber mit der angegebenen Kontonummer
     * @param inhaber Kontoinhaber
     * @param kontonummer Kontonummer
     * @throws NullPointerException wenn der Inhaber null ist
     * @throws UngueltigeKontonummerException wenn die Kontonummer ungültig ist
     */
    public Kinderkonto(Kunde inhaber, long kontonummer) {
        super(inhaber, kontonummer);
    }

    /**
     * erstellt ein Standard-Kinderkonto (Max Mustermann).
     */
    public Kinderkonto() {
        super();
    }

    /**
     * Übung 11: Einschubmethode der Template-Methode {@link Konto#abheben(Geldbetrag)}.
     * Beim Kinderkonto sind höchstens {@link #MAX_ABHEBUNG} pro Abhebung erlaubt und
     * der Kontostand darf nicht überzogen werden.
     *
     * @param betrag der abzuhebende Betrag (gültig, Konto nicht gesperrt)
     * @return true, wenn der Betrag höchstens {@link #MAX_ABHEBUNG} ist und der
     *         Kontostand danach nicht negativ wird
     */
    @Override
    protected boolean pruefeAbhebung(Geldbetrag betrag) {
        return betrag.compareTo(MAX_ABHEBUNG) <= 0
                && !getKontostand().minus(betrag).isNegativ();
    }

    @Override
    public String toString() {
        return "-- KINDERKONTO --" + System.lineSeparator() + super.toString();
    }
}
