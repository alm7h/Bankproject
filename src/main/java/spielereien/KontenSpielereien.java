package spielereien;

import java.time.LocalDate;
import java.util.Scanner;

import bankprojekt.basisdaten.Geldbetrag;
import bankprojekt.basisdaten.Girokonto;
import bankprojekt.basisdaten.Konto;
import bankprojekt.basisdaten.Kunde;
import bankprojekt.basisdaten.Sparbuch;
import bankprojekt.exceptions.GesperrtException;
import bankprojekt.nuetzliches.EinAusgabe;

/**
 * Testprogramm für Konten
 * @author Doro
 */
public class    KontenSpielereien {

    /**
     * Testprogramm für Konten
     * @param args wird nicht benutzt
     * @throws java.util.InputMismatchException wenn der Benutzer keine Zahl eingibt
     * @throws NullPointerException wenn der Benutzer eine Zahl außer 1 und 2 eingibt
     */
    public static void main(String[] args) {
        Kunde.MUSTERMANN.setAdresse("ganz woanders");
        System.out.println(Kunde.MUSTERMANN);



        Kunde ich = new Kunde("Dorothea", "Hubrich", "zuhause", LocalDate.parse("1976-07-13"));

        Girokonto k1 = new Girokonto(ich, 999, new Geldbetrag(500));
        //letzter Parameter ist der Dispo
        Girokonto k2 = k1;
        k1.einzahlen(new Geldbetrag(100));
        System.out.println(k2.getKontostand());
        System.out.println("-----------------");

        Kunde inhaber = k1.getInhaber();
        inhaber.setAdresse("unbekannt verzogen");
        System.out.println(k1);
        System.out.println("Meine Adresse: " + ich.getAdresse());
        System.out.println("-----------------");




        @SuppressWarnings("resource")
        Scanner tastatur = new Scanner(System.in);
        System.out.println("1 (Giro) oder 2 (Spar)? ");
        int nr = tastatur.nextInt();
        Konto k = null;
        switch(nr) {
            case 1 -> {
                k = new Girokonto(ich, 1234, new Geldbetrag(1000));
            }
            case 2 -> {
                k = new Sparbuch(ich, 9876);
            }
        }





        k.einzahlen(new Geldbetrag(50));

        try
        {
            double betrag = EinAusgabe.eingabePositiveKommazahl("Wie viel wollen Sie abheben?");
            boolean hatGeklappt = k.abheben(new Geldbetrag(betrag));
            System.out.println("Abhebung hat geklappt: " + hatGeklappt);
            System.out.println(k.toString());
            //k.ausgebenAufDerKonsole();
        }
        catch (GesperrtException e)
        {
            System.out.println("Zugriff auf gesperrtes Konto - Polizei rufen!");
        }

	



	
/*		Object nurAusgeben = k1;
		System.out.println(nurAusgeben);
		
		Konto k3 = new Girokonto(ich, 123, new Geldbetrag(500));
		Konto k4 = new Girokonto(ich, 123, new Geldbetrag(500));
		if(k3.equals(k4)) {
			System.out.println("mit equals gleich");
		}
		if(k3 == k4) {
			System.out.println("mit == gleich");
		}
*/
    }
}
