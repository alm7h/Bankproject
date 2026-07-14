package bankprojekt.oberflaeche;

import java.time.LocalDate;

import bankprojekt.basisdaten.Geldbetrag;
import bankprojekt.basisdaten.Girokonto;
import bankprojekt.basisdaten.Konto;
import bankprojekt.basisdaten.Kunde;
import bankprojekt.basisdaten.Waehrung;
import bankprojekt.exceptions.GesperrtException;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Übung 13 (Aufgabe 2): Controller im Sinne von MVC für ein einzelnes Konto.
 * <p>
 * Der Controller
 * <ul>
 *   <li>(a) bringt die {@link KontoOberflaeche} (View) auf den Bildschirm,</li>
 *   <li>(b) erzeugt ein {@link Girokonto} (Model) und verbindet es mit der Oberfläche,</li>
 *   <li>(c) enthält die Methoden, um auf die Ereignisse der Oberfläche zu reagieren
 *       (Einzahlen, Abheben, Sperren, Adressänderung).</li>
 * </ul>
 * <p>
 * Gestartet wird die Anwendung über die Klasse {@link Start} (nicht über diese
 * Klasse direkt), damit JavaFX auch vom Klassenpfad aus zuverlässig hochfährt.
 *
 * @see Start
 */
public class KontoController extends Application {

	/**
	 * Übung 13: das Model – das anzuzeigende und zu verändernde Konto
	 */
	private Konto konto;

	/**
	 * Übung 13: die View – die vorgegebene Oberfläche
	 */
	private KontoOberflaeche oberflaeche;

	/**
	 * Übung 13 (Aufgabe 2a + 2b): Erzeugt Model und View, verbindet beide und
	 * bringt die Oberfläche auf den Bildschirm.
	 * @param buehne die von JavaFX bereitgestellte Hauptbühne
	 */
	@Override
	public void start(Stage buehne) {
		// Übung 13 (2b): Model erzeugen – Inhaber und Konto frei festgelegt
		Kunde inhaber = new Kunde("Doro", "Hubrich",
				"Beispielstraße 1, 12345 Musterstadt",
				LocalDate.of(1980, 5, 20));
		this.konto = new Girokonto(inhaber, 1234567, new Geldbetrag(1000));

		// Übung 13: View erzeugen
		this.oberflaeche = new KontoOberflaeche();

		// Übung 13 (Aufgabe 3): die View richtet ihre Anzeige selbst am Model ein
		oberflaeche.zeigeKonto(konto);
		// Übung 13 (Aufgabe 2c): der Controller reagiert auf die Ereignisse
		ereignisseVerbinden();

		// Übung 13 (2a): Oberfläche auf den Bildschirm bringen
		Scene szene = new Scene(oberflaeche.getView(), 500, 400);
		buehne.setTitle("Ein Konto verändern");
		buehne.setScene(szene);
		buehne.show();
	}

	/**
	 * Übung 13 (Aufgabe 2c): Verbindet die Knöpfe der Oberfläche mit den
	 * Reaktionsmethoden des Controllers.
	 */
	private void ereignisseVerbinden() {
		oberflaeche.getEinzahlen().setOnAction(ereignis -> einzahlen());
		oberflaeche.getAbheben().setOnAction(ereignis -> abheben());
	}

	/**
	 * Übung 13 (Aufgabe 2c): Reaktion auf den Einzahlen-Knopf.
	 */
	private void einzahlen() {
		try {
			Geldbetrag betrag = betragAusEingabe();
			konto.einzahlen(betrag);
			oberflaeche.getMeldung().setText("Einzahlung von " + betrag + " erfolgreich.");
		} catch (NumberFormatException e) {
			oberflaeche.getMeldung().setText("Bitte einen gültigen Betrag eingeben.");
		} catch (IllegalArgumentException e) {
			oberflaeche.getMeldung().setText("Einzahlung nicht möglich: " + e.getMessage());
		}
	}

	/**
	 * Übung 13 (Aufgabe 2c): Reaktion auf den Abheben-Knopf.
	 */
	private void abheben() {
		try {
			Geldbetrag betrag = betragAusEingabe();
			boolean erfolg = konto.abheben(betrag);
			if (erfolg) {
				oberflaeche.getMeldung().setText("Abhebung von " + betrag + " erfolgreich.");
			} else {
				oberflaeche.getMeldung().setText("Abhebung abgelehnt: nicht genug Deckung.");
			}
		} catch (GesperrtException e) {
			oberflaeche.getMeldung().setText("Das Konto ist gesperrt – Abheben nicht möglich.");
		} catch (NumberFormatException e) {
			oberflaeche.getMeldung().setText("Bitte einen gültigen Betrag eingeben.");
		} catch (IllegalArgumentException e) {
			oberflaeche.getMeldung().setText("Abhebung nicht möglich: " + e.getMessage());
		}
	}

	/**
	 * Übung 13 (Aufgabe 2c): Liest Betrag und Währung aus den Eingabefeldern der
	 * Oberfläche und baut daraus einen {@link Geldbetrag}.
	 * @return der eingegebene Geldbetrag
	 * @throws NumberFormatException wenn im Betragsfeld keine gültige Zahl steht
	 */
	private Geldbetrag betragAusEingabe() throws NumberFormatException {
		double wert = Double.parseDouble(oberflaeche.getBetrag().getText().trim().replace(',', '.'));
		Waehrung waehrung = oberflaeche.getWaehrung().getValue();
		return new Geldbetrag(wert, waehrung);
	}
}
