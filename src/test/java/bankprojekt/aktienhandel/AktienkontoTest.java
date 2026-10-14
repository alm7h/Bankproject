package bankprojekt.aktienhandel;

import bankprojekt.basisdaten.Geldbetrag;
import bankprojekt.basisdaten.Kunde;
import bankprojekt.exceptions.GesperrtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testklasse für das {@link Aktienkonto} (Depot, Kauf- und Verkaufaufträge).
 * <p>
 * Der Kurs einer {@link Aktie} ändert sich zufällig alle 1 bis 5 Sekunden. Damit die
 * Tests deterministisch bleiben, gilt:
 * <ul>
 *   <li>Jeder Test verwendet eine eigene WKN (statisches Aktienregister).</li>
 *   <li>Höchstkaufpreis bzw. Mindestverkaufpreis sind so gewählt, dass die Bedingung
 *       sofort erfüllt ist; erwartete Kontostände werden aus dem gelieferten
 *       Kauf- bzw. Verkaufpreis berechnet, nicht aus einem festen Kurs.</li>
 *   <li>Für "der Auftrag wartet" wird eine Grenze gewählt, die der Kurs in der kurzen
 *       Wartezeit nicht erreichen kann (höchstens 3 % pro Änderung, Abstand mindestens
 *       1 Sekunde).</li>
 *   <li>Gewartet wird nur über {@code Future.get(timeout)}, nie mit sleep().</li>
 * </ul>
 */
class AktienkontoTest {

    private static final Geldbetrag KURS = new Geldbetrag(100);
    private static final Geldbetrag SOFORT_KAUFEN = new Geldbetrag(1_000_000);
    private static final Geldbetrag SOFORT_VERKAUFEN = Geldbetrag.NULL_EURO;
    private static final long TIMEOUT = 10;

    private Aktienkonto konto;
    private String wkn;

    @BeforeEach
    void setUp() {
        konto = new Aktienkonto(new Kunde("Max", "Mustermann", "Weg 1", 1980, 1, 1), 4711);
        konto.einzahlen(new Geldbetrag(10_000));
        wkn = neueAktie(KURS);
    }

    private static String neueAktie(Geldbetrag kurs) {
        String wkn = "TEST-" + UUID.randomUUID();
        new Aktie(wkn, kurs);
        return wkn;
    }

    private static Geldbetrag ergebnis(Future<Geldbetrag> auftrag) throws Exception {
        return auftrag.get(TIMEOUT, TimeUnit.SECONDS);
    }

    @Nested
    class KontoTests {
        @Test
        void konstruktor_setztWerte_undDepotIstLeer() {
            Kunde k = new Kunde("Anna", "Aktie", "x", 1990, 1, 1);
            Aktienkonto neu = new Aktienkonto(k, 99);
            assertSame(k, neu.getInhaber());
            assertEquals(99, neu.getKontonummer());
            assertEquals(Geldbetrag.NULL_EURO, neu.getKontostand());
            assertEquals(0, neu.getDepotbestand(wkn));
        }

        @Test
        void standardkonstruktor_erzeugtMustermannKonto() {
            Aktienkonto standard = new Aktienkonto();
            assertSame(Kunde.MUSTERMANN, standard.getInhaber());
            assertEquals(1234567, standard.getKontonummer());
        }

        @Test
        void abheben_erlaubtGenauDenKontostand_undKeinUeberziehen() throws GesperrtException {
            assertFalse(konto.abheben(new Geldbetrag(10_000.01)));
            assertTrue(konto.abheben(new Geldbetrag(10_000)));
            assertEquals(Geldbetrag.NULL_EURO, konto.getKontostand());
            assertFalse(konto.abheben(new Geldbetrag(0.01)));
        }
    }

