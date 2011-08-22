package spade.analysis.transform;

import java.awt.Component;
import java.util.Hashtable;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.lib.lang.Language;
import spade.lib.util.DoubleArray;
import spade.lib.util.NumValManager;
import spade.vis.database.Attribute;

/**
* Transforms numeric attributes using some arithmetical operations and functions
*/
public class MathTransformer extends BaseAttributeTransformer {
	static ResourceBundle res = Language.getTextResource("spade.analysis.transform.Res");
	/**
	* Possible transformation modes
	*/
	public static final int TR_NONE = 0, TR_LOG = 1, TR_LOG10 = 2, TR_ORDER_ASC = 3, TR_ORDER_DESC = 4, TR_ZSCORE = 5, TR_FIRST = TR_NONE, TR_LAST = TR_ZSCORE; //modify this when more modes are added!!!
	/**
	* The same modes as strings (to be used for saving and restoring states)
	*/
	public static final String trModeNames[] = { "none", "log", "log10", "order_ascend", "order_descend", "z_score" };
	/**
	* Current transformation mode
	*/
	protected int currMode = TR_NONE;
	/**
	* In the mode when each attribute is transformed individually, this array
	* defines the transformation mode for each attribute.
	*/
	protected int trModes[] = null;

	/**
	* Returns the identifier (unique name) of this transformation method.
	*/
	@Override
	public String getMethodName() {
		return "arithmetic";
	}

	/**
	* Returns the parameters (properties) of this transformation method so that
	* the state of the transformer may be completely restored.
	*/
	@Override
	public Hashtable getProperties() {
		int mode = currMode;
		if (transformIndividually) {
			mode = TR_NONE;
			if (trModes != null) {
				for (int i = 0; i < trModes.length && mode == TR_NONE; i++) {
					mode = trModes[i];
				}
			}
		}
		if (mode == TR_NONE)
			return null;
		Hashtable prop = super.getProperties();
		if (prop == null) {
			prop = new Hashtable();
		}
		if (!transformIndividually) {
			prop.put("mode", trModeNames[currMode]);
		} else {
			prop.put("mode_count", String.valueOf(trModes.length));
			for (int i = 0; i < trModes.length; i++) {
				prop.put("mode" + i, trModeNames[trModes[i]]);
			}
		}
		return prop;
	}

	/**
	* Sets the parameters (properties) of this transformation method.
	*/
	@Override
	public void setProperties(Hashtable prop) {
		if (prop == null)
			return;
		Object obj = prop.get("mode");
		if (obj != null) {
			String modeStr = obj.toString();
			for (int i = 0; i < trModeNames.length; i++)
				if (modeStr.equalsIgnoreCase(trModeNames[i])) {
					currMode = i;
					transformIndividually = false;
					break;
				}
		}
		obj = prop.get("mode_count");
		if (obj != null) {
			int k = 0;
			try {
				k = Integer.valueOf(obj.toString()).intValue();
			} catch (NumberFormatException e) {
			}
			if (k > 0) {
				transformIndividually = true;
				if (trModes == null || trModes.length < k) {
					trModes = new int[k];
				}
				for (int i = 0; i < k; i++) {
					trModes[i] = TR_NONE;
					obj = prop.get("mode" + i);
					if (obj != null) {
						String modeStr = obj.toString();
						for (int j = 0; j < trModeNames.length; j++)
							if (modeStr.equalsIgnoreCase(trModeNames[j])) {
								trModes[i] = j;
								break;
							}
					}
				}
			}
		}
		super.setProperties(prop);
	}

