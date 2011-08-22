package spade.analysis.tools.somlink;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Choice;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.Panel;
import java.awt.Point;
import java.awt.ScrollPane;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Vector;

import spade.analysis.classification.ClassBroadcastPanel;
import spade.analysis.classification.QualitativeClassifier;
import spade.analysis.system.ESDACore;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Destroyable;
import spade.lib.basicwin.Dialogs;
import spade.lib.basicwin.FlexibleGridLayout;
import spade.lib.basicwin.ImageCanvas;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.RowLayout;
import spade.lib.basicwin.Slider;
import spade.lib.basicwin.SplitLayout;
import spade.lib.color.ColorScale2D;
import spade.lib.util.FloatArray;
import spade.lib.util.IntArray;
import spade.lib.util.NumValManager;
import spade.lib.util.StringUtil;
import spade.vis.database.Attribute;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataRecord;
import spade.vis.database.DataTable;
import spade.vis.dmap.DGeoLayer;
import spade.vis.geometry.RealPoint;
import spade.vis.util.IndexMapView;
import spade.vis.util.ParamValIndexView;
import useSOM.SOMCellInfo;
import useSOM.SOMResult;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Oct 30, 2009
 * Time: 4:16:14 PM
 * Displays a matrix representing SOM results.
 */
public class SOMMatrixView extends Panel implements ItemListener, ActionListener, WindowListener, Destroyable {
	/**
	 * The system's core
	 */
	protected ESDACore core = null;
	/**
	 * Describes an application of SOM to some data and contains the
	 * SOM results and everything that is needed for building the matrix view.
	 */
	public SOMApplInfo somInfo = null;
	/**
	 * A matrix of components each representing one SOM cell
	 */
	protected SOMCellView cellViews[][] = null;
	/**
	 * Used to generate unique names
	 */
	public static int instanceN = 0;
	/**
	 * The minimum and maximum N of k-means clusters
	 */
	//protected int minNClusters=0, maxNClusters=0;
	/**
	 * The index of the first column containing k-means cluster labels
	 * in the table with SOM neurons (tblSOMneuro in SOMApplInfo)
	 */
	//protected int neurClColIdx =-1;
	/**
	 * The index of the first column containing distances to k-means cluster centroids
	 * in the table with SOM neurons (tblSOMneuro in SOMApplInfo)
	 */
	//protected int neurDistColIdx =-1;
	/**
	 * The index of the first column containing k-means cluster labels
	 * in the table with SOM results (tblSOMResult in SOMApplInfo)
	 */
	//protected int objClColIdx=-1;
	/**
	 * Whether to show images and statistics in the cells
	 */
	protected Checkbox cbShowImages = null, cbShowIndexImages = null, cbShowStatistics = null, cbShowDistances = null;
	/**
	 * Allows the user to choose results of k-means clustering of the SOM
	 * prototypes
	 */
	//protected Choice choiceClNum=null;
	/**
	 * Additional windows for detailed viewing of the contents of the cells
	 */
	protected HashMap<Integer, Frame> cellContentWins = null;
	/**
	 * Additional windows for viewing of the contents of the cell clusters
	 */
	protected HashMap<String, Frame> clusterContentWins = null;
	/**
	 * The classifier is needed for broadcasting the colors
	 */
	protected QualitativeClassifier classifier = null;
	/**
	 * Broadcasts class colors
	 */
	protected ClassBroadcastPanel cbp = null;
	/**
	 * Whether to join neighbouring cells depending on the distances between them
	 */
	protected Checkbox cbJoinCells = null;
	/**
	 * Used for setting the distance threshold for uniting neighbouring cells
	 */
	protected Slider slider = null;
	/**
	 * Used in combination with the slider
	 */
	protected TextField tfSliderPos = null;
	/**
	 * Used for choosing the number of neighbours to consider in joining the cells;
	 * either 4 or 8
	 */
	protected Choice chNNei = null;
	/**
	 * The currently used distance threshold for joininh SOM cells
	 */
	protected double distanceThr = Double.NaN;
	/**
	 * The column in the table with cell neurons (tblSOMneuro in SOMApplInfo) in which
	 * the clusters resulting from joining SOM cells are stored
	 */
	protected int jointClustersNeuroColN = -1;
	/**
	 * The column in the table with all objects (tblSOMResult in SOMApplInfo) in which
	 * the clusters resulting from joining SOM cells are stored
	 */
	protected int jointClustersColN = -1;
	/**
	 * Used for switching between the "rectangular" and "polar" color scales or
	 * the use of original colors coming from the SOM tool
	 */
	protected Checkbox cbRectColorScale = null, cbPolarColorScale = null, cbOrigColors = null;
	/**
	 * Uses a projection of the SOM cells to generate similar colours for
	 * similar cells (i.e. with similar feature vectors)
	 */
	protected Checkbox cbColorsBySimilarity = null;
	/**
	 * Used to run the projection algorithm for some more iterations
	 */
	protected Button bReProject = null, bRefineProjection = null;
	/**
	 * The layer containing the map background image; may be used for creating
	 * "index" maps
	 */
	public DGeoLayer bkgLayer = null;

