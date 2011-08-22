package spade.vis.preference;

import java.awt.Image;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.StringTokenizer;
import java.util.Vector;

import spade.lib.util.IdUtil;
import spade.lib.util.StringUtil;
import spade.vis.geometry.GeomSign;

/**
* Specifies correspondences between values of a qualitative attribute or
* combinations of values of several attributes and icons (images) to be used
* for representation of these values (or combinations) on a map.
* Each correspondence is specified by an instance of IconVisSpec.
*/
public class IconCorrespondence implements VisPreference {
	/**
	* The identifiers of the attributes for which this correspondence
	* specification is relevant.
	* Stores "pure" rather than "extended" attribute identifiers, i.e. attribute
	* identifiers without table identifiers attached.
	*/
	protected Vector attrIds = null;
	/**
	* This vector consists of instances of IconVisSpec. Each instance specifies
	* a correspondence between a value or a combination of values and an icon
	* to be used for representation of this value or combination.
	*/
	protected Vector corr = null;

	/**
	* Returns its copy (the internal variables are also cloned)
	*/
	@Override
	public Object clone() {
		IconCorrespondence icorr = new IconCorrespondence();
		if (attrIds != null && attrIds.size() > 0) {
			icorr.setAttributes((Vector) attrIds.clone());
		}
		if (corr != null && corr.size() > 0) {
			for (int i = 0; i < corr.size(); i++) {
				icorr.addCorrespondence((IconVisSpec) corr.elementAt(i));
			}
		}
		return icorr;
	}

	/**
	* Sets the identifiers of the attributes for which this correspondence
	* specification is relevant.
	*/
	public void setAttributes(Vector attrIds) {
		this.attrIds = IdUtil.getPureAttrIds(attrIds);
	}

	/**
	* Returns the identifiers of the attributes for which this correspondence
	* specification is relevant.
	*/
	public Vector getAttributes() {
		return attrIds;
	}

	/**
	* Checks if the specification is relevant for the given attribute.
	*/
	public boolean isRelevant(String attrId) {
		if (attrId == null || attrIds == null)
			return false;
		return StringUtil.isStringInVectorIgnoreCase(IdUtil.getPureAttrId(attrId), attrIds);
	}

	/**
	* Checks if the specification is relevant for the given set of attributes.
	*/
	public boolean isRelevant(Vector aIds) {
		if (aIds == null || attrIds == null || aIds.size() != attrIds.size())
			return false;
		for (int i = 0; i < aIds.size(); i++)
			if (!isRelevant((String) aIds.elementAt(i)))
				return false;
		return true;
	}

	/**
	* Adds a correspondence between a value or a combination of values and an icon
	* to be used for representation of this value or combination.
	*/
	public void addCorrespondence(IconVisSpec spec) {
		if (spec == null)
			return;
		if (corr == null) {
			corr = new Vector(10, 10);
		}
		corr.addElement(spec);
	}

	/**
	* Returns the number of correspondences specified
	*/
	public int getCorrCount() {
		if (corr == null)
			return 0;
		return corr.size();
	}

	/**
	* Returns the correspondence with the given index
	*/
	public IconVisSpec getCorrespondence(int idx) {
		if (idx < 0 || idx > getCorrCount())
			return null;
		return (IconVisSpec) corr.elementAt(idx);
	}

	/**
	* Removes the correspondence with the given index
	*/
	public void removeCorrespondence(int idx) {
		if (idx < 0 || idx > getCorrCount())
			return;
		corr.removeElementAt(idx);
	}

	/**
	* Checks if there is a correspondence for the specified set of attribute-value
	* pairs. Each pair is a two-string array. If such a correspondence exists,
	* returns its index, otherwise returns -1.
	*/
	public int findCorrespondence(Vector pairs) {
		if (pairs == null || corr == null)
			return -1;
		for (int i = 0; i < pairs.size(); i++) {
			String p[] = (String[]) pairs.elementAt(i);
			if (!isRelevant(p[0]))
				return -1;
		}
		for (int i = 0; i < corr.size(); i++) {
			IconVisSpec spec = getCorrespondence(i);
			if (!spec.isDefault() && spec.getPairCount() == pairs.size()) {
				boolean ok = true;
				for (int j = 0; j < pairs.size() && ok; j++) {
					String p[] = (String[]) pairs.elementAt(j);
					ok = spec.hasPair(p[0], p[1]);
				}
				if (ok)
					return i;
			}
		}
		return -1;
	}

	/**
	* Checks if there is a correspondence for the specified set of attribute-value
	* pairs. Attribute identifiers and values are specified in two vectors.
	* If such a correspondence exists, returns its index, otherwise returns -1.
	*/
	public int findCorrespondence(Vector attrIds, Vector values) {
		if (attrIds == null || values == null || corr == null)
			return -1;
		attrIds = IdUtil.getPureAttrIds(attrIds);
		for (int i = 0; i < attrIds.size(); i++)
			if (!isRelevant((String) attrIds.elementAt(i)))
				return -1;
		for (int i = 0; i < corr.size(); i++) {
			IconVisSpec spec = getCorrespondence(i);
			if (!spec.isDefault() && spec.getPairCount() == attrIds.size()) {
				boolean ok = true;
				for (int j = 0; j < attrIds.size() && ok; j++) {
					ok = spec.hasPair((String) attrIds.elementAt(j), (String) values.elementAt(j));
				}
				if (ok)
					return i;
			}
		}
		return -1;
	}

