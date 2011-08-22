package spade.lib.color;

import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ResourceBundle;

import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.FocusListener;
import spade.lib.basicwin.Focuser;
import spade.lib.basicwin.FocuserCanvas;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.Slider;
import spade.lib.lang.Language;
import spade.lib.util.StringUtil;

/**
* An interface component to manipulate parameters of color scales
*/

public class ColorScaleManipulator extends Panel implements FocusListener, ActionListener, ItemListener, PropertyChangeListener {
	/**
	* Current color scale
	*/
	static ResourceBundle res = Language.getTextResource("spade.lib.color.Res");
	protected ColorScale cscale = null;
	protected ColorScaleRegister csreg = new ColorScaleRegister();

	protected Label alphaLabel = null;
	protected Focuser f = null;
	protected Slider sl = null;
	protected Checkbox scaleTypeCB[] = null;
//  protected Checkbox dynUpd=null;

	protected boolean ready = false;
//  protected boolean dynamic=false;
//----------------- notification about changes of the color scale---------------
	/**
	* A ColorScaleManipulator may have a number of listeners of changes
	* of the color scale (PropertyChangeListeners).
	* To handle the list of listeners and notify them about changes of the
	* properties, a ColorScaleManipulator uses a PropertyChangeSupport.
	*/
	protected PropertyChangeSupport pcSupport = null;

	@Override
	public synchronized void addPropertyChangeListener(PropertyChangeListener l) {
		if (l == null)
			return;
		if (pcSupport == null) {
			pcSupport = new PropertyChangeSupport(this);
		}
		pcSupport.addPropertyChangeListener(l);
	}

	@Override
	public synchronized void removePropertyChangeListener(PropertyChangeListener l) {
		if (l == null || pcSupport == null)
			return;
		pcSupport.removePropertyChangeListener(l);
	}

	public void notifyPropertyChange(String propName, Object oldValue, Object newValue) {
		if (pcSupport == null)
			return;
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		pcSupport.firePropertyChange(propName, oldValue, newValue);
		setCursor(Cursor.getDefaultCursor());
	}

//----------------- end notification about changes of the color scale---------------

	public ColorScaleManipulator(ColorScale cs) {
		setBackground(Color.white);
		setLayout(new ColumnLayout());
		setup(cs);
	}

	protected void setup(ColorScale cs) {
		if (cs == null)
			return;
		ready = false;
		cscale = cs;
		cscale.addPropertyChangeListener(this);
		float alpha = 100.0f - cscale.getAlpha() * 100.0f;
		sl = new Slider(this, 0.0f, 100.0f, alpha);
		//sl.setValue(alpha);
		sl.setNAD(true);
		alphaLabel = new Label(getAlphaText());
		add(alphaLabel);
		add(sl);
		f = new Focuser();
		f.addFocusListener(this);
		f.setAbsMinMax(cscale.getMinValue(), cscale.getMaxValue());
		System.out.println("min=" + cscale.getMinLimit() + " max=" + cscale.getMaxLimit());
		f.setCurrMinMax(cscale.getMinLimit(), cscale.getMaxLimit());
		f.setIsVertical(false);
		f.setIsUsedForQuery(true);

		Panel p = new Panel(new FlowLayout());
		// following string: "Show values from"
		p.add(new Label(res.getString("Show_values_from")));
		TextField minTF = new TextField(StringUtil.doubleToStr(f.getCurrMin(), f.getAbsMin(), f.getAbsMax()), 5);
		p.add(minTF);
		// following string:"to"
		p.add(new Label(res.getString("to")));
		TextField maxTF = new TextField(StringUtil.doubleToStr(f.getCurrMax(), f.getAbsMin(), f.getAbsMax()), 5);
		p.add(maxTF);
		add(p);
		f.setTextFields(minTF, maxTF);
		FocuserCanvas fc = new FocuserCanvas(f, false);
		fc.setMargins(20, 20);
		add(fc);
//ID
//    dynamic = cscale.getDynamic();
//    dynUpd = new Checkbox(res.getString("dynamic_map_update"), dynamic);
//    dynUpd.addItemListener(this);
//    add(dynUpd);
//~ID

		Component manipulator = cscale.getManipulator();
		if (manipulator != null) {
			add(new Line(false));
			add(manipulator);
		}
		if (csreg.getScaleCount() > 1) {
			add(new Line(false));
			// following string:"Change the scale type:"
			add(new Label(res.getString("Change_the_scale_type")));
			scaleTypeCB = new Checkbox[csreg.getScaleCount()];
			CheckboxGroup cbg = new CheckboxGroup();
			String csClassName = cscale.getClass().getName();
			for (int i = 0; i < csreg.getScaleCount(); i++) {
				scaleTypeCB[i] = new Checkbox(csreg.getScaleName(i), cbg, csClassName.equals(csreg.getScaleClassName(i)));
				add(scaleTypeCB[i]);
				scaleTypeCB[i].addItemListener(this);
			}
		}
		ready = true;
	}

