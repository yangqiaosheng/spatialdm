//ID
package spade.lib.color;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Canvas;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.util.Vector;

import spade.lib.basicwin.CManager;
import spade.lib.basicwin.GetPathDlg;
import spade.lib.basicwin.OKDialog;
import spade.lib.help.Helper;
import spade.lib.lang.Language;
import spade.lib.slider.MultipleSlider;
import spade.lib.slider.SliderEvent;
import spade.lib.slider.SliderListener;

public class RegionsColorScale extends BaseColorScale implements SliderListener, ActionListener/*, ComponentListener*/{

	public class Node {
		public Color less = new Color(0);
		public Color more = new Color(0);
		public float value = 0;

		public Node(Color less, Color more, float value) {
			this.less = less;
			this.more = more;
			this.value = value;
		}

		@Override
		public String toString() {
			return "value=" + Float.toString(value) + " less=" + less + " more=" + more;
		}

		@Override
		public Object clone() {
			return new Node(new Color(less.getRGB()), new Color(more.getRGB()), value);
		}
	}

	static ResourceBundle res = Language.getTextResource("spade.lib.color.Res");
	public final static int LIN = 0;
	public final static int LOG = 1;
	public final static int EXP = 2;
	public final static int RGB = 0;
	public final static int HSB = 1;
	public final static int RHSB = 2;
	public final static float eps = 0.000001f;

	public class Gap {
		public int inter = LIN;
		public int steps = 0;
		public int colspace = RGB;

		public Gap(int inter, int steps, int colspace) {
			this.inter = inter;
			this.steps = steps;
			this.colspace = colspace;
		}

		public Gap() {
		}

		@Override
		public String toString() {
			return "inter=" + Integer.toString(inter) + " steps=" + Integer.toString(steps) + " colspace=" + Integer.toString(colspace);
		}

		@Override
		public Object clone() {
			return new Gap(inter, steps, colspace);
		}
	}

	String filename = "";

	public Vector nodes = new Vector();
	public Vector gaps = new Vector();
	public int curgap = 0, curnode = 0;
	public MultipleSlider slider;
//  Histogram hist;
	ManipulatorBar bar = new ManipulatorBar(/*nodes, gaps*/);
	Panel mp = new Panel();
	// following string: "Save"
	Button save = new Button(res.getString("Save"));
	// following string:"Load"
	Button load = new Button(res.getString("Load"));
	// following string:"Undo"
	Button undo = new Button(res.getString("Undo"));

	Vector unodes = new Vector();
	Vector ugaps = new Vector();
	Vector uslider = new Vector();
	Vector uuslider = new Vector();
	ManipulatorBar ubar;

	@Override
	public void setMinMax(float min, float max) {
		boolean firstRun = false;
		if (Float.isNaN(minV) || Float.isNaN(maxV)) {
			firstRun = true;
		}
		super.setMinMax(min, max);
		if (firstRun) {
			generateDefaultScale();
		}
	}

	private void generateDefaultScale() {
		nodes.removeAllElements();
		gaps.removeAllElements();
		/*
		nodes.addElement(new Node(Color.black, Color.white, minLimit));
		gaps.addElement(new Gap());
		nodes.addElement(new Node(Color.black, Color.white, maxLimit));
		*/
		slider = new MultipleSlider(minV, maxV, MultipleSlider.HORIZONTAL, MultipleSlider.TOP);
		slider.addSliderListener(this);

		if (minV < 0) {
			nodes.addElement(new Node(new Color(0x3279FA), new Color(0x3279FA), minV));
		} else if (minV < 200) {
			nodes.addElement(new Node(Color.blue, new Color(0x7CBD71), minV));
		} else {
			nodes.addElement(new Node(Color.green, new Color(0xFDFEC1), minV));
		}

		if (minV < 0 && maxV > 0) {
			nodes.addElement(new Node(Color.white, new Color(0x7CBD71), 0));
			slider.addSlider(0);
			gaps.addElement(new Gap(EXP, 0, RGB));
		}
		if (minV < 200 && maxV > 200) {
			nodes.addElement(new Node(new Color(0xE7FDB3), new Color(0xFDFEC1), 200));
			slider.addSlider(200);
			gaps.addElement(new Gap(LOG, 0, RGB));
		}
		if (maxV < 0) {
			gaps.addElement(new Gap(EXP, 0, RGB));
		} else {
			gaps.addElement(new Gap(LOG, 0, RGB));
		}

		if (maxV < 0) {
			nodes.addElement(new Node(Color.white, Color.white, maxV));
		} else if (maxV < 200) {
			nodes.addElement(new Node(new Color(0xE7FDB3), new Color(0xE7FDB3), maxV));
		} else {
			nodes.addElement(new Node(new Color(0xDF3308), new Color(0xDF3308), maxV));
		}

		pushUndo();
		//System.out.println("Undo initialized");
		save.addActionListener(this);
		load.addActionListener(this);
		undo.addActionListener(this);
		//  hist = new Histogram(minV, maxV);
	}