	/**
	* Returns the default correspondence, if exists.
	*/
	public IconVisSpec getDefaultCorrespondence() {
		for (int i = 0; i < getCorrCount(); i++) {
			IconVisSpec spec = getCorrespondence(i);
			if (spec.isDefault())
				return spec;
		}
		return null;
	}

	/**
	* Returns the closest correspondence for the specified set of attribute values.
	* "Closest" means that, if there is no exact correspondence, the last
	* attribute is ignored, and a correspondence for the remaining attributes is
	* looked for, and so on. If no closest correspondence found, the default
	* correspondence is returned (if exists).
	*/
	public IconVisSpec getClosestCorrespondence(Vector attrIds, Vector values) {
		if (getCorrCount() < 1)
			return null;
		if (attrIds == null || values == null)
			return getDefaultCorrespondence();
		attrIds = IdUtil.getPureAttrIds(attrIds);
		for (int i = 0; i < attrIds.size(); i++)
			if (!isRelevant((String) attrIds.elementAt(i)))
				return null;
		IconVisSpec selSp = null;
		int nNullValues = attrIds.size();
		for (int i = 0; i < corr.size(); i++) {
			IconVisSpec spec = getCorrespondence(i);
			if (!spec.isDefault()) {
				boolean ok = true;
				int nNulls = 0;
				for (int j = 0; j < attrIds.size() && ok; j++)
					if (!spec.hasPair((String) attrIds.elementAt(j), (String) values.elementAt(j)))
						if (spec.hasPair((String) attrIds.elementAt(j), null)) {
							++nNulls;
						} else {
							ok = false;
						}
				if (!ok) {
					continue;
				}
				if (nNulls == 0)
					return spec;
				if (nNulls < nNullValues) {
					selSp = spec;
					nNullValues = nNulls;
				}
			}
		}
		if (selSp != null)
			return selSp;
		return getDefaultCorrespondence();
	}

	/**
	* Writes itself or its settings to the given DataOutputStream.
	*/
	@Override
	public void write(DataOutputStream out) throws IOException {
		if (getCorrCount() < 1)
			return;
		if (attrIds == null || attrIds.size() < 1)
			return;
		out.writeBytes("ATTRIBUTES=");
		for (int i = 0; i < attrIds.size(); i++) {
			if (i > 0) {
				out.write(';');
			}
			out.writeBytes("\"" + (String) attrIds.elementAt(i) + "\"");
		}
		out.write('\n');
		for (int i = 0; i < getCorrCount(); i++) {
			IconVisSpec isp = getCorrespondence(i);
			if (isp.getIcon() == null)
				if (isp.getPathToImage() == null) {
					out.writeBytes("_NONE_=");
				} else {
					out.writeBytes("\"" + isp.getPathToImage() + "\"=");
				}
			else if (isp.getIcon() instanceof Image)
				if (isp.getPathToImage() == null) {
					continue;
				} else {
					out.writeBytes("\"" + isp.getPathToImage() + "\"=");
				}
			else if (isp.getIcon() instanceof GeomSign) {
				GeomSign sign = (GeomSign) isp.getIcon();
				out.writeBytes(sign.toString() + "=");
			} else {
				continue; //unknown icon type
			}
			if (isp.isDefault()) {
				out.writeBytes("_default_\n");
			} else {
				String txt = "";
				for (int j = 0; j < attrIds.size(); j++) {
					if (isp.hasAttribute((String) attrIds.elementAt(j))) {
						String val = isp.getAttrValue((String) attrIds.elementAt(j));
						if (val == null) {
							val = "_any_";
						}
						if (txt.length() > 0) {
							txt += ";";
						}
						txt += "\"" + val + "\"";
					}
				}
				out.writeBytes(txt + "\n");
			}
		}
		out.flush();
	}

	/**
	* Reads itself or its settings from the given BufferedReader. Returns true
	* if read successfully. The signal to stop reading is a line containing the
	* tag </preference>.
	*/
	@Override
	public boolean read(BufferedReader br) throws IOException {
		if (br == null)
			return false;
		attrIds = null;
		corr = null;
		boolean endFound = false;
		while (true) {
			String str = br.readLine();
			if (str == null) {
				break;
			}
			str = str.trim();
			if (str.length() < 1) {
				continue;
			}
			if (str.equalsIgnoreCase("</preference>")) {
				endFound = true;
				break;
			}
			StringTokenizer st = new StringTokenizer(str, "=");
			if (st.countTokens() < 2) {
				continue;
			}
			String w1 = st.nextToken().trim(), w2 = st.nextToken().trim();
			if (w1.equalsIgnoreCase("ATTRIBUTES")) {
				attrIds = StringUtil.getNames(w2);
			} else {
				if (attrIds == null || attrIds.size() < 1) {
					continue;
				}
				IconVisSpec isp = new IconVisSpec();
				if (w1.equals("_NONE_")) {
					isp.setIcon(null);
				} else if (w1.startsWith(GeomSign.getKey())) {
					isp.setIcon(GeomSign.valueOf(w1));
				} else {
					isp.setPathToImage(StringUtil.removeQuotes(w1));
				}
				if (w2.indexOf("_default_") < 0) {
					Vector values = StringUtil.getNames(w2);
					if (values != null && values.size() > 0) {
						for (int i = 0; i < attrIds.size(); i++) {
							String v = null;
							if (i < values.size()) {
								v = (String) values.elementAt(i);
								if (v.equals("_any_")) {
									v = null;
								}
							}
							isp.addAttrValuePair((String) attrIds.elementAt(i), v);
						}
					}
				}
				addCorrespondence(isp);
			}
		}
		return endFound;
	}
}
