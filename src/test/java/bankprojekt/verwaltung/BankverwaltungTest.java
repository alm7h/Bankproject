package bankprojekt.verwaltung;

import bankprojekt.aktienhandel.Aktienkonto;
import bankprojekt.basisdaten.Festgeldkonto;
import bankprojekt.basisdaten.Geldbetrag;
import bankprojekt.basisdaten.Girokonto;
import bankprojekt.basisdaten.Kinderkonto;
import bankprojekt.basisdaten.Konto;
import bankprojekt.basisdaten.Sparbuch;
import bankprojekt.basisdaten.Waehrung;
import bankprojekt.exceptions.GesperrtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testklasse für die Verwaltung von Kunden, Konten und Benutzern ({@link Bankverwaltung}).
 */
class BankverwaltungTest {

    /** schneller Passwort-Hash für die Testbenutzer (Passwort "geheim") */
    private static final String HASH = Passwortschutz.hashen("geheim".toCharArray(), 1_000);
    private static final long BLZ = 12345678L;
    private static final Clock UHR = Clock.fixed(Instant.parse("2026-10-14T10:15:30Z"), ZoneId.of("Europe/Berlin"));
    private static final LocalDateTime JETZT = LocalDateTime.now(UHR);

    private Bankverwaltung verwaltung;
    private Benutzer admin;

    @BeforeEach
    void setUp() {
        verwaltung = new Bankverwaltung(new Bank(BLZ), UHR);
        admin = verwaltung.erstenBenutzerEinrichten("admin", "Admin", HASH);
    }

    /**
     * Erzeugt gültige Kundendaten.
     * @param vorname Vorname
     * @param nachname Nachname
     * @return die Kundendaten
     */
    private static Kundendaten daten(String vorname, String nachname) {
        return new Kundendaten(vorname, nachname, LocalDate.of(1990, 5, 1), "Berlin", "030 123456",
                vorname.toLowerCase() + "@example.org");
    }

    /**
     * Liefert den jüngsten Protokolleintrag.
     * @return der letzte Eintrag
     */
    private Protokolleintrag letzterEintrag() {
        List<Protokolleintrag> protokoll = verwaltung.getProtokoll();
        return protokoll.get(protokoll.size() - 1);
    }

    @Nested
    class AkteurTests {
        @Test
        void aenderung_ohneAngemeldetenBenutzer_wirftIllegalStateException() {
            assertThrows(IllegalStateException.class, () -> verwaltung.kundeAnlegen(null, daten("Anna", "Schmidt")));
            assertTrue(verwaltung.getKunden().isEmpty());
        }

        @Test
        void aenderung_mitBenutzerEinerAnderenVerwaltung_wirftIllegalStateException() {
            Bankverwaltung andere = new Bankverwaltung(new Bank(BLZ), UHR);
            Benutzer fremd = andere.erstenBenutzerEinrichten("admin", "Admin", HASH);
            assertThrows(IllegalStateException.class, () -> verwaltung.kundeAnlegen(fremd, daten("Anna", "Schmidt")));
        }

        @Test
        void aenderung_mitNichtRegistriertemBenutzer_wirftIllegalStateException() {
            // aktiver Benutzer, der nie in der Verwaltung angelegt wurde
            Benutzer nichtRegistriert = new Benutzer("bert", "Bert", HASH, JETZT);
            assertThrows(IllegalStateException.class,
                    () -> verwaltung.kundeAnlegen(nichtRegistriert, daten("Anna", "Schmidt")));
        }

        @Test
        void aenderung_mitDeaktiviertemBenutzer_wirftIllegalStateException() {
            Benutzer bert = verwaltung.benutzerAnlegen(admin, "bert", "Bert", "geheim".toCharArray());
            verwaltung.benutzerDeaktivieren(admin, "bert");
            assertThrows(IllegalStateException.class, () -> verwaltung.kundeAnlegen(bert, daten("Anna", "Schmidt")));
        }

        @Test
        void konstruktor_mitNull_wirftNullPointerException() {
            assertThrows(NullPointerException.class, () -> new Bankverwaltung(null));
            assertThrows(NullPointerException.class, () -> new Bankverwaltung(new Bank(BLZ), null));
        }

        @Test
        void getBankleitzahl_liefertDieDerBank() {
            assertEquals(BLZ, verwaltung.getBankleitzahl());
        }
    }

    @Nested
    class KundenTests {
        @Test
        void kundeAnlegen_vergibtFortlaufendeKundennummern_undIstAktiv() {
            Kundenakte anna = verwaltung.kundeAnlegen(admin, daten("Anna", "Schmidt"));
            Kundenakte bert = verwaltung.kundeAnlegen(admin, daten("Bert", "Müller"));

            assertEquals("K000001", anna.getKundennummer());
            assertEquals("K000002", bert.getKundennummer());
            assertEquals(Kundenstatus.AKTIV, anna.getStatus());
            assertTrue(anna.isAktiv());
            assertEquals(LocalDate.now(UHR), anna.getAngelegtAm());
            assertEquals(List.of(anna, bert), verwaltung.getKunden());
        }

