package spielereien;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import bankprojekt.aktienhandel.Aktie;
import bankprojekt.aktienhandel.Aktienkonto;
import bankprojekt.basisdaten.Geldbetrag;
import bankprojekt.exceptions.GesperrtException;

/**
 * Kleines Hauptprogramm (Präsentationsschicht) für den Aktienhandel.
 * <p>
 * Es legt drei Aktien an, erteilt mehrere Kauf- und Verkaufaufträge gleichzeitig
 * und gibt die jeweiligen Kaufpreise bzw. Erlöse aus. Nebenläufig werden die
 * aktuellen Kurse alle paar Sekunden angezeigt, damit man die Bearbeitung der
 * Aufträge verfolgen kann.
 * <p>
 * Threadsteuerung ausschließlich über ExecutorServices:
 * <ul>
 *   <li>die nebenläufige Kursanzeige läuft über einen ScheduledExecutorService,</li>
 *   <li>die Aufträge laufen über den ExecutorService des Aktienkontos,</li>
 *   <li>das Einsammeln der Ergebnisse erfolgt über {@link Future#get}.</li>
 * </ul>
 * KEIN sleep(), KEIN new Thread(), KEIN wait/notify.
 * <p>
 * Hinweis: Nur dieses Programm der Präsentationsschicht gibt etwas auf der
 * Konsole aus; die Verarbeitungsschicht (Aktie, Aktienkonto, Konto) bleibt
 * vollständig ausgabefrei.
 * @author m.alharethi
 */
public class Aktienspielereien {

	/**
	 * Verknüpft die Beschreibung eines Auftrags mit seinem Future, damit das
	 * Ergebnis später passend beschriftet ausgegeben werden kann.
	 * @param beschreibung Klartext-Beschreibung des Auftrags
	 * @param future das vom Aktienkonto gelieferte Future auf Kaufpreis/Erlös
	 */
	private record Auftrag(String beschreibung, Future<Geldbetrag> future) {}

