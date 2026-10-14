package bankprojekt.verwaltung;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

import bankprojekt.basisdaten.Geldbetrag;
import bankprojekt.basisdaten.Konto;
import bankprojekt.basisdaten.Waehrung;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;

/**
 * Die Akte eines Kontos in der {@link Bankverwaltung}. Sie verbindet das
 * {@link Konto} der {@link Bank} mit der Kundennummer seines Inhabers und
 * ergänzt Kontoart, IBAN, Eröffnungs- und Auflösungsdatum.
 * <p>
 * Ein aufgelöstes Konto wird nicht gelöscht, sondern bleibt dauerhaft gesperrt
 * erhalten, damit Protokoll und Kundenhistorie nachvollziehbar bleiben.
 * Geändert wird eine Akte nur über die {@link Bankverwaltung}.
 * @author m.alharethi
 */
public final class Kontoakte implements Serializable {
    /**
     * Versionsnummer für die Serialisierung.
     */
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * das Konto aus der Bank
     */
    private final Konto konto;
    /**
     * Kundennummer des Inhabers
     */
    private final String kundennummer;
    /**
     * Art des Kontos
     */
    private final Kontoart kontoart;
    /**
     * IBAN ohne Leerzeichen
     */
    private final String iban;
    /**
     * Tag der Eröffnung
     */
    private final LocalDate eroeffnetAm;
    /**
     * Tag der Auflösung, null solange das Konto offen ist
     */
    private LocalDate aufgeloestAm;

    /**
     * Legt die Akte für ein neu eröffnetes Konto an.
     * @param konto das Konto aus der Bank
     * @param kundennummer Kundennummer des Inhabers
     * @param kontoart Art des Kontos
     * @param bankleitzahl Bankleitzahl, aus der die IBAN gebildet wird
     * @param eroeffnetAm Tag der Eröffnung
     */
    Kontoakte(Konto konto, String kundennummer, Kontoart kontoart, long bankleitzahl, LocalDate eroeffnetAm) {
        this.konto = konto;
        this.kundennummer = kundennummer;
        this.kontoart = kontoart;
        this.iban = konto.getIban(bankleitzahl);
        this.eroeffnetAm = eroeffnetAm;
    }

    /**
     * Vermerkt die Auflösung des Kontos und sperrt es dauerhaft.
     * @param heute Tag der Auflösung
     */
    void aufloesen(LocalDate heute) {
        konto.sperren();
        this.aufgeloestAm = heute;
    }

    /**
     * Liefert das Konto aus der Bank.
     * @return das Konto
     */
    Konto getKonto() {
        return konto;
    }

    /**
     * Liefert die Kontonummer.
     * @return die Kontonummer
     */
    public long getKontonummer() {
        return konto.getKontonummer();
    }

    /**
     * Liefert die IBAN ohne Leerzeichen.
     * @return die IBAN, z. B. "DE02123456780000000001"
     */
    public String getIban() {
        return iban;
    }

    /**
     * Liefert die IBAN in Vierergruppen, wie sie üblicherweise angezeigt wird.
     * @return die IBAN, z. B. "DE02 1234 5678 0000 0000 01"
     */
    public String getIbanFormatiert() {
        return iban.replaceAll("(.{4})(?!$)", "$1 ");
    }

    /**
     * Liefert die Kundennummer des Inhabers.
     * @return die Kundennummer
     */
    public String getKundennummer() {
        return kundennummer;
    }

    /**
     * Liefert den Namen des Inhabers.
     * @return der Name in der Form "Nachname, Vorname"
     */
    public String getInhaberName() {
        return konto.getInhaber().getName();
    }

    /**
     * Liefert die Kontoart.
     * @return die Kontoart
     */
    public Kontoart getKontoart() {
        return kontoart;
    }

    /**
     * Liefert die Währung, in der das Konto geführt wird.
     * @return die Währung des Kontostands
     */
    public Waehrung getWaehrung() {
        return konto.getKontostand().getWaehrung();
    }

    /**
     * Liefert den aktuellen Kontostand.
     * @return der Kontostand
     */
    public Geldbetrag getKontostand() {
        return konto.getKontostand();
    }

    /**
     * Der Kontostand als nur lesbare Property, damit die Oberfläche ihn beobachten kann.
     * @return der Kontostand als Property
     */
    public ReadOnlyObjectProperty<Geldbetrag> kontostandProperty() {
        return konto.kontostandProperty();
    }

    /**
     * Der Gesperrt-Zustand als nur lesbare Property; gesperrt und entsperrt wird
     * über die {@link Bankverwaltung}.
     * @return der Gesperrt-Zustand als Property
     */
    public ReadOnlyBooleanProperty gesperrtProperty() {
        return konto.gesperrtProperty();
    }

    /**
     * Liefert den Status des Kontos.
     * @return {@link Kontostatus#AUFGELOEST}, wenn es aufgelöst ist, sonst
     *         {@link Kontostatus#GESPERRT} oder {@link Kontostatus#AKTIV}
     */
    public Kontostatus getStatus() {
        if (aufgeloestAm != null)
            return Kontostatus.AUFGELOEST;
        return konto.isGesperrt() ? Kontostatus.GESPERRT : Kontostatus.AKTIV;
    }

    /**
     * Liefert den Tag der Eröffnung.
     * @return der Eröffnungstag
     */
    public LocalDate getEroeffnetAm() {
        return eroeffnetAm;
    }

    /**
     * Liefert den Tag der Auflösung.
     * @return der Auflösungstag oder null, solange das Konto offen ist
     */
    public LocalDate getAufgeloestAm() {
        return aufgeloestAm;
    }

    /**
     * Liefert IBAN und Kontoart, z. B. für Auswahllisten.
     * @return z. B. "DE02 1234 5678 0000 0000 01 (Girokonto)"
     */
    @Override
    public String toString() {
        return getIbanFormatiert() + " (" + kontoart + ")";
    }
}
