package spade.lib.font;

import java.awt.BorderLayout;
import java.awt.Checkbox;
import java.awt.Choice;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.Toolkit;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.lang.reflect.Method;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.lib.lang.Language;
import spade.vis.dmap.DrawingParameters;

public class FontSelectDlg extends Panel implements ItemListener {

	static ResourceBundle res = Language.getTextResource("spade.lib.font.Res");
	public Vector flist = null;
	protected Checkbox cbUnderlined = null, cbShadowed = null, cbDynApply = null;
	protected Choice chFonts = null, chFontSizes = null, chFontStyles = null;
	public Font fntSelected = null;
	public Color colorFnt = null;
	protected Color colorShadow = null;
	protected String fontName = null;
	protected int fontSize = 10, fontStyle = Font.PLAIN, fontOption = DrawingParameters.NORMAL;
	protected boolean fontUnderlined = false, fontShadowed = false;

	protected FontExample example = null;

	public FontSelectDlg(Font fntCurrent, int fontOption, Color colorFnt) {
		super();
		setLayout(new BorderLayout());
		this.fontOption = fontOption;
		fontUnderlined = (fontOption == DrawingParameters.UNDERLINED || fontOption == (DrawingParameters.UNDERLINED | DrawingParameters.SHADOWED));
		fontShadowed = (fontOption == DrawingParameters.SHADOWED || fontOption == (DrawingParameters.UNDERLINED | DrawingParameters.SHADOWED));
		this.colorFnt = colorFnt;
		colorShadow = ((colorFnt.getRed() + colorFnt.getGreen() + colorFnt.getBlue()) > 3 * 128) ? Color.black : Color.white;
		Panel pFontSelect = new Panel(new GridLayout(7, 1, 0, 0));
		chFonts = new Choice();
		chFontSizes = new Choice();
		chFontStyles = new Choice();
		// following string: "Underlined"
		cbUnderlined = new Checkbox(res.getString("Underlined"), fontUnderlined);
		cbUnderlined.addItemListener(this);
		// following string:"Shadowed
		cbShadowed = new Checkbox(res.getString("Shadowed"), fontShadowed);
		cbShadowed.addItemListener(this);
		// following string:"Dynamic apply"
		cbDynApply = new Checkbox(res.getString("Dynamic_apply"), true);
		cbDynApply.addItemListener(this);

		Panel p = new Panel(new FlowLayout(FlowLayout.LEFT, 0, 2));
		// following string:"Font"
		p.add(new Label(res.getString("Font"), Label.CENTER));
		p.add(chFonts);
		pFontSelect.add(p);
		p = new Panel(new FlowLayout(FlowLayout.LEFT, 0, 2));
		// following string:"Style"
		p.add(new Label(res.getString("Style"), Label.CENTER));
		p.add(chFontStyles);
		pFontSelect.add(p);
		p = new Panel(new FlowLayout(FlowLayout.LEFT, 0, 2));
		// following string:"Size"
		p.add(new Label(res.getString("Size"), Label.CENTER));
		p.add(chFontSizes);
		pFontSelect.add(p);
		pFontSelect.add(cbUnderlined);
		pFontSelect.add(cbShadowed);
		pFontSelect.add(new Label("", Label.CENTER));
		pFontSelect.add(cbDynApply);

		add(pFontSelect, "North");
		// font example drawing
		example = new FontExample(fntCurrent, colorFnt);
		example.setStyle(fontOption);
		addFontListener(example);
		add(example, "Center");
		// following string:"Normal"
		chFontStyles.add(res.getString("Normal"));
		// following string:"Bold"
		chFontStyles.add(res.getString("Bold"));
		// following string:"Italic"
		chFontStyles.add(res.getString("Italic"));
		// following string:"Bold Italic"
		chFontStyles.add(res.getString("Bold_Italic"));
		for (int i = 4; i <= 72; i++) {
			chFontSizes.add(Integer.toString(i));
		}
		String sFontList[] = null;
		try {
			// Java 1.2
			//String[] sFontsList=GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
			Class ge = Class.forName("java.awt.GraphicsEnvironment");
			if (ge != null) {
				Method glge = ge.getMethod("getLocalGraphicsEnvironment", null);
				if (glge != null) {
					Object oLGE = glge.invoke(null, null);
					if (oLGE != null) {
						Method gaffn = oLGE.getClass().getMethod("getAvailableFontFamilyNames", null);
						if (gaffn != null) {
							sFontList = (String[]) gaffn.invoke(oLGE, null);
						}
					}
				}
			}
		} catch (Exception ex) {
		}
		if (sFontList == null) {
			// Java 1.1 compatible:
			sFontList = Toolkit.getDefaultToolkit().getFontList();
		}

		for (String element : sFontList) {
			chFonts.add(element);
		}
		if (fntCurrent != null) {
			chFonts.select(fntCurrent.getName());
			chFontSizes.select(Integer.toString(fntCurrent.getSize()));
			chFontStyles.select(fntCurrent.getStyle());
			example.setFont(fntCurrent);
		}
		chFonts.addItemListener(this);
		chFontSizes.addItemListener(this);
		chFontStyles.addItemListener(this);
	}

	public void addFontListener(FontListener fl) {
		if (flist == null) {
			flist = new Vector(2, 2);
		} else {
			for (int i = 0; i < flist.size(); i++)
				if (flist.elementAt(i).equals(fl)) {
					System.out.println("WARNING: Listener " + fl + " was already added!");
					return;
				}
		}
		flist.addElement(fl);
	}

	public void removeFontListener(FontListener fl) {
		if (flist == null)
			return;
		else {
			for (int i = 0; i < flist.size(); i++)
				if (flist.elementAt(i).equals(fl)) {
					flist.removeElementAt(i);
					return;
				}
		}
	}

	public void notifyFontChanged() {
		FontListener fl;
		boolean dynApply = cbDynApply.getState();
		for (int i = 0; i < flist.size(); i++) {
			fl = (FontListener) (flist.elementAt(i));
			if (dynApply || (fl instanceof FontExample)) {
				fl.fontChanged(fntSelected, fontOption, this);
			}
		}
	}

	@Override
	public void itemStateChanged(ItemEvent ie) {
		Object src = ie.getSource();
		if (src instanceof Choice || fntSelected == null) {
			fontName = chFonts.getSelectedItem();
			fontStyle = chFontStyles.getSelectedIndex();
			/* switch (fontStyle) {
			  case 0: fontStyle=Font.PLAIN; break;
			  case 1: fontStyle=Font.BOLD; break;
			  case 2: fontStyle=Font.ITALIC; break;
			  case 3: fontStyle=Font.BOLD | Font.ITALIC;
			}  */
			try {
				fontSize = Integer.parseInt(chFontSizes.getSelectedItem());
			} catch (NumberFormatException nfe) {
				fontSize = 10;
			}
			fntSelected = new Font(fontName, fontStyle, fontSize);
		}
		fontUnderlined = cbUnderlined.getState();
		fontShadowed = cbShadowed.getState();
		fontOption = (fontUnderlined ? 1 : 0) | (fontShadowed ? 2 : 0);
		System.out.println("Selected font: " + fontName + " style " + fontStyle + " size " + fontSize + " option " + fontOption);
		notifyFontChanged();
	}

	public void setCurrentFontColor(Color colorFnt) {
		if (example != null) {
			example.setColor(colorFnt);
		}
		this.colorFnt = colorFnt;
	}
}
