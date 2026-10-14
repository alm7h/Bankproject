package bankprojekt.verwaltung;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import bankprojekt.basisdaten.Geldbetrag;
import bankprojekt.basisdaten.Waehrung;
import bankprojekt.exceptions.GesperrtException;

/**
 * Alles, was ein Kunde selbst tun kann: sich als Neukunde registrieren, Konten
 * beantragen, seine Konten und Kontoauszüge ansehen, überweisen, zwischen eigenen
 * Konten umbuchen, ein Konto sperren, Kontaktdaten und Passwort ändern.
 * <p>
 * Die Daten liegen in der {@link Bankverwaltung}; diese Klasse ist die Sicht des
 * Kunden darauf. Jede Aktion außer der Registrierung verlangt den angemeldeten
 * {@link Kundenzugang} und erlaubt nur den Zugriff auf die eigenen Konten. Jede Änderung
 * wird protokolliert. Ungültige Eingaben führen zu einer {@link IllegalArgumentException},
 * unzulässige Aktionen zu einer {@link IllegalStateException} – jeweils mit einer
 * Meldung, die die Oberfläche anzeigen kann.
 * @author m.alharethi
 */
public final class Kundenportal {
    /**
     * Höchstlänge eines Verwendungszwecks (wie im SEPA-Verfahren)
     */
    static final int MAX_VERWENDUNGSZWECK = 140;

    /**
     * die Verwaltung mit allen Daten
     */
    private final Bankverwaltung verwaltung;

    /**
     * Erstellt das Kundenportal für eine Verwaltung.
     * @param verwaltung die Bankverwaltung
     * @throws NullPointerException wenn die Verwaltung null ist
     */
    public Kundenportal(Bankverwaltung verwaltung) {
        this.verwaltung = Objects.requireNonNull(verwaltung, "Verwaltung darf nicht null sein");
    }

    // ------------------------------------------------------------------ Prüfungen

    /**
     * Prüft, ob der Zugang zu dieser Verwaltung gehört und sich anmelden darf.
     * @param akteur der angemeldete Zugang
     * @throws IllegalStateException wenn kein gültiger Kunde angemeldet ist
     */
    private void pruefeZugang(Kundenzugang akteur) {
        if (akteur == null || verwaltung.findeKundenzugang(akteur.getBenutzername()).orElse(null) != akteur
                || !verwaltung.kundenzugangAktiv(akteur))
            throw new IllegalStateException("Diese Aktion ist nur für angemeldete Kunden erlaubt.");
    }

    /**
     * Prüft den Zugang und liefert den zugehörigen Kunden.
     * @param akteur der angemeldete Zugang
     * @return die Kundenakte
     * @throws IllegalStateException wenn kein gültiger Kunde angemeldet ist oder der
     *                               Neukundenantrag noch nicht genehmigt ist
     */
    private Kundenakte pruefeKunde(Kundenzugang akteur) {
        pruefeZugang(akteur);
        if (!akteur.istKunde())
            throw new IllegalStateException("Ihr Antrag ist noch nicht genehmigt – Sie haben noch keine Konten.");
        return verwaltung.kundeHolen(akteur.getKundennummer());
    }

    /**
     * Liefert ein Konto des Kunden.
     * @param kunde der Kunde
     * @param kontonummer die Kontonummer
     * @return das Konto
     * @throws IllegalArgumentException wenn das Konto nicht dem Kunden gehört
     */
    private Kontoakte eigenesKonto(Kundenakte kunde, long kontonummer) {
        return verwaltung.findeKonto(kontonummer)
                .filter(k -> k.getKundennummer().equals(kunde.getKundennummer()))
                .orElseThrow(() -> new IllegalArgumentException("Dieses Konto gehört nicht zu Ihrem Zugang."));
    }

    // ------------------------------------------------------------------ Registrierung und Anträge

    /**
     * Prüft gewünschte Zugangsdaten, bevor sie verwendet werden (z. B. in einem
     * Schritt der Registrierung).
     * @param benutzername gewünschter Benutzername
     * @param passwort gewünschtes Passwort
     * @return der Benutzername in Kleinbuchstaben
     * @throws IllegalArgumentException wenn der Name ungültig oder vergeben oder das
     *                                  Passwort zu kurz ist
     */
    public String zugangsdatenPruefen(String benutzername, char[] passwort) {
        String name = Eingabepruefung.benutzername(benutzername);
        verwaltung.benutzernameFreiPruefen(name);
        Eingabepruefung.passwort(passwort);
        return name;
    }