        @Test
        void kundeAnlegen_uebernimmtAlleStammdaten() {
            Kundenakte anna = verwaltung.kundeAnlegen(admin, daten("Anna", "Schmidt"));
            assertEquals("Anna", anna.getVorname());
            assertEquals("Schmidt", anna.getNachname());
            assertEquals("Schmidt, Anna", anna.getName());
            assertEquals(LocalDate.of(1990, 5, 1), anna.getGeburtsdatum());
            assertEquals("Berlin", anna.getAdresse());
            assertEquals("030 123456", anna.getTelefon());
            assertEquals("anna@example.org", anna.getEmail());
            assertEquals(daten("Anna", "Schmidt"), anna.getKundendaten());
            assertEquals("K000001 – Schmidt, Anna", anna.toString());
        }

        @Test
        void kundeAnlegen_wirdProtokolliert() {
            verwaltung.kundeAnlegen(admin, daten("Anna", "Schmidt"));
            Protokolleintrag eintrag = letzterEintrag();
            assertEquals(Protokollaktion.KUNDE_ANGELEGT, eintrag.aktion());
            assertEquals("admin", eintrag.benutzer());
            assertEquals(JETZT, eintrag.zeitpunkt());
            assertTrue(eintrag.details().contains("K000001"));
        }

        @Test
        void kundeAnlegen_ohneDaten_wirftNullPointerException() {
            assertThrows(NullPointerException.class, () -> verwaltung.kundeAnlegen(admin, null));
        }

        @Test
        void kundeBearbeiten_aendertDaten_undKontenSehenDenNeuenNamen() {
            Kundenakte anna = verwaltung.kundeAnlegen(admin, daten("Anna", "Schmidt"));
            Kontoakte konto = verwaltung.kontoEroeffnen(admin, "K000001", Kontoart.SPARBUCH, Waehrung.EURO, null);

            Kundendaten neu = new Kundendaten("Anna", "Meier", LocalDate.of(1990, 5, 1), "Hamburg", "",
                    "anna.meier@example.org");
            verwaltung.kundeBearbeiten(admin, "K000001", neu);

            assertEquals("Meier, Anna", anna.getName());
            assertEquals("Hamburg", anna.getAdresse());
            assertEquals("", anna.getTelefon());
            assertEquals("anna.meier@example.org", anna.getEmail());
            assertEquals("K000001", anna.getKundennummer(), "Die Kundennummer bleibt");
            assertEquals("Meier, Anna", konto.getInhaberName(), "Der Kunde ist zugleich Kontoinhaber");
        }

        @Test
        void kundeBearbeiten_protokolliertDieGeaendertenFelder() {
            verwaltung.kundeAnlegen(admin, daten("Anna", "Schmidt"));
            verwaltung.kundeBearbeiten(admin, "K000001", new Kundendaten("Anna", "Schmidt",
                    LocalDate.of(1990, 5, 1), "Hamburg", "030 123456", "neu@example.org"));
            Protokolleintrag eintrag = letzterEintrag();
            assertEquals(Protokollaktion.KUNDE_BEARBEITET, eintrag.aktion());
            assertTrue(eintrag.details().endsWith("Adresse, E-Mail"), eintrag.details());
        }

        @Test
        void kundeBearbeiten_ohneAenderung_protokolliertKeineAenderungen() {
            verwaltung.kundeAnlegen(admin, daten("Anna", "Schmidt"));
            verwaltung.kundeBearbeiten(admin, "K000001", daten("Anna", "Schmidt"));
            assertTrue(letzterEintrag().details().endsWith("keine Änderungen"));
        }

        @Test
        void kundeBearbeiten_alleFelderGeaendert_werdenAlleGenannt() {
            verwaltung.kundeAnlegen(admin, daten("Anna", "Schmidt"));
            verwaltung.kundeBearbeiten(admin, "K000001", new Kundendaten("Anne", "Schmid",
                    LocalDate.of(1991, 5, 1), "Hamburg", "", "x@example.org"));
            assertTrue(letzterEintrag().details()
                    .endsWith("Vorname, Nachname, Geburtsdatum, Adresse, Telefon, E-Mail"));
        }

        @Test
        void kundeBearbeiten_unbekannteKundennummer_wirftIllegalArgumentException() {
            assertThrows(IllegalArgumentException.class,
                    () -> verwaltung.kundeBearbeiten(admin, "K999999", daten("Anna", "Schmidt")));
        }

