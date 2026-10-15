package bankprojekt.oberflaeche;

import bankprojekt.basisdaten.Geldbetrag;
import bankprojekt.basisdaten.Waehrung;
import bankprojekt.verwaltung.Bankverwaltung;
import bankprojekt.verwaltung.Benutzer;
import bankprojekt.verwaltung.Kontoakte;
import bankprojekt.verwaltung.Kontoantrag;
import bankprojekt.verwaltung.Kontoart;
import bankprojekt.verwaltung.Kundenakte;
import bankprojekt.verwaltung.Kundenportal;
import bankprojekt.verwaltung.Kundenzugang;
import bankprojekt.verwaltung.Testdaten;
import bankprojekt.verwaltung.VerstellbareUhr;
import javafx.scene.Parent;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testklasse für die neuen Formulare der Verwaltung: {@link GenehmigungFormularController},
 * {@link AblehnungFormularController}, {@link PortalzugangFormularController} und
 * {@link BarFormularController}.
 */
@ExtendWith(JavaFxErweiterung.class)
class VerwaltungFormulareTest {

    private Bankverwaltung verwaltung;
    private Benutzer admin;
    private Kundenakte anna;

    @BeforeEach
    void setUp() {
        verwaltung = Testdaten.verwaltung(new VerstellbareUhr());
        admin = Testdaten.admin(verwaltung);
        anna = verwaltung.kundeAnlegen(admin, Testdaten.kunde("Anna", "Schmidt"));
    }

    @Test
    void genehmigung_neukunde_mitDispoInDerAntragswaehrung() {
        Kontoantrag antrag = Testdaten.neukunde(verwaltung, "carla.k", Testdaten.kunde("Carla", "Klein"),
                Kontoart.GIROKONTO);
        Dialoge.Geladen<GenehmigungFormularController> geladen = FxHilfe.laden("genehmigungFormular.fxml");
        Parent formular = geladen.wurzel();
        GenehmigungFormularController c = geladen.controller();
        c.setAntrag(antrag);
        assertEquals("Klein, Carla beantragt ein Girokonto in EUR.", FxHilfe.text(formular, "zusammenfassung"));
        assertTrue(FxHilfe.text(formular, "hinweis").startsWith("Bei der Genehmigung werden der Kunde und das Konto"));
        assertEquals("EUR", FxHilfe.text(formular, "waehrung"));
        FxHilfe.tippe(formular, "dispo", "300");
        assertEquals(new Geldbetrag(300), c.getDispo());
        assertSame(FxHilfe.suche(formular, "fehler"), c.getFehlerfeld());
    }

    @Test
    void genehmigung_zusatzkonto_ohneDispo() {
        Kundenzugang zugang = Testdaten.kundenzugang(verwaltung, "anna.s", anna.getKundennummer());
        Kontoantrag antrag = new Kundenportal(verwaltung).zusatzkontoBeantragen(zugang, Kontoart.SPARBUCH, Waehrung.DENAR);
        Dialoge.Geladen<GenehmigungFormularController> geladen = FxHilfe.laden("genehmigungFormular.fxml");
        GenehmigungFormularController c = geladen.controller();
        c.setAntrag(antrag);
        assertFalse(FxHilfe.suche(geladen.wurzel(), "dispoZeile").isVisible());
        assertTrue(FxHilfe.text(geladen.wurzel(), "hinweis").startsWith("Bei der Genehmigung wird das Konto eröffnet."));
        assertNull(c.getDispo());
    }

    @Test
    void ablehnung_liestDieBegruendung() {
        Kontoantrag antrag = Testdaten.neukunde(verwaltung, "carla.k", Testdaten.kunde("Carla", "Klein"),
                Kontoart.SPARBUCH);
        Dialoge.Geladen<AblehnungFormularController> geladen = FxHilfe.laden("ablehnungFormular.fxml");
        AblehnungFormularController c = geladen.controller();
        c.setAntrag(antrag);
        assertEquals("Klein, Carla hat ein Sparbuch in EUR beantragt.",
                FxHilfe.text(geladen.wurzel(), "zusammenfassung"));
        FxHilfe.tippe(geladen.wurzel(), "begruendung", "zu jung");
        assertEquals("zu jung", c.getBegruendung());
        assertSame(FxHilfe.suche(geladen.wurzel(), "fehler"), c.getFehlerfeld());
    }

    @Test
    void portalzugang_einrichten_undZuruecksetzen() {
        Dialoge.Geladen<PortalzugangFormularController> neu = FxHilfe.laden("portalzugangFormular.fxml");
        Parent formular = neu.wurzel();
        PortalzugangFormularController c = neu.controller();
        c.setKunde(anna, null);
        assertEquals("K000001 – Schmidt, Anna", FxHilfe.text(formular, "kunde"));
        assertFalse(FxHilfe.suche(formular, "benutzername").isDisabled());
        assertEquals("Startpasswort *", FxHilfe.text(formular, "passwortBeschriftung"));
        FxHilfe.tippe(formular, "benutzername", "anna.s");
        FxHilfe.tippe(formular, "passwort", "start1");
        FxHilfe.tippe(formular, "wiederholung", "start2");
        assertEquals("anna.s", c.getBenutzername());
        assertThrows(IllegalArgumentException.class, c::getPasswort);
        FxHilfe.tippe(formular, "wiederholung", "start1");
        assertArrayEquals("start1".toCharArray(), c.getPasswort());
        assertSame(FxHilfe.suche(formular, "fehler"), c.getFehlerfeld());

        Dialoge.Geladen<PortalzugangFormularController> zurueck = FxHilfe.laden("portalzugangFormular.fxml");
        zurueck.controller().setKunde(anna, Testdaten.kundenzugang(verwaltung, "anna.s", anna.getKundennummer()));
        assertTrue(FxHilfe.suche(zurueck.wurzel(), "benutzername").isDisabled());
        assertEquals("anna.s", FxHilfe.<TextField>suche(zurueck.wurzel(), "benutzername").getText());
        assertEquals("Neues Passwort *", FxHilfe.text(zurueck.wurzel(), "passwortBeschriftung"));
    }

    @Test
    void bar_liestBetragInKontowaehrungUndText() {
        Kontoakte konto = verwaltung.kontoEroeffnen(admin, anna.getKundennummer(), Kontoart.SPARBUCH, Waehrung.FRANC,
                null);
        Dialoge.Geladen<BarFormularController> geladen = FxHilfe.laden("barFormular.fxml");
        Parent formular = geladen.wurzel();
        BarFormularController c = geladen.controller();
        c.setKonto(konto);
        assertTrue(FxHilfe.text(formular, "konto").startsWith(konto.toString()));
        assertEquals("KMF", FxHilfe.text(formular, "waehrung"));
        FxHilfe.tippe(formular, "betrag", "1000");
        FxHilfe.tippe(formular, "text", "Geschenk");
        assertEquals(new Geldbetrag(1000, Waehrung.FRANC), c.getBetrag());
        assertEquals("Geschenk", c.getText());
        assertSame(FxHilfe.suche(formular, "fehler"), c.getFehlerfeld());
    }
}
