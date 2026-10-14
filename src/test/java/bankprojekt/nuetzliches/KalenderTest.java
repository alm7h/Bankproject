package bankprojekt.nuetzliches;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testklasse für den {@link Kalender}.
 */
class KalenderTest {

    @Test
    void getHeutigesDatum_liefertDasHeutigeDatum() {
        // Datum vor und nach dem Aufruf merken: so bleibt der Test auch dann korrekt,
        // wenn er genau um Mitternacht läuft
        LocalDate vorher = LocalDate.now();
        LocalDate heute = new Kalender().getHeutigesDatum();
        LocalDate nachher = LocalDate.now();

        assertNotNull(heute);
        assertFalse(heute.isBefore(vorher), "Datum liegt vor dem heutigen Tag: " + heute);
        assertFalse(heute.isAfter(nachher), "Datum liegt nach dem heutigen Tag: " + heute);
    }

    @Test
    void kalender_istSerialisierbar_undLiefertNachDemEinlesenWeiterDasHeutigeDatum() throws Exception {
        // Ein Sparbuch hält einen Kalender und wird mit der Bank serialisiert
        assertTrue(Serializable.class.isAssignableFrom(Kalender.class));

        ByteArrayOutputStream puffer = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(puffer)) {
            out.writeObject(new Kalender());
        }
        Kalender kopie;
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(puffer.toByteArray()))) {
            kopie = (Kalender) in.readObject();
        }

        LocalDate vorher = LocalDate.now();
        LocalDate heute = kopie.getHeutigesDatum();
        LocalDate nachher = LocalDate.now();
        assertFalse(heute.isBefore(vorher));
        assertFalse(heute.isAfter(nachher));
    }
}
