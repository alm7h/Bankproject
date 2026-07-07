package bankprojekt.fabriken;

import bankprojekt.basisdaten.Festgeldkonto;
import bankprojekt.basisdaten.Konto;
import bankprojekt.basisdaten.Kunde;

/**
 * Übung 11 b): konkrete Fabrik, die {@link Festgeldkonto}-Konten erzeugt.
 * Zeigt, dass ein neuer Kontotyp ohne Änderung der Bank nutzbar wird.
 */
public class FestgeldkontoFabrik extends Kontofabrik {

    @Override
    public Konto erstellen(Kunde inhaber, long kontonummer) {
        return new Festgeldkonto(inhaber, kontonummer);
    }
}