        @Test
        void kundeDeaktivieren_sperrtDenKunden_ohneIhnZuLoeschen() {
            Kundenakte anna = verwaltung.kundeAnlegen(admin, daten("Anna", "Schmidt"));
            verwaltung.kundeDeaktivieren(admin, "K000001");

            assertEquals(Kundenstatus.GESPERRT, anna.getStatus());
            assertFalse(anna.isAktiv());
            assertEquals(List.of(anna), verwaltung.getKunden(), "kein hartes Löschen");
            assertEquals(Protokollaktion.KUNDE_DEAKTIVIERT, letzterEintrag().aktion());
        }

        @Test
        void kundeDeaktivieren_zweimal_wirftIllegalStateException() {
            verwaltung.kundeAnlegen(admin, daten("Anna", "Schmidt"));
            verwaltung.kundeDeaktivieren(admin, "K000001");
            assertThrows(IllegalStateException.class, () -> verwaltung.kundeDeaktivieren(admin, "K000001"));
        }

        @Test
        void kundeAktivieren_machtDenKundenWiederAktiv() {
            Kundenakte anna = verwaltung.kundeAnlegen(admin, daten("Anna", "Schmidt"));
            verwaltung.kundeDeaktivieren(admin, "K000001");
            verwaltung.kundeAktivieren(admin, "K000001");
            assertTrue(anna.isAktiv());
            assertEquals(Protokollaktion.KUNDE_AKTIVIERT, letzterEintrag().aktion());
        }

        @Test
        void kundeAktivieren_beiAktivemKunden_wirftIllegalStateException() {
            verwaltung.kundeAnlegen(admin, daten("Anna", "Schmidt"));
            assertThrows(IllegalStateException.class, () -> verwaltung.kundeAktivieren(admin, "K000001"));
        }

        @Test
        void findeKunde_liefertLeer_beiUnbekannterNummer() {
            assertTrue(verwaltung.findeKunde("K000001").isEmpty());
            Kundenakte anna = verwaltung.kundeAnlegen(admin, daten("Anna", "Schmidt"));
            assertSame(anna, verwaltung.findeKunde("K000001").orElseThrow());
        }

        @Test
        void getKunden_istNichtVeraenderbar() {
            verwaltung.kundeAnlegen(admin, daten("Anna", "Schmidt"));
            List<Kundenakte> kunden = verwaltung.getKunden();
            assertThrows(UnsupportedOperationException.class, () -> kunden.remove(0));
        }
    }

    @Nested
    class KundensucheTests {
        private Kundenakte anna;
        private Kundenakte bert;

        @BeforeEach
        void kundenAnlegen() {
            anna = verwaltung.kundeAnlegen(admin, daten("Anna", "Schmidt"));
            bert = verwaltung.kundeAnlegen(admin, daten("Bert", "Müller"));
            verwaltung.kundeDeaktivieren(admin, bert.getKundennummer());
        }

        @Test
        void ohneSuchtextUndFilter_liefertAlle() {
            assertEquals(List.of(anna, bert), verwaltung.kundenSuchen(null, null));
            assertEquals(List.of(anna, bert), verwaltung.kundenSuchen("  ", null));
        }

        @Test
        void nachNamensteil_ohneRuecksichtAufGrossKleinschreibung() {
            assertEquals(List.of(anna), verwaltung.kundenSuchen("schmi", null));
            assertEquals(List.of(bert), verwaltung.kundenSuchen("BERT", null));
            assertEquals(List.of(anna), verwaltung.kundenSuchen("anna schmidt", null));
            assertEquals(List.of(bert), verwaltung.kundenSuchen("Müller, Bert", null));
        }

        @Test
        void nachKundennummer() {
            assertEquals(List.of(bert), verwaltung.kundenSuchen("k000002", null));
            assertEquals(List.of(anna, bert), verwaltung.kundenSuchen("K0000", null));
        }

        @Test
        void nachStatus() {
            assertEquals(List.of(anna), verwaltung.kundenSuchen(null, Kundenstatus.AKTIV));
            assertEquals(List.of(bert), verwaltung.kundenSuchen("", Kundenstatus.GESPERRT));
            assertEquals(List.of(), verwaltung.kundenSuchen("anna", Kundenstatus.GESPERRT));
        }
    }

    @Nested
    class KontoeroeffnungTests {
        private Kundenakte anna;

        @BeforeEach
        void kundeAnlegen() {
            anna = verwaltung.kundeAnlegen(admin, daten("Anna", "Schmidt"));
        }

