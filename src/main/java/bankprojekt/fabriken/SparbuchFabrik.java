package bankprojekt.fabriken;

import bankprojekt.basisdaten.Konto;
import bankprojekt.basisdaten.Kunde;
import bankprojekt.basisdaten.Sparbuch;

/**
 * Übung 11 b): konkrete Fabrik, die {@link Sparbuch}-Konten erzeugt.
 */
public class SparbuchFabrik extends Kontofabrik {

    @Override
    public Konto erstellen(Kunde inhaber, long kontonummer) {
        return new Sparbuch(inhaber, kontonummer);
    }
}