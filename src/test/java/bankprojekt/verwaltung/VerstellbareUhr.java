package bankprojekt.verwaltung;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * Uhr für Tests, die sich vorstellen lässt (z. B. um eine Anmeldesperre ablaufen zu lassen).
 */
public final class VerstellbareUhr extends Clock {
    private Instant jetzt = Instant.parse("2026-10-14T10:00:00Z");

    /**
     * Stellt die Uhr vor.
     * @param dauer um wie viel
     */
    public void vorstellen(Duration dauer) {
        jetzt = jetzt.plus(dauer);
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }

    @Override
    public Instant instant() {
        return jetzt;
    }
}
