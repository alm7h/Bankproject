package bankprojekt.basisdaten;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testklasse für den {@link Kunde}n.
 */
class KundeTest {

    private static final LocalDate GEBURTSTAG = LocalDate.of(2000, 1, 1);

    private Kunde neuerKunde() {
        return new Kunde("Max", "Müller", "Weg 1", GEBURTSTAG);
    }

    @Nested
    class KonstruktorTests {
        @Test
        void konstruktorMitLocalDate_setztAlleWerte() {
            Kunde k = neuerKunde();
            assertEquals("Max", k.getVorname());
            assertEquals("Müller", k.getNachname());
            assertEquals("Weg 1", k.getAdresse());
            assertEquals(GEBURTSTAG, k.getGeburtstag());
        }

        @Test
        void konstruktorMitJahrMonatTag_setztGeburtstag() {
            Kunde k = new Kunde("Max", "Müller", "Weg 1", 1985, 3, 15);
            assertEquals(LocalDate.of(1985, 3, 15), k.getGeburtstag());
        }

        @Test
        void konstruktorMitJahrMonatTag_wirftException_beiUngueltigemDatum() {
            // LocalDate.of meldet ein ungültiges Datum mit einer DateTimeException
            assertThrows(java.time.DateTimeException.class,
                    () -> new Kunde("Max", "Müller", "Weg 1", 2001, 2, 30));
        }

        @Test
        void standardkonstruktor_erzeugtMaxMustermannMitHeutigemGeburtstag() {
            Kunde k = new Kunde();
            assertEquals("Max", k.getVorname());
            assertEquals("Mustermann", k.getNachname());
            assertEquals("Adresse", k.getAdresse());
            assertEquals(LocalDate.now(), k.getGeburtstag());
        }

        @Test
        void konstruktor_wirftNullPointerException_beiNullWerten() {
            assertThrows(NullPointerException.class, () -> new Kunde(null, "Müller", "Weg 1", GEBURTSTAG));
            assertThrows(NullPointerException.class, () -> new Kunde("Max", null, "Weg 1", GEBURTSTAG));
            assertThrows(NullPointerException.class, () -> new Kunde("Max", "Müller", null, GEBURTSTAG));
            assertThrows(NullPointerException.class, () -> new Kunde("Max", "Müller", "Weg 1", (LocalDate) null));
        }

        @Test
        void konstruktor_wirftIllegalArgumentException_beiGeburtstagInDerZukunft() {
            LocalDate morgen = LocalDate.now().plusDays(1);
            assertThrows(IllegalArgumentException.class, () -> new Kunde("Max", "Müller", "Weg 1", morgen));
        }
    }

    /**
     * Der String-Konstruktor parst das Datum im kurzen Format des Standard-Locales.
     * Laut JavaDoc ist das Format "tt.mm.yy", daher wird hier mit deutschem Locale getestet.
     */
    @Nested
    class StringKonstruktorTests {
        private Locale vorher;

        @BeforeEach
        void localeSetzen() {
            vorher = Locale.getDefault();
            Locale.setDefault(Locale.GERMANY);
        }

        @AfterEach
        void localeZuruecksetzen() {
            Locale.setDefault(vorher);
        }

        @Test
        void stringKonstruktor_parstDatumImFormatTtMmYy_ausDemJahr2000er() {
            Kunde k = new Kunde("Max", "Müller", "Weg 1", "15.03.05");
            assertEquals(LocalDate.of(2005, 3, 15), k.getGeburtstag());
        }

        @Test
        void stringKonstruktor_parstDatumImFormatTtMmYy_ausDem20Jahrhundert() {
            // Ein Kunde, der 1985 geboren ist, muss sich mit "15.03.85" anlegen lassen
            Kunde k = new Kunde("Max", "Müller", "Weg 1", "15.03.85");
            assertEquals(LocalDate.of(1985, 3, 15), k.getGeburtstag());
        }

        @Test
        void stringKonstruktor_wirftDateTimeParseException_beiFalschemFormat() {
            assertThrows(DateTimeParseException.class, () -> new Kunde("Max", "Müller", "Weg 1", "kein Datum"));
        }
    }

    @Nested
    class SetterTests {
        @Test
        void setter_aendernDieWerte() {
            Kunde k = neuerKunde();
            k.setVorname("Moritz");
            k.setNachname("Meier");
            k.setAdresse("Allee 2");
            k.setGeburtstag(LocalDate.of(1990, 5, 6));
            assertEquals("Moritz", k.getVorname());
            assertEquals("Meier", k.getNachname());
            assertEquals("Allee 2", k.getAdresse());
            assertEquals(LocalDate.of(1990, 5, 6), k.getGeburtstag());
        }

