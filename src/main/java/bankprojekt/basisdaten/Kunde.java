package bankprojekt.basisdaten;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import java.util.Objects;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Kunde einer Bank
 * @author Dorothea Hubrich
 */
@SuppressWarnings("unused")
// Übung 10: Serializable ergänzt, damit der Kontoinhaber mit dem
// Bank-Objekt serialisiert werden kann (LocalDate und String sind bereits serialisierbar).
public class Kunde implements Comparable<Kunde>, Serializable {

    /**
     * Versionsnummer für die Serialisierung.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Ein Musterkunde
     */
    public static final Kunde MUSTERMANN = new Kunde();

    /**
     * englische oder deutsche Anrede, je nach den Systemeinstellungen
     */
    private static final String ANREDE;

    /**
     * liefert die systemspezifische Anrede
     * @return systemspezifische Anrede
     */
    public static String getAnrede() {
        return ANREDE;
    }

    /**
     * der Vorname
     */
    private String vorname;
    /**
     * Der Nachname
     */
    private String nachname;
    /**
     * Die Adresse
     */
    // Übung 13: Die Adresse ist jetzt als JavaFX-Property verfügbar, damit die
    // Oberfläche sie direkt an ein Steuerelement binden kann. Das Feld ist
    // transient, weil JavaFX-Properties nicht serialisierbar sind; der reine
    // String-Wert wird in writeObject/readObject von Hand gesichert.
    private transient StringProperty adresse = new SimpleStringProperty();
    /**
     * Geburtstag
     */
    private LocalDate geburtstag;

    /**
     * erzeugt den Standardkunden Max Mustermann
     */
    public Kunde() {
        this("Max", "Mustermann", "Adresse", LocalDate.now());
    }

    /**
     * Erzeugt einen Kunden mit den übergebenen Werten
     * @param vorname Vorname
     * @param nachname Nachname
     * @param adresse Adresse
     * @param geburtstag Geburtstag
     * @throws NullPointerException wenn einer der Parameter null ist
     */
    public Kunde(String vorname, String nachname,
                 String adresse, LocalDate geburtstag) throws NullPointerException {
        setVorname(vorname);
        setNachname(nachname);
        setAdresse(adresse);
        setGeburtstag(geburtstag);
    }

