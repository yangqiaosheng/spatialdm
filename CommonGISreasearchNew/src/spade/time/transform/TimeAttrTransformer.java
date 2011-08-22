package spade.time.transform;

import java.util.Vector;

import spade.analysis.transform.BaseAttributeTransformer;
import spade.lib.util.IntArray;
import spade.time.TimeMoment;
import spade.time.vis.VisAttrDescriptor;
import spade.vis.database.Attribute;
import spade.vis.database.Parameter;

/**
* The base class for attribute transformer applied to time-dependent attributes.
*/
public abstract class TimeAttrTransformer extends BaseAttributeTransformer {
	/**
	* The temporal parameter of the table
	*/
	protected Parameter tPar = null;
	/**
	* For each time-dependent attribute to be transformed this vector contains
	* an IntArray with the column numbers containing the values of this attribute
	* at different time moments. The column numbers are sorted according to the
	* respective time moments.
	*/
	protected Vector timeAttrColNs = null;
	/**
	* For each transformed table column contains the index of the corresponding
	* IntArray in the vector timeAttrColNs. This IntArray contains the indexes
	* of the columns corresponding to the same attribute (and, possibly, the same
	* values of non-temporal parameters) but different time moments. For the
	* table columns which are not transformed the corresponding array elements
	* are -1.
	*/
	protected int idxTimeAttrColNs[] = null;

	/**
	* Informs whether the attributes may be (potentially) transformed
	* individually. Returns false
	*/
	@Override
	public boolean getAllowIndividualTransformation() {
		return false;
	}

	/**
	* The transformer is given a list of attribute identifiers selected for
	* visualization. Some of the attributes (but not necessarily all) may be
	* time-dependent. Only time-dependent attributes can be transformed by this
	* transformer.
	*/
	@Override
	public void setAttributes(Vector attrIds) {
		if (attrIds == null || attrIds.size() < 1 || table == null || !table.hasData())
			return;
		if (tPar == null) {
			for (int i = 0; i < table.getParamCount() && tPar == null; i++) {
				tPar = table.getParameter(i);
				if (!tPar.isTemporal()) {
					tPar = null;
				}
			}
		}
		if (tPar == null)
			return; //no temporal parameter in the table
		Vector aDescr = new Vector(attrIds.size(), 1);
		for (int i = 0; i < attrIds.size(); i++) {
			Attribute at = table.getAttribute((String) attrIds.elementAt(i));
			if (at == null) {
				continue;
			}
			VisAttrDescriptor d = null;
			if (!at.hasChildren()) {
				if (at.getParent() == null) {
					continue;
				}
				Object parVal = at.getParamValue(tPar.getName());
				if (parVal == null || !(parVal instanceof TimeMoment)) {
					continue;
				}
				d = new VisAttrDescriptor();
				d.attr = at;
				d.parent = at.getParent();
				d.isTimeDependent = true;
				int npar = at.getParameterCount();
				if (npar > 1) {
					d.fixedParams = new Vector(npar, 1);
					d.fixedParamVals = new Vector(npar, 1);
					for (int j = 0; j < npar; j++) {
						String pname = at.getParamName(j);
						if (!pname.equals(tPar.getName())) {
							d.fixedParams.addElement(pname);
							d.fixedParamVals.addElement(at.getParamValue(j));
						}
					}
				}
			} else {
				if (!at.dependsOnParameter(tPar)) {
					continue;
				}
				d = new VisAttrDescriptor();
				d.parent = at;
				d.isTimeDependent = true;
			}
			if (d != null) {
				aDescr.addElement(d);
			}
		}
		if (aDescr.size() > 0) {
			setAttributeDescriptions(aDescr);
		}
	}

	/**
	* Sets the columns (specified by their numbers) to be transformed by the given
	* transformer. Only time-dependent attributes can be transformed by this
	* transformer; hence, the columns which are not time-relevant will be ignored.
	*/
	@Override
	public void setColumnNumbers(IntArray colNs) {
		transColList = null;
		transColNs = null;
		if (colNs == null || colNs.size() < 1 || table == null)
			return;
		if (tPar == null) {
			for (int i = 0; i < table.getParamCount() && tPar == null; i++) {
				tPar = table.getParameter(i);
				if (!tPar.isTemporal()) {
					tPar = null;
				}
			}
		}
		if (tPar == null || tPar.getValueCount() < 2)
			return; //no temporal parameter in the table
		Vector aDescr = new Vector(colNs.size() / tPar.getValueCount() + 10, 10);
		boolean done[] = new boolean[colNs.size()];
		for (int i = 0; i < colNs.size(); i++) {
			done[i] = false;
		}
		for (int i = 0; i < colNs.size(); i++)
			if (!done[i]) {
				Attribute at = table.getAttribute(colNs.elementAt(i));
				if (at == null || at.getParent() == null) {
					continue;
				}
				Object parVal = at.getParamValue(tPar.getName());
				if (parVal == null || !(parVal instanceof TimeMoment)) {
					continue;
				}
				VisAttrDescriptor d = new VisAttrDescriptor();
				d.attr = at;
				d.parent = at.getParent();
				d.isTimeDependent = true;
				int npar = at.getParameterCount();
				if (npar > 1) {
					d.fixedParams = new Vector(npar, 1);
					d.fixedParamVals = new Vector(npar, 1);
					for (int j = 0; j < npar; j++) {
						String pname = at.getParamName(j);
						if (!pname.equals(tPar.getName())) {
							d.fixedParams.addElement(pname);
							d.fixedParamVals.addElement(at.getParamValue(j));
						}
					}
				}
				//for children of the same parent set done[...] to true
				for (int j = i + 1; j < colNs.size(); j++) {
					Attribute a1 = table.getAttribute(colNs.elementAt(j));
					if (a1 != null && d.parent.equals(a1.getParent()) && a1.getParameterCount() == at.getParameterCount()) {
						boolean same = true;
						if (d.fixedParams != null) {
							for (int k = 0; k < d.fixedParams.size() && same; k++) {
								Object pv = a1.getParamValue((String) d.fixedParams.elementAt(k));
								same = pv != null && pv.equals(d.fixedParamVals.elementAt(k));
							}
						}
						if (same) {
							done[j] = true;
						}
					}
				}
				aDescr.addElement(d);
			}
		if (aDescr.size() > 0) {
			setAttributeDescriptions(aDescr);
		}
	}

