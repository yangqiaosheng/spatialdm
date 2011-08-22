package ui;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.awt.TextArea;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.system.DataKeeper;
import spade.analysis.system.ESDACore;
import spade.analysis.system.SystemUI;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.OKDialog;
import spade.lib.basicwin.SelectDialog;
import spade.lib.lang.Language;
import spade.lib.util.IdMaker;
import spade.vis.database.Attribute;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.DataRecord;
import spade.vis.database.DataTable;
import spade.vis.database.Parameter;
import spade.vis.space.GeoLayer;
import spade.vis.space.LayerManager;

public class TableJoiner implements ActionListener {
	static ResourceBundle res = Language.getTextResource("ui.Res");

	protected TextArea sel = null;
	protected List tblList = null;
	protected ESDACore core = null;
	/**
	* The tables to join
	*/
	protected Vector toJoin = null;
	/**
	* For each table contains a vector of selected attributes (instances of
	* spade.vis.database.Attribute)
	*/
	protected Vector attrs[] = null;

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource().equals(tblList) || e.getActionCommand().equals("attr_select")) {
			int idx = tblList.getSelectedIndex();
			if (idx < 0 || idx >= toJoin.size())
				return;
			Vector selected = null;
			if (attrs[idx] != null) {
				selected = new Vector(attrs[idx].size(), 1);
				for (int i = 0; i < attrs[idx].size(); i++) {
					Attribute at = (Attribute) attrs[idx].elementAt(i);
					if (at.getParent() != null) {
						at = at.getParent();
					}
					if (!selected.contains(at.getIdentifier())) {
						selected.addElement(at.getIdentifier());
					}
				}
			}
			AttributeChooser attrSel = new AttributeChooser();
			attrs[idx] = attrSel.selectColumns((AttributeDataPortion) toJoin.elementAt(idx), selected, null, false, res.getString("Select_attributes_to_join"), core.getUI());
			updateList();
		} else if (e.getSource().equals(advanced)) {
			simple.dispose();
			advMode = true;
		}
	}

	private OKDialog simple;
	private Button advanced;
	private boolean advMode = false;

	public void joinTables(LayerManager lman, ESDACore core) {
		if (lman == null)
			return;
		this.core = core;

		int nlayers = lman.getLayerCount();
		if (nlayers < 1)
			return;
		SelectDialog selDia = new SelectDialog(CManager.getAnyFrame(), res.getString("Select_layer"), res.getString("Select_layer"));

		Vector layers = new Vector(nlayers, 1);
		;
		for (int i = 0; i < nlayers; i++) {
			GeoLayer l = lman.getGeoLayer(i);
/**/		if (l.hasThematicData()) {
				layers.addElement(l);
//        selDia.addOption(l.getName(),l.getContainerIdentifier(),layers.size()==1);
			}
		}
		if (layers.size() < 1) {
			core.getUI().showMessage(res.getString("no_layers_with_data"), true);
			return;
		}

		DataKeeper dk = core.getDataKeeper();
		if (dk == null)
			return;

		Vector layersFiltered = new Vector();
		for (int i = 0; i < layers.size(); i++) {
			GeoLayer layer = (GeoLayer) layers.elementAt(i);
			String eid = layer.getEntitySetIdentifier();
			int tablesCount = 0;
			for (int n = 0; n < dk.getTableCount(); n++) {
				AttributeDataPortion dt = dk.getTable(n);
				if (dt.getEntitySetIdentifier().equals(eid)) {
					tablesCount++;
				}
			}
			if (tablesCount > 1) {
				layersFiltered.addElement(layer);
			}
		}

		if (layersFiltered.size() < 1) {
			core.getUI().showMessage(res.getString("no_layers_with_2_tables"), true);
			return;
		}

		for (int i = 0; i < layersFiltered.size(); i++) {
			GeoLayer l = (GeoLayer) layersFiltered.elementAt(i);
			selDia.addOption(l.getName(), l.getContainerIdentifier(), false/*layersFiltered.size()==1*/);
		}

		int selected = 0;
		if (layersFiltered.size() > 1) {
			selDia.show();
			if (selDia.wasCancelled())
				return;
			selected = selDia.getSelectedOptionN();
		}

		GeoLayer layer = (GeoLayer) layersFiltered.elementAt(selected);
		if (layer == null)
			return;

		String eid = layer.getEntitySetIdentifier();

		toJoin = new Vector(dk.getTableCount(), 1);
		for (int n = 0; n < dk.getTableCount(); n++) {
			AttributeDataPortion dt = dk.getTable(n);
			if (dt.getEntitySetIdentifier().equals(eid)) {
				toJoin.addElement(dt);
			}
		}
		if (toJoin.size() < 1) {
			core.getUI().showMessage(res.getString("no_tables_to_join"), true);
			return;
		}
		attrs = new Vector[toJoin.size()];
		for (int i = 0; i < attrs.length; i++) {
			attrs[i] = null;
		}
/**/
		TextField tableName = new TextField(res.getString("Table_join"));

		Panel advP = new Panel(new ColumnLayout());
		Vector tablesCB = new Vector();
		Panel tableList = new Panel(new ColumnLayout());
		for (int i = 0; i < toJoin.size(); i++) {
			tablesCB.addElement(new Checkbox(((AttributeDataPortion) toJoin.elementAt(i)).getName(), true));
			tableList.add((Checkbox) tablesCB.elementAt(i));
		}
		advP.add(tableList);
		advP.add(new Label(res.getString("New_table_name")), BorderLayout.NORTH);
		advP.add(tableName);
		advanced = new Button(res.getString("Select_individual_attributes"));
		advanced.addActionListener(this);
		advP.add(advanced);

		simple = new OKDialog(CManager.getAnyFrame( /*this*/), res.getString("Select_tables_to_join"), OKDialog.OK_CANCEL_MODE, true);
		simple.addContent(advP);
		simple.show();
		if (simple.wasCancelled())
			return;
/**/
		for (int i = 0; i < toJoin.size(); i++) {
			if (((Checkbox) tablesCB.elementAt(i)).getState()) {
				attrs[i] = ((DataTable) toJoin.elementAt(i)).getAttrList();
			} else {
				attrs[i] = null;
			}
		}
/**/
		if (advMode) {
			Panel mainP = new Panel(new GridLayout(1, 2, 10, 0));
			Panel p = new Panel(new BorderLayout());
			p.add(new Label(res.getString("tables") + ":"), BorderLayout.NORTH);
			tblList = new List(10, false);
			for (int i = 0; i < toJoin.size(); i++) {
				tblList.add(((AttributeDataPortion) toJoin.elementAt(i)).getName());
			}
			tblList.addActionListener(this);
			tblList.select(0);
			p.add(tblList, BorderLayout.CENTER);
			Panel pf = new Panel(new FlowLayout(FlowLayout.CENTER, 0, 2));
			p.add(pf, BorderLayout.SOUTH);
			Button but = new Button(res.getString("Select_attributes"));
			but.setActionCommand("attr_select");
			but.addActionListener(this);
			pf.add(but);
			mainP.add(p);

			p = new Panel(new BorderLayout());
			Panel pp = new Panel(new BorderLayout());
			pp.add(new Label(res.getString("New_table_name")), BorderLayout.WEST);
			pp.add(tableName, BorderLayout.CENTER);
			p.add(pp, BorderLayout.NORTH);
			sel = new TextArea();
			p.add(sel, BorderLayout.CENTER);
			mainP.add(p);

			updateList();

			OKDialog adia = new OKDialog(CManager.getAnyFrame( /*this*/), res.getString("Select_attributes_to_join"), OKDialog.OK_CANCEL_MODE, true);
			adia.addContent(mainP);
			adia.show();
			if (adia.wasCancelled())
				return;
		}
/**/
		int nattr = 0;
		for (int i = 0; i < toJoin.size(); i++)
			if (attrs[i] != null) {
				nattr += attrs[i].size();
			}
		if (nattr < 1) {
			core.getUI().showMessage(res.getString("No_attributes"), true);
			return;
		}

		Vector aList = new Vector(nattr, 1);
		Vector attrIds = new Vector(nattr, 1);
		Vector tables = new Vector(nattr, 1);

		for (int t = 0; t < toJoin.size(); t++) {
			AttributeDataPortion dt = ((AttributeDataPortion) toJoin.elementAt(t));
			for (int atr = 0; atr < dt.getAttrCount(); atr++) {
				attrIds.addElement(new Integer(atr));
				tables.addElement(dt);
				Attribute at = dt.getAttribute(atr);
				if (attrs[t] != null && attrs[t].contains(at)) {
					aList.addElement(new Object());
				} else {
					aList.addElement(null);
				}
			}
		}
		DataTable resT = new DataTable();
		resT.setName(tableName.getText());
		Vector selAttrs = new Vector();

		Hashtable newParameters = new Hashtable();

		Vector oldAttrs = new Vector();
		Vector newAttrs = new Vector();

		Vector oldParents = new Vector();
		Vector newParents = new Vector();

		Hashtable tableNames = new Hashtable();

		for (int a = 0; a < aList.size(); a++)
			if (aList.elementAt(a) != null) {
				Attribute curAttr = ((DataTable) tables.elementAt(a)).getAttribute(((Integer) attrIds.elementAt(a)).intValue());
				String attrName = curAttr.getName();

				Attribute newAttr = new Attribute(IdMaker.makeId(attrName, resT), curAttr.getType());
				newAttr.setName(attrName);

				if (curAttr.hasParameters()) {
					for (int i = 0; i < curAttr.getParameterCount(); i++) {
						String paramName = curAttr.getParamName(i);
						Parameter curPar;
						if (newParameters.containsKey(paramName)) {
							curPar = (Parameter) newParameters.get(paramName);
						} else {
							curPar = new Parameter();
							curPar.setName(paramName);
							newParameters.put(paramName, curPar);
						}
						curPar.addValue(curAttr.getParamValue(i));
					}
				}

				if (curAttr.getParent() != null) {
/*
          Attribute curParent = curAttr.getParent();
          Attribute equalParent = null;
          boolean meltParents = false;
          for (int i=0; i<oldParents.size(); i++) {
            Attribute previousParent = (Attribute)oldParents.elementAt(i);
            if (previousParent.getName().equals(curParent.getName()) && previousParent.hasChildren() && curParent.hasChildren() &&
                previousParent.getChild(0).getParameterCount() == curParent.getChild(0).getParameterCount()) {
              for (int j=0; j<previousParent.getChild(0).getParameterCount(); j++) {
                if (previousParent.getChild(0).getParamName(j).equals(curParent.getChild(0).getParamName(j))) {
                  meltParents=true;
                  equalParent = previousParent;
                }
                else meltParents=false;
              }
            }
          }

          if (meltParents) {
// table already contains a parent attribute with the same name and the same set of parameters
            Attribute newParent = (Attribute) newParents.elementAt(oldParents.indexOf(equalParent));
            newParent.addChild(newAttr);
          } else
*/
					if (oldParents.contains(curAttr.getParent())) {
						Attribute newParent = (Attribute) newParents.elementAt(oldParents.indexOf(curAttr.getParent()));
						newParent.addChild(newAttr);
					} else {
						oldParents.addElement(curAttr.getParent());
						Attribute newParent = new Attribute(IdMaker.makeId(curAttr.getParent().getName(), resT), curAttr.getParent().getType());
						newParent.setName(curAttr.getParent().getName());
						newParent.addChild(newAttr);
						newParents.addElement(newParent);
						tableNames.put(newParent, ((DataTable) tables.elementAt(a)).getName());
					}
				}
				oldAttrs.addElement(curAttr);
				newAttrs.addElement(newAttr);
				selAttrs.addElement(new Integer(a));
				tableNames.put(newAttr, ((DataTable) tables.elementAt(a)).getName());
			}
/*
// delete single-child parents
    for (int i = 0; i < newParents.size(); i++) {
      Attribute parent = (Attribute) newParents.elementAt(i);
      if ( parent.getChildrenCount() < 2) {
//System.out.println("*** Removing single-child parent");
        parent.removeAllChildren();
        newParents.removeElementAt(i);
        oldParents.removeElementAt(i);
        i--;
      }
    }
*/
		Vector parentsNames = new Vector();
		Vector toRename = new Vector();

// reinitialize identifiers of parent attributes
		for (int i = 0; i < newParents.size(); i++) {
			Attribute parent = (Attribute) newParents.elementAt(i);
			String parentName = parent.getName();
			if (parentsNames.contains(parentName)) {
				Attribute first = (Attribute) newParents.elementAt(parentsNames.indexOf(parentName));
				if (!toRename.contains(first)) {
					toRename.addElement(first);
				}
				parentName += " (" + tableNames.get(parent) + ")";
				parent.setName(parentName);
			}
			parentsNames.addElement(parentName);
			parent.setIdentifier(IdMaker.makeId(parent.getName(), resT));
		}

// finalizing renaming of parent attributes
		if (!toRename.isEmpty()) {
			for (int i = 0; i < toRename.size(); i++) {
				Attribute attr = (Attribute) toRename.elementAt(i);
				String attrName = attr.getName();
				attrName += " (" + tableNames.get(attr) + ")";
				attr.setName(attrName);
			}
/*
// delete single-value parameters
		String single;
		do {
		  Enumeration e = newParameters.keys();
		  single = "";

		  while (e.hasMoreElements() && single.equals("")) {
		    String parName = (String) e.nextElement();
		    Parameter curPar = (Parameter) newParameters.get(parName);
		    if (curPar.getValueCount() < 2) single = parName;
		  }
		  if (!single.equals("")) {
//System.out.println("*** Removing single-value parameter");
		    newParameters.remove(single);
		  }
		} while(!single.equals(""));
*/
		}

// set up values of parameters in children
		for (int i = 0; i < oldAttrs.size(); i++) {
			Attribute oldAttr = (Attribute) oldAttrs.elementAt(i);
			Attribute newAttr = (Attribute) newAttrs.elementAt(i);
			if (oldAttr.hasParameters()) {
				for (int j = 0; j < oldAttr.getParameterCount(); j++) {
					String parName = oldAttr.getParamName(j);
					if (newParameters.containsKey(parName) && newAttr.getParent() != null) {
//System.out.println("*** Adding parameter value "+ oldAttr.getParamName(j)+ " -> "+ oldAttr.getParamValue(j));
						newAttr.addParamValPair(oldAttr.getParamValPair(j));
					}
				}
			}
		}

// add parameters into resulting table
		for (Enumeration e = newParameters.elements(); e.hasMoreElements();) {
			Parameter curPar = (Parameter) e.nextElement();
//System.out.println("*** Adding parameter "+ curPar.getName());
			resT.addParameter(/*(Parameter)e.nextElement()*/curPar);
		}

		toRename.removeAllElements();

// adding reference to the table to attributes with matching names
		for (int i = 0; i < newAttrs.size(); i++) {
			Attribute attr = (Attribute) newAttrs.elementAt(i);
			attr.setIdentifier(IdMaker.makeId(attr.getName(), resT));
			String attrName = attr.getName();
			if (resT.findAttrByName(attrName) != -1) {
				Attribute first = resT.getAttribute(resT.findAttrByName(attrName));
				// memorizing first occurence
				if (!toRename.contains(first)) {
					toRename.addElement(first);
				}
				// renaming second occurence
				attrName += " (" + tableNames.get(attr) + ")";
				attr.setName(attrName);
			}
			resT.addAttribute(attr);
		}

// renaming first occurencies of attributes with matching names
		if (!toRename.isEmpty()) {
			for (int i = 0; i < toRename.size(); i++) {
				Attribute attr = (Attribute) toRename.elementAt(i);
				String attrName = attr.getName();
				attrName += " (" + tableNames.get(attr) + ")";
				attr.setName(attrName);
			}
		}

// testing result
/*
    System.out.println("--- Attributes of the resulting table ---");

    for (int i=0; i < newAttrs.size(); i++) {
      Attribute attr = (Attribute)newAttrs.elementAt(i);
      System.out.println("\nAttribute "+attr.getName());
      System.out.println("Class: "+attr);
      System.out.println("ID: "+attr.getIdentifier());
      Attribute parent = attr.getParent();
      System.out.println("Parent: "+ (parent==null ? "no parent" : parent.getName()));
      if (attr.hasParameters()) {
        for (int j=0; j < attr.getParameterCount(); j++)
          System.out.println("Parameter: "+ attr.getParamName(j)+" -> "+attr.getParamValue(j));
      } else
        System.out.println("No parameters");

//      System.out.println("Parameters"+attr.g);
    }

    System.out.println("-----------------------------------------");
*/

		Vector itemIds = new Vector();
		Vector itemNames = new Vector();

		SystemUI ui = core.getUI();

		for (int tb = 0; tb < tables.size(); tb++) {
			DataTable table = (DataTable) tables.elementAt(tb);
			for (int itm = 0; itm < table.getDataItemCount(); itm++) {
				int curIdx = itemIds.indexOf(table.getDataItemId(itm));
				if (curIdx == -1) {
					itemIds.addElement(table.getDataItemId(itm));
					itemNames.addElement(table.getDataItemName(itm));
				} else if ((String) itemIds.elementAt(curIdx) == (String) itemNames.elementAt(curIdx)) {
					itemNames.setElementAt(table.getDataItemName(itm), curIdx);
				}
			}
			if (ui != null) {
				ui.showMessage(res.getString("Preparing_table_") + tb + res.getString("_of_") + tables.size());
			}
		}

		int total = itemIds.size();
		boolean empty;
		for (int item = 0; item < total; item++) {
			DataRecord rec = new DataRecord((String) itemIds.elementAt(item), (String) itemNames.elementAt(item));
			empty = true;
			for (int aIdx = 0; aIdx < selAttrs.size(); aIdx++) {
				int attrN = ((Integer) selAttrs.elementAt(aIdx)).intValue();
				AttributeDataPortion curTbl = (AttributeDataPortion) tables.elementAt(attrN);
				int curAttr = ((Integer) attrIds.elementAt(attrN)).intValue();
				DataRecord curRec = (DataRecord) curTbl.getDataItem(curTbl.indexOf(rec.getId()));
				if (curRec != null) {
					rec.setAttrValue(curRec.getAttrValue(curAttr), aIdx);
					empty = false;
				}
			}

			if (!empty) {
				resT.addDataRecord(rec);
			}

			if (ui != null && item % 50 == 0) {
				ui.showMessage(res.getString("Calculating") + (int) ((float) item / total * 100) + " %");
			}

		}

		if (resT.hasData()) {
			resT.finishedDataLoading();
		}
		dk.linkTableToMapLayer(core.getDataLoader().addTable(resT), core.getUI().getCurrentMapN(), layer.getContainerIdentifier());

		if (ui != null) {
			ui.showMessage(res.getString("Done"));
		}

		return;
	}

	public void updateList() {
		String s = res.getString("Selected_attributes");
		for (int i = 0; i < toJoin.size(); i++) {
			AttributeDataPortion table = (AttributeDataPortion) toJoin.elementAt(i);
			s += res.getString("From_table") + table.getName() + ":\n";
			boolean empty = true;
			if (attrs[i] != null) {
				for (int j = 0; j < attrs[i].size(); j++) {
					s += ((Attribute) attrs[i].elementAt(j)).getName() + "\n";
					empty = false;
				}
			}
			if (empty) {
				s += res.getString("None") + "\n";
			}
		}
		sel.setText(s);
	}

}
