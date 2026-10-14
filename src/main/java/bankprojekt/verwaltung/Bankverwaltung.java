package bankprojekt.verwaltung;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serial;
import java.io.Serializable;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;

import bankprojekt.basisdaten.Geldbetrag;
import bankprojekt.basisdaten.Konto;
import bankprojekt.basisdaten.Waehrung;

/**
 * Die Bankverwaltung für die Mitarbeiter: verwaltet Kunden, Konten und Benutzer
 * einer {@link Bank} und führt ein Protokoll darüber, wer wann was getan hat.
 * <p>
 * Die {@link Bank} und die Basisdaten bleiben dabei unverändert: Kunden- und
 * Kontoakten ergänzen {@link bankprojekt.basisdaten.Kunde} und {@link Konto} um
 * Verwaltungsdaten, Konten entstehen weiterhin über
 * {@link Bank#kontoErstellen(bankprojekt.fabriken.Kontofabrik, bankprojekt.basisdaten.Kunde)}.
 * <p>
 * Jede ändernde Methode verlangt den ausführenden {@link Benutzer}. Er muss zu
 * dieser Verwaltung gehören und aktiv sein, sonst wird eine
 * {@link IllegalStateException} geworfen – ohne Anmeldung ist also nichts möglich.
 * Jede Änderung wird protokolliert. Ungültige Eingaben führen zu einer
 * {@link IllegalArgumentException}, unzulässige Aktionen (z. B. ein Konto mit
 * Guthaben auflösen) zu einer {@link IllegalStateException}; die Meldungen sind
 * zur Anzeige in der Oberfläche gedacht.
 * <p>
 * Die gesamte Verwaltung ist serialisierbar und wird vom {@link Datenspeicher}
 * nach jeder Änderung gespeichert.
 * @author m.alharethi
 */
public final class Bankverwaltung implements Serializable {
    /**
     * Versionsnummer für die Serialisierung.
     */
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Name, unter dem Aktionen ohne angemeldeten Benutzer protokolliert werden
     */
    static final String SYSTEM = "System";

    /**
     * die verwaltete Bank mit allen Konten
     */
    private final Bank bank;
    /**
     * Kundenakten nach Kundennummer
     */
    private final Map<String, Kundenakte> kunden = new LinkedHashMap<>();
    /**
     * Kontoakten nach Kontonummer
     */
    private final Map<Long, Kontoakte> konten = new LinkedHashMap<>();
    /**
     * Benutzer nach Benutzername (Kleinbuchstaben)
     */
    private final Map<String, Benutzer> benutzer = new LinkedHashMap<>();
    /**
     * Protokoll aller Aktionen, ältester Eintrag zuerst
     */
    private final List<Protokolleintrag> protokoll = new ArrayList<>();
    /**
     * laufende Nummer für die nächste Kundennummer
     */
    private long naechsteKundennummer = 1;

    /**
     * Uhr für Datum und Zeitstempel (in Tests austauschbar)
     */
    private transient Clock uhr;
    /**
     * wird nach jeder Änderung aufgerufen (z. B. zum Speichern), darf null sein
     */
    private transient Runnable beiAenderung;

    /**
     * Erstellt eine leere Verwaltung für die angegebene Bank mit der Systemuhr.
     * @param bank die zu verwaltende Bank
     * @throws NullPointerException wenn die Bank null ist
     */
    public Bankverwaltung(Bank bank) {
        this(bank, Clock.systemDefaultZone());
    }

    /**
     * Erstellt eine leere Verwaltung für die angegebene Bank.
     * @param bank die zu verwaltende Bank
     * @param uhr die Uhr für Datum und Zeitstempel
     * @throws NullPointerException wenn Bank oder Uhr null ist
     */
    public Bankverwaltung(Bank bank, Clock uhr) {
        this.bank = Objects.requireNonNull(bank, "Bank darf nicht null sein");
        this.uhr = Objects.requireNonNull(uhr, "Uhr darf nicht null sein");
    }

    // ------------------------------------------------------------------ Allgemeines

    /**
     * Liefert die Bankleitzahl der verwalteten Bank.
     * @return die Bankleitzahl
     */
    public long getBankleitzahl() {
        return bank.getBankleitzahl();
    }

