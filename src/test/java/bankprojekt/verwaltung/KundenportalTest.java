package bankprojekt.verwaltung;

import bankprojekt.basisdaten.Geldbetrag;
import bankprojekt.basisdaten.Waehrung;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testklasse für die Kundenseite ({@link Kundenportal}): Registrierung, Anträge,
 * eigene Daten und Konten, Überweisungen und Umbuchungen.
 */
class KundenportalTest {

    private Bankverwaltung verwaltung;
    private Benutzer admin;
    private Kundenportal portal;

    @BeforeEach
    void setUp() {
        verwaltung = Testdaten.verwaltung(new VerstellbareUhr());
        admin = Testdaten.admin(verwaltung);
        portal = new Kundenportal(verwaltung);
    }

    private Protokolleintrag letzterEintrag() {
        List<Protokolleintrag> protokoll = verwaltung.getProtokoll();
        return protokoll.get(protokoll.size() - 1);
    }

    @Test
    void konstruktor_ohneVerwaltung_wirft() {
        assertThrows(NullPointerException.class, () -> new Kundenportal(null));
    }

    @Nested
    class RegistrierungTests {
        @Test
        void neukundeRegistrieren_legtZugangUndOffenenAntragAn() {
            Kontoantrag antrag = portal.neukundeRegistrieren(Testdaten.kunde("Anna", "Schmidt"), Kontoart.GIROKONTO,
                    Waehrung.EURO, " Anna.S ", "geheim1".toCharArray());

            Kundenzugang zugang = verwaltung.findeKundenzugang("anna.s").orElseThrow();
            assertFalse(zugang.istKunde());
            assertNull(zugang.getKundennummer());
            assertFalse(zugang.mussPasswortAendern());
            assertTrue(zugang.passwortPruefen("geheim1".toCharArray()));
            assertEquals(Antragsart.NEUKUNDE, antrag.getArt());
            assertEquals("anna.s", antrag.getBenutzername());
            assertEquals("Schmidt, Anna", antrag.getAntragsteller());
            assertEquals(List.of(antrag), portal.getAntraege(zugang));
            assertTrue(verwaltung.getKunden().isEmpty(), "Kunde entsteht erst bei Genehmigung");
            assertEquals(Protokollaktion.REGISTRIERUNG, letzterEintrag().aktion());
            assertEquals("anna.s", letzterEintrag().benutzer());
        }

        @Test
        void zugangsdatenPruefen_vorDerRegistrierung() {
            assertEquals("anna.s", portal.zugangsdatenPruefen("Anna.S", "geheim".toCharArray()));
            assertThrows(IllegalArgumentException.class, () -> portal.zugangsdatenPruefen("admin", "geheim".toCharArray()));
            assertThrows(IllegalArgumentException.class, () -> portal.zugangsdatenPruefen("a b", "geheim".toCharArray()));
            assertThrows(IllegalArgumentException.class, () -> portal.zugangsdatenPruefen("anna.s", "123".toCharArray()));
        }

        @Test
        void registrierung_mitFehlendenAngaben_legtNichtsAn() {
            assertThrows(NullPointerException.class, () -> portal.neukundeRegistrieren(null, Kontoart.SPARBUCH,
                    Waehrung.EURO, "anna.s", "geheim".toCharArray()));
            assertThrows(IllegalArgumentException.class, () -> portal.neukundeRegistrieren(
                    Testdaten.kunde("Anna", "Schmidt"), null, Waehrung.EURO, "anna.s", "geheim".toCharArray()));
            assertThrows(IllegalArgumentException.class, () -> portal.neukundeRegistrieren(
                    Testdaten.kunde("Anna", "Schmidt"), Kontoart.SPARBUCH, null, "anna.s", "geheim".toCharArray()));
            assertThrows(IllegalArgumentException.class, () -> portal.neukundeRegistrieren(
                    Testdaten.kunde("Anna", "Schmidt"), Kontoart.SPARBUCH, Waehrung.EURO, "admin", "geheim".toCharArray()));
            assertTrue(verwaltung.findeKundenzugang("anna.s").isEmpty());
            assertTrue(verwaltung.getAntraege(null).isEmpty());
        }

