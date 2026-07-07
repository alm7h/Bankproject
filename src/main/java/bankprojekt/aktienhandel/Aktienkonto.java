package bankprojekt.aktienhandel;

// Übung 9: komplette Klasse neu erstellt (Aufgabe 2) und auf
// ExecutorService.submit(...) zur Threadsteuerung umgestellt.
import java.beans.PropertyChangeListener;
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
 * Ein Konto, das zusaetzlich ein Aktiendepot verwaltet. Ueber {@link #kaufauftrag}
 * und {@link #verkaufauftrag} koennen Aktien gekauft bzw. verkauft werden, sobald
 * der Kurs einen gewuenschten Grenzpreis erreicht.
 * <p>
 * Threadsteuerung (Vorgabe): Beide Auftragsmethoden reichen ihre Arbeit ueber
 * {@link ExecutorService#submit(java.util.concurrent.Callable)} an einen
 * ExecutorService und liefern dessen {@link Future} zurueck. Das "asynchrone
 * Warten" auf den passenden Kurs erfolgt OHNE sleep()/wait()/notify(): Der
 * ausgefuehrte Callable blockiert auf einer {@link BlockingQueue}, die vom
 * Kurs-Listener der {@link Aktie} bei jeder Kursaenderung gefuettert wird.
 * Es werden also ausschliesslich die neueren ExecutorServices verwendet,
 * KEIN sleep(), KEIN new Thread(), KEIN wait/notify.
 *
 * Übung 9: komplette Klasse neu erstellt.
 */
public class Aktienkonto extends Konto {

	// Übung 9
	/**
	 * Gemeinsam genutzter ExecutorService, der die Kauf-/Verkaufauftraege aller
	 * Aktienkonten ausfuehrt. Es werden Daemon-Threads verwendet, damit die JVM
	 * beim Beenden nicht durch wartende Auftraege blockiert wird. Der Thread wird
	 * ueber die Standard-ThreadFactory erzeugt (kein eigenes "new Thread()").
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
	 * die aktuell gehaltene Stueckzahl zu. Alle Zugriffe erfolgen unter
	 * synchronized(this), damit Depot und Kontostand konsistent bleiben, auch wenn
	 * mehrere Auftraege gleichzeitig (auf verschiedenen Worker-Threads) handeln.
	 */
	private final Map<String, Integer> depot = new HashMap<>();

	// Übung 9
	/**
	 * erstellt ein Aktienkonto fuer den angegebenen Inhaber mit der angegebenen
	 * Kontonummer und einem anfaenglichen Kontostand von 0.
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

	// Claude changed it
	// Übung 11 (Bemerkung): Die fruehere ueberschriebene abheben()-Methode wurde durch
	// die Einschubmethode pruefeAbhebung() ersetzt, da abheben() jetzt die finale
	// Template-Methode in Konto ist. Die Pruefregel bleibt identisch (Abheben nur, bis
	// der Kontostand 0 erreicht). Die Synchronisation uebernimmt nun die synchronized
	// Template-Methode Konto.abheben(); deshalb braucht pruefeAbhebung() selbst kein
	// synchronized (sie laeuft stets unter dem Lock von abheben()).
	/**
	 * Einschubmethode der Template-Methode {@link Konto#abheben(Geldbetrag)}.
	 * Beim Aktienkonto ist Abheben nur moeglich, bis der Kontostand auf 0 sinkt;
	 * das Konto kann also nicht ins Minus geraten.
	 *
	 * @param betrag abzuhebender Betrag (gueltig, Konto nicht gesperrt)
	 * @return true, wenn der Kontostand abzueglich des Betrags nicht negativ wird
	 */
	@Override
	protected boolean pruefeAbhebung(Geldbetrag betrag) {
		return !getKontostand().minus(betrag).isNegativ();
	}

	// Übung 9
	/**
	 * Zahlt den angegebenen Betrag ein. Ueberschrieben nur, um die Einzahlung mit
	 * den Kauf-/Verkaufauftraegen ueber denselben Lock zu synchronisieren.
	 * @param betrag einzuzahlender Betrag
	 */
	@Override
	public synchronized void einzahlen(Geldbetrag betrag) {
		super.einzahlen(betrag);
	}

	// Übung 9
	/**
	 * Wartet asynchron, bis der Kurs der Aktie mit der angegebenen WKN unter den
	 * Hoechstpreis gefallen ist, und kauft dann anzahl Stueck davon (vermindert den
	 * Kontostand entsprechend und legt sie ins Depot). Reicht der Kontostand zu
	 * diesem Zeitpunkt nicht aus oder gibt es die WKN gar nicht, wird nichts gekauft.
	 *
	 * @param wkn Wertpapierkennnummer der gewuenschten Aktie
	 * @param anzahl gewuenschte Stueckzahl
	 * @param hoechstpreis Kurs, unter den der Aktienkurs fallen muss, bevor gekauft wird
	 * @return Future auf den tatsaechlich bezahlten Gesamtkaufpreis (0 €, wenn nicht gekauft wurde)
	 */
	public Future<Geldbetrag> kaufauftrag(String wkn, int anzahl, Geldbetrag hoechstpreis) {
		// Threadsteuerung ueber den ExecutorService: submit liefert direkt das Future.
		return HANDEL.submit(() -> {
			Aktie aktie = Aktie.getAktie(wkn);
			// Gibt es die WKN nicht oder sind die Parameter ungueltig? -> nichts kaufen.
			if (aktie == null || anzahl <= 0 || hoechstpreis == null)
				return Geldbetrag.NULL_EURO;

			Geldbetrag kurs = warteAufKurs(aktie, k -> k.compareTo(hoechstpreis) <= 0);

			synchronized (this) {
				Geldbetrag gesamtpreis = kurs.mal(anzahl);
				if (abheben(gesamtpreis)) {
					depot.merge(wkn, anzahl, Integer::sum);
					return gesamtpreis;
				}
				// Kontostand nicht ausreichend -> nichts kaufen, 0 € zurueck.
				return Geldbetrag.NULL_EURO;
			}
		});
	}

	// Übung 9
	/**
	 * Gibt es keine Aktie mit der gewuenschten WKN im Depot, findet kein Verkauf
	 * statt (Ergebnis 0 €). Andernfalls wird asynchron gewartet, bis der Kurs den
	 * Minimalpreis erreicht hat; dann wird der komplette Depotbestand dieser Aktie
	 * verkauft (Kontostand wird entsprechend erhoeht).
	 *
	 * @param wkn Wertpapierkennnummer der zu verkaufenden Aktie
	 * @param minimalpreis Kurs, den die Aktie mindestens erreichen muss, bevor verkauft wird
	 * @return Future auf den Gesamterloes (0 €, wenn die Aktie nicht im Depot ist)
	 */
	public Future<Geldbetrag> verkaufauftrag(String wkn, Geldbetrag minimalpreis) {
		// Threadsteuerung ueber den ExecutorService: submit liefert direkt das Future.
		return HANDEL.submit(() -> {
			Aktie aktie = Aktie.getAktie(wkn);
			if (aktie == null || minimalpreis == null)
				return Geldbetrag.NULL_EURO;
			synchronized (this) {
				// Keine Aktie dieser WKN im Depot -> kein Verkauf, 0 € zurueck.
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
				einzahlen(erloes);   // Erloes dem Kontostand gutschreiben
				return erloes;
			}
		});
	}

	// Übung 9
	/**
	 * Blockiert den aufrufenden (Worker-)Thread, bis der Kurs der Aktie die
	 * angegebene Bedingung erfuellt, und liefert diesen Kurs zurueck.
	 * <p>
	 * Das Warten erfolgt OHNE sleep()/wait()/notify(): Ein Kurs-Listener legt jede
	 * Kursaenderung in eine {@link BlockingQueue}; {@link BlockingQueue#take()}
	 * blockiert effizient bis zur naechsten Aenderung. So wird die Bedingung nur
	 * bei tatsaechlichen Kursaenderungen geprueft (kein Busy-Waiting).
	 *
	 * @param aktie die zu beobachtende Aktie
	 * @param bedingung erfuellt, sobald der uebergebene Kurs passt
	 * @return der erste Kurs, der die Bedingung erfuellt
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
			Geldbetrag kurs = aktie.getKurs();           // aktuellen Kurs sofort pruefen
			while (!bedingung.test(kurs)) {
				kurs = kursupdates.take();               // bis zur naechsten Kursaenderung warten
			}
			return kurs;
		} finally {
			aktie.removePropertyChangeListener(beobachter);
		}
	}

	// Übung 9
	/**
	 * Liefert die aktuell im Depot gehaltene Stueckzahl der angegebenen WKN.
	 * (Hilfsmethode, z. B. fuer Tests.)
	 * @param wkn Wertpapierkennnummer
	 * @return gehaltene Stueckzahl (0, wenn nicht im Depot)
	 */
	public synchronized int getDepotbestand(String wkn) {
		return depot.getOrDefault(wkn, 0);
	}
}
