package spade.analysis.geocomp;

import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.geocomp.functions.Function;
import spade.analysis.geocomp.functions.Max;
import spade.analysis.geocomp.functions.Mean;
import spade.analysis.geocomp.functions.Median;
import spade.analysis.geocomp.functions.Min;
import spade.analysis.geocomp.functions.Mode;
import spade.analysis.geocomp.functions.RMS;
import spade.analysis.geocomp.functions.Range;
import spade.analysis.geocomp.functions.Sum;
import spade.analysis.system.ESDACore;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Destroyable;
import spade.lib.basicwin.Line;
import spade.lib.lang.Language;
import spade.vis.database.DataRecord;
import spade.vis.database.DataTable;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DGeoObject;
import spade.vis.geometry.RasterGeometry;
import spade.vis.geometry.RealRectangle;
import spade.vis.space.GeoLayer;

public class TableCreatePanel extends Panel implements ActionListener, ItemListener, PropertyChangeListener, Destroyable {
	static ResourceBundle res = Language.getTextResource("spade.analysis.geocomp.Res");

	protected static final int initialSize = 20;

	protected Vector layers;
	protected RealRectangle bounds;
	protected DataTable table;
	protected DGeoLayer layer;
	protected ESDACore core;

	protected Checkbox union;
	protected Checkbox intersect;
	protected TextField tCol;
	protected int col;
	protected TextField tRow;
	protected int row;
	protected TextField tAsp;
	protected float asp = 1;
	protected Checkbox lock;

	protected Checkbox all;
	protected Checkbox some;
	protected Checkbox none;
	protected Checkbox[][] fcb;

	protected boolean isDestroyed = false;

	public TableCreatePanel(Vector layers, Checkbox[][] fcb, DataTable table, DGeoLayer layer, ESDACore core) {
		this.layers = layers;
		this.table = table;
		this.layer = layer;
		this.core = core;
		this.fcb = fcb;

		table.addPropertyChangeListener(this);
		layer.addPropertyChangeListener(this);

		setLayout(new ColumnLayout());

		Panel p1 = new Panel(new GridLayout(2, 1));
		CheckboxGroup comb = new CheckboxGroup();
		union = new Checkbox(res.getString("union"), false, comb);
		intersect = new Checkbox(res.getString("intersection"), true, comb);
		union.addItemListener(this);
		intersect.addItemListener(this);
		p1.add(union);
		p1.add(intersect);
		add(p1);
		add(new Line(false));

		recalculateBounds();
		if (bounds.getWidth() > bounds.getHeight()) {
			col = initialSize;
			row = (int) (bounds.getHeight() / (bounds.getWidth() / initialSize));
		} else {
			row = initialSize;
			col = (int) (bounds.getWidth() / (bounds.getHeight() / initialSize));
		}

		Panel p2 = new Panel(new GridLayout(2, 2));
		p2.add(new Label(res.getString("Columns")));
		tCol = new TextField(Integer.toString(col), 5);
		tCol.addActionListener(this);
		p2.add(tCol);
		p2.add(new Label(res.getString("Rows")));
		tRow = new TextField(Integer.toString(row), 5);
		tRow.addActionListener(this);
		p2.add(tRow);
		add(p2);

		Panel p3 = new Panel();
		p3.add(new Label(res.getString("Aspect_ratio")));
		tAsp = new TextField("1.0", 5);
		tAsp.addActionListener(this);
		p3.add(tAsp);
		add(p3);
		lock = new Checkbox(res.getString("lock"), true);
//    lock.addItemListener(this);
		add(lock);

		add(new Line(false));
		Panel pm = new Panel(new GridLayout(3, 1));
		CheckboxGroup combM = new CheckboxGroup();
// res.getString("intersection")
		all = new Checkbox("all cells are added", false, combM);
		some = new Checkbox("with some defined values", false, combM);
		none = new Checkbox("cells with all data", true, combM);

		all.addItemListener(this);
		some.addItemListener(this);
		none.addItemListener(this);
		pm.add(all);
		pm.add(some);
		pm.add(none);
		add(pm);

		update();
	}

	@Override
	public void actionPerformed(ActionEvent ev) {
		if (!ev.getActionCommand().equals("closed")) {

			try {
				col = Integer.parseInt(tCol.getText());
			} catch (Exception ex) {
			}

			try {
				row = Integer.parseInt(tRow.getText());
			} catch (Exception ex) {
			}

			try {
				asp = new Float(tAsp.getText()).floatValue();
			} catch (Exception ex) {
			}

			if (ev.getSource() == tAsp) {
				float d = (float) Math.sqrt(bounds.getWidth() / asp * bounds.getHeight() / (col * row));
				col = Math.round(bounds.getWidth() / asp / d);
				row = Math.round(bounds.getHeight() / d);
			} else if (lock.getState()) {
				if (ev.getSource() == tCol) {
					row = Math.round(bounds.getHeight() / (bounds.getWidth() / col / asp));
				} else if (ev.getSource() == tRow) {
					col = Math.round(bounds.getWidth() / (bounds.getHeight() / row * asp));
				}
			} else {
				asp = bounds.getWidth() / col / (bounds.getHeight() / row);
			}

			tRow.setText(Integer.toString(row));
			tCol.setText(Integer.toString(col));
			tAsp.setText(Float.toString(asp));

			update();

		}
	}

