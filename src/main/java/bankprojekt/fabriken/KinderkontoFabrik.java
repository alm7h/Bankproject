package bankprojekt.fabriken;

import bankprojekt.basisdaten.Kinderkonto;
import bankprojekt.basisdaten.Konto;
import bankprojekt.basisdaten.Kunde;

/**
 * Konkrete Fabrik, die {@link Kinderkonto}-Konten erzeugt.
 * Zeigt, dass ein neuer Kontotyp ohne Änderung der Bank nutzbar wird.
 * @author m.alharethi
 */
public class KinderkontoFabrik extends Kontofabrik {

    @Override
    public Konto erstellen(Kunde inhaber, long kontonummer) {
        return new Kinderkonto(inhaber, kontonummer);
    }
}
