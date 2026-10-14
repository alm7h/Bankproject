package bankprojekt.fabriken;

import bankprojekt.aktienhandel.Aktienkonto;
import bankprojekt.basisdaten.Konto;
import bankprojekt.basisdaten.Kunde;

/**
 * Konkrete Fabrik, die {@link Aktienkonto}-Konten erzeugt.
 * Zeigt, dass auch ein in einem anderen Paket liegender Kontotyp ohne Änderung
 * der Bank nutzbar wird.
 * @author m.alharethi
 */
public class AktienkontoFabrik extends Kontofabrik {
    /**
     * Erzeugt eine Fabrik für Aktienkonten.
     */
    public AktienkontoFabrik() {
    }


    @Override
    public Konto erstellen(Kunde inhaber, long kontonummer) {
        return new Aktienkonto(inhaber, kontonummer);
    }
}
