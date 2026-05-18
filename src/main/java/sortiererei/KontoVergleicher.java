package sortiererei;

import bankprojekt.basisdaten.Geldbetrag;
import bankprojekt.basisdaten.Konto;

/**
* ein Vergleicher für zwei Konten
*/
public class KontoVergleicher implements Vergleicher {

	@Override
	public int vergleichen(Object a, Object b) {
		if(a instanceof Konto ka && b instanceof Konto kb)
			//instanceof sollte nur verwendet werden, wenn es zwingend notwendig ist
			//leider haben wir noch keine Typparameter kennengelernt...
		{
			Geldbetrag standA = ka.getKontostand();
			Geldbetrag standB = kb.getKontostand();
			if(standA.compareTo(standB) > 0) 
				return 1;
			else if(standA.compareTo(standB) < 0) 
				return -1;
			else 
				return 0;
		}
		else
		{
			throw new IllegalArgumentException();
		}
	}

}
