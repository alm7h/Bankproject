package bankprojekt.verwaltung;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

import bankprojekt.basisdaten.Waehrung;

/**
 * Ein Antrag auf ein Konto, gestellt im Kundenportal und von den Mitarbeitern in der
 * Verwaltung genehmigt oder abgelehnt.
 * <p>
 * Ein {@link Antragsart#NEUKUNDE Neukundenantrag} enthält die Stammdaten des künftigen
 * Kunden; bei der Genehmigung werden Kunde und Konto angelegt. Ein Antrag auf ein
 * {@link Antragsart#ZUSATZKONTO weiteres Konto} gehört zu einem bestehenden Kunden.
 * Geändert wird ein Antrag nur über die {@link Bankverwaltung}.
 * @author m.alharethi
 */
public final class Kontoantrag implements Serializable {
    /**
     * Versionsnummer für die Serialisierung.
     */
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * eindeutige Antragsnummer, z. B. "A000001"
     */
    private final String antragsnummer;
    /**
     * Art des Antrags
     */
    private final Antragsart art;
    /**
     * Benutzername des Kundenzugangs, über den der Antrag gestellt wurde
     */
    private final String benutzername;
    /**
     * Name des Antragstellers zum Zeitpunkt des Antrags ("Nachname, Vorname")
     */
    private final String antragsteller;
    /**
     * Stammdaten des künftigen Kunden; nur bei einem Neukundenantrag, sonst null
     */
    private final Kundendaten kundendaten;
    /**
     * Kundennummer: beim weiteren Konto von Anfang an, beim Neukunden nach der Genehmigung
     */
    private String kundennummer;
    /**
     * gewünschte Kontoart
     */
    private final Kontoart kontoart;
    /**
     * gewünschte Währung
     */
    private final Waehrung waehrung;
    /**
     * Zeitpunkt des Antrags
     */
    private final LocalDateTime eingereichtAm;
    /**
     * Bearbeitungsstand
     */
    private Antragsstatus status = Antragsstatus.OFFEN;
    /**
     * Benutzername des Mitarbeiters, der entschieden hat
     */
    private String bearbeitetVon;
    /**
     * Zeitpunkt der Entscheidung
     */
    private LocalDateTime bearbeitetAm;
    /**
     * Begründung einer Ablehnung
     */
    private String begruendung;
    /**
     * Nummer des eröffneten Kontos nach der Genehmigung
     */
    private Long kontonummer;

    /**
     * Legt einen offenen Antrag an.
     * @param antragsnummer die vergebene Antragsnummer
     * @param art Art des Antrags
     * @param benutzername Benutzername des Antragstellers
     * @param antragsteller Name des Antragstellers
     * @param kundendaten Stammdaten (nur Neukunde, sonst null)
     * @param kundennummer Kundennummer (nur weiteres Konto, sonst null)
     * @param kontoart gewünschte Kontoart
     * @param waehrung gewünschte Währung
     * @param eingereichtAm Zeitpunkt des Antrags
     */
    Kontoantrag(String antragsnummer, Antragsart art, String benutzername, String antragsteller,
                Kundendaten kundendaten, String kundennummer, Kontoart kontoart, Waehrung waehrung,
                LocalDateTime eingereichtAm) {
        this.antragsnummer = antragsnummer;
        this.art = art;
        this.benutzername = benutzername;
        this.antragsteller = antragsteller;
        this.kundendaten = kundendaten;
        this.kundennummer = kundennummer;
        this.kontoart = kontoart;
        this.waehrung = waehrung;
        this.eingereichtAm = eingereichtAm;
    }

    /**
     * Vermerkt die Genehmigung.
     * @param mitarbeiter wer genehmigt hat
     * @param zeitpunkt wann
     * @param kundennummer Kundennummer des (ggf. neu angelegten) Kunden
     * @param kontonummer Nummer des eröffneten Kontos
     */
    void genehmigen(String mitarbeiter, LocalDateTime zeitpunkt, String kundennummer, long kontonummer) {
        this.status = Antragsstatus.GENEHMIGT;
        this.bearbeitetVon = mitarbeiter;
        this.bearbeitetAm = zeitpunkt;
        this.kundennummer = kundennummer;
        this.kontonummer = kontonummer;
    }

    /**
     * Vermerkt die Ablehnung.
     * @param mitarbeiter wer abgelehnt hat
     * @param zeitpunkt wann
     * @param begruendung warum
     */
    void ablehnen(String mitarbeiter, LocalDateTime zeitpunkt, String begruendung) {
        this.status = Antragsstatus.ABGELEHNT;
        this.bearbeitetVon = mitarbeiter;
        this.bearbeitetAm = zeitpunkt;
        this.begruendung = begruendung;
    }

    /**
     * Liefert die Antragsnummer.
     * @return die Antragsnummer, z. B. "A000001"
     */
    public String getAntragsnummer() {
        return antragsnummer;
    }

    /**
     * Liefert die Art des Antrags.
     * @return Neukunde oder weiteres Konto
     */
    public Antragsart getArt() {
        return art;
    }

    /**
     * Liefert den Benutzernamen des Antragstellers.
     * @return der Benutzername des Kundenzugangs
     */
    public String getBenutzername() {
        return benutzername;
    }

    /**
     * Liefert den Namen des Antragstellers.
     * @return der Name in der Form "Nachname, Vorname"
     */
    public String getAntragsteller() {
        return antragsteller;
    }

    /**
     * Liefert die Stammdaten des künftigen Kunden.
     * @return die Kundendaten; null bei einem Antrag auf ein weiteres Konto
     */
    public Kundendaten getKundendaten() {
        return kundendaten;
    }

    /**
     * Liefert die Kundennummer.
     * @return die Kundennummer; bei einem Neukunden erst nach der Genehmigung, sonst null
     */
    public String getKundennummer() {
        return kundennummer;
    }

    /**
     * Liefert die gewünschte Kontoart.
     * @return die Kontoart
     */
    public Kontoart getKontoart() {
        return kontoart;
    }

    /**
     * Liefert die gewünschte Währung.
     * @return die Währung
     */
    public Waehrung getWaehrung() {
        return waehrung;
    }

    /**
     * Liefert den Zeitpunkt des Antrags.
     * @return wann der Antrag gestellt wurde
     */
    public LocalDateTime getEingereichtAm() {
        return eingereichtAm;
    }

    /**
     * Liefert den Bearbeitungsstand.
     * @return offen, genehmigt oder abgelehnt
     */
    public Antragsstatus getStatus() {
        return status;
    }

    /**
     * Liefert, wer entschieden hat.
     * @return Benutzername des Mitarbeiters oder null, solange der Antrag offen ist
     */
    public String getBearbeitetVon() {
        return bearbeitetVon;
    }

    /**
     * Liefert den Zeitpunkt der Entscheidung.
     * @return der Zeitpunkt oder null, solange der Antrag offen ist
     */
    public LocalDateTime getBearbeitetAm() {
        return bearbeitetAm;
    }

    /**
     * Liefert die Begründung einer Ablehnung.
     * @return die Begründung oder null, wenn der Antrag nicht abgelehnt wurde
     */
    public String getBegruendung() {
        return begruendung;
    }

    /**
     * Liefert die Nummer des eröffneten Kontos.
     * @return die Kontonummer oder null, solange der Antrag nicht genehmigt ist
     */
    public Long getKontonummer() {
        return kontonummer;
    }

    /**
     * Liefert Antragsnummer und Kontoart, z. B. für Listen.
     * @return z. B. "A000001 – Girokonto"
     */
    @Override
    public String toString() {
        return antragsnummer + " – " + kontoart;
    }
}
