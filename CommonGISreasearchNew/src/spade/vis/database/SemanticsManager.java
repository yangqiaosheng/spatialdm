package spade.vis.database;

import java.awt.Checkbox;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.awt.ScrollPane;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.util.Vector;

import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.OKDialog;
import spade.lib.basicwin.PopupManager;
import spade.lib.lang.Language;
import spade.lib.util.IdUtil;
import spade.lib.util.StringUtil;
import spade.vis.preference.VisPreference;
import spade.vis.spec.TagReader;
import spade.vis.spec.TagReaderFactory;

public class SemanticsManager implements java.io.Serializable {
	static ResourceBundle res = Language.getTextResource("spade.vis.database.Res");
	/**
	* The path to the file with the semantics (may be also a URL)
	*/
	protected String pathToSem = null;

	protected AttributeDataPortion dTable = null;

	public boolean questionsAllowed = true, warningsAllowed = true;

	/**
	* Vectors of Strings representing attributes with some properties
	*/
	protected Vector quantitativeAttrs = null, // list of quantitative attributes
			costCriteriaAttrs = null, // list of cost criteria
			benefitCriteriaAttrs = null; // list of benefit criteria

	/**
	* Vectors of vectors representing groups of comparable attributes
	*/
	protected Vector comparableGroups = null;
	/**
	* Vectors of vectors representing groups of attributes linked by inclusion
	* relationships. In each vector the first attribute (possibly, null) includes
	* all others.
	*/
	protected Vector inclusionGroups = null;
	/**
	* Preferences for cartographic representation of attribute values. The type
	* of each element of the vector is not restricted (for example, this may be
	* an instance of spade.vis.preferences.IconCorrespondence or something
	* else), but it must implement the interface spade.vis.preference.VisPreference.
	*/
	protected Vector presPref = null;
	/**
	* The correspondence between possible types of representation preferences
	* and the classes supporting them. The classes supporting
	* different preference types must implement the interface
	* spade.vis.preference.VisPreference.
	*/
	protected String prefTypeCorr[][] = { { "icons", "spade.vis.preference.IconCorrespondence" } };
	/**
	* Other descriptors, which can describe any aspects of data semantics.
	* Each descriptor has to implement the interface spase.vis.spec.TagReader.
	* In particular, there may be a TimeRefDescriptor that describes time
	* references contained in data.
	*/
	protected Vector descriptors = null;

	protected boolean wasAnythingChanged = false;

	public boolean getWasAnythingChanged() {
		return wasAnythingChanged;
	}

	public SemanticsManager(AttributeDataPortion table) {
		setTable(table);
	}

	public void setTable(AttributeDataPortion table) {
		this.dTable = table;
	}

	/**
	* Sets the path to the file with the semantics (may be also a URL)
	*/
	public void setPathToSemantics(String path) {
		pathToSem = path;
	}

	/**
	* Returns the path to the file with the semantics (may be also a URL)
	*/
	public String getPathToSemantics() {
		return pathToSem;
	}

	/**
	* Replies if any semantic information is available
	*/
	public boolean hasAnySemantics() {
		return (quantitativeAttrs != null && quantitativeAttrs.size() > 0) || (costCriteriaAttrs != null && costCriteriaAttrs.size() > 0) || (benefitCriteriaAttrs != null && benefitCriteriaAttrs.size() > 0)
				|| (comparableGroups != null && comparableGroups.size() > 0) || (inclusionGroups != null && inclusionGroups.size() > 0) || (presPref != null && presPref.size() > 0) || (descriptors != null && descriptors.size() > 0);
	}

	private static String qaString = "QuantitativeAttributes=", ccaString = "CostCriteriaAttributes=", bcaString = "BenefitCriteriaAttributes=", cgString = "ComparableGroup=", igString = "InclusionGroup=";

