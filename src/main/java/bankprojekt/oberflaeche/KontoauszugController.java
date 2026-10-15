package bankprojekt.oberflaeche;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import bankprojekt.basisdaten.Geldbetrag;
import bankprojekt.verwaltung.Buchung;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

/**
 * Controller des Kontoauszugs ({@code kontoauszug.fxml}): zeigt die Buchungen eines
 * Kontos, die neueste zuerst; Abgänge erscheinen rot.
 * @author m.alharethi
 */
public class KontoauszugController {
    /**
     * Wird vom FXMLLoader beim Laden des Kontoauszugs aufgerufen.
     */
    public KontoauszugController() {
    }

    /** Datum und Uhrzeit einer Buchung */
    private static final DateTimeFormatter BUCHUNGSZEIT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    /** Tabelle der Buchungen */
    @FXML
    private TableView<Buchung> auszugTabelle;
    /** Spalte Datum */
    @FXML
    private TableColumn<Buchung, String> datumSpalte;
    /** Spalte Art */
    @FXML
    private TableColumn<Buchung, String> artSpalte;
    /** Spalte Text */
    @FXML
    private TableColumn<Buchung, String> textSpalte;
    /** Spalte Betrag */
    @FXML
    private TableColumn<Buchung, Geldbetrag> betragSpalte;
    /** Spalte Saldo nach der Buchung */
    @FXML
    private TableColumn<Buchung, Geldbetrag> saldoSpalte;

    /**
     * Richtet die Spalten ein.
     */
    @FXML
    private void initialize() {
        datumSpalte.setCellValueFactory(z -> new ReadOnlyStringWrapper(BUCHUNGSZEIT.format(z.getValue().zeitpunkt())));
        artSpalte.setCellValueFactory(z -> new ReadOnlyStringWrapper(z.getValue().art().toString()));
        textSpalte.setCellValueFactory(z -> new ReadOnlyStringWrapper(z.getValue().text()));
        betragSpalte.setCellValueFactory(z -> new ReadOnlyObjectWrapper<>(z.getValue().betrag()));
        betragSpalte.setCellFactory(s -> Darstellung.betragZelle());
        saldoSpalte.setCellValueFactory(z -> new ReadOnlyObjectWrapper<>(z.getValue().saldoDanach()));
        saldoSpalte.setCellFactory(s -> Darstellung.betragZelle());
    }

    /**
     * Zeigt Buchungen an.
     * @param buchungen die Buchungen, älteste zuerst (wie sie die Verwaltung liefert)
     */
    public void setBuchungen(List<Buchung> buchungen) {
        List<Buchung> neuesteZuerst = new ArrayList<>(buchungen);
        Collections.reverse(neuesteZuerst);
        auszugTabelle.getItems().setAll(neuesteZuerst);
    }
}