	private void update() {

		GeoLayer clayer;
/*
    DataTable newTable = new DataTable();
    newTable.setName(table.getName());
    Parameter par = new Parameter();
    par.setName("CustomParameter");
    newTable.addParameter(par);
    Attribute parent = new Attribute(IdMaker.makeId("par", newTable), AttributeTypes.real);
    parent.setName("GridValues");
    for (int i=0; i < layers.size(); i++) {
      clayer = (GeoLayer)layers.elementAt(i);
      Attribute child = new Attribute(IdMaker.makeId("id", newTable), AttributeTypes.real);
      child.setName(table.getAttribute(i).getName());
      TimeMoment tVal = new TimeCount(i);
      par.addValue(tVal);
      child.addParamValPair("CustomParameter", tVal);
      parent.addChild(child);
      newTable.addAttribute(child);
    }
*/
		table.removeAllData();

		double stepX = (double) bounds.getWidth() / col;
		double stepY = (double) bounds.getHeight() / row;

// the following part needs heavy optimization...

		String id;
		DataRecord rec;
		RasterGeometry rg;
		double xx, yy;

		for (int y = 0; y < row; y++) {
			yy = Math.min(bounds.ry1, bounds.ry2)/*+stepY/2*/+ y * stepY;
			for (int x = 0; x < col; x++) {
				xx = Math.min(bounds.rx1, bounds.rx2)/*+stepX/2*/+ x * stepX;

				id = Integer.toString(x) + "_" + Integer.toString(y);
				rec = new DataRecord(id);

// this code seems to become correct after half-step shift
				rec.addAttrValue(String.valueOf(xx + stepX / 2));
				rec.addAttrValue(String.valueOf(yy + stepY / 2));

				rec.addAttrValue(String.valueOf(xx));
				rec.addAttrValue(String.valueOf(yy));
				rec.addAttrValue(String.valueOf(xx + stepX));
				rec.addAttrValue(String.valueOf(yy + stepY));

				int meaningful = 0;
				int total = 0;

				for (int i = 0; i < layers.size(); i++) {
					clayer = (GeoLayer) layers.elementAt(i);

/**/				rg = (RasterGeometry) (((DGeoObject) clayer.getObjectAt(0)).getGeometry()); //getRaster(layer, ui);
					if (rg == null)
						return;

					for (int func = 0; func < TableFromRaster.functions.length; func++)
						if (fcb[i][func].getState()) {

							total++;

							Function op = null;
							switch (func) {
							case TableFromRaster.fMean:
								op = new Mean();
								break;
							case TableFromRaster.fMedian:
								op = new Median();
								break;
							case TableFromRaster.fRMS:
								op = new RMS();
								break;
							case TableFromRaster.fMax:
								op = new Max();
								break;
							case TableFromRaster.fMin:
								op = new Min();
								break;
							case TableFromRaster.fMaxMin:
								op = new Range();
								break;
							case TableFromRaster.fSum:
								op = new Sum();
								break;
							case TableFromRaster.fMode:
								op = new Mode();
								break;
							}
							if (op == null) {
								continue;
							}

//            float val = rg.getAggregatedValue(xx, yy, stepX, stepY);
							float val = rg.getAggregatedValue((float) (xx + stepX / 2), (float) (yy + stepY / 2), (float) stepX, (float) stepY, op);
							if (Float.isNaN(val)) {
								rec.addAttrValue("");
							} else {
								rec.addAttrValue(new Float(val));
								meaningful++;
							}
						}
				}
				if (all.getState() || some.getState() && meaningful > 0 || none.getState() && meaningful == total) {
					table.addDataRecord(rec);
				}
			}
		}
		core.getHighlighterForContainer(table.getContainerIdentifier()).clearSelection(null);

		layer.setWholeLayerBounds(bounds);
		table.notifyPropertyChange("data_updated", null, null);

/*
    // P.G. here should be notification about change of "ObjectSet"
    layer.notifyPropertyChange("ObjectSet",null,null);
    // ~P.G.
    layer.notifyPropertyChange("ObjectData",null,null);
*/

//    ui.showMessage("Done.");
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		recalculateBounds();

		float d = (float) Math.sqrt(bounds.getWidth() / asp * bounds.getHeight() / (col * row));
		col = Math.round(bounds.getWidth() / asp / d);
		row = Math.round(bounds.getHeight() / d);

		tRow.setText(Integer.toString(row));
		tCol.setText(Integer.toString(col));

		update();
	}

	private void recalculateBounds() {
		for (int i = 0; i < layers.size(); i++) {
			GeoLayer layer = (GeoLayer) layers.elementAt(i);

			RasterGeometry rg = (RasterGeometry) (((DGeoObject) layer.getObjectAt(0)).getGeometry());
			if (rg == null)
				return;

			if (bounds == null) {
				bounds = new RealRectangle(rg.rx1, rg.ry1, rg.rx2, rg.ry2);
			} else if (union.getState()) {
				bounds = bounds.union(rg);
			} else {
				bounds = bounds.intersect(rg);
			}
		}
	}

	/**
	 * This method gets called when a bound property is changed.
	 * @param evt A PropertyChangeEvent object describing the event source
	 *   	and the property that has changed.
	 */

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if ((evt.getSource() == table || evt.getSource() == layer) && evt.getPropertyName().equals("destroyed")) {
			isDestroyed = true;
		}
	}

	/**
	 * Makes necessary operations for destroying, e.g. unregisters from
	 * listening highlighting and other events.
	 */
	@Override
	public void destroy() {
		table.removePropertyChangeListener(this);
		layer.removePropertyChangeListener(this);
	}

	/**
	 * Replies whether is destroyed or not
	 */
	@Override
	public boolean isDestroyed() {
		return isDestroyed;
	}
}