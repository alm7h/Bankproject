package generisch;
import java.time.LocalDate;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.JapaneseDate;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import bankprojekt.basisdaten.Geldbetrag;
import bankprojekt.basisdaten.Girokonto;
import bankprojekt.basisdaten.Konto;
import bankprojekt.basisdaten.Kunde;
import bankprojekt.basisdaten.Sparbuch;

/**
 * eine sehr primitive Liste
 * @author Doro
 * @param <T> Datentyp der Elemente in der Liste
 */
public class SortierteListe<T extends Comparable<? super T>> {
	/**
	 * ein Element der Liste
	 * @author Doro
	 */
	private class Element
	{
		private T wert;
		private Element naechster;
		/**
		 * erstellt ein Element der Liste mit Inhalt w und 
		 * Nachfolger n
		 * @param w Inhalt des neuen Elementes
		 * @param n sein Nachfolger
		 */
		public Element(T w, Element n)
		{ 
			wert = w;
			naechster = n;
		}
	}

	/**
	 * "Zeiger" auf das erste Element der Liste
	 */
	private Element anfang = null;
	
	/**
	 * Die von allen Elementen zu erfüllende Bedingung
	 */
	private Probe<? super T> bedingung;

	/**
	 * erstellt eine leere Liste, deren Elemente die bedingung erfüllen müssen
	 * @param bedingung zu überprüfende bedingung für alle Elemente in der Liste
	 * @throws IllegalArgumentException wenn bedingung null ist
	 */
	public SortierteListe(Probe<? super T> bedingung)
	{
		if(bedingung == null)
			throw new IllegalArgumentException();
		this.bedingung = bedingung;
	}
	
	/**
	 * fügt den angegeben Wert in die Liste ein, wenn es die Bedingung erfüllt
	 * @param w einzufügender Wert
	 * @throws NullPointerException wenn w null ist
	 */
	public void add(T w)
	{
		if(bedingung.isOk(w))
		{

			Element neu = new Element(w, null);
			Element zeiger = anfang;
			if(zeiger == null)
			{
           			anfang = neu;
        		}
        		else if(zeiger.wert.compareTo(w) > 0)
        		{
                		neu.naechster = anfang;
                		anfang = neu;
        		}
        		else
       			{
            			while(zeiger.naechster != null 
            					&& zeiger.naechster.wert.compareTo(w) < 0)
            			{
                			zeiger = zeiger.naechster;
            			}
            			neu.naechster = zeiger.naechster;
            			zeiger.naechster = neu;
        		}
		}
	}

	/**
     * Fügt alle Elemente der angegebenen Sammlung zur Liste hinzu, die die Bedingung erfüllen.
     * Jedes Element wird einzeln mit der Methode {@code add} der Liste hinzugefügt.
     *
     * @param neueMitglieder die Sammlung der hinzuzufügenden Elemente
     *                        (kann null sein, in diesem Fall passiert nichts)
     */
    public void addAll(Collection<? extends T> neueMitglieder) {
        if (neueMitglieder == null) return;
        for (T element : neueMitglieder) {
            this.add(element);
        }
    }


    /**
     * Removes all elements from the list that meet the specified condition.
     * The condition is defined by the provided instance of {@code Probe}.
     * If the condition is null, the method does nothing.
     *
     * @param bedingung the condition that determines which elements to remove from the list
     *                  (must not be null to be applied)
     */
    public void remove(Probe<? super T> bedingung) {
        if (bedingung == null) return;

        // Sonderfall: Passende Elemente am Anfang entfernen
        while (anfang != null && bedingung.isOk(anfang.wert)) {
            anfang = anfang.naechster;
        }

        if (anfang == null) return;

        // Restliche Liste durchlaufen
        Element zeiger = anfang;
        while (zeiger.naechster != null) {
            if (bedingung.isOk(zeiger.naechster.wert)) {
                zeiger.naechster = zeiger.naechster.naechster;
            } else {
                zeiger = zeiger.naechster;
            }
        }
    }

    /**
     * Removes all elements from the list that are greater than the specified element.
     * The comparison is based on the natural ordering defined by the {@code Comparable} interface.
     * If the specified element is {@code null}, the method does nothing.
     *
     * @param element the reference element used to determine which elements to remove
     *                (elements greater than this will be removed; null elements are ignored)
     */


