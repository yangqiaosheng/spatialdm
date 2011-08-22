package spade.vis.preference;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Label;
import java.awt.MediaTracker;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.GetPathDlg;
import spade.lib.basicwin.ItemPainter;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.NotificationLine;
import spade.lib.basicwin.OKDialog;
import spade.lib.basicwin.OwnList;
import spade.lib.basicwin.StringInRectangle;
import spade.lib.lang.Language;
import spade.vis.geometry.GeomSign;

/**
* This panel allows the user to set or edit a correspondence between values
* of a qualitative attribute or combinations of values of several attributes
* and icons (images) to be used for representation of these values (or
* combinations) on a map.
*/
public class SelectIconsPanel extends Panel implements ActionListener, ItemPainter {
	/**
	* Contains identifiers of the attributes.
	*/
	static ResourceBundle res = Language.getTextResource("spade.vis.preference.Res");
	protected Vector attrIds = null;

	/**
	* Contains names of the attributes
	*/
	protected Vector attrNames = null;

	/**
	* This Vector contains for each attribute an array with all its values
	* (without repetitions)
	*/
	protected Vector attrVal = null;

	/**
	* The specified correspondence between attribute values or combinations and
	* icons to be edited in the dialog.
	*/
	protected IconCorrespondence icorr = null;

	/**
	* Represents currently defined correspondences
	*/
	protected OwnList corrList = null;

	/**
	* The status label
	*/
	protected NotificationLine lStatus = null;

	/**
	* Constructs the panel for setting or editing a correspondence between
	* attribute values and icons. The argument icorr contains the previously
	* existing correspondences (if any). The panel needs also a vector with the
	* identifiers of all the attributes, a vector with their names, and a vector
	* where for each attribute there is an array with all its possible values.
	*/
	public SelectIconsPanel(IconCorrespondence icorr, Vector attrIds, Vector attrNames, Vector attrVal) {
		if (icorr != null) {
			this.icorr = (IconCorrespondence) icorr.clone();
		}
		this.attrIds = attrIds;
		this.attrNames = attrNames;
		this.attrVal = attrVal;
		//construct the interior
		setLayout(new BorderLayout());
		Panel mainP = new Panel(new ColumnLayout());
		add(mainP, "Center");
		lStatus = new NotificationLine("");
		mainP.add(lStatus);
		// following string: "Set the icons to represent values of attribute"
		mainP.add(new Label(res.getString("Set_the_icons_to") + ((attrIds.size() < 2) ? ":" : res.getString("s"))));
		for (int i = 0; i < attrIds.size(); i++) {
			Panel p = new Panel(new BorderLayout());
			if (attrIds.size() > 1) {
				p.add(new Label(String.valueOf(i + 1) + ")"), "West");
			}
			p.add(new Label((String) attrNames.elementAt(i)), "Center");
			mainP.add(p);
		}
		add(mainP, "North");
		corrList = new OwnList(this);
		add(corrList, "Center");
		if (icorr != null) {
			corrList.setNItems(icorr.getCorrCount());
		}
		Panel p = new Panel(new FlowLayout(FlowLayout.CENTER, 0, 3));
		// following string:
		Button b = new Button(res.getString("Edit"));
		b.setActionCommand("edit");
		b.addActionListener(this);
		p.add(b);
		add(p, "South");
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		lStatus.showMessage(null, false);
		String cmd = e.getActionCommand();
		if (cmd == null)
			return;
		if (cmd.equals("edit")) {
			int corrN = corrList.getSelectedIndex();
			IconVisSpec isp = icorr.getCorrespondence(corrN);
			if (corrN < 0)
				return;
			// following string: "How to draw this combination ?"
			OKDialog dlg = new OKDialog(CManager.getAnyFrame(this), res.getString("How_to_draw_this"), false);
			Panel p = new Panel();
			p.setLayout(new ColumnLayout());
			for (int i = 0; i < attrIds.size(); i++) {
				p.add(new Label((String) attrNames.elementAt(i) + "=" + isp.getAttrValue((String) attrIds.elementAt(i))));
			}
			p.add(new Line(false));
			CheckboxGroup chbg = new CheckboxGroup();
			Checkbox cb[] = new Checkbox[3];
			// following string: "Do not show"
			p.add(cb[0] = new Checkbox(res.getString("Do_not_show"), isp.getIcon() == null, chbg));
			// following string: "Geometric sign ..."
			p.add(cb[1] = new Checkbox(res.getString("Geometric_sign_"), isp.getIcon() instanceof GeomSign, chbg));
			p.add(cb[2] = new Checkbox("GIF/JPG ...", isp.getIcon() instanceof Image, chbg));
			dlg.addContent(p);
			dlg.show();
			if (cb[0].getState()) {
				isp.setIcon(null);
			} else if (cb[1].getState()) {
				GeomSign gs = null;
				if (isp.getIcon() instanceof GeomSign) {
					gs = (GeomSign) isp.getIcon();
				}
				IconControlPanel icp = new IconControlPanel(gs);
				// following string: "generate icon"
				OKDialog icpdlg = new OKDialog(CManager.getAnyFrame(this), res.getString("generate_icon"), false);
				icpdlg.addContent(icp);
				icpdlg.show();
				isp.setIcon(icp.getGeneratedSign());
				isp.setPathToImage(null);
			} else { // cb[2].getState()
				GetPathDlg pdlg = new GetPathDlg(CManager.getAnyFrame(this),
				// following string:"Specify the file with the icon"
						res.getString("Specify_the_file_with"));
				pdlg.setFileMask("*.gif;*.jpeg");
				pdlg.show();
				String path = pdlg.getPath();
				if (path == null) {
					isp.setIcon(null);
				} else {
					Image img = getToolkit().getImage(path);
					MediaTracker mt = new MediaTracker(this);
					mt.addImage(img, 0);
					try {
						mt.waitForID(0);
					} catch (InterruptedException ie) {
						// following string: "Could not load the image "+path+":\n  "+ie
						String msg = res.getString("Could_not_load_the") + path + ":\n  " + ie;
						lStatus.showMessage(msg, true);
						System.out.println(msg);
						isp.setIcon(null);
						corrList.repaintItem(corrN);
						return;
					}
					isp.setIcon(img);
					isp.setPathToImage(path);
				}
			}
			corrList.repaintItem(corrN);
		}
	}