    /**
     * Liefert die verwaltete Bank (nur für Paket und Tests, damit Änderungen
     * nicht am Protokoll vorbei geschehen).
     * @return die Bank
     */
    Bank getBank() {
        return bank;
    }

    /**
     * Liefert die Uhr der Verwaltung.
     * @return die Uhr
     */
    Clock getUhr() {
        return uhr;
    }

    /**
     * Setzt die Aktion, die nach jeder Änderung ausgeführt wird (z. B. Speichern).
     * @param beiAenderung die Aktion oder null für keine
     */
    void setBeiAenderung(Runnable beiAenderung) {
        this.beiAenderung = beiAenderung;
    }

    /**
     * Liefert das Protokoll.
     * @return nicht veränderbare Liste aller Einträge, ältester zuerst
     */
    public List<Protokolleintrag> getProtokoll() {
        return Collections.unmodifiableList(new ArrayList<>(protokoll));
    }

    /**
     * Fügt dem Protokoll einen Eintrag hinzu und meldet die Änderung.
     * @param benutzername wer die Aktion ausgeführt hat
     * @param aktion die Aktion
     * @param details nähere Beschreibung
     */
    void protokollieren(String benutzername, Protokollaktion aktion, String details) {
        protokoll.add(new Protokolleintrag(LocalDateTime.now(uhr), benutzername, aktion, details));
        geaendert();
    }

    /**
     * Ruft die Änderungsaktion auf, falls eine gesetzt ist.
     */
    private void geaendert() {
        if (beiAenderung != null)
            beiAenderung.run();
    }

    /**
     * Prüft, ob der ausführende Benutzer zu dieser Verwaltung gehört und aktiv ist.
     * @param akteur der ausführende Benutzer
     * @return der Benutzername des Akteurs (für das Protokoll)
     * @throws IllegalStateException wenn kein gültiger, aktiver Benutzer angemeldet ist
     */
    private String pruefeAkteur(Benutzer akteur) {
        if (akteur == null || benutzer.get(akteur.getBenutzername()) != akteur || !akteur.isAktiv())
            throw new IllegalStateException("Diese Aktion ist nur für angemeldete, aktive Benutzer erlaubt.");
        return akteur.getBenutzername();
    }

    /**
     * Bereitet einen Suchtext vor.
     * @param suchtext eingegebener Suchtext, darf null sein
     * @return der Suchtext in Kleinbuchstaben ohne Leerzeichen am Rand, "" für null
     */
    private static String suchbegriff(String suchtext) {
        return suchtext == null ? "" : suchtext.strip().toLowerCase(Locale.ROOT);
    }

    // ------------------------------------------------------------------ Kunden

    /**
     * Legt einen neuen, aktiven Kunden mit automatisch vergebener Kundennummer an.
     * @param akteur der ausführende Benutzer
     * @param daten die Stammdaten des Kunden
     * @return die neue Kundenakte
     * @throws IllegalStateException wenn der Akteur nicht angemeldet/aktiv ist
     * @throws NullPointerException wenn die Daten null sind
     */
    public Kundenakte kundeAnlegen(Benutzer akteur, Kundendaten daten) {
        String wer = pruefeAkteur(akteur);
        Objects.requireNonNull(daten, "Kundendaten dürfen nicht null sein");
        String kundennummer = String.format("K%06d", naechsteKundennummer);
        if (kunden.containsKey(kundennummer))
            throw new IllegalStateException("Die Kundennummer " + kundennummer + " ist bereits vergeben.");
        Kundenakte akte = new Kundenakte(kundennummer, daten, LocalDate.now(uhr));
        naechsteKundennummer++;
        kunden.put(kundennummer, akte);
        protokollieren(wer, Protokollaktion.KUNDE_ANGELEGT, "Kunde " + akte + " angelegt");
        return akte;
    }