        @Test
        void nachAblehnung_kannErneutBeantragtWerden_aberNichtWaehrendDerPruefung() {
            Testdaten.neukunde(verwaltung, "anna.s", Testdaten.kunde("Anna", "Schmidt"), Kontoart.GIROKONTO);
            Kundenzugang zugang = verwaltung.findeKundenzugang("anna.s").orElseThrow();
            assertFalse(portal.kannErneutBeantragen(zugang));
            assertThrows(IllegalStateException.class, () -> portal.erneutBeantragen(zugang,
                    Testdaten.kunde("Anna", "Schmidt"), Kontoart.SPARBUCH, Waehrung.EURO));

            verwaltung.antragAblehnen(admin, "A000001", "Girokonto nur mit Gehaltsnachweis");
            assertTrue(portal.kannErneutBeantragen(zugang));
            Kontoantrag zweiter = portal.erneutBeantragen(zugang, Testdaten.kunde("Anna", "Schmidt"),
                    Kontoart.SPARBUCH, Waehrung.EURO);
            assertEquals("A000002", zweiter.getAntragsnummer());
            assertEquals(Protokollaktion.ANTRAG_GESTELLT, letzterEintrag().aktion());
            assertEquals(2, portal.getAntraege(zugang).size());
        }

        @Test
        void erneutBeantragen_alsKunde_oderOhneAngaben_wirft() {
            Kundenakte anna = verwaltung.kundeAnlegen(admin, Testdaten.kunde("Anna", "Schmidt"));
            Kundenzugang kunde = Testdaten.kundenzugang(verwaltung, "anna.s", anna.getKundennummer());
            assertFalse(portal.kannErneutBeantragen(kunde));
            assertThrows(IllegalStateException.class, () -> portal.erneutBeantragen(kunde,
                    Testdaten.kunde("Anna", "Schmidt"), Kontoart.SPARBUCH, Waehrung.EURO));

            Kundenzugang neu = Testdaten.kundenzugang(verwaltung, "neu", null);
            assertThrows(NullPointerException.class,
                    () -> portal.erneutBeantragen(neu, null, Kontoart.SPARBUCH, Waehrung.EURO));
            assertThrows(IllegalArgumentException.class,
                    () -> portal.erneutBeantragen(neu, Testdaten.kunde("Neu", "Kunde"), null, Waehrung.EURO));
        }
    }

    @Nested
    class KundeTests {
        private Kundenakte anna;
        private Kundenzugang zugang;
        private Kontoakte giro;

        @BeforeEach
        void kunde() {
            anna = verwaltung.kundeAnlegen(admin, Testdaten.kunde("Anna", "Schmidt"));
            zugang = Testdaten.kundenzugang(verwaltung, "anna.s", anna.getKundennummer());
            giro = verwaltung.kontoEroeffnen(admin, anna.getKundennummer(), Kontoart.GIROKONTO, Waehrung.EURO,
                    new Geldbetrag(100));
        }

        @Test
        void eigeneDatenUndKonten() {
            assertSame(anna, portal.getKunde(zugang).orElseThrow());
            assertEquals(List.of(giro), portal.getKonten(zugang));
            Kundenzugang neu = Testdaten.kundenzugang(verwaltung, "neu", null);
            assertTrue(portal.getKunde(neu).isEmpty());
            assertTrue(portal.getKonten(neu).isEmpty());
        }

        @Test
        void zusatzkontoBeantragen() {
            Kontoantrag antrag = portal.zusatzkontoBeantragen(zugang, Kontoart.SPARBUCH, Waehrung.DENAR);
            assertEquals(Antragsart.ZUSATZKONTO, antrag.getArt());
            assertEquals(anna.getKundennummer(), antrag.getKundennummer());
            assertNull(antrag.getKundendaten());
            assertEquals("Schmidt, Anna", antrag.getAntragsteller());
            assertEquals(Protokollaktion.ANTRAG_GESTELLT, letzterEintrag().aktion());
            assertThrows(IllegalArgumentException.class,
                    () -> portal.zusatzkontoBeantragen(zugang, Kontoart.SPARBUCH, null));
        }

        @Test
        void neukundeOhneGenehmigung_kannNochKeineKontenNutzen() {
            Kundenzugang neu = Testdaten.kundenzugang(verwaltung, "neu", null);
            IllegalStateException e = assertThrows(IllegalStateException.class,
                    () -> portal.zusatzkontoBeantragen(neu, Kontoart.SPARBUCH, Waehrung.EURO));
            assertTrue(e.getMessage().contains("noch nicht genehmigt"));
        }

        @Test
        void fremdeOderUngueltigeZugaenge_werdenAbgewiesen() {
            assertThrows(IllegalStateException.class, () -> portal.getKonten(null));
            Bankverwaltung andere = Testdaten.verwaltung(new VerstellbareUhr());
            Kundenzugang fremd = Testdaten.kundenzugang(andere, "anna.s", null);
            assertThrows(IllegalStateException.class, () -> portal.getKonten(fremd));
            verwaltung.kundeDeaktivieren(admin, anna.getKundennummer());
            assertThrows(IllegalStateException.class, () -> portal.getKonten(zugang), "gesperrter Kunde");
        }