	public void pushUndo() {
		unodes.removeAllElements();
		for (int i = 0; i < nodes.size(); i++) {
			unodes.addElement(((Node) nodes.elementAt(i)).clone());
		}

		ugaps.removeAllElements();
		for (int i = 0; i < gaps.size(); i++) {
			ugaps.addElement(((Gap) gaps.elementAt(i)).clone());
		}

// uu = u
		uuslider.removeAllElements();
		for (int i = 0; i < uslider.size(); i++) {
			uuslider.addElement(new Float(((Float) uslider.elementAt(i)).floatValue()));
		}

// u = slider
		uslider.removeAllElements();
		for (int i = 0; i < slider.getCount(); i++) {
			uslider.addElement(new Float(slider.getValue(i)));
		}
/*
System.out.println("After push:");
System.out.println("uu "+ uuslider);
System.out.println("u  "+ uslider);
System.out.println("sl "+ slider.getSliders());
*/
	}

	public void popUndo() {
// nodes <-> unodes

		Vector tnodes = new Vector();
		Vector tgaps = new Vector();

// temp=nodes

		for (int i = 0; i < nodes.size(); i++) {
			tnodes.addElement(((Node) nodes.elementAt(i)).clone());
		}

		for (int i = 0; i < gaps.size(); i++) {
			tgaps.addElement(((Gap) gaps.elementAt(i)).clone());
		}

// nodes=unodes +
		nodes.removeAllElements();
		for (int i = 0; i < unodes.size(); i++) {
			nodes.addElement(((Node) unodes.elementAt(i)).clone());
		}

		gaps.removeAllElements();
		for (int i = 0; i < ugaps.size(); i++) {
			gaps.addElement(((Gap) ugaps.elementAt(i)).clone());
		}

// unodes=temp
		unodes = tnodes;
		ugaps = tgaps;

// s==uu +
		for (int i = slider.getCount() - 1; i >= 0; i--) {
			slider.removeSlider(i);
		}
		for (int i = 0; i < uuslider.size(); i++) {
			slider.addSlider(((Float) uuslider.elementAt(i)).floatValue());
		}

// uu==u +
		uuslider.removeAllElements();
		for (int i = 0; i < uslider.size(); i++) {
			uuslider.addElement(new Float(((Float) uslider.elementAt(i)).floatValue()));
		}

// u==s +
		uslider.removeAllElements();
		for (int i = 0; i < slider.getCount(); i++) {
			uslider.addElement(new Float(slider.getValue(i)));
		}
		update();
		slider.repaint();

/*
System.out.println("After pop:");
System.out.println("uu "+ uuslider);
System.out.println("u  "+ uslider);
System.out.println("sl "+ slider.getSliders());
*/
	}

	protected static String lastDir = null;

