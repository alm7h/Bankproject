package spielereien;

import java.util.OptionalDouble;
import java.util.function.DoubleUnaryOperator;

/**
 * Klasse zur Nullstellensuche einer mathematischen Funktion
 * mittels des Bisektionsverfahrens (Intervallhalbierung).
 * <p>Die Methode erwartet, dass f(a) und f(b) verschiedene Vorzeichen haben
 * (Zwischenwertsatz). Sie halbiert das Intervall solange, bis seine Länge
 * kleiner als 0.01 ist, und gibt dann den Mittelpunkt als Näherung zurück.</p>
 */
public class Nullstellensuche {

    /**
     * Sucht eine Nullstelle der übergebenen Funktion im Intervall [a, b]
     * mittels Bisektionsverfahren.
     *
     * <p>Die Funktion wird als Lambda-Ausdruck (DoubleUnaryOperator) übergeben.</p>
     *
     * @param f  die mathematische Funktion (ℝ → ℝ), z.B. {@code x -> x*x - 25}
     * @param a  linke Intervallgrenze
     * @param b  rechte Intervallgrenze
     * @return   {@link OptionalDouble} mit der gefundenen Näherung der Nullstelle,
     *           oder {@link OptionalDouble#empty()}, wenn im Intervall kein
     *           Vorzeichenwechsel vorliegt (d.h. keine Nullstelle gefunden werden kann)
     */
    public static OptionalDouble findeNullstelle(DoubleUnaryOperator f, double a, double b) {

        // Kein Vorzeichenwechsel → keine Nullstelle im Intervall (oder gar keine)
        // Statt if: Ausdruck als bedingten Rückgabewert formuliert
        return (f.applyAsDouble(a) * f.applyAsDouble(b) > 0)
                ? OptionalDouble.empty()
                : bisektion(f, a, b);
    }

    /**
     * Führt das eigentliche Bisektionsverfahren durch.
     * Rekursiv: halbiert das Intervall, bis es kleiner als 0.01 ist.
     * @param f  die Funktion
     * @param a  linke Grenze (f(a) und f(b) haben verschiedene Vorzeichen)
     * @param b  rechte Grenze
     * @return   Näherungswert der Nullstelle als OptionalDouble
     */
    private static OptionalDouble bisektion(DoubleUnaryOperator f, double a, double b) {
        double mitte = (a + b) / 2.0;

        // Abbruchbedingung: Intervall kleiner als 0.01 → Näherung gut genug
        return (Math.abs(b - a) < 0.01)
                ? OptionalDouble.of(mitte)
                // Vorzeichenwechsel in linker oder rechter Hälfte?
                // f(a)*f(mitte) <= 0  →  Nullstelle in [a, mitte], sonst in [mitte, b]
                : (f.applyAsDouble(a) * f.applyAsDouble(mitte) <= 0)
                ? bisektion(f, a, mitte)
                : bisektion(f, mitte, b);
    }
}