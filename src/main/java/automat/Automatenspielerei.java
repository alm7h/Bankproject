package automat;

/**
 * Test für EndlicherAutomat
 * @author Dorothea Hubrich
 */
public class Automatenspielerei {
	/**
	 * Testet mit Hilfe des Bezeichner-Automatens die Gültigkeit einiger
	 * Namen für Variablen
	 * @param args wird nicht verwendet
	*/
	public static void main(String[] args)
	{
		EndlicherAutomat variablenautomat = new Bezeichnerautomat();
		//der Automat prüft auf gültige Variablennamen
		System.out.println("abc: " + variablenautomat.testen("abc"));  //true
		System.out.println("1abc: " + variablenautomat.testen("1abc"));  //false
		System.out.println("a: " + variablenautomat.testen("a"));  //true
		System.out.println("_a_b_c: " + variablenautomat.testen("_a_b_c"));  //true
		System.out.println("ha.llo: " + variablenautomat.testen("ha.llo"));  //false
		System.out.println("_: " + variablenautomat.testen("_"));  //true
		System.out.println("größer: " + variablenautomat.testen("größer"));  //false
	}

}