	protected String getUserDir() {
		if (lastDir != null)
			return lastDir;
		try {
			lastDir = System.getProperty("user.dir");
			return lastDir;
		} catch (Throwable thr) {
		}
		return null;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == save) {
			String dir = getUserDir();
			if (dir == null)
				return;
			// following string:"Select file to save color scale"
			GetPathDlg fd = new GetPathDlg(CManager.getAnyFrame(null), res.getString("Select_file_to_save"));
			fd.setFileMask("*.rcs");
			fd.setDirectory(dir);

			fd.show();
			filename = fd.getPath();
			if (filename == null || filename.length() < 1)
				return;
			lastDir = fd.getDirectory();

			if (filename.lastIndexOf(".") < 0) {
				filename += ".rcs";
			} else if (filename.lastIndexOf(".") == filename.length() - 1) {
				filename += "rcs";
			}

			saveToFile(filename);
		}
		if (e.getSource() == load) {
			String dir = getUserDir();
			if (dir == null)
				return;
			// following string:"Select file to load color scale"
			GetPathDlg fd = new GetPathDlg(CManager.getAnyFrame(null), res.getString("Select_file_to_load"));
			fd.setFileMask("*.rcs");
			fd.setDirectory(dir);
			fd.show();
			filename = fd.getPath();
			if (filename == null || filename.length() < 1)
				return;
			lastDir = fd.getDirectory();

			loadFromFile(filename);
		}
		if (e.getSource() == undo) {
//System.out.println("Undo invoked");
			popUndo();
			bar.repaint();
			notifyScaleChange();
		}
	}

	public void saveToFile(String filename) {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
			writer.write("RCS1.0");
//      writer.write(" Default color scale");
			writer.write("\n");
			for (int i = 0; i < nodes.size(); i++) {
				writer.write(new Float(((Node) nodes.elementAt(i)).value).toString());
				if (i < nodes.size() - 1) {
					writer.write(" ");
				} else {
					writer.write("\n");
				}
			}

			for (int i = 0; i < gaps.size(); i++) {
				writer.write(Integer.toHexString(((Node) nodes.elementAt(i)).more.getRGB()).substring(2));
				writer.write(" ");
				writer.write(Integer.toHexString(((Node) nodes.elementAt(i + 1)).less.getRGB()).substring(2));
				writer.write(" ");

				switch (((Gap) gaps.elementAt(i)).colspace) {
				case RGB:
					writer.write("RGB ");
					break;
				case HSB:
					writer.write("HSB ");
					break;
				case RHSB:
					writer.write("RHSB ");
					break;
				default:
					writer.write("RGB ");
					break;
				}

				switch (((Gap) gaps.elementAt(i)).inter) {
				case LIN:
					writer.write("LIN ");
					break;
				case LOG:
					writer.write("LOG ");
					break;
				case EXP:
					writer.write("EXP ");
					break;
				default:
					writer.write("LIN ");
					break;
				}

				writer.write(new Integer(((Gap) gaps.elementAt(i)).steps).toString());
				writer.write("\n");
			}

			writer.close();
			notifyScaleChange();
		} catch (Exception ex) {
			System.out.println(ex.toString());
		}
	}

	public void loadFromFile(String filename) {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(filename));
			Vector lnodes = new Vector();
			Vector lgaps = new Vector();

			String line = reader.readLine();
			// following string:"File is empty"
			if (line == null)
				throw new Exception(res.getString("File_is_empty"));
			line.trim();
			String id = line.substring(0, 3);
			String ver = line.substring(3, 6);
			String descr = "";
			if (line.length() > 7) {
				descr = line.substring(7);
			}
			// following string:"Wrong format"
			if (!id.equals("RCS"))
				throw new Exception(res.getString("Wrong_format"));
			// following string:"Version not supported"
			if (ver.compareTo("1.0") != 0)
				throw new Exception(res.getString("Version_not_supported"));

			line = reader.readLine();

			StringTokenizer st = new StringTokenizer(line);
			while (st.hasMoreTokens()) {
				lnodes.addElement(new Node(Color.black, Color.black, new Float(st.nextToken()).floatValue()));
			}

			for (int i = 0; i < lnodes.size() - 1; i++) {
				line = reader.readLine();
				// following string:"Wrong format"
				if (line == null)
					throw new Exception(res.getString("Wrong_format"));
				line.trim();
				st = new StringTokenizer(line);

				String cs1 = st.nextToken();
				String cs2 = st.nextToken();
				String scolspace = st.nextToken();
				String sinter = st.nextToken();
				String steps = st.nextToken();

				int colspace;
				int inter;

				if (scolspace.equals("RHSB")) {
					colspace = RHSB;
				} else if (scolspace.equals("HSB")) {
					colspace = HSB;
				} else {
					colspace = RGB;
				}

				if (sinter.equals("EXP")) {
					inter = EXP;
				} else if (sinter.equals("LOG")) {
					inter = LOG;
				} else {
					inter = LIN;
				}

				((Node) lnodes.elementAt(i)).more = new Color(Integer.parseInt(cs1, 16));
				((Node) lnodes.elementAt(i + 1)).less = new Color(Integer.parseInt(cs2, 16));
				lgaps.addElement(new Gap(inter, Integer.parseInt(steps), colspace));
			}
			((Node) lnodes.firstElement()).less = ((Node) lnodes.firstElement()).more;
			((Node) lnodes.lastElement()).more = ((Node) lnodes.lastElement()).less;

			reader.close();
			System.out.println("New regions color scale loaded. " + descr);

