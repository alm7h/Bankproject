package bankprojekt.basisdaten;

import bankprojekt.exceptions.GesperrtException;
import bankprojekt.nuetzliches.Kalender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * @author m.alharethi
 */
@ExtendWith(MockitoExtension.class)
class SparbuchMockitoTest {

    private Sparbuch sparbuch;

    @Mock
    private Kalender kalenderMock;

    @BeforeEach
    void setUp() {
        Kunde inhaber = new Kunde("Max", "Mustermann", "Weg 1", 2000, 1, 1);
        // Startdatum: 1. Januar 2026
        when(kalenderMock.getHeutigesDatum()).thenReturn(LocalDate.of(2026, 1, 1));
        sparbuch = new Sparbuch(inhaber, 12345678, kalenderMock);
        sparbuch.einzahlen(new Geldbetrag(5000, Waehrung.EURO));
    }

    /**
     * Prüft, dass das Abhebe-Limit beim Monatswechsel korrekt zurückgesetzt wird
     * und danach wieder Abhebungen möglich sind.
     */
    @Test
    void testAbhebelimitResetBeiMonatswechsel() throws GesperrtException {
        // Im Januar: 1500€ abheben (erfolgreich)
        boolean erfolgJanuar = sparbuch.abheben(new Geldbetrag(1500, Waehrung.EURO));
        assertTrue(erfolgJanuar);

        // Im Januar: Weitere 1000€ abheben (schlägt fehl, da Limit 2000€)
        boolean erfolgJanuarZwei = sparbuch.abheben(new Geldbetrag(1000, Waehrung.EURO));
        assertFalse(erfolgJanuarZwei, "Abhebung im gleichen Monat über 2000€ darf nicht möglich sein");

        // Datum auf Februar ändern
        when(kalenderMock.getHeutigesDatum()).thenReturn(LocalDate.of(2026, 2, 1));

        // Im Februar: 1500€ abheben (muss erfolgreich sein, da Limit zurückgesetzt)
        boolean erfolgFebruar = sparbuch.abheben(new Geldbetrag(1500, Waehrung.EURO));
        assertTrue(erfolgFebruar, "Abhebung im neuen Monat muss wieder möglich sein");
    }

    /**
     * Prüft, dass das Limit nach einem Monatswechsel nur EINMAL zurückgesetzt wird
     * und nicht bei jeder weiteren Abhebung im neuen Monat erneut.
     *
     * Gefundener Bug: In der ursprünglichen Implementierung wurde
     * 'zeitpunkt' in der abheben()-Methode nie auf das aktuelle Datum aktualisiert.
     * Dadurch wurde 'bereitsAbgehoben' bei JEDER Abhebung in einem anderen Monat
     * als dem Initialmonat erneut auf null zurückgesetzt – das monatliche Limit
     * ließ sich damit beliebig oft umgehen.
     * Dieser Test scheiterte vor der Bugkorrektur in Sparbuch.abheben():
     * Die zweite Abhebung von 1500€ im Februar wurde fälschlicherweise akzeptiert,
     * da bereitsAbgehoben durch einen erneuten Reset auf 0 fiel.
     */
    @Test
    void testAbhebelimitResetNurEinmalProMonat() throws GesperrtException {
        // Datum auf Februar wechseln (neuer Monat gegenüber dem Initialmonat Januar)
        when(kalenderMock.getHeutigesDatum()).thenReturn(LocalDate.of(2026, 2, 1));

        // Erste Abhebung in Februar: 1500€ – Limit-Reset findet statt, bereitsAbgehoben = 1500
        boolean ersteAbhebung = sparbuch.abheben(new Geldbetrag(1500, Waehrung.EURO));
        assertTrue(ersteAbhebung, "Erste Abhebung nach Monatswechsel muss möglich sein");

        // Zweite Abhebung in Februar: weitere 1500€ → Gesamtbetrag wäre 3000€ > 2000€ Limit
        // FEHLER: Ohne korrektes zeitpunkt-Update würde bereitsAbgehoben erneut auf 0 gesetzt
        //      und diese Abhebung fälschlicherweise genehmigt.
        boolean zweiteAbhebung = sparbuch.abheben(new Geldbetrag(1500, Waehrung.EURO));
        assertFalse(zweiteAbhebung,
                "Zweite Abhebung im selben Monat, die das Limit überschreitet, muss abgelehnt werden");
    }

