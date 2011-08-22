package spade.analysis.plot.correlation;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.util.Vector;

import spade.analysis.datamanage.DataStorage;
import spade.lib.basicwin.Drawable;

/**
 * This class is a drawable object that is able to draw a<br>
 * circle diagram and a 3d diagram for visualisation of values between<br>
 * [-1; 1]<br>
 * The Class gets Vectors with values which are stored in an<br>
 * instance of propraktikum03.calc.DataStorage. The methods addRow and<br>
 * addColumn are linked to the DataStorageObject.<br>
 * <br>
 * To call the class:<br>
 * <br>
 * A change of the preferences is possible at any time (e.g. maxFontSize; resize;... see fields)<br>
 * <br>
 * First Vectors with double-values have to be added, which will be displayed. These values have to be within [-1.0; 1.0].<br>
 * Thereafter descriptions for the values can be added, also a Vector containing Strings. The order of value descriptions within the Vector has to be identical to that of the value Vector.<br>
 * <br>
 * Next, the MultiCorrelation has to be added to a drawable Canvas, and this Canvas to the MultiCorrelationGraphics.
 * <br>
 * The draw(Graphics) method results in the drawing of the graph, as circle diagram if mode3d is false, or as 3d diagram if mode3d is true.<br>
 * <br>
 * <br>
 * The graph is used to draw 3d charts for MultiCorrelation but is also able to draw only one row or one column.<br>
 * <br>
 * The minimum number of painted objects is one.<br>
 * <br>
 * @author Christian Gro�
 * @version 1.0
 */
public class MultiCorrelationGraphics implements Drawable {

	/*
	 Added by G.A. to allow compilation of Correlation3dComponent
	*/
	public void setMode(boolean b) {
	}

	/** Is required by drawable
	 */
	protected Canvas canvas = null;
	/** These bounds define the drawing range for the graphics object.
	 */
	protected Rectangle bounds = null;
	/** Is needed by the interface drawable. to check if the object<br>
	 *object has been destroyed, Other classes call isDestroyed() which<br>
	 *returns destroyed.
	 */
	protected boolean destroyed = false;

	/** The diagrams will be drawn and described with the data,
	 *which is stored in an instance of DataStorage<br>
	 * <br>
	 * This class manages all data. All methods and variables
	 *are public, so that MultiCorrelationGraphics has maximum access.<br>
	 */
	DataStorage table = new DataStorage();

	/** If mode3d is true, the 3d diagrmm will be drawn, else the
	 *circle diagram will be drawn.
	 */
	private boolean mode3d = true;

	// Initialisation of the basic values

	/** Defines the actually used font size.<br>
	 * <br>
	 * The font size is variable, if resize is true!<br>
	 */
	private int fontSize = 8; // 3d/2d: size of fonts in the diagram
	/** Defines the maximum font size in the diagrams. If resize is
	 *true, the graph will use the maximum space, so that the font size
	 *will be large. For prevention the method setSize can be called.
	 */
	private int maxFontSize = 0; // 2d
	/** RecSize defines the actually used space for each circle in the
	 *circle diagram.
	 */
	private int recSize = 50; // 3d/2d: size of the rectangles
	/** Defines the minimum circle size. if the space for each
	 *circle is less then minCircleSize, minCircleSize will be
	 *set as the new recSize.
	 */
	private int minCircleSize = 0; // 2d only: minimum circle size
	/** Temporarily needed variable for circleSizeCalculations.
	 */
	private int tempCircleSize = 0; // 2d only
	/** This Vector contains the descriptions for the graph.
	 */
	private Vector descriptor = new Vector();

	/** If resize is true the component resize will cause the
	 *relations within the diagrams to adapt.
	 */
	private boolean resize = true;

	/** Defines the BorderColor/GridColor in the 2d diagramm
	 */
	public Color borderColor = new Color(0x000000);
	/** defines the font color
	 */
	public Color fontColor = new Color(0x000000);
	/** defines the color of the cellBackgrounds in the 2d diagram
	 */
	public Color cellColor = new Color(0xffffff);

	/** defines the clear space between top and the diagram
	 */
	public int topMargin = 20;
	/** defines the clear space between the left border and the diagram
	 */
	public int leftMargin = 50;

	/** This value is needed for the 3d diagram.
	 * The bars in this row will be drawn in filled mode. All
	 *other bars will be drawn as grids.
	 */
	public int rowSelected = 0;