/////////////////

			if (lgaps.size() == 1) {
				((Node) lnodes.firstElement()).value = slider.getMin();
				((Node) lnodes.lastElement()).value = slider.getMax();
			} else {
				//no overlapping
				if (slider.getMax() < ((Node) lnodes.firstElement()).value ||
				// following string:"Scale not compatible"
						slider.getMin() > ((Node) lnodes.lastElement()).value)
					throw new Exception(res.getString("Scale_not_compatible"));

				while (lnodes.size() >= 3 && slider.getMin() > ((Node) lnodes.elementAt(1)).value) {
					lnodes.removeElementAt(0);
					lgaps.removeElementAt(0);
				}

				while (lnodes.size() >= 3 && slider.getMax() < ((Node) lnodes.elementAt(lnodes.size() - 2)).value) {
					lnodes.removeElementAt(lnodes.size() - 1);
					lgaps.removeElementAt(lgaps.size() - 1);
				}

				if (lgaps.size() == 1) {
					((Node) lnodes.firstElement()).value = slider.getMin();
					((Node) lnodes.lastElement()).value = slider.getMax();
				} else {
					//enlarge or shrink to fit slider
					if (slider.getMin() < ((Node) lnodes.firstElement()).value || lnodes.size() >= 3 && slider.getMin() < ((Node) lnodes.elementAt(1)).value) {
						((Node) lnodes.firstElement()).value = slider.getMin();
					}
					if (slider.getMax() > ((Node) lnodes.lastElement()).value || lnodes.size() >= 3 && slider.getMax() > ((Node) lnodes.elementAt(lnodes.size() - 2)).value) {
						((Node) lnodes.lastElement()).value = slider.getMax();
					}
				}
				//now loaded values are not narrower than slider values
			}
			nodes = lnodes;
			gaps = lgaps;

			for (int i = slider.getCount() - 1; i >= 0; i--) {
				slider.removeSlider(i);
			}
			if (gaps.size() > 1) {
				for (int i = 1; i < nodes.size() - 1; i++) {
					slider.addSlider(((Node) nodes.elementAt(i)).value);
				}
			}
			pushUndo();
			pushUndo();

			bar.repaint();
			slider.repaint();
			notifyScaleChange();
		} catch (Exception ex) {
			System.out.println(ex.toString());
		}
	}

	/**
	* Returns a UI component that allows the user to manipulate specific
	* parameters of this color scale (i.e. not transparency or minimum and
	* maximum limits that are manipulated in the same way for all color scales)
	*/
	@Override
	public Component getManipulator() {
		slider.setBackground(Color.white);
		Panel p = new Panel();
		Panel p1 = new Panel();
		Panel p2 = new Panel();
		Panel p3 = new Panel();
		Panel p4 = new Panel();
		p.setLayout(new BorderLayout());
		p1.setLayout(new BorderLayout());
		p2.setLayout(new BorderLayout());
		p4.setLayout(new BorderLayout());
		//  p3.setLayout(new GridLayout(1,2,4,4));
		//  hist = new Label("Histogram will be here", Label.CENTER);

		//  p.add(hist, "Center");
		p.add(p1, "South");
		p1.add(slider, "Center");
		p1.add(p2, "South");

		p2.add(p4, "Center");
		p4.add(bar, "North");

		p4.add(p3, "Center");
		p3.add(save);
		p3.add(load);
		p3.add(undo);
		mp = p;
		return p;
	}

	/**
	 * Returns the color for the given value packed into an integer:
	 * top 8 bits (0xFF << 24) - alpha value
	 * 2nd 8 bits (0xFF << 16) - red value
	 * 3rd 8 bits (0xFF << 8) - green value
	 * bottom 8 bits (0xFF) - blue value
	 */
	@Override
	public int encodeValue(float value) {
		if (nodes == null || nodes.size() < 1)
			return 0;
//  hist.addValue((float)value);

		int i = 0;
		Node first = (Node) nodes.firstElement();
		Node last = (Node) nodes.lastElement();
		if (first.value >= value)
			return first.more.getRGB();
		else if (last.value <= value)
			return last.less.getRGB();
		else {
			//  if (first.value >= value || last.value <  value) return 0x00000000; else
			while (((Node) nodes.elementAt(i)).value < value) {
				i++;
			}
		}
		Node ni1 = (Node) nodes.elementAt(i - 1);
		Node ni = (Node) nodes.elementAt(i);
		Gap g = (Gap) gaps.elementAt(i - 1);

		if (g.steps == 1)
			return ni1.more.getRGB();

		Color c1 = ni1.more;
		Color c2 = ni.less;
		float x1 = ni1.value;
		float x2 = ni.value;
		float k = (value - x1) / (x2 - x1);
		if (g.inter == LOG) {
			k = (float) Math.log(k * (Math.E - 1) + 1);
		} else if (g.inter == EXP) {
			k = (float) ((Math.exp(k) - 1) / (Math.E - 1));
		}

		if (g.colspace == RGB) {
			int r1 = c1.getRed();
			int r2 = c2.getRed();
			int g1 = c1.getGreen();
			int g2 = c2.getGreen();
			int b1 = c1.getBlue();
			int b2 = c2.getBlue();
			float rs = r2 - r1;
			float gs = g2 - g1;
			float bs = b2 - b1;

			float rv = rs * k + r1;
			float gv = gs * k + g1;
			float bv = bs * k + b1;

			if (g.steps > 0) {
				rv = (rs < eps && rs > -eps) ? rv : (float) (Math.floor((rv - r1) / rs * g.steps) * rs / (g.steps - 1) + r1);
				gv = (gs < eps && gs > -eps) ? gv : (float) (Math.floor((gv - g1) / gs * g.steps) * gs / (g.steps - 1) + g1);
				bv = (bs < eps && bs > -eps) ? bv : (float) (Math.floor((bv - b1) / bs * g.steps) * bs / (g.steps - 1) + b1);
			}
			return 0xFF000000 + ((int) rv << 16) + ((int) gv << 8) + (int) bv;
		} else {
			float h1, h2, s1, s2, b1, b2;
			float[] col1 = Color.RGBtoHSB(c1.getRed(), c1.getGreen(), c1.getBlue(), null);
			float[] col2 = Color.RGBtoHSB(c2.getRed(), c2.getGreen(), c2.getBlue(), null);
			h1 = col1[0];
			s1 = col1[1];
			b1 = col1[2];
			h2 = col2[0];
			s2 = col2[1];
			b2 = col2[2];

			if (g.colspace == RHSB) {
				if (h1 < h2) {
					h1 += 1.0;
				} else {
					h2 += 1.0;
				}
			}

			float hs = h2 - h1;
			float ss = s2 - s1;
			float bs = b2 - b1;

			float hv = hs * k + h1;
			float sv = ss * k + s1;
			float bv = bs * k + b1;

			if (g.steps > 0) {
				hv = (hs < eps && hs > -eps) ? hv : (float) (Math.floor((hv - h1) / hs * g.steps) * hs / (g.steps - 1) + h1);
				sv = (ss < eps && ss > -eps) ? sv : (float) (Math.floor((sv - s1) / ss * g.steps) * ss / (g.steps - 1) + s1);
				bv = (bs < eps && bs > -eps) ? bv : (float) (Math.floor((bv - b1) / bs * g.steps) * bs / (g.steps - 1) + b1);
			}
			return Color.getHSBColor(hv, sv, bv).getRGB();
		}

	}

	public void update() { // slider -> nodes, gaps
//    ((Node)nodes.firstElement()).value = slider.getMin();
//    ((Node)nodes.lastElement()).value = slider.getMax();

		if (slider.getCount() + 2 > nodes.size()) { //node added
			int i = 0;
			if (slider.getCount() != 1) {
				while (Math.abs(((Node) nodes.elementAt(i + 1)).value - slider.getValue(i)) < eps) {
					i++;
				}
			}
			nodes.insertElementAt(new Node(new Color(encodeValue(slider.getValue(i))), new Color(encodeValue(slider.getValue(i))), slider.getValue(i)), i + 1);
			gaps.insertElementAt(new Gap(), i);
		} else if (slider.getCount() + 2 < nodes.size()) { //node deleted
			int i = 0;
			while (i < slider.getCount() && Math.abs(((Node) nodes.elementAt(i + 1)).value - slider.getValue(i)) < eps) {
				i++;
			}
			nodes.removeElementAt(i + 1);
			gaps.removeElementAt(i);
			if (curnode == nodes.size()) {
				curnode--;
			}
			if (curgap == gaps.size()) {
				curgap--;
			}
		} else { //slider moved
			if (slider.getCount() != 0) {
				int i = 0;
				if (slider.getCount() != 1) {
					while (i < slider.getCount() && Math.abs(((Node) nodes.elementAt(i + 1)).value - slider.getValue(i)) < eps) {
						i++;
					}
				}
				if (i < slider.getCount()) {
					((Node) nodes.elementAt(i + 1)).value = slider.getValue(i);
				}
			}
		}
		bar.repaint();
		notifyScaleChange();
	}

	@Override
	public void SliderCountChanged(SliderEvent e) {
		slider = (MultipleSlider) e.getSource();
		pushUndo();
		update();
	}

	@Override
	public void SliderReleased(SliderEvent e) {
		slider = (MultipleSlider) e.getSource();
		pushUndo();
		update();
	}

	@Override
	public void SliderLimitsChanged(SliderEvent e) {
	}

	@Override
	public void SliderHighlighted(SliderEvent e) {
	}

	@Override
	public void SliderResized(SliderEvent e) {
	}

	@Override
	public void SliderDragged(SliderEvent e) {
		if (dynamic) {
			slider = (MultipleSlider) e.getSource();
//    pushUndo();
			update();
		}
	}

	public class ManipulatorBar extends Canvas implements ItemListener, ActionListener, MouseListener, MouseMotionListener, ColorListener {

// int pixels[] = new int[r * 256];
//  Vector nodes, gaps;
		int cgap;
		int place;
		float fplace;
		ColorDlg cdialog = null;

		CheckboxGroup colSpace = new CheckboxGroup();
		CheckboxGroup interType = new CheckboxGroup();
		Checkbox cs1 = new Checkbox("RGB", colSpace, true);
		Checkbox cs2 = new Checkbox("HSB", colSpace, false);
		Checkbox cs3 = new Checkbox("RHSB", colSpace, false);
		Checkbox it1 = new Checkbox("Linear", interType, true);
		Checkbox it2 = new Checkbox("Log", interType, false);
		Checkbox it3 = new Checkbox("Exp", interType, false);
		TextField stepEdit = new TextField(3);
		TextField valEdit = new TextField(6);
		// following string:
		Button help = new Button(res.getString("Help"));

		int width;
		int barSize;
		final int height = 25;
		final int hgap = 22; // 22 - for unchanged slider
		final int vgap = 4;
		final int skip = 2;
		final float ctol = 0.3f;

		public ManipulatorBar(/*Vector nodes, Vector gaps*/) {
			super();
//    this.nodes=nodes;
//    this.gaps=gaps;
			setSize(100, height + 2 * vgap);

			addMouseListener(this);
			addMouseMotionListener(this);
			valEdit.addActionListener(this);
			stepEdit.addActionListener(this);
			help.addActionListener(this);
			cs1.addItemListener(this);
			cs2.addItemListener(this);
			cs3.addItemListener(this);
			it1.addItemListener(this);
			it2.addItemListener(this);
			it3.addItemListener(this);
		}

		@Override
		public void colorChanged(Color col, Object sel) {
			int id = ((Integer) sel).intValue();
			if (id % 2 != 0) {
				((Node) nodes.elementAt(id / 2 + 1)).less = col;
			} else {
				((Node) nodes.elementAt(id / 2)).more = col;
			}
			repaint();
			slider.repaint();
			notifyScaleChange();
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (e.getSource() == valEdit) {
				newVal();
			} else if (e.getSource() == stepEdit) {
				newStep();
			} else if (e.getSource() == help) {
				Helper.help("color_scale");
			}
		}

		public void newVal() {
			try {
				float val = new Float(valEdit.getText()).floatValue();
				if (cgap == 0 && val >= ((Node) nodes.elementAt(1)).value || cgap == nodes.size() - 1 && val <= ((Node) nodes.elementAt(nodes.size() - 2)).value || cgap != 0 && cgap != nodes.size() - 1
						&& val <= ((Node) nodes.elementAt(cgap - 1)).value || cgap != 0 && cgap != nodes.size() - 1 && val >= ((Node) nodes.elementAt(cgap + 1)).value)
					throw new Exception();
				((Node) nodes.elementAt(cgap)).value = val;
				if (cgap > 0 && cgap < nodes.size() - 1) {
					slider.setValue(val, cgap - 1);
					slider.resetScale();
					slider.repaint();
				} else if (cgap == 0) {
					slider.setMin(val);
				} else {
					slider.setMax(val);
				}
				notifyScaleChange();
			} catch (Exception ex) {
				valEdit.setText(Float.toString(((Node) nodes.elementAt(cgap)).value));
			}
		}

		public void newStep() {
			try {
				int steps = Integer.parseInt(stepEdit.getText());
				if (steps < 0)
					throw new Exception();
				((Gap) gaps.elementAt(cgap)).steps = steps;
				repaint();
				notifyScaleChange();
			} catch (Exception ex) {
				stepEdit.setText(Integer.toString(((Gap) gaps.elementAt(cgap)).steps));
			}
		}

		@Override
		public void itemStateChanged(ItemEvent e) {
			if (e.getSource() == it1) {
				((Gap) gaps.elementAt(cgap)).inter = LIN;
			} else if (e.getSource() == it2) {
				((Gap) gaps.elementAt(cgap)).inter = LOG;
			} else if (e.getSource() == it3) {
				((Gap) gaps.elementAt(cgap)).inter = EXP;
			} else if (e.getSource() == cs1) {
				((Gap) gaps.elementAt(cgap)).colspace = RGB;
			} else if (e.getSource() == cs2) {
				((Gap) gaps.elementAt(cgap)).colspace = HSB;
			} else if (e.getSource() == cs3) {
				((Gap) gaps.elementAt(cgap)).colspace = RHSB;
			}
			repaint();
			notifyScaleChange();
		}

		@Override
		public void paint(Graphics g) {
/*
	for(int i=0; i<256; i++) //y
		for(int j=0; j<r; j++) //x
			pixels[i * r + j] =
			 Color.HSBtoRGB((float)0.0,(float)0.0,(float)i/255);
  img = createImage(new MemoryImageSource(r, 256,
	                 	      ColorModel.getRGBdefault(), pixels, 0, r));
*/
			width = this.getSize().width;
			barSize = (width - 2 * hgap) / gaps.size();
			for (int i = 0; i < gaps.size(); i++) {
				for (int x = skip; x < barSize - skip; x++) {
					g.setColor(new Color(encodeValue((((Node) nodes.elementAt(i + 1)).value - ((Node) nodes.elementAt(i)).value) / barSize * x + ((Node) nodes.elementAt(i)).value)));
					g.drawLine(hgap + x + i * barSize, vgap, hgap + x + i * barSize, height + vgap);
				}
			}
			g.setColor(Color.black);
			g.drawRect(hgap - 3, vgap - 1, width - 2 * hgap + 5, height + 2);
		}

		boolean drag = false;
		Color cdrag;

		public void valueSetup(int cur) {
			cgap = cur;
			Panel valPanel = new Panel();
			// following string:"value: "
			valPanel.add(new Label(res.getString("value_1")));
			valPanel.add(valEdit);
			valEdit.setText(new Float(((Node) nodes.elementAt(cgap)).value).toString());
			// following string:"Change value"
			OKDialog dlg = new OKDialog(CManager.getAnyFrame(this), res.getString("Change_value"), false);
			dlg.add(valPanel);
			dlg.setVisible(true);
			newVal();
		}

		public void gapSetup(int cur) {
			Panel gapEdit = new Panel();
			Panel pg1 = new Panel();
			Panel pg2 = new Panel();
			Panel pg3 = new Panel();
			gapEdit.setLayout(new BorderLayout());
			pg1.setLayout(new GridLayout(1, 2));
			pg2.setLayout(new GridLayout(4, 1));
			pg3.setLayout(new GridLayout(4, 1));

			gapEdit.add(pg1, "Center");
			gapEdit.add(help, "South");
			pg1.add(pg2);
			pg1.add(pg3);
			pg2.add(cs1);
			pg2.add(cs2);
			pg2.add(cs3);
			pg3.add(it1);
			pg3.add(it2);
			pg3.add(it3);
			// following string:"steps:"
			pg2.add(new Label(res.getString("steps_1")));
			pg3.add(stepEdit);
			Gap current = (Gap) gaps.elementAt(cur);
			stepEdit.setText(new Integer((current).steps).toString());
			if (current.colspace == 0) {
				colSpace.setSelectedCheckbox(cs1);
			} else if (current.colspace == 1) {
				colSpace.setSelectedCheckbox(cs2);
			} else {
				colSpace.setSelectedCheckbox(cs3);
			}
			if (current.inter == 0) {
				interType.setSelectedCheckbox(it1);
			} else if (current.inter == 1) {
				interType.setSelectedCheckbox(it2);
			} else {
				interType.setSelectedCheckbox(it3);
			}

			// following string:"Gap parameters"
			OKDialog dlg = new OKDialog(CManager.getAnyFrame(this), res.getString("Gap_parameters"), false);
			dlg.add(gapEdit);
			dlg.setVisible(true);
			newStep();
		}

		@Override
		public void mousePressed(MouseEvent e) {
			cgap = (e.getX() - hgap) / barSize;
			place = (e.getX() - hgap - cgap * barSize);
			fplace = (float) place / barSize;

			if (fplace < ctol) {
				cdrag = ((Node) nodes.elementAt(cgap)).more;
			} else if (fplace > 1 - ctol) {
				cdrag = ((Node) nodes.elementAt(cgap + 1)).less;
			}
		}

		@Override
		public void mouseClicked(MouseEvent e) {
//System.out.println("* mouseClicked");
			cgap = (e.getX() - hgap) / barSize;
			place = (e.getX() - hgap - cgap * barSize);
			fplace = (float) place / barSize;

			if (place <= skip) {
				valueSetup(cgap);
				pushUndo();
			} else if (place >= barSize - skip) {
				valueSetup(cgap + 1);
				pushUndo();
			} else

			if (fplace < ctol) {
//      pushUndo();
				// following string:"Select color"
				if (cdialog == null) {
					cdialog = new ColorDlg(CManager.getAnyFrame(null));
				}
				Color toChange = new Color(((Node) nodes.elementAt(cgap)).more.getRGB());
				cdialog.selectColor(this, new Integer(cgap * 2), ((Node) nodes.elementAt(cgap)).more, dynamic);
				Color edited = new Color(((Node) nodes.elementAt(cgap)).more.getRGB());
				if (!cdialog.wasCancelled()) {
					((Node) nodes.elementAt(cgap)).more = toChange;
					pushUndo();
					((Node) nodes.elementAt(cgap)).more = edited;
				}
			} else if (fplace > 1 - ctol) {
//      pushUndo();
				// following string:"Select color"
				if (cdialog == null) {
					cdialog = new ColorDlg(CManager.getAnyFrame(null));
				}
				Color toChange = new Color(((Node) nodes.elementAt(cgap + 1)).less.getRGB());
				cdialog.selectColor(this, new Integer(cgap * 2 + 1), ((Node) nodes.elementAt(cgap + 1)).less, dynamic);
				Color edited = new Color(((Node) nodes.elementAt(cgap + 1)).less.getRGB());
				if (!cdialog.wasCancelled()) {
					((Node) nodes.elementAt(cgap + 1)).less = toChange;
					pushUndo();
					((Node) nodes.elementAt(cgap + 1)).less = edited;
				}
			} else {
				pushUndo();
				gapSetup(cgap);
			}
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			if (!drag)
				return;
			pushUndo();
//System.out.println("* mouseReleased after drag");
			cgap = (e.getX() - hgap) / barSize;
			place = (e.getX() - hgap - cgap * barSize);
			fplace = (float) place / barSize;

			if (fplace < ctol) {
				((Node) nodes.elementAt(cgap)).more = cdrag;
			} else if (fplace > 1 - ctol) {
				((Node) nodes.elementAt(cgap + 1)).less = cdrag;
			} else {
				((Node) nodes.elementAt(cgap)).more = cdrag;
				((Node) nodes.elementAt(cgap + 1)).less = cdrag;
			}

			drag = false;
			repaint();
			notifyScaleChange();
		}

		@Override
		public void mouseExited(MouseEvent e) {
		}

		@Override
		public void mouseEntered(MouseEvent e) {
		}

		@Override
		public void mouseMoved(MouseEvent e) {
		}

		@Override
		public void mouseDragged(MouseEvent e) {
			drag = true;
		}
	}

	/**
	 * Pass parameter string to setup color scale
	 */
	@Override
	public void setParameters(String par) {
		if (par == null || par == "")
			return;
		filename = par;
		if (filename != null && !filename.equals("")) {
			loadFromFile(filename);
		}
	}

	/**
	 * Pass parameter string to setup color scale
	 */
	@Override
	public String getParameters() {
		return filename;
	}
}
//~ID
