package bankprojekt.verwaltung;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testklasse für die Anmeldung mit Sperre nach zu vielen Fehlversuchen ({@link Anmeldung}).
 */
class AnmeldungTest {

    /** schneller Passwort-Hash für den Testbenutzer (Passwort "geheim") */
    private static final String HASH = Passwortschutz.hashen("geheim".toCharArray(), 1_000);

    private VerstellbareUhr uhr;
    private Bankverwaltung verwaltung;
    private Benutzer admin;
    private Anmeldung anmeldung;

    @BeforeEach
    void setUp() {
        uhr = new VerstellbareUhr();
        verwaltung = new Bankverwaltung(new Bank(12345678L), uhr);
        admin = verwaltung.erstenBenutzerEinrichten("admin", "Admin", HASH);
        anmeldung = new Anmeldung(verwaltung);
    }

    /**
     * Liefert den jüngsten Protokolleintrag.
     * @return der letzte Eintrag
     */
    private Protokolleintrag letzterEintrag() {
        List<Protokolleintrag> protokoll = verwaltung.getProtokoll();
        return protokoll.get(protokoll.size() - 1);
    }

    /**
     * Meldet sich mehrfach mit falschem Passwort an.
     * @param anzahl Anzahl der Fehlversuche
     * @return das Ergebnis des letzten Versuchs
     */
    private Anmeldeergebnis fehlversuche(int anzahl) {
        Anmeldeergebnis ergebnis = null;
        for (int i = 0; i < anzahl; i++)
            ergebnis = anmeldung.anmelden("admin", "falsch".toCharArray());
        return ergebnis;
    }

    @Test
    void richtigeZugangsdaten_meldenAn_undWerdenProtokolliert() {
        Anmeldeergebnis ergebnis = anmeldung.anmelden("admin", "geheim".toCharArray());

        assertTrue(ergebnis.isErfolgreich());
        assertEquals(Anmeldeergebnis.Status.ERFOLGREICH, ergebnis.status());
        assertSame(admin, ergebnis.benutzer());
        assertEquals(Duration.ZERO, ergebnis.restsperre());
        assertNotNull(admin.getLetzteAnmeldung());
        assertEquals(Protokollaktion.ANMELDUNG, letzterEintrag().aktion());
        assertEquals("admin", letzterEintrag().benutzer());
    }

    @Test
    void benutzername_ohneRuecksichtAufGrossschreibungUndLeerzeichen() {
        assertTrue(anmeldung.anmelden("  ADMIN ", "geheim".toCharArray()).isErfolgreich());
    }

    @Test
    void falschesPasswort_wirdAbgelehnt_undProtokolliert() {
        Anmeldeergebnis ergebnis = anmeldung.anmelden("admin", "Geheim".toCharArray());

        assertFalse(ergebnis.isErfolgreich());
        assertEquals(Anmeldeergebnis.Status.FALSCHE_ZUGANGSDATEN, ergebnis.status());
        assertNull(ergebnis.benutzer());
        assertEquals(Anmeldung.MAX_FEHLVERSUCHE - 1, ergebnis.verbleibendeVersuche());
        assertEquals(Protokollaktion.ANMELDUNG_FEHLGESCHLAGEN, letzterEintrag().aktion());
        assertEquals("admin", letzterEintrag().benutzer());
        assertNull(admin.getLetzteAnmeldung());
    }

    @Test
    void unbekannterBenutzer_wirdWieFalschesPasswortBehandelt() {
        Anmeldeergebnis ergebnis = anmeldung.anmelden("gibtsnicht", "geheim".toCharArray());
        assertEquals(Anmeldeergebnis.Status.FALSCHE_ZUGANGSDATEN, ergebnis.status());
        assertEquals(Anmeldung.MAX_FEHLVERSUCHE - 1, ergebnis.verbleibendeVersuche());
    }

    @Test
    void leereOderFehlendeEingaben_werdenOhneAusnahmeAbgelehnt() {
        assertEquals(Anmeldeergebnis.Status.FALSCHE_ZUGANGSDATEN, anmeldung.anmelden(null, null).status());
        assertEquals("(leer)", letzterEintrag().benutzer());
        assertEquals(Anmeldeergebnis.Status.FALSCHE_ZUGANGSDATEN,
                anmeldung.anmelden("admin", new char[0]).status());
    }