	/** Switches the y-axis so that positive values will be drawn
	 *downwards and negative values will be drawn upwards.
	 */
	private boolean switchPosNeg = false;

	/* Dwitches the signed/unsigned mode in the 3d mode.<br>
	 *<br>
	         /** This variable is needed to control the signed and unsigned
	  *mode in 3d mode.<br>
	  * <br>
	  * If true, the displayed values are drawn in both directions,
	  *negative and positive. If signed is false (default), all
	  *values are drawn upwards.<br>
	  */
	private boolean signed = false;

	/** Defines the used grid colour in the 3d mode. The grid is the
	 *parrallelogram, where the drawing of the bar starts.
	 */
	public Color gridColor = new Color(0xeeeeee);
	/** Defines the background color of both modes.
	 */
	public Color backgroundColor = new Color(0x999999);
	public Color posGitterColor = new Color(0x88dddd);
	public Color negGitterColor = new Color(0xdddd88);
	public int barColor = 0x88aaaa;

	/** No constructor is needed.
	 */
	public MultiCorrelationGraphics() {
	}

	/** Calculates the approximated width of the 3d diagram
	     *based on the amount of data in DataStorage and the active RectangleSizes.<br>
	 * <br>
	 * @return Returns the approximated 3d diagram width in pixels
	 */
	private int calc3dWidth() {
		return this.recSize * (table.columnCount + 2);
	}

	/** Calculates the approximated height of the 3d diagram
	 *based on the active values in DataStorage and RectangleSize.<br>
	 * <br>
	 * @return Integer value which approximates the height as a number of pixels
	 */
	private int calc3dHeight() {
		return this.recSize * (table.rowCount + 3) * 2 / 3;
	}

	/** Calculates the width of the 2d circle diagram in pixels
	      /** Calculates the width of the 2d circle diagram in pixels
	  *based on the data in DataStorage and the RectangleSize.<br>
	  *
	  * @return Width as number of pixels, for the 2d diagram
	  */
	private int calcWidth() {
		return this.recSize * table.columnCount + this.leftMargin;
	}

	/** Calculates the height of the 2d diagram in pixels based on
	 *the data in DataStorage and the RectangleSize.<br>
	 *
	 * @return Approx. height of the 2d diagram
	 */
	private int calcHeight() {
		return this.recSize * (table.rowCount + 1) + (2 * this.topMargin);
	}

	/** Sets the new RectagleSize (the drawn grid) for the
	 *2d circle diagram. This value is also used for adjusting the<br>
	      /** Sets the new RectagleSize (the drawn grid) for the
	  *2d circle diagram. This value is also used for adjusting the<br>
	  *maximum circle size.
	  * <br>
	  * If the canvas is resized, this method will be called, for
	  *calculating the new Rectanglesize<br>
	  *<br>
	  *<br>
	  *The method is called by setBounds(...), if resize - which is
	  *defined by setAdapting(...) - is true.
	  *
	  * @param maxHeight maximum given space for height
	  * @param maxWidth maximum given space for width
	  */
	private void setNewRecSize(double maxHeight, double maxWidth) {

		if (maxHeight < maxWidth) {
			this.setRectangleSize((int) Math.round(maxHeight / (table.rowCount + 2)));
		} else {
			this.setRectangleSize((int) Math.round(maxWidth / (table.columnCount + 1)));
		}

	}

	/** The 3d rectangle size is used for
	 *adjusting the bars and the drawn grid to the given space.<br>
	 *<br>
	 * <br>
	 * The method is called by setBounds(...). The method is only called <br>
	 *if resize is true, which has been set in setAdapting(...)<br>
	 *<br>
	 * @param maxHeight maximum space for height
	 * @param maxWidth maximum space for width
	 */
	private void setNew3dRecSize(double maxHeight, double maxWidth) {

		int opt1 = (int) Math.round(maxHeight / (table.rowCount + 2) * 4 / 3);
		int opt2 = (int) Math.round(maxWidth / (table.columnCount + 1));

		setRectangleSize(opt1 > opt2 ? opt2 : opt1);

	}

	// </internals>

	/** This method sets the new size of the grid rectangles of the 3d mode in pixels.
	 *
	 * If resize is false, the new values are set, otherwise not.
	 *
	 * @param size preferred size in pixels
	 */
	public void set3dRectangleSize(int size) {

		if (!resize) {
			this.recSize = size - 2;
			this.topMargin = recSize / 2;
			this.leftMargin = recSize;
		}
	}

