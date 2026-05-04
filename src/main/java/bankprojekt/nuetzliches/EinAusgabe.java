package bankprojekt.nuetzliches;

import java.util.InputMismatchException;
import java.util.Scanner;

/**
 * Nützliche Methoden für Ein- und Ausgaben auf der Konsole
 */
public class EinAusgabe {
	/**
	 * fordert den Benutzer zur Eingabe einer double-Zahl auf
	 * @param aufforderung Aufforderungstext, der auf der Konsole erscheint
	 * @return die eingegebenen Zahl
	 */
	public static double eingabePositiveKommazahl(String aufforderung)
	{
		if(aufforderung == null)
			aufforderung = "Bitte Zahl eingeben: ";
		@SuppressWarnings("resource")
		Scanner tastatur = new Scanner(System.in);
		System.out.println(aufforderung);
		double zahl = -1;
		do {
			try {
				zahl = tastatur.nextDouble();			
			} catch (InputMismatchException e)
			{
				System.out.print("Und noch mal... :");
				tastatur.next();
			}
		} while (zahl < 0);
		return zahl;
	}
}
