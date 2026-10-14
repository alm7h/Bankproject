package bankprojekt.fabriken;

import bankprojekt.basisdaten.Konto;
import bankprojekt.basisdaten.Kunde;
import bankprojekt.basisdaten.Sparbuch;

/**
 * Konkrete Fabrik, die {@link Sparbuch}-Konten erzeugt.
 * @author m.alharethi
 */
public class SparbuchFabrik extends Kontofabrik {
    /**
     * Erzeugt eine Fabrik für Sparbücher.
     */
    public SparbuchFabrik() {
    }


    @Override
    public Konto erstellen(Kunde inhaber, long kontonummer) {
        return new Sparbuch(inhaber, kontonummer);
    }
}