	/** Sets the new rectangle and circle size for the 2d diagram.
	 *
	 * @param size preferred size in pixels
	 */
	public void setRectangleSize(int size) {

		if (this.minCircleSize < size) {
			this.tempCircleSize = this.minCircleSize;
			this.minCircleSize = 0;

		}

		if (this.tempCircleSize < size) {
			this.minCircleSize = this.tempCircleSize;
			this.tempCircleSize = 0;
		}

		this.recSize = this.leftMargin = size - 2;
		this.topMargin = this.recSize / 2;
	}

	/** only used 2d mode
	 * This method sets the new font size for the 2d diagram.
	 *
	 * @param size preferred font size in pixels
	 */
	public void setFontSize(int size) {
		this.fontSize = size;

		if (this.maxFontSize < size) {
			maxFontSize = size;
		}
	}

	/** Only used in 2d mode
	 *
	 * @return returns the now used font size
	 */
	public int getFontSize() {
		return this.fontSize;
	}

	/** This method enables or disables the 3d mode. If the 3d
	 *mode is disabled the 2d mode will be activated and vice versa.
	 * <br>
	 * @param b enables or disables the 3dmode
	 */
	public void set3d(boolean b) {
		mode3d = b;
	}

	/** Switches the display of signed or unsigned values.
	*
	* @param b signed or unsigned
	*/
	public void switchAbs(boolean b) {
		this.signed = b;
	}

	/** 3d mode only
	 * Switches the vertical axis.
	 *
	 * @param b switch axis
	 */
	public void switchPosNeg(boolean b) {
		this.switchPosNeg = b;
	}

	/** Calculates the preferred size based on the selected mode,
	 *rectangle size and the used data.<br>
	      /** Calculates the preferred size based on the selected mode,
	  *rectangle size and the used data.<br>
	  *
	  * @return preferred width, preferred height
	  */
	@Override
	public Dimension getPreferredSize() {

		if (mode3d)
			return new Dimension(calc3dWidth(), calc3dHeight());
		else
			return new Dimension(calcWidth(), calcHeight());
	}

	/** Sets the adaption mode. If b is true, the diagram will be resized,
	 *if the canvas is resized.
	 *
	 * @param b defines the adapting behaviour
	 */
	public void setAdapting(boolean b) {
		this.resize = b;
	}

	/** Sets the Canvas, on which the diagrams will be drawn.
	 *
	 *
	 * The class is able to manipulate the canvas, but this option is
	 *actually not used.
	 *
	 * @param c Drawable Canvas.
	 */
	@Override
	public void setCanvas(Canvas c) {
		canvas = c;
	}

	/** Sets the drawable space. If resize is activated, the graph will
	 *be adjusted if the bounds change
	 *
	 * @param b Rectangle that determines the new bounds
	 */
	@Override
	public void setBounds(Rectangle b) {
		this.bounds = b;

		double maxHeight = b.height;
		double maxWidth = b.width;

		if (resize) {
			if (mode3d) {
				setNew3dRecSize(maxHeight, maxWidth);
			} else {
				setNewRecSize(maxHeight, maxWidth);
			}
		}
	}

	/** This method returns the set bounds
	 *
	 * @return the actually used bounds
	 */
	@Override
	public Rectangle getBounds() {
		return this.bounds;
	}

	// required for drawable

	// required for drawable
	/** is needed for implementation of drawable<br>
	 * When the component is destroyed, all unneeded data is destroyed here.<br>
	 */
	@Override
	public void destroy() {
		destroyed = true;
	}

	/** returns the destroyed state.
	 *
	 * @return destroyed state
	 */
	@Override
	public boolean isDestroyed() {
		return destroyed;
	}

	/** This method is called to draw the graph. draw(Graphics g) decides,
	 *whether a 3d diagram or a 2d diagram is drawn.<br>
	 * <br>
	 * If mode3d is true, draw3dPlot(...) is called<br>
	 * otherwise drawPlot(...) is called<br>
	 * <br>
	 * @param g Graphics object on which will be drawn
	 */
	@Override
	public void draw(Graphics g) {

		if (bounds == null)
			return;

		if (this.canvas != null) {
			if (mode3d) {
				draw3dPlot(g);
			}

		}

	}

