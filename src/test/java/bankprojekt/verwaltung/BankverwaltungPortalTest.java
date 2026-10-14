package bankprojekt.verwaltung;

import bankprojekt.basisdaten.Geldbetrag;
import bankprojekt.basisdaten.Waehrung;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testklasse für die Mitarbeiterseite des Kundenportals in der {@link Bankverwaltung}:
 * Kontoanträge entscheiden, Portalzugänge einrichten, Bareinzahlungen und Kontoauszüge.
 */
class BankverwaltungPortalTest {

    private VerstellbareUhr uhr;
    private Bankverwaltung verwaltung;
    private Benutzer admin;

    @BeforeEach
    void setUp() {
        uhr = new VerstellbareUhr();
        verwaltung = Testdaten.verwaltung(uhr);
        admin = Testdaten.admin(verwaltung);
    }

    private Protokolleintrag letzterEintrag() {
        List<Protokolleintrag> protokoll = verwaltung.getProtokoll();
        return protokoll.get(protokoll.size() - 1);
    }

    @Nested
    class AntragTests {
        @Test
        void neukundenantrag_genehmigen_legtKundeUndKontoAn_undOrdnetDenZugangZu() {
            Kontoantrag antrag = Testdaten.neukunde(verwaltung, "anna.s", Testdaten.kunde("Anna", "Schmidt"),
                    Kontoart.GIROKONTO);
            assertEquals("A000001", antrag.getAntragsnummer());
            assertEquals(Antragsstatus.OFFEN, antrag.getStatus());
            assertEquals(1, verwaltung.getAnzahlOffeneAntraege());

            uhr.vorstellen(java.time.Duration.ofHours(2));
            Kontoakte konto = verwaltung.antragGenehmigen(admin, "A000001", new Geldbetrag(300));

            Kundenakte anna = verwaltung.findeKunde("K000001").orElseThrow();
            assertEquals("Schmidt, Anna", anna.getName());
            assertEquals("K000001", konto.getKundennummer());
            assertEquals(Kontoart.GIROKONTO, konto.getKontoart());
            assertEquals("K000001", verwaltung.findeKundenzugang("anna.s").orElseThrow().getKundennummer());
            assertEquals(Antragsstatus.GENEHMIGT, antrag.getStatus());
            assertEquals("admin", antrag.getBearbeitetVon());
            assertEquals(LocalDateTime.of(2026, 10, 14, 12, 0), antrag.getBearbeitetAm());
            assertEquals("K000001", antrag.getKundennummer());
            assertEquals(konto.getKontonummer(), antrag.getKontonummer());
            assertNull(antrag.getBegruendung());
            assertEquals(0, verwaltung.getAnzahlOffeneAntraege());
            assertEquals(Protokollaktion.ANTRAG_GENEHMIGT, letzterEintrag().aktion());
        }

        @Test
        void zusatzkontoantrag_genehmigen_eroeffnetNurDasKonto() {
            Kundenakte anna = verwaltung.kundeAnlegen(admin, Testdaten.kunde("Anna", "Schmidt"));
            Kontoantrag antrag = verwaltung.antragStellen(Antragsart.ZUSATZKONTO, "anna.s", anna.getName(), null,
                    anna.getKundennummer(), Kontoart.SPARBUCH, Waehrung.DENAR);
            Kontoakte konto = verwaltung.antragGenehmigen(admin, antrag.getAntragsnummer(), null);
            assertEquals(1, verwaltung.getKunden().size(), "kein neuer Kunde");
            assertEquals(Waehrung.DENAR, konto.getWaehrung());
            assertEquals(List.of(konto), verwaltung.getKontenVon(anna.getKundennummer()));
        }

        @Test
        void genehmigen_girokontoOhneDispo_wirft_undLegtKeinenKundenAn() {
            Testdaten.neukunde(verwaltung, "anna.s", Testdaten.kunde("Anna", "Schmidt"), Kontoart.GIROKONTO);
            assertThrows(IllegalArgumentException.class, () -> verwaltung.antragGenehmigen(admin, "A000001", null));
            assertTrue(verwaltung.getKunden().isEmpty());
            assertEquals(Antragsstatus.OFFEN, verwaltung.findeAntrag("A000001").orElseThrow().getStatus());
        }

