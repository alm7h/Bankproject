package bankprojekt.aktienhandel;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
// Übung 9
// Bemerkung: Imports fuer die Thread-Steuerung ueber ExecutorServices
// (bewusst KEIN sleep(), KEIN new Thread(), KEIN wait/notify).
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
// Übung 9
// Bemerkung: PropertyChangeSupport fuer die Beobachtung der Kursaenderungen
// (Observer-Pattern). Dadurch kann ein Aktienkonto ereignisgesteuert auf den
// passenden Kurs warten - ohne Polling, ohne sleep()/wait()/notify().
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import bankprojekt.basisdaten.Geldbetrag;

/**
 * Eine Aktie, die ständig ihren Kurs verändert
 * @author Doro
 *
 */
public class Aktie {
	
	private static Map<String, Aktie> alleAktien = new ConcurrentHashMap<>();

	// Übung 9
	/**
	 * Gemeinsam genutzter ScheduledExecutorService, der die regelmaessigen
	 * Kursaenderungen ALLER Aktien steuert. Es werden Daemon-Threads verwendet,
	 * damit die JVM beim Beenden des Hauptprogramms nicht durch die laufenden
	 * Hintergrundaufgaben blockiert wird.
	 * Bemerkung: Bewusst ein zentraler ExecutorService statt eigener Threads -
	 * so entstehen nicht pro Aktie ein eigener Thread, sondern nur wenige
	 * wiederverwendete Worker-Threads.
	 */
	private static final ScheduledExecutorService KURS_SCHEDULER =
			Executors.newScheduledThreadPool(
					Runtime.getRuntime().availableProcessors(),
					runnable -> {
						// Bemerkung: Wir nutzen die Standard-ThreadFactory des
						// Executor-Frameworks und markieren den erzeugten Thread
						// nur als Daemon. Dadurch kommt unser Code OHNE eigenes
						// "new Thread()" aus (Vorgabe eingehalten).
						Thread t = Executors.defaultThreadFactory().newThread(runnable);
						t.setDaemon(true); // Daemon: blockiert das Beenden der JVM nicht
						t.setName("Aktie-Kursaenderung");
						return t;
					});

	private String wkn;
	// Übung 9
	/**
	 * aktueller Kurs der Aktie.
	 * Bemerkung: volatile, da der Kurs vom Scheduler-Thread geschrieben und
	 * gleichzeitig von anderen Threads (z. B. ueber getKurs()) gelesen wird.
	 * Da Geldbetrag unveraenderlich ist, genuegt volatile fuer die sichere
	 * Veroeffentlichung des jeweils neuen Kurses.
	 */
	private volatile Geldbetrag kurs;

	// Übung 9
	/**
	 * Verwaltet die angemeldeten Beobachter und benachrichtigt sie bei jeder
	 * Kursaenderung. PropertyChangeSupport ist thread-sicher; das Feuern erfolgt
	 * aus dem Scheduler-Thread heraus.
	 */
	private final PropertyChangeSupport kursAenderungen = new PropertyChangeSupport(this);

	/**
	 * gibt die Aktie mit der gewünschten Wertpapierkennnummer zurück
	 * @param wkn Wertpapierkennnummer
	 * @return Aktie mit der angegebenen Wertpapierkennnummer oder null, wenn es diese WKN
	 * 			nicht gibt.
	 */
	public static Aktie getAktie(String wkn)
	{
		return alleAktien.get(wkn);
	}
	
