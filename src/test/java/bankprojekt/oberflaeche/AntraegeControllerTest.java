package bankprojekt.oberflaeche;

import bankprojekt.basisdaten.Geldbetrag;
import bankprojekt.verwaltung.Antragsstatus;
import bankprojekt.verwaltung.Bankverwaltung;
import bankprojekt.verwaltung.Benutzer;
import bankprojekt.verwaltung.Kontoantrag;
import bankprojekt.verwaltung.Kontoart;
import bankprojekt.verwaltung.Kundenakte;
import bankprojekt.verwaltung.Kundenportal;
import bankprojekt.verwaltung.Testdaten;
import bankprojekt.verwaltung.VerstellbareUhr;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testklasse für die Antragsansicht der Verwaltung ({@link AntraegeController}).
 */
@ExtendWith(JavaFxErweiterung.class)
class AntraegeControllerTest {

    private Bankverwaltung verwaltung;
    private Benutzer admin;
    private Kontoantrag carla;
    private Kontoantrag zusatz;
    private Parent ansicht;
    private AntraegeController controller;
    private TableView<Kontoantrag> tabelle;
    private int entscheidungen;

    @BeforeEach
    void setUp() {
        verwaltung = Testdaten.verwaltung(new VerstellbareUhr());
        admin = Testdaten.admin(verwaltung);
        carla = Testdaten.neukunde(verwaltung, "carla.k", Testdaten.kunde("Carla", "Klein"), Kontoart.GIROKONTO);
        Kundenakte anna = verwaltung.kundeAnlegen(admin, Testdaten.kunde("Anna", "Schmidt"));
        zusatz = new Kundenportal(verwaltung).zusatzkontoBeantragen(
                Testdaten.kundenzugang(verwaltung, "anna.s", anna.getKundennummer()), Kontoart.SPARBUCH,
                bankprojekt.basisdaten.Waehrung.EURO);

        Dialoge.Geladen<AntraegeController> geladen = FxHilfe.laden("antraege.fxml");
        ansicht = geladen.wurzel();
        controller = geladen.controller();
        controller.setSitzung(new Sitzung(verwaltung, admin));
        controller.setNachEntscheidung(() -> entscheidungen++);
        controller.aktualisieren();
        FxHilfe.auslegen(ansicht);
        tabelle = FxHilfe.tabelle(ansicht);
    }

    private Button knopf(String id) {
        return FxHilfe.suche(ansicht, id);
    }

    private ComboBox<Antragsstatus> statusFilter() {
        return FxHilfe.suche(ansicht, "statusFilter");
    }

    @Test
    void zeigtZuerstDieOffenenAntraege() {
        assertEquals(Antragsstatus.OFFEN, statusFilter().getValue());
        assertEquals(List.of(carla, zusatz), tabelle.getItems());
        assertEquals("A000001", FxHilfe.zelle(tabelle, "Antrag", carla));
        assertEquals("14.10.2026 10:00:00", FxHilfe.zelle(tabelle, "Eingereicht", carla));
        assertEquals("Neukunde", FxHilfe.zelle(tabelle, "Art", carla));
        assertEquals("Klein, Carla", FxHilfe.zelle(tabelle, "Antragsteller", carla));
        assertEquals("Girokonto", FxHilfe.zelle(tabelle, "Kontoart", carla));
        assertEquals("EUR", FxHilfe.zelle(tabelle, "Währung", carla));
        assertEquals("offen", FxHilfe.zelle(tabelle, "Status", carla));
        assertEquals("2 Anträge angezeigt, 2 offen", FxHilfe.text(ansicht, "anzahl"));
        assertFalse(FxHilfe.suche(ansicht, "detailInhalt").isVisible());
    }

    @Test
    void details_einesNeukunden_undEinesZusatzkontos() {
        tabelle.getSelectionModel().select(carla);
        String details = FxHilfe.text(ansicht, "details");
        assertTrue(details.startsWith("A000001 – Neukunde"));
        assertTrue(details.contains("Benutzername: carla.k"));
        assertTrue(details.contains("Gewünscht: Girokonto in EUR"));
        assertTrue(details.contains("Name: Carla Klein"));
        assertTrue(details.contains("Telefon: –"));
        assertFalse(knopf("genehmigenKnopf").isDisabled());

        tabelle.getSelectionModel().select(zusatz);
        assertTrue(FxHilfe.text(ansicht, "details").contains("Kunde: K000001 – Schmidt, Anna"));
    }

    @Test
    void genehmigen_einesGirokontos_mitDispo() {
        tabelle.getSelectionModel().select(carla);
        Dialogantworten antworten = new Dialogantworten().dann(d -> {
            Parent formular = Dialogantworten.inhalt(d);
            assertTrue(FxHilfe.suche(formular, "dispoZeile").isVisible());
            Dialogantworten.knopf(d, "Genehmigen").fire();
            assertEquals("Dispo ist ein Pflichtfeld.", FxHilfe.text(formular, "fehler"));
            FxHilfe.tippe(formular, "dispo", "500");
            Dialogantworten.knopf(d, "Genehmigen").fire();
        });
        knopf("genehmigenKnopf").fire();

        assertEquals("Antrag A000001 genehmigen", antworten.letzter().getHeaderText());
        assertEquals(Antragsstatus.GENEHMIGT, carla.getStatus());
        assertEquals(1, entscheidungen);
        assertEquals(List.of(zusatz), tabelle.getItems(), "nur noch offene");
        assertEquals("Klein, Carla", verwaltung.findeKunde("K000002").orElseThrow().getName());
    }

