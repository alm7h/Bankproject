package bankprojekt.basisdaten;

import bankprojekt.exceptions.GesperrtException;
import bankprojekt.nuetzliches.Kalender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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
     * Gefundener Bug (Übung 00): In der ursprünglichen Implementierung wurde
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
        // BUG: Ohne korrektes zeitpunkt-Update würde bereitsAbgehoben erneut auf 0 gesetzt
        //      und diese Abhebung fälschlicherweise genehmigt.
        boolean zweiteAbhebung = sparbuch.abheben(new Geldbetrag(1500, Waehrung.EURO));
        assertFalse(zweiteAbhebung,
                "Zweite Abhebung im selben Monat, die das Limit überschreitet, muss abgelehnt werden");
    }
}