    /**
     * Registriert einen Neukunden: legt seinen Portalzugang an und stellt den Antrag auf
     * das gewünschte Konto. Der Kunde kann sich sofort anmelden und den Stand verfolgen;
     * Kunde und Konto entstehen erst, wenn die Mitarbeiter den Antrag genehmigen.
     * @param daten die Stammdaten
     * @param kontoart gewünschte Kontoart
     * @param waehrung gewünschte Währung
     * @param benutzername gewünschter Benutzername
     * @param passwort gewünschtes Passwort
     * @return der gestellte Antrag
     * @throws NullPointerException wenn die Daten null sind
     * @throws IllegalArgumentException bei ungültigen Angaben oder vergebenem Benutzernamen
     */
    public Kontoantrag neukundeRegistrieren(Kundendaten daten, Kontoart kontoart, Waehrung waehrung,
                                           String benutzername, char[] passwort) {
        Objects.requireNonNull(daten, "Kundendaten dürfen nicht null sein");
        Eingabepruefung.kontowahl(kontoart, waehrung);
        String name = zugangsdatenPruefen(benutzername, passwort);
        verwaltung.kundenzugangHinzufuegen(
                new Kundenzugang(name, Passwortschutz.hashen(passwort), null, false, verwaltung.jetzt()));
        Kontoantrag antrag = verwaltung.antragStellen(Antragsart.NEUKUNDE, name, name(daten), daten, null,
                kontoart, waehrung);
        verwaltung.protokollieren(name, Protokollaktion.REGISTRIERUNG, "Neukunde " + name(daten)
                + " registriert, Antrag " + antrag.getAntragsnummer() + " (" + kontoart + ", " + waehrung + ")");
        return antrag;
    }

    /**
     * Liefert den Namen aus Kundendaten.
     * @param daten die Kundendaten
     * @return "Nachname, Vorname"
     */
    private static String name(Kundendaten daten) {
        return daten.nachname() + ", " + daten.vorname();
    }

    /**
     * Gibt an, ob ein Neukunde einen neuen Antrag stellen kann: Er ist noch kein Kunde und
     * hat keinen offenen Antrag (typischerweise nach einer Ablehnung).
     * @param akteur der angemeldete Zugang
     * @return true, wenn {@link #erneutBeantragen} möglich ist
     */
    public boolean kannErneutBeantragen(Kundenzugang akteur) {
        pruefeZugang(akteur);
        return !akteur.istKunde() && getAntraege(akteur).stream()
                .noneMatch(a -> a.getStatus() == Antragsstatus.OFFEN);
    }

    /**
     * Stellt nach einer Ablehnung einen neuen Neukundenantrag.
     * @param akteur der angemeldete Zugang (noch kein Kunde)
     * @param daten die (ggf. korrigierten) Stammdaten
     * @param kontoart gewünschte Kontoart
     * @param waehrung gewünschte Währung
     * @return der neue Antrag
     * @throws IllegalStateException wenn der Zugang schon Kunde ist oder noch ein Antrag offen ist
     * @throws IllegalArgumentException wenn Kontoart oder Währung fehlen
     * @throws NullPointerException wenn die Daten null sind
     */
    public Kontoantrag erneutBeantragen(Kundenzugang akteur, Kundendaten daten, Kontoart kontoart,
                                       Waehrung waehrung) {
        pruefeZugang(akteur);
        if (akteur.istKunde())
            throw new IllegalStateException("Sie sind bereits Kunde – bitte beantragen Sie ein weiteres Konto.");
        if (!kannErneutBeantragen(akteur))
            throw new IllegalStateException("Ihr Antrag wird noch geprüft.");
        Objects.requireNonNull(daten, "Kundendaten dürfen nicht null sein");
        Eingabepruefung.kontowahl(kontoart, waehrung);
        Kontoantrag antrag = verwaltung.antragStellen(Antragsart.NEUKUNDE, akteur.getBenutzername(), name(daten),
                daten, null, kontoart, waehrung);
        verwaltung.protokollieren(akteur.getBenutzername(), Protokollaktion.ANTRAG_GESTELLT,
                "Neuer Neukundenantrag " + antrag.getAntragsnummer() + " (" + kontoart + ", " + waehrung + ")");
        return antrag;
    }