        @Test
        void kontaktdatenAendern_nurAdresseTelefonEmail() {
            portal.kontaktdatenAendern(zugang, "Hamburg", "040 123456", "anna@neu.de");
            assertEquals("Hamburg", anna.getAdresse());
            assertEquals("040 123456", anna.getTelefon());
            assertEquals("anna@neu.de", anna.getEmail());
            assertEquals("Schmidt, Anna", anna.getName());
            assertEquals(Protokollaktion.KONTAKTDATEN_GEAENDERT, letzterEintrag().aktion());
            assertTrue(letzterEintrag().details().endsWith("Adresse, Telefon, E-Mail"));
            assertThrows(IllegalArgumentException.class, () -> portal.kontaktdatenAendern(zugang, "", "", "x@y.de"));
            assertThrows(IllegalArgumentException.class,
                    () -> portal.kontaktdatenAendern(zugang, "Hamburg", "", "kaputt"));
        }

        @Test
        void passwortAendern() {
            assertThrows(IllegalArgumentException.class,
                    () -> portal.passwortAendern(zugang, "falsch".toCharArray(), "neu-1234".toCharArray()));
            assertThrows(IllegalArgumentException.class,
                    () -> portal.passwortAendern(zugang, Testdaten.PASSWORT.toCharArray(), "123".toCharArray()));
            assertThrows(IllegalArgumentException.class, () -> portal.passwortAendern(zugang,
                    Testdaten.PASSWORT.toCharArray(), Testdaten.PASSWORT.toCharArray()));

            verwaltung.portalpasswortZuruecksetzen(admin, anna.getKundennummer(), "start1".toCharArray());
            assertTrue(zugang.mussPasswortAendern());
            portal.passwortAendern(zugang, "start1".toCharArray(), "neu-1234".toCharArray());
            assertTrue(zugang.passwortPruefen("neu-1234".toCharArray()));
            assertFalse(zugang.mussPasswortAendern());
            assertEquals(Protokollaktion.PASSWORT_GEAENDERT, letzterEintrag().aktion());
        }

        @Test
        void kontoSperren_nurEigeneAktiveKonten() {
            portal.kontoSperren(zugang, giro.getKontonummer());
            assertEquals(Kontostatus.GESPERRT, giro.getStatus());
            assertTrue(letzterEintrag().details().contains("vom Kunden"));
            assertThrows(IllegalStateException.class, () -> portal.kontoSperren(zugang, giro.getKontonummer()));

            Kundenakte bert = verwaltung.kundeAnlegen(admin, Testdaten.kunde("Bert", "Müller"));
            Kontoakte fremd = verwaltung.kontoEroeffnen(admin, bert.getKundennummer(), Kontoart.SPARBUCH,
                    Waehrung.EURO, null);
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                    () -> portal.kontoSperren(zugang, fremd.getKontonummer()));
            assertEquals("Dieses Konto gehört nicht zu Ihrem Zugang.", e.getMessage());
            assertThrows(IllegalArgumentException.class, () -> portal.kontoSperren(zugang, 999L));
        }

