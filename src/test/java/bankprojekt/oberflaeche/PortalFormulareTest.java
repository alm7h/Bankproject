package bankprojekt.oberflaeche;

import bankprojekt.basisdaten.Geldbetrag;
import bankprojekt.basisdaten.Waehrung;
import bankprojekt.verwaltung.Bankverwaltung;
import bankprojekt.verwaltung.Benutzer;
import bankprojekt.verwaltung.Buchung;
import bankprojekt.verwaltung.Kontoakte;
import bankprojekt.verwaltung.Kontoart;
import bankprojekt.verwaltung.Kundenakte;
import bankprojekt.verwaltung.Testdaten;
import bankprojekt.verwaltung.VerstellbareUhr;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testklasse für die Formulare und den Kontoauszug des Kundenportals:
 * {@link KontoauszugController}, {@link UeberweisungFormularController},
 * {@link UmbuchungFormularController}, {@link KontoantragFormularController} und
 * {@link PasswortFormularController}.
 */
@ExtendWith(JavaFxErweiterung.class)
class PortalFormulareTest {

    private Bankverwaltung verwaltung;
    private Benutzer admin;
    private Kontoakte giro;
    private Kontoakte spar;

    @BeforeEach
    void setUp() {
        verwaltung = Testdaten.verwaltung(new VerstellbareUhr());
        admin = Testdaten.admin(verwaltung);
        Kundenakte anna = verwaltung.kundeAnlegen(admin, Testdaten.kunde("Anna", "Schmidt"));
        giro = verwaltung.kontoEroeffnen(admin, anna.getKundennummer(), Kontoart.GIROKONTO, Waehrung.DOBRA,
                new Geldbetrag(100, Waehrung.DOBRA));
        spar = verwaltung.kontoEroeffnen(admin, anna.getKundennummer(), Kontoart.SPARBUCH, Waehrung.EURO, null);
    }

    @Nested
    class KontoauszugTests {
        @Test
        void neuesteBuchungZuerst_mitRotenAbgaengen() {
            verwaltung.bareinzahlung(admin, spar.getKontonummer(), new Geldbetrag(50), "Erste");
            verwaltung.barauszahlung(admin, spar.getKontonummer(), new Geldbetrag(20), "Zweite");
            Dialoge.Geladen<KontoauszugController> geladen = FxHilfe.laden("kontoauszug.fxml");
            geladen.controller().setBuchungen(verwaltung.getBuchungen(spar.getKontonummer()));
            FxHilfe.auslegen(geladen.wurzel());
            @SuppressWarnings("unchecked")
            TableView<Buchung> tabelle = (TableView<Buchung>) geladen.wurzel();
            Buchung neueste = tabelle.getItems().get(0);
            assertEquals("Zweite", neueste.text());
            assertEquals("14.10.2026 10:00", FxHilfe.zelle(tabelle, "Datum", neueste));
            assertEquals("Barauszahlung", FxHilfe.zelle(tabelle, "Art", neueste));
            assertEquals("Zweite", FxHilfe.zelle(tabelle, "Text", neueste));
            assertEquals(new Geldbetrag(-20), FxHilfe.zelle(tabelle, "Betrag", neueste));
            assertEquals(new Geldbetrag(30), FxHilfe.zelle(tabelle, "Saldo", neueste));
            assertEquals(1, tabelle.lookupAll(".table-cell").stream()
                    .filter(z -> z.getStyleClass().contains("negativ")).count(), "nur der Abgang ist rot");
        }
    }

    @Nested
    class UeberweisungTests {
        @Test
        void liestIbanBetragInKontowaehrungUndZweck() {
            Dialoge.Geladen<UeberweisungFormularController> geladen = FxHilfe.laden("ueberweisungFormular.fxml");
            Parent formular = geladen.wurzel();
            UeberweisungFormularController c = geladen.controller();
            c.setKonto(giro);
            assertEquals("STN", FxHilfe.text(formular, "waehrung"));
            FxHilfe.tippe(formular, "iban", "DE12 3456");
            FxHilfe.tippe(formular, "betrag", "12,5");
            FxHilfe.tippe(formular, "zweck", "Danke");
            assertEquals("DE12 3456", c.getIban());
            assertEquals(new Geldbetrag(12.5, Waehrung.DOBRA), c.getBetrag());
            assertEquals("Danke", c.getVerwendungszweck());
            assertSame(FxHilfe.suche(formular, "fehler"), c.getFehlerfeld());
            FxHilfe.tippe(formular, "betrag", "");
            assertThrows(IllegalArgumentException.class, c::getBetrag);
        }
    }