    @Test
    void abheben_erlaubtGenau2000_undLehntDanachEinenCentAb() throws GesperrtException {
        assertTrue(sparbuch.abheben(new Geldbetrag(2000)));
        assertFalse(sparbuch.abheben(new Geldbetrag(0.01)), "Monatslimit ist ausgeschöpft");
        assertEquals(new Geldbetrag(3000), sparbuch.getKontostand());
    }

    @Test
    void abheben_summiertMehrereAbhebungenImMonat() throws GesperrtException {
        assertTrue(sparbuch.abheben(new Geldbetrag(1500)));
        assertTrue(sparbuch.abheben(new Geldbetrag(500)));
        assertFalse(sparbuch.abheben(new Geldbetrag(0.01)));
    }

    @Test
    void abgelehnteAbhebung_zaehltNichtZurMonatssumme() throws GesperrtException {
        assertFalse(sparbuch.abheben(new Geldbetrag(2000.01)));
        // Die abgelehnten 2000,01 € dürfen das Limit nicht verbrauchen
        assertTrue(sparbuch.abheben(new Geldbetrag(2000)));
    }

    @Test
    void monatslimit_rechnetFremdwaehrungInEuroUm() throws GesperrtException {
        // 123000 MKD entsprechen genau 2000 €
        assertTrue(sparbuch.abheben(new Geldbetrag(123000, Waehrung.DENAR)));
        assertFalse(sparbuch.abheben(new Geldbetrag(0.01)));
    }

    @Test
    void monatslimit_giltBisZumLetztenTagDesMonats() throws GesperrtException {
        assertTrue(sparbuch.abheben(new Geldbetrag(2000)));
        when(kalenderMock.getHeutigesDatum()).thenReturn(LocalDate.of(2026, 1, 31));
        assertFalse(sparbuch.abheben(new Geldbetrag(0.01)), "Noch derselbe Monat");
    }

    @Test
    void monatslimit_wirdBeimJahreswechselImGleichenMonatZurueckgesetzt() throws GesperrtException {
        // Januar 2026 -> Januar 2027: gleicher Monat, aber anderes Jahr
        assertTrue(sparbuch.abheben(new Geldbetrag(2000)));
        when(kalenderMock.getHeutigesDatum()).thenReturn(LocalDate.of(2027, 1, 1));
        assertTrue(sparbuch.abheben(new Geldbetrag(2000)), "Neues Jahr, neues Limit");
    }

    @Test
    void monatslimit_wirdNachAbgelehnterAbhebungImNeuenMonatNurEinmalZurueckgesetzt() throws GesperrtException {
        assertTrue(sparbuch.abheben(new Geldbetrag(2000)));
        when(kalenderMock.getHeutigesDatum()).thenReturn(LocalDate.of(2026, 2, 1));
        // Erste Abhebung im Februar scheitert am Limit, setzt aber den Monat um
        assertFalse(sparbuch.abheben(new Geldbetrag(2000.01)));
        assertTrue(sparbuch.abheben(new Geldbetrag(2000)));
        assertFalse(sparbuch.abheben(new Geldbetrag(0.01)));
    }

    /**
     * Ein Kalender mit festem Datum. Ein Mockito-Mock ist nicht serialisierbar,
     * daher wird für den Serialisierungstest diese kleine Unterklasse verwendet.
     */
    private static class FesterKalender extends Kalender {
        private static final long serialVersionUID = 1L;
        private final LocalDate datum;

        FesterKalender(LocalDate datum) {
            this.datum = datum;
        }

        @Override
        public LocalDate getHeutigesDatum() {
            return datum;
        }
    }

    @Test
    void serialisierung_bewahrtMonatssumme() throws Exception {
        Sparbuch original = new Sparbuch(new Kunde("Max", "Mustermann", "Weg 1", 2000, 1, 1),
                4711, new FesterKalender(LocalDate.of(2026, 1, 1)));
        original.einzahlen(new Geldbetrag(5000));
        assertTrue(original.abheben(new Geldbetrag(1500)));

        ByteArrayOutputStream puffer = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(puffer)) {
            out.writeObject(original);
        }
        Sparbuch gelesen;
        try (ObjectInputStream in = new ObjectInputStream(
                new ByteArrayInputStream(puffer.toByteArray()))) {
            gelesen = (Sparbuch) in.readObject();
        }

        assertEquals(new Geldbetrag(3500), gelesen.getKontostand());
        assertFalse(gelesen.abheben(new Geldbetrag(500.01)), "Bereits abgehobene 1500 € bleiben erhalten");
        assertTrue(gelesen.abheben(new Geldbetrag(500)));
    }
}