    /**
     * Beantragt als bestehender Kunde ein weiteres Konto.
     * @param akteur der angemeldete Kunde
     * @param kontoart gewünschte Kontoart
     * @param waehrung gewünschte Währung
     * @return der gestellte Antrag
     * @throws IllegalStateException wenn kein Kunde angemeldet ist
     * @throws IllegalArgumentException wenn Kontoart oder Währung fehlen
     */
    public Kontoantrag zusatzkontoBeantragen(Kundenzugang akteur, Kontoart kontoart, Waehrung waehrung) {
        Kundenakte kunde = pruefeKunde(akteur);
        Eingabepruefung.kontowahl(kontoart, waehrung);
        Kontoantrag antrag = verwaltung.antragStellen(Antragsart.ZUSATZKONTO, akteur.getBenutzername(),
                kunde.getName(), null, kunde.getKundennummer(), kontoart, waehrung);
        verwaltung.protokollieren(akteur.getBenutzername(), Protokollaktion.ANTRAG_GESTELLT, "Antrag "
                + antrag.getAntragsnummer() + " auf ein weiteres Konto (" + kontoart + ", " + waehrung + ")");
        return antrag;
    }

    /**
     * Liefert die eigenen Anträge.
     * @param akteur der angemeldete Zugang
     * @return nicht veränderbare Liste, nach Antragsnummer sortiert
     */
    public List<Kontoantrag> getAntraege(Kundenzugang akteur) {
        pruefeZugang(akteur);
        return verwaltung.antraegeVon(akteur.getBenutzername());
    }

    // ------------------------------------------------------------------ eigene Daten und Konten

    /**
     * Liefert die eigene Kundenakte.
     * @param akteur der angemeldete Zugang
     * @return die Kundenakte; leer, solange der Neukundenantrag nicht genehmigt ist
     */
    public Optional<Kundenakte> getKunde(Kundenzugang akteur) {
        pruefeZugang(akteur);
        return akteur.istKunde() ? Optional.of(verwaltung.kundeHolen(akteur.getKundennummer())) : Optional.empty();
    }

    /**
     * Liefert die eigenen Konten (auch gesperrte und aufgelöste).
     * @param akteur der angemeldete Zugang
     * @return nicht veränderbare Liste, nach Kontonummer sortiert; leer ohne Kundennummer
     */
    public List<Kontoakte> getKonten(Kundenzugang akteur) {
        pruefeZugang(akteur);
        return akteur.istKunde() ? verwaltung.getKontenVon(akteur.getKundennummer()) : List.of();
    }

    /**
     * Liefert den Kontoauszug eines eigenen Kontos.
     * @param akteur der angemeldete Kunde
     * @param kontonummer die Kontonummer
     * @return nicht veränderbare Liste der Buchungen, älteste zuerst
     * @throws IllegalStateException wenn kein Kunde angemeldet ist
     * @throws IllegalArgumentException wenn das Konto nicht dem Kunden gehört
     */
    public List<Buchung> getKontoauszug(Kundenzugang akteur, long kontonummer) {
        Kundenakte kunde = pruefeKunde(akteur);
        return verwaltung.getBuchungen(eigenesKonto(kunde, kontonummer).getKontonummer());
    }

    /**
     * Ändert die eigenen Kontaktdaten. Name und Geburtsdatum ändern nur die Mitarbeiter.
     * @param akteur der angemeldete Kunde
     * @param adresse neue Adresse (Pflicht)
     * @param telefon neue Telefonnummer (freiwillig)
     * @param email neue E-Mail-Adresse (Pflicht)
     * @throws IllegalStateException wenn kein Kunde angemeldet ist
     * @throws IllegalArgumentException bei ungültigen Angaben
     */
    public void kontaktdatenAendern(Kundenzugang akteur, String adresse, String telefon, String email) {
        Kundenakte kunde = pruefeKunde(akteur);
        Kundendaten alt = kunde.getKundendaten();
        Kundendaten neu = new Kundendaten(alt.vorname(), alt.nachname(), alt.geburtsdatum(), adresse, telefon, email);
        String aenderungen = Bankverwaltung.aenderungenBeschreiben(alt, neu);
        kunde.aktualisieren(neu);
        verwaltung.protokollieren(akteur.getBenutzername(), Protokollaktion.KONTAKTDATEN_GEAENDERT,
                "Kunde " + kunde + ": " + aenderungen);
    }

