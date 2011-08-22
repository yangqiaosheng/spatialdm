package spade.analysis.decision;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.ScrollPane;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.system.Supervisor;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.OKDialog;
import spade.lib.basicwin.OwnList;
import spade.lib.basicwin.TImgButton;
import spade.lib.basicwin.TriangleDrawer;
import spade.lib.color.ColorCanvas;
import spade.lib.lang.Language;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataTable;

/**
* Shows the user the current ranking (and, possibly, classification) of
* decision options and allows her/him to edit it, i.e. to change the order
* of the options.
*/
public class DecisionPanel extends Panel implements ActionListener, ItemListener, PropertyChangeListener {
	static ResourceBundle res = Language.getTextResource("spade.analysis.decision.Res");
	/**
	* The table with the options and their characteristics
	*/
	protected DataTable dTable = null;
	protected Supervisor supervisor = null;
	/**
	* The identifier of the column (attribute) with a ranking or classification
	* of the options
	*/
	protected String attrId = null;
	/**
	* The identifier of the current user (got from applet parameter "User")
	* Needed for voting's user identification
	*/
	protected String userId = null;
	protected Label userIdL = null;
	/**
	* A component that keeps and displays the list of options.
	*/
	protected OptionList optList = null;
	/**
	* The interface element for manipulating the list of options
	*/
	protected OwnList list = null;
	/**
	* This checkbox indicates whether option classification should be used;
	* otherwise, ranking is applied
	*/
	protected Checkbox classCB = null;
	/**
	* The edit control for specification of the number of classes
	*/
	protected TextField classNTF = null;
	/**
	* The edit controls for specification of the number of items in each class
	*/
	protected TextField cntf[] = null;
	/**
	* The panel in which statistics about current classes is shown
	*/
	protected Panel statP = null;
	/**
	* The panel in which userID is shown and can be changed
	*/
	protected Panel userIdP = null;