        @Test
        void girokonto_mitIbanKontostandNullUndGewaehlterWaehrung() {
            Kontoakte konto = verwaltung.kontoEroeffnen(admin, "K000001", Kontoart.GIROKONTO, Waehrung.DENAR,
                    new Geldbetrag(1000, Waehrung.DENAR));

            assertEquals(1L, konto.getKontonummer());
            assertEquals("K000001", konto.getKundennummer());
            assertEquals(Kontoart.GIROKONTO, konto.getKontoart());
            assertEquals(Waehrung.DENAR, konto.getWaehrung());
            assertEquals(new Geldbetrag(0, Waehrung.DENAR), konto.getKontostand());
            assertEquals(Kontostatus.AKTIV, konto.getStatus());
            assertEquals(LocalDate.now(UHR), konto.getEroeffnetAm());
            assertNull(konto.getAufgeloestAm());
            assertEquals("Schmidt, Anna", konto.getInhaberName());
            assertInstanceOf(Girokonto.class, konto.getKonto());
            assertSame(konto.getKonto(), verwaltung.getBank().getKonten().get(1L),
                    "Die Akte verweist auf das Konto der Bank");
            assertSame(anna.getKunde(), konto.getKonto().getInhaber());
        }

        @Test
        void iban_istEineGueltigeDeutscheIban() {
            Kontoakte konto = verwaltung.kontoEroeffnen(admin, "K000001", Kontoart.SPARBUCH, Waehrung.EURO, null);
            String iban = konto.getIban();
            assertTrue(iban.matches("DE\\d{20}"), iban);
            assertTrue(iban.endsWith("12345678" + "0000000001"), "BLZ + zehnstellige Kontonummer");
            // Prüfsumme nach ISO 13616: Ländercode + Prüfziffern ans Ende, D=13, E=14, Rest mod 97 = 1
            BigInteger zahl = new BigInteger(iban.substring(4) + "1314" + iban.substring(2, 4));
            assertEquals(1, zahl.mod(BigInteger.valueOf(97)).intValue());
            assertEquals(iban.substring(0, 4) + " " + iban.substring(4, 8) + " " + iban.substring(8, 12) + " "
                    + iban.substring(12, 16) + " " + iban.substring(16, 20) + " " + iban.substring(20),
                    konto.getIbanFormatiert());
            assertEquals(konto.getIbanFormatiert() + " (Sparbuch)", konto.toString());
        }

        @Test
        void einKundeKannMehrereKontenHaben_mitEindeutigenNummern() {
            Kontoakte a = verwaltung.kontoEroeffnen(admin, "K000001", Kontoart.SPARBUCH, Waehrung.EURO, null);
            Kontoakte b = verwaltung.kontoEroeffnen(admin, "K000001", Kontoart.FESTGELDKONTO, Waehrung.EURO, null);
            Kontoakte c = verwaltung.kontoEroeffnen(admin, "K000001", Kontoart.GIROKONTO, Waehrung.EURO,
                    new Geldbetrag(500));

            assertEquals(List.of(a, b, c), verwaltung.getKontenVon("K000001"));
            assertEquals(3, List.of(a.getKontonummer(), b.getKontonummer(), c.getKontonummer())
                    .stream().distinct().count());
            assertEquals(3, List.of(a.getIban(), b.getIban(), c.getIban()).stream().distinct().count());
        }

        @ParameterizedTest
        @EnumSource(Kontoart.class)
        void jedeKontoart_erzeugtDenPassendenKontotyp(Kontoart art) {
            Kontoakte konto = verwaltung.kontoEroeffnen(admin, "K000001", art, Waehrung.EURO, new Geldbetrag(100));
            Map<Kontoart, Class<? extends Konto>> erwartet = Map.of(
                    Kontoart.GIROKONTO, Girokonto.class, Kontoart.SPARBUCH, Sparbuch.class,
                    Kontoart.FESTGELDKONTO, Festgeldkonto.class, Kontoart.KINDERKONTO, Kinderkonto.class,
                    Kontoart.AKTIENKONTO, Aktienkonto.class);
            assertEquals(erwartet.get(art), konto.getKonto().getClass());
            assertEquals(art == Kontoart.GIROKONTO, art.brauchtDispo());
        }

        @Test
        void eroeffnung_wirdProtokolliert() {
            verwaltung.kontoEroeffnen(admin, "K000001", Kontoart.SPARBUCH, Waehrung.EURO, null);
            Protokolleintrag eintrag = letzterEintrag();
            assertEquals(Protokollaktion.KONTO_EROEFFNET, eintrag.aktion());
            assertTrue(eintrag.details().contains("K000001"));
            assertTrue(eintrag.details().contains("DE"));
        }

        @Test
        void girokonto_ohneOderMitNullDispo_wirft_undVerbrauchtKeineKontonummer() {
            assertThrows(IllegalArgumentException.class,
                    () -> verwaltung.kontoEroeffnen(admin, "K000001", Kontoart.GIROKONTO, Waehrung.EURO, null));
            assertThrows(IllegalArgumentException.class, () -> verwaltung.kontoEroeffnen(admin, "K000001",
                    Kontoart.GIROKONTO, Waehrung.EURO, new Geldbetrag(0)));
            assertThrows(IllegalArgumentException.class, () -> verwaltung.kontoEroeffnen(admin, "K000001",
                    Kontoart.GIROKONTO, Waehrung.EURO, new Geldbetrag(-5)));
            assertTrue(verwaltung.getKonten().isEmpty());
            assertTrue(verwaltung.getBank().getKonten().isEmpty());
            Kontoakte konto = verwaltung.kontoEroeffnen(admin, "K000001", Kontoart.SPARBUCH, Waehrung.EURO, null);
            assertEquals(1L, konto.getKontonummer());
        }