    /**
     * Ändert das eigene Passwort.
     * @param akteur der angemeldete Zugang
     * @param altesPasswort das bisherige Passwort
     * @param neuesPasswort das neue Passwort (mindestens 4 Zeichen, anders als das bisherige)
     * @throws IllegalStateException wenn kein gültiger Zugang angemeldet ist
     * @throws IllegalArgumentException wenn das bisherige Passwort falsch oder das neue
     *                                  ungültig ist
     */
    public void passwortAendern(Kundenzugang akteur, char[] altesPasswort, char[] neuesPasswort) {
        pruefeZugang(akteur);
        if (!akteur.passwortPruefen(altesPasswort))
            throw new IllegalArgumentException("Das bisherige Passwort ist falsch.");
        Eingabepruefung.passwort(neuesPasswort);
        if (Arrays.equals(altesPasswort, neuesPasswort))
            throw new IllegalArgumentException("Das neue Passwort muss sich vom bisherigen unterscheiden.");
        akteur.setPasswort(Passwortschutz.hashen(neuesPasswort), false);
        verwaltung.protokollieren(akteur.getBenutzername(), Protokollaktion.PASSWORT_GEAENDERT,
                "Portalpasswort geändert");
    }

    /**
     * Sperrt ein eigenes Konto (z. B. weil die Karte verloren ist). Entsperren können
     * nur die Mitarbeiter.
     * @param akteur der angemeldete Kunde
     * @param kontonummer die Kontonummer
     * @throws IllegalStateException wenn kein Kunde angemeldet ist oder das Konto nicht aktiv ist
     * @throws IllegalArgumentException wenn das Konto nicht dem Kunden gehört
     */
    public void kontoSperren(Kundenzugang akteur, long kontonummer) {
        Kontoakte konto = eigenesKonto(pruefeKunde(akteur), kontonummer);
        if (konto.getStatus() != Kontostatus.AKTIV)
            throw new IllegalStateException("Das Konto " + konto.getIbanFormatiert() + " ist bereits "
                    + konto.getStatus() + ".");
        konto.getKonto().sperren();
        verwaltung.protokollieren(akteur.getBenutzername(), Protokollaktion.KONTO_GESPERRT,
                "Konto " + konto.getIbanFormatiert() + " vom Kunden im Kundenportal gesperrt");
    }

    // ------------------------------------------------------------------ Zahlungsverkehr

