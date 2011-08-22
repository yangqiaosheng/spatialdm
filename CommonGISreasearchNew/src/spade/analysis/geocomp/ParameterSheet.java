package spade.analysis.geocomp;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ResourceBundle;

import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.DialogContent;
import spade.lib.lang.Language;
import spade.vis.dmap.DGeoLayer;
import spade.vis.geometry.RasterGeometry;
import spade.vis.geometry.RealPoint;
import spade.vis.geometry.RealRectangle;
import spade.vis.map.MapDraw;
import spade.vis.space.LayerManager;

public class ParameterSheet extends Panel implements DialogContent, ActionListener, ItemListener {
	static ResourceBundle res = Language.getTextResource("spade.analysis.geocomp.Res");
	protected String err = null;

	TextField eCol;
	TextField eRow;
	TextField eDX;
	TextField eDY;
	TextField eXbeg;
	TextField eYbeg;
	TextField eXend;
	TextField eYend;
	Checkbox eIntr;
	Checkbox eGeog;
	List cLayer;
	Button selAll;
	Button selNone;
	Checkbox rbOR;
	Checkbox rbAND;
	protected RasterGeometry rgeom = null;
	protected MapDraw map = null;
	protected Rectangle rect = null, rectOld = null;
	protected Graphics g;
	protected LayerManager lman;

	private RealPoint beg = null;
	private RealPoint end = null;
	private Point mBeg = null;
	private Point mEnd = null;

	public ParameterSheet(RasterGeometry rg, MapDraw map, LayerManager lman) {
		super();
		this.lman = lman;
		rgeom = rg;
		this.map = map;
		addComponents();
	}

/*
  public ParameterSheet(RasterGeometry rg, MapDraw map) {
    super();
    rgeom=rg;
    this.map=map;
    addComponents();
  }
*/
	public void addComponents() {
		g = map.getGraphics();
		g.setXORMode(Color.white);

		beg = new RealPoint();
		end = new RealPoint();
		beg.x = rgeom.rx1;//rgeom.Xbeg;
		beg.y = rgeom.ry1;//rgeom.Ybeg;
		end.x = rgeom.rx2;//rgeom.Xbeg + rgeom.DX * rgeom.Col;
		end.y = rgeom.ry2;//rgeom.Ybeg + rgeom.DY * rgeom.Row;
		mBeg = map.getMapContext().getScreenPoint(beg);
		mEnd = map.getMapContext().getScreenPoint(end);
		rect = new Rectangle(Math.min(mBeg.x, mEnd.x), Math.min(mBeg.y, mEnd.y), Math.abs(mEnd.x - mBeg.x), Math.abs(mEnd.y - mBeg.y));
		g.drawRect(rect.x, rect.y, rect.width, rect.height);

		Panel p = new Panel(new GridLayout(5, 5));
		setLayout(new BorderLayout());
		eCol = new TextField(Integer.toString(rgeom.Col));
		eRow = new TextField(Integer.toString(rgeom.Row));
		eDX = new TextField(Float.toString(rgeom.DX));
		eDY = new TextField(Float.toString(rgeom.DY));
		eXbeg = new TextField(Float.toString(rgeom.rx1));
		eYbeg = new TextField(Float.toString(rgeom.ry1));
		eXend = new TextField(Float.toString(rgeom.rx2));
		eYend = new TextField(Float.toString(rgeom.ry2));
		// following string: "intepolate"
		eIntr = new Checkbox(res.getString("intepolate"), rgeom.Intr);
		// following string:"geographic"
		eGeog = new Checkbox(res.getString("geographic"), rgeom.Geog);
		selAll = new Button(res.getString("Select_All"));
		selNone = new Button(res.getString("Select_None"));

		CheckboxGroup cbLogic = new CheckboxGroup();

		rbOR = new Checkbox(res.getString("union"), cbLogic, true);
		rbAND = new Checkbox(res.getString("intersection"), cbLogic, false);

// subject to change
//    eXend.setEditable(false);
//    eYend.setEditable(false);
		eCol.setEditable(false);
		eRow.setEditable(false);

		cLayer = new List(6, true);
		cLayer.addItemListener(this);
//    cLayer.add("Select layer");
		for (int i = 0; i < lman.getLayerCount(); i++) {
			cLayer.add(lman.getGeoLayer(i).getName());
		}

		// following string: "X start"
		p.add(new Label(res.getString("Xstart")));
		p.add(eXbeg);
		// following string: "Y start"
		p.add(new Label(res.getString("Ystart")));
		p.add(eYbeg);
		// following string: "X end"
		p.add(new Label(res.getString("Xend")));
		p.add(eXend);
		// following string: "Y end"
		p.add(new Label(res.getString("Yend")));
		p.add(eYend);
		// following string: "Step X"
		p.add(new Label(res.getString("Step_X")));
		p.add(eDX);
		// following string: "Step Y"
		p.add(new Label(res.getString("Step_Y")));
		p.add(eDY);
		// following string: "Columns"
		p.add(new Label(res.getString("Columns")));
		p.add(eCol);
		// following string: "Rows"
		p.add(new Label(res.getString("Rows")));
		p.add(eRow);
		p.add(eIntr);
		p.add(eGeog);
		p.add(new Label(""));
		p.add(new Label(""));

		Panel pp = new Panel(new BorderLayout());
		Panel pl = new Panel(new GridLayout(4, 1, 8, 8));
		Panel pr = new Panel(new ColumnLayout());
		Panel plr = new Panel(new GridLayout(1, 3, 8, 8));
		Panel pb = new Panel(new GridLayout(4, 1, 8, 8));

		pr.add(new Label(res.getString("Bounds_as_in_layer")));
		pr.add(cLayer);
		pl.add(new Label(""));
		pl.add(selAll);
		pl.add(selNone);
		pl.add(new Label(""));
		pb.add(new Label(""));
		pb.add(rbOR);
		pb.add(rbAND);
		pb.add(new Label(""));
		plr.add(pr);
		plr.add(pl);
		plr.add(pb);
		pp.add(plr, "Center");

		add(p, "North");
		add(pp, "Center");

		eCol.addActionListener(this);
		eRow.addActionListener(this);
		eDX.addActionListener(this);
		eDY.addActionListener(this);
		eXbeg.addActionListener(this);
		eYbeg.addActionListener(this);
		eXend.addActionListener(this);
		eYend.addActionListener(this);

		selAll.addActionListener(this);
		selNone.addActionListener(this);
		rbAND.addItemListener(this);
		rbOR.addItemListener(this);

		update();
	}