	//Table

	/** Adds a new row with data to the DataStorage. The values are
	 *given as a Vector, but have to be numeric.<br>
	 * <br>
	 * @param content Vector with numeric values
	 */
	public void addRow(Vector content) {
		table.addRow(content);
	}

	/** Updates the used DataStorage. The given Vector will be saved at
	 *the position, which is defined with the int value.<br>
	 * <br>
	 * The old Vector at this position will be removed.<br>
	 * <br>
	 * @param adr adress, which determines the position of the Vector
	 * @param content the new Vector with numeric data which will be placed
	 *      at the position defined with the given int
	 */
	public void updateRow(int adr, Vector content) {
		table.updateRow(adr, content);
	}

	/** Adds a new column to the DataStorage. The Vector content has to
	 *contain numeric values. These values are used to draw the graphics.<br>
	 * <br>
	 * The new column is added at the end of the table.<br>
	 * <br>
	 * @param content Vector with numeric values.
	 */
	public void addColumn(Vector content) {
		table.addColumn(content);
	}

	// TischFunktionen
	/** A string with alphanumeric values, which is stored in a description
	 *Vector. The descriptions for the rows have to be added in linear order.<br>
	 * <br>
	 * If the description for attribute 4 is added, the first 3 attribute
	     *descriptions have to be added beforehand because the new description is added
	 *at the end of the Vector.
	 *
	 * @param des Description for a row in the StorageTable
	 */
	public void addDescriptor(String des) {
		descriptor.addElement(des);
	}

	//Painting Functions

	// Drawfunctions
	// 3D -------------------------------------------------------------------------------

	/** used in 3d mode
	     * This method draws a 3d bar in the selected row and column width the submitted
	 * value space is used for the calculation of the width and height.
	 * space represents the maximum width.<br>
	 * <br>
	 * The method is called in an iteration in draw3dPlot.
	 * Based on the given row and column parameter, the method
	 * takes one of the values out of the actual instance of DataStorage.<br>
	 * <br>
	 * The bars of one row are filled, if the row is selected with
	 * rowSelected. The color is blue, if the actual value is positive,
	 * otherwise it is red. All other bars are drawn as grids.<br>
	 * <br>
	 * @param iRow selected row
	 * @param iColumn selected column
	 * @param space maximum width
	 * @param value value (interval: [1.0;-1.0]
	 * @param g used Graphics object
	 */
	private void drawBar(int iRow, int iColumn, int space, double value, Graphics g) {

		/* This function draws the bars for the 3d diagram */

		/*calculates the colour for positive and negative values
		     based on RGB and red for positive and blue for negative values, we use
		     a spectrum from r    = [128; 255] e.g. if the value is 1.0, r=255, b=0, g=0
		                                        b, g = [0  ; 128]
		 */

		Color temp = new Color(0);

		int rot = 0;
		int gelb = 0;
		int blau = 0;
		double vTemp = Math.abs(value);

		if (value <= 0) {

			rot = (int) Math.round(128 * vTemp);
			gelb = (int) Math.round(255 * vTemp);
			blau = gelb;

		} else {

			blau = (int) Math.round(vTemp * 128);
			rot = (int) Math.round(255 * vTemp);
			gelb = rot;

		}

		if (!signed) {
			value = Math.abs(value);
		} else if (switchPosNeg == true) {
			value = (-1 * value);
		}

		temp = new Color(255 - rot, 255 - gelb, 255 - blau);

		int neigung = 3;

		int maxHoehe = space;
		int hoehe = (int) Math.round(maxHoehe * value);
		int borderX;
		int borderY;
		int widthX;
		int widthY;

		Polygon p1 = new Polygon(); //front
		Polygon p2 = new Polygon(); //seite
		Polygon p3 = new Polygon(); //deckeloben
		Polygon p4 = new Polygon(); //links
		Polygon p5 = new Polygon(); //hinten
		Polygon p6 = new Polygon(); //center

		borderX = leftMargin + iColumn * (space - space / neigung) + iRow * (space / neigung);
		borderY = topMargin + (table.rowCount + 1) * space / neigung + iColumn * space / neigung - iRow * (space / neigung);

		//front
		p1.addPoint(borderX, borderY);
		p1.addPoint(borderX, borderY - hoehe);
		p1.addPoint(borderX + space - space / neigung, borderY + space / neigung - hoehe);
		p1.addPoint(borderX + space - space / neigung, borderY + space / neigung);

		//right side
		p2.addPoint(borderX + space - space / neigung, borderY + space / neigung);
		p2.addPoint(borderX + space, borderY);
		p2.addPoint(borderX + space, borderY - hoehe);
		p2.addPoint(borderX + space - space / neigung, borderY + space / neigung - hoehe);

		//top
		p3.addPoint(borderX, borderY - hoehe);
		p3.addPoint(borderX + space / neigung, borderY - space / neigung - hoehe);
		p3.addPoint(borderX + space, borderY - hoehe);
		p3.addPoint(borderX + space - space / neigung, borderY + space / neigung - hoehe);

		//left side
		p4.addPoint(borderX, borderY);
		p4.addPoint(borderX, borderY - hoehe);
		p4.addPoint(borderX + space / neigung, borderY - space / neigung - hoehe);
		p4.addPoint(borderX + space / neigung, borderY - space / neigung);

		//back
		p5.addPoint(borderX + space, borderY);
		p5.addPoint(borderX + space, borderY - hoehe);
		p5.addPoint(borderX + space / neigung, borderY - space / neigung - hoehe);
		p5.addPoint(borderX + space / neigung, borderY - space / neigung);

		//bottom
		p6.addPoint(borderX, borderY);
		p6.addPoint(borderX + space / neigung, borderY - space / neigung);
		p6.addPoint(borderX + space, borderY);
		p6.addPoint(borderX + space / neigung, borderY - space / neigung);

		if ((iRow + 1 == this.rowSelected) && (iRow != iColumn)) {
			g.setColor(temp);
			g.fillPolygon(p1);
			g.fillPolygon(p2);
			g.fillPolygon(p3); //top
			g.fillPolygon(p4);
			g.fillPolygon(p5);
			g.fillPolygon(p6);

			g.setColor(new Color(0));
			g.drawPolygon(p1);
			g.drawPolygon(p2);
			g.drawPolygon(p3);
			g.drawPolygon(p4);
			g.drawPolygon(p5);
			g.setColor(gridColor);
			g.drawPolygon(p6);

		} else {

			g.setColor(temp);
			g.drawPolygon(p1);
			g.drawPolygon(p2);

			g.drawPolygon(p3);

			g.setColor(temp);
			g.drawPolygon(p4);
			g.drawPolygon(p5);
			g.setColor(gridColor);
			g.drawPolygon(p6);

		}
	}

