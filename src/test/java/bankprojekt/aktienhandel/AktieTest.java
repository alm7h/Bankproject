package bankprojekt.aktienhandel;

import bankprojekt.basisdaten.Geldbetrag;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Testklasse für die {@link Aktie}.
 * <p>
 * Aktie verwaltet alle Aktien in einer statischen Map und lehnt doppelte WKNs ab,
 * deshalb erhält jeder Test über {@link #neueWkn()} eine eindeutige WKN.
 * Die Kursänderung läuft im Hintergrund über einen ScheduledExecutorService
 * (alle 1 bis 5 Sekunden). Auf sie wird ohne sleep() gewartet: Ein Beobachter
 * vervollständigt ein {@link CompletableFuture}, auf das mit
 * {@code get(timeout)} gewartet wird.
 */
class AktieTest {

    private static final Geldbetrag KURS = new Geldbetrag(100);

    /**
     * Wartezeit für eine Kursänderung: Sie erfolgt spätestens nach 5 Sekunden; der
     * Puffer deckt den seltenen Fall ab, dass eine Änderung so klein ist, dass sie
     * auf Cent gerundet keine Änderung ergibt (dann wird kein Ereignis gefeuert).
     */
    private static final long WARTEZEIT_SEKUNDEN = 20;

    private static String neueWkn() {
        return "TEST-" + UUID.randomUUID();
    }

    @Nested
    class KonstruktorTests {
        @Test
        void konstruktor_setztWknUndKurs() {
            String wkn = neueWkn();
            Aktie aktie = new Aktie(wkn, KURS);
            assertEquals(wkn, aktie.getWkn());
            assertEquals(KURS, aktie.getKurs());
        }

        @Test
        void konstruktor_erlaubtKursNull() {
            // laut JavaDoc wird nur ein negativer Kurs abgelehnt
            Aktie aktie = new Aktie(neueWkn(), Geldbetrag.NULL_EURO);
            assertEquals(Geldbetrag.NULL_EURO, aktie.getKurs());
        }

        @Test
        void konstruktor_wirftIllegalArgumentException_beiNullWerten() {
            assertThrows(IllegalArgumentException.class, () -> new Aktie(null, KURS));
            assertThrows(IllegalArgumentException.class, () -> new Aktie(neueWkn(), null));
        }

        @Test
        void konstruktor_wirftIllegalArgumentException_beiNegativemKurs_undRegistriertNichts() {
            String wkn = neueWkn();
            assertThrows(IllegalArgumentException.class, () -> new Aktie(wkn, new Geldbetrag(-0.01)));
            assertNull(Aktie.getAktie(wkn), "Eine abgelehnte Aktie darf nicht registriert werden");
        }

        @Test
        void konstruktor_wirftIllegalArgumentException_beiDoppelterWkn_undBehaeltDieErsteAktie() {
            String wkn = neueWkn();
            Aktie erste = new Aktie(wkn, KURS);
            assertThrows(IllegalArgumentException.class, () -> new Aktie(wkn, new Geldbetrag(5)));
            assertSame(erste, Aktie.getAktie(wkn));
        }

        @Test
        void gleichzeitigeKonstruktion_mitGleicherWkn_registriertGenauEineAktie() throws Exception {
            // Viele Threads versuchen gleichzeitig, dieselbe WKN anzulegen. Durch das atomare
            // putIfAbsent darf genau einer erfolgreich sein; alle anderen erhalten eine
            // IllegalArgumentException, und registriert ist genau die erfolgreiche Aktie.
            String wkn = neueWkn();
            int anzahl = 16;
            ExecutorService pool = Executors.newFixedThreadPool(anzahl);
            try {
                List<Callable<Aktie>> versuche = new ArrayList<>();
                for (int i = 0; i < anzahl; i++) {
                    versuche.add(() -> new Aktie(wkn, KURS));
                }
                List<Aktie> erfolgreich = new ArrayList<>();
                int abgelehnt = 0;
                for (Future<Aktie> f : pool.invokeAll(versuche, WARTEZEIT_SEKUNDEN, TimeUnit.SECONDS)) {
                    try {
                        erfolgreich.add(f.get());
                    } catch (ExecutionException e) {
                        assertInstanceOf(IllegalArgumentException.class, e.getCause());
                        abgelehnt++;
                    }
                }
                assertEquals(1, erfolgreich.size(), "Genau eine Aktie darf angelegt werden");
                assertEquals(anzahl - 1, abgelehnt);
                assertSame(erfolgreich.get(0), Aktie.getAktie(wkn));
            } finally {
                pool.shutdownNow();
            }
        }
    }

    @Nested
    class GetAktieTests {
        @Test
        void getAktie_liefertDieAktieZurWkn() {
            String wkn = neueWkn();
            Aktie aktie = new Aktie(wkn, KURS);
            assertSame(aktie, Aktie.getAktie(wkn));
        }

        @Test
        void getAktie_liefertNull_beiUnbekannterWkn() {
            assertNull(Aktie.getAktie(neueWkn()));
        }

        @Test
        void getAktie_unterscheidetMehrereAktien() {
            String wkn1 = neueWkn();
            String wkn2 = neueWkn();
            Aktie a1 = new Aktie(wkn1, KURS);
            Aktie a2 = new Aktie(wkn2, new Geldbetrag(50));
            assertSame(a1, Aktie.getAktie(wkn1));
            assertSame(a2, Aktie.getAktie(wkn2));
        }
    }

    /**
     * Beobachter-Tests (Observer-Muster) mit der echten, zeitgesteuerten Kursänderung.
     */
    @Nested
    class BeobachterTests {
        @Test
        void beobachter_erhaeltKursaenderung_mitAltemUndNeuemKurs() throws Exception {
            Aktie aktie = new Aktie(neueWkn(), KURS);
            CompletableFuture<PropertyChangeEvent> ereignis = new CompletableFuture<>();
            aktie.addPropertyChangeListener(ereignis::complete);

            PropertyChangeEvent e = ereignis.get(WARTEZEIT_SEKUNDEN, TimeUnit.SECONDS);

            assertEquals("kurs", e.getPropertyName());
            assertSame(aktie, e.getSource());
            assertEquals(KURS, e.getOldValue(), "Erste Änderung geht vom Startkurs aus");
            Geldbetrag neu = (Geldbetrag) e.getNewValue();
            assertNotEquals(KURS, neu);
            // Änderung um höchstens 3 % in derselben Währung, der Kurs bleibt positiv
            assertTrue(neu.compareTo(new Geldbetrag(97)) >= 0 && neu.compareTo(new Geldbetrag(103)) <= 0,
                    "Kurs außerhalb von ±3 %: " + neu);
            assertEquals(KURS.getWaehrung(), neu.getWaehrung());
            assertFalse(neu.isNegativ());
        }

        @Test
        void getKurs_liefertBeimBenachrichtigenBereitsDenNeuenKurs() throws Exception {
            Aktie aktie = new Aktie(neueWkn(), KURS);
            CompletableFuture<Geldbetrag[]> kurse = new CompletableFuture<>();
            // Innerhalb der Benachrichtigung: getKurs() muss schon den neuen Kurs liefern
            aktie.addPropertyChangeListener(e ->
                    kurse.complete(new Geldbetrag[] {(Geldbetrag) e.getNewValue(), aktie.getKurs()}));

            Geldbetrag[] ergebnis = kurse.get(WARTEZEIT_SEKUNDEN, TimeUnit.SECONDS);

            assertEquals(ergebnis[0], ergebnis[1]);
        }

        @Test
        void kursAendertSich_auchBeiSehrNiedrigemKurs() throws Exception {
            // Bei 0,10 € ist eine Änderung um höchstens 3 % kleiner als ein halber Cent.
            // Würde der Kurs auf Cent gerundet, bliebe er für immer bei 0,10 €.
            Geldbetrag startkurs = new Geldbetrag(0.10);
            Aktie aktie = new Aktie(neueWkn(), startkurs);
            CompletableFuture<Geldbetrag> neuerKurs = new CompletableFuture<>();
            aktie.addPropertyChangeListener(e -> neuerKurs.complete((Geldbetrag) e.getNewValue()));

            Geldbetrag neu = neuerKurs.get(WARTEZEIT_SEKUNDEN, TimeUnit.SECONDS);

            assertNotEquals(startkurs, neu);
            assertTrue(neu.compareTo(new Geldbetrag(0.097)) >= 0 && neu.compareTo(new Geldbetrag(0.103)) <= 0,
                    "Kurs außerhalb von ±3 %: " + neu.getBetrag());
        }

        @Test
        void abgemeldeterBeobachter_wirdNichtMehrBenachrichtigt() throws Exception {
            Aktie aktie = new Aktie(neueWkn(), KURS);
            PropertyChangeListener abgemeldet = mock(PropertyChangeListener.class);
            CompletableFuture<PropertyChangeEvent> ereignis = new CompletableFuture<>();

            aktie.addPropertyChangeListener(abgemeldet);
            aktie.removePropertyChangeListener(abgemeldet);
            aktie.addPropertyChangeListener(ereignis::complete);

            // Sobald der angemeldete Beobachter benachrichtigt wurde, hätte es auch der
            // abgemeldete werden müssen (gleicher firePropertyChange-Aufruf) – er darf es nicht.
            ereignis.get(WARTEZEIT_SEKUNDEN, TimeUnit.SECONDS);
            verify(abgemeldet, never()).propertyChange(any());
        }
    }
}