	/**
	* The transformer is given a list of descriptors of the attributes selected
	* for visualization. The descriptors are instances of VisAttrDesriptor. Some
	* of the attributes (but not necessarily all) may be time-dependent. Only
	* time-dependent attributes can be transformed by this transformer.
	*/
	public void setAttributeDescriptions(Vector aDescr) {
		if (aDescr == null || aDescr.size() < 1 || table == null || !table.hasData())
			return;
		if (!table.hasTemporalParameter())
			return;
		if (tPar == null) {
			for (int i = 0; i < table.getParamCount() && tPar == null; i++) {
				tPar = table.getParameter(i);
				if (!tPar.isTemporal()) {
					tPar = null;
				}
			}
		}
		if (tPar == null)
			return; //no temporal parameter in the table
		transColNs = new int[table.getAttrCount()];
		idxTimeAttrColNs = new int[table.getAttrCount()];
		for (int i = 0; i < transColNs.length; i++) {
			transColNs[i] = -1;
			idxTimeAttrColNs[i] = -1;
		}
		transColList = new IntArray(50, 50);
		for (int i = 0; i < aDescr.size(); i++) {
			VisAttrDescriptor d = (VisAttrDescriptor) aDescr.elementAt(i);
			Attribute at = (d.parent != null) ? d.parent : d.attr;
			if (at == null) {
				continue;
			}
			if (!at.hasChildren()) {
				if (at.getParent() == null) {
					continue;
				}
				Object parVal = at.getParamValue(tPar.getName());
				if (parVal == null || !(parVal instanceof TimeMoment)) {
					continue;
				}
				d = new VisAttrDescriptor();
				d.attr = at;
				d.parent = at.getParent();
				d.isTimeDependent = true;
				int npar = at.getParameterCount();
				if (npar > 1) {
					d.fixedParams = new Vector(npar, 1);
					d.fixedParamVals = new Vector(npar, 1);
					for (int j = 0; j < npar; j++) {
						String pname = at.getParamName(j);
						if (!pname.equals(tPar.getName())) {
							d.fixedParams.addElement(pname);
							d.fixedParamVals.addElement(at.getParamValue(j));
						}
					}
				}
				at = d.parent;
			}
			IntArray colNs = new IntArray(tPar.getValueCount(), 1);
			int nsub = 0;
			for (int k = 0; k < tPar.getValueCount(); k++) {
				colNs.addElement(-1);
				for (int n = 0; n < at.getChildrenCount(); n++) {
					Attribute child = d.parent.getChild(n);
					if (child.hasParamValue(tPar.getName(), tPar.getValue(k))) {
						boolean ok = true;
						if (d.fixedParams != null) {
							for (int j = 0; j < d.fixedParams.size() && ok; j++) {
								ok = child.hasParamValue((String) d.fixedParams.elementAt(j), d.fixedParamVals.elementAt(j));
							}
						}
						if (ok) {
							int idx = table.getAttrIndex(child.getIdentifier());
							if (idx >= 0) {
								colNs.setElementAt(idx, k);
								++nsub;
							}
							break;
						}
					}
				}
			}
			if (nsub < 1) {
				colNs = null;
			} else {
				if (timeAttrColNs == null) {
					timeAttrColNs = new Vector(aDescr.size(), 1);
				}
				boolean added = false;
				for (int j = 0; j < colNs.size(); j++) {
					int k = colNs.elementAt(j);
					if (k >= 0 && transColNs[k] < 0) {
						if (!added) {
							timeAttrColNs.addElement(colNs);
							added = true;
						}
						transColList.addElement(k);
						transColNs[k] = transColList.size() - 1;
						idxTimeAttrColNs[k] = timeAttrColNs.size() - 1;
					}
				}
			}
		}
		if (timeAttrColNs == null || timeAttrColNs.size() < 1) {
			transColNs = null;
			idxTimeAttrColNs = null;
			transColList = null;
			return; //nothing is transformed
		}
		if (nextTrans != null)
			if (nextTrans instanceof TimeAttrTransformer) {
				((TimeAttrTransformer) nextTrans).setAttributeDescriptions(aDescr);
			} else {
				nextTrans.setColumnNumbers(transColList);
			}
	}

	/**
	* Informs whether the transformer has all necessary settings for the
	* transformation and whether the data given to it can be transformed.
	*/
	@Override
	public boolean isValid() {
		return table != null && tPar != null && transColList != null && transColList.size() > 0;
	}

	/**
	* Returns its temporal parameter
	*/
	public Parameter getTemporalParameter() {
		return tPar;
	}
}