    /**
     * Überweist von einem eigenen Girokonto auf ein Girokonto dieser Bank.
     * @param akteur der angemeldete Kunde
     * @param vonKontonummer das eigene Girokonto
     * @param zielIban IBAN des Empfängerkontos (Leerzeichen egal)
     * @param betrag der Betrag (größer 0), z. B. in der Kontowährung
     * @param verwendungszweck freiwilliger Verwendungszweck (höchstens 140 Zeichen)
     * @throws IllegalStateException wenn kein Kunde angemeldet ist, ein Konto gesperrt ist,
     *                               das Empfängerkonto keine Überweisungen empfangen kann
     *                               oder die Deckung nicht reicht
     * @throws IllegalArgumentException bei ungültigen Angaben, unbekannter IBAN oder
     *                                  fremdem Absenderkonto
     */
    public void ueberweisen(Kundenzugang akteur, long vonKontonummer, String zielIban, Geldbetrag betrag,
                            String verwendungszweck) {
        Kundenakte kunde = pruefeKunde(akteur);
        Kontoakte von = eigenesKonto(kunde, vonKontonummer);
        if (von.getKontoart() != Kontoart.GIROKONTO)
            throw new IllegalStateException("Überweisungen sind nur von einem Girokonto aus möglich.");
        String iban = Eingabepruefung.pflichtfeld(zielIban, "IBAN des Empfängers");
        Kontoakte nach = verwaltung.findeKontoNachIban(iban).orElseThrow(() ->
                new IllegalArgumentException("Die IBAN " + iban + " ist bei uns nicht bekannt."));
        if (nach == von)
            throw new IllegalArgumentException("Absender- und Empfängerkonto sind gleich.");
        if (nach.getKontoart() != Kontoart.GIROKONTO || nach.getStatus() != Kontostatus.AKTIV)
            throw new IllegalStateException("Das Empfängerkonto kann keine Überweisungen empfangen.");
        Eingabepruefung.betrag(betrag);
        String zweck = Eingabepruefung.freiwillig(verwendungszweck);
        if (zweck.length() > MAX_VERWENDUNGSZWECK)
            throw new IllegalArgumentException("Der Verwendungszweck darf höchstens " + MAX_VERWENDUNGSZWECK
                    + " Zeichen lang sein.");
        boolean ausgefuehrt;
        try {
            ausgefuehrt = verwaltung.getBank().geldUeberweisen(von.getKontonummer(), nach.getKontonummer(),
                    betrag, zweck);
        } catch (GesperrtException e) {
            throw new IllegalStateException(Bankverwaltung.gesperrtMeldung(von), e);
        }
        if (!ausgefuehrt)
            throw new IllegalStateException("Die Überweisung ist nicht gedeckt: Kontostand und Dispo reichen nicht aus.");
        String zusatz = zweck.isEmpty() ? "" : " – " + zweck;
        verwaltung.buchen(von, Buchungsart.UEBERWEISUNG_AUSGANG, betrag,
                "An " + nach.getInhaberName() + " (" + nach.getIbanFormatiert() + ")" + zusatz, akteur.getBenutzername());
        verwaltung.buchen(nach, Buchungsart.UEBERWEISUNG_EINGANG, betrag,
                "Von " + von.getInhaberName() + " (" + von.getIbanFormatiert() + ")" + zusatz, akteur.getBenutzername());
        verwaltung.protokollieren(akteur.getBenutzername(), Protokollaktion.UEBERWEISUNG, betrag + " von "
                + von.getIbanFormatiert() + " an " + nach.getIbanFormatiert() + zusatz);
    }

    /**
     * Bucht Geld zwischen zwei eigenen Konten um (z. B. vom Girokonto auf das Sparbuch).
     * Es gelten die Abhebungsregeln des abgebenden Kontos.
     * @param akteur der angemeldete Kunde
     * @param vonKontonummer abgebendes eigenes Konto
     * @param nachKontonummer empfangendes eigenes Konto
     * @param betrag der Betrag (größer 0)
     * @param text freiwilliger Buchungstext
     * @throws IllegalStateException wenn kein Kunde angemeldet ist, ein Konto gesperrt ist
     *                               oder die Regeln des abgebenden Kontos die Abbuchung nicht erlauben
     * @throws IllegalArgumentException bei ungültigen Angaben, gleichen oder fremden Konten
     */
    public void umbuchen(Kundenzugang akteur, long vonKontonummer, long nachKontonummer, Geldbetrag betrag,
                         String text) {
        Kundenakte kunde = pruefeKunde(akteur);
        Kontoakte von = eigenesKonto(kunde, vonKontonummer);
        Kontoakte nach = eigenesKonto(kunde, nachKontonummer);
        if (von == nach)
            throw new IllegalArgumentException("Bitte zwei verschiedene Konten wählen.");
        // Zielkonto vorher prüfen, damit nicht abgebucht wird, wenn die Gutschrift scheitern würde
        if (nach.getStatus() != Kontostatus.AKTIV)
            throw new IllegalStateException(Bankverwaltung.gesperrtMeldung(nach));
        Eingabepruefung.betrag(betrag);
        String buchungstext = Bankverwaltung.buchungstext(text, "Umbuchung");
        verwaltung.abheben(von, betrag);
        verwaltung.einzahlen(nach, betrag);
        verwaltung.buchen(von, Buchungsart.UMBUCHUNG_AUSGANG, betrag,
                buchungstext + " auf " + nach, akteur.getBenutzername());
        verwaltung.buchen(nach, Buchungsart.UMBUCHUNG_EINGANG, betrag,
                buchungstext + " von " + von, akteur.getBenutzername());
        verwaltung.protokollieren(akteur.getBenutzername(), Protokollaktion.UMBUCHUNG, betrag + " von "
                + von.getIbanFormatiert() + " auf " + nach.getIbanFormatiert());
    }
}
