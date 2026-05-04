package automat;

/**
 * Ein Endlicher Automat, der prüft, ob eine Zeichenkette ein gültiger
 * C-Bezeichner ist.
 */
public class Bezeichnerautomat implements EndlicherAutomat {

    private int zustand;

    /**
     * Erzeugt einen neuen Bezeichnerautomaten im Startzustand.
     */
    public Bezeichnerautomat() {
        starten();
    }

    @Override
    public void starten() {
        this.zustand = 0;
    }

    @Override
    public void zustandswechsel(char zeichen) {
        switch (zustand) {
            case 0:

                if (Character.isLetter(zeichen) || zeichen == '_') {
                    zustand = 1;
                } else {
                    zustand = 2;
                }
                break;
            case 1:

                if (Character.isLetter(zeichen) || Character.isDigit(zeichen) || zeichen == '_') {
                    zustand = 1;
                } else {
                    zustand = 2;
                }
                break;
            case 2:
                // wenn zustand 2 einmal erreicht bleibt immer im zustand 2
                zustand = 2;
                break;
            default:
                zustand = 2;
        }
    }

    @Override
    public boolean istInAkzeptierendemZustand() {
        // Nur Zustand 1 ist ein akzeptierender Endzustand (Doppelkreis im Diagramm)
        return zustand == 1;
    }
}