    @Test
    void deaktivierterBenutzer_kannSichNichtAnmelden_undDasZaehltAlsFehlversuch() {
        Benutzer bert = verwaltung.benutzerAnlegen(admin, "bert", "Bert", "geheim".toCharArray());
        verwaltung.benutzerDeaktivieren(admin, "bert");

        Anmeldeergebnis ergebnis = anmeldung.anmelden("bert", "geheim".toCharArray());

        assertEquals(Anmeldeergebnis.Status.DEAKTIVIERT, ergebnis.status());
        assertNull(ergebnis.benutzer());
        assertEquals(Anmeldung.MAX_FEHLVERSUCHE - 1, ergebnis.verbleibendeVersuche());
        assertNull(bert.getLetzteAnmeldung());
        assertEquals(Protokollaktion.ANMELDUNG_FEHLGESCHLAGEN, letzterEintrag().aktion());
        assertTrue(letzterEintrag().details().contains("deaktiviert"));
    }

    @Test
    void fuenfFehlversuche_sperrenDieAnmeldung_auchFuerRichtigesPasswort() {
        Anmeldeergebnis vierter = fehlversuche(Anmeldung.MAX_FEHLVERSUCHE - 1);
        assertEquals(Anmeldeergebnis.Status.FALSCHE_ZUGANGSDATEN, vierter.status());
        assertEquals(1, vierter.verbleibendeVersuche());

        Anmeldeergebnis fuenfter = fehlversuche(1);
        assertEquals(Anmeldeergebnis.Status.GESPERRT, fuenfter.status());
        assertEquals(Anmeldung.SPERRDAUER, fuenfter.restsperre());
        assertEquals(Protokollaktion.ANMELDUNG_GESPERRT, letzterEintrag().aktion());

        uhr.vorstellen(Duration.ofSeconds(20));
        int eintraege = verwaltung.getProtokoll().size();
        Anmeldeergebnis gesperrt = anmeldung.anmelden("admin", "geheim".toCharArray());
        assertEquals(Anmeldeergebnis.Status.GESPERRT, gesperrt.status());
        assertEquals(Duration.ofSeconds(40), gesperrt.restsperre());
        assertEquals(Duration.ofSeconds(40), anmeldung.getRestsperre());
        assertEquals(eintraege, verwaltung.getProtokoll().size(), "Während der Sperre wird nichts geprüft");
    }

    @Test
    void nachAblaufDerSperre_istAnmeldungWiederMoeglich() {
        fehlversuche(Anmeldung.MAX_FEHLVERSUCHE);
        uhr.vorstellen(Anmeldung.SPERRDAUER);

        assertEquals(Duration.ZERO, anmeldung.getRestsperre());
        assertTrue(anmeldung.anmelden("admin", "geheim".toCharArray()).isErfolgreich());
    }

    @Test
    void nachAblaufDerSperre_beginntDieZaehlungVonVorn() {
        fehlversuche(Anmeldung.MAX_FEHLVERSUCHE);
        uhr.vorstellen(Anmeldung.SPERRDAUER.plusSeconds(1));
        Anmeldeergebnis ergebnis = fehlversuche(1);
        assertEquals(Anmeldeergebnis.Status.FALSCHE_ZUGANGSDATEN, ergebnis.status());
        assertEquals(Anmeldung.MAX_FEHLVERSUCHE - 1, ergebnis.verbleibendeVersuche());
    }

    @Test
    void erfolgreicheAnmeldung_setztDenZaehlerZurueck() {
        fehlversuche(Anmeldung.MAX_FEHLVERSUCHE - 1);
        assertTrue(anmeldung.anmelden("admin", "geheim".toCharArray()).isErfolgreich());
        Anmeldeergebnis ergebnis = fehlversuche(Anmeldung.MAX_FEHLVERSUCHE - 1);
        assertEquals(Anmeldeergebnis.Status.FALSCHE_ZUGANGSDATEN, ergebnis.status());
        assertEquals(1, ergebnis.verbleibendeVersuche());
    }

