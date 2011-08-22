package spade.vis.mapvis;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.Metrics;
import spade.lib.basicwin.Slider;
import spade.lib.color.ColorCanvas;
import spade.lib.color.ColorDlg;
import spade.lib.color.ColorListener;
import spade.lib.lang.Language;
import spade.lib.util.StringUtil;
import spade.vis.geometry.Sign;
import spade.vis.geometry.StructSign;

/**
* a dialog for changing visual elements of diagrams on the map
*/
class SignParamsDlg extends Dialog implements ActionListener, ItemListener, ColorListener/*, PropertyChangeListener*/{
	static ResourceBundle res = Language.getTextResource("spade.vis.mapvis.Res");
	/**
	* Listener of sign changes
	*/
	protected SignDrawer sd = null;
	protected Sign si = null, si1 = null;
	protected SampleSignCanvas ssc = null, ssc1 = null;
	protected Checkbox cbFrame = null, cbOrder[] = null;
	protected Vector chSize = null, lSize = null, ccs = null;
	protected ColorDlg cDlg = null;
	// following string: "preserved","descending","ascending"
	protected String orderNames[] = { res.getString("preserved"), res.getString("descending"), res.getString("ascending") };
	protected boolean sir = false, // sign is round => 1 dimension
			shcs = true, // sign has a constant size
			shmins = false, // sign has a min. size, impossible together with shcs
			shmaxs = false; // sign has a max. size, impossible together with shcs
	protected Frame ownerFrame = null;

	@Override
	public void dispose() {
		if (cDlg != null) {
			cDlg.dispose();
		}
		/*
		if (sd instanceof AttrColorHandler) {
		  ((AttrColorHandler)sd).removePropertyChangeListener(this);
		}
		*/
		super.dispose();
	}

	public SignParamsDlg(Frame owner, SignDrawer sd) {
		// following string: "Set parameters of diagrams"
		super(owner, res.getString("Set_parameters_of1"), true);
		ownerFrame = owner;
		this.sd = sd;
		si = sd.getSignInstance();
		sir = si.getIsRound();
		shcs = si.mayChangeProperty(Sign.SIZE);
		shmins = si.mayChangeProperty(Sign.MIN_SIZE);
		shmaxs = si.mayChangeProperty(Sign.MAX_SIZE);
		if (shmaxs) {
			si.setSizes(si.getMaxWidth(), si.getMaxHeight());
		}

		Panel p = new Panel();
		add(p);
		p.setLayout(new ColumnLayout());
		Panel upP = new Panel();
		upP.setLayout(new BorderLayout());
		p.add(upP);
		ssc = new SampleSignCanvas(si);
		if (shmins && shmaxs) {
			si1 = sd.getSignInstance();
			si1.setSizes(si.getMinWidth(), si.getMinHeight());
			ssc1 = new SampleSignCanvas(si1);
			Panel pp = new Panel(new GridLayout(1, 2));
			pp.add(ssc1);
			pp.add(ssc);
			upP.add(pp, "West");
		} else {
			upP.add(ssc, "West");
		}
		upP.add(new Line(true), "Center");
		Panel upPr = new Panel();
		upPr.setLayout(new ColumnLayout());
		upP.add(upPr, "East");
		if (si.mayChangeProperty(Sign.USE_FRAME)) {
			// controls for usage of frame
			//upPr.add(new Line(false));
			// following string: "Frame the sign"
			cbFrame = new Checkbox(res.getString("Frame_the_sign1"), si.getMustDrawFrame());
			cbFrame.addItemListener(this);
			upPr.add(cbFrame);
		}
		if ((si instanceof StructSign) && si.mayChangeProperty(Sign.SEGMENT_ORDER)) {
			StructSign ss = (StructSign) si;
			upPr.add(new Line(false));
			// following string: "Order of segments"
			upPr.add(new Label(res.getString("Order_of_segments1"), Label.LEFT));
			CheckboxGroup cbg = new CheckboxGroup();
			cbOrder = new Checkbox[3];
			for (int i = 0; i < cbOrder.length; i++) {
				cbOrder[i] = new Checkbox(orderNames[i], cbg, i == ss.getOrderingMethod());
				cbOrder[i].addItemListener(this);
				upPr.add(cbOrder[i]);
			}
		}
		if (si.mayChangeProperty(Sign.COLOR)) {
			// control for the (single) color of sign
			upPr.add(new Line(false));
			// following string: "Set color of the sign"
			Button b = new Button(res.getString("Set_color_of_the_sign1"));
			upPr.add(b);
			b.addActionListener(this);
			b.setActionCommand("singleColor");
		}
		if (si.mayChangeProperty(Sign.SIZE) || si.mayChangeProperty(Sign.MIN_SIZE) || si.mayChangeProperty(Sign.MAX_SIZE)) {
			// controls for sizes
			p.add(new Line(false));
			chSize = new Vector(4, 4);
			lSize = new Vector(4, 4);
			if (shcs)
				if (sir) { // only diameter
					// following string: "Diameter="
					p.add(makePanel(res.getString("Diameter_1"), si.getDiameter()));
				} else { // h and w
					// following string: "Height="
					p.add(makePanel(res.getString("Height_1"), si.getHeight()));
					// following string: "Width="
					p.add(makePanel(res.getString("Width_1"), si.getWidth()));
				}
			else {
				if (shmins)
					if (sir) { // only diameter
						// following string: "Min Diameter="
						p.add(makePanel(res.getString("Min_Diameter_1"), si.getMinWidth()));
					} else { // h and w
						// following string: "Min Height="
						p.add(makePanel(res.getString("Min_Height_1"), si.getMinHeight()));
						// following string: "Min Width="
						p.add(makePanel(res.getString("Min_Width_1"), si.getMinWidth()));
					}
				if (shmaxs)
					if (sir) { // only diameter
						// following string: "Max Diameter="
						p.add(makePanel(res.getString("Max_Diameter_1"), si.getMaxWidth()));
					} else { // h and w
						// following string: "Max Height="
						p.add(makePanel(res.getString("Max_Height_1"), si.getMaxHeight()));
						// following string: "Max Width="
						p.add(makePanel(res.getString("Max_Width_1"), si.getMaxWidth()));
					}
			}
		}
		if (sd instanceof AttrColorHandler) {
			//((AttrColorHandler)sd).addPropertyChangeListener(this);
			// control for the attribute colors of sign
			p.add(new Line(false));
			DataPresenter dpres = (DataPresenter) sd;
			Vector attr = dpres.getAttributes();
			ccs = new Vector(attr.size());
			for (int i = 0; i < attr.size(); i++) {
				String id = (String) attr.elementAt(i);
				ColorCanvas cc = new ColorCanvas();
				ccs.addElement(cc);
				if (id == null) {
					continue;
				}
				cc.setColor(dpres.getColorForAttribute(i));
				cc.setActionListener(this);
				Panel pp = new Panel();
				pp.setLayout(new BorderLayout());
				pp.add(cc, "West");
				pp.add(new Label(dpres.getAttrName(i)));
				p.add(pp);
			}
		}
		p.add(new Line(false));
		Panel pp = new Panel();
		pp.setLayout(new FlowLayout());
		p.add(pp);
		Button b = new Button("OK");
		pp.add(b);
		b.addActionListener(this);
		b.setActionCommand("close");
	}

