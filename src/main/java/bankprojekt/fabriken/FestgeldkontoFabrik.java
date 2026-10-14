package bankprojekt.fabriken;

import bankprojekt.basisdaten.Festgeldkonto;
import bankprojekt.basisdaten.Konto;
import bankprojekt.basisdaten.Kunde;

/**
 * Konkrete Fabrik, die {@link Festgeldkonto}-Konten erzeugt.
 * Zeigt, dass ein neuer Kontotyp ohne Änderung der Bank nutzbar wird.
 * @author m.alharethi
 */
public class FestgeldkontoFabrik extends Kontofabrik {
    /**
     * Erzeugt eine Fabrik für Festgeldkonten.
     */
    public FestgeldkontoFabrik() {
    }


    @Override
    public Konto erstellen(Kunde inhaber, long kontonummer) {
        return new Festgeldkonto(inhaber, kontonummer);
    }
}
