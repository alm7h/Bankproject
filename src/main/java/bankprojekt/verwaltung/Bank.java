package bankprojekt.verwaltung;
import java.util.*;
import bankprojekt.basisdaten.Kunde;
import bankprojekt.basisdaten.Konto;
import bankprojekt.basisdaten.Girokonto;
import bankprojekt.basisdaten.Sparbuch;
import bankprojekt.basisdaten.Geldbetrag;
import bankprojekt.exceptions.GesperrtException;

/**
 * Verwaltet Konten und Kunden einer Bank unter Verwendung von Java Collections.
 */
public class Bank {

    private final long bankleitzahl;
    Geldbetrag dispo_default;
    /**
     * Speichert Konten mit der Kontonummer als Schlüssel.
     */
    private final Map<Long, Konto> konten;

    /**
     * Zähler zur Generierung eindeutiger Kontonummern.
     */
    private long naechsteFreieKontonummer = 1L;

    /**
     * Erstellt eine Bank mit der angegebenen Bankleitzahl.
     * @param bankleitzahl Die Bankleitzahl der Bank.
     */
    public Bank(long bankleitzahl) throws IllegalArgumentException {
        if(bankleitzahl<0) throw new IllegalArgumentException("bankleitzahl ist negativ");
        this.bankleitzahl = bankleitzahl;
        this.konten = new HashMap<>();
        dispo_default = new Geldbetrag(500);
    }

    /**
     * Liefert eine Map aller gespeicherten Konten, wobei die Kontonummern als Schlüssel fungieren.
     *
     * @return eine Map mit Kontonummern als Schlüsseln und den zugehörigen Konto-Objekten als Werten.
     */
    public Map<Long, Konto> getKonten() {
        return konten;
    }

    /**
     * Liefert die Bankleitzahl der Bank.
     *
     * @return die Bankleitzahl.
     */
    public long getBankleitzahl() {
        return bankleitzahl;
    }

    /**
     * Erstellt ein Girokonto für den Kunden mit einer neuen Nummer.
     * @param inhaber Der Kontoinhaber.
     * @throws NullPointerException Falls der inhaber null ist.
     * @return Die neu vergebene Kontonummer.
     */
    public long girokontoErstellen(Kunde inhaber) throws NullPointerException {
        if(inhaber == null) throw new NullPointerException("Kunde ist null");
        long kontonummer = naechsteFreieKontonummer++;
        Girokonto konto = new Girokonto(inhaber, kontonummer, dispo_default);
        konten.put(kontonummer, konto);
        return kontonummer;
    }
    /**
     * Erstellt ein Sparbuch für den Kunden mit einer neuen Nummer.
     * @param inhaber Der Kontoinhaber.
     * @throws NullPointerException Falls der inhaber null ist.
     * @return Die neu vergebene Kontonummer.
     */
    public long sparbuchErstellen(Kunde inhaber) throws NullPointerException {
        if(inhaber == null) throw new NullPointerException("Kunde ist null");
        long nummer = naechsteFreieKontonummer++;
        Sparbuch neu = new Sparbuch(inhaber, nummer);
        konten.put(nummer, neu);
        return nummer;
    }

    /**
     * Listet alle Kontonummern und Kontostände auf.
     * @return Ein String mit einer Zeile pro Konto.
     */
    public String getAlleKonten() {
        StringBuilder sb = new StringBuilder();
        for (Konto k : konten.values()) {
            sb.append(k.getKontonummer())
                    .append(": ")
                    .append(k.getKontostand())
                    .append("\n");
        }
        return sb.toString();
    }

    /**
     * Liefert ein Set aller gültigen Kontonummern.
     *
     * @return Menge der Kontonummern.
     */
    public Set<Long> getAlleKontonummern() {
        Set<Long> kontonummern = konten.keySet();
        return new HashSet<>(kontonummern);
    }

    /**
     * Liefert alle Kunden absteigend sortiert nach Geburtstag.
     * Nutzt ein TreeSet mit einem eigenen Comparator.
     * @return Sortierte Menge der Kunden.
     */
    public SortedSet<Kunde> getAlleKunden() {
        // Sortierung: Geburtstag absteigend (juengere zuerst). Bei gleichem Geburtstag nach dem
        // vollstaendigen Namen aufsteigend ("Nachname, Vorname"). Dadurch bleiben Kunden mit
        // gleichem Geburtstag aber unterschiedlichem Namen verschieden im Set, waehrend der
        // gleiche Kunde (gleicher Name und Geburtstag) nur einmal enthalten ist.
        SortedSet<Kunde> kundenSet = new TreeSet<>(new Comparator<Kunde>() {
            @Override
            public int compare(Kunde a, Kunde b) {
                // Absteigend nach Geburtstag: spaeteres Datum (juenger) kommt zuerst
                int cmpGeburtstag = b.getGeburtstag().compareTo(a.getGeburtstag());
                if (cmpGeburtstag != 0) {
                    return cmpGeburtstag;
                }
                // Bei Gleichheit des Geburtstags: Name aufsteigend
                return a.getName().compareTo(b.getName());
            }
        });

        for (Konto k : konten.values()) {
            kundenSet.add(k.getInhaber());
        }
        return kundenSet;
    }


    /**
     * Löscht das Konto mit der Nummer.
     * @param nummer Kontonummer.
     * @return true, wenn gelöscht wurde.
     */
    public boolean kontoLoeschen(long nummer) {
        return konten.remove(nummer) != null;
    }

    /**
     * Liefert den aktuellen Kontostand.
     * @param nummer Kontonummer.
     * @return Der Kontostand oder null, falls das Konto nicht existiert.
     */
    public Geldbetrag getKontostand(long nummer) {
        Konto k = konten.get(nummer);
        if (k != null) {
            return k.getKontostand();
        } else {
            return null;
        }
    }

    /**
     * Hebt einen bestimmten Geldbetrag von dem Konto mit der angegebenen Kontonummer ab,
     * sofern das Konto existiert und die Abhebung möglich ist.
     *
     * @param von Die Kontonummer, von der der Betrag abgehoben werden soll.
     * @param betrag Der Geldbetrag, der abgehoben werden soll.
     * @return true, wenn die Abhebung erfolgreich war, false, wenn die Kontonummer
     *         nicht existiert oder die Abhebung nicht ausgeführt werden konnte.
     */
    public boolean geldAbheben(long von, Geldbetrag betrag) throws GesperrtException {
        Konto k = konten.get(von);
        if (k != null) {
            return k.abheben(betrag);
        } else {
            return false;
        }
    }

    /**
     * Zahlt einen bestimmten Geldbetrag auf ein Konto ein, falls das Konto existiert.
     *
     * @param auf   Die Kontonummer des Kontos, auf das der Betrag eingezahlt werden soll.
     * @param betrag Der einzuzahlende Geldbetrag.
     */
    public void geldEinzahlen(long auf, Geldbetrag betrag) {

        Konto k = konten.get(auf);

        if (k != null) {
            k.einzahlen(betrag);
        } else {
            return;
        }
    }
}
