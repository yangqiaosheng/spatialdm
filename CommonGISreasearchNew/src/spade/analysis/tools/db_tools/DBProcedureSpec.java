package spade.analysis.tools.db_tools;

import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 29-Dec-2006
 * Time: 15:59:57
 * Specifies a database procedure over data from some table.
 */
public class DBProcedureSpec {
	/**
	 * The divisions to be applied to the data. These must be instances of
	 * subclasses of DivisionSpec.
	 */
	public Vector divisions = null;
	/**
	 * Indicates whether it is necessary to count the number of data items
	 * for each partition.
	 */
	public boolean findCount = true;
	/**
	 * Specifications for obtaining attribute statistics, instances of AttrStatSpec.
	 */
	public Vector attrStatSpecs = null;

	/**
	 * Adds a specification for data division
	 */
	public void addDivisionSpec(DivisionSpec spec) {
		if (divisions == null) {
			divisions = new Vector(10, 10);
		}
		divisions.addElement(spec);
	}

	/**
	 * Returns the number of division specifications
	 */
	public int getDivisionSpecCount() {
		if (divisions == null)
			return 0;
		return divisions.size();
	}

	/**
	 * Returns the division specification with the given index
	 */
	public DivisionSpec getDivisionSpec(int idx) {
		if (divisions == null || idx < 0 || idx >= divisions.size())
			return null;
		return (DivisionSpec) divisions.elementAt(idx);
	}

	/**
	 * Adds a specification for obtaining attribute statistics.
	 */
	public void addAttrStatSpec(AttrStatSpec spec) {
		if (attrStatSpecs == null) {
			attrStatSpecs = new Vector(20, 20);
		}
		attrStatSpecs.addElement(spec);
	}

	/**
	 * Returns the number of specifications for obtaining attribute statistics
	 */
	public int getAttrStatSpecCount() {
		if (attrStatSpecs == null)
			return 0;
		return attrStatSpecs.size();
	}

	/**
	 * Returns the specification for obtaining attribute statistics with the
	 * given index
	 */
	public AttrStatSpec getAttrStatSpec(int idx) {
		if (attrStatSpecs == null || idx < 0 || idx >= attrStatSpecs.size())
			return null;
		return (AttrStatSpec) attrStatSpecs.elementAt(idx);
	}
}