        @Test
        void ablehnen_mitBegruendung() {
            Kontoantrag antrag = Testdaten.neukunde(verwaltung, "anna.s", Testdaten.kunde("Anna", "Schmidt"),
                    Kontoart.SPARBUCH);
            assertThrows(IllegalArgumentException.class, () -> verwaltung.antragAblehnen(admin, "A000001", " "));
            verwaltung.antragAblehnen(admin, "A000001", " Unterlagen unvollständig ");
            assertEquals(Antragsstatus.ABGELEHNT, antrag.getStatus());
            assertEquals("Unterlagen unvollständig", antrag.getBegruendung());
            assertNull(antrag.getKontonummer());
            assertTrue(verwaltung.getKunden().isEmpty());
            assertTrue(letzterEintrag().details().endsWith("abgelehnt: Unterlagen unvollständig"));
        }

        @Test
        void entschiedeneOderUnbekannteAntraege_koennenNichtEntschiedenWerden() {
            Testdaten.neukunde(verwaltung, "anna.s", Testdaten.kunde("Anna", "Schmidt"), Kontoart.SPARBUCH);
            verwaltung.antragAblehnen(admin, "A000001", "nein");
            assertThrows(IllegalStateException.class, () -> verwaltung.antragGenehmigen(admin, "A000001", null));
            assertThrows(IllegalStateException.class, () -> verwaltung.antragAblehnen(admin, "A000001", "x"));
            assertThrows(IllegalArgumentException.class, () -> verwaltung.antragAblehnen(admin, "A999999", "x"));
            assertThrows(IllegalStateException.class, () -> verwaltung.antragAblehnen(null, "A000001", "x"));
        }

        @Test
        void getAntraege_nachStatus_undSortiert() {
            Kontoantrag a1 = Testdaten.neukunde(verwaltung, "anna.s", Testdaten.kunde("Anna", "Schmidt"),
                    Kontoart.SPARBUCH);
            Kontoantrag a2 = Testdaten.neukunde(verwaltung, "bert.m", Testdaten.kunde("Bert", "Müller"),
                    Kontoart.SPARBUCH);
            verwaltung.antragAblehnen(admin, a1.getAntragsnummer(), "nein");
            assertEquals(List.of(a1, a2), verwaltung.getAntraege(null));
            assertEquals(List.of(a2), verwaltung.getAntraege(Antragsstatus.OFFEN));
            assertEquals(List.of(a1), verwaltung.getAntraege(Antragsstatus.ABGELEHNT));
            assertEquals(List.of(a2), verwaltung.antraegeVon("bert.m"));
            assertTrue(verwaltung.findeAntrag("A000003").isEmpty());
        }

        @Test
        void antrag_liefertSeineDaten() {
            Kundendaten daten = Testdaten.kunde("Anna", "Schmidt");
            Kontoantrag antrag = Testdaten.neukunde(verwaltung, "anna.s", daten, Kontoart.SPARBUCH);
            assertEquals(Antragsart.NEUKUNDE, antrag.getArt());
            assertEquals("anna.s", antrag.getBenutzername());
            assertEquals("Schmidt, Anna", antrag.getAntragsteller());
            assertEquals(daten, antrag.getKundendaten());
            assertNull(antrag.getKundennummer());
            assertEquals(Kontoart.SPARBUCH, antrag.getKontoart());
            assertEquals(Waehrung.EURO, antrag.getWaehrung());
            assertEquals(LocalDateTime.of(2026, 10, 14, 10, 0), antrag.getEingereichtAm());
            assertNull(antrag.getBearbeitetVon());
            assertNull(antrag.getBearbeitetAm());
            assertEquals("A000001 – Sparbuch", antrag.toString());
            assertEquals("Neukunde", Antragsart.NEUKUNDE.toString());
            assertEquals("offen", Antragsstatus.OFFEN.toString());
        }
    }

    @Nested
    class PortalzugangTests {
        private Kundenakte anna;

