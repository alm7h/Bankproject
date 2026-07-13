package bankprojekt.aktienhandel;

// Übung 12: Neuer Test fuer das Observer-Muster des Aktienkontos (Aufgabe 12 a).
// Geprueft wird mit Mockito, dass ein angemeldeter Beobachter bei jeder
// Depotaenderung (Kauf bzw. Verkauf von Aktien) korrekt benachrichtigt wird.
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import bankprojekt.basisdaten.Geldbetrag;
import bankprojekt.basisdaten.Kunde;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Testet das Observer-Muster des {@link Aktienkonto}s (Übung 12 a).
 * <p>
 * Wichtiger Hinweis aus der Aufgabenstellung: Die statischen Methoden von
 * {@link Aktie} lassen sich nicht mocken, deshalb wird mit einem "echten"
 * Aktie-Objekt gearbeitet. Hoechstkaufpreis und Mindestverkaufpreis werden so
 * gewaehlt, dass Kauf bzw. Verkauf sofort (beim aktuellen Kurs) stattfinden.
 * Der Beobachter selbst ist ein Mockito-Mock, dessen Aufruf verifiziert wird.
 */
class AktienkontoObserverTest {

	// Übung 12: eindeutige WKN pro Testmethode, da Aktie alle Aktien statisch
	// verwaltet und den Konstruktor bei doppelter WKN abweist.
	private static final Geldbetrag KURS = new Geldbetrag(100.0);

	private Aktienkonto neuesKontoMitGuthaben() {
		Aktienkonto konto = new Aktienkonto(new Kunde(), 1234);
		konto.einzahlen(new Geldbetrag(1_000_000.0)); // reichlich Guthaben fuer den Kauf
		return konto;
	}


	// Übung 12: Beim Kauf muss der Beobachter genau einmal benachrichtigt werden.
	@Test
	void beobachterWirdBeiKaufBenachrichtigt() throws Exception {
		new Aktie("TESTKAUF", KURS);
		Aktienkonto konto = neuesKontoMitGuthaben();

		PropertyChangeListener beobachter = mock(PropertyChangeListener.class);
		konto.addPropertyChangeListener(beobachter);

		// Hoechstkaufpreis extrem hoch -> Bedingung sofort erfuellt -> Kauf sofort.
		Future<Geldbetrag> auftrag =
				konto.kaufauftrag("TESTKAUF", 5, new Geldbetrag(1_000_000.0));
		auftrag.get(5, TimeUnit.SECONDS); // auf Abschluss des Kaufs warten

		// Der Beobachter muss genau einmal ueber die Depotaenderung informiert worden sein.
		ArgumentCaptor<PropertyChangeEvent> captor =
				ArgumentCaptor.forClass(PropertyChangeEvent.class);
		verify(beobachter, times(1)).propertyChange(captor.capture());

		PropertyChangeEvent evt = captor.getValue();
		assertEquals("TESTKAUF", evt.getPropertyName()); // WKN als Property-Name
		assertEquals(0, evt.getOldValue());              // vorher kein Bestand
		assertEquals(5, evt.getNewValue());              // nachher 5 Stueck
	}


	// Übung 12: Beim Verkauf muss der Beobachter erneut benachrichtigt werden
	// (alter Bestand -> 0).
	@Test
	void beobachterWirdBeiVerkaufBenachrichtigt() throws Exception {
		new Aktie("TESTVERKAUF", KURS);
		Aktienkonto konto = neuesKontoMitGuthaben();

		// Zuerst Aktien ins Depot legen (ohne Beobachter, damit nur der Verkauf zaehlt).
		konto.kaufauftrag("TESTVERKAUF", 3, new Geldbetrag(1_000_000.0))
				.get(5, TimeUnit.SECONDS);

		PropertyChangeListener beobachter = mock(PropertyChangeListener.class);
		konto.addPropertyChangeListener(beobachter);

		// Mindestverkaufpreis 0 € -> Bedingung sofort erfuellt -> Verkauf sofort.
		konto.verkaufauftrag("TESTVERKAUF", Geldbetrag.NULL_EURO)
				.get(5, TimeUnit.SECONDS);

		ArgumentCaptor<PropertyChangeEvent> captor =
				ArgumentCaptor.forClass(PropertyChangeEvent.class);
		verify(beobachter, times(1)).propertyChange(captor.capture());

		PropertyChangeEvent evt = captor.getValue();
		assertEquals("TESTVERKAUF", evt.getPropertyName());
		assertEquals(3, evt.getOldValue()); // vorher 3 Stueck
		assertEquals(0, evt.getNewValue()); // komplett verkauft
	}


	// Übung 12: Ein abgemeldeter Beobachter darf nicht mehr benachrichtigt werden.
	@Test
	void abgemeldeterBeobachterWirdNichtBenachrichtigt() throws Exception {
		new Aktie("TESTABMELDE", KURS);
		Aktienkonto konto = neuesKontoMitGuthaben();

		PropertyChangeListener beobachter = mock(PropertyChangeListener.class);
		konto.addPropertyChangeListener(beobachter);
		konto.removePropertyChangeListener(beobachter);

		konto.kaufauftrag("TESTABMELDE", 2, new Geldbetrag(1_000_000.0))
				.get(5, TimeUnit.SECONDS);

		verify(beobachter, never()).propertyChange(any());
	}
}