	/**
	* Constructs a DecisionPanel for the given table and the given attribute
	* specifying initial ranking or classification
	*/
	public DecisionPanel(DataTable table, String attr, Supervisor sup, boolean useClasses) {
		dTable = table;
		attrId = attr;
		supervisor = sup;
		if (dTable == null || attrId == null || dTable.getDataItemCount() < 1)
			return;
		int attrIdx = dTable.getAttrIndex(attrId);
		if (attrIdx < 0)
			return;
		if (!AttributeTypes.isNumericType(dTable.getAttributeType(attrIdx)))
			return;
		optList = new OptionList();
		if (useClasses && dTable.getAttributeOrigin(attrIdx) == AttributeTypes.classify_order) {
			optList.setUseClasses(true);
		}
		for (int i = 0; i < dTable.getDataItemCount(); i++) {
			float val = (float) dTable.getNumericAttrValue(attrIdx, i);
			if (!Float.isNaN(val)) {
				optList.addOption(i, dTable.getDataItemId(i), dTable.getDataItemName(i), Math.round(val));
			}
		}
		if (optList.getOptionCount() < 1)
			return;
		optList.setUseClasses(useClasses);
		optList.addPropertyChangeListener(this);
		optList.setObjectCollectionId(dTable.getEntitySetIdentifier());
		optList.setTableId(dTable.getContainerIdentifier());
		optList.setObjectEventHandler(sup);
		optList.setHighlighter(sup.getHighlighter(dTable.getEntitySetIdentifier()));
		list = new OwnList(optList);
		list.setNItems(optList.getOptionCount());
		Panel pan = new Panel(new BorderLayout());
		pan.add(list, "Center");
		Vector attrList = dTable.getAttributeDependencyList(attrIdx);
		if (attrList != null) {
			if (dTable.getAttributeOrigin(attrIdx) == AttributeTypes.classify_order && attrList.size() == 1) {
				int n = dTable.getAttrIndex((String) attrList.elementAt(0));
				if (n >= 0) {
					Vector v = dTable.getAttributeDependencyList(n);
					if (v != null && v.size() > 0) {
						attrList = v;
					}
				}
			}
			optList.setAttributeList(attrList);
			Vector colors = new Vector(attrList.size(), 1);
			for (int i = 0; i < attrList.size(); i++) {
				colors.addElement(supervisor.getColorForAttribute((String) attrList.elementAt(i)));
			}
			optList.setAttributeColors(colors);
		}
		optList.setListDrawer(list.getListDrawCanvas());
		Panel p = new Panel(new FlowLayout(FlowLayout.CENTER, 20, 2));
		TriangleDrawer td = new TriangleDrawer(TriangleDrawer.N);
		td.setPreferredSize(12, 12);
		td.setMargins(1, 1);
		TImgButton ib = new TImgButton(td);
		p.add(ib);
		ib.setActionCommand("up");
		ib.addActionListener(this);
		td = new TriangleDrawer(TriangleDrawer.S);
		td.setPreferredSize(12, 12);
		td.setMargins(1, 1);
		ib = new TImgButton(td);
		p.add(ib);
		ib.setActionCommand("down");
		ib.addActionListener(this);
		pan.add(p, "South");
		setLayout(new BorderLayout());
		pan.setBackground(Color.white);
		add(pan, "West");
		Panel pp = new Panel(new ColumnLayout());
		pp.add(new Label(res.getString("Decision_type_")));
		CheckboxGroup cbg = new CheckboxGroup();
		Checkbox cb = new Checkbox(res.getString("ranking"), cbg, !useClasses);
		cb.addItemListener(this);
		pp.add(cb);
		classCB = new Checkbox(res.getString("classification"), cbg, useClasses);
		classCB.addItemListener(this);
		pp.add(classCB);
		p = new Panel(new BorderLayout());
		p.add(new Label(res.getString("Nof_classes_"), Label.RIGHT), "Center");
		classNTF = new TextField(String.valueOf(optList.getClassCount()), 2);
		classNTF.setEnabled(useClasses);
		classNTF.addActionListener(this);
		p.add(classNTF, "East");
		pp.add(p);
		statP = new Panel(new ColumnLayout());
		if (useClasses) {
			showClassStatistics();
		}
		ScrollPane scp = new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED);
		scp.add(statP);
		pan = new Panel(new BorderLayout());
		pan.add(pp, "North");
		pan.add(scp, "Center");
		pan.setBackground(Color.lightGray);
		add(pan, "East");

		Panel uidLayoutP = new Panel(new ColumnLayout());
		userIdP = new Panel(new BorderLayout());
		Button bChangeUser = new Button(res.getString("Change"));
		bChangeUser.addActionListener(this);
		bChangeUser.setActionCommand("change_userId");
		userId = sup.getSystemSettings().getParameterAsString("User");

		if (userId == null || userId.length() < 1) {
			userId = "*** ??? ***";
		} else {
			bChangeUser.setEnabled(false);
		}

