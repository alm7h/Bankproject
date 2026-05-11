package bankprojekt.verwaltung;
import java.util.*;
import bankprojekt.basisdaten.Kunde;
import bankprojekt.basisdaten.Konto;
import bankprojekt.basisdaten.Girokonto;
import bankprojekt.basisdaten.Sparbuch;
import bankprojekt.basisdaten.Geldbetrag;
import bankprojekt.basisdaten.UeberweisungsfaehigesKonto;
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
        }
    }


    /**
     * Führt eine Überweisung eines Geldbetrags von einem Konto zu einem anderen durch,
     * sofern beide Konten existieren und die Transaktion möglich ist.
     *
     * @param vonKontonr Die Kontonummer des sendenden Kontos.
     * @param nachKontonr Die Kontonummer des empfangenden Kontos.
     * @param betrag Der zu überweisende Geldbetrag. Muss positiv und nicht null sein.
     * @param verwendungszweck Der Verwendungszweck der Überweisung. Darf nicht null sein.
     * @return true, wenn die Überweisung erfolgreich war, false, wenn sie aufgrund
     *         von Bedingungen (z.B. fehlende Konten, nicht transferfähige Konten)
     *         nicht durchgeführt werden konnte.
     * @throws GesperrtException Wenn das sendende Konto gesperrt ist.
     * @throws IllegalArgumentException Wenn einer der Parameter ungültig ist,
     *         z.B. bei identischen Kontonummern, einem negativen Betrag oder
     *         einem null-Wert für verpflichtende Parameter.
     */
    public boolean geldUeberweisen(long vonKontonr, long nachKontonr, Geldbetrag betrag, String verwendungszweck)
            throws GesperrtException, IllegalArgumentException {
        // Grundvalidierungen (werfen bewusst IllegalArgumentException bei ungueltigen Eingaben)
        if (vonKontonr == nachKontonr) {
            throw new IllegalArgumentException("Quell- und Zielkontonummer duerfen nicht identisch sein");
        }
        if (betrag == null || betrag.isNegativ() || betrag.equals(Geldbetrag.NULL_EURO)) {
            throw new IllegalArgumentException("Betrag muss positiv sein und darf nicht null sein");
        }
        if (verwendungszweck == null) {
            throw new IllegalArgumentException("Verwendungszweck darf nicht null sein");
        }

        Konto von = konten.get(vonKontonr);
        Konto nach = konten.get(nachKontonr);
        if (von == null || nach == null) {
            // Eine der Kontonummern ist unbekannt -> keine Ueberweisung
            return false;
        }
        if (!(von instanceof UeberweisungsfaehigesKonto) || !(nach instanceof UeberweisungsfaehigesKonto)) {
            // Mindestens eines der Konten ist nicht ueberweisungsfaehig
            return false;
        }

        UeberweisungsfaehigesKonto sender = (UeberweisungsfaehigesKonto) von;
        UeberweisungsfaehigesKonto empfaenger = (UeberweisungsfaehigesKonto) nach;

        String empfaengerName = empfaenger.getInhaber().getName();
        String senderName = sender.getInhaber().getName();

        // 1) Beim Sender abbuchen (kann GesperrtException werfen oder false liefern)
        boolean abgebucht = sender.ueberweisungAbsenden(betrag, empfaengerName, nachKontonr, this.bankleitzahl, verwendungszweck);
        if (!abgebucht) {
            return false;
        }

        // 2) Beim Empfaenger gutschreiben. Sollte hier (unerwartet) etwas schiefgehen,
        //    wird eine Rollback-Einzahlung auf dem Sender versucht, damit kein Geld verschwindet.
        try {
            empfaenger.ueberweisungEmpfangen(betrag, senderName, vonKontonr, this.bankleitzahl, verwendungszweck);
            return true;
        } catch (RuntimeException e) {
            // Rollback: Gutschrift fehlgeschlagen -> Betrag dem Sender wieder gutschreiben
            try {
                sender.einzahlen(betrag);
            } catch (RuntimeException rollbackFehler) {
                // Sollte praktisch nicht auftreten (Einzahlung validiert nur Betrag != null/negativ),
                // aber falls doch, bleibt als Schutz zumindest ein definierter Rueckgabewert.
            }
            return false;
        }
    }


    /**
     * Berechnet die Gesamtkontostände aller Kunden der Bank. Dabei werden die Kontostände
     * aller Konten eines Kunden summiert. Falls ein Konto einen negativen Kontostand aufweist,
     * wird dessen absoluter Betrag von der Gesamtsumme subtrahiert.
     *
     * @return Eine Map, die jeweils einen Kunden (Kunde) als Schlüssel und
     *         dessen Gesamtkontostand (Geldbetrag) als Wert enthält.
     */
    public Map<Kunde, Geldbetrag> getGesamtkontostaende() {
        Map<Kunde, Geldbetrag> result = new HashMap<>();
        for (Konto konto : konten.values()) {
            Kunde kunde = konto.getInhaber();
            Geldbetrag stand = konto.getKontostand();

            Geldbetrag summe = result.getOrDefault(kunde, Geldbetrag.NULL_EURO);
            Geldbetrag neu;
            if (stand.isNegativ()) {
                // Negative Staende als Subtraktion des absoluten Betrags behandeln
                Geldbetrag betragAbsolut = new Geldbetrag(Math.abs(stand.getBetrag()), stand.getWaehrung());
                neu = summe.minus(betragAbsolut);
            } else {
                neu = summe.plus(stand);
            }
            result.put(kunde, neu);
        }
        return result;
    }
}