	/**
	 * erstellt eine neu Aktie mit den angegebenen Werten
	 * @param wkn Wertpapierkennnummer
	 * @param k aktueller Kurs
	 * @throws IllegalArgumentException wenn einer der Parameter null bzw. negativ ist
	 * 		                            oder es eine Aktie mit dieser WKN bereits gibt
	 */
	public Aktie(String wkn, Geldbetrag k) {
		if(wkn == null || k == null || k.isNegativ() || alleAktien.containsKey(wkn))
			throw new IllegalArgumentException();	
		this.wkn = wkn;
		this.kurs = k;
		alleAktien.put(wkn, this);

		// Übung 9
		// Bemerkung: Zufaellige Zeitspanne zeit zwischen 1 und 5 Sekunden (beide
		// inklusive). nextInt(1, 6) liefert Werte aus {1, 2, 3, 4, 5}.
		int zeit = ThreadLocalRandom.current().nextInt(1, 6);

		// Bemerkung: Sorgt dafuer, dass alle zeit Sekunden der Kurs veraendert
		// wird. scheduleAtFixedRate uebernimmt die periodische Ausfuehrung
		// vollstaendig - dadurch kein sleep(), kein new Thread(), kein wait/notify.
		// Erste Aenderung nach zeit Sekunden, danach wieder alle zeit Sekunden.
		KURS_SCHEDULER.scheduleAtFixedRate(this::kursAendern, zeit, zeit, TimeUnit.SECONDS);
	}


	/**
	 * Veraendert den aktuellen Kurs der Aktie um einen zufaelligen Prozentsatz.
	 * Diese Methode wird regelmaessig vom {@link #KURS_SCHEDULER} aufgerufen.
	 * Bemerkung: Der Prozentsatz liegt zwischen -3 % und +3 % (mit
	 * Nachkommastellen). Der Multiplikationsfaktor ist damit mindestens 0,97,
	 * ein positiver Kurs bleibt also stets positiv.
	 */
	private void kursAendern() {
		// Zufallszahl zwischen -3 und 3 (gerne mit Nachkommastellen)
		double prozent = ThreadLocalRandom.current().nextDouble(-3.0, 3.0);
		// Übung 9: alten Kurs merken, um die Beobachter zu informieren
		Geldbetrag alterKurs = this.kurs;
		// Kurs um diese Prozentzahl veraendern: neuer Kurs = alter Kurs * (1 + p/100)
		this.kurs = this.kurs.mal(1.0 + prozent / 100.0);
		// Übung 9: alle angemeldeten Beobachter ueber die Kursaenderung
		// informieren (Grundlage fuer die Kauf-/Verkaufauftraege im Aktienkonto).
		kursAenderungen.firePropertyChange("kurs", alterKurs, this.kurs);
	}

	// Übung 9
	/**
	 * Meldet einen Beobachter an, der bei jeder Kursaenderung benachrichtigt wird.
	 * @param beobachter der zu benachrichtigende Listener
	 */
	public void addPropertyChangeListener(PropertyChangeListener beobachter) {
		kursAenderungen.addPropertyChangeListener(beobachter);
	}

	// Übung 9
	/**
	 * Meldet einen zuvor angemeldeten Beobachter wieder ab.
	 * @param beobachter der abzumeldende Listener
	 */
	public void removePropertyChangeListener(PropertyChangeListener beobachter) {
		kursAenderungen.removePropertyChangeListener(beobachter);
	}

	/**
	 * Wertpapierkennnummer
	 * @return WKN der Aktie
	 */
	public String getWkn() {
		return wkn;
	}

	/**
	 * aktueller Kurs
	 * @return Kurs der Aktie
	 */
	public Geldbetrag getKurs() {
		return kurs;
	}

	// =========================================================================
	// Übung 9:
	// - Importe fuer ExecutorService/ScheduledExecutorService ergaenzt
	// - statischen, gemeinsamen ScheduledExecutorService mit Daemon-Threads
	//   hinzugefuegt (KURS_SCHEDULER)
	// - Feld kurs auf volatile umgestellt (Thread-Sichtbarkeit)
	// - Konstruktor startet die periodische Kursaenderung alle 'zeit' (1-5) Sekunden
	// - private Methode kursAendern() fuer die zufaellige Kursaenderung (-3% .. +3%)
	// Die Thread-Steuerung erfolgt ausschliesslich ueber den ExecutorService:
	// kein sleep(), kein new Thread(), kein wait/notify.
	// =========================================================================
}
