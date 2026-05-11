package bankprojekt.verwaltung;
import java.util.*;
import bankprojekt.basisdaten.Kunde;
import bankprojekt.basisdaten.Konto;
import bankprojekt.basisdaten.Girokonto;
import bankprojekt.basisdaten.Sparbuch;
import bankprojekt.basisdaten.Geldbetrag;
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
}