	/** used for 3d mode
	 * Draws the descriptions for the 3d graph. The font size will be adjusted
	 * based on the given value space.<br>
	 *  <br>
	 * <br>
	 * All desriptions are drawn once, after draw3dPlot has finished drawing
	 * (the methods drawGrid(...) are called out of an iteration in
	 * draw3dPlot(...) and draw only one bar at a time).<br>
	 * <br>
	 * @param space maximum width
	 * @param g Graphics object
	 */
	private void set3dDescriptors(int space, Graphics g) {
		int borderX;
		int borderY;
		int neigung = 3;
		String text = "";

		if (descriptor.size() == table.columnCount) {

			int size = space / 7;
			g.setFont(new Font("Arial", 0, size));

			for (int i = -1; i <= table.columnCount; i++) {
				for (int k = -1; k <= table.rowCount + 1; k++) {

					borderX = leftMargin + i * (space - space / neigung) + k * (space / neigung);
					borderY = topMargin + (table.rowCount + 1) * space / neigung + i * space / neigung - k * (space / neigung);

					if ((i == -1) && (k >= 0) && (k <= table.rowCount)) {
						text = String.valueOf(descriptor.elementAt(k));

						if (text.length() > 5) {
							text = text.substring(0, 5);
						}

						g.setColor(this.fontColor);
						g.drawString(text, borderX + space / neigung, borderY);
					}

					if ((k == -1) && (i >= 0) && (i < table.columnCount)) {
						text = String.valueOf(descriptor.elementAt(i));

						if (text.length() > 5) {
							text = text.substring(0, 5);

						}

						g.setColor(this.fontColor);
						g.drawString(text, borderX + space / neigung, borderY);
					}
				}
			}
		}
	}

