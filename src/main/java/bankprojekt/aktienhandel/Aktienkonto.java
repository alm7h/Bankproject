package bankprojekt.aktienhandel;

// Übung 9: komplette Klasse neu erstellt (Aufgabe 2) und auf
// ExecutorService.submit(...) zur Threadsteuerung umgestellt.
import java.beans.PropertyChangeListener;

// Übung 12: PropertyChangeSupport importiert, damit das Aktienkonto als
// beobachtetes Subjekt seine Beobachter über Depotänderungen (Kauf/Verkauf)
// informieren kann (Observer-Muster, Aufgabe 12 a).
import java.beans.PropertyChangeSupport;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

import bankprojekt.basisdaten.Geldbetrag;
import bankprojekt.basisdaten.Konto;
import bankprojekt.basisdaten.Kunde;

/**
 * Ein Konto, das zusätzlich ein Aktiendepot verwaltet. Über {@link #kaufauftrag}
 * und {@link #verkaufauftrag} können Aktien gekauft bzw. verkauft werden, sobald
 * der Kurs einen gewünschten Grenzpreis erreicht.
 * <p>
 * Threadsteuerung (Vorgabe): Beide Auftragsmethoden reichen ihre Arbeit über
 * {@link ExecutorService#submit(java.util.concurrent.Callable)} an einen
 * ExecutorService und liefern dessen {@link Future} zurück. Das "asynchrone
 * Warten" auf den passenden Kurs erfolgt OHNE sleep()/wait()/notify(): Der
 * ausgeführte Callable blockiert auf einer {@link BlockingQueue}, die vom
 * Kurs-Listener der {@link Aktie} bei jeder Kursänderung gefüttert wird.
 * Es werden also ausschließlich die neueren ExecutorServices verwendet,
 * KEIN sleep(), KEIN new Thread(), KEIN wait/notify.
 *
 * Übung 9: komplette Klasse neu erstellt.
 */
public class Aktienkonto extends Konto {

	// Übung 9
	/**
	 * Gemeinsam genutzter ExecutorService, der die Kauf-/Verkaufaufträge aller
	 * Aktienkonten ausführt. Es werden Daemon-Threads verwendet, damit die JVM
	 * beim Beenden nicht durch wartende Aufträge blockiert wird. Der Thread wird
	 * über die Standard-ThreadFactory erzeugt (kein eigenes "new Thread()").
	 */
	private static final ExecutorService HANDEL =
			Executors.newCachedThreadPool(runnable -> {
				Thread t = Executors.defaultThreadFactory().newThread(runnable);
				t.setDaemon(true);
				t.setName("Aktienkonto-Handel");
				return t;
			});

	// Übung 9
	/**
	 * Das Aktiendepot: ordnet jeder im Depot enthaltenen Wertpapierkennnummer (WKN)
	 * die aktuell gehaltene Stückzahl zu. Alle Zugriffe erfolgen unter
	 * synchronized(this), damit Depot und Kontostand konsistent bleiben, auch wenn
	 * mehrere Aufträge gleichzeitig (auf verschiedenen Worker-Threads) handeln.
	 */
	private final Map<String, Integer> depot = new HashMap<>();


	// Übung 12: Das Aktienkonto ist das beobachtete Subjekt des Observer-Musters.
	// Über diesen PropertyChangeSupport werden die angemeldeten Beobachter bei
	// jeder Aenderung des Depotbestands (Aktien kommen hinzu oder werden entfernt)
	// benachrichtigt - NICHT bei bloßen Kursänderungen. PropertyChangeSupport ist
	// thread-sicher, das Feuern erfolgt aus den Worker-Threads der Aufträge heraus.
	private final PropertyChangeSupport depotAenderungen = new PropertyChangeSupport(this);

	// Übung 9
	/**
	 * erstellt ein Aktienkonto für den angegebenen Inhaber mit der angegebenen
	 * Kontonummer und einem anfänglichen Kontostand von 0.
	 * @param inhaber Kontoinhaber
	 * @param kontonummer Kontonummer
	 */
	public Aktienkonto(Kunde inhaber, long kontonummer) {
		super(inhaber, kontonummer);
	}

	// Übung 9
	/**
	 * erstellt ein Standard-Aktienkonto (Max Mustermann).
	 */
	public Aktienkonto() {
		super();
	}