	public SOMMatrixView(SOMApplInfo somInfo, ESDACore core) {
		this.somInfo = somInfo;
		this.core = core;
		if (somInfo == null || somInfo.somRes == null || somInfo.somRes.cellInfos == null)
			return;

		setName("SOM matrix view " + (++instanceN));

		SOMResult sr = somInfo.somRes;
		cellViews = new SOMCellView[sr.xdim][sr.ydim];
		Panel mainP = new Panel(new GridLayout(sr.ydim, sr.xdim));
		SplitLayout spl = new SplitLayout(this, SplitLayout.VERT);
		setLayout(spl);
		spl.addComponent(mainP, 0.9f);
		Panel controlP = new Panel(new ColumnLayout());
		spl.addComponent(controlP, 0.1f);

		classifier = new QualitativeClassifier(somInfo.tblSOMResult, somInfo.tblSOMResult.getAttributeId(somInfo.colIdxSOMCells));
		classifier.setup();
		cbp = new ClassBroadcastPanel();
		cbp.construct(classifier, core.getSupervisor());
		controlP.add(cbp);
		controlP.add(new Line(false));

		int cellIdx = 0;
		int nNonEmpty = 0;
		for (int y = 0; y < sr.ydim; y++) {
			for (int x = 0; x < sr.xdim; x++) {
				SOMCellInfo sci = sr.cellInfos[x][y];
				cellViews[x][y] = new SOMCellView();
				cellViews[x][y].setOwner(this);
				mainP.add(cellViews[x][y]);
				cellViews[x][y].cellX = x;
				cellViews[x][y].cellY = y;
				cellViews[x][y].cellIdx = cellIdx;
				cellViews[x][y].nObj = sci.nObj;
				cellViews[x][y].maxNObjPerCell = sr.maxObjectsPerCell;
				cellViews[x][y].origColor = sci.cellColor;
				if (cellViews[x][y].origColor == null) {
					cellViews[x][y].origColor = ColorScale2D.getColor(x, y, 0, sr.xdim - 1, 0, sr.ydim - 1);
				}
				cellViews[x][y].cellColor = cellViews[x][y].origColor;
				cellViews[x][y].indivColor = cellViews[x][y].cellColor;
				//if (sci.nObj>0)
				cellViews[x][y].setBackground(cellViews[x][y].cellColor);
				cellViews[x][y].distances = sci.distances;
				cellViews[x][y].maxDistance = sr.maxDistBetweenNeighbours;
				cellViews[x][y].maxDistToPrototype = sci.maxDistToPrototype;
				cellViews[x][y].aveDistToPrototype = sci.aveDistToPrototype;
				if (somInfo.images != null && sci.protoId >= 0) {
					cellViews[x][y].image = somInfo.images.get(sci.protoId);
				}
				++cellIdx;
				if (sci.nObj > 0) {
					++nNonEmpty;
				}
			}
		}
		if (somInfo.images != null) {
			cbShowImages = new Checkbox("show images", true);
			cbShowImages.addItemListener(this);
			controlP.add(cbShowImages);
		}
		cbShowIndexImages = new Checkbox("show index images", false);
		cbShowIndexImages.addItemListener(this);
		controlP.add(cbShowIndexImages);
		cbShowStatistics = new Checkbox("show statistics", true);
		cbShowStatistics.addItemListener(this);
		controlP.add(cbShowStatistics);
		cbShowDistances = new Checkbox("show distances", true);
		cbShowDistances.addItemListener(this);
		controlP.add(cbShowDistances);
		controlP.add(new Line(false));
		CheckboxGroup cbg = new CheckboxGroup();
		controlP.add(new Label("Color scale:"));
		cbOrigColors = new Checkbox("original", true, cbg);
		controlP.add(cbOrigColors);
		cbOrigColors.addItemListener(this);
		cbPolarColorScale = new Checkbox("polar", false, cbg);
		controlP.add(cbPolarColorScale);
		cbPolarColorScale.addItemListener(this);
		cbRectColorScale = new Checkbox("rectangular", false, cbg);
		controlP.add(cbRectColorScale);
		cbRectColorScale.addItemListener(this);
		cbColorsBySimilarity = new Checkbox("reflect similarity", false);
		controlP.add(cbColorsBySimilarity);
		cbColorsBySimilarity.addItemListener(this);
		cbColorsBySimilarity.setEnabled(false);
		bRefineProjection = new Button("Refine projection");
		bRefineProjection.setActionCommand("refine_projection");
		bRefineProjection.addActionListener(this);
		bRefineProjection.setEnabled(false);
		Panel p = new Panel(new FlowLayout(FlowLayout.CENTER));
		p.add(bRefineProjection);
		controlP.add(p);
		bReProject = new Button("Re-run projection");
		bReProject.setActionCommand("reproject");
		bReProject.addActionListener(this);
		bReProject.setEnabled(false);
		p = new Panel(new FlowLayout(FlowLayout.CENTER));
		p.add(bReProject);
		controlP.add(p);
		controlP.add(new Line(false));

/*
    if (somInfo.tblSOMneuro!=null) {
      Vector attrIds=new Vector(somInfo.tblSOMneuro.getAttrCount());
      for (int i=0; i<somInfo.tblSOMneuro.getAttrCount(); i++) {
        String aId=somInfo.tblSOMneuro.getAttributeId(i);
        if (!aId.equalsIgnoreCase("idx") &&
            !aId.equalsIgnoreCase("x") &&
            !aId.equalsIgnoreCase("y"))
          attrIds.addElement(aId);
      }
      minNClusters=nNonEmpty/10;
      if (minNClusters<2) minNClusters=2;
      maxNClusters=nNonEmpty/3;
      int resCN[]=SOMPostProcessor.applyKMeans(core,somInfo.tblSOMneuro,attrIds,minNClusters,maxNClusters);
      if (resCN!=null) {
        neurClColIdx =resCN[0];
        neurDistColIdx =resCN[1];
        maxNClusters=neurDistColIdx -neurClColIdx +1;
        objClColIdx=SOMPostProcessor.assignClustersOfPrototypesToAllObjects(somInfo,neurClColIdx,neurDistColIdx-1);
        controlP.add(new Label("k-means clusters:"));
        choiceClNum=new Choice();
        controlP.add(choiceClNum);
        choiceClNum.addItemListener(this);
        choiceClNum.addItem("no");
        for (int i=minNClusters; i<=maxNClusters; i++)
          choiceClNum.addItem("k="+i);
        controlP.add(new Line(false));
      }
      else
        maxNClusters=0;
    }
*/
		double minmax[] = somInfo.getMinMaxDistanceBtwNbs();
		if (minmax != null && somInfo.tblSOMneuro != null) {
			cbJoinCells = new Checkbox("join cells", false);
			cbJoinCells.addItemListener(this);
			controlP.add(cbJoinCells);
			controlP.add(new Label("distance threshold:"));
			tfSliderPos = new TextField(StringUtil.doubleToStr(minmax[0], minmax[0], minmax[1]), 10);
			controlP.add(tfSliderPos);
			cbJoinCells.addItemListener(this);
			slider = new Slider(this, minmax[0], minmax[1], minmax[0]);
			slider.setTextField(tfSliderPos);
			slider.setNAD(true);
			controlP.add(slider);
			slider.setEnabled(false);
			chNNei = new Choice();
			chNNei.addItem("4");
			chNNei.addItem("8");
			chNNei.addItemListener(this);
			chNNei.setEnabled(false);
			p = new Panel(new RowLayout(3, 0));
			p.add(new Label("consider"));
			p.add(chNNei);
			p.add(new Label("neighbours"));
			controlP.add(p);
			controlP.add(new Line(false));
		}
	}

