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
    GIROKONTO("Girokonto",
            "Das Konto für den Alltag: Überweisungen senden und empfangen, Dispo nach Prüfung."),
    /** Sparbuch (Sparkonto) */
    SPARBUCH("Sparbuch",
            "Zum Sparen: bis zu 2.000 € im Monat verfügbar, ein kleiner Rest bleibt immer stehen."),
    /** Festgeldkonto */
    FESTGELDKONTO("Festgeldkonto",
            "Für feste Anlagen: Geld steht erst nach einer Kündigung zur Verfügung."),
    /** Kinderkonto */
    KINDERKONTO("Kinderkonto",
            "Für Kinder: höchstens 50 € pro Abhebung, kein Überziehen."),
    /** Aktienkonto */
    AKTIENKONTO("Aktienkonto",
            "Für den Wertpapierhandel: Guthaben für Kauf- und Verkaufsaufträge.");

    /**
     * Lesbare Bezeichnung für die Oberfläche
     */
    private final String bezeichnung;
    /**
     * kurze Beschreibung für Kunden (z. B. bei der Kontowahl im Kundenportal)
     */
    private final String beschreibung;

    /**
     * Legt eine Kontoart mit ihrer lesbaren Bezeichnung und Beschreibung an.
     * @param bezeichnung lesbare Bezeichnung
     * @param beschreibung kurze Beschreibung für Kunden
     */
    Kontoart(String bezeichnung, String beschreibung) {
        this.bezeichnung = bezeichnung;
        this.beschreibung = beschreibung;
    }

    /**
     * Liefert eine kurze Beschreibung der Kontoart für Kunden.
     * @return die Beschreibung
     */
    public String getBeschreibung() {
        return beschreibung;
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
