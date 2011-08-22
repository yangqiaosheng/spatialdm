package ui.bitmap;

import java.awt.BorderLayout;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Choice;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ResourceBundle;

import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Slider;
import spade.lib.lang.Language;
import spade.lib.util.StringUtil;
import ui.ImageSaverProperties;

public class SaveImageDlg extends Panel implements ActionListener, ItemListener, ImageSaverProperties {
	static ResourceBundle res = Language.getTextResource("ui.bitmap.Res");

	protected Checkbox cbSaveMap = null, cbSaveLegend = null, cbTogether = null, cbSaveAsIs = null;

	protected Choice chFormat = null;
	protected TextField tfJPGQuality = null;
	protected Slider slQuality = null;
	protected Panel pSaveOpt = null, pJPEGControl = null, pPNGControl = null;
	//public level
	public String fmt = "jpg";
	public boolean saveMap = true, saveLegend = true, saveAsIs = false;
	public float jpegQuality = 0.85f;
	public int pngCompression = 1;

	public SaveImageDlg() {
		addComponents(true);
	}

	public void addComponents(boolean isApplication) {
		removeAll();
		setLayout(new BorderLayout());
		chFormat = new Choice();
		chFormat.add(res.getString("JPEG_format_JPG"));
		chFormat.add(res.getString("png"));
		if (isApplication) {
			chFormat.add(res.getString("Windows_BitMap_BMP"));
		}
		chFormat.addItemListener(this);
		cbSaveMap = new Checkbox(res.getString("Map"), saveMap);
		cbSaveLegend = new Checkbox(res.getString("Legend"), saveLegend);
		cbTogether = new Checkbox(res.getString("together"), false);
		// following string: "Save map as on the screen"
		cbSaveAsIs = new Checkbox(res.getString("Save_map_as_on_the"), saveAsIs);
		cbSaveMap.addItemListener(this);
		cbSaveLegend.addItemListener(this);
		cbSaveAsIs.addItemListener(this);

		Panel pSaveWhat = new Panel(new GridLayout(5, 1, 0, 0));
		pSaveWhat.add(new Label(res.getString("Select_what_to_save_"), Label.CENTER));
		pSaveWhat.add(cbSaveMap);
		pSaveWhat.add(cbSaveLegend);
		cbSaveMap.addItemListener(this);
		cbSaveLegend.addItemListener(this);
		Panel ptg = new Panel();
		ptg.setLayout(new BorderLayout());
		Panel tab = new Panel();
		ptg.add(tab, "West");
		ptg.add(cbTogether, "Center");
		pSaveWhat.add(ptg);

		pSaveWhat.add(new spade.lib.basicwin.Line(false));

		slQuality = new Slider(this, 0, 100, Math.round(100 * jpegQuality));
		slQuality.setPreferredSize(170, slQuality.getPreferredSize().height);
		slQuality.setNAD(true);
		tfJPGQuality = new TextField(StringUtil.doubleToStr(slQuality.getValue(), slQuality.getAbsMin(), slQuality.getAbsMax()), 7);
		tfJPGQuality.addActionListener(this);

		Panel p = new Panel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		p.add(new Label(res.getString("JPEG_Quality_"), Label.RIGHT));
		p.add(tfJPGQuality);

		Panel pQualityLevels = new Panel(new BorderLayout());
		pQualityLevels.add(new Label(res.getString("Poor"), Label.CENTER), "West");
		pQualityLevels.add(new Label(res.getString("Average"), Label.CENTER), "Center");
		pQualityLevels.add(new Label(res.getString("Best"), Label.CENTER), "East");

		ColumnLayout clPQuality = new ColumnLayout();
		clPQuality.setAlignment(ColumnLayout.Hor_Stretched);
		Panel pJPGQuality = new Panel(clPQuality);
		pJPGQuality.add(p);
		pJPGQuality.add(slQuality);
		pJPGQuality.add(pQualityLevels);

		pJPEGControl = new Panel(new BorderLayout());
		pJPEGControl.add(pJPGQuality, "North");
		pJPEGControl.add(pQualityLevels, "Center");

		pPNGControl = new Panel(new BorderLayout());

		ColumnLayout clPNGCompression = new ColumnLayout();
		clPNGCompression.setAlignment(ColumnLayout.Hor_Stretched);

		Panel pPNGCompressionOptions = new Panel(clPNGCompression);
		CheckboxGroup cbgPNGComp = new CheckboxGroup();
		Checkbox cbComp = null;
		cbComp = new Checkbox(res.getString("No"), cbgPNGComp, false);
		cbComp.addItemListener(this);
		pPNGCompressionOptions.add(cbComp);
		cbComp = new Checkbox(res.getString("Fast"), cbgPNGComp, false);
		cbComp.addItemListener(this);
		pPNGCompressionOptions.add(cbComp);
		cbComp = new Checkbox(res.getString("Default"), cbgPNGComp, true);
		cbComp.addItemListener(this);
		pPNGCompressionOptions.add(cbComp);
		cbComp = new Checkbox(res.getString("Max"), cbgPNGComp, false);
		cbComp.addItemListener(this);
		pPNGCompressionOptions.add(cbComp);

		pPNGControl.add(new Label(res.getString("Compression"), Label.CENTER), "North");
		pPNGControl.add(pPNGCompressionOptions, "Center");

		pSaveOpt = new Panel(new GridLayout(2, 1, 0, 0));
		//pSaveOpt.add(new Label("Size",Label.CENTER));
		//pSaveOpt.add(cbSaveAsIs);
		pSaveOpt.add(new Label(res.getString("Format_"), Label.CENTER));
		pSaveOpt.add(chFormat);
//    add(pSaveWhat,"North");
		add(pSaveOpt, "Center");
		add(pJPEGControl, "South");
	}

