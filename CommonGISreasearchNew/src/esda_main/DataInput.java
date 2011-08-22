package esda_main;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;

import spade.analysis.system.DataLoader;
import spade.lib.util.Parameters;
import spade.vis.database.Attribute;
import spade.vis.database.DataRecord;
import spade.vis.database.DataTable;
import spade.vis.preference.IconVisSpec;
import data_input.DataInputManager;
import data_input.EnteredPoint;

/**
* Suports input of observation locations in FloraWeb project
*/
public class DataInput extends ShowMap {
	DataInputManager dIMan = null;
	protected Vector icons = null;
	protected Vector groups = null;
	protected String taxList = "";

	/**
	* Read and set values of system parameters
	*/
	@Override
	protected void setSystemParameters(Parameters parm) {
		super.setSystemParameters(parm);
		//set specific parameters for data input
		parm.setParameter("select_in_layer", getParameter("select_in_layer"));
		parm.setParameter("mapdist_properties", getParameter("mapdist_properties"));
		parm.setParameter("mapdist_imagesdir", getParameter("mapdist_imagesdir"));
		parm.setParameter("mapdist_switchscale", getParameter("mapdist_switchscale"));

	}

	/**
	* Skips reading system's parameters from the file system.cnf
	*/
	@Override
	protected void readSystemParamsFromFile(Parameters parm) {
	}

	@Override
	protected void constructUI() {
		super.constructUI();
		dIMan = new DataInputManager();
		dIMan.setSupervisor(supervisor);
		dIMan.setDataLoader((DataLoader) ui.getDataKeeper());
		if (dIMan.prepareToWork()) {
			ui.placeComponent(dIMan);
		}
		if (getParameter("mapdist_switchscale") != null) {
			dIMan.switchScale = Float.valueOf(getParameter("mapdist_switchscale")).floatValue();
		}
		System.out.println("switchscale:" + dIMan.switchScale);
	}

	/**
	 * Initialization
	 */
	@Override
	public void init() {
		System.out.println("suports input of observation locations in FloraWeb project");
		super.init();
		prepareMapDist();
	}

	/**
	 * Only for FlorawWeb-Project!
	 * Method is called from JavaScript and
	 * imports und shows of old daten for species distribution
	 * @param oTaxList 'taxnr - frequency' list with comma delimeter (for example "1234-4,3456-1,7654-1")
	 * or 'taxnr-code' where code is one letter A..Z (for example "1234-A,3456-Y,7654-Z")
	 * @param oMTBData tk-identifier list with comma delimeter
	 */
	public void setSpeciesDistribution(String oTaxList, String oMTBData) {

		System.out.println("setSpeciesDistribution()");
		taxList = oTaxList;
		DataTable dt = new DataTable();
		dt.setContainerIdentifier("MTB");
		StringTokenizer tz = new StringTokenizer(oMTBData, ",");
		boolean addedGroupAttr = false;
		while (tz.hasMoreTokens()) {
			String t = tz.nextToken();
			int p = t.indexOf("-");
			if (p >= 0) {
				String id = t.substring(0, p);
				String val = t.substring(p + 1);

				if (dt.getAttrCount() == 0) {
					try {
						int ii = Integer.valueOf(val.trim()).intValue();
						dt.addAttribute("specnum", "specnum", 'I');
						System.out.println("added specnum-attribute");

					} catch (NumberFormatException nfe) {
						dt.addAttribute("group", "group", 'C'); //
						Attribute attr = dt.getAttribute("group");

						String valList[] = new String[groups.size()];
						Color colorList[] = new Color[groups.size()];
						for (int i = 0; i < groups.size(); i++) {
							valList[i] = ((String[]) groups.elementAt(i))[1];
							colorList[i] = new Color(Integer.valueOf(((String[]) groups.elementAt(i))[2], 16).intValue());
						}
						attr.setValueListAndColors(valList, colorList);

						dt.addAttribute("descr", "descr", 'C');

						System.out.println("added group-attribute");
						addedGroupAttr = true;

					}
				}

				DataRecord r = new DataRecord(id, id);
				if (addedGroupAttr) {
					r.addAttrValue(getGroup(val.charAt(0)));
					r.addAttrValue(getDescriptor(val.charAt(0)));
				} else {
					r.addAttrValue(val);
				}

				dt.addDataRecord(r);
			} else {
				System.out.println("InputDataFormatError:" + t);
				return;
			}
		}

		System.out.println(dt.getDataItemCount() + " items are recived.");
		if (addedGroupAttr) {
			makeIconSpecList();
			dIMan.setIconVisSpecList(iconSpecList);
		}
		dIMan.setDataTable(dt);
		dIMan.showDataOnMap();
	}

	/**
	 * Only for FlorawWeb-Project!
	 * Method is called from JavaScript and
	 * selects und shows tk-cells
	 * @param oMTBs tk-identifier list with comma delimeter
	 */
	public void setSearchMTBs(String oMTBs) {
		System.out.println("setSearchMTBs");
		Vector ids = new Vector();
		StringTokenizer tz = new StringTokenizer(oMTBs + ",", ",");
		while (tz.hasMoreTokens()) {
			ids.addElement(tz.nextToken().trim());
		}
		dIMan.setSelectedObjects(ids);
	}

