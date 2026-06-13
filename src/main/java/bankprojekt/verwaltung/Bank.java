package bankprojekt.verwaltung;
import java.time.LocalDate;
import java.time.MonthDay;
import java.time.Period;
import java.util.*;
import java.util.stream.Collectors;

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
     * Fügt ein (Mock-)Konto in die Kontenliste der Bank ein und liefert die
     * dabei vergebene Kontonummer zurück. Nur für Testzwecke!
     * * @param k Das einzufügende Konto (normalerweise ein Mock-Objekt)
     * @return Die vergebene Kontonummer
     */
    public long mockEinfuegen(Konto k) {
        long nummer = naechsteFreieKontonummer++;
        konten.put(nummer, k);
        return nummer;
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
        SortedSet<Kunde> kundenSet = new TreeSet<>((a, b) -> {
            // Absteigend nach Geburtstag: spaeteres Datum (juenger) kommt zuerst
            int cmpGeburtstag = b.getGeburtstag().compareTo(a.getGeburtstag());
            if (cmpGeburtstag != 0) {
                return cmpGeburtstag;
            }
            // Bei Gleichheit des Geburtstags: Name aufsteigend
            return a.getName().compareTo(b.getName());
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
        if (!(von instanceof UeberweisungsfaehigesKonto sender) || !(nach instanceof UeberweisungsfaehigesKonto empfaenger)) {
            // Mindestens eines der Konten ist nicht ueberweisungsfaehig
            return false;
        }

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


    /**
     * Löscht alle Konten eines bestimmten Kunden aus der Bank.
     * @param inhaber Der Kontoinhaber, dessen Konten gelöscht werden sollen. Darf nicht null sein.
     * @return Die Anzahl der gelöschten Konten.
     * @throws NullPointerException Falls der übergebene Kontoinhaber null ist.
     */
    public int kontenEinesKundenLoeschen(Kunde inhaber) throws NullPointerException {
        if (inhaber == null) {
            throw new NullPointerException("Inhaber darf nicht null sein");
        }
        int geloescht = 0;
        Iterator<Map.Entry<Long, Konto>> it = konten.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Long, Konto> entry = it.next();
            if (inhaber.equals(entry.getValue().getInhaber())) {
                it.remove();
                geloescht++;
            }
        }
        return geloescht;
    }

    // =====================================================================
//                                 Übung 7a
// =====================================================================

    /**
     * Liefert eine Liste aller Kunden, die mindestens ein Konto mit negativem
     * Kontostand haben. Kunden mit mehreren überzogenen Konten erscheinen nur einmal.
     * @return Liste der betroffenen Kunden (ohne Duplikate)
     */
    public List<Kunde> getKundenMitLeeremKonto() {
        return konten.values().stream()
                .filter(k -> k.getKontostand().isNegativ())   // nur Konten mit negativem Stand
                .map(Konto::getInhaber)                        // Inhaber extrahieren
                .distinct()                                    // Duplikate entfernen
                .collect(Collectors.toList());
    }

    /**
     * Liefert die Namen und Geburtstage aller Kunden der Bank, je Kunde eine Zeile.
     * Doppelte Kunden werden aussortiert.
     * Sortierung: aufsteigend nach Monat und Tag des Geburtstages (Geburtsjahr ignoriert).
     * @return Formatierter String, eine Zeile pro Kunde
     */
    public String getKundengeburtstage() {
        return konten.values().stream()
                .map(Konto::getInhaber)                                          // Inhaber holen
                .distinct()                                                      // Duplikate raus
                .sorted(Comparator.comparing(                                    // nach MonthDay sortieren
                        k -> MonthDay.from(k.getGeburtstag())))
                .map(k -> k.getName() + ": " + k.getGeburtstag())               // Zeile formatieren
                .collect(Collectors.joining(System.lineSeparator()));            // Zeilen verbinden
    }

    /**
     * Liefert die Anzahl der Kunden, die jetzt mindestens 67 Jahre alt sind.
     * Jeder Kunde wird nur einmal gezählt (auch wenn er mehrere Konten hat).
     * Tipp: Period.between(...).getYears() liefert das genaue Alter.
     * @return Anzahl der Senioren (≥ 67 Jahre)
     */
    public long getAnzahlSenioren() {
        return konten.values().stream()
                .map(Konto::getInhaber)                                          // Inhaber holen
                .distinct()                                                      // Duplikate raus
                .filter(k -> Period.between(k.getGeburtstag(), LocalDate.now())
                        .getYears() >= 67)                           // Alter ≥ 67
                .count();
    }

    /**
     * Zahlt auf EIN Konto jedes Kunden, der in diesem Kalenderjahr 18 wird, den
     * übergebenen Betrag ein. Hat ein solcher Kunde mehrere Konten, wird genau
     * eines davon (das erste gefundene) ausgewählt.
     * @param betrag der einzuzahlende Geldbetrag
     */
    public void schenkungAnNeuerwachsene(Geldbetrag betrag) {
        int aktuellesJahr = LocalDate.now().getYear();

        konten.values().stream()
                // Nur Konten von Kunden, die dieses Jahr 18 werden
                .filter(k -> k.getInhaber().getGeburtstag().getYear() == aktuellesJahr - 18)
                // Pro Kunde nur ein Konto: nach Inhaber gruppieren, jeweils erstes Konto nehmen
                .collect(Collectors.toMap(
                        Konto::getInhaber,          // Schlüssel: Kunde
                        k -> k,                     // Wert: Konto
                        (k1, k2) -> k1))            // bei Kollision: erstes behalten
                .values()
                // Einzahlung vornehmen
                .forEach(k -> k.einzahlen(betrag));
    }
}
