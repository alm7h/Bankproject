package sortiererei;

/**
 * Interface für den Vergleich zweier Objekte
 * @author Doro 
 *
 */
public interface Vergleicher<T> {
	/**
	 * vergleicht a und b
	 * @param a zu vergleichendes Objekt
	 * @param b zu vergleichendes Objekt
	 * @return positiver Wert, wenn a größer als b, 
	 *         negativer Wert, wenn a kleiner als b
	 *         und 0, wenn a == b 
	 * @throws IllegalArgumentException wenn a und b nicht verglichen werden können
	 */
	public int vergleichen(T a, T b);
	
	/**
	 * gibt zurück, ob a > b
	 * @param a zu vergleichendes Objekt
	 * @param b zu vergleichendes Objekt
	 * @return a > b
	 * @throws IllegalArgumentException wenn a und b nicht verglichen werden können
	 */
	public default boolean isGroesser(T a, T b)
	{
		return vergleichen(a, b) > 0;
	}
	
	
	/**
	 * prüft, ob a kleiner b
	 * @param a erstes zu vergleichesdes Objekt
	 * @param b zweites zu vergleichendes Objekt
	 * @return a kleiner b
	 * @throws IllegalArgumentException wenn a und b nicht verglichen werden können
	 */
	default public boolean isKleiner(T a, T b)
	{
		return this.vergleichen(a,  b) < 0;
	}
	
	/**
	 * prüft, ob a == b
	 * @param a erstes zu vergleichesdes Objekt
	 * @param b zweites zu vergleichendes Objekt
	 * @return a == b
	 * @throws IllegalArgumentException wenn a und b nicht verglichen werden können
	 */
	default public boolean isGleich(T a, T b)
	{
		return this.vergleichen(a,  b) == 0;
	}
	
	/**
	 * sortiert das Array x aufsteigend
	 * @param x das zu sortierende Array
	 * @throws NullPointerException wenn x == null
	 */
	default public void sortiere(T[] x) {
		boolean unsortiert = true;
		T temp;
		while (unsortiert) 
		{
			unsortiert = false;          
			for (int i = 0; i < x.length - 1; i++) 
			{
				if (this.isGroesser(x[i], x[i+1])) 
				{
					temp = x[i];
					x[i] = x[i + 1];
					x[i + 1] = temp;
					unsortiert = true; 
				}
			}
		}
	}
	
}