	/** used for 3d mode
	 * Draws the 3d grid with the selected grid colour. The grid is adjusted with space.<br>
	 *  <br>
	 * <br>
	 * All rectangles are drawn once, out of an iteration.<br>
	 * <br>
	 * <br>
	 * <br>
	 * @param space maximum with
	 * @param g Graphicobject
	 */
	private void drawGrid(int space, Graphics g) {
		/* tablepos saves the current drawing position in String size
		     * iRow's value is the current row and iColumn's value is the current column
		 * space is the current font size. The font size is needed for distance calculations
		 * between the lines, which will be drawn
		 */

		int neigung = 3;

		int borderX;
		int borderY;
		int widthX;
		int widthY;
		int rX = 0;
		int rY = 0;
		int oY = 0;
		int oX = 0;
		Polygon p;

		for (int i = -1; i <= table.columnCount; i++) {
			for (int k = -1; k <= table.rowCount + 1; k++) {

				p = new Polygon();
				borderX = leftMargin + i * (space - space / neigung) + k * (space / neigung);
				borderY = topMargin + (table.rowCount + 1) * space / neigung + i * space / neigung - k * (space / neigung);

				p.addPoint(borderX, borderY);
				p.addPoint(borderX + space / neigung, borderY - space / neigung);
				p.addPoint(borderX + space, borderY);
				p.addPoint(borderX + space - space / neigung, borderY + space / neigung);

				g.setColor(gridColor);

				g.drawPolygon(p);

				if ((i == table.columnCount) && (k == 0)) {
					rX = borderX + space - space / neigung;
					rY = borderY + space / neigung;
				}
				if ((i == 0) && (k == table.rowCount)) {
					oX = borderX + space / neigung;
					oY = borderY - space / neigung;
				}
			}
		}

		borderX = this.leftMargin;
		borderY = this.topMargin + (space * (table.rowCount + 1)) / neigung;

		g.setColor(new Color(0));
		g.drawLine(borderX, borderY, rX, rY);

		g.drawLine(borderX - 1, borderY, oX - 1, oY);

		widthY = (table.rowCount + 1) * (space - space / neigung);

		g.drawLine(leftMargin - 1, topMargin, leftMargin - 1, widthY);

	}

	/** used in 3d mode<br>
	 * Draws the 3d plot. Calls the functions drawbar, drawGrid, set3dDescriptors.<br>
	 * <br>
	 * This method draws the 3d bar graph. The method iterates through the
	 * DataStorage instance and calls drawBar(...) each time. The methods drawGrid(...)
	     * and drawDescriptors are called after the drawing of the graph is finished.<br>
	 * <br>
	 * draw3dPlot(...) is called from draw(...).<br>
	 * <br>
	 * @param g Graphics object
	 */
	private void draw3dPlot(Graphics g) {

		if ((table.rowCount >= 0) && (table.columnCount > 0) && (g != null)) {
			/* checks if drawing is allowed.
			 * if there is no data for drawing and no place to draw (g) then
			 * drawing will not start */

			//this.setBounds(this.bounds);

			String zk = ""; //later used temporary String
			int tablepos = 0; //later used for calculation issues (table and font position)
			int iRow = 0; //used for counting issues
			Vector content = new Vector(); //temporary Vector.

			double value = 0;

			//Initialising of used fonts.
			int space = this.recSize; // if font size is variable, this is the space variable (for table and font position)
			Font f = new Font("Arial", 0, fontSize);
			g.setFont(f);

			//Clears the Graphics for redrawing.
			g.setColor(backgroundColor);
			Rectangle rec = bounds;
			g.fillRect(0, 0, rec.width, rec.height);

			drawGrid(space, g);

			for (int iColumn = 0; iColumn < table.columnCount; iColumn++) {
				/* iColumn is the counter for the column. The table will be drawn
				 * columnwise. */

				for (iRow = table.rowCount; iRow >= 0; iRow--) {
					// iRow counts the rows.
					content = (Vector) table.row.elementAt(iRow); //gets the Vector row[iRow]

					if (content.elementAt(iColumn) instanceof String) {
						zk = (String) content.elementAt(iColumn);
						value = Double.valueOf(zk).doubleValue();
					} else if (content.elementAt(iColumn) instanceof Float) {
						Float fk = (Float) content.elementAt(iColumn);
						value = fk.doubleValue();
					}

					drawBar(iRow, iColumn, space, value, g);

				}
				tablepos += table.obj2int(table.columnSize.elementAt(iColumn));
			}
			set3dDescriptors(space, g);
		}
	}

}