	/**
	 * Sets the layer containing the map background image, which may be used for creating
	 * "index" maps
	 */
	public void setMapBkgLayer(DGeoLayer bkgLayer) {
		this.bkgLayer = bkgLayer;
	}

	/**
	 * Assigns colors to the cells according to the current settings
	 */
	protected void colorCells() {
		SOMResult sr = somInfo.somRes;
		float minX = Float.NaN, maxX = minX, minY = minX, maxY = minX;
		boolean polar = cbPolarColorScale.getState(), sim = cbColorsBySimilarity != null && cbColorsBySimilarity.getState();
		if (sim && somInfo.projection != null) {
			for (int x = 0; x < sr.xdim; x++) {
				for (int y = 0; y < sr.ydim; y++) {
					RealPoint p = somInfo.projection[x][y];
					if (Float.isNaN(minX) || minX > p.x) {
						minX = p.x;
					}
					if (Float.isNaN(maxX) || maxX < p.x) {
						maxX = p.x;
					}
					if (Float.isNaN(minY) || minY > p.y) {
						minY = p.y;
					}
					if (Float.isNaN(maxY) || maxY < p.y) {
						maxY = p.y;
					}
				}
			}
		} else {
			minX = 0;
			maxX = sr.xdim - 1;
			minY = 0;
			maxY = sr.ydim - 1;
		}
		Vector<Color> colors = new Vector(sr.xdim * sr.ydim, 1);
		Vector<String> values = new Vector(sr.xdim * sr.ydim, 1);
		for (int y = 0; y < sr.ydim; y++) {
			for (int x = 0; x < sr.xdim; x++) {
				float xx = x, yy = y;
				if (sim) {
					RealPoint p = somInfo.projection[x][y];
					xx = p.x;
					yy = p.y;
				}
				Color c = (polar) ? ColorScale2D.getColorCircular(xx, yy, minX, maxX, minY, maxY) : ColorScale2D.getColor(xx, yy, minX, maxX, minY, maxY);
				cellViews[x][y].indivColor = c;
				cellViews[x][y].cellColor = (cbOrigColors.getState()) ? cellViews[x][y].origColor : cellViews[x][y].indivColor;
				//if (cellViews[x][y].nObj>0) {
				cellViews[x][y].setBackground(cellViews[x][y].cellColor);
				cellViews[x][y].repaint();
				//}
				if (cellViews[x][y].nObj > 0) {
					//cellViews[x][y].setBackground(cellViews[x][y].cellColor);
					//cellViews[x][y].repaint();
					int oIdx = sr.cellInfos[x][y].oIds.get(0).intValue() - 1;
					String val = somInfo.tblSOMResult.getAttrValueAsString(somInfo.colIdxSOMCells, oIdx);
					values.addElement(val);
					colors.addElement(cellViews[x][y].cellColor);
				}
			}
		}
		Attribute at = somInfo.tblSOMResult.getAttribute(somInfo.colIdxSOMCells);
		Color ac[] = new Color[colors.size()];
		ac = colors.toArray(ac);
		String av[] = new String[values.size()];
		av = values.toArray(av);
		at.setValueListAndColors(av, ac);
		Vector attr = new Vector(1, 1);
		attr.addElement(at.getIdentifier());
		somInfo.tblSOMResult.notifyPropertyChange("value_colors", null, attr);
	}