	@Override
	public void actionPerformed(ActionEvent ev) {
		if (ev.getSource().equals(eCol) || ev.getSource().equals(eRow) || ev.getSource().equals(eDX) || ev.getSource().equals(eDY)) {
			TextField tf = (TextField) ev.getSource();
			String str = tf.getText();
			if (str != null) {
				try {
					float val = Float.valueOf(str).floatValue();
					if (val > 0)
						if (ev.getSource().equals(eCol)) {
							eDX.setText(String.valueOf((rgeom.rx2 - rgeom.rx1) / val));
						} else if (ev.getSource().equals(eRow)) {
							eDY.setText(String.valueOf((rgeom.ry2 - rgeom.ry1) / val));
						} else if (ev.getSource().equals(eDX)) {
							eCol.setText(String.valueOf(Math.round((rgeom.rx2 - rgeom.rx1) / val)));
						} else if (ev.getSource().equals(eDY)) {
							eRow.setText(String.valueOf(Math.round((rgeom.ry2 - rgeom.ry1) / val)));
						}
				} catch (NumberFormatException nfe) {
				}
			}
		} else if (ev.getSource().equals(selAll)) {
			for (int i = 0; i < cLayer.getItemCount(); i++) {
				cLayer.select(i);
			}
		} else if (ev.getSource().equals(selNone)) {
			for (int i = 0; i < cLayer.getItemCount(); i++) {
				cLayer.deselect(i);
			}
		}
		update();
	}

	@Override
	public void itemStateChanged(ItemEvent ev) {
		update();
	}

