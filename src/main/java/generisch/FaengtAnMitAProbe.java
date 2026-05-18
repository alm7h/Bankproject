package generisch;

/**
 * Probe für Strings: prüft, ob der String mit A beginnt.
 */
public class FaengtAnMitAProbe implements Probe<String> {

	@Override
	public boolean isOk(String pruefobjekt) {
			return pruefobjekt.startsWith("A");
	}

}