	// Übung 11 (Bemerkung): Die frühere überschriebene abheben()-Methode wurde durch
	// die Einschubmethode pruefeAbhebung() ersetzt, da abheben() jetzt die finale
	// Template-Methode in Konto ist. Die Prüfregel bleibt identisch (Abheben nur, bis
	// der Kontostand 0 erreicht). Die Synchronisation übernimmt nun die synchronized
	// Template-Methode Konto.abheben(); deshalb braucht pruefeAbhebung() selbst kein
	// synchronized (sie läuft stets unter dem Lock von abheben()).
	/**
	 * Einschubmethode der Template-Methode {@link Konto#abheben(Geldbetrag)}.
	 * Beim Aktienkonto ist Abheben nur möglich, bis der Kontostand auf 0 sinkt;
	 * das Konto kann also nicht ins Minus geraten.
	 *
	 * @param betrag abzuhebender Betrag (gültig, Konto nicht gesperrt)
	 * @return true, wenn der Kontostand abzüglich des Betrags nicht negativ wird
	 */
	@Override
	protected boolean pruefeAbhebung(Geldbetrag betrag) {
		return !getKontostand().minus(betrag).isNegativ();
	}

	// Übung 9
	/**
	 * Zahlt den angegebenen Betrag ein. Ueberschrieben nur, um die Einzahlung mit
	 * den Kauf-/Verkaufaufträgen über denselben Lock zu synchronisieren.
	 * @param betrag einzuzahlender Betrag
	 */
	@Override
	public synchronized void einzahlen(Geldbetrag betrag) {
		super.einzahlen(betrag);
	}

	// Übung 9
	/**
	 * Wartet asynchron, bis der Kurs der Aktie mit der angegebenen WKN unter den
	 * Höchstpreis gefallen ist, und kauft dann anzahl Stück davon (vermindert den
	 * Kontostand entsprechend und legt sie ins Depot). Reicht der Kontostand zu
	 * diesem Zeitpunkt nicht aus oder gibt es die WKN gar nicht, wird nichts gekauft.
	 *
	 * @param wkn Wertpapierkennnummer der gewünschten Aktie
	 * @param anzahl gewünschte Stückzahl
	 * @param höchstpreis Kurs, unter den der Aktienkurs fallen muss, bevor gekauft wird
	 * @return Future auf den tatsächlich bezahlten Gesamtkaufpreis (0 €, wenn nicht gekauft wurde)
	 */
	public Future<Geldbetrag> kaufauftrag(String wkn, int anzahl, Geldbetrag hoechstpreis) {
		// Threadsteuerung über den ExecutorService: submit liefert direkt das Future.
		return HANDEL.submit(() -> {
			Aktie aktie = Aktie.getAktie(wkn);
			// Gibt es die WKN nicht oder sind die Parameter ungültig? -> nichts kaufen.
			if (aktie == null || anzahl <= 0 || hoechstpreis == null)
				return Geldbetrag.NULL_EURO;

			Geldbetrag kurs = warteAufKurs(aktie, k -> k.compareTo(hoechstpreis) <= 0);

			synchronized (this) {
				Geldbetrag gesamtpreis = kurs.mal(anzahl);
				if (abheben(gesamtpreis)) {

					// Übung 12: alten Bestand merken, damit die Beobachter über die
					// konkrete Aenderung (alter -> neuer Bestand dieser WKN) informiert
					// werden können.
					int alterBestand = depot.getOrDefault(wkn, 0);
					depot.merge(wkn, anzahl, Integer::sum);
					int neuerBestand = depot.get(wkn);
					// Übung 12: Beobachter über den Zuwachs im Depot benachrichtigen
					// (Property-Name = WKN, alter/neuer Bestand als Werte).
					depotAenderungen.firePropertyChange(wkn, alterBestand, neuerBestand);
					return gesamtpreis;
				}
				// Kontostand nicht ausreichend -> nichts kaufen, 0 € zurück.
				return Geldbetrag.NULL_EURO;
			}
		});
	}