    @Nested
    class KaufTests {
        @Test
        void kaufauftrag_kauftSofort_undBuchtPreisAb() throws Exception {
            Geldbetrag preis = ergebnis(konto.kaufauftrag(wkn, 5, SOFORT_KAUFEN));

            assertEquals(5, konto.getDepotbestand(wkn));
            assertFalse(preis.isNegativ());
            assertNotEquals(Geldbetrag.NULL_EURO, preis);
            assertEquals(new Geldbetrag(10_000).minus(preis), konto.getKontostand());
            // 5 Stück zu einem Kurs, der höchstens um wenige Prozent von 100 € abweicht
            assertTrue(preis.compareTo(new Geldbetrag(400)) > 0 && preis.compareTo(new Geldbetrag(600)) < 0,
                    "unplausibler Kaufpreis: " + preis);
        }

        @Test
        void mehrereKaeufe_summierenSichImDepot() throws Exception {
            Geldbetrag p1 = ergebnis(konto.kaufauftrag(wkn, 2, SOFORT_KAUFEN));
            Geldbetrag p2 = ergebnis(konto.kaufauftrag(wkn, 3, SOFORT_KAUFEN));
            assertEquals(5, konto.getDepotbestand(wkn));
            assertEquals(new Geldbetrag(10_000).minus(p1).minus(p2), konto.getKontostand());
        }

        @Test
        void kaufauftrag_kauftNichts_wennKontostandNichtReicht() throws Exception {
            // 1000 Stück zu etwa 100 € kosten weit mehr als die 10.000 € Guthaben
            Geldbetrag preis = ergebnis(konto.kaufauftrag(wkn, 1000, SOFORT_KAUFEN));
            assertEquals(Geldbetrag.NULL_EURO, preis);
            assertEquals(0, konto.getDepotbestand(wkn));
            assertEquals(new Geldbetrag(10_000), konto.getKontostand());
        }

        @Test
        void kaufauftrag_kauftNichts_beiUnbekannterWkn() throws Exception {
            String unbekannt = "TEST-" + UUID.randomUUID();
            assertEquals(Geldbetrag.NULL_EURO, ergebnis(konto.kaufauftrag(unbekannt, 1, SOFORT_KAUFEN)));
            assertEquals(0, konto.getDepotbestand(unbekannt));
            assertEquals(new Geldbetrag(10_000), konto.getKontostand());
        }

        @Test
        void kaufauftrag_kauftNichts_beiUngueltigerAnzahlOderOhneHoechstpreis() throws Exception {
            assertEquals(Geldbetrag.NULL_EURO, ergebnis(konto.kaufauftrag(wkn, 0, SOFORT_KAUFEN)));
            assertEquals(Geldbetrag.NULL_EURO, ergebnis(konto.kaufauftrag(wkn, -1, SOFORT_KAUFEN)));
            assertEquals(Geldbetrag.NULL_EURO, ergebnis(konto.kaufauftrag(wkn, 1, null)));
            assertEquals(0, konto.getDepotbestand(wkn));
            assertEquals(new Geldbetrag(10_000), konto.getKontostand());
        }

        @Test
        void kaufauftrag_wartet_solangeDerKursUeberDemHoechstpreisLiegt() throws Exception {
            // 1 € liegt 99 % unter dem Kurs - in 1,5 Sekunden unerreichbar
            Future<Geldbetrag> auftrag = konto.kaufauftrag(wkn, 1, new Geldbetrag(1));
            assertThrows(TimeoutException.class, () -> auftrag.get(1500, TimeUnit.MILLISECONDS));
            assertFalse(auftrag.isDone());
            assertTrue(auftrag.cancel(true), "Ein wartender Auftrag muss sich abbrechen lassen");
            assertEquals(0, konto.getDepotbestand(wkn));
            assertEquals(new Geldbetrag(10_000), konto.getKontostand());
        }
    }

    @Nested
    class VerkaufTests {
        @Test
        void verkaufauftrag_verkauftDenKomplettenBestand_undSchreibtErloesGut() throws Exception {
            Geldbetrag preis = ergebnis(konto.kaufauftrag(wkn, 4, SOFORT_KAUFEN));
            Geldbetrag erloes = ergebnis(konto.verkaufauftrag(wkn, SOFORT_VERKAUFEN));

            assertEquals(0, konto.getDepotbestand(wkn));
            assertNotEquals(Geldbetrag.NULL_EURO, erloes);
            assertEquals(new Geldbetrag(10_000).minus(preis).plus(erloes), konto.getKontostand());
        }

