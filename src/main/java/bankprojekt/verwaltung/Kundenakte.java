package bankprojekt.verwaltung;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

import bankprojekt.basisdaten.Kunde;

/**
 * Die Akte eines Kunden in der {@link Bankverwaltung}. Sie ergänzt den
 * {@link Kunde} aus den Basisdaten (Name, Geburtsdatum, Adresse) um die
 * Verwaltungsdaten Kundennummer, Telefon, E-Mail und Status. Der {@link Kunde}
 * ist zugleich Inhaber aller Konten des Kunden; Änderungen an Name oder Adresse
 * sind deshalb sofort auch an den Konten sichtbar.
 * <p>
 * Geändert wird eine Akte nur über die {@link Bankverwaltung} (die dabei
 * protokolliert); von außen ist sie nur lesbar.
 * @author m.alharethi
 */
public final class Kundenakte implements Serializable {
    /**
     * Versionsnummer für die Serialisierung.
     */
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * eindeutige, automatisch vergebene Kundennummer, z. B. "K000001"
     */
    private final String kundennummer;
    /**
     * der Kunde aus den Basisdaten, zugleich Inhaber der Konten
     */
    private final Kunde kunde;
    /**
     * Telefonnummer, "" wenn keine angegeben ist
     */
    private String telefon;
    /**
     * E-Mail-Adresse
     */
    private String email;
    /**
     * aktueller Status
     */
    private Kundenstatus status = Kundenstatus.AKTIV;
    /**
     * Tag der Anlage
     */
    private final LocalDate angelegtAm;

    /**
     * Legt eine neue, aktive Kundenakte an.
     * @param kundennummer die vergebene Kundennummer
     * @param daten die (bereits geprüften) Stammdaten
     * @param angelegtAm Tag der Anlage
     */
    Kundenakte(String kundennummer, Kundendaten daten, LocalDate angelegtAm) {
        this.kundennummer = kundennummer;
        this.kunde = new Kunde(daten.vorname(), daten.nachname(), daten.adresse(), daten.geburtsdatum());
        this.telefon = daten.telefon();
        this.email = daten.email();
        this.angelegtAm = angelegtAm;
    }

    /**
     * Übernimmt geänderte Stammdaten.
     * @param daten die (bereits geprüften) neuen Stammdaten
     */
    void aktualisieren(Kundendaten daten) {
        kunde.setVorname(daten.vorname());
        kunde.setNachname(daten.nachname());
        kunde.setGeburtstag(daten.geburtsdatum());
        kunde.setAdresse(daten.adresse());
        this.telefon = daten.telefon();
        this.email = daten.email();
    }

    /**
     * Setzt den Status.
     * @param status der neue Status
     */
    void setStatus(Kundenstatus status) {
        this.status = status;
    }

    /**
     * Liefert den Kunden aus den Basisdaten (Inhaber der Konten).
     * @return der Kunde
     */
    Kunde getKunde() {
        return kunde;
    }

    /**
     * Liefert die Kundennummer.
     * @return die Kundennummer, z. B. "K000001"
     */
    public String getKundennummer() {
        return kundennummer;
    }

    /**
     * Liefert den Vornamen.
     * @return der Vorname
     */
    public String getVorname() {
        return kunde.getVorname();
    }

    /**
     * Liefert den Nachnamen.
     * @return der Nachname
     */
    public String getNachname() {
        return kunde.getNachname();
    }

    /**
     * Liefert den vollständigen Namen in der Form "Nachname, Vorname".
     * @return der vollständige Name
     */
    public String getName() {
        return kunde.getName();
    }

    /**
     * Liefert das Geburtsdatum.
     * @return das Geburtsdatum
     */
    public LocalDate getGeburtsdatum() {
        return kunde.getGeburtstag();
    }

    /**
     * Liefert die Adresse.
     * @return die Adresse
     */
    public String getAdresse() {
        return kunde.getAdresse();
    }

    /**
     * Liefert die Telefonnummer.
     * @return die Telefonnummer, "" wenn keine angegeben ist
     */
    public String getTelefon() {
        return telefon;
    }

    /**
     * Liefert die E-Mail-Adresse.
     * @return die E-Mail-Adresse
     */
    public String getEmail() {
        return email;
    }

    /**
     * Liefert den Status.
     * @return der aktuelle Status
     */
    public Kundenstatus getStatus() {
        return status;
    }

    /**
     * Gibt an, ob der Kunde aktiv ist.
     * @return true, wenn der Status {@link Kundenstatus#AKTIV} ist
     */
    public boolean isAktiv() {
        return status == Kundenstatus.AKTIV;
    }

    /**
     * Liefert den Tag der Anlage.
     * @return der Tag, an dem der Kunde angelegt wurde
     */
    public LocalDate getAngelegtAm() {
        return angelegtAm;
    }

    /**
     * Liefert die aktuellen Stammdaten, z. B. zum Vorbelegen eines Bearbeitungsformulars.
     * @return die Stammdaten des Kunden
     */
    public Kundendaten getKundendaten() {
        return new Kundendaten(getVorname(), getNachname(), getGeburtsdatum(),
                getAdresse(), telefon, email);
    }

    /**
     * Liefert Kundennummer und Namen, z. B. für Auswahllisten.
     * @return z. B. "K000001 – Mustermann, Max"
     */
    @Override
    public String toString() {
        return kundennummer + " – " + getName();
    }
}
