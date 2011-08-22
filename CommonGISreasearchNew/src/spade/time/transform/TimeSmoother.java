package spade.time.transform;

import java.awt.Component;
import java.util.Hashtable;
import java.util.ResourceBundle;

import spade.lib.lang.Language;
import spade.lib.util.IntArray;
import spade.lib.util.NumValManager;
import spade.lib.util.SmoothingParams;
import spade.time.TimeMoment;
import spade.vis.database.ThematicDataItem;

/**
* Applies smoothing to values of a time-dependent attribute, i.e. averaging on
* the specified interval
*/
public class TimeSmoother extends TimeAttrTransformer {
	static ResourceBundle res = Language.getTextResource("spade.time.transform.Res");
	/**
	* Smoothing parameters: operation, depth, etc.
	*/
	protected SmoothingParams spar = new SmoothingParams();
	/**
	* The string representation of the aggregation start time moment in the
	* mode of cumulative aggregation starting from a fixed moment. Used
	* additionally to the index of the value of the temporal parameter
	* (@see SmoothingParams.smoothStartIdx) because the moment may be
	* specified before a reference to the table is set (e.g. when restoring
	* previously saved transformers).
	*/
	protected String aggStartTime = null;

	/**
	* Returns the identifier (unique name) of this transformation method.
	*/
	@Override
	public String getMethodName() {
		return "time_smooth";
	}