        @Test
        void ohneKontoartOderWaehrung_wirftIllegalArgumentException() {
            assertThrows(IllegalArgumentException.class,
                    () -> verwaltung.kontoEroeffnen(admin, "K000001", null, Waehrung.EURO, null));
            assertThrows(IllegalArgumentException.class,
                    () -> verwaltung.kontoEroeffnen(admin, "K000001", Kontoart.SPARBUCH, null, null));
        }

        @Test
        void fuerUnbekanntenKunden_wirftIllegalArgumentException() {
            assertThrows(IllegalArgumentException.class,
                    () -> verwaltung.kontoEroeffnen(admin, "K000099", Kontoart.SPARBUCH, Waehrung.EURO, null));
        }

        @Test
        void fuerGesperrtenKunden_wirftIllegalStateException() {
            verwaltung.kundeDeaktivieren(admin, "K000001");
            assertThrows(IllegalStateException.class,
                    () -> verwaltung.kontoEroeffnen(admin, "K000001", Kontoart.SPARBUCH, Waehrung.EURO, null));
            assertTrue(verwaltung.getKonten().isEmpty());
        }
    }

    @Nested
    class KontostatusTests {
        private Kontoakte konto;

        @BeforeEach
        void kontoEroeffnen() {
            verwaltung.kundeAnlegen(admin, daten("Anna", "Schmidt"));
            konto = verwaltung.kontoEroeffnen(admin, "K000001", Kontoart.GIROKONTO, Waehrung.EURO,
                    new Geldbetrag(500));
        }

        @Test
        void sperren_undEntsperren() {
            verwaltung.kontoSperren(admin, 1L);
            assertEquals(Kontostatus.GESPERRT, konto.getStatus());
            assertTrue(konto.gesperrtProperty().get());
            assertEquals(Protokollaktion.KONTO_GESPERRT, letzterEintrag().aktion());

            verwaltung.kontoEntsperren(admin, 1L);
            assertEquals(Kontostatus.AKTIV, konto.getStatus());
            assertFalse(konto.gesperrtProperty().get());
            assertEquals(Protokollaktion.KONTO_ENTSPERRT, letzterEintrag().aktion());
        }

        @Test
        void gesperrtesKonto_erlaubtKeineBuchungen() {
            verwaltung.kontoSperren(admin, 1L);
            assertThrows(GesperrtException.class, () -> konto.getKonto().einzahlen(new Geldbetrag(10)));
        }

        @Test
        void sperren_zweimal_undEntsperren_einesAktivenKontos_wirft() {
            assertThrows(IllegalStateException.class, () -> verwaltung.kontoEntsperren(admin, 1L));
            verwaltung.kontoSperren(admin, 1L);
            assertThrows(IllegalStateException.class, () -> verwaltung.kontoSperren(admin, 1L));
        }

        @Test
        void unbekanntesKonto_wirftIllegalArgumentException() {
            assertThrows(IllegalArgumentException.class, () -> verwaltung.kontoSperren(admin, 99L));
            assertTrue(verwaltung.findeKonto(99L).isEmpty());
            assertSame(konto, verwaltung.findeKonto(1L).orElseThrow());
        }

        @Test
        void aufloesen_beiKontostandNull_sperrtDauerhaft_undBehaeltDasKonto() {
            verwaltung.kontoAufloesen(admin, 1L);

            assertEquals(Kontostatus.AUFGELOEST, konto.getStatus());
            assertEquals(LocalDate.now(UHR), konto.getAufgeloestAm());
            assertTrue(konto.getKonto().isGesperrt());
            assertEquals(List.of(konto), verwaltung.getKonten(), "Das Konto bleibt erhalten");
            assertTrue(verwaltung.getBank().getKonten().containsKey(1L));
            assertEquals(Protokollaktion.KONTO_AUFGELOEST, letzterEintrag().aktion());
        }

        @Test
        void aufloesen_einesGesperrtenKontos_istMoeglich() {
            verwaltung.kontoSperren(admin, 1L);
            verwaltung.kontoAufloesen(admin, 1L);
            assertEquals(Kontostatus.AUFGELOEST, konto.getStatus());
        }

        @Test
        void aufgeloestesKonto_kannWederEntsperrtNochGesperrtNochErneutAufgeloestWerden() {
            verwaltung.kontoAufloesen(admin, 1L);
            assertThrows(IllegalStateException.class, () -> verwaltung.kontoEntsperren(admin, 1L));
            assertThrows(IllegalStateException.class, () -> verwaltung.kontoSperren(admin, 1L));
            assertThrows(IllegalStateException.class, () -> verwaltung.kontoAufloesen(admin, 1L));
        }

