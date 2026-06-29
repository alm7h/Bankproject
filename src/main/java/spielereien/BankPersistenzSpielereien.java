package spielereien;

// Übung 10: neues Hauptprogramm zum Aufgabenteil a)
// (mehrere Bank-Objekte in einer zip-Datei speichern und wieder einlesen).
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import bankprojekt.basisdaten.Geldbetrag;
import bankprojekt.basisdaten.Kunde;
import bankprojekt.verwaltung.Bank;

/**
 * Kleines Hauptprogramm, das zwei Banken anlegt, sie in zwei Einträge einer
 * zip-Datei ("bankdatei.zip") schreibt und anschließend wieder einliest.
 * Demonstriert die in {@link Bank#speichern(java.io.OutputStream)} und
 * {@link Bank#einlesen(java.io.InputStream)} ergänzte Persistenz zusammen mit
 * {@link ZipOutputStream}/{@link ZipInputStream}.
 *
 * @author Claude
 */
public class BankPersistenzSpielereien {

    /**
     * Dateiname der zip-Datei, in der die beiden Banken gespeichert werden.
     */
    private static final String DATEINAME = "bankdatei.zip";

    /**
     * Startet das Programm.
     * @param args werden nicht verwendet
     * @throws IOException wenn beim Schreiben oder Lesen der zip-Datei etwas schiefgeht
     */
    public static void main(String[] args) throws IOException {
        // 1. Zwei Banken mit ein paar Konten anlegen
        Bank ersteBank = ersteBankAnlegen();
        Bank zweiteBank = zweiteBankAnlegen();

        // 2. Beide Banken in zwei Einträge einer zip-Datei schreiben
        try (ZipOutputStream zipAus = new ZipOutputStream(new FileOutputStream(DATEINAME))) {
            // erster Eintrag: ersteBank.dat
            zipAus.putNextEntry(new ZipEntry("ersteBank.dat"));
            ersteBank.speichern(zipAus);
            zipAus.closeEntry();

            // zweiter Eintrag: zweiteBank.dat
            zipAus.putNextEntry(new ZipEntry("zweiteBank.dat"));
            zweiteBank.speichern(zipAus);
            zipAus.closeEntry();

            // Bearbeitung der zip-Datei abschließen
            zipAus.finish();
        }
        System.out.println("Die Datei \"" + DATEINAME + "\" wurde mit zwei Einträgen erstellt.");
        System.out.println();

        // 3. Beide Banken wieder einlesen und die Kontenübersicht ausgeben
        try (ZipInputStream zipEin = new ZipInputStream(new FileInputStream(DATEINAME))) {
            ZipEntry eintrag;
            // getNextEntry() bringt uns jeweils zum nächsten Eintrag der zip-Datei
            while ((eintrag = zipEin.getNextEntry()) != null) {
                System.out.println("=== Eintrag: " + eintrag.getName() + " ===");
                Bank geladen = Bank.einlesen(zipEin);
                System.out.print(geladen.getAlleKonten());
                System.out.println();
                zipEin.closeEntry();
            }
        }
    }

    /**
     * Legt die erste Bank mit ein paar Konten an.
     * @return die erste Bank
     */
    private static Bank ersteBankAnlegen() {
        Bank bank = new Bank(10010010);
        Kunde anna = new Kunde("Anna", "Schmidt", "Berlin", 1990, 5, 1);
        Kunde bert = new Kunde("Bert", "Müller", "Bonn", 1985, 3, 3);

        long giro = bank.girokontoErstellen(anna);
        bank.geldEinzahlen(giro, new Geldbetrag(250));

        long spar = bank.sparbuchErstellen(bert);
        bank.geldEinzahlen(spar, new Geldbetrag(1000));

        return bank;
    }

    /**
     * Legt die zweite Bank mit ein paar Konten an.
     * @return die zweite Bank
     */
    private static Bank zweiteBankAnlegen() {
        Bank bank = new Bank(20020020);
        Kunde clara = new Kunde("Clara", "Weber", "Köln", 2000, 7, 7);

        long giro = bank.girokontoErstellen(clara);
        bank.geldEinzahlen(giro, new Geldbetrag(42));

        long spar = bank.sparbuchErstellen(clara);
        bank.geldEinzahlen(spar, new Geldbetrag(5000));

        return bank;
    }
}
