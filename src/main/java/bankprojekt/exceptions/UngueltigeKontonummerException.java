package bankprojekt.exceptions;

/**
 * tritt auf, wenn versucht wird, eine ungültige Kontonummer (also
 * negativ oder mehr als 10 Stellen) zu vergeben
 */
@SuppressWarnings("serial")
public class UngueltigeKontonummerException extends RuntimeException {
    /**
     * Erzeugt die Exception für eine ungültige Kontonummer.
     */
    public UngueltigeKontonummerException() {
    }

}
