package sortiererei;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;

import bankprojekt.basisdaten.Geldbetrag;
import bankprojekt.basisdaten.Girokonto;
import bankprojekt.basisdaten.Konto;
import bankprojekt.basisdaten.Kunde;
import bankprojekt.basisdaten.Sparbuch;

/**
 * enthält den Sortieralgorithmus BubbleSort
 * @author Doro
 * 
 */
public class Sortieren {

	/**
	 * sortiert das Array x aufsteigend
	 * @param x das zu sortierende Array
	 * @throws NullPointerException wenn x == null
	 */
	public static void sortiere(int[] x) {
		boolean unsortiert = true;
		int temp;
		while (unsortiert) 
		{
			unsortiert = false;          
			for (int i = 0; i < x.length - 1; i++) 
			{
				if (x[i] > x[i + 1]) 
				{
					temp = x[i];
					x[i] = x[i + 1];
					x[i + 1] = temp;
					unsortiert = true; 
				}
			}
		}
	}
	
	/**
	 * sortiert das Array x aufsteigend
	 * @param x das zu sortierende Array
	 * @throws NullPointerException wenn x == null
	 */
	public static <T extends Comparable<? super T>> void sortiere(T[] x) {
		boolean unsortiert = true;
		T temp;
		while (unsortiert) 
		{
			unsortiert = false;          
			for (int i = 0; i < x.length - 1; i++) 
			{
				if (x[i].compareTo(x[i + 1]) > 0) 
				{
					temp = x[i];
					x[i] = x[i + 1];
					x[i + 1] = temp;
					unsortiert = true; 
				}
			}
		}
	}
	
	/**
	 * sortiert das Array x aufsteigend
	 * @param x das zu sortierende Array
	 * @param vergleich bringt die Methode mit, um jeweils zwei Objekte
	 *                  aus x miteinander zu vergleichen
	 * @throws NullPointerException wenn x == null
	 * @throws IllegalArgumentException wenn Objekte aus x nicht mit Hilfe von
	 *                                  vergleich vergleichbar sind oder vergleich
	 *                                  null ist
	 */
	public static <T> void sortiere(T[] x, Vergleicher<? super T> vergleich) {
		if(vergleich == null)
			throw new IllegalArgumentException();
		boolean unsortiert = true;
		T temp;
		while (unsortiert) 
		{
			unsortiert = false;          
			for (int i = 0; i < x.length - 1; i++) 
			{
				if (vergleich.isGroesser(x[i],x[i + 1])) 
				{
					temp = x[i];
					x[i] = x[i + 1];
					x[i + 1] = temp;
					unsortiert = true; 
				}
			}
		}
	}

	/**
	 * sortiert einige Arrays
	 * @param args wird nicht benutzt
	 */
	public static void main(String[] args) {
		int[] liste = { 0, 9, 4, 6, 2, 8, 5, 1, 7, 3 };
		sortiere(liste);
		for (int i = 0; i < liste.length; i++)
			System.out.print(liste[i] + " ");
		System.out.println();
		
		String[] liste2 = { "Physalis", "Apfel", "Orange", "Birne", "Ananas" };
		sortiere(liste2);
		for (int i = 0; i < liste2.length; i++)
			System.out.print(liste2[i] + " ");
		System.out.println();
		
		BigDecimal[] liste3 = { new BigDecimal("123.45"), new BigDecimal("666"),
				              new BigDecimal("100.00"), new BigDecimal("345.67"),
				              new BigDecimal("9999.99")};
		sortiere(liste3);
		for (int i = 0; i < liste3.length; i++)
			System.out.print(liste3[i] + " ");
		System.out.println();
		
		Kunde anna = new Kunde("Anna", "Müller", "hier", LocalDate.parse("1979-05-14"));
		Kunde berta = new Kunde("Berta", "Beerenbaum", "hier", LocalDate.parse("1980-03-15"));
		Kunde chris = new Kunde("Chris", "Tall", "hier", LocalDate.parse("1979-01-07"));
		Kunde anton = new Kunde("Anton", "Meier", "hier", LocalDate.parse("1982-10-23"));
		Girokonto giro1 = new Girokonto(anna, 1246734, new Geldbetrag(500));
		giro1.einzahlen(new Geldbetrag(100));
		Sparbuch spar1 = new Sparbuch(berta, 895975696);
		spar1.einzahlen(new Geldbetrag(200));
		Girokonto giro2 = new Girokonto(chris, 111111, new Geldbetrag(200));
		giro2.einzahlen(new Geldbetrag(20));
		Sparbuch spar2 = new Sparbuch(anton, 773377448);
		spar2.einzahlen(new Geldbetrag(350));
		
		Konto[] liste4 = { giro1, spar1, giro2, spar2};
		sortiere(liste4);
		System.out.println(Arrays.toString(liste4));
		System.out.println();	
		
		System.out.println("----------------------");
        Vergleicher<Konto> x = new KontoVergleicher();
        x.sortiere(liste4);
		for (int i = 0; i < liste4.length; i++)
			System.out.println(liste4[i] + " ");
		System.out.println();
		
		System.out.println("----------------------");
		sortiere(liste4, new AllesVergleicher());
		for (int i = 0; i < liste4.length; i++)
			System.out.println(liste4[i] + " ");
		System.out.println();
	}

}
