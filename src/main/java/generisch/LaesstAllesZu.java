package generisch;

/**
 * eine Probe, die alle Elemente erlaubt
 */
public class LaesstAllesZu implements Probe<Object> {

	@Override
	public boolean isOk(Object pruefobjekt) {
		if(pruefobjekt == null) 
			throw new NullPointerException();
		return true;
	}

}