	/**
	 * Only for FlorawWeb-Project!
	 * Method is called from JavaScript and
	 * returns identifier list of selected tk-cells.
	 * @return oMTBs tk-identifier list with comma delimeter
	 */

	public String getSearchMTBs() {
		Vector objs = dIMan.getSelectedObjects();
		if (objs == null || objs.size() == 0)
			return "";
		String s = "";
		for (int i = 0; i < objs.size(); i++) {
			s += ",'" + (String) objs.elementAt(i) + "'";
		}
		return s.substring(1);
	}

	/**
	 * Only for FlorawWeb-Project!
	 * Method is called from JavaScript and returns taxnr list
	 * @return taxnr list
	 */

	public String getSpecies() {
		return taxList;
	}

	/**
	 featurecode.group
	*/
	protected Vector iconSpecList = null;

	protected String getGroup(char ch) {
		for (int i = 0; i < groups.size(); i++) {
			String s = ((String[]) groups.elementAt(i))[0];
			for (int j = 0; j < s.length(); j++) {
				if (s.charAt(j) == ch)
					return ((String[]) groups.elementAt(i))[1];
			}

		}
		return "unknown";
	}

	protected String getDescriptor(char ch) {
		for (int i = 0; i < icons.size(); i++) {
			String s = ((String[]) icons.elementAt(i))[0];
			if (s.charAt(0) == ch)
				return ((String[]) icons.elementAt(i))[2];
		}
		return "unknown";
	}

	protected void makeIconSpecList() {
		iconSpecList = new Vector();
		String s = getDocumentBase().toString();
		int p = s.lastIndexOf("/");
		if (p >= 0) {
			s = s.substring(0, p + 1);
		}
		s += getParameter("mapdist_imagesdir");
		System.out.println("iconsdir:" + s);

		for (int i = 0; i < icons.size(); i++) {
			IconVisSpec sp = new IconVisSpec();
			sp.setPathToImage(s + ((String[]) icons.elementAt(i))[1]);
			sp.setImage(sp.loadImage(this));
			sp.addAttrValuePair("descr", ((String[]) icons.elementAt(i))[2]);
			iconSpecList.addElement(sp);
		}

	}

	protected InputStream openInputStream(String path) {
		if (path == null)
			return null;
		URL url = spade.lib.util.URLSupport.makeURLbyPath(getDocumentBase(), path);
		try {
			return url.openStream();
		} catch (IOException ioe) {
			System.out.println("can not open url:" + url);
		}
		return null;
	}

	protected void prepareMapDist() {
		if (groups != null && icons != null)
			return;

		InputStream is = openInputStream(getParameter("mapdist_properties"));
		if (is == null)
			return;
		Properties prop = new Properties();
		try {
			prop.load(is);
			System.out.println("mapdist.props:" + prop.size());
		} catch (IOException ioe) {
			System.out.println("can not load mapdist-properties");
			return;
		}

		groups = new Vector();
		for (int i = 1; i <= 100; i++) {
			String s[] = new String[3]; // codeset (string),descr,color(hex)

			s[0] = prop.getProperty("featurecode.group." + i);
			s[1] = prop.getProperty("featurecode.group" + i + ".desc");
			s[2] = prop.getProperty("featurecode.group" + i + ".fillcol");

			if (s[0] != null && s[1] != null && s[2] != null) {
				groups.addElement(s);
			}

		}
		System.out.println("total groups:" + groups.size());
		if (groups.size() == 0) {
			groups = null;
		}

		icons = new Vector();
		for (int i = 0; i < 100; i++) {
			String s[] = new String[3]; // code,file,descr
			s[0] = prop.getProperty("symbol." + i + ".code");
			s[1] = prop.getProperty("symbol." + i + ".file");
			s[2] = prop.getProperty("symbol." + i + ".desc");
			if (s[0] != null && s[1] != null && s[2] != null) {
				icons.addElement(s);
			}
		}

		System.out.println("total icons:" + icons.size());
		if (icons.size() == 0) {
			icons = null;
		}

	}

	public void setSelectedTerritory() {
		dIMan.setSelectedTerritory(false);
	}

	/**
	* Only for FlorawWeb-Project!
	* Method is called from JavaScript and
	* returns entered points and cells data
	* with comma delimeter. Cells as tk-identifiers and
	* Points as prefix 'p', tk-identifiers, coordinates and radius.
	* For examples: "1234,4678,p2345,3333.22,4444.44,45.3,1235,pp2345,3333.22,4444.44,45.3"
	* @param list cells and points.
	*/
	public String getLocalities() {
		String s = "";

		Vector sc = dIMan.getSelectedCells();
		if (sc != null && sc.size() > 0) {
			for (int i = 0; i < sc.size(); i++) {
				s += "," + (String) sc.elementAt(i);
			}
		}

		Vector ep = dIMan.getEnteredPoints();
		if (ep != null && ep.size() > 0) {
			for (int i = 0; i < ep.size(); i++) {
				EnteredPoint p = (EnteredPoint) ep.elementAt(i);
				s += ",p" + p.tk_grid + "," + p.x + "," + p.y + "," + p.radius;
			}
		}
		if (s.length() == 0)
			return "";
		return s.substring(1);
	}

	public void clearDistribution() {

		System.out.println("clearDistribution");
		dIMan.clearDataOnMap();
	}

}