    /**
     * Ändert die Stammdaten eines Kunden. Die Kundennummer bleibt unverändert.
     * @param akteur der ausführende Benutzer
     * @param kundennummer Kundennummer des Kunden
     * @param daten die neuen Stammdaten
     * @throws IllegalStateException wenn der Akteur nicht angemeldet/aktiv ist
     * @throws IllegalArgumentException wenn es den Kunden nicht gibt
     * @throws NullPointerException wenn die Daten null sind
     */
    public void kundeBearbeiten(Benutzer akteur, String kundennummer, Kundendaten daten) {
        String wer = pruefeAkteur(akteur);
        Objects.requireNonNull(daten, "Kundendaten dürfen nicht null sein");
        Kundenakte akte = kundeHolen(kundennummer);
        String aenderungen = aenderungenBeschreiben(akte.getKundendaten(), daten);
        akte.aktualisieren(daten);
        protokollieren(wer, Protokollaktion.KUNDE_BEARBEITET,
                "Kunde " + akte + " bearbeitet: " + aenderungen);
    }

    /**
     * Beschreibt, welche Felder sich zwischen alten und neuen Kundendaten unterscheiden.
     * @param alt die bisherigen Daten
     * @param neu die neuen Daten
     * @return z. B. "Adresse, E-Mail" oder "keine Änderungen"
     */
    private static String aenderungenBeschreiben(Kundendaten alt, Kundendaten neu) {
        StringJoiner felder = new StringJoiner(", ");
        if (!alt.vorname().equals(neu.vorname())) felder.add("Vorname");
        if (!alt.nachname().equals(neu.nachname())) felder.add("Nachname");
        if (!alt.geburtsdatum().equals(neu.geburtsdatum())) felder.add("Geburtsdatum");
        if (!alt.adresse().equals(neu.adresse())) felder.add("Adresse");
        if (!alt.telefon().equals(neu.telefon())) felder.add("Telefon");
        if (!alt.email().equals(neu.email())) felder.add("E-Mail");
        return felder.length() == 0 ? "keine Änderungen" : felder.toString();
    }

    /**
     * Deaktiviert (sperrt) einen Kunden. Er wird nicht gelöscht; seine Konten
     * bleiben unverändert, es können aber keine neuen Konten eröffnet werden.
     * @param akteur der ausführende Benutzer
     * @param kundennummer Kundennummer des Kunden
     * @throws IllegalStateException wenn der Akteur nicht angemeldet/aktiv ist oder
     *                               der Kunde bereits gesperrt ist
     * @throws IllegalArgumentException wenn es den Kunden nicht gibt
     */
    public void kundeDeaktivieren(Benutzer akteur, String kundennummer) {
        String wer = pruefeAkteur(akteur);
        Kundenakte akte = kundeHolen(kundennummer);
        if (!akte.isAktiv())
            throw new IllegalStateException("Der Kunde " + akte + " ist bereits gesperrt.");
        akte.setStatus(Kundenstatus.GESPERRT);
        protokollieren(wer, Protokollaktion.KUNDE_DEAKTIVIERT, "Kunde " + akte + " deaktiviert");
    }

    /**
     * Aktiviert einen gesperrten Kunden wieder.
     * @param akteur der ausführende Benutzer
     * @param kundennummer Kundennummer des Kunden
     * @throws IllegalStateException wenn der Akteur nicht angemeldet/aktiv ist oder
     *                               der Kunde bereits aktiv ist
     * @throws IllegalArgumentException wenn es den Kunden nicht gibt
     */
    public void kundeAktivieren(Benutzer akteur, String kundennummer) {
        String wer = pruefeAkteur(akteur);
        Kundenakte akte = kundeHolen(kundennummer);
        if (akte.isAktiv())
            throw new IllegalStateException("Der Kunde " + akte + " ist bereits aktiv.");
        akte.setStatus(Kundenstatus.AKTIV);
        protokollieren(wer, Protokollaktion.KUNDE_AKTIVIERT, "Kunde " + akte + " aktiviert");
    }

    /**
     * Sucht einen Kunden über seine Kundennummer.
     * @param kundennummer die Kundennummer
     * @return die Kundenakte, falls vorhanden
     */
    public Optional<Kundenakte> findeKunde(String kundennummer) {
        return Optional.ofNullable(kunden.get(kundennummer));
    }