        @Test
        void aufloesen_mitGuthaben_wirftIllegalStateException() throws GesperrtException {
            verwaltung.getBank().geldEinzahlen(1L, new Geldbetrag(0.01));
            IllegalStateException e = assertThrows(IllegalStateException.class,
                    () -> verwaltung.kontoAufloesen(admin, 1L));
            assertTrue(e.getMessage().contains("0"));
            assertEquals(Kontostatus.AKTIV, konto.getStatus());
        }

        @Test
        void aufloesen_imMinus_wirftIllegalStateException() throws GesperrtException {
            assertTrue(verwaltung.getBank().geldAbheben(1L, new Geldbetrag(10)));
            assertThrows(IllegalStateException.class, () -> verwaltung.kontoAufloesen(admin, 1L));
        }

        @Test
        void kontostandProperty_zeigtBuchungenSofort() throws GesperrtException {
            verwaltung.getBank().geldEinzahlen(1L, new Geldbetrag(25));
            assertEquals(new Geldbetrag(25), konto.kontostandProperty().get());
            assertEquals(new Geldbetrag(25), konto.getKontostand());
        }
    }

    @Nested
    class KontosucheTests {
        private Kontoakte annaGiro;
        private Kontoakte annaSpar;
        private Kontoakte bertSpar;

        @BeforeEach
        void kontenEroeffnen() {
            verwaltung.kundeAnlegen(admin, daten("Anna", "Schmidt"));
            verwaltung.kundeAnlegen(admin, daten("Bert", "Müller"));
            annaGiro = verwaltung.kontoEroeffnen(admin, "K000001", Kontoart.GIROKONTO, Waehrung.EURO,
                    new Geldbetrag(500));
            annaSpar = verwaltung.kontoEroeffnen(admin, "K000001", Kontoart.SPARBUCH, Waehrung.EURO, null);
            bertSpar = verwaltung.kontoEroeffnen(admin, "K000002", Kontoart.SPARBUCH, Waehrung.EURO, null);
            verwaltung.kontoSperren(admin, annaSpar.getKontonummer());
        }

        @Test
        void ohneSuchtextUndFilter_liefertAlle() {
            assertEquals(List.of(annaGiro, annaSpar, bertSpar), verwaltung.kontenSuchen(null, null, null));
            assertEquals(List.of(annaGiro, annaSpar, bertSpar), verwaltung.getKonten());
        }

        @Test
        void nachIban_mitOderOhneLeerzeichen() {
            assertEquals(List.of(bertSpar), verwaltung.kontenSuchen(bertSpar.getIbanFormatiert(), null, null));
            assertEquals(List.of(bertSpar), verwaltung.kontenSuchen(bertSpar.getIban().toLowerCase(), null, null));
        }

        @Test
        void nachKontonummer_mussGenauPassen() {
            // "2" kommt in der BLZ jeder IBAN vor, gemeint ist aber die Kontonummer 2
            assertEquals(List.of(annaSpar), verwaltung.kontenSuchen("2", null, null));
            assertEquals(List.of(annaSpar), verwaltung.kontenSuchen(" 0000000002 ", null, null));
            assertEquals(List.of(), verwaltung.kontenSuchen("99999999999999999999999", null, null));
        }

        @Test
        void nachTeilDerIban() {
            assertEquals(List.of(annaGiro, annaSpar, bertSpar), verwaltung.kontenSuchen("de", null, null));
            assertTrue(verwaltung.kontenSuchen(bertSpar.getIbanFormatiert().substring(0, 9), null, null)
                    .contains(bertSpar), "Anfang der IBAN mit Leerzeichen");
            // ohne Leerzeichen nur Ziffern -> wird als Kontonummer 3 gesucht
            assertEquals(List.of(bertSpar), verwaltung.kontenSuchen("0000 0000 03", null, null));
        }

        @Test
        void nachInhaberOderKundennummer() {
            assertEquals(List.of(bertSpar), verwaltung.kontenSuchen("müller", null, null));
            assertEquals(List.of(annaGiro, annaSpar), verwaltung.kontenSuchen("k000001", null, null));
        }

        @Test
        void nachKundeUndStatus() {
            assertEquals(List.of(annaGiro, annaSpar), verwaltung.kontenSuchen(null, "K000001", null));
            assertEquals(List.of(annaSpar), verwaltung.kontenSuchen(null, null, Kontostatus.GESPERRT));
            assertEquals(List.of(annaGiro), verwaltung.kontenSuchen(null, "K000001", Kontostatus.AKTIV));
            assertEquals(List.of(), verwaltung.kontenSuchen(null, "K000002", Kontostatus.AUFGELOEST));
        }
    }

