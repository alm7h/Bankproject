package bankprojekt.verwaltung;

import bankprojekt.basisdaten.Geldbetrag;
import bankprojekt.basisdaten.Girokonto;
import bankprojekt.basisdaten.Konto;
import bankprojekt.basisdaten.Kunde;
import bankprojekt.exceptions.GesperrtException;
import bankprojekt.fabriken.Kontofabrik;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BankMockitoTest {

    private Bank bank;
    private Kunde dummyKunde1;
    private Kunde dummyKunde2;

    @BeforeEach
    void setUp() {
        bank = new Bank(12345678L);
        dummyKunde1 = new Kunde("Max", "Müller", "Weg 1", 2000, 1, 1);
        dummyKunde2 = new Kunde("Lisa", "Schmidt", "Weg 2", 1995, 5, 5);
    }

    /**
     * Übung 11 b): Ersatz für die gelöschte Methode {@code Bank.mockEinfuegen}.
     * Statt das Mock-Konto direkt einzufügen, wird es über die neue Methode
     * {@link Bank#kontoErstellen(Kontofabrik, Kunde)} eingebracht – mit einer
     * Wegwerf-{@link Kontofabrik}, die genau dieses Mock-Konto liefert. Genau dafür
     * ist das Abstract-Factory-Muster nützlich: Der Test bestimmt über die Fabrik,
     * welches (hier: gemockte) Konto die Bank erhält.
     *
     * @param mock das einzufügende Mock-Konto
     * @return die von der Bank vergebene Kontonummer
     */
    private long mockEinfuegen(Konto mock) {
        return bank.kontoErstellen(new Kontofabrik() {
            @Override
            public Konto erstellen(Kunde inhaber, long kontonummer) {
                return mock;
            }
        }, dummyKunde1);
    }

    // -------------------------------------------------------------------------
    // Tests für getKontostand()
    // -------------------------------------------------------------------------

    /**
     * Prüft, dass der Kontostand korrekt über den Mock durchgereicht wird.
     */
    @Test
    void testGetKontostandMitMock() {
        // Arrange
        Konto mockKonto = mock(Konto.class);
        Geldbetrag erwarteterStand = new Geldbetrag(150);
        when(mockKonto.getKontostand()).thenReturn(erwarteterStand);
        long kontoNr = mockEinfuegen(mockKonto);

        // Act
        Geldbetrag tatsaechlicherStand = bank.getKontostand(kontoNr);

        // Assert
        assertEquals(erwarteterStand, tatsaechlicherStand,
                "Der Kontostand sollte vom Mock durchgereicht werden");
        verify(mockKonto).getKontostand();
    }

    /**
     * Prüft, dass getKontostand() null zurückgibt, wenn die Kontonummer nicht existiert.
     */
    @Test
    void testGetKontostandUnbekannteKontonummer() {
        // Act
        Geldbetrag stand = bank.getKontostand(99999L);

        // Assert
        assertNull(stand, "Bei unbekannter Kontonummer muss null zurückgegeben werden");
    }

    // -------------------------------------------------------------------------
    // Tests für geldUeberweisen()
    // -------------------------------------------------------------------------

    /**
     * Prüft den Erfolgsfall: Sender bucht ab, Empfänger erhält das Geld.
     */
    @Test
    void testGeldUeberweisen_Erfolgreich() throws GesperrtException {
        // Arrange
        Girokonto senderMock = mock(Girokonto.class);
        Girokonto empfaengerMock = mock(Girokonto.class);

        when(senderMock.getInhaber()).thenReturn(dummyKunde1);
        when(empfaengerMock.getInhaber()).thenReturn(dummyKunde2);
        when(senderMock.ueberweisungAbsenden(any(), anyString(), anyLong(), anyLong(), anyString()))
                .thenReturn(true);

        long senderNr = mockEinfuegen(senderMock);
        long empfaengerNr = mockEinfuegen(empfaengerMock);
        Geldbetrag ueberweisungsBetrag = new Geldbetrag(50);

        // Act
        boolean result = bank.geldUeberweisen(senderNr, empfaengerNr, ueberweisungsBetrag, "Miete");

        // Assert
        assertTrue(result, "Die Überweisung sollte erfolgreich sein");
        verify(senderMock).ueberweisungAbsenden(eq(ueberweisungsBetrag), eq("Schmidt, Lisa"),
                eq(empfaengerNr), eq(bank.getBankleitzahl()), eq("Miete"));
        verify(empfaengerMock).ueberweisungEmpfangen(eq(ueberweisungsBetrag), eq("Müller, Max"),
                eq(senderNr), eq(bank.getBankleitzahl()), eq("Miete"));
    }

    /**
     * Prüft, dass bei fehlgeschlagener Abbuchung beim Sender der Empfänger
     * kein Geld erhält (Atomizität der Überweisung).
     */
    @Test
    void testGeldUeberweisen_AbbuchungSchlaegtFehl() throws GesperrtException {
        // Arrange
        Girokonto senderMock = mock(Girokonto.class);
        Girokonto empfaengerMock = mock(Girokonto.class);

        when(senderMock.getInhaber()).thenReturn(dummyKunde1);
        when(empfaengerMock.getInhaber()).thenReturn(dummyKunde2);
        when(senderMock.ueberweisungAbsenden(any(), anyString(), anyLong(), anyLong(), anyString()))
                .thenReturn(false);

        long senderNr = mockEinfuegen(senderMock);
        long empfaengerNr = mockEinfuegen(empfaengerMock);

        // Act
        boolean result = bank.geldUeberweisen(senderNr, empfaengerNr, new Geldbetrag(50), "Miete");

        // Assert
        assertFalse(result, "Die Überweisung sollte abgebrochen werden");
        verify(empfaengerMock, never()).ueberweisungEmpfangen(
                any(), anyString(), anyLong(), anyLong(), anyString());
    }

    /**
     * Prüft, dass eine Überweisung false liefert, wenn eine der Kontonummern
     * nicht in der Bank existiert.
     */
    @Test
    void testGeldUeberweisen_EmpfaengerExistiertNicht() throws GesperrtException {
        // Arrange
        Girokonto senderMock = mock(Girokonto.class);
        long senderNr = mockEinfuegen(senderMock);

        // Act — Empfänger-Kontonummer ist unbekannt
        boolean result = bank.geldUeberweisen(senderNr, 99999L, new Geldbetrag(50), "Test");

        // Assert
        assertFalse(result, "Überweisung an nicht existierende Kontonummer muss false liefern");
    }

    /**
     * Prüft, dass eine Überweisung false liefert, wenn der Sender kein
     * überweisungsfähiges Konto ist (z.B. ein einfaches Konto-Mock).
     */
    @Test
    void testGeldUeberweisen_KeinUeberweisungsfaehigesKonto() throws GesperrtException {
        // Arrange — Konto (abstrakte Klasse) implementiert UeberweisungsfaehigesKonto NICHT
        Konto nichtUeberweisbarMock = mock(Konto.class);
        Girokonto empfaengerMock = mock(Girokonto.class);

        long senderNr = mockEinfuegen(nichtUeberweisbarMock);
        long empfaengerNr = mockEinfuegen(empfaengerMock);

        // Act
        boolean result = bank.geldUeberweisen(senderNr, empfaengerNr, new Geldbetrag(100), "Test");

        // Assert
        assertFalse(result, "Überweisung von nicht-überweisungsfähigem Konto muss false liefern");
        // Empfänger darf kein Geld erhalten haben
        verify(empfaengerMock, never()).ueberweisungEmpfangen(
                any(), anyString(), anyLong(), anyLong(), anyString());
    }

    /**
     * Prüft, dass eine IllegalArgumentException geworfen wird, wenn Sender-
     * und Empfängerkontonummer identisch sind.
     */
    @Test
    void testGeldUeberweisen_GleicheKontonummer() {
        // Kontonummer 1L wurde nie in die Bank eingefügt – der Check erfolgt vor der Kontensuche
        assertThrows(IllegalArgumentException.class,
                () -> bank.geldUeberweisen(1L, 1L, new Geldbetrag(100), "Test"),
                "Identische Kontonummern müssen IllegalArgumentException werfen");
    }

    /**
     * Prüft, dass eine GesperrtException weitergeleitet wird, wenn das Senderkonto
     * gesperrt ist, und dass der Empfänger in diesem Fall kein Geld erhält.
     */
    @Test
    void testGeldUeberweisen_GesperrtException() throws GesperrtException {
        // Arrange
        Girokonto senderMock = mock(Girokonto.class);
        Girokonto empfaengerMock = mock(Girokonto.class);

        when(senderMock.getInhaber()).thenReturn(dummyKunde1);
        when(empfaengerMock.getInhaber()).thenReturn(dummyKunde2);
        // Abbuchung beim Sender wirft GesperrtException (Konto gesperrt)
        when(senderMock.ueberweisungAbsenden(any(), anyString(), anyLong(), anyLong(), anyString()))
                .thenThrow(new GesperrtException(1L));

        long senderNr = mockEinfuegen(senderMock);
        long empfaengerNr = mockEinfuegen(empfaengerMock);

        // Act & Assert
        assertThrows(GesperrtException.class,
                () -> bank.geldUeberweisen(senderNr, empfaengerNr, new Geldbetrag(100), "Test"),
                "GesperrtException des Senders muss weitergeleitet werden");

        // Empfänger darf kein Geld erhalten haben, da die Abbuchung scheiterte
        verify(empfaengerMock, never()).ueberweisungEmpfangen(
                any(), anyString(), anyLong(), anyLong(), anyString());
    }
}