    /**
     * Liefert einen Kunden oder meldet einen Fehler.
     * @param kundennummer die Kundennummer
     * @return die Kundenakte
     * @throws IllegalArgumentException wenn es den Kunden nicht gibt
     */
    private Kundenakte kundeHolen(String kundennummer) {
        return findeKunde(kundennummer).orElseThrow(() ->
                new IllegalArgumentException("Einen Kunden mit der Nummer " + kundennummer + " gibt es nicht."));
    }

    /**
     * Liefert alle Kunden.
     * @return nicht veränderbare Liste aller Kunden, nach Kundennummer sortiert
     */
    public List<Kundenakte> getKunden() {
        return kundenSuchen(null, null);
    }

    /**
     * Sucht Kunden nach Name oder Kundennummer und filtert nach Status.
     * @param suchtext Teil des Namens ("Max", "Mustermann", "Mustermann, Max") oder
     *                 der Kundennummer, Groß-/Kleinschreibung egal; null oder leer findet alle
     * @param status nur Kunden mit diesem Status; null für alle
     * @return nicht veränderbare Liste der passenden Kunden, nach Kundennummer sortiert
     */
    public List<Kundenakte> kundenSuchen(String suchtext, Kundenstatus status) {
        String begriff = suchbegriff(suchtext);
        return kunden.values().stream()
                .filter(k -> status == null || k.getStatus() == status)
                .filter(k -> begriff.isEmpty()
                        || k.getKundennummer().toLowerCase(Locale.ROOT).contains(begriff)
                        || k.getName().toLowerCase(Locale.ROOT).contains(begriff)
                        || (k.getVorname() + " " + k.getNachname()).toLowerCase(Locale.ROOT).contains(begriff))
                .sorted(Comparator.comparing(Kundenakte::getKundennummer))
                .toList();
    }

    // ------------------------------------------------------------------ Konten

    /**
     * Eröffnet ein neues Konto für einen aktiven Kunden. Das Konto wird über die
     * passende Kontofabrik in der Bank erzeugt, erhält dort eine eindeutige
     * Kontonummer und beginnt mit dem Kontostand 0 in der gewählten Währung.
     * @param akteur der ausführende Benutzer
     * @param kundennummer Kundennummer des Inhabers
     * @param kontoart Art des Kontos
     * @param waehrung Währung, in der das Konto geführt wird
     * @param dispo Dispo in der Kontowährung; nur für ein Girokonto (muss dann größer 0
     *              sein), sonst ohne Bedeutung
     * @return die neue Kontoakte
     * @throws IllegalStateException wenn der Akteur nicht angemeldet/aktiv ist oder der
     *                               Kunde gesperrt ist
     * @throws IllegalArgumentException wenn es den Kunden nicht gibt, Kontoart oder
     *                                  Währung fehlen oder der Dispo ungültig ist
     */
    public Kontoakte kontoEroeffnen(Benutzer akteur, String kundennummer, Kontoart kontoart,
                                    Waehrung waehrung, Geldbetrag dispo) {
        String wer = pruefeAkteur(akteur);
        Kundenakte kunde = kundeHolen(kundennummer);
        if (kontoart == null)
            throw new IllegalArgumentException("Bitte eine Kontoart auswählen.");
        if (waehrung == null)
            throw new IllegalArgumentException("Bitte eine Währung auswählen.");
        if (!kunde.isAktiv())
            throw new IllegalStateException("Für den gesperrten Kunden " + kunde
                    + " können keine Konten eröffnet werden.");
        long kontonummer = bank.kontoErstellen(kontoart.fabrik(dispo), kunde.getKunde());
        if (konten.containsKey(kontonummer)) {
            // kann nur bei inkonsistenten Daten auftreten; das neue Konto wieder entfernen
            bank.kontoLoeschen(kontonummer);
            throw new IllegalStateException("Die Kontonummer " + kontonummer + " ist bereits vergeben.");
        }
        Konto konto = bank.getKonten().get(kontonummer);
        konto.waehrungswechsel(waehrung);
        Kontoakte akte = new Kontoakte(konto, kundennummer, kontoart, bank.getBankleitzahl(), LocalDate.now(uhr));
        konten.put(kontonummer, akte);
        protokollieren(wer, Protokollaktion.KONTO_EROEFFNET, kontoart + " " + akte.getIbanFormatiert()
                + " (" + waehrung + ") für Kunde " + kunde + " eröffnet");
        return akte;
    }