	public IconCorrespondence getCorrespondence() {
		return icorr;
	}

//----------------- ItemPainter interface --------------------------------------
	/**
	* Returns the height of the item (an item includes an icon and an attribute
	* name)
	*/
	@Override
	public int itemH() {
		return 24;
	}

	/**
	* Returns the width of the item (depending on the maximum length among
	* attribute names.
	*/
	@Override
	public int maxItemW() {
		return 300;
	}

	/**
	* Draws an item of the list including an icon and an attribute name
	*/
	@Override
	public void drawItem(Graphics g, int n, int x, int y, int w, boolean isActive) {
		if (icorr == null || n >= icorr.getCorrCount())
			return;
		g.setColor((isActive) ? Color.blue.darker() : getBackground());
		g.fillRect(x, y, w + 1, itemH() + 1);
		IconVisSpec isp = icorr.getCorrespondence(n);
		if (isp.getIcon() == null) {
			isp.loadImage(this);
		}
		if (isp.getIcon() != null)
			if (isp.getIcon() instanceof Image) {
				Image img = (Image) isp.getIcon();
				int xmarg = (itemH() - img.getWidth(null)) / 2, ymarg = (itemH() - img.getHeight(null)) / 2;
				g.drawImage(img, x + xmarg, y + ymarg, null);
			} else if (isp.getIcon() instanceof GeomSign) {
				GeomSign sign = (GeomSign) isp.getIcon();
				int xmarg = (itemH() - sign.getWidth()) / 2, ymarg = (itemH() - sign.getHeight()) / 2;
				sign.draw(g, x + xmarg + sign.getWidth() / 2, y + ymarg + sign.getHeight() / 2);
			}
		//construct the text with attribute values
		String txt = null;
		if (isp.isDefault()) {
			txt = "<default icon>";
		} else {
			txt = "";
			for (int i = 0; i < attrIds.size(); i++) {
				if (i > 0) {
					txt += ";";
				}
				String val = isp.getAttrValue((String) attrIds.elementAt(i));
				if (attrIds.size() > 1) {
					txt += String.valueOf(i + 1) + ") ";
				}
				if (val != null) {
					txt += val;
				} else {
					txt += res.getString("any_value");
				}
			}
		}
		g.setColor((isActive) ? Color.white : getForeground());
		StringInRectangle.drawText(g, txt, itemH() + 1, y, w, false);
	}

	/**
	* Writes a text when there is no item in the list
	*/
	@Override
	public void drawEmptyList(Graphics g, int x, int y, int w, int h) {
		g.setColor(Color.red.darker());
		// following string:"No correspondences yet defined"
		String txt = res.getString("No_correspondences");
		int sw = g.getFontMetrics().stringWidth(txt);
		g.drawString(txt, x + (w - sw) / 2, y + h / 2);
	}
}