	/**
	 * Assigns colors to clusters of SOM neurons
	 */
/*
  protected void assignColorsToClusters () {
    if (somInfo.tblSOMneuro==null || neurClColIdx <0 || neurDistColIdx <0)
      return;
    int xCN=somInfo.tblSOMneuro.getAttrIndex("x"), yCN=somInfo.tblSOMneuro.getAttrIndex("y");
    //mixing of colors of the cluster members
    FloatArray reds =new FloatArray(50,10),greens =new FloatArray(50,10),blues=new FloatArray(50,10);
    for (int colIdx=neurClColIdx; colIdx<neurDistColIdx; colIdx++) {
      Attribute clAt=somInfo.tblSOMneuro.getAttribute(colIdx);
      String values[]=clAt.getValueList();
      Color colors[]=new Color[values.length];
      for (int clusterIdx =0; clusterIdx <values.length; clusterIdx++) {
        reds.removeAllElements(); greens.removeAllElements(); blues.removeAllElements();
        for (int rn=0; rn<somInfo.tblSOMneuro.getDataItemCount(); rn++) {
          DataRecord rec=somInfo.tblSOMneuro.getDataRecord(rn);
          if (values[clusterIdx].equals(rec.getAttrValueAsString(colIdx))) {
            int x=(int)rec.getNumericAttrValue(xCN), y=(int)rec.getNumericAttrValue(yCN);
            Color c=(cbOrigColors.getState())?cellViews[x][y].origColor:cellViews[x][y].indivColor;
            reds.addElement(c.getRed());
            greens.addElement(c.getGreen());
            blues.addElement(c.getBlue());
          }
        }
        int r=Math.round(NumValManager.getMean(reds)),
            g=Math.round(NumValManager.getMean(greens)),
            b=Math.round(NumValManager.getMean(blues));
        colors[clusterIdx]=new Color(r,g,b);
      }
      clAt.setValueListAndColors(values,colors);
      Vector attr=new Vector(1,1);
      attr.addElement(clAt.getIdentifier());
      somInfo.tblSOMneuro.notifyPropertyChange("value_colors",null,attr);
      int aIdx=colIdx-neurClColIdx+objClColIdx;
      clAt=somInfo.tblSOMResult.getAttribute(aIdx);
      clAt.setValueListAndColors(values,colors);
      attr.removeAllElements();
      attr.addElement(clAt.getIdentifier());
      somInfo.tblSOMResult.notifyPropertyChange("value_colors",null,attr);
    }
  }
*/