	/**
	* Returns the parameters (properties) of this transformation method so that
	* the state of the transformer may be completely restored.
	*/
	@Override
	public Hashtable getProperties() {
		if (spar.smoothMode == SmoothingParams.SmoothNONE)
			return null;
		Hashtable prop = super.getProperties();
		if (prop == null) {
			prop = new Hashtable();
		}
		prop.put("mode", SmoothingParams.smoothModeNames[spar.smoothMode]);
		prop.put("residuals", String.valueOf(spar.smoothDifference));
		if (spar.smoothDepth > 0) {
			prop.put("depth", String.valueOf(spar.smoothDepth));
			prop.put("centred", String.valueOf(spar.smoothCentered));
		} else {
			TimeMoment t = getAggStartMoment();
			if (t != null) {
				prop.put("start_from", t.toString());
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
			for (int i = 0; i < SmoothingParams.smoothModeNames.length; i++)
				if (modeStr.equalsIgnoreCase(SmoothingParams.smoothModeNames[i])) {
					spar.smoothMode = i;
					break;
				}
		}
		obj = prop.get("residuals");
		if (obj != null) {
			spar.smoothDifference = Boolean.valueOf(obj.toString()).booleanValue();
		}
		obj = prop.get("centred");
		if (obj != null) {
			spar.smoothCentered = Boolean.valueOf(obj.toString()).booleanValue();
		}
		obj = prop.get("depth");
		if (obj != null) {
			try {
				spar.smoothDepth = Integer.valueOf(obj.toString()).intValue();
			} catch (NumberFormatException e) {
			}
		}
		obj = prop.get("start_from");
		if (obj != null) {
			aggStartTime = obj.toString();
			spar.smoothDepth = 0;
		}
		super.setProperties(prop);
	}

	/**
	 * Returns the description of the data transformation made. The description may be
	 * shown to the user. If no transformation has been made, returns null.
	 */
	@Override
	public String getDescription() {
		if (spar.smoothMode == SmoothingParams.SmoothNONE) {
			if (nextTrans != null)
				return nextTrans.getDescription();
			return null;
		}
		String descr = ((spar.smoothDifference) ? res.getString("residuals_of_smoothing") : res.getString("smoothing")) + " (";
		switch (spar.smoothMode) {
		case SmoothingParams.SmoothAVG:
			descr += res.getString("mean");
			break;
		case SmoothingParams.SmoothMEDIAN:
			descr += res.getString("median");
			break;
		case SmoothingParams.SmoothMAX:
			descr += res.getString("maximum");
			break;
		case SmoothingParams.SmoothMIN:
			descr += res.getString("minimum");
			break;
		case SmoothingParams.SmoothMAXMIN:
			descr += res.getString("maximum-minimum");
			break;
		case SmoothingParams.SmoothSUM:
			descr += res.getString("sum");
			break;
		}
		if (spar.smoothDepth == 0) {
			descr += " " + res.getString("starting_from") + " " + getAggStartMoment();
		} else {
			descr += " " + res.getString("over") + " " + spar.smoothDepth + " " + res.getString("moments") + " " + ((spar.smoothCentered) ? res.getString("centered") : res.getString("backwards"));
		}
		descr += ")";
		if (nextTrans != null) {
			String d1 = nextTrans.getDescription();
			if (d1 != null) {
				descr += "; " + d1;
			}
		}
		return descr;
	}

	/**
	* Returns its smoothing parameters
	*/
	public SmoothingParams getSmoothingParams() {
		return spar;
	}

	/**
	* Sets the new smoothing parameters and recomputes the results
	*/
	public void setSmothingParams(SmoothingParams spar) {
		if (spar != null) {
			this.spar = spar;
			doTransformation();
		}
	}

	/**
	* In the mode of cumulative aggregation starting from a fixed time moment,
	* returns the starting moment of the aggregation.
	*/
	public TimeMoment getAggStartMoment() {
		if (tPar == null)
			return null;
		if (spar.smoothStartIdx >= 0 && spar.smoothStartIdx < tPar.getValueCount())
			return (TimeMoment) tPar.getValue(spar.smoothStartIdx);
		if (aggStartTime != null && setAggStartMoment(aggStartTime))
			return (TimeMoment) tPar.getValue(spar.smoothStartIdx);
		return null;
	}

	/**
	* Tries to set the starting moment for cumulative aggregation from the
	* given string representation. Returns true if successful.
	*/
	public boolean setAggStartMoment(String momentStr) {
		if (tPar == null || tPar.getFirstValue() == null)
			return false;
		TimeMoment tm = ((TimeMoment) tPar.getValue(0)).getCopy();
		int k = -1;
		if (tm.setMoment(momentStr)) {
			for (int i = 0; i < tPar.getValueCount() && k < 0; i++)
				if (tPar.getValue(i).equals(tm)) {
					k = i;
				}
		}
		if (k >= 0) {
			spar.smoothStartIdx = k;
		}
		return k >= 0;
	}

	/**
	* Performs the necessary data transformation and puts the results in the
	* internal array.
	*/
	@Override
	public void doTransformation() {
		if (table == null || !table.hasData() || transColList == null || transColList.size() < 1)
			return;
		if (data == null || data.length != table.getDataItemCount() || data[0].length != transColList.size()) {
			data = new double[table.getDataItemCount()][transColList.size()];
		}
		if (spar.smoothMode != SmoothingParams.SmoothNONE) {
			if (spar.smoothDepth == 0 && spar.smoothStartIdx < 0) {
				if (aggStartTime != null) {
					setAggStartMoment(aggStartTime);
				}
				if (spar.smoothStartIdx < 0) {
					spar.smoothMode = SmoothingParams.SmoothNONE;
				}
			}
			for (int k = 0; k < timeAttrColNs.size(); k++) {
				IntArray colNs = (IntArray) timeAttrColNs.elementAt(k);
				double values[] = new double[colNs.size()];
				for (int nr = 0; nr < table.getDataItemCount(); nr++) {
					ThematicDataItem rec = (ThematicDataItem) table.getDataItem(nr);
					for (int i = 0; i < colNs.size(); i++) {
						values[i] = getOrigAttrValue(colNs.elementAt(i), rec);
					}
					if (spar.smoothDepth == 0) {
						values = NumValManager.smoothWithAccumulation(values, spar.smoothMode, spar.smoothStartIdx);
					} else if (spar.smoothCentered) {
						values = NumValManager.smoothCentered(values, spar.smoothMode, spar.smoothDepth);
					} else {
						values = NumValManager.smoothBack(values, spar.smoothMode, spar.smoothDepth);
					}
					for (int i = 0; i < colNs.size(); i++) {
						if (!Double.isNaN(values[i]) && spar.smoothDifference) {
							values[i] = getOrigAttrValue(colNs.elementAt(i), rec) - values[i];
						}
						if (colNs.elementAt(i) >= 0) {
							data[nr][transColNs[colNs.elementAt(i)]] = values[i];
						}
					}
				}
			}
			//if there are some extra attributes which do not undergo this
			//transformation, put the original values in the corresponding matrix columns
			for (int i = 0; i < transColList.size(); i++) {
				int nc = transColList.elementAt(i);
				if (idxTimeAttrColNs[nc] < 0) {
					for (int nr = 0; nr < table.getDataItemCount(); nr++) {
						ThematicDataItem rec = (ThematicDataItem) table.getDataItem(nr);
						data[nr][i] = getOrigAttrValue(nc, rec);
					}
				}
			}
		} else {
			for (int nr = 0; nr < table.getDataItemCount(); nr++) {
				ThematicDataItem rec = (ThematicDataItem) table.getDataItem(nr);
				for (int i = 0; i < transColList.size(); i++) {
					int nc = transColList.elementAt(i);
					data[nr][i] = getOrigAttrValue(nc, rec);
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
		if (spar.smoothMode == SmoothingParams.SmoothNONE)
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
		return new TimeSmoothUI(this);
	}
}