	/**
	 * Hauptprogramm.
	 * @param args wird nicht benutzt
	 * @throws InterruptedException wenn das Warten auf ein Ergebnis unterbrochen wird
	 * @throws GesperrtException wenn das Aktienkonto gesperrt ist (kommt hier nicht vor)
	 */
	public static void main(String[] args) throws InterruptedException, GesperrtException {
		// 1) Drei Aktien anlegen.
		Aktie bmw = new Aktie("BMW111", new Geldbetrag(100));
		Aktie sap = new Aktie("SAP222", new Geldbetrag(150));
		Aktie tui = new Aktie("TUI333", new Geldbetrag(60));

		// 2) Ein Aktienkonto mit Startkapital anlegen.
		Aktienkonto konto = new Aktienkonto();
		konto.einzahlen(new Geldbetrag(1_000_000));
		System.out.println("=== Aktienhandel ===");
		System.out.println("Startkapital : " + konto.getKontostand());
		System.out.println("Startkurse   : " + kurseAlsText(bmw, sap, tui));
		System.out.println();

		// 3) Grundbestand aufbauen, damit es später etwas zu verkaufen gibt.
		//    Höchstpreis großzügig -> diese Kaeufe werden sofort ausgeführt.
		System.out.println("-- Grundbestand aufbauen (sofortige Kaeufe) --");
		drucke("GRUNDKAUF 100x SAP222",
				warteAufErgebnis(konto.kaufauftrag("SAP222", 100, new Geldbetrag(100_000))));
		drucke("GRUNDKAUF 200x TUI333",
				warteAufErgebnis(konto.kaufauftrag("TUI333", 200, new Geldbetrag(100_000))));
		System.out.println("Kontostand nach Grundkaeufen: " + konto.getKontostand());
		System.out.println();

		// 4) Nebenläufige Kursanzeige alle 2 Sekunden über einen ScheduledExecutorService.
		ScheduledExecutorService kursanzeige =
				Executors.newSingleThreadScheduledExecutor(runnable -> {
					Thread t = Executors.defaultThreadFactory().newThread(runnable);
					t.setDaemon(true);
					t.setName("Kursanzeige");
					return t;
				});
		kursanzeige.scheduleAtFixedRate(
				() -> System.out.println("   [Kurse] " + kurseAlsText(bmw, sap, tui)),
				2, 2, TimeUnit.SECONDS);

		// 5) Mehrere Kauf- UND Verkaufaufträge GLEICHZEITIG erteilen. Jeder Aufruf
		//    startet sofort einen nebenläufigen Auftrag (im ExecutorService des
		//    Aktienkontos) und liefert ein Future zurück.
		System.out.println("-- Mehrere Auftraege gleichzeitig (warten auf passende Kurse) --");
		List<Auftrag> auftraege = new ArrayList<>();
		auftraege.add(new Auftrag("KAUF    50x BMW111 @<= 99,50 €",
				konto.kaufauftrag("BMW111", 50, new Geldbetrag(99.50))));
		auftraege.add(new Auftrag("KAUF    20x BMW111 @<= 99,00 €",
				konto.kaufauftrag("BMW111", 20, new Geldbetrag(99.00))));
		auftraege.add(new Auftrag("VERKAUF    SAP222 @>= 150,75 €",
				konto.verkaufauftrag("SAP222", new Geldbetrag(150.75))));
		auftraege.add(new Auftrag("VERKAUF    TUI333 @>= 60,30 €",
				konto.verkaufauftrag("TUI333", new Geldbetrag(60.30))));

		// 6) Ergebnisse einsammeln und ausgeben. Die Aufträge laufen bereits alle
		//    parallel; hier wird nur auf ihr jeweiliges Ergebnis gewartet.
		for (Auftrag a : auftraege) {
			try {
				Geldbetrag ergebnis = a.future().get(90, TimeUnit.SECONDS);
				drucke("FERTIG  " + a.beschreibung(), ergebnis);
			} catch (TimeoutException e) {
				a.future().cancel(true); // Auftrag wurde in der Zeit nicht ausgelöst
				System.out.println("TIMEOUT " + a.beschreibung() + "  -> storniert");
			} catch (ExecutionException e) {
				System.out.println("FEHLER  " + a.beschreibung() + "  -> " + e.getCause());
			}
		}

		// 7) Endstand ausgeben und die Kursanzeige beenden.
		System.out.println();
		System.out.println("-- Endstand --");
		System.out.println("Endkontostand: " + konto.getKontostand());
		System.out.println("Depot        : BMW111=" + konto.getDepotbestand("BMW111")
				+ ", SAP222=" + konto.getDepotbestand("SAP222")
				+ ", TUI333=" + konto.getDepotbestand("TUI333"));
		kursanzeige.shutdownNow();
	}

	/**
	 * Hilfsmethode: holt das Ergebnis eines Auftrags-Futures (sofortige Aufträge).
	 * @param future das Future
	 * @return der enthaltene Geldbetrag, oder 0 € bei Problemen
	 */
	private static Geldbetrag warteAufErgebnis(Future<Geldbetrag> future) throws InterruptedException {
		try {
			return future.get();
		} catch (ExecutionException e) {
			return Geldbetrag.NULL_EURO;
		}
	}

	/**
	 * Hilfsmethode für die einheitliche Ergebnisausgabe.
	 */
	private static void drucke(String beschreibung, Geldbetrag betrag) {
		System.out.println(beschreibung + "  ->  " + betrag);
	}

	/**
	 * Hilfsmethode: aktuelle Kurse der drei Aktien als Text.
	 */
	private static String kurseAlsText(Aktie bmw, Aktie sap, Aktie tui) {
		return "BMW111=" + bmw.getKurs() + " | SAP222=" + sap.getKurs() + " | TUI333=" + tui.getKurs();
	}
}