    /**
     * Sperrt ein Konto; danach sind keine Buchungen mehr möglich.
     * @param akteur der ausführende Benutzer
     * @param kontonummer die Kontonummer
     * @throws IllegalStateException wenn der Akteur nicht angemeldet/aktiv ist oder das
     *                               Konto nicht aktiv ist
     * @throws IllegalArgumentException wenn es das Konto nicht gibt
     */
    public void kontoSperren(Benutzer akteur, long kontonummer) {
        String wer = pruefeAkteur(akteur);
        Kontoakte akte = kontoHolen(kontonummer);
        if (akte.getStatus() != Kontostatus.AKTIV)
            throw new IllegalStateException("Das Konto " + akte.getIbanFormatiert() + " ist "
                    + akte.getStatus() + " und kann nicht gesperrt werden.");
        akte.getKonto().sperren();
        protokollieren(wer, Protokollaktion.KONTO_GESPERRT, "Konto " + akte.getIbanFormatiert() + " gesperrt");
    }

    /**
     * Entsperrt ein gesperrtes Konto.
     * @param akteur der ausführende Benutzer
     * @param kontonummer die Kontonummer
     * @throws IllegalStateException wenn der Akteur nicht angemeldet/aktiv ist oder das
     *                               Konto nicht gesperrt (bzw. aufgelöst) ist
     * @throws IllegalArgumentException wenn es das Konto nicht gibt
     */
    public void kontoEntsperren(Benutzer akteur, long kontonummer) {
        String wer = pruefeAkteur(akteur);
        Kontoakte akte = kontoHolen(kontonummer);
        if (akte.getStatus() != Kontostatus.GESPERRT)
            throw new IllegalStateException("Das Konto " + akte.getIbanFormatiert() + " ist "
                    + akte.getStatus() + " und kann nicht entsperrt werden.");
        akte.getKonto().entsperren();
        protokollieren(wer, Protokollaktion.KONTO_ENTSPERRT, "Konto " + akte.getIbanFormatiert() + " entsperrt");
    }

    /**
     * Löst ein Konto auf. Das ist nur bei einem Kontostand von genau 0 möglich.
     * Das Konto wird dauerhaft gesperrt und bleibt zur Nachvollziehbarkeit erhalten.
     * @param akteur der ausführende Benutzer
     * @param kontonummer die Kontonummer
     * @throws IllegalStateException wenn der Akteur nicht angemeldet/aktiv ist, das Konto
     *                               schon aufgelöst ist oder der Kontostand nicht 0 ist
     * @throws IllegalArgumentException wenn es das Konto nicht gibt
     */
    public void kontoAufloesen(Benutzer akteur, long kontonummer) {
        String wer = pruefeAkteur(akteur);
        Kontoakte akte = kontoHolen(kontonummer);
        if (akte.getStatus() == Kontostatus.AUFGELOEST)
            throw new IllegalStateException("Das Konto " + akte.getIbanFormatiert() + " ist bereits aufgelöst.");
        if (akte.getKontostand().compareTo(Geldbetrag.NULL_EURO) != 0)
            throw new IllegalStateException("Das Konto " + akte.getIbanFormatiert()
                    + " kann nur bei einem Kontostand von 0 aufgelöst werden (aktuell "
                    + akte.getKontostand() + ").");
        akte.aufloesen(LocalDate.now(uhr));
        protokollieren(wer, Protokollaktion.KONTO_AUFGELOEST, "Konto " + akte.getIbanFormatiert() + " aufgelöst");
    }

    /**
     * Sucht ein Konto über seine Kontonummer.
     * @param kontonummer die Kontonummer
     * @return die Kontoakte, falls vorhanden
     */
    public Optional<Kontoakte> findeKonto(long kontonummer) {
        return Optional.ofNullable(konten.get(kontonummer));
    }

