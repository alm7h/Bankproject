package bankprojekt.fabriken;

import bankprojekt.basisdaten.Geldbetrag;
import bankprojekt.basisdaten.Girokonto;
import bankprojekt.basisdaten.Konto;
import bankprojekt.basisdaten.Kunde;

/**
 * Übung 11 b): konkrete Fabrik, die {@link Girokonto}en mit einem festen Dispo erzeugt.
 */
public class GirokontoFabrik extends Kontofabrik {

    /**
     * Dispo, mit dem die erzeugten Girokonten ausgestattet werden.
     */
    private final Geldbetrag dispo;

    /**
     * erzeugt eine Girokonto-Fabrik mit dem angegebenen Dispo.
     * @param dispo der Dispo für die erzeugten Girokonten
     */
    public GirokontoFabrik(Geldbetrag dispo) {
        this.dispo = dispo;
    }

    @Override
    public Konto erstellen(Kunde inhaber, long kontonummer) {
        return new Girokonto(inhaber, kontonummer, dispo);
    }
}
