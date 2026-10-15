package bankprojekt.oberflaeche;

import bankprojekt.basisdaten.Waehrung;
import bankprojekt.verwaltung.Antragsart;
import bankprojekt.verwaltung.Bankverwaltung;
import bankprojekt.verwaltung.Kontoantrag;
import bankprojekt.verwaltung.Kontoart;
import bankprojekt.verwaltung.Kundenportal;
import bankprojekt.verwaltung.Testdaten;
import bankprojekt.verwaltung.VerstellbareUhr;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.RadioButton;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testklasse für die Registrierung eines Neukunden ({@link RegistrierungController}).
 */
@ExtendWith(JavaFxErweiterung.class)
class RegistrierungControllerTest {

    private Bankverwaltung verwaltung;
    private Parent assistent;
    private final List<String> zurueck = new ArrayList<>();

    @BeforeEach
    void setUp() {
        verwaltung = Testdaten.verwaltung(new VerstellbareUhr());
        Dialoge.Geladen<RegistrierungController> geladen = FxHilfe.laden("registrierung.fxml");
        assistent = geladen.wurzel();
        geladen.controller().setPortal(new Kundenportal(verwaltung), zurueck::add);
    }

    private void weiter() {
        FxHilfe.druecke(assistent, "weiterKnopf");
    }

    private String titel() {
        return FxHilfe.text(assistent, "titel");
    }

    private String fehler() {
        return FxHilfe.text(assistent, "schrittFehler");
    }

    private Button knopf(String id) {
        return FxHilfe.suche(assistent, id);
    }

    private boolean sichtbar(String id) {
        return FxHilfe.suche(assistent, id).isVisible();
    }

    private void datenEingeben() {
        Parent daten = FxHilfe.suche(assistent, "datenformular");
        FxHilfe.tippe(daten, "vorname", "Anna");
        FxHilfe.tippe(daten, "nachname", "Schmidt");
        FxHilfe.tippe(daten, "geburtsdatum", "1.5.1990");
        FxHilfe.tippe(daten, "adresse", "Hauptstr. 1\n10115 Berlin");
        FxHilfe.tippe(daten, "email", "anna@example.org");
    }

    private void kontoartWaehlen(Kontoart art) {
        assistent.lookupAll(".radio-button").stream()
                .map(n -> (RadioButton) n)
                .filter(r -> r.getUserData() == art)
                .findFirst().orElseThrow().fire();
    }

    private void zugangEingeben(String name, String passwort, String wiederholung) {
        FxHilfe.tippe(assistent, "benutzername", name);
        FxHilfe.tippe(assistent, "passwort", passwort);
        FxHilfe.tippe(assistent, "wiederholung", wiederholung);
    }

    /**
     * Geht mit gültigen Eingaben bis zur Zusammenfassung.
     */
    private void bisZurZusammenfassung() {
        weiter();
        datenEingeben();
        weiter();
        kontoartWaehlen(Kontoart.SPARBUCH);
        FxHilfe.<ComboBox<Waehrung>>suche(assistent, "waehrung").setValue(Waehrung.DENAR);
        weiter();
        zugangEingeben("Anna.S", "geheim1", "geheim1");
        weiter();
    }

    @Test
    void beginntMitDerBegruessung() {
        assertEquals("Willkommen bei unserer Bank!", titel());
        assertEquals("Schritt 1 von 5", FxHilfe.text(assistent, "schrittAnzeige"));
        assertTrue(FxHilfe.text(assistent, "willkommenText").contains("In wenigen Schritten"));
        assertTrue(sichtbar("seiteWillkommen"));
        assertFalse(knopf("zurueckKnopf").isVisible());
        assertEquals("Weiter", knopf("weiterKnopf").getText());
        assertEquals(5, assistent.lookupAll(".radio-button").size(), "eine Auswahl je Kontoart");
        assertEquals(Waehrung.EURO, FxHilfe.<ComboBox<Waehrung>>suche(assistent, "waehrung").getValue());
    }