	@Override
	public Dimension getPreferredSize() {
		Dimension sz = super.getPreferredSize(), ssz = getToolkit().getScreenSize();
		int w = sz.width, h = sz.height;
		if (w > 0.75 * ssz.width) {
			w = Math.round(0.75f * ssz.width);
		}
		if (h > 0.75 * ssz.height) {
			h = Math.round(0.75f * ssz.height);
		}
		return new Dimension(w, h);
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		if (e.getSource().equals(cbShowImages)) {
			SOMResult sr = somInfo.somRes;
			boolean show = cbShowImages.getState();
			for (int y = 0; y < sr.ydim; y++) {
				for (int x = 0; x < sr.xdim; x++) {
					cellViews[x][y].drawImage = show;
					cellViews[x][y].repaint();
				}
			}
		} else if (e.getSource().equals(cbShowIndexImages)) {
			SOMResult sr = somInfo.somRes;
			boolean show = cbShowIndexImages.getState();
			if (show) {
				boolean imagesExist = false;
				for (int y = 0; y < sr.ydim && !imagesExist; y++) {
					for (int x = 0; x < sr.xdim && !imagesExist; x++) {
						imagesExist = cellViews[x][y].indexImage != null;
					}
				}
				if (!imagesExist) //generate the images
					if (somInfo.applySOMtoParam && somInfo.paramSOM != null) {
						ParamValIndexView indexP = new ParamValIndexView(somInfo.paramSOM);
						int nCols = Dialogs.askForIntValue(CManager.getAnyFrame(this), "Desired number of columns in the index display?", indexP.getNColumns(), 1, somInfo.paramSOM.getValueCount(), null, "N of columns?", false);
						indexP.setNColumns(nCols);
						for (int y = 0; y < sr.ydim; y++) {
							for (int x = 0; x < sr.xdim; x++)
								if (cellViews[x][y].nObj > 0) {
									SOMCellInfo sci = sr.cellInfos[x][y];
									if (sci.oIds == null || sci.oIds.isEmpty()) {
										continue;
									}
									indexP.deselectAllValues();
									for (int i = 0; i < sci.oIds.size(); i++) {
										indexP.addSelectedValueIndex(sci.oIds.get(i) - 1);
									}
									cellViews[x][y].indexImage = indexP.getImage();
								}
						}
					} else if (!somInfo.applySOMtoParam && somInfo.tblSOMlayer != null) {
						IndexMapView imap = new IndexMapView(bkgLayer, somInfo.tblSOMlayer);
						for (int y = 0; y < sr.ydim; y++) {
							for (int x = 0; x < sr.xdim; x++)
								if (cellViews[x][y].nObj > 0) {
									SOMCellInfo sci = sr.cellInfos[x][y];
									if (sci.oIds == null || sci.oIds.isEmpty()) {
										continue;
									}
									imap.deselectAllObjects();
									for (int i = 0; i < sci.oIds.size(); i++) {
										int idx = sci.oIds.get(i) - 1;
										imap.addSelectedObject(somInfo.tblSOM.getDataItemId(idx));
									}
									cellViews[x][y].indexImage = imap.getImage();
								}
						}
						imap.destroy();
					}
			}
			for (int y = 0; y < sr.ydim; y++) {
				for (int x = 0; x < sr.xdim; x++) {
					cellViews[x][y].drawIndexImage = show;
					cellViews[x][y].repaint();
				}
			}
		} else if (e.getSource().equals(cbShowStatistics)) {
			SOMResult sr = somInfo.somRes;
			boolean show = cbShowStatistics.getState();
			for (int y = 0; y < sr.ydim; y++) {
				for (int x = 0; x < sr.xdim; x++) {
					cellViews[x][y].showStatistics = show;
					cellViews[x][y].repaint();
				}
			}
		} else if (e.getSource().equals(cbShowDistances)) {
			SOMResult sr = somInfo.somRes;
			boolean show = cbShowDistances.getState();
			for (int y = 0; y < sr.ydim; y++) {
				for (int x = 0; x < sr.xdim; x++) {
					cellViews[x][y].showDistances = show;
					cellViews[x][y].repaint();
				}
			}
		} else
/*
    if (e.getSource().equals(choiceClNum)) {
      closeClusterContentWins();
      if (choiceClNum.getSelectedIndex()==0) {
        restoreOriginalCellColors();
        cbRectColorScale.setEnabled(true);
        cbPolarColorScale.setEnabled(true);
        cbOrigColors.setEnabled(true);
        cbColorsBySimilarity.setEnabled(!cbOrigColors.getState());
        return;
      }
      cbRectColorScale.setEnabled(false);
      cbPolarColorScale.setEnabled(false);
      cbOrigColors.setEnabled(false);
      cbColorsBySimilarity.setEnabled(false);
      //assignColorsToClusters();
      int colIdx=choiceClNum.getSelectedIndex()-1+neurClColIdx;
      Attribute clAt=somInfo.tblSOMneuro.getAttribute(colIdx);
      Color colors[]=clAt.getValueColors();
      int xCN=somInfo.tblSOMneuro.getAttrIndex("x"), yCN=somInfo.tblSOMneuro.getAttrIndex("y");
      for (int rn=0; rn<somInfo.tblSOMneuro.getDataItemCount(); rn++) {
        DataRecord rec=somInfo.tblSOMneuro.getDataRecord(rn);
        int x=(int)rec.getNumericAttrValue(xCN), y=(int)rec.getNumericAttrValue(yCN);
        String clLabel=rec.getAttrValueAsString(colIdx);
        int cIdx=clAt.getValueN(clLabel);
        cellViews[x][y].cellColor=colors[cIdx];
        cellViews[x][y].clusterLabel=clLabel;
        //if (cellViews[x][y].nObj>0) {
          cellViews[x][y].setBackground(cellViews[x][y].cellColor);
          cellViews[x][y].repaint();
        //}
      }
      classifier=new QualitativeClassifier(somInfo.tblSOMResult,
        somInfo.tblSOMResult.getAttributeId(objClColIdx+choiceClNum.getSelectedIndex()-1));
      classifier.setup();
      cbp.replaceClassifier(classifier);
    }
    else
*/
		if (e.getSource().equals(cbJoinCells) || e.getSource().equals(chNNei)) {
			closeClusterContentWins();
			if (cbJoinCells.getState()) {
/*
        if (choiceClNum!=null && choiceClNum.isEnabled()) {
          choiceClNum.select(0); choiceClNum.setEnabled(false);
          if (Double.isNaN(distanceThr))
            restoreOriginalCellColors();
        }
*/
				if (e.getSource().equals(cbJoinCells)) {
					slider.setEnabled(true);
					slider.repaint();
					chNNei.setEnabled(true);
				}
				joinNeighbouringCells(distanceThr, true);
			} else {
				slider.setEnabled(false);
				restoreOriginalCellColors();
/*
        if (choiceClNum!=null)
          choiceClNum.setEnabled(true);
*/
			}
		} else if (e.getSource().equals(cbRectColorScale) || e.getSource().equals(cbPolarColorScale) || e.getSource().equals(cbOrigColors) || e.getSource().equals(cbColorsBySimilarity)) {
			if (cbColorsBySimilarity.getState() && somInfo.projection == null) {
				somInfo.getProjection(1000);
			}
			if (cbOrigColors.getState()) {
				cbColorsBySimilarity.setState(false);
			}
			cbColorsBySimilarity.setEnabled(!cbOrigColors.getState());
			colorCells();
			if (cbJoinCells.getState()) {
				joinNeighbouringCells(distanceThr, true);
			}
		}
		bReProject.setEnabled(cbColorsBySimilarity.getState());
		bRefineProjection.setEnabled(cbColorsBySimilarity.getState());
	}

	protected void restoreOriginalCellColors() {
		SOMResult sr = somInfo.somRes;
		Vector<Color> colors = new Vector(sr.xdim * sr.ydim, 1);
		Vector<String> values = new Vector(sr.xdim * sr.ydim, 1);
		for (int y = 0; y < sr.ydim; y++) {
			for (int x = 0; x < sr.xdim; x++) {
				cellViews[x][y].clusterLabel = null;
				cellViews[x][y].cellColor = (cbOrigColors.getState()) ? cellViews[x][y].origColor : cellViews[x][y].indivColor;
				//if (cellViews[x][y].nObj>0) {
				cellViews[x][y].setBackground(cellViews[x][y].cellColor);
				cellViews[x][y].repaint();
				//}
				if (cellViews[x][y].nObj > 0) {
					//cellViews[x][y].setBackground(cellViews[x][y].cellColor);
					//cellViews[x][y].repaint();
					int oIdx = sr.cellInfos[x][y].oIds.get(0).intValue() - 1;
					String val = somInfo.tblSOMResult.getAttrValueAsString(somInfo.colIdxSOMCells, oIdx);
					values.addElement(val);
					colors.addElement(cellViews[x][y].cellColor);
				}
			}
		}
		Attribute at = somInfo.tblSOMResult.getAttribute(somInfo.colIdxSOMCells);
		Color ac[] = new Color[colors.size()];
		ac = colors.toArray(ac);
		String av[] = new String[values.size()];
		av = values.toArray(av);
		at.setValueListAndColors(av, ac);
		Vector attr = new Vector(1, 1);
		attr.addElement(at.getIdentifier());
		somInfo.tblSOMResult.notifyPropertyChange("value_colors", null, attr);
		classifier = new QualitativeClassifier(somInfo.tblSOMResult, somInfo.tblSOMResult.getAttributeId(somInfo.colIdxSOMCells));
		classifier.setup();
		cbp.replaceClassifier(classifier);
	}

