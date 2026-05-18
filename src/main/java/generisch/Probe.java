package generisch;

/**
 * prüft, ob ein Objekt eine bestimmte Bedingung erfüllt
 * @param <X> Datentyp der zu überprüfenden Objekte
 */
public interface Probe<X> {
	/**
	 * prüft, ob pruefobjekt eine bestimmte Bedingung erfüllt
	 * @param pruefobjekt Das zu überprüfende Objekt
	 * @return true, wenn die Bedingung erfüllt ist
	 * @throws NullPointerException wenn pruefobjekt null ist
	 */
	boolean isOk(X pruefobjekt);
}
