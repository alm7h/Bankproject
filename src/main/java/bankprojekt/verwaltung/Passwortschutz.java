package bankprojekt.verwaltung;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Hasht und prüft Passwörter mit PBKDF2 (HMAC-SHA256), dem im JDK enthaltenen
 * Standardverfahren zur Passwortspeicherung. Passwörter werden nie im Klartext
 * gespeichert, sondern nur als Hash mit zufälligem Salz.
 * <p>
 * Format des gespeicherten Werts: {@code pbkdf2-sha256$Iterationen$Salz$Hash}
 * (Salz und Hash Base64-kodiert). Weil die Iterationszahl mitgespeichert wird,
 * bleiben alte Hashes prüfbar, wenn sie später erhöht wird.
 * @author m.alharethi
 */
public final class Passwortschutz {
    /**
     * Kennung des Verfahrens am Anfang des gespeicherten Werts
     */
    private static final String KENNUNG = "pbkdf2-sha256";
    /**
     * Name des Verfahrens im JDK
     */
    private static final String ALGORITHMUS = "PBKDF2WithHmacSHA256";
    /**
     * Anzahl der Iterationen (Empfehlung der OWASP für PBKDF2-HMAC-SHA256)
     */
    static final int ITERATIONEN = 600_000;
    /**
     * Länge des zufälligen Salzes in Byte
     */
    private static final int SALZ_BYTES = 16;
    /**
     * Länge des Hashes in Bit
     */
    private static final int HASH_BITS = 256;
    /**
     * Zufallsquelle für das Salz
     */
    private static final SecureRandom ZUFALL = new SecureRandom();

    /**
     * Nicht instanziierbar: Die Klasse enthält nur statische Methoden.
     */
    private Passwortschutz() {
    }

    /**
     * Berechnet den Hash eines Passworts mit neuem, zufälligem Salz.
     * @param passwort das Passwort im Klartext
     * @return der speicherbare Hash-Wert
     * @throws NullPointerException wenn das Passwort null ist
     */
    public static String hashen(char[] passwort) {
        return hashen(passwort, ITERATIONEN);
    }

    /**
     * Berechnet den Hash eines Passworts mit neuem, zufälligem Salz und der
     * angegebenen Iterationszahl (für Tests, die schnelle Hashes brauchen).
     * @param passwort das Passwort im Klartext
     * @param iterationen Anzahl der Iterationen, größer 0
     * @return der speicherbare Hash-Wert
     * @throws NullPointerException wenn das Passwort null ist
     */
    static String hashen(char[] passwort, int iterationen) {
        if (passwort == null)
            throw new NullPointerException("Passwort darf nicht null sein");
        byte[] salz = new byte[SALZ_BYTES];
        ZUFALL.nextBytes(salz);
        byte[] hash = ableiten(passwort, salz, iterationen);
        Base64.Encoder base64 = Base64.getEncoder();
        return KENNUNG + "$" + iterationen + "$" + base64.encodeToString(salz)
                + "$" + base64.encodeToString(hash);
    }

    /**
     * Prüft, ob ein Passwort zu einem gespeicherten Hash-Wert passt. Der Vergleich
     * benötigt unabhängig vom Ergebnis gleich viel Zeit.
     * @param passwort das eingegebene Passwort
     * @param gespeichert der mit {@link #hashen(char[])} erzeugte Hash-Wert
     * @return true, wenn das Passwort passt; false auch bei null oder einem
     *         beschädigten Hash-Wert
     */
    public static boolean pruefen(char[] passwort, String gespeichert) {
        if (passwort == null || gespeichert == null)
            return false;
        String[] teile = gespeichert.split("\\$");
        if (teile.length != 4 || !KENNUNG.equals(teile[0]))
            return false;
        try {
            int iterationen = Integer.parseInt(teile[1]);
            Base64.Decoder base64 = Base64.getDecoder();
            byte[] salz = base64.decode(teile[2]);
            byte[] erwartet = base64.decode(teile[3]);
            if (iterationen <= 0 || salz.length == 0 || erwartet.length == 0)
                return false;
            return MessageDigest.isEqual(erwartet, ableiten(passwort, salz, iterationen));
        } catch (IllegalArgumentException e) {
            // keine Zahl oder kein gültiges Base64
            return false;
        }
    }

    /**
     * Leitet den Hash aus Passwort und Salz ab.
     * @param passwort das Passwort
     * @param salz das Salz
     * @param iterationen Anzahl der Iterationen
     * @return der Hash
     */
    private static byte[] ableiten(char[] passwort, byte[] salz, int iterationen) {
        PBEKeySpec spezifikation = new PBEKeySpec(passwort, salz, iterationen, HASH_BITS);
        try {
            return SecretKeyFactory.getInstance(ALGORITHMUS).generateSecret(spezifikation).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Das Verfahren " + ALGORITHMUS + " ist nicht verfügbar.", e);
        } finally {
            // Die interne Kopie des Passworts sofort überschreiben
            spezifikation.clearPassword();
        }
    }
}
