package bankprojekt.aktienhandel;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
// Bemerkung: Imports für die Thread-Steuerung über ExecutorServices
// (bewusst KEIN sleep(), KEIN new Thread(), KEIN wait/notify).
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
// Bemerkung: PropertyChangeSupport für die Beobachtung der Kursänderungen
// (Observer-Pattern). Dadurch kann ein Aktienkonto ereignisgesteuert auf den
// passenden Kurs warten - ohne Polling, ohne sleep()/wait()/notify().
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import bankprojekt.basisdaten.Geldbetrag;

/**
 * Eine Aktie, die ständig ihren Kurs verändert
 * @author Doro
 *
 * @author m.alharethi
 */
public class Aktie {
	
	private static Map<String, Aktie> alleAktien = new ConcurrentHashMap<>();

	/**
	 * Gemeinsam genutzter ScheduledExecutorService, der die regelmäßigen
	 * Kursänderungen ALLER Aktien steuert. Es werden Daemon-Threads verwendet,
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
	/**
	 * aktueller Kurs der Aktie.
	 * Bemerkung: volatile, da der Kurs vom Scheduler-Thread geschrieben und
	 * gleichzeitig von anderen Threads (z. B. über getKurs()) gelesen wird.
	 * Da Geldbetrag unveränderlich ist, genügt volatile für die sichere
	 * Veröffentlichung des jeweils neuen Kurses.
	 */
	private volatile Geldbetrag kurs;


	/**
	 * Verwaltet die angemeldeten Beobachter und benachrichtigt sie bei jeder
	 * Kursänderung. PropertyChangeSupport ist thread-sicher; das Feuern erfolgt
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


		// Bemerkung: Zufällige Zeitspanne zeit zwischen 1 und 5 Sekunden (beide
		// inklusive). nextInt(1, 6) liefert Werte aus {1, 2, 3, 4, 5}.
		int zeit = ThreadLocalRandom.current().nextInt(1, 6);

		// Bemerkung: Sorgt dafür, dass alle zeit Sekunden der Kurs verändert
		// wird. scheduleAtFixedRate übernimmt die periodische Ausführung
		// vollständig - dadurch kein sleep(), kein new Thread(), kein wait/notify.
		// Erste Aenderung nach zeit Sekunden, danach wieder alle zeit Sekunden.
		KURS_SCHEDULER.scheduleAtFixedRate(this::kursAendern, zeit, zeit, TimeUnit.SECONDS);
	}


	/**
	 * Verändert den aktuellen Kurs der Aktie um einen zufälligen Prozentsatz.
	 * Diese Methode wird regelmäßig vom {@link #KURS_SCHEDULER} aufgerufen.
	 * Bemerkung: Der Prozentsatz liegt zwischen -3 % und +3 % (mit
	 * Nachkommastellen). Der Multiplikationsfaktor ist damit mindestens 0,97,
	 * ein positiver Kurs bleibt also stets positiv.
	 */
	private void kursAendern() {
		// Zufallszahl zwischen -3 und 3 (gerne mit Nachkommastellen)
		double prozent = ThreadLocalRandom.current().nextDouble(-3.0, 3.0);
		// Alten Kurs merken, um die Beobachter zu informieren
		Geldbetrag alterKurs = this.kurs;
		// Kurs um diese Prozentzahl verändern: neuer Kurs = alter Kurs * (1 + p/100)
		// Bemerkung: Bewusst NICHT über Geldbetrag.mal, denn mal rundet auf Cent. Bei
		// einem Kurs unter etwa 0,17 € ist eine Änderung um höchstens 3 % kleiner als ein
		// halber Cent und würde immer auf den alten Kurs zurückgerundet - der Kurs bliebe
		// stehen. Der Kurs wird daher ungerundet geführt; gerundet wird erst der
		// Gesamtpreis eines Kaufs bzw. Verkaufs (kurs.mal(anzahl) im Aktienkonto).
		this.kurs = new Geldbetrag(alterKurs.getBetrag() * (1.0 + prozent / 100.0),
				alterKurs.getWaehrung());
		// Alle angemeldeten Beobachter über die Kursänderung
		// informieren (Grundlage für die Kauf-/Verkaufaufträge im Aktienkonto).
		kursAenderungen.firePropertyChange("kurs", alterKurs, this.kurs);
	}


	/**
	 * Meldet einen Beobachter an, der bei jeder Kursänderung benachrichtigt wird.
	 * @param beobachter der zu benachrichtigende Listener
	 */
	public void addPropertyChangeListener(PropertyChangeListener beobachter) {
		kursAenderungen.addPropertyChangeListener(beobachter);
	}


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
}