	protected String getAlphaText() {
		return res.getString("Transparency_") + Math.round(sl.getValue()) + "%";
	}

	@Override
	public void focusChanged(Object source, double lowerLimit, double upperLimit) {
		if (!ready)
			return;
		cscale.setMinLimit((float) lowerLimit);
		cscale.setMaxLimit((float) upperLimit);
		notifyPropertyChange("ScaleParameters", null, cscale);
	}

	@Override
	public void limitIsMoving(Object source, int n, double currValue) {
		if (!ready)
			return;
		if (cscale != null && cscale.getDynamic()) {
			if (n == 0) {
				cscale.setMinLimit((float) currValue);
			} else {
				cscale.setMaxLimit((float) currValue);
			}
			notifyPropertyChange("ScaleParameters", null, cscale);
		}
	}

	@Override
	public void actionPerformed(ActionEvent evt) {
		if (!ready)
			return;
		if (evt.getSource() == sl) {
			alphaLabel.setText(getAlphaText());
			if (cscale != null && cscale.getDynamic() || !sl.getIsDragging()) {
				cscale.setAlpha(Math.round(100.0f - sl.getValue()) / 100.0f);
				notifyPropertyChange("ScaleParameters", null, cscale);
			}
		}
	}

	@Override
	public void itemStateChanged(ItemEvent evt) {
/*    if (evt.getSource() == dynUpd) {
      dynamic = dynUpd.getState();
      cscale.setDynamic(dynamic);
    } else*/
		if (scaleTypeCB != null) {
			for (int i = 0; i < scaleTypeCB.length; i++)
				if (evt.getSource() == scaleTypeCB[i] && scaleTypeCB[i].getState()) {
					ColorScale cs = csreg.getColorScale(i);
					if (cs == null)
						return;
					cs.setMinMax(cscale.getMinValue(), cscale.getMaxValue());
					cs.setAlpha(cscale.getAlpha());
					cs.setMinLimit(cscale.getMinLimit());
					cs.setMaxLimit(cscale.getMaxLimit());
					cs.setDynamic(cscale.getDynamic());
					ColorScale oldCS = cscale;
					notifyPropertyChange("NewScale", oldCS, cs);
//          Window w=CManager.getWindow(this);
//          if (w!=null) w.setVisible(false);
					clearAll();
					setup(cs);
					validate();
//          if (w==null) CManager.validateAll(this);
//          else CManager.invalidateAll(this);
//          if (w!=null) { w.pack(); w.show(); }
					return;
				}
		}
	}

	protected void clearAll() {
		if (cscale != null) {
			cscale.removePropertyChangeListener(this);
		}
		removeAll();
	}

	/**
	* Reacts to changes of the color scale
	*/
	@Override
	public void propertyChange(PropertyChangeEvent e) {
		if (e.getSource().equals(cscale)) {
			notifyPropertyChange("ScaleParameters", null, cscale);
		}
	}
}