	/**
	* A utility method used to read "tags", i.e. sequences of lines starting
	* with <keyword ...> and ending with </keyword>. The first argument is
	* the first string of the tag (i.e. any string starting with "<"), the
	* second argument is the reader used to read the data.
	*/
	protected void readTag(String header, BufferedReader br) throws IOException {
		if (header == null || br == null || !header.startsWith("<"))
			return;
		String str = header.substring(1).toLowerCase();
		int k = str.indexOf('>');
		if (k > 0) {
			str = str.substring(0, k).trim();
		}
		k = str.indexOf(' ');
		if (k < 0) {
			k = str.length();
		}
		String key = str.substring(0, k);
		boolean ok = false;
		TagReader reader = TagReaderFactory.getTagReader(key);
		if (reader != null) {
			ok = reader.readDescription(header, br);
			if (ok) {
				if (descriptors == null) {
					descriptors = new Vector(5, 5);
				}
				descriptors.addElement(reader);
			}
		} else if (key.equals("preference")) { //this is a presentation preference
			//look for the preference type
			if (k < str.length()) {
				str = str.substring(k + 1).trim();
				if (str.startsWith("type")) {
					k = str.indexOf('=');
					if (k > 0) {
						String type = str.substring(k + 1).trim(), className = null;
						//find this type among the known preference types
						for (int i = 0; i < prefTypeCorr.length && className == null; i++)
							if (type.equalsIgnoreCase(prefTypeCorr[i][0])) {
								className = prefTypeCorr[i][1];
							}
						if (className != null) {
							Object obj = null;
							try {
								obj = Class.forName(className).newInstance();
							} catch (Exception e) {
							}
							if (obj != null && (obj instanceof VisPreference)) {
								VisPreference pref = (VisPreference) obj;
								ok = pref.read(br);
								if (ok) {
									addPreference(pref);
								}
							}
						}
					}
				}
			}
		}
		if (!ok) { //search for the end tag
			boolean end = false;
			do {
				str = br.readLine();
				if (str != null) {
					str = str.trim();
					if (str.length() < 1) {
						continue;
					}
					if (str.startsWith("</")) {//this the end tag
						str = str.toLowerCase();
						end = str.startsWith("</" + key);
					}
				}
			} while (!end && str != null);
		}
	}

	public void readSemanticsFromFile(BufferedReader br) {
		String str = null;
		try {
			do {
				str = br.readLine();
				if (str == null) {
					break;
				}
				str = str.trim();
				if (str.length() < 1) {
					continue;
				}
				if (str.startsWith("<")) {
					readTag(str, br);
				} else if (str.startsWith(qaString)) {
					if (quantitativeAttrs == null) {
						quantitativeAttrs = new Vector(10, 10);
					}
					String substr = str.substring(qaString.length());
					StringTokenizer st = new StringTokenizer(substr, ",");
					while (st.hasMoreTokens()) {
						quantitativeAttrs.addElement(new String(st.nextToken().trim()));
					}
				} else if (str.startsWith(ccaString)) {
					if (costCriteriaAttrs == null) {
						costCriteriaAttrs = new Vector(10, 10);
					}
					String substr = str.substring(ccaString.length());
					StringTokenizer st = new StringTokenizer(substr, ",");
					while (st.hasMoreTokens()) {
						costCriteriaAttrs.addElement(new String(st.nextToken().trim()));
					}
				} else if (str.startsWith(bcaString)) {
					if (benefitCriteriaAttrs == null) {
						benefitCriteriaAttrs = new Vector(10, 10);
					}
					String substr = str.substring(bcaString.length());
					StringTokenizer st = new StringTokenizer(substr, ",");
					while (st.hasMoreTokens()) {
						benefitCriteriaAttrs.addElement(new String(st.nextToken().trim()));
					}
				} else if (str.startsWith(cgString)) {
					if (comparableGroups == null) {
						comparableGroups = new Vector(10, 10);
					}
					Vector group = new Vector(10, 10);
					String substr = str.substring(cgString.length());
					StringTokenizer st = new StringTokenizer(substr, ",");
					while (st.hasMoreTokens()) {
						group.addElement(new String(st.nextToken().trim()));
					}
					comparableGroups.addElement(group);
				} else if (str.startsWith(igString)) {
					if (inclusionGroups == null) {
						inclusionGroups = new Vector(10, 10);
					}
					Vector group = new Vector(10, 10);
					String substr = str.substring(igString.length());
					StringTokenizer st = new StringTokenizer(substr, ",");
					while (st.hasMoreTokens()) {
						String s = st.nextToken().trim();
						group.addElement((s.equals("NULL")) ? null : new String(s));
					}
					inclusionGroups.addElement(group);
				}
			} while (true);
			br.close();
		} catch (IOException ioe) {
			System.out.println("* Read semantics from file. " + ioe);
		}
	}