	/**
	 * Returns the description of the data transformation made. The description may be
	 * shown to the user. If no transformation has been made, returns null.
	 */
	@Override
	public String getDescription() {
		int mode = currMode;
		if (transformIndividually) {
			mode = TR_NONE;
			if (trModes != null) {
				for (int i = 0; i < trModes.length && mode == TR_NONE; i++) {
					mode = trModes[i];
				}
			}
		}
		if (mode == TR_NONE) {
			if (nextTrans != null)
				return nextTrans.getDescription();
			return null;
		}
		if (!transformIndividually)
			return MathTransformUI.modeNames[currMode];
		String descr = "";
		for (int i = 0; i < trModes.length; i++)
			if (trModes[i] != TR_NONE) {
				if (descr.length() > 0) {
					descr += "; ";
				}
				descr += MathTransformUI.modeNames[trModes[i]] + " " + res.getString("of") + " " + table.getAttributeName(transColList.elementAt(i));
			}
		return descr;
	}

	/**
	* The transformer is given a list of attribute identifiers to be transformed.
	* Possibly, not all attributes can be transformed by a specific Transformer.
	*/
	@Override
	public void setAttributes(Vector attrIds) {
		if (attrIds == null || attrIds.size() < 1)
			return;
		setColumnNumbers(table.getRelevantColumnNumbers(attrIds));
	}

	/**
	* Informs whether the transformer has all necessary settings for the
	* transformation and whether the data given to it can be transformed.
	*/
	@Override
	public boolean isValid() {
		return table != null && transColList != null && transColList.size() > 0;
	}

	/**
	* Sets the current mode of data transformation common for all attributes.
	* Does not automatically do the transformation! Call doTransformation()
	* explicitly for the changes of the settings could actually have an effect.
	*/
	public void setTransMode(int mode) {
		if (mode != currMode && mode >= TR_FIRST && mode <= TR_LAST) {
			currMode = mode;
		}
	}

	/**
	* Returns the current mode of data transformation common for all attributes.
	*/
	public int getTransMode() {
		return currMode;
	}

	/**
	* Creates the array trModes with individual transformation modes for each
	* attribute.
	*/
	protected void checkCreateModeArray() {
		if (trModes == null || trModes.length < transColList.size()) {
			int trm[] = trModes;
			trModes = new int[transColList.size()];
			for (int i = 0; i < trModes.length; i++) {
				trModes[i] = currMode;
			}
			if (trm != null) {
				for (int i = 0; i < trm.length; i++) {
					trModes[i] = trm[i];
				}
			}
		}
	}

	/**
	* If individual transformation is allowed, this method sets the current mode
	* of data transformation for the attribute with the given identifier. The
	* identifier may, in particular, belong to a super-attribute. For all
	* attributes with the same parent, the transformation mode must be the same.
	* Does not automatically do the transformation! Call doTransformation()
	* explicitly for the changes of the settings could actually have an effect.
	*/
	public void setTransMode(int mode, String attrId) {
		if (mode < TR_FIRST || mode > TR_LAST || attrId == null)
			return;
		checkCreateModeArray();
		for (int i = 0; i < transColList.size(); i++) {
			Attribute at = table.getAttribute(transColList.elementAt(i));
			if (at == null) {
				continue;
			}
			if (at.getIdentifier().equals(attrId)) {
				trModes[i] = mode;
			} else {
				at = at.getParent();
				if (at != null && at.getIdentifier().equals(attrId)) {
					trModes[i] = mode;
				}
			}
		}
	}

	/**
	* If individual transformation is allowed, this method returns the current mode
	* of data transformation for the attribute with the given identifier. The
	* identifier may, in particular, belong to a super-attribute. For all
	* attributes with the same parent, the transformation mode must be the same.
	*/
	public int getTransMode(String attrId) {
		if (trModes == null || attrId == null || transColList == null)
			return currMode;
		checkCreateModeArray();
		for (int i = 0; i < transColList.size(); i++) {
			Attribute at = table.getAttribute(transColList.elementAt(i));
			if (at == null) {
				continue;
			}
			if (at.getIdentifier().equals(attrId))
				return trModes[i];
			at = at.getParent();
			if (at == null) {
				continue;
			}
			if (at.getIdentifier().equals(attrId))
				return trModes[i];
		}
		return currMode;
	}