    @Test
    void ohneFehlversuche_istNichtGesperrt() {
        assertEquals(Duration.ZERO, anmeldung.getRestsperre());
    }

    @Test
    void abmelden_wirdProtokolliert() {
        anmeldung.abmelden(admin);
        assertEquals(Protokollaktion.ABMELDUNG, letzterEintrag().aktion());
        assertEquals("admin", letzterEintrag().benutzer());

        int eintraege = verwaltung.getProtokoll().size();
        anmeldung.abmelden((Benutzer) null);
        anmeldung.abmelden((Kundenzugang) null);
        assertEquals(eintraege, verwaltung.getProtokoll().size());
    }

    @Test
    void kunde_meldetSichUeberDieselbeMaskeAn() {
        Kundenakte anna = verwaltung.kundeAnlegen(admin, Testdaten.kunde("Anna", "Schmidt"));
        Kundenzugang zugang = Testdaten.kundenzugang(verwaltung, "anna.s", anna.getKundennummer());

        Anmeldeergebnis ergebnis = anmeldung.anmelden("Anna.S", Testdaten.PASSWORT.toCharArray());

        assertTrue(ergebnis.isErfolgreich());
        assertTrue(ergebnis.isKunde());
        assertSame(zugang, ergebnis.kundenzugang());
        assertNull(ergebnis.benutzer());
        assertNotNull(zugang.getLetzteAnmeldung());
        assertEquals("Im Kundenportal angemeldet", letzterEintrag().details());
        assertFalse(anmeldung.anmelden("admin", Testdaten.PASSWORT.toCharArray()).isKunde(),
                "Mitarbeiter sind keine Kunden");
    }

    @Test
    void neukunde_mitOffenemAntrag_kannSichSchonAnmelden() {
        Kundenzugang zugang = Testdaten.kundenzugang(verwaltung, "neu", null);
        Anmeldeergebnis ergebnis = anmeldung.anmelden("neu", Testdaten.PASSWORT.toCharArray());
        assertSame(zugang, ergebnis.kundenzugang());
    }

    @Test
    void kundeEinesGesperrtenKunden_kannSichNichtAnmelden() {
        Kundenakte anna = verwaltung.kundeAnlegen(admin, Testdaten.kunde("Anna", "Schmidt"));
        Testdaten.kundenzugang(verwaltung, "anna.s", anna.getKundennummer());
        verwaltung.kundeDeaktivieren(admin, anna.getKundennummer());

        Anmeldeergebnis ergebnis = anmeldung.anmelden("anna.s", Testdaten.PASSWORT.toCharArray());

        assertEquals(Anmeldeergebnis.Status.DEAKTIVIERT, ergebnis.status());
        assertEquals("Zugang ist deaktiviert (Fehlversuch 1 von 5)", letzterEintrag().details());
    }

    @Test
    void kunde_mitFalschemPasswort_zaehltAlsFehlversuch() {
        Testdaten.kundenzugang(verwaltung, "neu", null);
        Anmeldeergebnis ergebnis = anmeldung.anmelden("neu", "falsch".toCharArray());
        assertEquals(Anmeldeergebnis.Status.FALSCHE_ZUGANGSDATEN, ergebnis.status());
        assertFalse(ergebnis.isKunde());
    }

    @Test
    void abmelden_einesKunden_wirdProtokolliert() {
        Kundenzugang zugang = Testdaten.kundenzugang(verwaltung, "neu", null);
        anmeldung.abmelden(zugang);
        assertEquals(Protokollaktion.ABMELDUNG, letzterEintrag().aktion());
        assertEquals("neu", letzterEintrag().benutzer());
        assertEquals("Vom Kundenportal abgemeldet", letzterEintrag().details());
    }

    @Test
    void konstruktor_mitNull_wirftNullPointerException() {
        assertThrows(NullPointerException.class, () -> new Anmeldung(null));
        assertThrows(NullPointerException.class, () -> new Anmeldung(verwaltung, null));
    }
}
