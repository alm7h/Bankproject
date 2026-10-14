package bankprojekt.verwaltung;

import bankprojekt.basisdaten.Geldbetrag;
import bankprojekt.fabriken.AktienkontoFabrik;
import bankprojekt.fabriken.FestgeldkontoFabrik;
import bankprojekt.fabriken.GirokontoFabrik;
import bankprojekt.fabriken.KinderkontoFabrik;
import bankprojekt.fabriken.Kontofabrik;
import bankprojekt.fabriken.SparbuchFabrik;

/**
 * Die Kontoarten, die in der {@link Bankverwaltung} eröffnet werden können.
 * Jede Art kennt die {@link Kontofabrik}, die das passende Konto erzeugt.
 * @author m.alharethi
 */
public enum Kontoart {
    /** Girokonto mit Dispo */
    GIROKONTO("Girokonto"),
    /** Sparbuch (Sparkonto) */
    SPARBUCH("Sparbuch"),
    /** Festgeldkonto */
    FESTGELDKONTO("Festgeldkonto"),
    /** Kinderkonto */
    KINDERKONTO("Kinderkonto"),
    /** Aktienkonto */
    AKTIENKONTO("Aktienkonto");

    /**
     * Lesbare Bezeichnung für die Oberfläche
     */
    private final String bezeichnung;

    /**
     * Legt eine Kontoart mit ihrer lesbaren Bezeichnung an.
     * @param bezeichnung lesbare Bezeichnung
     */
    Kontoart(String bezeichnung) {
        this.bezeichnung = bezeichnung;
    }

    /**
     * Gibt an, ob für diese Kontoart bei der Eröffnung ein Dispo angegeben werden muss.
     * @return true nur für das Girokonto
     */
    public boolean brauchtDispo() {
        return this == GIROKONTO;
    }

    /**
     * Liefert die Fabrik, die ein Konto dieser Art erzeugt.
     * @param dispo der Dispo für ein Girokonto; bei anderen Kontoarten ohne Bedeutung
     * @return die passende Kontofabrik
     * @throws IllegalArgumentException wenn ein Girokonto ohne positiven Dispo verlangt wird
     */
    Kontofabrik fabrik(Geldbetrag dispo) {
        return switch (this) {
            case GIROKONTO -> {
                if (dispo == null || dispo.compareTo(Geldbetrag.NULL_EURO) <= 0)
                    throw new IllegalArgumentException("Für ein Girokonto muss ein Dispo größer 0 angegeben werden.");
                yield new GirokontoFabrik(dispo);
            }
            case SPARBUCH -> new SparbuchFabrik();
            case FESTGELDKONTO -> new FestgeldkontoFabrik();
            case KINDERKONTO -> new KinderkontoFabrik();
            case AKTIENKONTO -> new AktienkontoFabrik();
        };
    }

    /**
     * Liefert die lesbare Bezeichnung der Kontoart.
     * @return die Bezeichnung, z. B. "Girokonto"
     */
    @Override
    public String toString() {
        return bezeichnung;
    }
}