	/**
	* Stores the relationships acquired in a file for further use.
	* Ignores derived attributes.
	*/
	public void writeSemanticsToFile(DataOutputStream dos) {
		try {
			String str = null;
			if (quantitativeAttrs != null && quantitativeAttrs.size() > 0) {
				str = qaString;
				boolean first = true;
				for (int i = 0; i < quantitativeAttrs.size(); i++) {
					String id = (String) quantitativeAttrs.elementAt(i);
					if (id == null) {
						continue;
					}
					Attribute attr = dTable.getAttribute(id);
					if (attr != null && attr.origin == AttributeTypes.original) {
						if (!first) {
							str += ",";
						}
						str += id;
						first = false;
					}
				}
				if (!first) {
					dos.writeBytes(str + "\n");
				}
			}
			if (benefitCriteriaAttrs != null && benefitCriteriaAttrs.size() > 0) {
				str = bcaString;
				boolean first = true;
				for (int i = 0; i < benefitCriteriaAttrs.size(); i++) {
					String id = (String) benefitCriteriaAttrs.elementAt(i);
					if (id == null) {
						continue;
					}
					Attribute attr = dTable.getAttribute(id);
					if (attr != null && attr.origin == AttributeTypes.original) {
						if (!first) {
							str += ",";
						}
						str += id;
						first = false;
					}
				}
				if (!first) {
					dos.writeBytes(str + "\n");
				}
			}
			if (costCriteriaAttrs != null && costCriteriaAttrs.size() > 0) {
				str = ccaString;
				boolean first = true;
				for (int i = 0; i < costCriteriaAttrs.size(); i++) {
					String id = (String) costCriteriaAttrs.elementAt(i);
					if (id == null) {
						continue;
					}
					Attribute attr = dTable.getAttribute(id);
					if (attr != null && attr.origin == AttributeTypes.original) {
						if (!first) {
							str += ",";
						}
						str += id;
						first = false;
					}
				}
				if (!first) {
					dos.writeBytes(str + "\n");
				}
			}
			if (comparableGroups != null && comparableGroups.size() > 0) {
				for (int n = 0; n < comparableGroups.size(); n++) {
					Vector group = (Vector) comparableGroups.elementAt(n);
					if (group != null && group.size() > 0) {
						str = cgString;
						boolean first = true;
						int nAdded = 0;
						for (int i = 0; i < group.size(); i++) {
							String id = (String) group.elementAt(i);
							if (id == null) {
								continue;
							}
							Attribute attr = dTable.getAttribute(id);
							if (attr != null && attr.origin == AttributeTypes.original) {
								if (!first) {
									str += ",";
								}
								str += id;
								first = false;
								++nAdded;
							}
						}
						if (nAdded > 0) {
							dos.writeBytes(str + "\n");
						}
					}
				}
			}
			if (inclusionGroups != null && inclusionGroups.size() > 0) {
				for (int n = 0; n < inclusionGroups.size(); n++) {
					Vector group = (Vector) inclusionGroups.elementAt(n);
					if (group != null && group.size() > 0) {
						str = igString;
						boolean first = true;
						int nAdded = 0;
						for (int i = 0; i < group.size(); i++) {
							String id = (String) group.elementAt(i);
							if (i > 0 && id == null) {
								continue;
							}
							Attribute attr = (id == null) ? null : dTable.getAttribute(id);
							if (id == null || (attr != null && attr.origin == AttributeTypes.original)) {
								if (!first) {
									str += ",";
								}
								str += (id == null) ? "NULL" : id;
								first = false;
								if (id != null) {
									++nAdded;
								}
							} else //if the attribute that includes others is derived, do not store the group
							if (i == 0) {
								break;
							}
						}
						if (nAdded > 0) {
							dos.writeBytes(str + "\n");
						}
					}
				}
			}
			//write additional descriptors of data semantics
			if (descriptors != null) {
				for (int i = 0; i < descriptors.size(); i++) {
					TagReader reader = (TagReader) descriptors.elementAt(i);
					reader.writeDescription(dos);
				}
			}
			//write the presentation preferences
			for (int i = 0; i < getPreferenceCount(); i++) {
				VisPreference pref = getPreference(i);
				if (pref == null) {
					continue;
				}
				String className = pref.getClass().getName();
				if (className.startsWith("L")) {
					className = className.substring(1);
				}
				//find the type of the preference using the array prefTypeCorr
				String type = null;
				for (int j = 0; j < prefTypeCorr.length && type == null; j++)
					if (className.equals(prefTypeCorr[j][1])) {
						type = prefTypeCorr[j][0];
					}
				if (type != null) {
					dos.writeBytes("<preference type=" + type + ">\n");
					pref.write(dos);
					dos.writeBytes("</preference>\n");
				}
			}
			dos.close();
		} catch (IOException ioe) {
			System.err.println("* save semantics to file: " + ioe);
		}
	}

	/**
	* Removes the table identifier from the given attribute identifier.
	* Before checking an attribute or adding it to any of the lists, the table
	* identifier must be removed!
	*/
	private String getPureAttrId(String attrId) {
		return IdUtil.getPureAttrId(attrId);
	}

	/**
	* Removes the table identifiers from all attribute identifiers contained
	* in the given vector. Returns another vector with "pure" identifiers.
	*/
	private Vector getPureAttrIds(Vector attrs) {
		return IdUtil.getPureAttrIds(attrs);
	}

	public boolean isAttributeBenefitCriterion(String attrId) {
		return (benefitCriteriaAttrs != null && benefitCriteriaAttrs.indexOf(getPureAttrId(attrId)) >= 0);
	}

	public void setAttributeIsBenefitCriterion(String attrId) {
		if (attrId == null)
			return;
		attrId = getPureAttrId(attrId);
		if (benefitCriteriaAttrs == null) {
			benefitCriteriaAttrs = new Vector(10, 10);
		}
		if (benefitCriteriaAttrs.indexOf(attrId) == -1) {
			benefitCriteriaAttrs.addElement(attrId);
			wasAnythingChanged = true;
		}
		eraseAttributeIsCostCriterion(attrId);
	}