        @Test
        void verkaufauftrag_verkauftNichts_wennAktieNichtImDepot() throws Exception {
            assertEquals(Geldbetrag.NULL_EURO, ergebnis(konto.verkaufauftrag(wkn, SOFORT_VERKAUFEN)));
            assertEquals(new Geldbetrag(10_000), konto.getKontostand());
        }

        @Test
        void verkaufauftrag_verkauftNichts_beiUnbekannterWknOderOhneMindestpreis() throws Exception {
            ergebnis(konto.kaufauftrag(wkn, 1, SOFORT_KAUFEN));
            Geldbetrag stand = konto.getKontostand();
            assertEquals(Geldbetrag.NULL_EURO,
                    ergebnis(konto.verkaufauftrag("TEST-" + UUID.randomUUID(), SOFORT_VERKAUFEN)));
            assertEquals(Geldbetrag.NULL_EURO, ergebnis(konto.verkaufauftrag(wkn, null)));
            assertEquals(1, konto.getDepotbestand(wkn));
            assertEquals(stand, konto.getKontostand());
        }

        @Test
        void zweiterVerkaufauftrag_findetNichtsMehrImDepot() throws Exception {
            ergebnis(konto.kaufauftrag(wkn, 2, SOFORT_KAUFEN));
            ergebnis(konto.verkaufauftrag(wkn, SOFORT_VERKAUFEN));
            Geldbetrag stand = konto.getKontostand();
            assertEquals(Geldbetrag.NULL_EURO, ergebnis(konto.verkaufauftrag(wkn, SOFORT_VERKAUFEN)));
            assertEquals(stand, konto.getKontostand());
        }

        @Test
        void verkaufauftrag_wartet_solangeDerKursUnterDemMindestpreisLiegt() throws Exception {
            ergebnis(konto.kaufauftrag(wkn, 2, SOFORT_KAUFEN));
            Geldbetrag stand = konto.getKontostand();

            // 1.000.000 € liegen weit über dem Kurs - in 1,5 Sekunden unerreichbar
            Future<Geldbetrag> auftrag = konto.verkaufauftrag(wkn, new Geldbetrag(1_000_000));
            assertThrows(TimeoutException.class, () -> auftrag.get(1500, TimeUnit.MILLISECONDS));
            assertTrue(auftrag.cancel(true));
            assertEquals(2, konto.getDepotbestand(wkn), "Der Bestand bleibt bis zum Verkauf im Depot");
            assertEquals(stand, konto.getKontostand());
        }
    }

    @Nested
    class GesperrtTests {
        @Test
        void kaufauftrag_wirftGesperrtException_wennKontoGesperrt() throws Exception {
            konto.sperren();
            assertThrows(GesperrtException.class, () -> konto.kaufauftrag(wkn, 1, SOFORT_KAUFEN));
            assertEquals(0, konto.getDepotbestand(wkn));
            assertEquals(new Geldbetrag(10_000), konto.getKontostand());
        }

        @Test
        void verkaufauftrag_wirftGesperrtException_wennKontoGesperrt() throws Exception {
            ergebnis(konto.kaufauftrag(wkn, 2, SOFORT_KAUFEN));
            Geldbetrag stand = konto.getKontostand();
            konto.sperren();
            assertThrows(GesperrtException.class, () -> konto.verkaufauftrag(wkn, SOFORT_VERKAUFEN));
            assertEquals(2, konto.getDepotbestand(wkn));
            assertEquals(stand, konto.getKontostand());
        }

        @Test
        void auftraege_sindNachEntsperrenWiederMoeglich() throws Exception {
            konto.sperren();
            konto.entsperren();
            ergebnis(konto.kaufauftrag(wkn, 1, SOFORT_KAUFEN));
            assertNotEquals(Geldbetrag.NULL_EURO, ergebnis(konto.verkaufauftrag(wkn, SOFORT_VERKAUFEN)));
        }

