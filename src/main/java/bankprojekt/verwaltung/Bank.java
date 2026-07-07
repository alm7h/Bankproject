package bankprojekt.verwaltung;
// Übung 10: Imports für die Serialisierung der Bank (speichern/einlesen).
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.MonthDay;
import java.time.Period;
import java.util.*;
import java.util.stream.Collectors;

import bankprojekt.basisdaten.Kunde;
import bankprojekt.basisdaten.Konto;
import bankprojekt.basisdaten.Geldbetrag;
import bankprojekt.basisdaten.UeberweisungsfaehigesKonto;
import bankprojekt.exceptions.GesperrtException;
import bankprojekt.fabriken.Kontofabrik;

/**
 * Verwaltet Konten und Kunden einer Bank unter Verwendung von Java Collections.
 */
// Übung 10: Serializable ergänzt, damit das gesamte Bank-Objekt über
// die Methoden speichern(...)/einlesen(...) gespeichert und geladen werden kann.
public class Bank implements Serializable {

    /**
     * Versionsnummer für die Serialisierung.
     */
    private static final long serialVersionUID = 1L;

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

    // Übung 10: neue Methode zum Speichern der kompletten Bank.
    /**
     * Speichert das komplette Bank-Objekt mit allen seinen Informationen
     * (Bankleitzahl, sämtliche Konten samt Inhabern und Kontoständen) in den
     * angegebenen Strom.
     * <p>
     * Die Speicherung erfolgt über die Java-Serialisierung. Dadurch wird beim
     * Einlesen automatisch der tatsächliche Laufzeittyp jedes Kontos
     * wiederhergestellt. Die Bank kann deshalb ohne Anpassung dieser Methode um
     * weitere Kontotypen (z.B. Aktienkonten) erweitert werden.
     *
     * @param ziel der Strom, in den das Bank-Objekt geschrieben wird
     * @throws IOException wenn beim Schreiben etwas schiefgeht
     */
    public void speichern(OutputStream ziel) throws IOException {
        // Übung 10: Der ObjectOutputStream wird bewusst NICHT geschlossen,
        // damit der übergebene Strom offen bleibt und weiterverwendet werden kann
        // (z.B. um mehrere Banken in verschiedene Einträge eines ZipOutputStreams
        // zu schreiben). flush() stellt sicher, dass alle Bytes tatsächlich in den
        // Zielstrom geschrieben werden, bevor z.B. closeEntry() aufgerufen wird.
        ObjectOutputStream oos = new ObjectOutputStream(ziel);
        oos.writeObject(this);
        oos.flush();
    }

    // Übung 10: neue Methode zum Einlesen einer gespeicherten Bank.
    /**
     * Liest aus der angegebenen Quelle ein zuvor mit {@link #speichern(OutputStream)}
     * gespeichertes Bank-Objekt ein.
     * <p>
     * Geht beim Einlesen etwas schief (z.B. fehlerhafte oder leere Quelle,
     * unbekannte Klasse), wird statt einer Exception eine leere Bank mit der
     * Bankleitzahl 0 zurückgegeben.
     *
     * @param quelle der Strom, aus dem das Bank-Objekt gelesen wird
     * @return die eingelesene Bank oder, im Fehlerfall, eine leere Bank mit Bankleitzahl 0
     */
    public static Bank einlesen(InputStream quelle) {
        try {
            // Übung 10: Der ObjectInputStream wird bewusst NICHT geschlossen,
            // damit der übergebene Strom offen bleibt (z.B. ein ZipInputStream, aus dem
            // anschließend per getNextEntry() der nächste Eintrag gelesen werden soll).
            ObjectInputStream ois = new ObjectInputStream(quelle);
            return (Bank) ois.readObject();
        } catch (Exception e) {
            // Im Fehlerfall (IOException, ClassNotFoundException, ClassCastException, ...)
            // wird laut Vorgabe eine leere Bank mit Bankleitzahl 0 geliefert.
            return new Bank(0);
        }
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
     * Übung 11 b): Erstellt mit Hilfe der übergebenen {@link Kontofabrik} ein neues
     * Konto für den angegebenen Inhaber, vergibt dafür eine neue Kontonummer und
     * speichert das Konto in der Bankverwaltung.
     * <p>
     * Diese Methode ersetzt im Sinne des Abstract-Factory-Musters die früheren
     * Methoden {@code girokontoErstellen}, {@code sparbuchErstellen} und
     * {@code mockEinfuegen}: Welcher Kontotyp entsteht, bestimmt allein die Fabrik;
     * die Bank ist nur noch für Nummernvergabe und Speicherung zuständig.
     *
     * @param fabrik die Fabrik, die das gewünschte Konto erzeugt
     * @param inhaber der Kontoinhaber
     * @return die neu vergebene Kontonummer
     * @throws NullPointerException falls die Fabrik oder der Inhaber null ist
     */
    public long kontoErstellen(Kontofabrik fabrik, Kunde inhaber) throws NullPointerException {
        if (fabrik == null) throw new NullPointerException("Fabrik ist null");
        if (inhaber == null) throw new NullPointerException("Kunde ist null");
        long kontonummer = naechsteFreieKontonummer++;
        Konto konto = fabrik.erstellen(inhaber, kontonummer);
        konten.put(kontonummer, konto);
        return kontonummer;
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
