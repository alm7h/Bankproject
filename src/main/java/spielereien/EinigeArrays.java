package spielereien;

import java.util.Arrays;
import java.util.function.IntToDoubleFunction;
import java.util.function.IntUnaryOperator;

/**
 * Eine kleine Spielerei mit Arrays, um den Umgang mit Interfaces 
 * zu üben
 */
public class EinigeArrays {

	/**
	 * erzeugt zwei Arrays mit Hilfe von Arrays.setAll
	 * @param args wird nicht verwendet
	 */
	public static void main(String[] args) {
		int[] eins = new int[20];
		//in das Array eins die Zahlen von 1 bis 20 hineinschreiben mit Hilfe von setAll:

        Arrays.setAll(eins, new IntUnaryOperator() {
            @Override
            public int applyAsInt(int index) {
                return index + 1; // Index 0 wird zu 1, Index 1 zu 2, etc.
            }
        });
		System.out.println("Die Zahlen von 1 bis 20: ");
		System.out.println(Arrays.toString(eins));

		//in das Array eins immer abwechselnd 0 und 1 hineinschreiben mit Hilfe von setAll:

        Arrays.setAll(eins, new IntUnaryOperator() {
            @Override
            public int applyAsInt(int index) {
                return index % 2; // Nutzt den Modulo-Operator: 0%2=0, 1%2=1, 2%2=0...
            }
        });
		System.out.println("Immer abwechselnd 0 und 1: ");
		System.out.println(Arrays.toString(eins));
		
		double[] zwei = new double[20];
		//in das Array zwei die Zweierpotenzen von 2^0 bis 2^19 hineinschreiben mit Hilfe von setAll:

        Arrays.setAll(zwei, new IntToDoubleFunction() {
            @Override
            public double applyAsDouble(int index) {
                return Math.pow(2, index); // Berechnet 2 hoch Index
            }
        });
		System.out.println("Die Zweierpotenzen von 2 hoch 0 bis 2 hoch 19:");
		System.out.println(Arrays.toString(zwei));
	}

}