    /**
     * Liefert ein Konto oder meldet einen Fehler.
     * @param kontonummer die Kontonummer
     * @return die Kontoakte
     * @throws IllegalArgumentException wenn es das Konto nicht gibt
     */
    private Kontoakte kontoHolen(long kontonummer) {
        return findeKonto(kontonummer).orElseThrow(() ->
                new IllegalArgumentException("Ein Konto mit der Nummer " + kontonummer + " gibt es nicht."));
    }

    /**
     * Liefert alle Konten.
     * @return nicht veränderbare Liste aller Konten, nach Kontonummer sortiert
     */
    public List<Kontoakte> getKonten() {
        return kontenSuchen(null, null, null);
    }

    /**
     * Liefert alle Konten eines Kunden (auch aufgelöste).
     * @param kundennummer die Kundennummer
     * @return nicht veränderbare Liste seiner Konten, nach Kontonummer sortiert
     */
    public List<Kontoakte> getKontenVon(String kundennummer) {
        return kontenSuchen(null, kundennummer, null);
    }

    /**
     * Sucht Konten nach Kontonummer, IBAN oder Inhaber und filtert nach Kunde und Status.
     * <p>
     * Besteht der Suchtext nur aus Ziffern, ist er eine Kontonummer und muss genau
     * passen (führende Nullen egal) – sonst würde z. B. "2" jedes Konto finden, weil
     * die Ziffer in der Bankleitzahl jeder IBAN vorkommt. Jeder andere Suchtext findet
     * Konten, deren IBAN ihn enthält (Leerzeichen egal) oder deren Inhabername bzw.
     * Kundennummer ihn enthält.
     * @param suchtext Kontonummer, Teil der IBAN, des Inhabernamens oder der
     *                 Kundennummer (Groß-/Kleinschreibung egal); null oder leer findet alle
     * @param kundennummer nur Konten dieses Kunden; null für alle
     * @param status nur Konten mit diesem Status; null für alle
     * @return nicht veränderbare Liste der passenden Konten, nach Kontonummer sortiert
     */
    public List<Kontoakte> kontenSuchen(String suchtext, String kundennummer, Kontostatus status) {
        String begriff = suchbegriff(suchtext);
        String kompakt = begriff.replace(" ", "");
        boolean nurZiffern = !kompakt.isEmpty() && kompakt.chars().allMatch(Character::isDigit);
        // führende Nullen entfernen (mindestens eine Ziffer bleibt stehen)
        String kontonummer = kompakt.replaceFirst("^0+(?=.)", "");
        return konten.values().stream()
                .filter(k -> kundennummer == null || k.getKundennummer().equals(kundennummer))
                .filter(k -> status == null || k.getStatus() == status)
                .filter(k -> begriff.isEmpty()
                        || (nurZiffern
                            ? Long.toString(k.getKontonummer()).equals(kontonummer)
                            : k.getIban().toLowerCase(Locale.ROOT).contains(kompakt)
                              || k.getInhaberName().toLowerCase(Locale.ROOT).contains(begriff)
                              || k.getKundennummer().toLowerCase(Locale.ROOT).contains(begriff)))
                .sorted(Comparator.comparingLong(Kontoakte::getKontonummer))
                .toList();
    }

    // ------------------------------------------------------------------ Benutzer

    /**
     * Richtet den allerersten Benutzer ein. Das ist nur möglich, solange es noch
     * keinen Benutzer gibt (sonst bräuchte man einen angemeldeten Benutzer).
     * @param benutzername der Benutzername
     * @param anzeigename der Anzeigename
     * @param passwortHash Hash des Passworts (siehe {@link Passwortschutz#hashen(char[])})
     * @return der neue Benutzer
     * @throws IllegalStateException wenn es schon Benutzer gibt
     * @throws IllegalArgumentException bei ungültigem Benutzer- oder Anzeigenamen
     */
    Benutzer erstenBenutzerEinrichten(String benutzername, String anzeigename, String passwortHash) {
        if (!benutzer.isEmpty())
            throw new IllegalStateException("Es gibt bereits Benutzer.");
        Benutzer neu = new Benutzer(Eingabepruefung.benutzername(benutzername),
                Eingabepruefung.pflichtfeld(anzeigename, "Anzeigename"),
                Objects.requireNonNull(passwortHash), LocalDateTime.now(uhr));
        benutzer.put(neu.getBenutzername(), neu);
        protokollieren(SYSTEM, Protokollaktion.ERSTEINRICHTUNG, "Startbenutzer " + neu + " eingerichtet");
        return neu;
    }