    @Nested
    class BenutzerTests {
        @Test
        void ersterBenutzer_nurEinmalEinrichtbar() {
            assertEquals(Protokollaktion.ERSTEINRICHTUNG, letzterEintrag().aktion());
            assertEquals(Bankverwaltung.SYSTEM, letzterEintrag().benutzer());
            assertThrows(IllegalStateException.class,
                    () -> verwaltung.erstenBenutzerEinrichten("zweiter", "Zweiter", HASH));
        }

        @Test
        void benutzerAnlegen_speichertNurDenHash_undDenNamenKlein() {
            Benutzer bert = verwaltung.benutzerAnlegen(admin, "  Bert.Neu ", "Bert Neu", "passwort".toCharArray());

            assertEquals("bert.neu", bert.getBenutzername());
            assertEquals("Bert Neu", bert.getAnzeigename());
            assertTrue(bert.isAktiv());
            assertEquals(JETZT, bert.getAngelegtAm());
            assertNull(bert.getLetzteAnmeldung());
            assertFalse(bert.getPasswortHash().contains("passwort"));
            assertTrue(bert.passwortPruefen("passwort".toCharArray()));
            assertSame(bert, verwaltung.findeBenutzer("BERT.NEU").orElseThrow());
            assertEquals(Protokollaktion.BENUTZER_ANGELEGT, letzterEintrag().aktion());
            assertEquals("Bert Neu (bert.neu)", bert.toString());
        }

        @Test
        void benutzerAnlegen_mitVergebenemNamen_wirftIllegalArgumentException() {
            assertThrows(IllegalArgumentException.class,
                    () -> verwaltung.benutzerAnlegen(admin, "ADMIN", "Noch ein Admin", "geheim".toCharArray()));
            assertEquals(1, verwaltung.getBenutzer().size());
        }

        @Test
        void benutzerAnlegen_mitUngueltigenAngaben_wirftIllegalArgumentException() {
            assertThrows(IllegalArgumentException.class,
                    () -> verwaltung.benutzerAnlegen(admin, "ab", "Zu kurz", "geheim".toCharArray()));
            assertThrows(IllegalArgumentException.class,
                    () -> verwaltung.benutzerAnlegen(admin, "mit leerzeichen", "X", "geheim".toCharArray()));
            assertThrows(IllegalArgumentException.class,
                    () -> verwaltung.benutzerAnlegen(admin, "bert", " ", "geheim".toCharArray()));
            assertThrows(IllegalArgumentException.class,
                    () -> verwaltung.benutzerAnlegen(admin, "bert", "Bert", "123".toCharArray()));
            assertThrows(IllegalArgumentException.class,
                    () -> verwaltung.benutzerAnlegen(admin, "bert", "Bert", null));
            assertEquals(1, verwaltung.getBenutzer().size());
        }

        @Test
        void benutzerBearbeiten_aendertDenAnzeigenamen() {
            verwaltung.benutzerBearbeiten(admin, "admin", "  Administratorin ");
            assertEquals("Administratorin", admin.getAnzeigename());
            assertEquals(Protokollaktion.BENUTZER_BEARBEITET, letzterEintrag().aktion());
            assertThrows(IllegalArgumentException.class, () -> verwaltung.benutzerBearbeiten(admin, "admin", ""));
            assertThrows(IllegalArgumentException.class, () -> verwaltung.benutzerBearbeiten(admin, "gibtsnicht", "X"));
        }

        @Test
        void passwortZuruecksetzen_neuesPasswortGilt_altesNichtMehr() {
            verwaltung.passwortZuruecksetzen(admin, "admin", "neuesPasswort".toCharArray());
            assertTrue(admin.passwortPruefen("neuesPasswort".toCharArray()));
            assertFalse(admin.passwortPruefen("geheim".toCharArray()));
            assertEquals(Protokollaktion.PASSWORT_ZURUECKGESETZT, letzterEintrag().aktion());
        }

        @Test
        void passwortZuruecksetzen_mitZuKurzemPasswort_wirft_undAltesBleibt() {
            assertThrows(IllegalArgumentException.class,
                    () -> verwaltung.passwortZuruecksetzen(admin, "admin", "abc".toCharArray()));
            assertTrue(admin.passwortPruefen("geheim".toCharArray()));
        }

        @Test
        void sichSelbstDeaktivieren_wirftIllegalStateException() {
            assertThrows(IllegalStateException.class, () -> verwaltung.benutzerDeaktivieren(admin, "admin"));
            assertTrue(admin.isAktiv());
        }