        @BeforeEach
        void kunde() {
            anna = verwaltung.kundeAnlegen(admin, Testdaten.kunde("Anna", "Schmidt"));
        }

        @Test
        void einrichten_mitStartpasswort_dasGeaendertWerdenMuss() {
            Kundenzugang zugang = verwaltung.portalzugangEinrichten(admin, "K000001", " Anna.S ", "start1".toCharArray());
            assertEquals("anna.s", zugang.getBenutzername());
            assertEquals("K000001", zugang.getKundennummer());
            assertTrue(zugang.istKunde());
            assertTrue(zugang.mussPasswortAendern());
            assertTrue(zugang.passwortPruefen("start1".toCharArray()));
            assertFalse(zugang.getPasswortHash().contains("start1"));
            assertEquals(LocalDateTime.of(2026, 10, 14, 10, 0), zugang.getAngelegtAm());
            assertNull(zugang.getLetzteAnmeldung());
            assertEquals("anna.s", zugang.toString());
            assertSame(zugang, verwaltung.getKundenzugangVon("K000001").orElseThrow());
            assertEquals(Protokollaktion.PORTALZUGANG_EINGERICHTET, letzterEintrag().aktion());
        }

        @Test
        void einrichten_zweimal_oderMitVergebenemNamen_wirft() {
            verwaltung.portalzugangEinrichten(admin, "K000001", "anna.s", "start1".toCharArray());
            assertThrows(IllegalStateException.class,
                    () -> verwaltung.portalzugangEinrichten(admin, "K000001", "anna.t", "start1".toCharArray()));
            verwaltung.kundeAnlegen(admin, Testdaten.kunde("Bert", "Müller"));
            assertThrows(IllegalArgumentException.class,
                    () -> verwaltung.portalzugangEinrichten(admin, "K000002", "ADMIN", "start1".toCharArray()),
                    "Mitarbeiter-Namen sind auch vergeben");
            assertThrows(IllegalArgumentException.class,
                    () -> verwaltung.portalzugangEinrichten(admin, "K000002", "anna.s", "start1".toCharArray()));
            assertThrows(IllegalArgumentException.class,
                    () -> verwaltung.portalzugangEinrichten(admin, "K000002", "bert", "123".toCharArray()));
        }

        @Test
        void mitarbeiterName_derSchonKundeIst_istVergeben() {
            verwaltung.portalzugangEinrichten(admin, "K000001", "anna.s", "start1".toCharArray());
            assertThrows(IllegalArgumentException.class,
                    () -> verwaltung.benutzerAnlegen(admin, "anna.s", "Anna", "geheim".toCharArray()));
        }

        @Test
        void passwortZuruecksetzen_erzwingtEineAenderung() {
            Kundenzugang zugang = Testdaten.kundenzugang(verwaltung, "anna.s", "K000001");
            assertFalse(zugang.mussPasswortAendern());
            verwaltung.portalpasswortZuruecksetzen(admin, "K000001", "neu-123".toCharArray());
            assertTrue(zugang.passwortPruefen("neu-123".toCharArray()));
            assertTrue(zugang.mussPasswortAendern());
            assertEquals(Protokollaktion.PORTALPASSWORT_ZURUECKGESETZT, letzterEintrag().aktion());
        }

        @Test
        void passwortZuruecksetzen_ohneZugang_wirft() {
            assertThrows(IllegalStateException.class,
                    () -> verwaltung.portalpasswortZuruecksetzen(admin, "K000001", "neu-123".toCharArray()));
        }

        @Test
        void findeKundenzugang_ohneRuecksichtAufGrossschreibung() {
            Kundenzugang zugang = Testdaten.kundenzugang(verwaltung, "anna.s", "K000001");
            assertSame(zugang, verwaltung.findeKundenzugang(" ANNA.S ").orElseThrow());
            assertTrue(verwaltung.findeKundenzugang(null).isEmpty());
            assertTrue(verwaltung.getKundenzugangVon("K000099").isEmpty());
        }