	@Override
	public void actionPerformed(ActionEvent ae) {
		Object src = ae.getSource();
		if (src instanceof Slider) {
			jpegQuality = (float) slQuality.getValue() / 100.00f;
			tfJPGQuality.setText(StringUtil.doubleToStr(slQuality.getValue(), slQuality.getAbsMin(), slQuality.getAbsMax()));
		}
		if (src instanceof TextField) {
			String sQuality = tfJPGQuality.getText();
			float val = (float) slQuality.getValue() / 100.00f;
			try {
				val = Float.valueOf(sQuality).floatValue();
			} catch (NumberFormatException nfe) {
				val = (float) slQuality.getValue() / 100.00f;
			}
			if (val != slQuality.getValue() / 100.00f) {
				slQuality.setValue(val);
			}
			tfJPGQuality.setText(StringUtil.doubleToStr(slQuality.getValue(), slQuality.getAbsMin(), slQuality.getAbsMax()));
		}
	}

	@Override
	public void itemStateChanged(ItemEvent ie) {
		Object src = ie.getSource();
		if (src instanceof Checkbox) {
			Checkbox cb = (Checkbox) src;
			if (cb.equals(cbSaveMap)) {
				saveMap = cbSaveMap.getState();
			}
			if (cb.equals(cbSaveLegend)) {
				saveLegend = cbSaveLegend.getState();
			}
			if (cb.equals(cbSaveAsIs)) {
				saveAsIs = cbSaveAsIs.getState();
			}
			if (cb.equals(cbSaveMap) || cb.equals(cbSaveLegend)) {
				if (cbSaveMap.getState() && cbSaveLegend.getState()) {
					cbTogether.setEnabled(true);
				} else {
					cbTogether.setEnabled(false);
					cbTogether.setState(false);
				}
			}
			if (cb.getLabel().equalsIgnoreCase(res.getString("No"))) {
				pngCompression = 0;
			} else if (cb.getLabel().equalsIgnoreCase(res.getString("Default"))) {
				pngCompression = 1;
			} else if (cb.getLabel().equalsIgnoreCase(res.getString("Fast"))) {
				pngCompression = 2;
			} else if (cb.getLabel().equalsIgnoreCase(res.getString("Max"))) {
				pngCompression = 3;
			}
			System.out.println("SaveImageDialog::PNGCompression=" + pngCompression);
		} else if (src instanceof Choice) {
			fmt = chFormat.getSelectedItem();
			fmt = fmt.substring(fmt.lastIndexOf(" ") + 1, fmt.length());
			fmt = fmt.toLowerCase();
			System.out.println("Selected format: " + fmt);
			setJPEGControlsEnabled(fmt.equalsIgnoreCase("jpg"));
			setPNGControlsEnabled(fmt.equalsIgnoreCase("png"));
		}
	}

	private void setJPEGControlsEnabled(boolean flag) {
		if (flag) {
			add(pJPEGControl, "South");
		} else {
			remove(pJPEGControl);
		}
		pJPEGControl.setVisible(flag);
		pJPEGControl.setEnabled(flag);
		CManager.getWindow(this).pack();
	}

	private void setPNGControlsEnabled(boolean flag) {
		if (flag) {
			add(pPNGControl, "South");
		} else {
			remove(pPNGControl);
		}
		pPNGControl.setVisible(flag);
		pPNGControl.setEnabled(flag);
		CManager.getWindow(this).pack();
	}

	// implementstion of the interface
	@Override
	public String getSelectedFormat() {
		return fmt;
	}

	@Override
	public boolean isSaveMap() {
		return cbSaveMap.getState();
	}

	@Override
	public boolean isSaveLegend() {
		return cbSaveLegend.getState();
	}

	@Override
	public boolean isSaveMapAsIs() {
		return cbSaveAsIs.getState();
	}

	@Override
	public boolean isSaveMapAndLegend() {
		return cbTogether.getState();
	}

	@Override
	public float getJPEGQuality() {
		return jpegQuality;
	}

	@Override
	public int getPNGCompression() {
		return pngCompression;
	}

	@Override
	public String fmt2MimeType(String fmtExt) {
		if (fmtExt == null || fmtExt.length() < 1 || fmtExt.length() > 4)
			return null;
		if (fmtExt.equalsIgnoreCase("jpg") || fmtExt.equalsIgnoreCase("jpeg"))
			return "image/jpeg";
		else if (fmtExt.equalsIgnoreCase("tif") || fmtExt.equalsIgnoreCase("tiff"))
			return "image/tiff";
		return "image/" + fmtExt;
	}
}
