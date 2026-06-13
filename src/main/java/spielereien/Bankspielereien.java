package spielereien;

import java.time.LocalDate;
import java.util.List;

import bankprojekt.basisdaten.Geldbetrag;
import bankprojekt.exceptions.GesperrtException;
import bankprojekt.basisdaten.Kunde;
import bankprojekt.verwaltung.Bank;

/**
 * probiert die Stream-Methoden der Bank-Klasse aus
 */
public class Bankspielereien {
	/**
	 * Methoden zur Stream-Aufgabe ausprobieren
	 * @param args wird nicht verwendet
	 */
	public static void main(String[] args) throws GesperrtException {
		Bank bank = new Bank(12345);
		int aktuellesJahr = LocalDate.now().getYear();
		Kunde opa = new Kunde("Opa", "Otto", "Altersheim", LocalDate.of(aktuellesJahr - 70, 3, 1));
		Kunde oma = new Kunde("Oma", "Emma", "Altersheim", LocalDate.of(aktuellesJahr - 67, 12, 5));
		Kunde kind = new Kunde("Kind", "Klein", "zuhause", LocalDate.of(aktuellesJahr - 8, 2, 28));
		Kunde teenager = new Kunde("Teenager", "Mittel", "zuhause", LocalDate.of(aktuellesJahr - 15, 6, 18));
		Kunde geradeErwachsen = new Kunde("Erwachsen", "Neu", "zuhause", LocalDate.of(aktuellesJahr - 18, 1, 1));
		Kunde nochNichtGanzErwachsen = new Kunde("Erwachsen", "Fast", "zuhause", LocalDate.of(aktuellesJahr - 18, 12, 31));
		Kunde mama = new Kunde("Mama", "Erna", "zuhause", LocalDate.of(aktuellesJahr - 42, 3, 5));
		Kunde papa = new Kunde("Papa", "Hugo", "zuhause", LocalDate.of(aktuellesJahr - 43, 7, 15));
		Kunde senior = new Kunde("Uropa", "Heinz", "Neben dem Friedhof", LocalDate.of(aktuellesJahr - 95, 2, 28));
		long nrOpa1 = bank.girokontoErstellen(opa);
		long nrOpa2 = bank.girokontoErstellen(opa);
		long nrOpa3 = bank.girokontoErstellen(opa);
		long nrOma = bank.girokontoErstellen(oma);
		long nrKind1 = bank.girokontoErstellen(kind);
		long nrKind2 = bank.girokontoErstellen(kind);
		long nrTeenager = bank.girokontoErstellen(teenager);
		long nrGeradeErwachsen1 = bank.girokontoErstellen(geradeErwachsen);
		long nrGeradeErwachsen2 = bank.girokontoErstellen(geradeErwachsen);
		long nrNochNichtGanzErwachsen = bank.girokontoErstellen(nochNichtGanzErwachsen);
		long nrMama1 = bank.girokontoErstellen(mama);
		long nrMama2 = bank.girokontoErstellen(mama);
		long nrMama3 = bank.girokontoErstellen(mama);
		long nrMama4 = bank.girokontoErstellen(mama);
		long nrPapa = bank.girokontoErstellen(papa);
		long nrSenior = bank.girokontoErstellen(senior);

        // Konten mit Geld befüllen
        bank.geldEinzahlen(nrOpa1,new Geldbetrag(1000));
        bank.geldEinzahlen(nrOpa2,new Geldbetrag(500));
        // nrOpa3 bleibt leer (0 €), dann abheben → ins Minus
        bank.geldEinzahlen(nrOma,new Geldbetrag(200));
        bank.geldEinzahlen(nrKind1,new Geldbetrag(50));
        // nrKind2 bleibt auf 0 (nicht negativ, soll NICHT erscheinen)
        bank.geldEinzahlen(nrTeenager,new Geldbetrag(80));
        bank.geldEinzahlen(nrGeradeErwachsen1, new Geldbetrag(0));   // 0 → nicht negativ
        bank.geldEinzahlen(nrGeradeErwachsen2, new Geldbetrag(30));
        bank.geldEinzahlen(nrMama1,new Geldbetrag(2000));
        bank.geldEinzahlen(nrMama2,new Geldbetrag(750));
        bank.geldEinzahlen(nrPapa,new Geldbetrag(300));
        bank.geldEinzahlen(nrSenior,new Geldbetrag(5000));

        // Konten absichtlich ins Minus bringen (Girokonto hat Dispo 500 €)
        // Opa Konto 3: abheben mehr als der Dispo erlaubt geht nicht, aber wir
        // nutzen den Dispo: Kontostand = 0, Dispo = 500 → abheben 300 → Stand = -300
        bank.geldAbheben(nrOpa3,new Geldbetrag(300));   // Opa ins Minus  (-300 €)
        bank.geldAbheben(nrMama3,new Geldbetrag(200));   // Mama ins Minus (-200 €)
        bank.geldAbheben(nrNochNichtGanzErwachsen, new Geldbetrag(100)); // Fast ins Minus (-100 €)

        // ================================================================
        // 1. getKundenMitLeeremKonto()
        // ================================================================
        System.out.println("1. getKundenMitLeeremKonto()");
        System.out.println("Kunden mit mindestens einem Konto im Minus:\n");
        List<Kunde> imMinus = bank.getKundenMitLeeremKonto();
        if (imMinus.isEmpty()) {
            System.out.println("  (niemand im Minus)");
        } else {
            imMinus.forEach(k -> System.out.println("  → " + k.getName()
                    + "  (geb. " + k.getGeburtstag() + ")"));
        }

        System.out.println();
        System.out.println("Erwartung: Opa Otto, Erna Mama, Fast Erwachsen");

        // ================================================================
        // 2. getKundengeburtstage()
        // ================================================================
        System.out.println("Alle Kunden nach Monat/Tag sortiert (Geburtsjahr ignoriert):\n");
        System.out.println(bank.getKundengeburtstage());
        System.out.println();
        System.out.println("Erwartung: Erwachsen(Jan.)  Uropa (Feb.), Kind (Feb.), Opa (März), Mama (März),");
        System.out.println("          Teenager (Juni) Papa(Juli) Erwachsen Fast (Dez./31) kommt nach Oma (Dez./5) – beide Dez.");

        // ================================================================
        // 3. getAnzahlSenioren()
        // ================================================================
        long anzahl = bank.getAnzahlSenioren();
        System.out.println("Kunden mit Alter ≥ 67 Jahre:\n");
        System.out.println("  Anzahl Senioren: " + anzahl);
        System.out.println();
        System.out.println("Erwartung: 2  (Opa 70,Uropa 95)");

        // ================================================================
        // 4. schenkungAnNeuerwachsene(Geldbetrag)
        // ================================================================

        // Kontostand VOR der Schenkung festhalten
        Geldbetrag vorher1 = bank.getKontostand(nrGeradeErwachsen1);
        Geldbetrag vorher2 = bank.getKontostand(nrGeradeErwachsen2);
        Geldbetrag vorherFast = bank.getKontostand(nrNochNichtGanzErwachsen);
        Geldbetrag vorherTeenager = bank.getKontostand(nrTeenager);
        Geldbetrag vorherOpa1 = bank.getKontostand(nrOpa1);

        System.out.println("Kontostand VOR der Schenkung:");
        System.out.println("  GeradeErwachsen Konto 1 : " + vorher1);
        System.out.println("  GeradeErwachsen Konto 2 : " + vorher2);
        System.out.println("  Fast Erwachsen (Dez.31) : " + vorherFast);
        System.out.println("  Teenager                : " + vorherTeenager);
        System.out.println("  Opa Konto 1             : " + vorherOpa1);

        // Schenkung ausführen
        Geldbetrag geschenk = new Geldbetrag(200);
        bank.schenkungAnNeuerwachsene(geschenk);

        Geldbetrag nachher1 = bank.getKontostand(nrGeradeErwachsen1);
        Geldbetrag nachher2 = bank.getKontostand(nrGeradeErwachsen2);
        Geldbetrag nachherFast = bank.getKontostand(nrNochNichtGanzErwachsen);
        Geldbetrag nachherTeenager = bank.getKontostand(nrTeenager);
        Geldbetrag nachherOpa1 = bank.getKontostand(nrOpa1);

        System.out.println("\nKontostand NACH der Schenkung (+200 €):");
        System.out.println("  GeradeErwachsen Konto 1 : " + nachher1);
        System.out.println("  GeradeErwachsen Konto 2 : " + nachher2);
        System.out.println("  Fast Erwachsen (Dez.31) : " + nachherFast);
        System.out.println("  Teenager                : " + nachherTeenager);
        System.out.println("  Opa Konto 1             : " + nachherOpa1);

        System.out.println();

    }
		
	}