	/**
	* Performs the necessary data transformation and puts the results in the
	* array data.
	*/
	@Override
	public void doTransformation() {
		if (table == null || !table.hasData() || transColList == null || transColList.size() < 1)
			return;
		if (data == null || data.length != table.getDataItemCount() || data[0].length != transColList.size()) {
			data = new double[table.getDataItemCount()][transColList.size()];
		}
		if (transformIndividually) {
			checkCreateModeArray();
		}
		for (int i = 0; i < transColList.size(); i++) {
			int nc = transColList.elementAt(i);
			int mode = (transformIndividually && trModes != null) ? trModes[i] : currMode;
			if (mode == TR_ZSCORE) {
				DoubleArray fa = new DoubleArray(table.getDataItemCount(), 10);
				for (int nr = 0; nr < table.getDataItemCount(); nr++) {
					fa.addElement(getOrigAttrValue(nc, nr));
				}
				double mean = NumValManager.getMean(fa), stdd = NumValManager.getStdD(fa, mean);
				if (stdd == 0f) {
					for (int nr = 0; nr < table.getDataItemCount(); nr++) {
						data[nr][i] = Double.NaN;
					}
				} else {
					for (int nr = 0; nr < table.getDataItemCount(); nr++) {
						data[nr][i] = (getOrigAttrValue(nc, nr) - mean) / stdd;
					}
				}
			} else if (mode == TR_ORDER_ASC || mode == TR_ORDER_DESC) {
				double vals[] = new double[table.getDataItemCount()];
				for (int nr = 0; nr < table.getDataItemCount(); nr++) {
					vals[nr] = getOrigAttrValue(nc, nr);
				}
				int order[] = (mode == TR_ORDER_ASC) ? NumValManager.getOrderIncrease(vals) : NumValManager.getOrderDecrease(vals);
				for (int nr = 0; nr < table.getDataItemCount(); nr++) {
					data[nr][i] = (order[nr] == -1) ? Double.NaN : order[nr];
				}
			} else {
				for (int nr = 0; nr < table.getDataItemCount(); nr++) {
					data[nr][i] = Double.NaN;
					double val = getOrigAttrValue(nc, nr);
					if (Double.isNaN(val)) {
						continue;
					}
					switch (mode) {
					case TR_NONE:
						data[nr][i] = val;
						break;
					case TR_LOG:
						if (val > 0.0f) {
							data[nr][i] = Math.log(val);
						}
						break;
					case TR_LOG10:
						if (val > 0.0f) {
							data[nr][i] = Math.log(val) / Math.log(10d);
						}
						break;
					}
				}
			}
		}
		if (nextTrans != null) {
			nextTrans.doTransformation();
		} else {
			notifyValuesChange();
		}
	}

	/**
	* Returns the string representation of the transformed value corresponding to
	* the given row and column of the original table. If the value is not
	* transformed, returns the value returned by the next transformer or null.
	*/
	@Override
	public String getTransformedValueAsString(int rowN, int colN) {
		if (rowN < 0 || colN < 0)
			return null;
		String val = null;
		if (nextTrans != null) {
			val = nextTrans.getTransformedValueAsString(rowN, colN);
		}
		int mode = currMode;
		if (transformIndividually) {
			checkCreateModeArray();
		}
		if (transformIndividually && trModes != null && transColNs != null && colN < transColNs.length && transColNs[colN] >= 0) {
			mode = trModes[transColNs[colN]];
		}
		if (mode == TR_NONE)
			return val;
		if (data == null || rowN >= data.length || transColNs == null || colN >= transColNs.length || transColNs[colN] < 0)
			return val;
		if (Double.isNaN(data[rowN][transColNs[colN]]))
			return null;
		String str = String.valueOf(data[rowN][transColNs[colN]]);
		if (val == null)
			return str;
		return str + " >> " + val;
	}

	/**
	* Returns the UI for setting the transformation parameters in this transformer
	*/
	@Override
	public Component getIndividualUI() {
		return new MathTransformUI(this);
	}
}
