package bankprojekt.fabriken;

import bankprojekt.aktienhandel.Aktienkonto;
import bankprojekt.basisdaten.Konto;
import bankprojekt.basisdaten.Kunde;

/**
 * Übung 11 b): konkrete Fabrik, die {@link Aktienkonto}-Konten erzeugt.
 * Zeigt, dass auch ein in einem anderen Paket liegender Kontotyp ohne Änderung
 * der Bank nutzbar wird.
 */
public class AktienkontoFabrik extends Kontofabrik {

    @Override
    public Konto erstellen(Kunde inhaber, long kontonummer) {
        return new Aktienkonto(inhaber, kontonummer);
    }
}