    /**
     * Legt einen neuen, aktiven Benutzer an.
     * @param akteur der ausführende Benutzer
     * @param benutzername der gewünschte Benutzername (wird in Kleinbuchstaben gespeichert)
     * @param anzeigename der angezeigte Name
     * @param passwort das Startpasswort (mindestens 4 Zeichen)
     * @return der neue Benutzer
     * @throws IllegalStateException wenn der Akteur nicht angemeldet/aktiv ist
     * @throws IllegalArgumentException bei ungültigen Angaben oder wenn der Benutzername
     *                                  schon vergeben ist
     */
    public Benutzer benutzerAnlegen(Benutzer akteur, String benutzername, String anzeigename, char[] passwort) {
        String wer = pruefeAkteur(akteur);
        String name = Eingabepruefung.benutzername(benutzername);
        String anzeige = Eingabepruefung.pflichtfeld(anzeigename, "Anzeigename");
        Eingabepruefung.passwort(passwort);
        if (benutzer.containsKey(name))
            throw new IllegalArgumentException("Der Benutzername \"" + name + "\" ist bereits vergeben.");
        Benutzer neu = new Benutzer(name, anzeige, Passwortschutz.hashen(passwort), LocalDateTime.now(uhr));
        benutzer.put(name, neu);
        protokollieren(wer, Protokollaktion.BENUTZER_ANGELEGT, "Benutzer " + neu + " angelegt");
        return neu;
    }

    /**
     * Ändert den Anzeigenamen eines Benutzers. Der Benutzername selbst ist unveränderlich.
     * @param akteur der ausführende Benutzer
     * @param benutzername der Benutzername des zu ändernden Benutzers
     * @param anzeigename der neue Anzeigename
     * @throws IllegalStateException wenn der Akteur nicht angemeldet/aktiv ist
     * @throws IllegalArgumentException wenn es den Benutzer nicht gibt oder der
     *                                  Anzeigename leer ist
     */
    public void benutzerBearbeiten(Benutzer akteur, String benutzername, String anzeigename) {
        String wer = pruefeAkteur(akteur);
        Benutzer ziel = benutzerHolen(benutzername);
        String anzeige = Eingabepruefung.pflichtfeld(anzeigename, "Anzeigename");
        String alt = ziel.getAnzeigename();
        ziel.setAnzeigename(anzeige);
        protokollieren(wer, Protokollaktion.BENUTZER_BEARBEITET, "Benutzer " + ziel.getBenutzername()
                + ": Anzeigename \"" + alt + "\" → \"" + anzeige + "\"");
    }

    /**
     * Setzt das Passwort eines Benutzers neu.
     * @param akteur der ausführende Benutzer
     * @param benutzername der Benutzername des Benutzers
     * @param neuesPasswort das neue Passwort (mindestens 4 Zeichen)
     * @throws IllegalStateException wenn der Akteur nicht angemeldet/aktiv ist
     * @throws IllegalArgumentException wenn es den Benutzer nicht gibt oder das Passwort
     *                                  zu kurz ist
     */
    public void passwortZuruecksetzen(Benutzer akteur, String benutzername, char[] neuesPasswort) {
        String wer = pruefeAkteur(akteur);
        Benutzer ziel = benutzerHolen(benutzername);
        Eingabepruefung.passwort(neuesPasswort);
        ziel.setPasswortHash(Passwortschutz.hashen(neuesPasswort));
        protokollieren(wer, Protokollaktion.PASSWORT_ZURUECKGESETZT,
                "Passwort von " + ziel.getBenutzername() + " zurückgesetzt");
    }

