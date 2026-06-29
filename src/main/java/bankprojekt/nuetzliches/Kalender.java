package bankprojekt.nuetzliches;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * Bereitstellung des heutigen Datums
 */
// Übung 10: Serializable ergänzt, da ein Sparbuch ein Kalender-Objekt hält
// und damit beim Speichern der Bank mit serialisiert werden muss.
public class Kalender implements Serializable {
	/**
	 * Versionsnummer für die Serialisierung.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * liefert das heutige Datum
	 * @return das heutige Datum
	 */
	public LocalDate getHeutigesDatum() {
		return LocalDate.now();
	}
}