		userIdL = new Label(res.getString("User_ID_") + userId, Label.LEFT);
		userIdP.add(userIdL, "Center");
		userIdP.add(bChangeUser, "East");
		uidLayoutP.add(userIdP);
		uidLayoutP.add(new spade.lib.basicwin.Line(false));
		add(uidLayoutP, "North");
	}

	/**
	* Replies whether there are any options in the list
	*/
	public boolean hasContents() {
		return optList != null && optList.getOptionCount() > 0;
	}

	/**
	* Displays number of elements in each class
	*/
	protected void showClassStatistics() {
		boolean useClasses = classCB.getState();
		if (!useClasses) {
			statP.removeAll();
			cntf = null;
			classNTF.setText("0");
		} else {
			int nclasses = optList.getClassCount();
			classNTF.setText(String.valueOf(nclasses));
			if (cntf == null || cntf.length != nclasses) {
				statP.removeAll();
				cntf = new TextField[nclasses];
				for (int i = 0; i < nclasses; i++) {
					Panel p = new Panel(new BorderLayout());
					ColorCanvas cc = new ColorCanvas();
					cc.setColor(optList.getClassColor(i));
					p.add(cc, "West");
					String clName = String.valueOf(i + 1);
					if (i == 0) {
						clName += res.getString("_good_");
					} else if (i == nclasses - 1) {
						clName += res.getString("_bad_");
					}
					p.add(new Label(clName), "Center");
					cntf[i] = new TextField(String.valueOf(optList.getClassElemCount(i)), 2);
					cntf[i].addActionListener(this);
					p.add(cntf[i], "East");
					statP.add(p);
				}
			} else {
				for (int i = 0; i < nclasses; i++) {
					cntf[i].setText(String.valueOf(optList.getClassElemCount(i)));
				}
			}
		}
		if (isShowing()) {
			CManager.validateAll(statP);
		}
	}

	/**
	* Reacts to changes of classification
	*/
	@Override
	public void propertyChange(PropertyChangeEvent e) {
		if (e.getPropertyName().equals("classes")) {
			showClassStatistics();
		}
	}

	/**
	* Reacts to the switch between classification and ranking
	*/
	@Override
	public void itemStateChanged(ItemEvent e) {
		boolean useClasses = classCB.getState();
		optList.setUseClasses(useClasses);
		classNTF.setEnabled(useClasses);
		showClassStatistics();
	}

	/**
	* Reacts to buttons
	*/
	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() instanceof TextField) {
			TextField tf = (TextField) e.getSource();
			int num = -1;
			try {
				num = Integer.valueOf(tf.getText().trim()).intValue();
			} catch (NumberFormatException nfe) {
			}
			if (num >= 0)
				if (tf == classNTF) {
					optList.setClassNumber(num);
				} else {
					int clIdx = -1;
					for (int i = 0; i < cntf.length && clIdx < 0; i++)
						if (tf == cntf[i]) {
							clIdx = i;
						}
					if (clIdx >= 0) {
						optList.setClassSize(clIdx, num);
					}
				}
			showClassStatistics();
			return;
		}
		boolean up = e.getActionCommand().equals("up");
		if (up || e.getActionCommand().equals("down")) {
			int n = list.getSelectedIndex(), n1 = (up) ? n - 1 : n + 1;
			if (n < 0 || n1 < 0 || n1 >= optList.getOptionCount())
				return;
			if (up) {
				optList.optionUp(n);
			} else {
				optList.optionDown(n);
			}
			list.repaintItem(n);
			list.setSelected(n1);
		}
		if (e.getActionCommand().equals("change_userId")) {
			userId = changeUserID();
		}
	}

	/**
	* Returns the result of ordering or classification of the options: an array
	* in which for each option (according to its index in the table) its order is
	* given (an integer number starting from 1)
	*/
	public int[] getResult() {
		if (classCB.getState())
			return optList.getOptionsClasses();
		return optList.getOptionsOrder();
	}

	/**
	* Returns the ID of current decision maker (user)
	* Normally provided by Zeno system through applet's parameter User.
	* If it is not such case must be entered in dialog by user (changeUserID)
	*/
	public String getUserID() {
		return userId;
	}

	public String changeUserID() {
		OKDialog changeUserDlg = new OKDialog(CManager.getAnyFrame(this), res.getString("specify_your"), false);
		Panel pChangeUserDlg = new Panel(new FlowLayout());
		pChangeUserDlg.add(new Label(res.getString("User_ID_"), Label.CENTER));
		TextField tfUserID = new TextField(userId, 10);
		pChangeUserDlg.add(tfUserID);
		changeUserDlg.addContent(pChangeUserDlg);
		String userEntered = "";
		boolean wrongUserID = true;
		while (wrongUserID) {
			changeUserDlg.show();
			userEntered = tfUserID.getText();
			wrongUserID = (userEntered.length() < 1 || userEntered.indexOf("*") > -1 || userEntered.indexOf("%") > -1 || userEntered.indexOf("&") > -1 || userEntered.indexOf("$") > -1 || userEntered.indexOf("?") > -1);
		}
		userIdL.setText(res.getString("User_ID_") + userEntered);
		return userEntered;
	}

	/**
	* Replies whether the result is ranking or classification
	*/
	public boolean isResultClassification() {
		return classCB.getState();
	}
}