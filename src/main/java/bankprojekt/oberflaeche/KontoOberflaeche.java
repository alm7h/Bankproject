package bankprojekt.oberflaeche;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.Parent;
import javafx.scene.control.TextField;
import bankprojekt.basisdaten.Waehrung;
// Übung 13 (Aufgabe 3): das Model, an das die Oberfläche ihre Anzeige bindet
import bankprojekt.basisdaten.Konto;

/**
 * Eine Oberfläche für ein einzelnes Konto. Man kann einzahlen
 * und abheben und sperren und die Adresse des Kontoinhabers 
 * ändern
 * @author Doro
 *
 */
public class KontoOberflaeche{
	private BorderPane flaeche;
	private Text txtUeberschrift;
	private GridPane gridAnzeige;
	private Text txtNummer;
	private Text txtStand;
	private HBox boxAktionen;
	private Text txtGesperrt;
	private Text txtName;
	private Text txtAdresse;
	
	/**
	 * Anzeige und Änderung des Gesperrt-Zustandes
	 */
	private CheckBox gesperrt;
	/**
	 * Anzeige der Kontonummer
	 */
	private Text nummer;
	/**
	 * Anzeige des Kontostandes
	 */
	private Text stand;
	/**
	 * Anzeige und Änderung des Namens des Kontoinhabers
	 */
	private Text name;
	/**
	 * Anzeige und Änderung der Adresse des Kontoinhabers
	 */
	private TextArea adresse;
	/**
	 * Auswahl des Betrags für eine Kontoaktion
	 */
	private TextField betrag;
	/**
	 * Auswahl für die Währung des Betrages für eine Kontoaktion
	 */
	private ChoiceBox<Waehrung> waehrung;
	/**
	 * löst eine Einzahlung aus
	 */
	private Button einzahlen;
	/**
	 * löst eine Abhebung aus
	 */
	private Button abheben;
	/**
	 * Anzeige von Meldungen über Kontoaktionen
	 */
	private Text meldung;
	
	/**
	 * erstellt die Oberfläche
	 */
	public KontoOberflaeche()
	{
		flaeche = new BorderPane();
		txtUeberschrift = new Text("Ein Konto verändern");
		txtUeberschrift.setFont(new Font("Sans Serif", 25));
		BorderPane.setAlignment(txtUeberschrift, Pos.CENTER);
		flaeche.setTop(txtUeberschrift);
		
		gridAnzeige = new GridPane();
		gridAnzeige.setPadding(new Insets(20));
		gridAnzeige.setVgap(10);
		gridAnzeige.setAlignment(Pos.CENTER);
		
		txtNummer = new Text("Kontonummer:");
		txtNummer.setFont(new Font("Sans Serif", 15));
		gridAnzeige.add(txtNummer, 0, 0);
		nummer = new Text();
		nummer.setFont(new Font("Sans Serif", 15));
		GridPane.setHalignment(nummer, HPos.RIGHT);
		gridAnzeige.add(nummer, 1, 0);
		
		txtStand = new Text("Kontostand:");
		txtStand.setFont(new Font("Sans Serif", 15));
		gridAnzeige.add(txtStand, 0, 1);
		stand = new Text();
		stand.setFont(new Font("Sans Serif", 15));
		GridPane.setHalignment(stand, HPos.RIGHT);
		gridAnzeige.add(stand, 1, 1);
		
		txtGesperrt = new Text("Gesperrt: ");
		txtGesperrt.setFont(new Font("Sans Serif", 15));
		gridAnzeige.add(txtGesperrt, 0, 2);
		gesperrt = new CheckBox();
		GridPane.setHalignment(gesperrt, HPos.RIGHT);
		gridAnzeige.add(gesperrt, 1, 2);
		
		txtName = new Text("Name: ");
		txtName.setFont(new Font("Sans Serif", 15));
		gridAnzeige.add(txtName, 0, 3);
		name = new Text();
		name.setFont(new Font("Sans Serif", 15));
		GridPane.setHalignment(name, HPos.RIGHT);
		gridAnzeige.add(name, 1, 3);
		
		txtAdresse = new Text("Adresse: ");
		txtAdresse.setFont(new Font("Sans Serif", 15));
		gridAnzeige.add(txtAdresse, 0, 4);
		adresse = new TextArea();
		adresse.setPrefColumnCount(25);
		adresse.setPrefRowCount(2);
		GridPane.setHalignment(adresse, HPos.RIGHT);
		gridAnzeige.add(adresse, 1, 4);
		
		meldung = new Text("Willkommen lieber Benutzer");
		meldung.setFont(new Font("Sans Serif", 15));
		meldung.setFill(Color.RED);
		gridAnzeige.add(meldung,  0, 5, 2, 1);
		
		flaeche.setCenter(gridAnzeige);
		
		boxAktionen = new HBox();
		boxAktionen.setSpacing(10);
		boxAktionen.setAlignment(Pos.CENTER);
		
		betrag = new TextField("100.00");
		waehrung = new ChoiceBox<Waehrung>();
		waehrung.setItems(FXCollections.observableArrayList(Waehrung.values()));
		waehrung.getSelectionModel().select(0);
		boxAktionen.getChildren().add(betrag);
		boxAktionen.getChildren().add(waehrung);
		einzahlen = new Button("Einzahlen");
		boxAktionen.getChildren().add(einzahlen);
		abheben = new Button("Abheben");
		boxAktionen.getChildren().add(abheben);
		
		flaeche.setBottom(boxAktionen);
	}