    /**
     * Deaktiviert einen Benutzer; er kann sich danach nicht mehr anmelden. Man kann
     * sich nicht selbst deaktivieren, und der letzte aktive Benutzer bleibt aktiv,
     * damit sich immer jemand anmelden kann.
     * @param akteur der ausführende Benutzer
     * @param benutzername der Benutzername des Benutzers
     * @throws IllegalStateException wenn der Akteur nicht angemeldet/aktiv ist, sich
     *                               selbst deaktivieren will oder der Benutzer schon
     *                               deaktiviert ist
     * @throws IllegalArgumentException wenn es den Benutzer nicht gibt
     */
    public void benutzerDeaktivieren(Benutzer akteur, String benutzername) {
        String wer = pruefeAkteur(akteur);
        Benutzer ziel = benutzerHolen(benutzername);
        if (ziel == akteur)
            throw new IllegalStateException("Sie können sich nicht selbst deaktivieren.");
        if (!ziel.isAktiv())
            throw new IllegalStateException("Der Benutzer " + ziel + " ist bereits deaktiviert.");
        // Da der Akteur selbst aktiv ist und nicht deaktiviert werden kann, bleibt
        // immer mindestens ein aktiver Benutzer übrig.
        ziel.setAktiv(false);
        protokollieren(wer, Protokollaktion.BENUTZER_DEAKTIVIERT, "Benutzer " + ziel + " deaktiviert");
    }

    /**
     * Aktiviert einen deaktivierten Benutzer wieder.
     * @param akteur der ausführende Benutzer
     * @param benutzername der Benutzername des Benutzers
     * @throws IllegalStateException wenn der Akteur nicht angemeldet/aktiv ist oder der
     *                               Benutzer schon aktiv ist
     * @throws IllegalArgumentException wenn es den Benutzer nicht gibt
     */
    public void benutzerAktivieren(Benutzer akteur, String benutzername) {
        String wer = pruefeAkteur(akteur);
        Benutzer ziel = benutzerHolen(benutzername);
        if (ziel.isAktiv())
            throw new IllegalStateException("Der Benutzer " + ziel + " ist bereits aktiv.");
        ziel.setAktiv(true);
        protokollieren(wer, Protokollaktion.BENUTZER_AKTIVIERT, "Benutzer " + ziel + " aktiviert");
    }

    /**
     * Vermerkt eine erfolgreiche Anmeldung am Benutzer und im Protokoll.
     * @param angemeldet der angemeldete Benutzer
     */
    void anmeldungVermerken(Benutzer angemeldet) {
        angemeldet.setLetzteAnmeldung(LocalDateTime.now(uhr));
        protokollieren(angemeldet.getBenutzername(), Protokollaktion.ANMELDUNG, "Erfolgreich angemeldet");
    }

    /**
     * Sucht einen Benutzer über seinen Benutzernamen (Groß-/Kleinschreibung egal).
     * @param benutzername der Benutzername
     * @return der Benutzer, falls vorhanden
     */
    public Optional<Benutzer> findeBenutzer(String benutzername) {
        if (benutzername == null)
            return Optional.empty();
        return Optional.ofNullable(benutzer.get(benutzername.strip().toLowerCase(Locale.ROOT)));
    }

    /**
     * Liefert einen Benutzer oder meldet einen Fehler.
     * @param benutzername der Benutzername
     * @return der Benutzer
     * @throws IllegalArgumentException wenn es den Benutzer nicht gibt
     */
    private Benutzer benutzerHolen(String benutzername) {
        return findeBenutzer(benutzername).orElseThrow(() ->
                new IllegalArgumentException("Einen Benutzer \"" + benutzername + "\" gibt es nicht."));
    }

    /**
     * Liefert alle Benutzer.
     * @return nicht veränderbare Liste aller Benutzer, nach Benutzername sortiert
     */
    public List<Benutzer> getBenutzer() {
        return benutzer.values().stream()
                .sorted(Comparator.comparing(Benutzer::getBenutzername))
                .toList();
    }

    /**
     * Stellt nach dem Einlesen die nicht gespeicherte Uhr wieder her.
     * @param in der Eingabestrom
     * @throws IOException bei einem Fehler beim Lesen
     * @throws ClassNotFoundException falls eine Klasse nicht gefunden wird
     */
    @Serial
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        this.uhr = Clock.systemDefaultZone();
    }
}