        @Test
        void kundenzugangAktiv_folgtDemKundenstatus() {
            Kundenzugang zugang = Testdaten.kundenzugang(verwaltung, "anna.s", anna.getKundennummer());
            Kundenzugang neu = Testdaten.kundenzugang(verwaltung, "neu", null);
            assertTrue(verwaltung.kundenzugangAktiv(zugang));
            assertTrue(verwaltung.kundenzugangAktiv(neu));
            verwaltung.kundeDeaktivieren(admin, anna.getKundennummer());
            assertFalse(verwaltung.kundenzugangAktiv(zugang));
        }
    }

    @Nested
    class BarTests {
        private Kontoakte giro;

        @BeforeEach
        void konto() {
            verwaltung.kundeAnlegen(admin, Testdaten.kunde("Anna", "Schmidt"));
            giro = verwaltung.kontoEroeffnen(admin, "K000001", Kontoart.GIROKONTO, Waehrung.EURO, new Geldbetrag(100));
        }

        @Test
        void bareinzahlung_buchtUndSchreibtDenKontoauszug() {
            verwaltung.bareinzahlung(admin, giro.getKontonummer(), new Geldbetrag(250), null);
            assertEquals(new Geldbetrag(250), giro.getKontostand());
            Buchung buchung = verwaltung.getBuchungen(giro.getKontonummer()).get(0);
            assertEquals(Buchungsart.BAREINZAHLUNG, buchung.art());
            assertEquals(new Geldbetrag(250), buchung.betrag());
            assertEquals(new Geldbetrag(250), buchung.saldoDanach());
            assertEquals("Bareinzahlung am Schalter", buchung.text());
            assertEquals("admin", buchung.ausgefuehrtVon());
            assertEquals(giro.getKontonummer(), buchung.kontonummer());
            assertEquals(LocalDateTime.of(2026, 10, 14, 10, 0), buchung.zeitpunkt());
            assertEquals(Protokollaktion.BAREINZAHLUNG, letzterEintrag().aktion());
        }

        @Test
        void barauszahlung_imRahmenDesDispos_mitNegativemBetragImAuszug() {
            verwaltung.barauszahlung(admin, giro.getKontonummer(), new Geldbetrag(80), " Geldautomat ");
            Buchung buchung = verwaltung.getBuchungen(giro.getKontonummer()).get(0);
            assertEquals(new Geldbetrag(-80), buchung.betrag());
            assertEquals(new Geldbetrag(-80), buchung.saldoDanach());
            assertEquals("Geldautomat", buchung.text());
            assertTrue(Buchungsart.BARAUSZAHLUNG.istAbgang());
            assertEquals(Protokollaktion.BARAUSZAHLUNG, letzterEintrag().aktion());
        }

        @Test
        void barauszahlung_ueberDenDispo_wirdAbgelehnt() {
            IllegalStateException e = assertThrows(IllegalStateException.class,
                    () -> verwaltung.barauszahlung(admin, giro.getKontonummer(), new Geldbetrag(101), null));
            assertTrue(e.getMessage().contains("Girokonto"), e.getMessage());
            assertTrue(verwaltung.getBuchungen(giro.getKontonummer()).isEmpty());
        }

        @Test
        void gesperrtesKonto_erlaubtKeineBarbuchung() {
            verwaltung.kontoSperren(admin, giro.getKontonummer());
            IllegalStateException e = assertThrows(IllegalStateException.class,
                    () -> verwaltung.bareinzahlung(admin, giro.getKontonummer(), new Geldbetrag(1), null));
            assertTrue(e.getMessage().endsWith("ist gesperrt – keine Buchung möglich."), e.getMessage());
            assertThrows(IllegalStateException.class,
                    () -> verwaltung.barauszahlung(admin, giro.getKontonummer(), new Geldbetrag(1), null));
        }

        @Test
        void ungueltigerBetrag_wirdAbgelehnt() {
            assertThrows(IllegalArgumentException.class,
                    () -> verwaltung.bareinzahlung(admin, giro.getKontonummer(), null, null));
            assertThrows(IllegalArgumentException.class,
                    () -> verwaltung.bareinzahlung(admin, giro.getKontonummer(), new Geldbetrag(0), null));
            assertThrows(IllegalArgumentException.class,
                    () -> verwaltung.barauszahlung(admin, giro.getKontonummer(), new Geldbetrag(-1), null));
        }