	/**
	 * gibt die Oberfläche zum Einfügen in den Scene-Graphen zurück
	 * @return die gefüllte Oberfläche
	 */
	public Parent getView() {
		return flaeche;
	}

	/**
	 * Übung 13 (Aufgabe 3): Richtet die Oberfläche für das übergebene Konto ein.
	 * Alle Anzeigeelemente werden an die Properties des Models gebunden, sodass die
	 * Oberfläche immer den aktuellen Zustand zeigt. Adresse und Gesperrt-Zustand
	 * sind in beide Richtungen gebunden und können über die Oberfläche verändert werden.
	 *
	 * @param konto das anzuzeigende Konto (Model)
	 */
	public void zeigeKonto(Konto konto) {
		// Übung 13 (3a): Kontonummer anzeigen (ändert sich nicht)
		nummer.setText(konto.getKontonummerFormatiert());

		// Übung 13 (3b): immer den aktuellen Kontostand anzeigen ...
		stand.textProperty().bind(konto.kontostandProperty().asString());
		// ... und je nach Plus/Minus einfärben (grün = im Plus, rot = im Minus).
		// Einfache Bindung an imPlusProperty; hält die Farbe automatisch aktuell.
		stand.fillProperty().bind(
				Bindings.when(konto.imPlusProperty())
						.then(Color.GREEN)
						.otherwise(Color.RED));

		// Übung 13 (3e): Name des Kontoinhabers anzeigen
		name.setText(konto.getInhaber().getName());

		// Übung 13: Gesperrt-Zustand anzeigen und per CheckBox verändern (beidseitig)
		gesperrt.selectedProperty().bindBidirectional(konto.gesperrtProperty());

		// Übung 13 (3f): Adresse anzeigen und per Textfeld ändern (beidseitig)
		adresse.setText(konto.getInhaber().getAdresse());
		adresse.textProperty().bindBidirectional(konto.getInhaber().adresseProperty());
	}

	// Übung 13 (Aufgabe 2): Zugriffsmethoden auf die Steuerelemente, damit der
	// Controller die Anzeige an das Model binden und auf Ereignisse reagieren kann.

	/**
	 * Übung 13: die CheckBox für den Gesperrt-Zustand
	 * @return die Gesperrt-CheckBox
	 */
	public CheckBox getGesperrt() {
		return gesperrt;
	}

	/**
	 * Übung 13: die Anzeige der Kontonummer
	 * @return der Kontonummer-Text
	 */
	public Text getNummer() {
		return nummer;
	}

	/**
	 * Übung 13: die Anzeige des Kontostandes
	 * @return der Kontostand-Text
	 */
	public Text getStand() {
		return stand;
	}

	/**
	 * Übung 13: die Anzeige des Inhabernamens
	 * @return der Namens-Text
	 */
	public Text getName() {
		return name;
	}

	/**
	 * Übung 13: das Eingabefeld für die Adresse des Inhabers
	 * @return das Adress-Textfeld
	 */
	public TextArea getAdresse() {
		return adresse;
	}

	/**
	 * Übung 13: das Eingabefeld für den Betrag einer Kontoaktion
	 * @return das Betrag-Textfeld
	 */
	public TextField getBetrag() {
		return betrag;
	}

	/**
	 * Übung 13: die Auswahl der Währung für eine Kontoaktion
	 * @return die Währungs-ChoiceBox
	 */
	public ChoiceBox<Waehrung> getWaehrung() {
		return waehrung;
	}

	/**
	 * Übung 13: der Knopf zum Einzahlen
	 * @return der Einzahlen-Knopf
	 */
	public Button getEinzahlen() {
		return einzahlen;
	}

	/**
	 * Übung 13: der Knopf zum Abheben
	 * @return der Abheben-Knopf
	 */
	public Button getAbheben() {
		return abheben;
	}

	/**
	 * Übung 13: die Anzeige für Meldungen über Kontoaktionen
	 * @return der Meldungs-Text
	 */
	public Text getMeldung() {
		return meldung;
	}
}