        @Test
        void kontoauszug_nurFuerEigeneKonten() {
            verwaltung.bareinzahlung(admin, giro.getKontonummer(), new Geldbetrag(20), null);
            assertEquals(1, portal.getKontoauszug(zugang, giro.getKontonummer()).size());
            assertThrows(IllegalArgumentException.class, () -> portal.getKontoauszug(zugang, 999L));
        }
    }

    @Nested
    class ZahlungsverkehrTests {
        private Kundenzugang anna;
        private Kontoakte giro;
        private Kontoakte spar;
        private Kontoakte bertGiro;

        @BeforeEach
        void konten() {
            Kundenakte a = verwaltung.kundeAnlegen(admin, Testdaten.kunde("Anna", "Schmidt"));
            Kundenakte b = verwaltung.kundeAnlegen(admin, Testdaten.kunde("Bert", "Müller"));
            anna = Testdaten.kundenzugang(verwaltung, "anna.s", a.getKundennummer());
            giro = verwaltung.kontoEroeffnen(admin, a.getKundennummer(), Kontoart.GIROKONTO, Waehrung.EURO,
                    new Geldbetrag(100));
            spar = verwaltung.kontoEroeffnen(admin, a.getKundennummer(), Kontoart.SPARBUCH, Waehrung.EURO, null);
            bertGiro = verwaltung.kontoEroeffnen(admin, b.getKundennummer(), Kontoart.GIROKONTO, Waehrung.EURO,
                    new Geldbetrag(100));
            verwaltung.bareinzahlung(admin, giro.getKontonummer(), new Geldbetrag(200), null);
        }

        @Test
        void ueberweisen_anEinFremdesGirokonto_mitAuszugBeiBeiden() {
            portal.ueberweisen(anna, giro.getKontonummer(), bertGiro.getIbanFormatiert().toLowerCase(),
                    new Geldbetrag(50), " Miete Oktober ");

            assertEquals(new Geldbetrag(150), giro.getKontostand());
            assertEquals(new Geldbetrag(50), bertGiro.getKontostand());
            Buchung aus = verwaltung.getBuchungen(giro.getKontonummer()).get(1);
            assertEquals(Buchungsart.UEBERWEISUNG_AUSGANG, aus.art());
            assertEquals(new Geldbetrag(-50), aus.betrag());
            assertEquals(new Geldbetrag(150), aus.saldoDanach());
            assertEquals("An Müller, Bert (" + bertGiro.getIbanFormatiert() + ") – Miete Oktober", aus.text());
            Buchung ein = verwaltung.getBuchungen(bertGiro.getKontonummer()).get(0);
            assertEquals(Buchungsart.UEBERWEISUNG_EINGANG, ein.art());
            assertEquals(new Geldbetrag(50), ein.betrag());
            assertEquals("Von Schmidt, Anna (" + giro.getIbanFormatiert() + ") – Miete Oktober", ein.text());
            assertEquals("anna.s", ein.ausgefuehrtVon());
            assertEquals(Protokollaktion.UEBERWEISUNG, letzterEintrag().aktion());
        }

        @Test
        void ueberweisen_ohneVerwendungszweck() {
            portal.ueberweisen(anna, giro.getKontonummer(), bertGiro.getIban(), new Geldbetrag(1), null);
            assertEquals("Von Schmidt, Anna (" + giro.getIbanFormatiert() + ")",
                    verwaltung.getBuchungen(bertGiro.getKontonummer()).get(0).text());
        }

        @Test
        void ueberweisen_imDispo_undDarueberHinaus() {
            portal.ueberweisen(anna, giro.getKontonummer(), bertGiro.getIban(), new Geldbetrag(300), "");
            assertEquals(new Geldbetrag(-100), giro.getKontostand());
            IllegalStateException e = assertThrows(IllegalStateException.class, () ->
                    portal.ueberweisen(anna, giro.getKontonummer(), bertGiro.getIban(), new Geldbetrag(0.01), ""));
            assertTrue(e.getMessage().startsWith("Die Überweisung ist nicht gedeckt"));
        }

        @Test
        void ueberweisen_nurVomGirokonto_undNurAnAktiveGirokonten() {
            // Die Meldung wird mitgeprüft: Die Bank lehnt solche Überweisungen ohnehin ab, meldet
            // dann aber nur "nicht gedeckt" – der Kunde soll den eigentlichen Grund erfahren.
            IllegalStateException vomSparbuch = assertThrows(IllegalStateException.class,
                    () -> portal.ueberweisen(anna, spar.getKontonummer(), bertGiro.getIban(), new Geldbetrag(1), ""));
            assertEquals("Überweisungen sind nur von einem Girokonto aus möglich.", vomSparbuch.getMessage());
            IllegalStateException aufsSparbuch = assertThrows(IllegalStateException.class,
                    () -> portal.ueberweisen(anna, giro.getKontonummer(), spar.getIban(), new Geldbetrag(1), ""));
            assertEquals("Das Empfängerkonto kann keine Überweisungen empfangen.", aufsSparbuch.getMessage());
            verwaltung.kontoSperren(admin, bertGiro.getKontonummer());
            IllegalStateException aufsGesperrte = assertThrows(IllegalStateException.class,
                    () -> portal.ueberweisen(anna, giro.getKontonummer(), bertGiro.getIban(), new Geldbetrag(1), ""));
            assertEquals("Das Empfängerkonto kann keine Überweisungen empfangen.", aufsGesperrte.getMessage());
            assertEquals(new Geldbetrag(200), giro.getKontostand(), "nichts abgebucht");
        }

        @Test
        void ueberweisen_vomGesperrtenKonto_wirft() {
            portal.kontoSperren(anna, giro.getKontonummer());
            IllegalStateException e = assertThrows(IllegalStateException.class,
                    () -> portal.ueberweisen(anna, giro.getKontonummer(), bertGiro.getIban(), new Geldbetrag(1), ""));
            assertTrue(e.getMessage().contains("gesperrt"));
            assertEquals(new Geldbetrag(0), bertGiro.getKontostand());
        }

        @Test
        void ueberweisen_ungueltigeAngaben() {
            assertThrows(IllegalArgumentException.class,
                    () -> portal.ueberweisen(anna, giro.getKontonummer(), " ", new Geldbetrag(1), ""));
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                    () -> portal.ueberweisen(anna, giro.getKontonummer(), "DE00 0000", new Geldbetrag(1), ""));
            assertTrue(e.getMessage().contains("nicht bekannt"));
            assertThrows(IllegalArgumentException.class,
                    () -> portal.ueberweisen(anna, giro.getKontonummer(), giro.getIban(), new Geldbetrag(1), ""));
            assertThrows(IllegalArgumentException.class,
                    () -> portal.ueberweisen(anna, giro.getKontonummer(), bertGiro.getIban(), new Geldbetrag(0), ""));
            assertThrows(IllegalArgumentException.class, () -> portal.ueberweisen(anna, giro.getKontonummer(),
                    bertGiro.getIban(), new Geldbetrag(1), "x".repeat(Kundenportal.MAX_VERWENDUNGSZWECK + 1)));
            assertThrows(IllegalArgumentException.class,
                    () -> portal.ueberweisen(anna, bertGiro.getKontonummer(), giro.getIban(), new Geldbetrag(1), ""),
                    "fremdes Absenderkonto");
            assertEquals(new Geldbetrag(200), giro.getKontostand());
        }

        @Test
        void umbuchen_zwischenEigenenKonten() {
            portal.umbuchen(anna, giro.getKontonummer(), spar.getKontonummer(), new Geldbetrag(80), "Sparen");
            assertEquals(new Geldbetrag(120), giro.getKontostand());
            assertEquals(new Geldbetrag(80), spar.getKontostand());
            Buchung aus = verwaltung.getBuchungen(giro.getKontonummer()).get(1);
            assertEquals(Buchungsart.UMBUCHUNG_AUSGANG, aus.art());
            assertEquals("Sparen auf " + spar, aus.text());
            Buchung ein = verwaltung.getBuchungen(spar.getKontonummer()).get(0);
            assertEquals(Buchungsart.UMBUCHUNG_EINGANG, ein.art());
            assertEquals("Sparen von " + giro, ein.text());
            assertEquals(Protokollaktion.UMBUCHUNG, letzterEintrag().aktion());

            portal.umbuchen(anna, spar.getKontonummer(), giro.getKontonummer(), new Geldbetrag(10), null);
            assertEquals("Umbuchung auf " + giro, verwaltung.getBuchungen(spar.getKontonummer()).get(1).text());
        }

        @Test
        void umbuchen_respektiertDieRegelnDesAbgebendenKontos() {
            IllegalStateException e = assertThrows(IllegalStateException.class,
                    () -> portal.umbuchen(anna, spar.getKontonummer(), giro.getKontonummer(), new Geldbetrag(1), ""));
            assertTrue(e.getMessage().contains("Sparbuch"), e.getMessage());
            assertEquals(new Geldbetrag(200), giro.getKontostand());
        }

        @Test
        void umbuchen_aufGesperrtesKonto_buchtNichtsAb() {
            portal.kontoSperren(anna, spar.getKontonummer());
            assertThrows(IllegalStateException.class,
                    () -> portal.umbuchen(anna, giro.getKontonummer(), spar.getKontonummer(), new Geldbetrag(5), ""));
            assertEquals(new Geldbetrag(200), giro.getKontostand());
            assertEquals(1, verwaltung.getBuchungen(giro.getKontonummer()).size());
        }

        @Test
        void umbuchen_ungueltigeAngaben() {
            assertThrows(IllegalArgumentException.class,
                    () -> portal.umbuchen(anna, giro.getKontonummer(), giro.getKontonummer(), new Geldbetrag(5), ""));
            assertThrows(IllegalArgumentException.class,
                    () -> portal.umbuchen(anna, giro.getKontonummer(), bertGiro.getKontonummer(), new Geldbetrag(5), ""));
            assertThrows(IllegalArgumentException.class,
                    () -> portal.umbuchen(anna, giro.getKontonummer(), spar.getKontonummer(), null, ""));
        }
    }
}