	public Panel makePanel(String l, int v) {
		Panel pp = new Panel();
		pp.setLayout(new BorderLayout());
		float val = v / Metrics.mm();
		Label lS = new Label(l + StringUtil.floatToStr(val, 1) + " mm");
		pp.add(lS, "North");
		lSize.addElement(lS);
		Slider sl = new Slider(this, 0.0f, 30.0f, val);
		pp.add(sl, "Center");
		chSize.addElement(sl);
		return pp;
	}

	@Override
	public void itemStateChanged(ItemEvent ie) {
		if (ie.getSource() == cbFrame) {
			si.setMustDrawFrame(cbFrame.getState());
			ssc.redraw();
			if (si1 != null) {
				si1.setMustDrawFrame(cbFrame.getState());
				ssc1.redraw();
			}
			sd.signPropertyChanged(Sign.USE_FRAME, si);
			return;
		}
		if (cbOrder != null) {
			for (int i = 0; i < cbOrder.length; i++)
				if (ie.getSource() == cbOrder[i]) {
					((StructSign) si).setOrderingMethod(i);
					ssc.redraw();
					if (si1 != null) {
						((StructSign) si1).setOrderingMethod(i);
						ssc1.redraw();
					}
					sd.signPropertyChanged(Sign.SEGMENT_ORDER, si);
					return;
				}
		}
	}