        @Test
        void setter_wirftNullPointerException_beiNull() {
            Kunde k = neuerKunde();
            assertThrows(NullPointerException.class, () -> k.setVorname(null));
            assertThrows(NullPointerException.class, () -> k.setNachname(null));
            assertThrows(NullPointerException.class, () -> k.setAdresse(null));
            assertThrows(NullPointerException.class, () -> k.setGeburtstag(null));
        }

        @Test
        void setGeburtstag_erlaubtHeute_undLehntMorgenAb() {
            Kunde k = neuerKunde();
            k.setGeburtstag(LocalDate.now());
            assertEquals(LocalDate.now(), k.getGeburtstag());
            assertThrows(IllegalArgumentException.class, () -> k.setGeburtstag(LocalDate.now().plusDays(1)));
            assertEquals(LocalDate.now(), k.getGeburtstag(), "Ungültiger Wert darf nichts verändern");
        }
    }

    @Nested
    class AdressPropertyTests {
        @Test
        void adresseProperty_spiegeltSetAdresse() {
            Kunde k = neuerKunde();
            k.setAdresse("Allee 2");
            assertEquals("Allee 2", k.adresseProperty().get());
        }

        @Test
        void aenderungUeberProperty_istUeberGetterSichtbar() {
            Kunde k = neuerKunde();
            k.adresseProperty().set("Platz 3");
            assertEquals("Platz 3", k.getAdresse());
        }
    }

    @Nested
    class VergleichTests {
        @Test
        void getName_liefertNachnameKommaVorname() {
            assertEquals("Müller, Max", neuerKunde().getName());
        }

        @Test
        void equals_gleicherNameReicht_auchBeiAndererAdresseUndGeburtstag() {
            Kunde a = neuerKunde();
            Kunde b = new Kunde("Max", "Müller", "Ganz woanders", LocalDate.of(1970, 7, 7));
            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
        }

        @Test
        void equals_falseBeiAnderemNamen() {
            assertNotEquals(neuerKunde(), new Kunde("Moritz", "Müller", "Weg 1", GEBURTSTAG));
            assertNotEquals(neuerKunde(), new Kunde("Max", "Meier", "Weg 1", GEBURTSTAG));
        }

        @Test
        void equals_trueFuerDasselbeObjekt() {
            Kunde k = neuerKunde();
            assertTrue(k.equals(k));
        }

        @Test
        void equals_falseBeiNullUndFremdemTyp() {
            // equals direkt aufrufen: assertNotEquals(null, x) bzw. assertNotEquals("…", x)
            // würde nicht Kunde.equals, sondern nur die Nullprüfung bzw. String.equals ausführen
            Kunde k = neuerKunde();
            assertFalse(k.equals(null));
            assertFalse(k.equals("Müller, Max"));
        }

        @Test
        void compareTo_sortiertNachVollstaendigemNamen() {
            Kunde mueller = neuerKunde();
            Kunde meier = new Kunde("Zoe", "Meier", "Weg 1", GEBURTSTAG);
            assertTrue(meier.compareTo(mueller) < 0, "Meier kommt vor Müller");
            assertTrue(mueller.compareTo(meier) > 0);
            assertEquals(0, mueller.compareTo(new Kunde("Max", "Müller", "x", GEBURTSTAG)));
        }
    }

    @Nested
    class AusgabeTests {
        private Locale vorher;

        @BeforeEach
        void localeSetzen() {
            vorher = Locale.getDefault();
            Locale.setDefault(Locale.GERMANY);
        }

        @AfterEach
        void localeZuruecksetzen() {
            Locale.setDefault(vorher);
        }

        @Test
        void toString_enthaeltNameAdresseUndKurzesDatum() {
            assertEquals("Max Müller wohnt in Weg 1 (01.01.00)", neuerKunde().toString());
        }

        @Test
        void getAnrede_istEineDerBeidenVorgesehenenAnreden() {
            // Die Anrede wird einmalig beim Laden der Klasse aus dem Locale bestimmt
            assertTrue(Kunde.getAnrede().equals("Hallo Benutzer!")
                    || Kunde.getAnrede().equals("Dear Customer!"));
        }
    }

    @Test
    void serialisierung_stelltAlleWerteInklusiveAdresseWiederHer() throws Exception {
        Kunde original = neuerKunde();

        ByteArrayOutputStream puffer = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(puffer)) {
            out.writeObject(original);
        }
        Kunde gelesen;
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(puffer.toByteArray()))) {
            gelesen = (Kunde) in.readObject();
        }

        assertEquals(original, gelesen);
        assertEquals("Max", gelesen.getVorname());
        assertEquals("Müller", gelesen.getNachname());
        assertEquals(GEBURTSTAG, gelesen.getGeburtstag());
        assertEquals("Weg 1", gelesen.getAdresse(), "Adresse steckt in einer transienten Property");
        assertNotNull(gelesen.adresseProperty(), "Property muss nach dem Einlesen neu aufgebaut sein");
    }
}
