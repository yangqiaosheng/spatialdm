package spade.analysis.plot;

/**
 * <p>�berschrift: </p>
 * <p>Beschreibung: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: Fraunhofer AIS.SPADE</p>
 * @author unbekannt
 * @version 1.0
 */
public class PairDataMatrixItem {
	int indexInContainer;
	String screenDescription;

	public PairDataMatrixItem(int index, String description) {
		indexInContainer = index;
		screenDescription = description;
	}
}