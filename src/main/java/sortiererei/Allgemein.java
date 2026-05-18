package sortiererei;

/**
 * Eine Klasse von Objekten, die sich mit allem vergleichen lassen
 */
public class Allgemein implements Comparable<Object>
{
	@Override
	public int compareTo(Object o) {
		return Integer.compare(this.hashCode(), o.hashCode());
	}
	
}