        @Test
        void fremdwaehrung_wirdImAuszugInDieKontowaehrungUmgerechnet() {
            Kontoakte denar = verwaltung.kontoEroeffnen(admin, "K000001", Kontoart.SPARBUCH, Waehrung.DENAR, null);
            verwaltung.bareinzahlung(admin, denar.getKontonummer(), new Geldbetrag(10), null);
            assertEquals(new Geldbetrag(615, Waehrung.DENAR), verwaltung.getBuchungen(denar.getKontonummer()).get(0).betrag());
        }

        @Test
        void kontoauszug_istNichtVeraenderbar_undOhneBuchungenLeer() {
            assertTrue(verwaltung.getBuchungen(giro.getKontonummer()).isEmpty());
            verwaltung.bareinzahlung(admin, giro.getKontonummer(), new Geldbetrag(1), null);
            List<Buchung> auszug = verwaltung.getBuchungen(giro.getKontonummer());
            assertThrows(UnsupportedOperationException.class, auszug::clear);
        }

        @Test
        void findeKontoNachIban() {
            assertSame(giro, verwaltung.findeKontoNachIban(giro.getIbanFormatiert().toLowerCase()).orElseThrow());
            assertTrue(verwaltung.findeKontoNachIban("DE00").isEmpty());
            assertTrue(verwaltung.findeKontoNachIban(null).isEmpty());
        }
    }

    @Test
    void dateiVonVorDemKundenportal_wirdErgaenzt() throws Exception {
        // Felder des Kundenportals leeren, als stamme die Datei aus der Version davor
        for (String feld : List.of("kundenzugaenge", "antraege", "buchungen")) {
            Field f = Bankverwaltung.class.getDeclaredField(feld);
            f.setAccessible(true);
            f.set(verwaltung, null);
        }
        Field nummer = Bankverwaltung.class.getDeclaredField("naechsteAntragsnummer");
        nummer.setAccessible(true);
        nummer.setLong(verwaltung, 0);

        ByteArrayOutputStream puffer = new ByteArrayOutputStream();
        try (ObjectOutputStream aus = new ObjectOutputStream(puffer)) {
            aus.writeObject(verwaltung);
        }
        Bankverwaltung geladen;
        try (ObjectInputStream ein = new ObjectInputStream(new ByteArrayInputStream(puffer.toByteArray()))) {
            geladen = (Bankverwaltung) ein.readObject();
        }

        assertTrue(geladen.getAntraege(null).isEmpty());
        assertTrue(geladen.getBuchungen(1L).isEmpty());
        assertTrue(geladen.findeKundenzugang("x").isEmpty());
        Kontoantrag antrag = Testdaten.neukunde(geladen, "anna.s", Testdaten.kunde("Anna", "Schmidt"), Kontoart.SPARBUCH);
        assertEquals("A000001", antrag.getAntragsnummer());
    }

    @Test
    void neueDaten_ueberlebenDieSerialisierung() throws Exception {
        Testdaten.neukunde(verwaltung, "anna.s", Testdaten.kunde("Anna", "Schmidt"), Kontoart.SPARBUCH);
        Kontoakte konto = verwaltung.antragGenehmigen(admin, "A000001", null);
        verwaltung.bareinzahlung(admin, konto.getKontonummer(), new Geldbetrag(5), null);

        ByteArrayOutputStream puffer = new ByteArrayOutputStream();
        try (ObjectOutputStream aus = new ObjectOutputStream(puffer)) {
            aus.writeObject(verwaltung);
        }
        Bankverwaltung geladen;
        try (ObjectInputStream ein = new ObjectInputStream(new ByteArrayInputStream(puffer.toByteArray()))) {
            geladen = (Bankverwaltung) ein.readObject();
        }
        assertEquals(Antragsstatus.GENEHMIGT, geladen.findeAntrag("A000001").orElseThrow().getStatus());
        assertEquals("K000001", geladen.findeKundenzugang("anna.s").orElseThrow().getKundennummer());
        assertEquals(1, geladen.getBuchungen(konto.getKontonummer()).size());
    }
}
