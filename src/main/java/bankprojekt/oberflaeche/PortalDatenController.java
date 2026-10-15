package bankprojekt.oberflaeche;

import java.io.UncheckedIOException;
import java.util.Arrays;

import bankprojekt.verwaltung.Kundenakte;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

/**
 * Controller von „Meine Daten" im Kundenportal ({@code portalDaten.fxml}): Stammdaten
 * ansehen, Kontaktdaten (Adresse, Telefon, E-Mail) ändern und das Passwort ändern.
 * @author m.alharethi
 */
public class PortalDatenController implements Portalansicht {
    /**
     * Wird vom FXMLLoader beim Laden der Ansicht aufgerufen.
     */
    public PortalDatenController() {
    }

    /** Anzeige Kundennummer */
    @FXML
    private Label kundennummer;
    /** Anzeige Name */
    @FXML
    private Label name;
    /** Anzeige Geburtsdatum */
    @FXML
    private Label geburtsdatum;
    /** Anzeige Benutzername */
    @FXML
    private Label benutzername;
    /** Eingabe Adresse */
    @FXML
    private TextArea adresse;
    /** Eingabe Telefon */
    @FXML
    private TextField telefon;
    /** Eingabe E-Mail */
    @FXML
    private TextField email;
    /** Erfolgs- oder Fehlermeldung */
    @FXML
    private Label meldung;

    /**
     * die Sitzung des angemeldeten Kunden
     */
    private Kundensitzung sitzung;

    @Override
    public void setSitzung(Kundensitzung sitzung) {
        this.sitzung = sitzung;
    }

    /**
     * Füllt die Felder mit den gespeicherten Daten.
     */
    @Override
    public void aktualisieren() {
        Kundenakte kunde = sitzung.portal().getKunde(sitzung.zugang()).orElseThrow();
        kundennummer.setText(kunde.getKundennummer());
        name.setText(kunde.getVorname() + " " + kunde.getNachname());
        geburtsdatum.setText(Darstellung.datum(kunde.getGeburtsdatum()));
        benutzername.setText(sitzung.zugang().getBenutzername());
        adresse.setText(kunde.getAdresse());
        telefon.setText(kunde.getTelefon());
        email.setText(kunde.getEmail());
        meldungZeigen("", true);
    }

    /**
     * Zeigt eine Meldung neben dem Speichern-Knopf.
     * @param text die Meldung
     * @param erfolg true für eine Erfolgs-, false für eine Fehlermeldung
     */
    private void meldungZeigen(String text, boolean erfolg) {
        meldung.setText(text);
        meldung.getStyleClass().setAll("label", erfolg ? "erfolgsmeldung" : "fehlermeldung");
    }

    /**
     * Reaktion auf „Kontaktdaten speichern".
     */
    @FXML
    private void kontaktdatenSpeichern() {
        try {
            sitzung.portal().kontaktdatenAendern(sitzung.zugang(), adresse.getText(), telefon.getText(),
                    email.getText());
            meldungZeigen("Ihre Kontaktdaten wurden gespeichert.", true);
        } catch (IllegalArgumentException | IllegalStateException | UncheckedIOException e) {
            meldungZeigen(e.getMessage(), false);
        }
    }

    /**
     * Reaktion auf „Passwort ändern".
     */
    @FXML
    private void passwortAendern() {
        Dialoge.Geladen<PasswortFormularController> formular = Dialoge.laden("passwortFormular.fxml");
        PasswortFormularController eingaben = formular.controller();
        if (Dialoge.formular(Dialoge.fenster(meldung), "Passwort ändern", formular.wurzel(), eingaben.getFehlerfeld(),
                () -> {
                    char[] alt = eingaben.getAltesPasswort();
                    char[] neu = eingaben.getNeuesPasswort();
                    try {
                        sitzung.portal().passwortAendern(sitzung.zugang(), alt, neu);
                    } finally {
                        Arrays.fill(alt, '\0');
                        Arrays.fill(neu, '\0');
                    }
                }))
            meldungZeigen("Ihr Passwort wurde geändert.", true);
    }
}