        @Test
        void deaktivieren_undAktivieren() {
            Benutzer bert = verwaltung.benutzerAnlegen(admin, "bert", "Bert", "geheim".toCharArray());
            verwaltung.benutzerDeaktivieren(admin, "bert");
            assertFalse(bert.isAktiv());
            assertEquals(Protokollaktion.BENUTZER_DEAKTIVIERT, letzterEintrag().aktion());
            assertThrows(IllegalStateException.class, () -> verwaltung.benutzerDeaktivieren(admin, "bert"));

            verwaltung.benutzerAktivieren(admin, "bert");
            assertTrue(bert.isAktiv());
            assertEquals(Protokollaktion.BENUTZER_AKTIVIERT, letzterEintrag().aktion());
            assertThrows(IllegalStateException.class, () -> verwaltung.benutzerAktivieren(admin, "bert"));
        }

        @Test
        void getBenutzer_istNachNamenSortiert() {
            Benutzer zora = verwaltung.benutzerAnlegen(admin, "zora", "Zora", "geheim".toCharArray());
            Benutzer bert = verwaltung.benutzerAnlegen(admin, "bert", "Bert", "geheim".toCharArray());
            assertEquals(List.of(admin, bert, zora), verwaltung.getBenutzer());
        }

        @Test
        void findeBenutzer_mitNull_liefertLeer() {
            assertTrue(verwaltung.findeBenutzer(null).isEmpty());
        }
    }

    @Nested
    class ProtokollUndSpeicherTests {
        @Test
        void getProtokoll_istNichtVeraenderbar() {
            List<Protokolleintrag> protokoll = verwaltung.getProtokoll();
            assertThrows(UnsupportedOperationException.class, protokoll::clear);
        }

        @Test
        void beiAenderung_wirdNachJederAenderungAufgerufen() {
            int[] aufrufe = {0};
            verwaltung.setBeiAenderung(() -> aufrufe[0]++);
            verwaltung.kundeAnlegen(admin, daten("Anna", "Schmidt"));
            verwaltung.kontoEroeffnen(admin, "K000001", Kontoart.SPARBUCH, Waehrung.EURO, null);
            verwaltung.kontoSperren(admin, 1L);
            assertEquals(3, aufrufe[0]);
        }

        @Test
        void fehlgeschlageneAenderung_wirdNichtProtokolliert() {
            int anzahl = verwaltung.getProtokoll().size();
            assertThrows(IllegalArgumentException.class, () -> verwaltung.kontoSperren(admin, 1L));
            assertEquals(anzahl, verwaltung.getProtokoll().size());
        }

        @Test
        void serialisierung_erhaeltAlleDaten_undDieVerbindungZurBank() throws Exception {
            verwaltung.kundeAnlegen(admin, daten("Anna", "Schmidt"));
            verwaltung.kontoEroeffnen(admin, "K000001", Kontoart.AKTIENKONTO, Waehrung.EURO, null);
            verwaltung.kontoEroeffnen(admin, "K000001", Kontoart.GIROKONTO, Waehrung.DOBRA,
                    new Geldbetrag(100, Waehrung.DOBRA));
            verwaltung.kontoSperren(admin, 2L);

            Bankverwaltung kopie = rundreise(verwaltung);

            assertEquals(1, kopie.getKunden().size());
            assertEquals(2, kopie.getKonten().size());
            assertEquals(verwaltung.getProtokoll(), kopie.getProtokoll());
            Kontoakte giro = kopie.findeKonto(2L).orElseThrow();
            assertEquals(Kontostatus.GESPERRT, giro.getStatus());
            assertEquals(Waehrung.DOBRA, giro.getWaehrung());
            assertSame(giro.getKonto(), kopie.getBank().getKonten().get(2L), "gleiches Konto-Objekt wie in der Bank");
            assertSame(kopie.findeKunde("K000001").orElseThrow().getKunde(), giro.getKonto().getInhaber());

            // nach dem Einlesen ist die Verwaltung weiter benutzbar (Uhr wiederhergestellt)
            Benutzer kopierterAdmin = kopie.findeBenutzer("admin").orElseThrow();
            Kundenakte bert = kopie.kundeAnlegen(kopierterAdmin, daten("Bert", "Müller"));
            assertEquals("K000002", bert.getKundennummer());
            assertTrue(kopierterAdmin.passwortPruefen("geheim".toCharArray()));
        }

        /**
         * Serialisiert eine Verwaltung und liest sie wieder ein.
         * @param original die Verwaltung
         * @return die eingelesene Kopie
         * @throws IOException bei Schreib-/Lesefehlern
         * @throws ClassNotFoundException bei unbekannten Klassen
         */
        private Bankverwaltung rundreise(Bankverwaltung original) throws IOException, ClassNotFoundException {
            ByteArrayOutputStream puffer = new ByteArrayOutputStream();
            try (ObjectOutputStream aus = new ObjectOutputStream(puffer)) {
                aus.writeObject(original);
            }
            try (ObjectInputStream ein = new ObjectInputStream(new ByteArrayInputStream(puffer.toByteArray()))) {
                return (Bankverwaltung) ein.readObject();
            }
        }
    }
}
