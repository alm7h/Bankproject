package spielereien;

import java.time.LocalDate;
import bankprojekt.basisdaten.*;

import java.util.Comparator;
import java.util.Scanner;
import java.util.Arrays;


/**
 * Klasse, um ein paar Konten und Kunden zu sortieren und Arrays.sort
 * zu nutzen
 */
public class KundenUndKontenSortieren {
	/**
	 * sortiert ein paar Kunden und Konten
	 * @param args wird nicht verwendet
	 */
	public static void main(String[] args) {
		Scanner tastatur = new Scanner(System.in);
		System.out.println("Bitte Bankleitzahl eingeben: ");
		long blz = tastatur.nextLong();  //ohne große Fehlerbehandlung...

		Kunde hans = new Kunde("Hans", "Meier", "Unterm Regenbogen 19", LocalDate.of(1990, 1, 5));
		Kunde otto = new Kunde("Otto", "Kar", "Hoch über den Wolken 7", LocalDate.of(1992, 2, 25));
		Kunde sabrina = new Kunde("Sabrina", "August", "Im Wald 15", LocalDate.of(1988, 3, 21));
		Kunde doro = new Kunde("Doro", "Hubrich", "Zuhause", LocalDate.of(1976, 7, 13));
		Kunde miriam = new Kunde("Miriam", "Andres", "Woanders 17", LocalDate.of(2002, 7, 15));
		Kunde ernst = new Kunde("Ernst", "August", "Im Wald 15", LocalDate.of(1989, 3, 1));
		
		Konto eins = new Girokonto(hans, 123, Geldbetrag.NULL_EURO);
		Konto zwei = new Sparbuch(otto, 234);
		Konto drei = new Sparbuch(doro, 333);
		Konto vier = new Girokonto(sabrina, 432, Geldbetrag.NULL_EURO);
		Konto fuenf = new Sparbuch(miriam, 987);
		Konto sechs = new Girokonto(ernst, 945, Geldbetrag.NULL_EURO);
		
		Kunde[] kunden = {hans, otto, sabrina, doro, miriam, ernst};
		Konto[] konten = {eins, zwei, drei, vier, fuenf, sechs};

        //************************** Übung 1 *************************************************

        // Sortierung der Kunden nach Nachname, Vorname
        Arrays.sort(kunden);
        System.out.println("Sortierte Kunden:");
        for(Kunde k : kunden){
            System.out.println(k);
        }

        // Sortiert nach der in Konto definierten compareTo-Methode (Kontonummern)
        Arrays.sort(konten);
        System.out.println("Sortierte Konten nach Kontonummern:");
        for(Konto k : konten){
            System.out.println(k);
        }

        // Sortierung der Konten nach Geburtsdatum
        Arrays.sort(konten, new Comparator<Konto>() {
            @Override
            public int compare(Konto k1, Konto k2) {
                return k1.getInhaber().getGeburtstag().compareTo(k2.getInhaber().getGeburtstag());
            }
        });

        System.out.println("Sortierte Konten nach Geburtsdatum:");
        for(Konto k : konten){
            System.out.println(k);
        }

        // Sortierung der Konton nach IBAN
        Arrays.sort(konten,new Comparator<Konto>() {
            @Override
            public int compare(Konto k1, Konto k2) {
                String iban1  = k1.getIban(blz);
                String iban2  = k2.getIban(blz);
                return iban1.compareTo(iban2);
            }
        });

        System.out.println("Sortierte Konten nach IBAN:");
        for(Konto k : konten){
            System.out.println(k);
        }
		
	}

}