	/*
	public void propertyChange (PropertyChangeEvent pce) {
	  //System.out.println("* VisParamsController PropertyChangeEvent received");
	  DataPresenter dpres=(DataPresenter)sd;
	  Vector attr=dpres.getAttributes();
	  for (int i=0; i<ccs.size(); i++)
	    if (attr.elementAt(i)!=null) {
	      ColorCanvas cc=(ColorCanvas)ccs.elementAt(i);
	      Color c=dpres.getColorForAttribute(i);
	      cc.setColor(c);
	      ((StructSign)si).setSegmentColor(i,c);
	      if (si1!=null)
	        ((StructSign)si1).setSegmentColor(i,c);
	    }
	  ssc.redraw();
	  if (ssc1!=null) ssc1.redraw();
	}
	*/
	@Override
	public void actionPerformed(ActionEvent ae) {
		if (ae.getSource() instanceof Button && ae.getActionCommand().equals("close")) {
			dispose();
		}
		if (ae.getSource() instanceof Button && ae.getActionCommand().equals("singleColor")) {
			// set color
			//System.out.println("* to set new color...");
			if (cDlg == null) {
				cDlg = new ColorDlg(ownerFrame, "");
			}
			//cDlg.setVisible(true);
			// following string: "Sign color"
			cDlg.setTitle(res.getString("Sign_color1"));
			cDlg.selectColor(this, this, si.getColor());
			return;
		}
		if (ae.getSource() instanceof ColorCanvas) {
			ColorCanvas cc = (ColorCanvas) ae.getSource();
			int n = ccs.indexOf(cc);
			DataPresenter dpres = (DataPresenter) sd;
			if (cDlg == null) {
				cDlg = new ColorDlg(ownerFrame, res.getString("Color_for_1") + dpres.getAttrName(n));
			}
			cDlg.selectColor(this, cc, cc.getColor());
			return;
		}
		if (ae.getSource() instanceof Slider) {
			Slider sl = (Slider) ae.getSource();
			float val = (float) sl.getValue();
			int n = chSize.indexOf(sl);
			if (val < 1.0f) {
				val = 1.0f;
				sl.setValue(val);
			}
			int size = Math.round(Metrics.mm() * val);
			if (shcs) {
				if (sir) {
					si.setSizes(size, size);
				} else // h and w, n<=1
				if (n == 0) {
					si.setHeight(size);
				} else {
					si.setWidth(size);
				}
				updateLabel(n, val);
				sd.signPropertyChanged(Sign.SIZE, si);
			} else {
				if (shmins) {
					if (sir) // only diameter
						if (n == 0) {
							si.setMinSizes(size, size);
							if (si1 != null) {
								si1.setMinSizes(size, size);
								si1.setSizes(size, size);
								ssc1.redraw();
							}
						} else {
							;
						}
					else // h and w, n<=1
					if (n == 0) {
						si.setMinHeight(size);
						if (si1 != null) {
							si1.setMinHeight(size);
							si1.setHeight(size);
							ssc1.redraw();
						}
					} else if (n == 1) {
						si.setMinWidth(size);
						if (si1 != null) {
							si1.setMinWidth(size);
							si1.setWidth(size);
							ssc1.redraw();
						}
					}
					updateLabel(n, val);
					sd.signPropertyChanged(Sign.MIN_SIZE, si);
				}
				if (shmaxs) {
					int nn = (shmins) ? 1 : 0;
					if (sir) // only diameter
						if (!shmins || n == 1) {
							si.setMaxSizes(size, size);
							if (si1 != null) {
								si1.setMaxSizes(size, size);
							}
							si.setSizes(size, size);
						} else {
							;
						}
					else // h and w, n==2*nn or 2*nn+1
					if (n == 2 * nn) {
						si.setMaxHeight(size);
						if (si1 != null) {
							si1.setMaxHeight(size);
						}
						si.setHeight(size);
					} else if (n == 2 * nn + 1) {
						si.setMaxWidth(size);
						if (si1 != null) {
							si1.setMaxWidth(size);
						}
						si.setWidth(size);
					}
					updateLabel(n, val);
					sd.signPropertyChanged(Sign.MAX_SIZE, si);
				}
			}
			ssc.redraw();
			return;
		}
	}

	public void updateLabel(int n, float val) {
		Label l = (Label) lSize.elementAt(n);
		String str = l.getText();
		int i = str.indexOf("=");
		str = str.substring(0, i + 1) + StringUtil.floatToStr(val, 1) + " mm";
		l.setText(str);
	}

	@Override
	public void colorChanged(Color c, Object sel) {
		// find a ColorCanvas produced the message
		if (sel == this) {
			si.setColor(c);
			sd.signPropertyChanged(Sign.COLOR, si);
			if (si1 != null) {
				si1.setColor(c);
			}
		} else {
			ColorCanvas cc = (ColorCanvas) sel;
			int n = ccs.indexOf(cc);
			cc.setColor(c);
			DataPresenter dpres = (DataPresenter) sd;
			dpres.setColorForAttribute(c, n);
			((StructSign) si).setSegmentColor(n, c);
			if (si1 != null) {
				((StructSign) si).setSegmentColor(n, c);
			}
		}
		ssc.redraw();
		if (ssc1 != null) {
			ssc1.redraw();
		}
		// hide a dialog
		cDlg.setVisible(false);
	}
}
