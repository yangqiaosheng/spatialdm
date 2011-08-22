package spade.analysis.calc;

import java.util.Vector;

import spade.analysis.datamanage.DataFilter;
import spade.lib.util.FloatArray;
import spade.lib.util.NumValManager;
import spade.vis.database.AttributeDataPortion;

public class CorrelationDataContainer implements PairDataContainer {

	protected AttributeDataPortion dataTable = null;
	protected Vector attributes = null;
	protected Vector selectedDistricts = null;
	protected DataFilter dataFilter = new DataFilter();

	protected Vector valueTable = null;

	public CorrelationDataContainer(AttributeDataPortion dt, Vector attr, Vector districtIDs) {

		dataTable = dt;
		attributes = attr;
		selectedDistricts = districtIDs;
		dataFilter.setTable(dataTable);

		calculate(attributes, selectedDistricts);

	}

	public void setDataTable(AttributeDataPortion dt) {
		dataTable = dt;
	}

	public void setAttributes(Vector attrIDs) {
		attributes = attrIDs;
	}

	public void setSelectedDistricts(Vector districtIDs) {
		selectedDistricts = districtIDs;
	}

	@Override
	public float getValue(int attrIdx1, int attrIdx2) {

		int i = 0;
		int j = 0;

		for (int k = 0; k < attributes.size(); k++) {
			//Attribute a = dataTable.getAttribute((String)attributes.elementAt(k));
			//String id = a.getIdentifier();
			//int idx = dataTable.getAttrIndex(id);
			int idx = dataTable.getAttrIndex((String) attributes.elementAt(k));

			if (idx == attrIdx1) {
				i = k;
			}

			if (idx == attrIdx2) {
				j = k;
			}

		}
		int index = 0;
		if (j <= i) {
			index = ((i * (i + 1)) / 2) + j;
		} else {
			index = ((j * (j + 1)) / 2) + i;
		}
		Float floatObject = (Float) valueTable.elementAt(index);
		float res = floatObject.floatValue();
		return res;

	}

	public Vector getValueRow(int index) {
		Vector valueRow = new Vector();
		int[] indeces = dataTable.getAttrIndices(attributes);
		for (int i = 0; i < attributes.size(); i++) {
			float correlationValue = getValue(index, indeces[i]);
			valueRow.addElement(new Float(correlationValue));
		}
		return valueRow;
	}

	public void calculate(Vector attr, Vector districtIDs) {

		attributes = attr;
		selectedDistricts = districtIDs;
		valueTable = new Vector();

		dataFilter.setRegion(selectedDistricts);
		Vector attributeValues = new Vector();
		for (int i = 0; i < attributes.size(); i++) {
			int idx = dataTable.getAttrIndex((String) attributes.elementAt(i));
			Vector v = dataFilter.filter(idx);
			attributeValues.addElement(v.clone());
		}
		dataFilter.clearRegions();

		Vector floatArrays = new Vector();

		for (int i = 0; i < attributeValues.size(); i++) {
			Vector v = (Vector) attributeValues.elementAt(i);
			FloatArray floatArray = new FloatArray();
			for (int j = 0; j < v.size(); j++) {
				Float floatObject = (Float) v.elementAt(j);
				float value = floatObject.floatValue();
				floatArray.addElement(value);
			}
			floatArrays.addElement(floatArray);
		}

		FloatArray standardDevValues = new FloatArray();
		FloatArray meanValues = new FloatArray();

		for (int i = 0; i < floatArrays.size(); i++) {
			FloatArray floatArray = (FloatArray) floatArrays.elementAt(i);
			float mean = NumValManager.getMean(floatArray);
			meanValues.addElement(mean);
			standardDevValues.addElement(NumValManager.getStdD(floatArray, mean));
		}

		for (int i = 0; i < floatArrays.size(); i++) {
			FloatArray fi = (FloatArray) floatArrays.elementAt(i);
			float iMean = meanValues.elementAt(i);
			float iStdDev = standardDevValues.elementAt(i);
			for (int j = 0; j < i + 1; j++) {
				FloatArray fj = (FloatArray) floatArrays.elementAt(j);
				float jMean = meanValues.elementAt(j);
				float jStdDev = standardDevValues.elementAt(j);
				int N = (fi.size() < fj.size()) ? fi.size() : fj.size();
				float covar = 0f;
				int valids = 0;
				for (int k = 0; k < N; k++) {
					if (Float.isNaN(fi.elementAt(k)) || Float.isNaN(fj.elementAt(k))) {
						continue;
					}
					covar += (fi.elementAt(k) - iMean) * (fj.elementAt(k) - jMean);
					valids++;
				}
				covar /= valids;
				//covar = NumValManager.getCovariance(fi, fj);
				float correlationValue = covar / (iStdDev * jStdDev);
				//float correlationValue = NumValManager.getCorrelation(fi,fj);
				correlationValue = (correlationValue > 1) ? 1f : correlationValue;
				correlationValue = (correlationValue < -1) ? -1f : correlationValue;
				correlationValue = roundFloat(correlationValue, 4);
				valueTable.addElement(new Float(correlationValue));
			}
		}

	}

	protected float roundFloat(float value, int exponent) {
		float erg = 0f;
		float factor = (float) Math.pow(10, exponent);
		float temp = Math.round(value * factor);
		erg = temp / factor;
		return erg;
	}

}