    @Test
    void genehmigen_ohneDispo_beiAnderenKontoarten() {
        tabelle.getSelectionModel().select(zusatz);
        new Dialogantworten().dann(d -> {
            assertFalse(FxHilfe.suche(Dialogantworten.inhalt(d), "dispoZeile").isVisible());
            Dialogantworten.knopf(d, "Genehmigen").fire();
        });
        knopf("genehmigenKnopf").fire();
        assertEquals(Antragsstatus.GENEHMIGT, zusatz.getStatus());
    }

    @Test
    void ablehnen_mitPflichtbegruendung() {
        tabelle.getSelectionModel().select(carla);
        new Dialogantworten().dann(d -> {
            Parent formular = Dialogantworten.inhalt(d);
            Dialogantworten.knopf(d, "Ablehnen").fire();
            assertEquals("Begründung ist ein Pflichtfeld.", FxHilfe.text(formular, "fehler"));
            FxHilfe.tippe(formular, "begruendung", "Gehaltsnachweis fehlt");
            Dialogantworten.knopf(d, "Ablehnen").fire();
        });
        knopf("ablehnenKnopf").fire();
        assertEquals(Antragsstatus.ABGELEHNT, carla.getStatus());
        assertEquals("Gehaltsnachweis fehlt", carla.getBegruendung());
        assertEquals(1, entscheidungen);
    }

    @Test
    void entschiedeneAntraege_zeigenDieEntscheidung_undSindNichtMehrEntscheidbar() {
        verwaltung.antragAblehnen(admin, carla.getAntragsnummer(), "nein");
        verwaltung.antragGenehmigen(admin, zusatz.getAntragsnummer(), null);
        statusFilter().getSelectionModel().select(0);   // "Alle"
        assertNull(statusFilter().getValue());
        assertEquals(2, tabelle.getItems().size());
        assertEquals("2 Anträge angezeigt, 0 offen", FxHilfe.text(ansicht, "anzahl"));

        tabelle.getSelectionModel().select(carla);
        assertTrue(FxHilfe.text(ansicht, "details").contains("Status: abgelehnt von admin"));
        assertTrue(FxHilfe.text(ansicht, "details").contains("Begründung: nein"));
        assertTrue(knopf("genehmigenKnopf").isDisabled() && knopf("ablehnenKnopf").isDisabled());
        tabelle.getSelectionModel().select(zusatz);
        assertFalse(FxHilfe.text(ansicht, "details").contains("Begründung"));

        statusFilter().setValue(Antragsstatus.GENEHMIGT);
        assertEquals("1 Antrag angezeigt, 0 offen", FxHilfe.text(ansicht, "anzahl"));
        assertSame(zusatz, tabelle.getSelectionModel().getSelectedItem(), "Auswahl bleibt");
    }

    @Test
    void abgebrocheneEntscheidungen_aendernNichts_undOhneAuswahlPassiertNichts() {
        new Dialogantworten().klicke("Abbrechen").klicke("Abbrechen");
        tabelle.getSelectionModel().select(carla);
        knopf("genehmigenKnopf").fire();
        knopf("ablehnenKnopf").fire();
        assertEquals(Antragsstatus.OFFEN, carla.getStatus());
        assertEquals(0, entscheidungen);

        tabelle.getSelectionModel().clearSelection();
        knopf("genehmigenKnopf").fire();
        knopf("ablehnenKnopf").fire();

        controller.setNachEntscheidung(null);
        tabelle.getSelectionModel().select(zusatz);
        new Dialogantworten().klicke("Genehmigen");
        knopf("genehmigenKnopf").fire();
        assertEquals(Antragsstatus.GENEHMIGT, zusatz.getStatus(), "auch ohne Folgeaktion");
    }

    @Test
    void ohneSitzung_bleibtDieTabelleLeer() {
        Dialoge.Geladen<AntraegeController> leer = FxHilfe.laden("antraege.fxml");
        leer.controller().aktualisieren();
        assertTrue(FxHilfe.<Kontoantrag>tabelle(leer.wurzel()).getItems().isEmpty());
    }

    @Test
    void genehmigenEinesGirokontos_mitDispoInDerKontowaehrung() {
        tabelle.getSelectionModel().select(carla);
        new Dialogantworten().dann(d -> {
            FxHilfe.tippe(Dialogantworten.inhalt(d), "dispo", "250,50");
            Dialogantworten.knopf(d, "Genehmigen").fire();
        });
        knopf("genehmigenKnopf").fire();
        assertNotNull(verwaltung.findeKonto(carla.getKontonummer()).orElseThrow());
        assertEquals(new Geldbetrag(0), verwaltung.findeKonto(carla.getKontonummer()).orElseThrow().getKontostand());
    }
}