    public  <E extends Comparable<? super T>>void removeAllBigger(E element) {
        if (element == null) return;

        // Wenn element.compareTo(wert) < 0, ist der wert größer als element
        while (anfang != null && element.compareTo(anfang.wert) < 0) {
            anfang = anfang.naechster;
        }

        if (anfang == null) return;

        Element zeiger = anfang;
        while (zeiger.naechster != null) {
            if (element.compareTo(zeiger.naechster.wert) < 0) {
                zeiger.naechster = zeiger.naechster.naechster;
            } else {
                zeiger = zeiger.naechster;
            }
        }
    }


	@Override
	public String toString()
	{
		String ausgabe = "[";
		Element zeiger = anfang;
		while(zeiger != null)
		{
			ausgabe += zeiger.wert + ", ";
			zeiger = zeiger.naechster;
		}
		ausgabe += "]";
		return ausgabe;
	}
	
	/**
	 * liefert das erste Element dieser Liste zurück und entfernt es gleichzeitig
	 * @return das erste Element aus dieser Liste
	 * @throws LeerException wenn this keine Elemente enthält.
	 */
	public T getSpitze()
	{
		if(anfang == null)
			throw new LeerException();
		T erstes = anfang.wert;
		anfang = anfang.naechster;
		return erstes;
			
	}

	
	/**
	 * EigeneListe ausprobieren
	 * @param args wird nicht verwendet
	 */
	public static void main(String args[])
	{
		SortierteListe<Integer> l = 
				new SortierteListe<Integer>(new LaesstAllesZu());
				//EigeneListe(Probe<Integer> bedingung)
		l.add(1);
		l.add(2);
		System.out.println(l);
		l.add(5);
		System.out.println(l);



		SortierteListe<String> woerter = 
				new SortierteListe<>(new FaengtAnMitAProbe());
		woerter.add("Adalbert");
		woerter.add("Bertha");
		woerter.add("Anna");
		woerter.add("Anton");
		woerter.add("Zacharias");
		System.out.println(woerter);



/*		SortierteListe<Exception> dasGingAllesSchief = 
				new SortierteListe<>(new FehlerNiedrigsterEbene());
		dasGingAllesSchief.add(new IllegalArgumentException());
		dasGingAllesSchief.add(new ArithmeticException());
		System.out.println(dasGingAllesSchief);
*/

		Kunde ich = new Kunde("Dorothea", "Hubrich", "zuhause", "13/07/76");
		Girokonto g1 = new Girokonto(ich, 1234, new Geldbetrag(0));
		Girokonto g2 = new Girokonto(ich, 999, new Geldbetrag(1000));
		Girokonto g3 = new Girokonto(ich, 34567, new Geldbetrag(70));
		
		SortierteListe<Girokonto> kontoliste = 
				new SortierteListe<>(new LaesstAllesZu());
		kontoliste.add(g1);
		kontoliste.add(g2);
		kontoliste.add(g3);
		System.out.println(kontoliste);


        // 1. SortierteListe mit ChronoLocalDate anlegen und Collection von LocalDate hinzufügen
        SortierteListe<ChronoLocalDate> liste1 = new SortierteListe<>(new LaesstAllesZu());
        Collection<LocalDate> localDates = List.of(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 5, 18),
                LocalDate.of(2026, 12, 31)
        );
        liste1.addAll(localDates);
        System.out.println("Liste 1 (ChronoLocalDate) nach addAll: " + liste1);

        // Elemente mit LaesstAllesZu-Probe löschen
        liste1.remove(new LaesstAllesZu());
        System.out.println("Liste 1 nach remove: " + liste1);


        // 2. Zweite SortierteListe mit LocalDate anlegen und gleiche Collection hinzufügen
        SortierteListe<LocalDate> liste2 = new SortierteListe<>(new LaesstAllesZu());
        liste2.addAll(localDates);
        System.out.println("Liste 2 (LocalDate) nach addAll: " + liste2);


        // JapaneseDate anlegen und größere Elemente entfernen
        JapaneseDate jDate = JapaneseDate.of(2026, 5, 1);
        liste2.removeAllBigger(jDate);
        System.out.println("Liste 2 nach removeAllBigger(JapaneseDate): " + liste2);
		
	}
}
