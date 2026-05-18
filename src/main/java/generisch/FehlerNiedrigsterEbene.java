package generisch;

/**
 * Probe für Exceptions, die keinen Verursacher haben
 */
public class FehlerNiedrigsterEbene implements Probe<Exception> {

	@Override
	public boolean isOk(Exception pruefobjekt) {
		return pruefobjekt.getCause() == null;
	}
}