	// Übung 9
	/**
	 * Gibt es keine Aktie mit der gewünschten WKN im Depot, findet kein Verkauf
	 * statt (Ergebnis 0 €). Andernfalls wird asynchron gewartet, bis der Kurs den
	 * Minimalpreis erreicht hat; dann wird der komplette Depotbestand dieser Aktie
	 * verkauft (Kontostand wird entsprechend erhöht).
	 *
	 * @param wkn Wertpapierkennnummer der zu verkaufenden Aktie
	 * @param minimalpreis Kurs, den die Aktie mindestens erreichen muss, bevor verkauft wird
	 * @return Future auf den Gesamterlös (0 €, wenn die Aktie nicht im Depot ist)
	 */
	public Future<Geldbetrag> verkaufauftrag(String wkn, Geldbetrag minimalpreis) {
		// Threadsteuerung über den ExecutorService: submit liefert direkt das Future.
		return HANDEL.submit(() -> {
			Aktie aktie = Aktie.getAktie(wkn);
			if (aktie == null || minimalpreis == null)
				return Geldbetrag.NULL_EURO;
			synchronized (this) {
				// Keine Aktie dieser WKN im Depot -> kein Verkauf, 0 € zurück.
				if (depot.getOrDefault(wkn, 0) <= 0)
					return Geldbetrag.NULL_EURO;
			}

			Geldbetrag kurs = warteAufKurs(aktie, k -> k.compareTo(minimalpreis) >= 0);

			synchronized (this) {
				int bestand = depot.getOrDefault(wkn, 0);
				if (bestand <= 0)
					return Geldbetrag.NULL_EURO;
				Geldbetrag erloes = kurs.mal(bestand);
				depot.remove(wkn);   // kompletten Bestand dieser Aktie verkaufen

				// Übung 12: Beobachter über das Entfernen der Aktien aus dem Depot
				// benachrichtigen (alter Bestand -> 0 für diese WKN).
				depotAenderungen.firePropertyChange(wkn, bestand, 0);
				einzahlen(erloes);   // Erlös dem Kontostand gutschreiben
				return erloes;
			}
		});
	}

	// Übung 9
	/**
	 * Blockiert den aufrufenden (Worker-)Thread, bis der Kurs der Aktie die
	 * angegebene Bedingung erfüllt, und liefert diesen Kurs zurück.
	 * <p>
	 * Das Warten erfolgt OHNE sleep()/wait()/notify(): Ein Kurs-Listener legt jede
	 * Kursänderung in eine {@link BlockingQueue}; {@link BlockingQueue#take()}
	 * blockiert effizient bis zur nächsten Aenderung. So wird die Bedingung nur
	 * bei tatsächlichen Kursänderungen geprüft (kein Busy-Waiting).
	 *
	 * @param aktie die zu beobachtende Aktie
	 * @param bedingung erfüllt, sobald der übergebene Kurs passt
	 * @return der erste Kurs, der die Bedingung erfüllt
	 * @throws InterruptedException wenn der Worker-Thread unterbrochen wird
	 */
	private static Geldbetrag warteAufKurs(Aktie aktie,
	                                       java.util.function.Predicate<Geldbetrag> bedingung)
			throws InterruptedException {
		BlockingQueue<Geldbetrag> kursupdates = new LinkedBlockingQueue<>();
		PropertyChangeListener beobachter =
				evt -> kursupdates.offer((Geldbetrag) evt.getNewValue());
		aktie.addPropertyChangeListener(beobachter);
		try {
			Geldbetrag kurs = aktie.getKurs();           // aktuellen Kurs sofort prüfen
			while (!bedingung.test(kurs)) {
				kurs = kursupdates.take();               // bis zur nächsten Kursänderung warten
			}
			return kurs;
		} finally {
			aktie.removePropertyChangeListener(beobachter);
		}
	}

	// Übung 9
	/**
	 * Liefert die aktuell im Depot gehaltene Stückzahl der angegebenen WKN.
	 * (Hilfsmethode, z. B. für Tests.)
	 * @param wkn Wertpapierkennnummer
	 * @return gehaltene Stückzahl (0, wenn nicht im Depot)
	 */
	public synchronized int getDepotbestand(String wkn) {
		return depot.getOrDefault(wkn, 0);
	}


	// Übung 12: An-/Abmeldung der Beobachter (Observer-Muster). Das Aktienkonto ist
	// das beobachtete Subjekt; ein angemeldeter Beobachter wird bei jeder
	// Depotänderung (Kauf/Verkauf) über propertyChange(...) benachrichtigt.
	/**
	 * Meldet einen Beobachter an, der bei jeder Aenderung des Aktiendepots
	 * (Kauf oder Verkauf von Aktien) benachrichtigt wird.
	 * @param beobachter der zu benachrichtigende Listener
	 */
	public void addPropertyChangeListener(PropertyChangeListener beobachter) {
		depotAenderungen.addPropertyChangeListener(beobachter);
	}


	// Übung 12: Gegenstück zur Anmeldung eines Depot-Beobachters.
	/**
	 * Meldet einen zuvor angemeldeten Depot-Beobachter wieder ab.
	 * @param beobachter der abzumeldende Listener
	 */
	public void removePropertyChangeListener(PropertyChangeListener beobachter) {
		depotAenderungen.removePropertyChangeListener(beobachter);
	}
}