    @Nested
    class UmbuchungTests {
        @Test
        void ohneZiel_wirdNachgefragt_beiMehrerenZielenKeineVorauswahl() {
            Kontoakte fest = verwaltung.kontoEroeffnen(admin, giro.getKundennummer(), Kontoart.FESTGELDKONTO,
                    Waehrung.EURO, null);
            Dialoge.Geladen<UmbuchungFormularController> geladen = FxHilfe.laden("umbuchungFormular.fxml");
            Parent formular = geladen.wurzel();
            UmbuchungFormularController c = geladen.controller();
            c.setKonten(giro, List.of(spar, fest));
            assertNull(FxHilfe.<ComboBox<Kontoakte>>suche(formular, "ziel").getValue());
            assertEquals("Bitte ein Zielkonto wählen.",
                    assertThrows(IllegalArgumentException.class, c::getZiel).getMessage());
            FxHilfe.<ComboBox<Kontoakte>>suche(formular, "ziel").setValue(fest);
            assertSame(fest, c.getZiel());
            FxHilfe.tippe(formular, "betrag", "3");
            FxHilfe.tippe(formular, "text", "x");
            assertEquals(new Geldbetrag(3, Waehrung.DOBRA), c.getBetrag());
            assertEquals("x", c.getText());
            assertSame(FxHilfe.suche(formular, "fehler"), c.getFehlerfeld());
        }
    }

    @Nested
    class KontoantragTests {
        @Test
        void beschreibungFolgtDerKontoart_datenNurFuerNeukunden() {
            Dialoge.Geladen<KontoantragFormularController> geladen = FxHilfe.laden("kontoantragFormular.fxml");
            Parent formular = geladen.wurzel();
            KontoantragFormularController c = geladen.controller();
            assertFalse(FxHilfe.suche(formular, "datenBereich").isVisible());
            assertEquals(Waehrung.EURO, c.getWaehrung());
            assertNull(c.getKontoart());
            ComboBox<Kontoart> art = FxHilfe.suche(formular, "kontoart");
            art.setValue(Kontoart.SPARBUCH);
            assertEquals(Kontoart.SPARBUCH.getBeschreibung(), FxHilfe.text(formular, "beschreibung"));
            art.setValue(null);
            assertEquals("", FxHilfe.text(formular, "beschreibung"));
            assertSame(FxHilfe.suche(formular, "antragFehler"), c.getFehlerfeld());
        }

        @Test
        void neukunde_mitUndOhneVorbelegung() {
            Dialoge.Geladen<KontoantragFormularController> geladen = FxHilfe.laden("kontoantragFormular.fxml");
            KontoantragFormularController c = geladen.controller();
            c.setNeukunde(Testdaten.kunde("Anna", "Schmidt"));
            assertTrue(FxHilfe.suche(geladen.wurzel(), "datenBereich").isVisible());
            assertEquals(Testdaten.kunde("Anna", "Schmidt"), c.getKundendaten());

            Dialoge.Geladen<KontoantragFormularController> leer = FxHilfe.laden("kontoantragFormular.fxml");
            leer.controller().setNeukunde(null);
            assertThrows(IllegalArgumentException.class, leer.controller()::getKundendaten);
        }
    }

    @Nested
    class PasswortTests {
        @Test
        void neuesPasswort_nurBeiGleicherWiederholung() {
            Dialoge.Geladen<PasswortFormularController> geladen = FxHilfe.laden("passwortFormular.fxml");
            Parent formular = geladen.wurzel();
            PasswortFormularController c = geladen.controller();
            assertTrue(FxHilfe.text(formular, "hinweis").contains("mindestens 4 Zeichen"));
            c.setHinweis("Bitte ändern");
            assertEquals("Bitte ändern", FxHilfe.text(formular, "hinweis"));
            FxHilfe.tippe(formular, "altesPasswort", "alt1");
            FxHilfe.tippe(formular, "neuesPasswort", "neu1");
            FxHilfe.tippe(formular, "wiederholung", "neu2");
            assertArrayEquals("alt1".toCharArray(), c.getAltesPasswort());
            assertThrows(IllegalArgumentException.class, c::getNeuesPasswort);
            FxHilfe.tippe(formular, "wiederholung", "neu1");
            assertArrayEquals("neu1".toCharArray(), c.getNeuesPasswort());
            assertSame(FxHilfe.suche(formular, "fehler"), c.getFehlerfeld());
        }
    }
}
