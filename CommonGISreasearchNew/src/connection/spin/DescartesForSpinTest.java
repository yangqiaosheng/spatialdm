package connection.spin;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.Choice;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.awt.TextArea;
import java.awt.TextField;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.Vector;

import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.OKDialog;

public class DescartesForSpinTest extends Frame implements WindowListener, ActionListener {
	protected DescartesForSpin descartes = null;
	protected Frame dFrame = null;
	protected TextArea log = null;
	protected Vector addedLayers = null;

	public DescartesForSpinTest() {
		super("Descartes controller");
		addWindowListener(this);
		setLayout(new BorderLayout());
		log = new TextArea(15, 40);
		add(log, "Center");
		Panel pp = new Panel(new GridLayout(3, 1, 0, 5));
		Panel p = new Panel(new FlowLayout());
		Button b = new Button("Start Descartes");
		b.addActionListener(this);
		b.setActionCommand("start");
		p.add(b);
		b = new Button("Stop Descartes");
		b.addActionListener(this);
		b.setActionCommand("stop");
		p.add(b);
		pp.add(p);
		p = new Panel(new FlowLayout());
		b = new Button("Add layer");
		b.addActionListener(this);
		b.setActionCommand("add_layer");
		p.add(b);
		b = new Button("Add raster");
		b.addActionListener(this);
		b.setActionCommand("add_raster");
		p.add(b);
		b = new Button("Remove layer");
		b.addActionListener(this);
		b.setActionCommand("remove_layer");
		p.add(b);
		pp.add(p);
		p = new Panel(new FlowLayout());
		b = new Button("Display attribute on map");
		b.addActionListener(this);
		b.setActionCommand("display_attr");
		p.add(b);
		b = new Button("Activate layer");
		b.addActionListener(this);
		b.setActionCommand("activate");
		p.add(b);
		pp.add(p);
		add(pp, "South");
		pack();
		Dimension d = getSize(), d1 = Toolkit.getDefaultToolkit().getScreenSize();
		setLocation(d1.width - d.width - 10, 10);
		show();
	}

	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		if (cmd == null)
			return;
		if (cmd.equals("start")) {
			openDescartes();
		} else if (cmd.equals("stop")) {
			closeDescartes();
		} else if (cmd.equals("add_layer")) {
			loadLayerFromDatabase();
		} else if (cmd.equals("add_raster")) {
			addRaster();
		} else if (cmd.equals("remove_layer")) {
			removeLayer();
		} else if (cmd.equals("activate")) {
			activateLayer();
		} else if (cmd.equals("display_attr")) {
			displayAttrOnMap();
		}
	}

	protected void openDescartes() {
		if (dFrame != null) {
			dFrame.toFront();
			log.append("Descartes is currently running\n");
		} else {
			if (descartes == null) {
				descartes = new DescartesForSpin();
			}
			dFrame = descartes.runDescartes(false, null);
			dFrame.addWindowListener(this);
			log.append("Descartes has been started\n");
		}
	}

	protected void closeDescartes() {
		if (descartes != null) {
			descartes.quit();
			dFrame = null;
			log.append("Descartes has bin finished\n");
			if (addedLayers != null) {
				addedLayers.removeAllElements();
			}
		} else {
			log.append("Descartes is currently not running\n");
		}
	}

	protected String getTextFromField(TextField tf) {
		if (tf == null)
			return null;
		String str = tf.getText();
		if (str == null)
			return null;
		str = str.trim();
		if (str.length() < 1)
			return null;
		return str;
	}

	protected void loadLayerFromDatabase() {
		if (dFrame == null) {
			log.append("Descartes is currently not running\n");
			return;
		}
		//construct a dialog for entering table specification
		Panel p = new Panel(new ColumnLayout());
		Panel pp = new Panel(new BorderLayout());
		pp.add(new Label("Database"), "West");
		TextField dbTF = new TextField("jdbc:oracle:thin:@spinner:1521:spin");
		pp.add(dbTF, "Center");
		p.add(pp);
		pp = new Panel(new BorderLayout());
		pp.add(new Label("User name"), "West");
		TextField userTF = new TextField("spin");
		pp.add(userTF, "Center");
		p.add(pp);
		pp = new Panel(new BorderLayout());
		pp.add(new Label("Password"), "West");
		TextField pswTF = new TextField("alive");
		pswTF.setEchoChar('*');
		pp.add(pswTF, "Center");
		p.add(pp);
		pp = new Panel(new BorderLayout());
		pp.add(new Label("Table"), "West");
		TextField tableTF = new TextField("STOCKPORT");
		pp.add(tableTF, "Center");
		p.add(pp);
		pp = new Panel(new BorderLayout());
		pp.add(new Label("Geometry field"), "West");
		TextField geomTF = new TextField("GEOM");
		pp.add(geomTF, "Center");
		p.add(pp);
		pp = new Panel(new BorderLayout());
		pp.add(new Label("Identifier field"), "West");
		TextField idTF = new TextField("ZONE_ID");
		pp.add(idTF, "Center");
		p.add(pp);
		pp = new Panel(new BorderLayout());
		pp.add(new Label("Name field"), "West");
		TextField nameTF = new TextField(20);
		pp.add(nameTF, "Center");
		p.add(pp);
		OKDialog dia = new OKDialog(this, "Specify the layer source", true);
		dia.addContent(p);
		String layerId = null;
		do {
			dia.show();
			if (dia.wasCancelled())
				return;
			layerId = descartes.readLayerFromOracle(getTextFromField(dbTF), getTextFromField(userTF), getTextFromField(pswTF), getTextFromField(tableTF), getTextFromField(idTF), getTextFromField(nameTF), getTextFromField(geomTF), null, null);
			if (layerId == null) {
				log.append("Could not load the layer into Descartes; try again\n");
			} else {
				log.append("The layer was added with the identifier " + layerId + "\n");
			}
		} while (layerId == null);
		if (layerId == null)
			return;
		if (addedLayers == null) {
			addedLayers = new Vector(10, 10);
		}
		addedLayers.addElement(layerId);
	}

	protected void addRaster() {
		if (dFrame == null) {
			log.append("Descartes is currently not running\n");
			return;
		}
		//construct a dialog for entering raster specification
		Panel p = new Panel(new ColumnLayout());
		p.add(new Label("Extent:", Label.CENTER));
		Panel pp = new Panel(new BorderLayout());
		pp.add(new Label("X0="), "West");
		TextField x0TF = new TextField("345000");
		pp.add(x0TF, "Center");
		p.add(pp);
		pp = new Panel(new BorderLayout());
		pp.add(new Label("Y0="), "West");
		TextField y0TF = new TextField("380000");
		pp.add(y0TF, "Center");
		p.add(pp);
		pp = new Panel(new BorderLayout());
		pp.add(new Label("X1="), "West");
		TextField x1TF = new TextField("410000");
		pp.add(x1TF, "Center");
		p.add(pp);
		pp = new Panel(new BorderLayout());
		pp.add(new Label("Y1="), "West");
		TextField y1TF = new TextField("425000");
		pp.add(y1TF, "Center");
		p.add(pp);
		pp = new Panel(new BorderLayout());
		pp.add(new Label("N of columns:"), "West");
		TextField nColsTF = new TextField("20");
		pp.add(nColsTF, "Center");
		p.add(pp);
		pp = new Panel(new BorderLayout());
		pp.add(new Label("N of rows:"), "West");
		TextField nRowsTF = new TextField("15");
		pp.add(nRowsTF, "Center");
		p.add(pp);
		pp = new Panel(new BorderLayout());
		pp.add(new Label("min value:"), "West");
		TextField minTF = new TextField("0");
		pp.add(minTF, "Center");
		p.add(pp);
		pp = new Panel(new BorderLayout());
		pp.add(new Label("max value:"), "West");
		TextField maxTF = new TextField("1");
		pp.add(maxTF, "Center");
		p.add(pp);
		pp = new Panel(new BorderLayout());
		pp.add(new Label("layer name:"), "West");
		TextField nameTF = new TextField("random grid");
		pp.add(nameTF, "Center");
		p.add(pp);
		OKDialog dia = new OKDialog(this, "Construct a raster", true);
		dia.addContent(p);
		String layerId = null;
		do {
			dia.show();
			if (dia.wasCancelled())
				return;
			double x0, y0, x1, y1;
			int ncols = 0, nrows = 0;
			try {
				x0 = Double.valueOf(x0TF.getText()).doubleValue();
			} catch (NumberFormatException nfe) {
				log.append("Illegal X0!\n");
				continue;
			}
			try {
				y0 = Double.valueOf(y0TF.getText()).doubleValue();
			} catch (NumberFormatException nfe) {
				log.append("Illegal Y0!\n");
				continue;
			}
			try {
				x1 = Double.valueOf(x1TF.getText()).doubleValue();
			} catch (NumberFormatException nfe) {
				log.append("Illegal X1!\n");
				continue;
			}
			try {
				y1 = Double.valueOf(y1TF.getText()).doubleValue();
			} catch (NumberFormatException nfe) {
				log.append("Illegal Y1!\n");
				continue;
			}
			if (x0 >= x1 || y0 >= y1) {
				log.append("Illegal bounds!\n");
				continue;
			}
			try {
				ncols = Integer.valueOf(nColsTF.getText()).intValue();
			} catch (NumberFormatException nfe) {
				log.append("Illegal number of columns!\n");
				continue;
			}
			if (ncols < 3) {
				log.append("Illegal number of columns!\n");
				continue;
			}
			try {
				nrows = Integer.valueOf(nRowsTF.getText()).intValue();
			} catch (NumberFormatException nfe) {
				log.append("Illegal number of rows!\n");
				continue;
			}
			if (nrows < 3) {
				log.append("Illegal number of rows!\n");
				continue;
			}
			float minv, maxv;
			try {
				minv = Float.valueOf(minTF.getText()).floatValue();
			} catch (NumberFormatException nfe) {
				log.append("Illegal minimum value!\n");
				continue;
			}
			try {
				maxv = Float.valueOf(maxTF.getText()).floatValue();
			} catch (NumberFormatException nfe) {
				log.append("Illegal maximum value!\n");
				continue;
			}
			if (maxv <= minv) {
				log.append("Maximum less than minimum!\n");
				continue;
			}
			float val[][] = new float[ncols][nrows];
			float diff = maxv - minv;
			for (int i = 0; i < ncols; i++) {
				for (int j = 0; j < nrows; j++) {
					val[i][j] = minv + (float) Math.random() * diff;
				}
			}
			double dx = (x1 - x0) / ncols, dy = (y1 - y0) / nrows;
			//add the "raster" to Descartes
			layerId = descartes.addRaster(ncols, nrows, dx, dy, x0, y0, val, nameTF.getText());
			if (layerId == null) {
				log.append("Could not load the raster layer into Descartes; try again\n");
			} else {
				log.append("The raster layer was added with the identifier " + layerId + "\n");
			}
		} while (layerId == null);
		if (layerId == null)
			return;
		if (addedLayers == null) {
			addedLayers = new Vector(10, 10);
		}
		addedLayers.addElement(layerId);
	}

	protected void removeLayer() {
		if (addedLayers == null || addedLayers.size() < 1) {
			log.append("No layer has been added yet!\n");
			return;
		}
		List lst = new List(5);
		for (int i = 0; i < addedLayers.size(); i++) {
			lst.add((String) addedLayers.elementAt(i));
		}
		Panel p = new Panel(new BorderLayout());
		p.add(lst, "Center");
		Checkbox cb = new Checkbox("remove the table", false);
		p.add(cb, "South");
		OKDialog dia = new OKDialog(this, "What layer to remove?", true);
		dia.addContent(p);
		dia.show();
		if (dia.wasCancelled() || lst.getSelectedIndex() < 0)
			return;
		int idx = lst.getSelectedIndex();
		descartes.removeLayer((String) addedLayers.elementAt(idx), cb.getState());
		log.append("Removed the layer with the identifier " + (String) addedLayers.elementAt(idx) + "\n");
		addedLayers.removeElementAt(idx);
	}

	protected void activateLayer() {
		if (addedLayers == null || addedLayers.size() < 1) {
			log.append("No layer has been added yet!\n");
			return;
		}
		List lst = new List(5);
		for (int i = 0; i < addedLayers.size(); i++) {
			lst.add((String) addedLayers.elementAt(i));
		}
		OKDialog dia = new OKDialog(this, "Select layer to activate", true);
		dia.addContent(lst);
		dia.show();
		if (dia.wasCancelled() || lst.getSelectedIndex() < 0)
			return;
		int idx = lst.getSelectedIndex();
		descartes.activateLayer((String) addedLayers.elementAt(idx));
		log.append("Activated the layer with the identifier " + (String) addedLayers.elementAt(idx) + "\n");
	}

	protected void displayAttrOnMap() {
		if (addedLayers == null || addedLayers.size() < 1) {
			log.append("No layer has been added yet!\n");
			return;
		}
		Choice lchoice = new Choice();
		for (int i = 0; i < addedLayers.size(); i++) {
			lchoice.add((String) addedLayers.elementAt(i));
		}
		Panel p = new Panel(new GridLayout(3, 1));
		p.add(lchoice);
		p.add(new Label("Attribute to display:"));
		TextField tf = new TextField(20);
		p.add(tf);
		OKDialog dia = new OKDialog(this, "What attribute to display?", true);
		dia.addContent(p);
		dia.show();
		if (dia.wasCancelled())
			return;
		String attrId = tf.getText();
		if (attrId == null)
			return;
		attrId = attrId.trim();
		if (attrId.length() < 1)
			return;
		int idx = lchoice.getSelectedIndex();
		String layerId = (String) addedLayers.elementAt(idx);
		Vector v = new Vector(1, 1);
		v.addElement(attrId);
		if (descartes.displayAttrOnMap(layerId, v)) {
			log.append("Displayed the attribute " + attrId + " in the layer with the identifier " + layerId + "\n");
		} else {
			log.append("Could not display the attribute " + attrId + " in the layer with the identifier " + layerId + "\n");
		}
	}

	public static void main(String args[]) {
		DescartesForSpinTest ds = new DescartesForSpinTest();
	}

	public void windowClosing(WindowEvent e) {
		if (e.getSource().equals(this)) {
			closeDescartes();
			dispose();
			System.exit(0);
		}
	}

	public void windowClosed(WindowEvent e) {
		if (e.getSource().equals(dFrame)) {
			dFrame = null;
			log.append("Descartes quitted\n");
			if (addedLayers != null) {
				addedLayers.removeAllElements();
			}
		}
	}

	public void windowOpened(WindowEvent e) {
	};

	public void windowIconified(WindowEvent e) {
	};

	public void windowDeiconified(WindowEvent e) {
	};

	public void windowActivated(WindowEvent e) {
	};

	public void windowDeactivated(WindowEvent e) {
	};
}