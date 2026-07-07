package bankprojekt.fabriken;

import bankprojekt.basisdaten.Konto;
import bankprojekt.basisdaten.Kunde;

/**
 * Übung 11 b): abstrakte Fabrik (Abstract-Factory-Muster) zur Erzeugung von Konten.
 * <p>
 * Eine konkrete Unterklasse kennt jeweils genau einen Kontotyp (und dessen
 * Konfiguration, z. B. den Dispo eines Girokontos) und erzeugt in
 * {@link #erstellen(Kunde, long)} ein passendes {@link Konto}. Die {@code Bank}
 * verwendet eine solche Fabrik in ihrer Methode {@code kontoErstellen(...)}:
 * Sie vergibt die Kontonummer und speichert das Konto, überlässt das eigentliche
 * Erzeugen aber der Fabrik. Dadurch sind Erzeugung und Verwaltung der Konten
 * sauber getrennt.
 */
public abstract class Kontofabrik {

    /**
     * Erzeugt ein neues Konto für den angegebenen Inhaber mit der angegebenen
     * Kontonummer. Welcher konkrete Kontotyp entsteht, bestimmt die jeweilige
     * Unterklasse der Fabrik.
     *
     * @param inhaber der Kontoinhaber
     * @param kontonummer die von der Bank vergebene Kontonummer
     * @return das neu erzeugte Konto
     */
    public abstract Konto erstellen(Kunde inhaber, long kontonummer);
}