	public void eraseAttributeIsBenefitCriterion(String attrId) {
		if (benefitCriteriaAttrs == null)
			return;
		if (attrId == null)
			return;
		attrId = getPureAttrId(attrId);
		if (benefitCriteriaAttrs.indexOf(attrId) > -1) {
			benefitCriteriaAttrs.removeElement(attrId);
		}
	}

	public boolean isAttributeCostCriterion(String attrId) {
		return (costCriteriaAttrs != null && costCriteriaAttrs.indexOf(getPureAttrId(attrId)) >= 0);
	}

	public void setAttributeIsCostCriterion(String attrId) {
		if (attrId == null)
			return;
		attrId = getPureAttrId(attrId);
		if (costCriteriaAttrs == null) {
			costCriteriaAttrs = new Vector(10, 10);
		}
		if (costCriteriaAttrs.indexOf(attrId) == -1) {
			costCriteriaAttrs.addElement(attrId);
			wasAnythingChanged = true;
		}
		eraseAttributeIsBenefitCriterion(attrId);
	}

	public void eraseAttributeIsCostCriterion(String attrId) {
		if (costCriteriaAttrs == null)
			return;
		if (attrId == null)
			return;
		attrId = getPureAttrId(attrId);
		if (costCriteriaAttrs.indexOf(attrId) > -1) {
			costCriteriaAttrs.removeElement(attrId);
		}
	}

	public boolean isAttributeQuantitative(String attrId) {
		return (quantitativeAttrs != null && quantitativeAttrs.indexOf(getPureAttrId(attrId)) >= 0);
	}

	public void setAttributeIsQuantitative(String attrId) {
		if (attrId == null)
			return;
		attrId = getPureAttrId(attrId);
		if (quantitativeAttrs == null) {
			quantitativeAttrs = new Vector(10, 10);
		}
		if (quantitativeAttrs.indexOf(attrId) == -1) {
			quantitativeAttrs.addElement(attrId);
			wasAnythingChanged = true;
		}
	}

	public boolean areAllAttributesQuantitative(Vector attrs) {
		if (quantitativeAttrs == null || attrs == null)
			return false;
		for (int i = 0; i < attrs.size(); i++)
			if (quantitativeAttrs.indexOf(getPureAttrId((String) attrs.elementAt(i))) < 0)
				return false;
		return true;
	}

	/**
	* Check comparability of the attributes listed in the vector. May ask the user
	* if the attributes are comparable. The methodName, if supplied, will be shown
	* to the user in an explanation why the question is being asked.
	*/
	public boolean areAttributesComparable(Vector attrs, String methodName) {
		if (attrs == null || attrs.size() < 1)
			return false;
		if (attrs.size() == 1)
			return true;
		//For attributes having parents, replace their identifiers in the vector
		//by the identifiers of the parents
		Vector topAttrs = new Vector(attrs.size(), 1);
		for (int i = 0; i < attrs.size(); i++) {
			Attribute attr = dTable.getAttribute((String) attrs.elementAt(i));
			if (attr == null)
				return false;
			Attribute parent = attr.getParent();
			String id = (parent == null) ? attr.getIdentifier() : parent.getIdentifier();
			if (!StringUtil.isStringInVectorIgnoreCase(id, topAttrs)) {
				topAttrs.addElement(id);
			}
		}
		if (topAttrs.size() == 1)
			return true; //all attributes have the same parent,
		//i.e. represent the same attribute for different values of parameters
		return areTopLevelAttributesComparable(topAttrs, methodName);
	}

	protected boolean areTopLevelAttributesComparable(Vector attrs, String methodName) {
		if (attrs == null || attrs.size() < 1)
			return false;
		if (attrs.size() == 1)
			return true;
		attrs = getPureAttrIds(attrs);
		if (comparableGroups == null) {
			comparableGroups = new Vector(10, 10);
		}
		// try to find a group which contains all attributes
		for (int i = 0; i < comparableGroups.size(); i++)
			if (StringUtil.isSubsetOf(attrs, (Vector) comparableGroups.elementAt(i)))
				return true;
		// ask if the attributes are comparable
		return askIfComparable(attrs, methodName);
	}

