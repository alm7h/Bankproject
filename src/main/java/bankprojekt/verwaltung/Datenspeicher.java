package bankprojekt.verwaltung;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

/**
 * Speichert eine {@link Bankverwaltung} dauerhaft in einer Datei (Java-Serialisierung,
 * wie schon {@link Bank#speichern(java.io.OutputStream)}) und lädt sie wieder.
 * <p>
 * Gibt es die Datei noch nicht, wird eine neue Verwaltung mit dem Startbenutzer
 * {@value #STARTBENUTZER} eingerichtet. Dessen Passwort ist <b>nur für die
 * Entwicklung</b> gedacht und muss vor dem echten Einsatz geändert werden.
 * <p>
 * Eine geladene Verwaltung wird nach jeder Änderung automatisch gespeichert.
 * Geschrieben wird zuerst in eine temporäre Datei, die dann die alte Datei
 * ersetzt, damit bei einem Abbruch keine halb geschriebene Datei zurückbleibt.
 * Beim Einlesen werden nur Klassen aus {@code bankprojekt} und dem JDK akzeptiert.
 * @author m.alharethi
 */
public final class Datenspeicher {
    /**
     * Systemeigenschaft, mit der sich die Datei festlegen lässt
     * (z. B. {@code -Dbankprojekt.datei=/tmp/test.ser})
     */
    public static final String DATEI_EIGENSCHAFT = "bankprojekt.datei";
    /**
     * Bankleitzahl einer neu eingerichteten Bank
     */
    public static final long BANKLEITZAHL = 12345678L;
    /**
     * Benutzername des Startbenutzers einer neu eingerichteten Verwaltung
     */
    public static final String STARTBENUTZER = "m.alharethi";
    /**
     * Passwort des Startbenutzers – NUR FÜR DIE ENTWICKLUNG, vor dem echten Einsatz ändern!
     */
    private static final String STARTPASSWORT = "1234";
    /**
     * Beim Einlesen erlaubte Klassen: nur die eigenen und die des JDK
     */
    private static final ObjectInputFilter FILTER =
            ObjectInputFilter.Config.createFilter("bankprojekt.**;java.**;!*");

    /**
     * die Datei, in der die Verwaltung gespeichert wird
     */
    private final Path datei;

    /**
     * Erstellt einen Datenspeicher für die angegebene Datei.
     * @param datei die Datei; fehlende Verzeichnisse werden beim Speichern angelegt
     * @throws NullPointerException wenn die Datei null ist
     */
    public Datenspeicher(Path datei) {
        this.datei = Objects.requireNonNull(datei, "Datei darf nicht null sein");
    }

    /**
     * Liefert die Standarddatei: den Wert der Systemeigenschaft
     * {@value #DATEI_EIGENSCHAFT}, sonst {@code ~/.bankprojekt/verwaltung.ser}.
     * @return der Pfad der Standarddatei
     */
    public static Path standardDatei() {
        String eigene = System.getProperty(DATEI_EIGENSCHAFT);
        if (eigene != null && !eigene.isBlank())
            return Path.of(eigene);
        return Path.of(System.getProperty("user.home"), ".bankprojekt", "verwaltung.ser");
    }

    /**
     * Liefert die Datei dieses Speichers.
     * @return der Pfad der Datei
     */
    public Path getDatei() {
        return datei;
    }

    /**
     * Lädt die Verwaltung aus der Datei. Gibt es die Datei nicht, wird eine neue
     * Verwaltung mit dem Startbenutzer eingerichtet und sofort gespeichert. Die
     * geladene Verwaltung speichert sich danach bei jeder Änderung selbst; scheitert
     * das, wirft die ändernde Methode eine {@link UncheckedIOException}.
     * @return die geladene oder neu eingerichtete Verwaltung
     * @throws IOException wenn die Datei nicht gelesen werden kann oder keine
     *                     gültige Verwaltung enthält
     */
    public Bankverwaltung laden() throws IOException {
        Bankverwaltung verwaltung;
        if (Files.notExists(datei)) {
            verwaltung = ersteinrichtung();
            speichern(verwaltung);
        } else {
            try (ObjectInputStream ein = new ObjectInputStream(
                    new BufferedInputStream(Files.newInputStream(datei)))) {
                ein.setObjectInputFilter(FILTER);
                Object gelesen = ein.readObject();
                if (!(gelesen instanceof Bankverwaltung geladen))
                    throw new InvalidObjectException("Die Datei " + datei + " enthält keine Bankverwaltung.");
                verwaltung = geladen;
            } catch (ClassNotFoundException e) {
                throw new IOException("Die Datei " + datei + " enthält unbekannte Daten.", e);
            }
        }
        verwaltung.setBeiAenderung(() -> speichernUnchecked(verwaltung));
        return verwaltung;
    }

    /**
     * Speichert die Verwaltung in die Datei.
     * @param verwaltung die zu speichernde Verwaltung
     * @throws IOException wenn nicht geschrieben werden kann
     */
    public void speichern(Bankverwaltung verwaltung) throws IOException {
        Path verzeichnis = datei.toAbsolutePath().getParent();
        if (verzeichnis == null)
            throw new IOException("Ungültige Datei: " + datei);
        Files.createDirectories(verzeichnis);
        Path temporaer = Files.createTempFile(verzeichnis, "verwaltung", ".tmp");
        try {
            try (ObjectOutputStream aus = new ObjectOutputStream(
                    new BufferedOutputStream(Files.newOutputStream(temporaer)))) {
                aus.writeObject(verwaltung);
            }
            Files.move(temporaer, datei, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } finally {
            Files.deleteIfExists(temporaer);
        }
    }

    /**
     * Speichert und verpackt einen Fehler in eine ungeprüfte Ausnahme (für den
     * Aufruf nach jeder Änderung).
     * @param verwaltung die zu speichernde Verwaltung
     * @throws UncheckedIOException wenn nicht geschrieben werden kann
     */
    private void speichernUnchecked(Bankverwaltung verwaltung) {
        try {
            speichern(verwaltung);
        } catch (IOException e) {
            throw new UncheckedIOException("Die Änderung konnte nicht gespeichert werden: " + e.getMessage(), e);
        }
    }

    /**
     * Richtet eine neue Verwaltung mit leerer Bank und dem Startbenutzer ein.
     * @return die neue Verwaltung
     */
    static Bankverwaltung ersteinrichtung() {
        Bankverwaltung verwaltung = new Bankverwaltung(new Bank(BANKLEITZAHL));
        verwaltung.erstenBenutzerEinrichten(STARTBENUTZER, "M. Alharethi",
                Passwortschutz.hashen(STARTPASSWORT.toCharArray()));
        return verwaltung;
    }
}
