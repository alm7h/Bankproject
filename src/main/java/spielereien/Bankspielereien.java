package spielereien;

import java.time.LocalDate;

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
		
	}
}