	protected void joinNeighbouringCells(double distanceThr, boolean updateTables) {
		if (Double.isNaN(distanceThr))
			return;
		//System.out.println("join cells; threshold = "+distanceThr);
		int nNei = (chNNei.getSelectedIndex() == 0) ? 4 : 8;
		Vector<Vector> groups = SOMCellsJoiner.joinCells(somInfo.somRes.cellInfos, distanceThr, nNei);
		if (groups == null) {
			core.getUI().showMessage("Failed to group the cells!", true);
			return;
		}
		this.distanceThr = distanceThr;
		if (groups.size() >= somInfo.somRes.xdim * somInfo.somRes.ydim) {
			core.getUI().showMessage("Threshold = " + distanceThr + ": all cells are separate!", false);
		} else if (groups.size() == 1) {
			core.getUI().showMessage("Threshold = " + distanceThr + ": all cells are together!", false);
		} else {
			core.getUI().showMessage("Threshold = " + distanceThr + ": obtained " + groups.size() + " groups", false);
		}

		SOMResult sr = somInfo.somRes;

		String classLabels[] = new String[groups.size()];
		Color classColors[] = new Color[groups.size()];
		FloatArray reds = new FloatArray(groups.elementAt(0).size(), 10), greens = new FloatArray(groups.elementAt(0).size(), 10), blues = new FloatArray(groups.elementAt(0).size(), 10);
		for (int i = 0; i < groups.size(); i++) {
			classLabels[i] = "cluster " + (i + 1);
			Vector<Point> group = groups.elementAt(i);
			reds.removeAllElements();
			greens.removeAllElements();
			blues.removeAllElements();
			for (int j = 0; j < group.size(); j++) {
				int x = group.elementAt(j).x, y = group.elementAt(j).y;
				Color c = (cbOrigColors.getState()) ? cellViews[x][y].origColor : cellViews[x][y].indivColor;
				reds.addElement(c.getRed());
				greens.addElement(c.getGreen());
				blues.addElement(c.getBlue());
			}
			int r = Math.round(NumValManager.getMean(reds)), g = Math.round(NumValManager.getMean(greens)), b = Math.round(NumValManager.getMean(blues));
			classColors[i] = new Color(r, g, b);
		}
		int clNs[][] = new int[sr.xdim][sr.ydim];
		for (int x = 0; x < sr.xdim; x++) {
			for (int y = 0; y < sr.ydim; y++) {
				clNs[x][y] = -1;
			}
		}
		for (int i = 0; i < groups.size(); i++) {
			Vector<Point> group = groups.elementAt(i);
			for (int j = 0; j < group.size(); j++) {
				int x = group.elementAt(j).x, y = group.elementAt(j).y;
				clNs[x][y] = i;
				cellViews[x][y].clusterLabel = classLabels[i];
				cellViews[x][y].cellColor = classColors[i];
				//if (cellViews[x][y].nObj>0) {
				cellViews[x][y].setBackground(cellViews[x][y].cellColor);
				cellViews[x][y].repaint();
				//}
			}
		}
		if (!updateTables)
			return;
		closeClusterContentWins();
		boolean newAttr = jointClustersNeuroColN < 0;
		DataTable tbl1 = somInfo.tblSOMneuro, tbl2 = somInfo.tblSOMResult;
		if (newAttr) {
			//create a new column in the table somInfo.tblSOMneuro
			Attribute at = new Attribute("at" + (tbl1.getAttrCount() + 1), AttributeTypes.character);
			String aName = "Clustering of " + somInfo.tblSOMResult.getAttributeName(somInfo.colIdxSOMCells);
			at.setName(aName);
			jointClustersNeuroColN = tbl1.getAttrCount();
			tbl1.addAttribute(at);
			at = new Attribute("at" + (tbl2.getAttrCount() + 1), AttributeTypes.character);
			at.setName(aName);
			jointClustersColN = tbl2.getAttrCount();
			tbl2.addAttribute(at);
		}
		int xCN = tbl1.getAttrIndex("x"), yCN = tbl1.getAttrIndex("y");
		for (int rn = 0; rn < tbl1.getDataItemCount(); rn++) {
			DataRecord pRec = tbl1.getDataRecord(rn);
			int x = (int) pRec.getNumericAttrValue(xCN), y = (int) pRec.getNumericAttrValue(yCN);
			String cLabel = (clNs[x][y] >= 0) ? classLabels[clNs[x][y]] : null;
			pRec.setAttrValue(cLabel, jointClustersNeuroColN);
			SOMCellInfo sci = sr.cellInfos[x][y];
			if (sci.nObj < 1) {
				continue;
			}
			for (int i = 0; i < sci.oIds.size(); i++) {
				int id = sci.oIds.get(i).intValue();
				if (id > 0) {
					DataRecord rec = somInfo.tblSOMResult.getDataRecord(id - 1);
					if (rec != null) {
						rec.setAttrValue(cLabel, jointClustersColN);
					}
				}
			}
		}
		tbl1.getAttribute(jointClustersNeuroColN).setValueListAndColors(classLabels, classColors);
		tbl2.getAttribute(jointClustersColN).setValueListAndColors(classLabels, classColors);

		Vector<String> attr = new Vector<String>(1, 1);
		attr.addElement(tbl1.getAttributeId(jointClustersNeuroColN));
		tbl1.notifyPropertyChange((newAttr) ? "new_attributes" : "values", null, attr);
		attr.removeAllElements();
		attr.addElement(tbl2.getAttributeId(jointClustersColN));
		tbl2.notifyPropertyChange((newAttr) ? "new_attributes" : "values", null, attr);
		if (classifier.getAttrColumnN() != jointClustersColN) {
			classifier = new QualitativeClassifier(tbl2, tbl2.getAttributeId(jointClustersColN));
			classifier.setup();
			cbp.replaceClassifier(classifier);
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource().equals(slider)) {
			double slPos = Double.NaN;
			try {
				slPos = Double.parseDouble(tfSliderPos.getText());
			} catch (Exception ex) {
			}
			if (Double.isNaN(slPos)) {
				slPos = slider.getValue();
			}
			if (slPos == distanceThr && slider.getIsDragging())
				return;
			joinNeighbouringCells(slPos, !slider.getIsDragging());
		} else if (e.getActionCommand().equals("reproject") || e.getActionCommand().equals("refine_projection")) {
			if (e.getActionCommand().equals("reproject")) {
				somInfo.getProjection(1000);
			} else {
				somInfo.refineProjection(1000);
			}
			if (cbColorsBySimilarity.getState() && somInfo.projection != null) {
				colorCells();
			}
		} else if ((e.getSource() instanceof SOMCellView) && e.getActionCommand().equals("CellViewClicked")) {
			SOMCellView scw = (SOMCellView) e.getSource();
			if (scw.cellX < 0 || scw.cellY < 0 || scw.nObj < 1)
				return;
			if (scw.clusterLabel == null) {
				if (cellContentWins != null) {
					Frame fr = cellContentWins.get(scw.cellIdx);
					if (fr != null) {
						fr.toFront();
						return;
					}
				}
			} else {
				if (clusterContentWins != null) {
					Frame fr = clusterContentWins.get(scw.clusterLabel);
					if (fr != null) {
						fr.toFront();
						return;
					}
				}
			}
			SOMResult sr = somInfo.somRes;
			SOMCellInfo sci = sr.cellInfos[scw.cellX][scw.cellY];
			if (sci.oIds == null || sci.oIds.isEmpty())
				return;
			IntArray oIdxs = new IntArray(sci.oIds.size(), 1);
			oIdxs.addElement(sci.oIds.get(0));
			for (int i = 1; i < sci.oIds.size(); i++) {
				int idx = sci.oIds.get(i);
				int insIdx = -1;
				for (int j = 0; j < oIdxs.size() && insIdx < 0; j++)
					if (idx < oIdxs.elementAt(j)) {
						insIdx = j;
					}
				if (insIdx >= 0) {
					oIdxs.insertElementAt(idx, insIdx);
				} else {
					oIdxs.addElement(idx);
				}
			}
			if (scw.clusterLabel != null) {
				for (int y = 0; y < sr.ydim; y++) {
					for (int x = 0; x < sr.xdim; x++)
						if (!scw.equals(cellViews[x][y]) && scw.clusterLabel.equals(cellViews[x][y].clusterLabel)) {
							sci = sr.cellInfos[x][y];
							if (sci.oIds == null || sci.oIds.isEmpty()) {
								continue;
							}
							for (int i = 0; i < sci.oIds.size(); i++) {
								int idx = sci.oIds.get(i);
								int insIdx = -1;
								for (int j = 0; j < oIdxs.size() && insIdx < 0; j++)
									if (idx < oIdxs.elementAt(j)) {
										insIdx = j;
									}
								if (insIdx >= 0) {
									oIdxs.insertElementAt(idx, insIdx);
								} else {
									oIdxs.addElement(idx);
								}
							}
						}
				}
			}
			DataTable tbl = (somInfo.tblSOMResult != null) ? somInfo.tblSOMResult : somInfo.tblSOM;
			Panel mainP = new Panel(new FlexibleGridLayout(2, 2));
			mainP.setBackground(scw.cellColor);
			Dimension size = null;
			for (int i = 0; i < oIdxs.size(); i++) {
				String oName = tbl.getDataItemName(oIdxs.elementAt(i) - 1);
				Label l = new Label(oName);
				l.setBackground(Color.white);
				Dimension ls = l.getPreferredSize();
				if (ls.width < 10 || ls.height < 10) {
					Graphics g = scw.getGraphics();
					if (oName != null) {
						ls.width = g.getFontMetrics().stringWidth(oName) + 20;
					}
					ls.height = g.getFontMetrics().getHeight() + 10;
				}
				BufferedImage img = null;
				if (somInfo.images != null) {
					img = somInfo.images.get(oIdxs.elementAt(i));
				}
				if (img == null) {
					mainP.add(l);
				} else {
					Panel p = new Panel(new BorderLayout());
					p.add(l, BorderLayout.NORTH);
					ImageCanvas ic = new ImageCanvas(img);
					p.add(ic, BorderLayout.CENTER);
					mainP.add(p);
					ls.height += img.getHeight();
					if (img.getWidth() > ls.width) {
						ls.width = img.getWidth();
					}
				}
				if (size == null) {
					size = ls;
				} else if (ls.width > size.width) {
					size.width = ls.width;
				}
			}
			if (size == null) {
				size = scw.getSize();
			} else {
				size.width += 5;
				size.height += 5;
			}
			int w = size.width, h = size.height;
			int nCols = scw.nObj, nRows = 1;
			while (nCols > 5) {
				++nRows;
				nCols = scw.nObj / nRows;
			}
			w *= nCols;
			h *= nRows;
			Panel leftP = null;
			float leftSz = 0.2f;
			if (somInfo.applySOMtoParam && somInfo.paramSOM != null) {
				ParamValIndexView indexP = new ParamValIndexView(somInfo.paramSOM);
				for (int i = 0; i < oIdxs.size(); i++) {
					indexP.addSelectedValueIndex(oIdxs.elementAt(i) - 1);
				}
				leftP = indexP;
			} else if (!somInfo.applySOMtoParam && somInfo.tblSOMlayer != null) {
				IndexMapView imap = new IndexMapView(bkgLayer, somInfo.tblSOMlayer);
				for (int i = 0; i < oIdxs.size(); i++) {
					imap.addSelectedObject(tbl.getDataItemId(oIdxs.elementAt(i) - 1));
				}
				leftP = imap;
				leftSz = 0.5f;
			}
			String title = scw.clusterLabel;
			if (title == null) {
				title = "Cell [" + scw.cellX + "," + scw.cellY + "]";
			}
			ScrollPane scp = new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED);
			scp.add(mainP);
			Component frContent = scp;
			Frame fr = new Frame(title);
			Insets ins = fr.getInsets();
			w += 10 + scp.getVScrollbarWidth() + ins.left + ins.right;
			h += 60 + scp.getHScrollbarHeight() + ins.top + ins.bottom;
			if (leftP != null) {
				Panel p = new Panel();
				frContent = p;
				SplitLayout spl = new SplitLayout(p, SplitLayout.VERT);
				p.setLayout(spl);
				spl.addComponent(leftP, leftSz);
				spl.addComponent(scp, 1 - leftSz);
				Dimension d = leftP.getPreferredSize();
				w += d.width + 10;
			}
			fr.add(frContent);
			Dimension ssz = getToolkit().getScreenSize();
			if (w > 0.5 * ssz.width) {
				w = Math.round(0.5f * ssz.width);
			}
			if (h > 0.5 * ssz.height) {
				h = Math.round(0.5f * ssz.height);
			}
			fr.setSize(w, h);
			Point loc = scw.getLocationOnScreen();
			size = scw.getSize();
			fr.setLocation(loc.x, loc.y + size.height);
			fr.setVisible(true);
			fr.addWindowListener(this);
			if (scw.clusterLabel == null) {
				if (cellContentWins == null) {
					cellContentWins = new HashMap<Integer, Frame>();
				}
				cellContentWins.put(scw.cellIdx, fr);
			} else {
				if (clusterContentWins == null) {
					clusterContentWins = new HashMap<String, Frame>();
				}
				clusterContentWins.put(scw.clusterLabel, fr);
			}
		}
	}

	protected void closeClusterContentWins() {
		if (clusterContentWins != null && !clusterContentWins.isEmpty()) {
			for (Object element : clusterContentWins.keySet()) {
				Frame fr = clusterContentWins.get(element);
				fr.dispose();
			}
			clusterContentWins.clear();
		}
	}

	protected boolean destroyed = false;

	@Override
	public boolean isDestroyed() {
		return destroyed;
	}

	@Override
	public void destroy() {
		closeClusterContentWins();
		if (cellContentWins != null && !cellContentWins.isEmpty()) {
			for (Object element : cellContentWins.keySet()) {
				Frame fr = cellContentWins.get(element);
				fr.dispose();
			}
			cellContentWins.clear();
		}
		destroyed = true;
	}

	/**
	 * Invoked when the user attempts to close the window
	 * from the window's system menu.
	 */
	@Override
	public void windowClosing(WindowEvent e) {
		if (e.getSource() instanceof Frame) {
			Frame frClosing = (Frame) e.getSource();
			if (cellContentWins != null && !cellContentWins.isEmpty()) {
				for (Object element : cellContentWins.keySet()) {
					Integer key = (Integer) element;
					Frame fr = cellContentWins.get(key);
					if (fr.equals(frClosing)) {
						fr.dispose();
						cellContentWins.remove(key);
						//System.out.println("Closed the window for cell "+key);
						return;
					}
				}
			}
			if (clusterContentWins != null && !clusterContentWins.isEmpty()) {
				for (Object element : clusterContentWins.keySet()) {
					String key = (String) element;
					Frame fr = clusterContentWins.get(key);
					if (fr.equals(frClosing)) {
						fr.dispose();
						clusterContentWins.remove(key);
						//System.out.println("Closed the window for cluster "+key);
						return;
					}
				}
			}
		}
	}

	@Override
	public void windowOpened(WindowEvent e) {
	}

	@Override
	public void windowClosed(WindowEvent e) {
	}

	@Override
	public void windowIconified(WindowEvent e) {
	}

	@Override
	public void windowDeiconified(WindowEvent e) {
	}

	@Override
	public void windowActivated(WindowEvent e) {
	}

	@Override
	public void windowDeactivated(WindowEvent e) {
	}
}