    /**
     * Erzeugt einen Kunden mit den übergebenen Werten
     * @param vorname Vorname
     * @param nachname Nachname
     * @param adresse Adresse
     * @param geburtstag Geburtstag im Format tt.mm.yy
     * @throws java.time.format.DateTimeParseException Falls das Format des übergebenen Datums nicht korrekt ist
     * @throws NullPointerException Falls einer der Parameter null ist
     */
    public Kunde(String vorname, String nachname,
                 String adresse, String geburtstag)  {
        this(vorname, nachname, adresse,
                LocalDate.parse(geburtstag, DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)));
    }

    /**
     * Erzeugt einen Kunden mit den übergebenen Werten
     * @param vorname Vorname
     * @param nachname Nachname
     * @param adresse Adresse
     * @param gebJahr Geburtsjahr
     * @param gebMonat Geburtsmonat
     * @param gebTag Tag des Geburtstages
     * @throws java.time.format.DateTimeParseException Falls die drei Werte für den Geburtstag kein gültiges
     *                           Datum ergeben
     */
    public Kunde(String vorname, String nachname, String adresse,
                 int gebJahr, int gebMonat, int gebTag)  {
        this(vorname, nachname, adresse, LocalDate.of(gebJahr, gebMonat, gebTag));
    }

    /**
     * Adresse des Kunden
     *
     * @return Adresse des Kunden
     */
    public String getAdresse() {
        return adresse.get();   // Übung 13: Wert aus der Property lesen
    }

    /**
     * Setzt die Adresse auf den angegebenen Wert
     * @param adresse neue Adresse
     * @throws NullPointerException Falls adresse null ist
     */
    public void setAdresse(String adresse) throws NullPointerException {
        if(adresse == null)
            throw new NullPointerException("Adresse darf nicht null sein");
        this.adresse.set(adresse);   // Übung 13: Wert in die Property schreiben
    }

    /**
     * Übung 13: Die Adresse als Property, damit die Oberfläche sie beobachten
     * und daran binden kann.
     * @return die Adresse als StringProperty
     */
    public StringProperty adresseProperty() {
        return adresse;
    }

    /**
     * Nachname des Kunden
     * @return Nachname des Kunden
     */
    public String getNachname() {
        return nachname;
    }

    /**
     * setzt den Nachnamen auf den angegebenen Wert
     * @param nachname neuer Nachname
     * @throws NullPointerException wenn Nachname null ist
     */
    public void setNachname(String nachname) throws NullPointerException {
        if(nachname == null)
            throw new NullPointerException("Nachname darf nicht null sein");
        this.nachname = nachname;
    }

    /**
     * Vorname des Kunden
     * @return Vorname des Kunden
     */
    public String getVorname() {
        return vorname;
    }

    /**
     * setzt den Vornamen auf den angegebenen Wert
     * @param vorname neuer Vorname
     * @throws NullPointerException wenn Vorname null ist
     */
    public void setVorname(String vorname) throws NullPointerException {
        if(vorname == null)
            throw new NullPointerException("Vorname darf nicht null sein");
        this.vorname = vorname;
    }

    /**
     * Geburtstag des Kunden
     * @return Geburtstag des Kunden
     */
    public LocalDate getGeburtstag() {
        return geburtstag;
    }
    
    public void setGeburtstag(LocalDate geburtstag) throws NullPointerException, IllegalArgumentException {
        if(geburtstag == null) throw new NullPointerException("Geburtstag darf nicht null");
        if (geburtstag.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Geburtstag darf nicht nach dem heutigen Datum überschreiten");
        }
        this.geburtstag = geburtstag;
    }

    /**
     * vollständiger Name des Kunden in der Form "Nachname, Vorname"
     *
     * @return vollständiger Name des Kunden
     */
    public String getName() {
        return this.nachname + ", " + this.vorname;
    }

    /**
     * gibt alle Daten des Kunden aus
     */
    @Override
    public String toString() {
        String ausgabe;
        DateTimeFormatter df = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT);
        ausgabe = this.vorname + " " + this.nachname +
                " wohnt in " + this.getAdresse() +
                " (" + df.format(this.geburtstag)+")";
        return ausgabe;
    }

    /**
     * Vergleich von this mit other; Zwei Kunden gelten als gleich,
     * wen sie den gleichen vollständigen Namen haben
     *
     * @param other der Vergleichskunde
     * @return True, wenn beide Kunden den gleichen Namen haben
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || this.getClass() != other.getClass()) {
            return false;
        }
        Kunde that = (Kunde) other;
        return this.getName().equals(that.getName());
    }

    /**
     * Calculates the hash code for the customer object based on the customer's full name.
     * The full name is derived from the combination of the customer's last name and first name.
     *
     * @return the hash code value for this customer
     */
    @Override
    public int hashCode()
    {
        return Objects.hash(this.getName());
    }

    static
    {
        if(Locale.getDefault().getCountry().equals("DE"))
            ANREDE = "Hallo Benutzer!";
        else
            ANREDE = "Dear Customer!";
    }

    /**
     * Compares this customer object to another customer based on their full names.
     * The comparison is determined using the lexicographical order of the full names.
     *
     * @param other the customer to be compared with this customer
     * @return a negative integer, zero, or a positive integer as this customer's
     *         name is less than, equal to, or greater than the other customer's name
     */
    @Override
    public int compareTo(Kunde other) {
        return this.getName().compareTo(other.getName());
    }

    /**
     * Übung 13: Da die Adresse jetzt in einer transienten JavaFX-Property steckt,
     * wird ihr reiner String-Wert hier von Hand mitserialisiert.
     * @param out der Ausgabestrom
     * @throws IOException bei einem Fehler beim Schreiben
     */
    @Serial
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();          // vorname, nachname, geburtstag
        out.writeObject(adresse.get());    // Adresse als String sichern
    }

    /**
     * Übung 13: Baut nach dem Einlesen die transiente Adress-Property wieder auf.
     * @param in der Eingabestrom
     * @throws IOException bei einem Fehler beim Lesen
     * @throws ClassNotFoundException falls eine Klasse nicht gefunden wird
     */
    @Serial
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        this.adresse = new SimpleStringProperty((String) in.readObject());
    }
}