    @Test
    void kompletterDurchlauf_stelltDenAntrag_undDanktMitNamen() {
        bisZurZusammenfassung();
        assertEquals("Alles richtig?", titel());
        assertEquals("Antrag absenden", knopf("weiterKnopf").getText());
        String zusammenfassung = FxHilfe.text(assistent, "zusammenfassung");
        assertTrue(zusammenfassung.contains("Name: Anna Schmidt"));
        assertTrue(zusammenfassung.contains("Adresse: Hauptstr. 1, 10115 Berlin"));
        assertTrue(zusammenfassung.contains("Telefon: –"));
        assertTrue(zusammenfassung.contains("Konto: Sparbuch in MKD"));
        assertTrue(zusammenfassung.contains("Benutzername: anna.s"));

        weiter();

        assertEquals("Vielen Dank!", titel());
        assertEquals("Antrag eingegangen", FxHilfe.text(assistent, "schrittAnzeige"));
        assertTrue(FxHilfe.text(assistent, "dankeText").startsWith("Vielen Dank, Anna! Ihr Antrag A000001 auf ein Sparbuch"));
        assertFalse(knopf("abbrechenKnopf").isVisible());
        assertFalse(knopf("zurueckKnopf").isVisible());
        assertEquals("Zur Anmeldung", knopf("weiterKnopf").getText());
        Kontoantrag antrag = verwaltung.findeAntrag("A000001").orElseThrow();
        assertEquals(Antragsart.NEUKUNDE, antrag.getArt());
        assertEquals(Waehrung.DENAR, antrag.getWaehrung());
        assertTrue(verwaltung.findeKundenzugang("anna.s").isPresent());

        weiter();
        assertEquals(List.of("anna.s"), zurueck);
    }

    @Test
    void jederSchritt_wirdGeprueft() {
        weiter();
        weiter();   // ohne Daten
        assertEquals("Vorname ist ein Pflichtfeld.", fehler());
        assertEquals("Erzählen Sie uns von sich", titel());
        datenEingeben();
        weiter();
        assertEquals("", fehler());

        weiter();   // ohne Kontoart
        assertEquals("Bitte wählen Sie eine Kontoart.", fehler());
        kontoartWaehlen(Kontoart.GIROKONTO);
        weiter();

        zugangEingeben("anna.s", "geheim1", "geheim2");
        weiter();
        assertEquals("Die beiden Passwörter stimmen nicht überein.", fehler());
        zugangEingeben("admin", "geheim1", "geheim1");
        weiter();
        assertTrue(fehler().contains("bereits vergeben"), fehler());
        zugangEingeben("anna.s", "123", "123");
        weiter();
        assertTrue(fehler().contains("mindestens 4 Zeichen"), fehler());
        zugangEingeben("anna.s", "geheim1", "geheim1");
        weiter();
        assertEquals("Alles richtig?", titel());
    }

    @Test
    void fehlerBeimAbsenden_bleibtAufDerZusammenfassung() {
        bisZurZusammenfassung();
        // inzwischen hat sich jemand denselben Namen gesichert
        Testdaten.kundenzugang(verwaltung, "anna.s", null);
        weiter();
        assertTrue(fehler().contains("bereits vergeben"));
        assertEquals("Alles richtig?", titel());
    }

    @Test
    void zurueck_behaeltDieEingaben() {
        weiter();
        datenEingeben();
        weiter();
        assertTrue(knopf("zurueckKnopf").isVisible());
        FxHilfe.druecke(assistent, "zurueckKnopf");
        assertEquals("Erzählen Sie uns von sich", titel());
        Parent daten = FxHilfe.suche(assistent, "datenformular");
        assertEquals("Anna", FxHilfe.<javafx.scene.control.TextField>suche(daten, "vorname").getText());
    }

    @Test
    void abbrechen_aufDerBegruessung_ohneRueckfrage() {
        FxHilfe.druecke(assistent, "abbrechenKnopf");
        assertEquals(1, zurueck.size());
        assertNull(zurueck.get(0));
    }

    @Test
    void abbrechen_mitEingaben_fragtNach() {
        weiter();
        Dialogantworten antworten = new Dialogantworten().klicke("Abbrechen").klicke("Verwerfen");
        FxHilfe.druecke(assistent, "abbrechenKnopf");
        assertTrue(zurueck.isEmpty(), "weiter bearbeiten");
        assertEquals("Registrierung abbrechen", antworten.letzter().getHeaderText());
        FxHilfe.druecke(assistent, "abbrechenKnopf");
        assertEquals(1, zurueck.size());
    }

    @Test
    void nurDieSeiteDesAktuellenSchrittsIstSichtbar() {
        weiter();
        long sichtbar = List.of("seiteWillkommen", "seiteDaten", "seiteKonto", "seiteZugang",
                "seiteZusammenfassung", "seiteDanke").stream().map(id -> FxHilfe.<Node>suche(assistent, id))
                .filter(Node::isVisible).count();
        assertEquals(1, sichtbar);
        assertTrue(sichtbar("seiteDaten"));
    }

    @Test
    void setPortal_mitNull_wirft() {
        Dialoge.Geladen<RegistrierungController> geladen = FxHilfe.laden("registrierung.fxml");
        assertThrows(NullPointerException.class, () -> geladen.controller().setPortal(null, n -> { }));
        assertThrows(NullPointerException.class,
                () -> geladen.controller().setPortal(new Kundenportal(verwaltung), null));
    }
}