	/**
	* Asks the user if the attributes listed in the vector are comparable. The
	* methodName, if supplied, will be shown to the user in an explanation why
	* the question is being asked.
	*/
	protected boolean askIfComparable(Vector attrs, String methodName) {
		if (!questionsAllowed)
			return false;
		Panel p = new Panel();
		p.setLayout(new ColumnLayout());
		Label label = null;
		// following string: "Are the attributes comparable ?"
		p.add(label = new Label(res.getString("Are_the_attributes"), Label.CENTER));
		if (methodName != null) {
			new PopupManager(label,
			/* following string:
			"To apply a visualization method\n"+
			"<"+methodName+">\n"+
			"to your data, it is necessary to know\n"+
			"if the attributes are comparable"*/
			res.getString("To_apply_a") + "<" + methodName + ">\n" + res.getString("to_your_data_it_is") + res.getString("if_the_attributes_are"), true);
		}
		Panel pp = null;
		if (attrs.size() > 5) {
			pp = new Panel(new ColumnLayout());
			ScrollPane scp = new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED);
			scp.add(pp);
			p.add(scp);
		}
		for (int i = 0; i < attrs.size(); i++) {
			Label lab = new Label("" + (i + 1) + ". " + dTable.getAttributeName((String) attrs.elementAt(i)));
			if (pp == null) {
				p.add(lab);
			} else {
				pp.add(lab);
			}
		}
		if (pp == null) {
			p.add(new Line(false));
		}
		// following string:"do not memorize this relationship"
		Checkbox cb = new Checkbox(res.getString("do_not_memorize_this"), false);
		p.add(cb);
		p.add(new Line(false));
		new PopupManager(cb,
		/* following string:
		"Depending on your selection, the information "+
		"about comparability of the attributes may be stored "+
		"in the system's knowledge base or discarded"*/
		res.getString("Depending_on_your") + res.getString("about_comparability") + res.getString("in_the_system_s"), true);
		OKDialog dlg = new OKDialog(CManager.getAnyFrame(),
		// following string:"Check comparability of attributes"
				res.getString("Check_comparability"), OKDialog.YES_NO_MODE, true);
		dlg.addContent(p);
		dlg.show();
		if (dlg.wasCancelled())
			return false;
		if (cb.getState()) {
			if (warningsAllowed && methodName != null) {
				p = new Panel();
				p.setLayout(new ColumnLayout());
				// following string:"Now you will get a <"+methodName+">"
				p.add(new Label(res.getString("Now_you_will_get_a_") + methodName + ">", Label.CENTER));
				// following string:"visualization of the attributes which are not comparable"
				p.add(new Label(res.getString("visualization_of_the"), Label.CENTER));
				p.add(new Label());
				// following string:"Attention !"
				p.add(new Label(res.getString("Attention_"), Label.CENTER));
				// following string:"This visualization can be interpreted wrongly !"
				p.add(new Label(res.getString("This_visualization"), Label.CENTER));
				p.add(new Line(false));
				dlg = new OKDialog(CManager.getAnyFrame(),
				// following string:"Visualization of non-comparable attributes"
						res.getString("Visualization_of_non"), false);
				dlg.addContent(p);
				dlg.show();
			} else { // present this visualziation without the warning
			}
		} else {
			setAttributesComparable(attrs);
		}
		//printComparabilityInfo();
		return true;
	}

	public void setAttributesComparable(Vector attrs) {
		for (int i = 0; i < attrs.size() - 1; i++) {
			for (int j = i + 1; j < attrs.size(); j++) {
				String attr1 = (String) attrs.elementAt(i), attr2 = (String) attrs.elementAt(j);
				setAttributesComparable(attr1, attr2);
			}
		}
		wasAnythingChanged = true;
	}

	public void setAttributesComparable(String attr1, String attr2) {
		if (attr1 == null || attr2 == null)
			return;
		attr1 = getPureAttrId(attr1);
		attr2 = getPureAttrId(attr2);
		if (comparableGroups == null) {
			comparableGroups = new Vector(10, 5);
		}
		for (int i = 0; i < comparableGroups.size(); i++) {
			Vector group = (Vector) comparableGroups.elementAt(i);
			int n1 = group.indexOf(attr1), n2 = group.indexOf(attr2);
			if (n1 >= 0 && n2 >= 0)
				return; // this relationship is already known
			if ((n1 >= 0 && n2 == -1) || (n1 == -1 && n2 >= 0)) {
				setAttributesComparable(attr1, attr2, i);
				return;
			}
		}
		setAttributesComparable(attr1, attr2, -1);
	}

	protected void setAttributesComparable(String attr1, String attr2, int groupNum) {
		if (groupNum == -1) {
			if (comparableGroups == null) {
				comparableGroups = new Vector(10, 5);
			}
			Vector group = new Vector(5, 5);
			group.addElement(attr1);
			group.addElement(attr2);
			comparableGroups.addElement(group);
		} else {
			Vector group = (Vector) comparableGroups.elementAt(groupNum);
			int n1 = group.indexOf(attr1), n2 = group.indexOf(attr2);
			if (n1 >= 0 && n2 == -1) {
				group.addElement(attr2);
				joinGroupWithElementToThis(groupNum, attr2);
			} else {
				group.addElement(attr1);
				joinGroupWithElementToThis(groupNum, attr1);
			}
		}
		wasAnythingChanged = true;
	}

	protected void joinGroupWithElementToThis(int groupN, String attr) {
		for (int i = 0; i < comparableGroups.size(); i++)
			if (i != groupN) {
				Vector v = (Vector) comparableGroups.elementAt(i);
				int n = v.indexOf(attr);
				if (n < 0) {
					continue;
				}
				// move all elements except <attr> from the group found
				// to the group groupN
				Vector vv = (Vector) comparableGroups.elementAt(groupN);
				for (int j = 0; j < v.size(); j++) {
					String str = (String) v.elementAt(j);
					if (str.equals(attr)) {
						continue;
					}
					vv.addElement(str);
				}
				comparableGroups.removeElementAt(i);
				return;
			}
	}

	protected void printComparabilityInfo() {
		if (comparableGroups == null)
			return;
		for (int i = 0; i < comparableGroups.size(); i++) {
			Vector v = (Vector) comparableGroups.elementAt(i);
			System.out.println("*C group=" + i + ", elements:");
			System.out.print("*C ");
			for (int j = 0; j < v.size(); j++) {
				System.out.print(((j == 0) ? "" : ",") + (String) v.elementAt(j));
			}
			System.out.println();
		}
	}

	protected int findInclusionGroupWithAttributes(Vector attrs) {
		if (inclusionGroups == null)
			return -1;
		for (int i = 0; i < inclusionGroups.size(); i++)
			if (StringUtil.isSubsetOf(attrs, (Vector) inclusionGroups.elementAt(i)))
				return i;
		return -1;
	}

	/**
	* Checks existence of inclusion relationship between the attributes listed in
	* the vector. May ask the user if the attributes are included in some whole.
	* The methodName, if supplied, will be shown to the user in an explanation why
	* the question is being asked.
	*/
	public boolean areAttributesIncluded(Vector attrs, String methodName) {
		if (attrs == null || attrs.size() < 2)
			return false;
		if (attrs.elementAt(0) == null) {
			attrs.removeElementAt(0);
		}
		Vector pureAttrs = getPureAttrIds(attrs);
		// 1. find a group where all are present
		int n = findInclusionGroupWithAttributes(pureAttrs);
		if (n >= 0) { //the group is found; determine the main attribute (container)
			Vector group = (Vector) inclusionGroups.elementAt(n);
			boolean mainExists = false;
			if (group.elementAt(0) != null) { //the main attribute is the first in the group
				int idx = StringUtil.indexOfStringInVectorIgnoreCase((String) group.elementAt(0), pureAttrs);
				if (idx >= 0) { //the main attribute is present among the selected attributes
					mainExists = true;
					if (idx > 0) { //move the main attribute to the first place
						Object main = attrs.elementAt(idx);
						attrs.removeElementAt(idx);
						attrs.insertElementAt(main, 0);
					}
				}
			}
			if (!mainExists) {
				//null at the beginning will signalize that the main attribute is absent
				attrs.insertElementAt(null, 0);
			}
			return true;
		}
		if (!questionsAllowed)
			return false;
		//2. Ask the user about the relationships between the attributes
		Panel p = new Panel();
		p.setLayout(new ColumnLayout());
		// following string:"Select the attribute which includes all others"
		p.add(new Label(res.getString("Select_the_attribute"), Label.CENTER));
		List l = new List(5);
		// following string:"No such attribute"
		l.add(res.getString("No_such_attribute"));
		for (int i = 0; i < attrs.size(); i++) {
			l.add(dTable.getAttributeName((String) attrs.elementAt(i)));
		}
		l.select(0);
		p.add(l);
		p.add(new Line(false));
		OKDialog dlg = new OKDialog(CManager.getAnyFrame(),
		// following string:"Check inclusion of all attributes in one"
				res.getString("Check_inclusion_of"), false);
		dlg.addContent(p);
		dlg.show();
		boolean insertedNull = false;
		if (l.getSelectedIndex() == 0) {
			attrs.insertElementAt(null, 0);
			insertedNull = true;
		} else {
			n = l.getSelectedIndex() - 1;
			if (n > 0) { //put the main attribute on the first place
				Object main = attrs.elementAt(n);
				attrs.removeElementAt(n);
				attrs.insertElementAt(main, 0);
			}
		}
		boolean result = askIfAttributesIncluded(attrs, methodName);
		if (!result && insertedNull && attrs.elementAt(0) == null) {
			attrs.removeElementAt(0);
		}
		return result;
	}

	/**
	* the 1st element of attrs contains ID of the overall attribute
	* (possible, null)
	* other elements - included attributes
	*/
	public boolean areAttributesWithOrderIncluded(Vector attrs) {
		if (inclusionGroups == null || attrs == null)
			return false;
		attrs = getPureAttrIds(attrs);
		String main = (String) attrs.elementAt(0);
		boolean found = false;
		int foundN = -1;
		if (main == null) {
			for (int i = 0; i < inclusionGroups.size(); i++)
				if (StringUtil.isSubsetOf(attrs, (Vector) inclusionGroups.elementAt(i), 1))
					return true;
		} else { // main!=null !
			for (int i = 0; i < inclusionGroups.size(); i++) {
				Vector group = (Vector) inclusionGroups.elementAt(i);
				String grMain = (String) group.elementAt(0);
				if (grMain == null) {
					continue;
				}
				if (main.equals(grMain) && StringUtil.isSubsetOf(attrs, group))
					return true;
			}
		}
		return false;
	}

	/**
	* Asks the user if the attributes listed in the vector are included in some
	* whole. The methodName, if supplied, will be shown to the user in an
	* explanation why the question is being asked.
	*/
	public boolean askIfAttributesIncluded(Vector attrs, String methodName) {
		if (!questionsAllowed)
			return false;
		Panel p = new Panel();
		p.setLayout(new ColumnLayout());
		// following string:"Do the attributes"
		p.add(new Label(res.getString("Do_the_attributes"), Label.CENTER));
		Panel pp = null;
		if (attrs.size() > 5) {
			pp = new Panel(new ColumnLayout());
			ScrollPane scp = new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED);
			scp.add(pp);
			p.add(scp);
		}
		String main = (String) attrs.elementAt(0);
		int i0 = (main == null) ? 1 : 0;
		for (int i = i0; i < attrs.size(); i++) {
			Label lab = new Label("" + (i + 1 - i0) + ". " + dTable.getAttributeName((String) attrs.elementAt(i)));
			if (pp == null) {
				p.add(lab);
			} else {
				pp.add(lab);
			}
		}
		if (main == null) {
			// following string:"represent non-overlapping parts of some whole?"
			p.add(new Label(res.getString("represent_non"), Label.CENTER));
		} else {
			// following string:"represent non-overlapping parts of the attribute"
			p.add(new Label(res.getString("represent_non1"), Label.CENTER));
			p.add(new Label(dTable.getAttributeName(main)));
		}
		p.add(new Line(false));
		// following string:"do not memorize this relationship"
		Checkbox cb = new Checkbox(res.getString("do_not_memorize_this"), false);
		p.add(cb);
		p.add(new Line(false));
		new PopupManager(cb,
		/* following string:"Depending on your selection, the information "+
		"about inclusion of the attributes may be stored "+
		"in the system's knowledge base or discarded"*/
		res.getString("Depending_on_your") + res.getString("about_inclusion_of") + res.getString("in_the_system_s"), true);
		OKDialog dlg = new OKDialog(CManager.getAnyFrame(),
		// following string:"Check inclusion of attributes"
				res.getString("Check_inclusion_of1"), OKDialog.YES_NO_MODE, true);
		dlg.addContent(p);
		dlg.show();
		if (dlg.wasCancelled())
			return false;
		if (cb.getState()) {
			if (warningsAllowed && methodName != null) {
				p = new Panel();
				p.setLayout(new ColumnLayout());
				// following string:"Now you will get a <"+methodName+">"
				p.add(new Label(res.getString("Now_you_will_get_a_") + methodName + ">", Label.CENTER));
				// following string:"visualization of the attributes which DO NOT"
				p.add(new Label(res.getString("visualization_of_the1"), Label.CENTER));
				// following string:"represent non-overlapping parts of some meaningfull whole"
				p.add(new Label(res.getString("represent_non2"), Label.CENTER));
				p.add(new Label());
				// following string:"Attention !"
				p.add(new Label(res.getString("Attention_"), Label.CENTER));
				// following string:"This visualization can be interpreted wrongly !"
				p.add(new Label(res.getString("This_visualization"), Label.CENTER));
				p.add(new Line(false));
				dlg = new OKDialog(CManager.getAnyFrame(),
				// following string: "Visualization of non-inclusive attributes"
						res.getString("Visualization_of_non1"), false);
				dlg.addContent(p);
				dlg.show();
			} else { // present this visualziation without the warning
			}
		} else {
			addInclusionRelationship(attrs);
		}
		//printInclusionInfo();
		return true;
	}

	public void addInclusionRelationship(Vector attrs) {
		if (attrs == null || attrs.size() < 2)
			return;
		attrs = getPureAttrIds(attrs);
		if (inclusionGroups == null) {
			inclusionGroups = new Vector(10, 10);
		}
		if (quantitativeAttrs == null) {
			quantitativeAttrs = new Vector(50, 10);
		}
		for (int j = 0; j < attrs.size(); j++)
			if (attrs.elementAt(j) != null && !StringUtil.isStringInVectorIgnoreCase((String) attrs.elementAt(j), quantitativeAttrs)) {
				quantitativeAttrs.addElement(attrs.elementAt(j));
			}
		//possibly, the new group includes some of previous groups ...
		for (int i = inclusionGroups.size() - 1; i >= 0; i--) {
			Vector group = (Vector) inclusionGroups.elementAt(i);
			if (StringUtil.isSubsetOf(group, attrs)) //the group is included in attrs
				if (group.elementAt(0) == null || StringUtil.sameStrings((String) group.elementAt(0), (String) attrs.elementAt(0))) {
					//add all attributes from group to attrs and remove the group
					for (int j = 1; j < group.size(); j++)
						if (!StringUtil.isStringInVectorIgnoreCase((String) group.elementAt(j), attrs)) {
							attrs.addElement(group.elementAt(j));
						}
					inclusionGroups.removeElementAt(i);
				} else {
					; //this may be a conflict!
						//think what to do in this case...
				}
		}
		inclusionGroups.addElement(attrs);
		if (attrs.elementAt(0) == null) {
			Vector subattrs = new Vector(attrs.size() - 1, 1);
			for (int i = 1; i < attrs.size(); i++) {
				subattrs.addElement(attrs.elementAt(i));
			}
			setAttributesComparable(subattrs);
		} else {
			setAttributesComparable(attrs);
		}
		wasAnythingChanged = true;
		//add logical implications
		if (attrs.elementAt(0) != null) {
			String main = (String) attrs.elementAt(0);
			//if the main element of the group is included in some other group G,
			//one can produce a new group by replacing this element in G by the
			//content of the group attr
			for (int i = 0; i < inclusionGroups.size(); i++) {
				Vector group = (Vector) inclusionGroups.elementAt(i);
				int idx = StringUtil.indexOfStringInVectorIgnoreCase(main, group);
				if (idx > 0) {
					Vector newGroup = (Vector) group.clone();
					newGroup.removeElementAt(idx);
					for (int j = 1; j < attrs.size(); j++)
						if (!StringUtil.isStringInVectorIgnoreCase((String) attrs.elementAt(j), newGroup)) {
							newGroup.addElement(attrs.elementAt(j));
						}
					if (!areAttributesWithOrderIncluded(newGroup)) {
						addInclusionRelationship(newGroup);
					}
				}
			}
		}
		for (int i = 1; i < attrs.size(); i++) {
			String elem = (String) attrs.elementAt(i);
			//if some of the elements of the group attr contains other attributes,
			//we can produce a new group by inserting in attrs the attributes included
			//in this element instead of this element
			for (int j = 0; j < inclusionGroups.size(); j++) {
				Vector group = (Vector) inclusionGroups.elementAt(j);
				if (group.elementAt(0) != null && elem.equalsIgnoreCase((String) group.elementAt(0))) {
					Vector newGroup = (Vector) attrs.clone();
					newGroup.removeElementAt(i);
					for (int k = 1; k < group.size(); k++)
						if (!StringUtil.isStringInVectorIgnoreCase((String) group.elementAt(k), newGroup)) {
							newGroup.addElement(group.elementAt(k));
						}
					if (!areAttributesWithOrderIncluded(newGroup)) {
						addInclusionRelationship(newGroup);
					}
				}
			}
		}
	}

	protected void printInclusionInfo() {
		if (inclusionGroups == null)
			return;
		for (int i = 0; i < inclusionGroups.size(); i++) {
			Vector v = (Vector) inclusionGroups.elementAt(i);
			System.out.print("*I group " + i + ": ");
			for (int j = 0; j < v.size(); j++) {
				System.out.print(((j == 0) ? "" : ",") + (String) v.elementAt(j));
			}
			System.out.println();
		}
	}

	/**
	* Returns the number of specified preferences for cartographic representation
	* of attribute values.
	*/
	public int getPreferenceCount() {
		if (presPref == null)
			return 0;
		return presPref.size();
	}

	/**
	* Returns the presentation preference with the given index. The type
	* of the returned object is not restricted (for example, this may be
	* an instance of spade.vis.preferences.IconCorrespondence or something
	* else).
	*/
	public VisPreference getPreference(int idx) {
		if (idx < 0 || idx >= getPreferenceCount())
			return null;
		return (VisPreference) presPref.elementAt(idx);
	}

	/**
	* Returns its semantic descriptors. The classes processing these descriptors
	* must check their types before doing anything else.
	*/
	public Vector getSemDescriptors() {
		return descriptors;
	}

	/**
	* Adds a preference for cartographic representation of attribute values.
	* The type of the argument is not restricted (for example, this may be
	* an instance of spade.vis.preferences.IconCorrespondence or something
	* else).
	*/
	public void addPreference(VisPreference pref) {
		if (pref == null)
			return;
		if (presPref == null) {
			presPref = new Vector(5, 5);
		}
		presPref.addElement(pref);
		wasAnythingChanged = true;
	}

	/**
	* Removes the representation preference with the specified index.
	*/
	public void removePreference(int idx) {
		if (idx < 0 || idx >= getPreferenceCount())
			return;
		presPref.removeElementAt(idx);
		wasAnythingChanged = true;
	}
}