        /*
         * Für "während des Wartens gesperrt" hält der Test den Monitor des Kontos: Der
         * Auftrag (Bedingung sofort erfüllt) kann seinen synchronized-Handelsschritt erst
         * nach dem Freigeben ausführen - und bis dahin ist das Konto bereits gesperrt.
         */
        @Test
        void kaufauftrag_kauftNichts_wennKontoWaehrendDesWartensGesperrtWird() throws Exception {
            Future<Geldbetrag> auftrag;
            synchronized (konto) {
                auftrag = konto.kaufauftrag(wkn, 1, SOFORT_KAUFEN);
                konto.sperren();
            }
            assertEquals(Geldbetrag.NULL_EURO, ergebnis(auftrag), "kein Kauf und keine Exception im Future");
            assertEquals(0, konto.getDepotbestand(wkn));
            assertEquals(new Geldbetrag(10_000), konto.getKontostand());
        }

        @Test
        void verkaufauftrag_verkauftNichts_wennKontoWaehrendDesWartensGesperrtWird() throws Exception {
            ergebnis(konto.kaufauftrag(wkn, 2, SOFORT_KAUFEN));
            Geldbetrag stand = konto.getKontostand();
            Future<Geldbetrag> auftrag;
            synchronized (konto) {
                auftrag = konto.verkaufauftrag(wkn, SOFORT_VERKAUFEN);
                konto.sperren();
            }
            assertEquals(Geldbetrag.NULL_EURO, ergebnis(auftrag));
            assertEquals(2, konto.getDepotbestand(wkn), "Die Aktien bleiben im Depot");
            assertEquals(stand, konto.getKontostand());
        }
    }

    @Nested
    class NebenlaeufigkeitTests {
        @Test
        void gleichzeitigeKaeufe_haltenDepotUndKontostandKonsistent() throws Exception {
            List<Future<Geldbetrag>> auftraege = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                auftraege.add(konto.kaufauftrag(wkn, 1, SOFORT_KAUFEN));
            }
            Geldbetrag summe = Geldbetrag.NULL_EURO;
            for (Future<Geldbetrag> f : auftraege) {
                summe = summe.plus(ergebnis(f));
            }
            assertEquals(20, konto.getDepotbestand(wkn));
            assertEquals(new Geldbetrag(10_000).minus(summe), konto.getKontostand());
        }

        @Test
        void gleichzeitigeKaeufe_kaufenNieMehrAlsDasGuthaben() throws Exception {
            // Guthaben reicht für etwa 2 von 5 Käufen à 40 Stück (je ca. 4.000 €)
            List<Future<Geldbetrag>> auftraege = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                auftraege.add(konto.kaufauftrag(wkn, 40, SOFORT_KAUFEN));
            }
            int erfolgreich = 0;
            Geldbetrag summe = Geldbetrag.NULL_EURO;
            for (Future<Geldbetrag> f : auftraege) {
                Geldbetrag preis = ergebnis(f);
                if (!preis.equals(Geldbetrag.NULL_EURO)) {
                    erfolgreich++;
                    summe = summe.plus(preis);
                }
            }
            assertEquals(40 * erfolgreich, konto.getDepotbestand(wkn));
            assertEquals(new Geldbetrag(10_000).minus(summe), konto.getKontostand());
            assertFalse(konto.getKontostand().isNegativ(), "Das Aktienkonto darf nie ins Minus geraten");
        }
    }

    @Test
    void serialisierung_bewahrtDepotUndKontostand() throws Exception {
        ergebnis(konto.kaufauftrag(wkn, 3, SOFORT_KAUFEN));

        ByteArrayOutputStream puffer = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(puffer)) {
            out.writeObject(konto);
        }
        Aktienkonto kopie;
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(puffer.toByteArray()))) {
            kopie = (Aktienkonto) in.readObject();
        }

        assertEquals(3, kopie.getDepotbestand(wkn));
        assertEquals(konto.getKontostand(), kopie.getKontostand());
        // Das eingelesene Konto kann weiter handeln
        assertNotEquals(Geldbetrag.NULL_EURO, ergebnis(kopie.verkaufauftrag(wkn, SOFORT_VERKAUFEN)));
        assertEquals(0, kopie.getDepotbestand(wkn));
    }
}
