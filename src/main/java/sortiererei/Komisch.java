package sortiererei;

/**
 * Eine eher unsinnige Klasse, deren Objekte nur mit anderen
 * Number-Objekten vergleichbar sind, aber nicht untereinander. Man kann 
 * sie also nicht sortieren.
 */
public class Komisch implements Comparable<Number>{
	/**
	 * im Objekt gespeicherter Wert
	 */
	private long wert;

	/**
	 * erstellt ein Objekt der Klasse mit dem angegegeben Wert
	 * @param wert Wert des Objekts
	 */
	public Komisch(long wert) {
		this.wert = wert;
	}

	@Override
	public int compareTo(Number o) {
		return Long.compare(wert, o.longValue());
	}
	
	@Override
	public String toString() {
		return "Komisch - " + wert + " Hash: " + this.hashCode();
	}
}