	public void update() {
		RealRectangle bounds = null;
		for (int i = 0; i < cLayer.getItemCount(); i++) {
			DGeoLayer gl = (DGeoLayer) lman.getGeoLayer(i);
			RealRectangle rect = gl.getWholeLayerBounds();
			if (cLayer.isIndexSelected(i))
				if (bounds == null) {
					bounds = rect;
				} else if (rect != null)
					if (rbOR.getState()) {
						bounds = bounds.union(rect);
					} else {
						bounds = bounds.intersect(rect);
					}
		}

		if (bounds != null) {
			eXbeg.setText(new Float(bounds.rx1).toString());
			eXend.setText(new Float(bounds.rx2).toString());
			eYbeg.setText(new Float(bounds.ry1).toString());
			eYend.setText(new Float(bounds.ry2).toString());
		}

		try {
			eCol.setText(new Integer(Math.round((new Float(eXend.getText()).floatValue() - new Float(eXbeg.getText()).floatValue()) / new Float(eDX.getText()).floatValue())).toString());
		} catch (NumberFormatException ex) {
			eCol.setText(res.getString("ERROR"));
		}
		try {
			eRow.setText(new Integer(Math.round((new Float(eYend.getText()).floatValue() - new Float(eYbeg.getText()).floatValue()) / new Float(eDY.getText()).floatValue())).toString());
		} catch (NumberFormatException ex) {
			eRow.setText(res.getString("ERROR"));
		}
// and then correct bounding box
		try {
			eXend.setText(new Float(new Float(eXbeg.getText()).floatValue() + new Float(eDX.getText()).floatValue() * (Integer.parseInt(eCol.getText()))).toString());
		} catch (NumberFormatException ex) {
			eXend.setText(res.getString("ERROR"));
		}
		try {
			eYend.setText(new Float(new Float(eYbeg.getText()).floatValue() + new Float(eDY.getText()).floatValue() * (Integer.parseInt(eRow.getText()))).toString());
		} catch (NumberFormatException ex) {
			eYend.setText(res.getString("ERROR"));
		}

// clear old
		g.drawRect(rect.x, rect.y, rect.width, rect.height);

/*
    beg = new RealPoint();
    end = new RealPoint();
    beg.x = rgeom.Xbeg;
    beg.y = rgeom.Ybeg;
    end.x = rgeom.Xbeg + rgeom.DX * rgeom.Col;
    end.y = rgeom.Ybeg + rgeom.DY * rgeom.Row;
    mBeg = map.getMapContext().getScreenPoint(beg);
    mEnd = map.getMapContext().getScreenPoint(end);
    rect = new Rectangle(Math.min(mBeg.x, mEnd.x), Math.min(mBeg.y, mEnd.y), Math.abs(mEnd.x - mBeg.x), Math.abs(mEnd.y - mBeg.y));
*/

		beg = new RealPoint();
		end = new RealPoint();
		beg.x = new Float(eXbeg.getText()).floatValue();
		beg.y = new Float(eYbeg.getText()).floatValue();
		end.x = beg.x + new Float(eDX.getText()).floatValue() * (Integer.parseInt(eCol.getText()));
		end.y = beg.y + new Float(eDY.getText()).floatValue() * (Integer.parseInt(eRow.getText()));
		mBeg = map.getMapContext().getScreenPoint(beg);
		mEnd = map.getMapContext().getScreenPoint(end);
		rect = new Rectangle(Math.min(mBeg.x, mEnd.x), Math.min(mBeg.y, mEnd.y), Math.abs(mEnd.x - mBeg.x), Math.abs(mEnd.y - mBeg.y));

// draw new
		g.drawRect(rect.x, rect.y, rect.width, rect.height);

	}

	public void updateGeometry(RasterGeometry par) {
		update();
		par.Col = Integer.parseInt(eCol.getText());
		par.Row = Integer.parseInt(eRow.getText());
		par.DX = new Float(eDX.getText()).floatValue();
		par.DY = new Float(eDY.getText()).floatValue();
		par.Xbeg = new Float(eXbeg.getText()).floatValue() + par.DX / 2;
		par.Ybeg = new Float(eYbeg.getText()).floatValue() + par.DY / 2;
//      par.init((float)par.Xbeg, (float)par.Ybeg, new Float(eXend.getText()).floatValue(), new Float(eYend.getText()).floatValue());
		par.init(new Float(eXbeg.getText()).floatValue(), new Float(eYbeg.getText()).floatValue(), new Float(eXend.getText()).floatValue(), new Float(eYend.getText()).floatValue());
		par.Intr = eIntr.getState();
		par.Geog = eGeog.getState();
	}

	public void clearHighlighting() {
		beg = new RealPoint();
		end = new RealPoint();
		beg.x = new Float(eXbeg.getText()).floatValue();
		beg.y = new Float(eYbeg.getText()).floatValue();
		end.x = beg.x + new Float(eDX.getText()).floatValue() * (Integer.parseInt(eCol.getText()));
		end.y = beg.y + new Float(eDY.getText()).floatValue() * (Integer.parseInt(eRow.getText()));
		mBeg = map.getMapContext().getScreenPoint(beg);
		mEnd = map.getMapContext().getScreenPoint(end);
		rect = new Rectangle(Math.min(mBeg.x, mEnd.x), Math.min(mBeg.y, mEnd.y), Math.abs(mEnd.x - mBeg.x), Math.abs(mEnd.y - mBeg.y));

		g.drawRect(rect.x, rect.y, rect.width, rect.height);

		map.getGraphics().setPaintMode();
	}

	@Override
	public String getErrorMessage() {
		return err;
	}

	@Override
	public boolean canClose() {
		err = null;
		update();
		if (eXend.getText().equals(res.getString("ERROR")) || eYend.getText().equals(res.getString("ERROR"))) {
			err = res.getString("Invalid_parameters");
			return false;
		}
		return true;
	}

}
