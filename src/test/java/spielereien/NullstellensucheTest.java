package spielereien;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import spielereien.Nullstellensuche;

import java.util.OptionalDouble;
import java.util.function.DoubleUnaryOperator;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * JUnit5 Tests
 * Echte Lambda-Ausdrücke werden für mathematische Korrektheitstests verwendet.
 */
@ExtendWith(MockitoExtension.class)
class NullstellensucheTest {

    /** Toleranz: Bisektion stoppt bei Intervall < 0.01, max. Fehler = 0.005 */
    private static final double TOLERANZ = 0.01;

    // =================================================================
    // 1. Korrektheit – echte Lambda-Ausdrücke
    // =================================================================
    @Nested
    @DisplayName("Mathematische Korrektheit (echte Lambdas)")
    class KorrektheitsTests {

        @Test
        @DisplayName("f(x)=x²-25 in [0,10] → Nullstelle ≈ 5")
        void f_xQuadratMinus25_positivIntervall() {
            OptionalDouble result = Nullstellensuche.findeNullstelle(x -> x * x - 25, 0, 10);
            assertTrue(result.isPresent());
            assertEquals(5.0, result.getAsDouble(), TOLERANZ);
        }

        @Test
        @DisplayName("f(x)=x²-25 in [-10,0] → Nullstelle ≈ -5")
        void f_xQuadratMinus25_negativIntervall() {
            OptionalDouble result = Nullstellensuche.findeNullstelle(x -> x * x - 25, -10, 0);
            assertTrue(result.isPresent());
            assertEquals(-5.0, result.getAsDouble(), TOLERANZ);
        }

        @Test
        @DisplayName("g(x)=e^(3x)-7 in [0,2] → Nullstelle ≈ ln(7)/3")
        void g_expFunktion() {
            OptionalDouble result = Nullstellensuche.findeNullstelle(
                    x -> Math.exp(3 * x) - 7, 0, 2);
            assertTrue(result.isPresent());
            assertEquals(Math.log(7) / 3.0, result.getAsDouble(), TOLERANZ);
        }

        @Test
        @DisplayName("h(x)=sin(x²)-0.5 in [0,1] → Nullstelle ≈ √(π/6)")
        void h_sinFunktion() {
            OptionalDouble result = Nullstellensuche.findeNullstelle(
                    x -> Math.sin(x * x) - 0.5, 0, 1);
            assertTrue(result.isPresent());
            assertEquals(Math.sqrt(Math.PI / 6.0), result.getAsDouble(), TOLERANZ);
        }

        @Test
        @DisplayName("k(x)=x²+1 in [-10,10] → empty (keine reelle Nullstelle)")
        void k_keineNullstelle() {
            OptionalDouble result = Nullstellensuche.findeNullstelle(x -> x * x + 1, -10, 10);
            assertTrue(result.isEmpty(),
                    "k(x)=x²+1 hat keine reelle Nullstelle → OptionalDouble.empty() erwartet");
        }

        @Test
        @DisplayName("Lineare Funktion f(x)=x-3 in [0,10] → Nullstelle ≈ 3")
        void linear_nullstelleBei3() {
            OptionalDouble result = Nullstellensuche.findeNullstelle(x -> x - 3, 0, 10);
            assertTrue(result.isPresent());
            assertEquals(3.0, result.getAsDouble(), TOLERANZ);
        }

        @Test
        @DisplayName("Nullstelle an linker Intervallgrenze f(5)=0 → wird erkannt")
        void nullstelleAmLinkenRand() {
            // f(a)=0 → Produkt f(a)*f(b) = 0 → nicht > 0 → Bisektion läuft
            OptionalDouble result = Nullstellensuche.findeNullstelle(x -> x - 5, 5, 10);
            assertTrue(result.isPresent());
            assertEquals(5.0, result.getAsDouble(), TOLERANZ);
        }

        @Test
        @DisplayName("Sehr kleines Intervall [4.99, 5.01] → Ergebnis sofort (Intervall < 0.01)")
        void kleinIntervall_sofortAbbruch() {
            // 5.01 - 4.99 = 0.02 > 0.01, genau ein Bisektionsschritt nötig
            OptionalDouble result = Nullstellensuche.findeNullstelle(x -> x - 5, 4.99, 5.01);
            assertTrue(result.isPresent());
            assertEquals(5.0, result.getAsDouble(), TOLERANZ);
        }
    }
}