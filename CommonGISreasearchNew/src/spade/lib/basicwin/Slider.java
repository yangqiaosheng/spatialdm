package spade.lib.basicwin;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import spade.lib.util.StringUtil;
import spade.time.TimeMoment;

public class Slider extends Canvas implements ActionListener, MouseListener, MouseMotionListener {
	public static final int UPDATE = 2001;
	public static int mm = Metrics.mm(), prefH = 6 * mm, minH = 4 * mm, prefW = 60 * mm;
	public double absMin, absMax;
	public int minl, maxl, slst, slfi, sly;
	public boolean drawMinMaxLabels = true;
	public double absP; // ArrowPos
	public int scrP = -1, prevScrP = -1;
	protected boolean drag = false;
	protected boolean NAD = false; // Notify About Drag
	/**
	 * Indicates that the value is expected to be integer
	 */
	protected boolean valueIsInteger = false;
	protected ActionListener al = null;
	/**
	 * If the attribute is temporal, these fields contain the absolute minimum and
	 * maximum time moments.
	 */
	protected TimeMoment minTime = null, maxTime = null;
	/**
	 * The length of the time interval
	 */
	protected long timeLength = 0L;

	/**
	* The text field for specification of the current value
	*/
	protected TextField TF = null;
	/**
	* Indicates "destroyed" state. Initially id false.
	*/
	protected boolean destroyed = false;

	public Slider(ActionListener al, double absMin, double absMax, double absP) {
		super();
		this.al = al;
		this.absMin = absMin;
		this.absMax = absMax;
		this.absP = absP;
		addMouseListener(this);
		addMouseMotionListener(this);
		setSize(getSize().width, 3 * mm + 16);
	}

	public Slider(ActionListener al, float absMin, float absMax, float absP) {
		super();
		this.al = al;
		this.absMin = absMin;
		this.absMax = absMax;
		this.absP = absP;
		addMouseListener(this);
		addMouseMotionListener(this);
		setSize(getSize().width, 3 * mm + 16);
	}

	/**
	 * If the attribute is temporal, sets the absolute minimum and maximum time
	 * moments. Thios information is used for displaying "nice" dates in text
	 * fields.
	 */
	public void setAbsMinMaxTime(TimeMoment t1, TimeMoment t2) {
		minTime = t1;
		maxTime = t2;
		if (minTime != null && maxTime != null) {
			timeLength = maxTime.subtract(minTime);
		} else {
			timeLength = 0L;
		}
	}

	public TimeMoment getMinTime() {
		return minTime;
	}

	public TimeMoment getMaxTime() {
		return maxTime;
	}

	public long getTimeIntervalLength() {
		return timeLength;
	}

	protected void computeLabelWidths() {
		if (minTime != null) {
			minl = Metrics.stringWidth(minTime.toString()) + 2 * mm;
		} else {
			minl = Metrics.stringWidth(StringUtil.doubleToStr(absMin)) + 2 * mm;
		}
		if (maxTime != null) {
			maxl = Metrics.stringWidth(maxTime.toString()) + 2 * mm;
		} else {
			maxl = Metrics.stringWidth(StringUtil.doubleToStr(absMax)) + 2 * mm;
		}
	}

	public void setValues(float absMin, float absMax, float absP) {
		this.absMin = absMin;
		this.absMax = absMax;
		computeLabelWidths();
		setValue(absP);
	}

	public void setValues(double absMin, double absMax, double absP) {
		this.absMin = absMin;
		this.absMax = absMax;
		computeLabelWidths();
		setValue(absP);
	}

	public void setAbsMin(float absMin) {
		this.absMin = absMin;
		computeLabelWidths();
		repaint();
	}

	public void setAbsMin(double absMin) {
		this.absMin = absMin;
		computeLabelWidths();
		repaint();
	}

	public void setAbsMax(float absMax) {
		this.absMax = absMax;
		computeLabelWidths();
		repaint();
	}

	public void setAbsMax(double absMax) {
		this.absMax = absMax;
		computeLabelWidths();
		repaint();
	}

	public double getAbsMin() {
		return absMin;
	}

	public double getAbsMax() {
		return absMax;
	}

	public void setMinMaxL(int minl, int maxl) {
		this.minl = minl;
		this.maxl = maxl;
	}

	public void setShowMinMaxLabels(boolean show) {
		drawMinMaxLabels = show;
	}

	public void setNAD(boolean NAD) {
		this.NAD = NAD;
	}

	public void setValueIsInteger(boolean valueIsInteger) {
		this.valueIsInteger = valueIsInteger;
	}

	public double getValue() {
		return absP;
	}

	public int getPos() {
		return scrP - slst - 3;
	}

	public int getIntValue() {
		return (int) Math.round(absP);
	}

	public TimeMoment getTimeForPos(double pos) {
		if (minTime == null || maxTime == null || timeLength < 1)
			return null;
		if (pos <= absMin)
			return minTime;
		if (pos >= absMax)
			return maxTime;
		double factor = (pos - absMin) / (absMax - absMin);
		long timePos = Math.round(factor * timeLength);
		TimeMoment t = minTime.getCopy();
		t.add((int) timePos);
		return t;
	}

	public TimeMoment getCurrTime() {
		return getTimeForPos(absP);
	}

	public void showValueInField() {
		if (minTime != null && getCurrTime() == null) // it may happen if time filter is working
			return;
		if (TF != null)
			if (minTime != null) {
				TF.setText(getCurrTime().toString());
			} else if (valueIsInteger) {
				TF.setText(String.valueOf(this.getIntValue()));
			} else {
				TF.setText(StringUtil.doubleToStr(absP, absMin, absMax));
			}
	}

	public void setValue(float absP) {
		this.absP = absP;
		showValueInField();
		repaint();
	}

	public void setValue(double absP) {
		this.absP = absP;
		showValueInField();
		repaint();
	}

	public void setPos(int pos) {
		scrP = 3 + slst + pos;
		absP = ScrToAbs(scrP);
		if (scrP < 3 + slst) {
			scrP = -1;
			prevScrP = -1;
		}
		repaint();
	}

	@Override
	public void setEnabled(boolean b) {
		super.setEnabled(b);
		if (TF != null) {
			TF.setEnabled(b);
		}
	}

	protected int AbsToScr(double xf) {
		return (int) ((slfi - slst - 6) * (xf - absMin) / (absMax - absMin));
	}

	protected double ScrToAbs(int x) {
		return absMin + (absMax - absMin) * (x - slst - 3) / (slfi - slst - 6);
	}

	protected void drawArrow(Graphics g, int x, int y) {
		int h = 17;
		if (y - 8 + h > getSize().height - 2) {
			h = getSize().height - 2 - y + 8;
		}
		if (prevScrP >= 3 + slst && prevScrP <= slfi - 3) {
			int xx = prevScrP;
			g.setColor(getBackground());
			g.fillRect(xx - 3, y - 8, 7, h);
			g.setColor(Color.gray);
			g.drawLine(xx - 3, sly, xx + 3, sly);
			g.setColor(Color.black);
			g.drawLine(xx - 3, sly + 1, xx + 3, sly + 1);
			g.setColor(Color.gray.brighter());
			g.drawLine(xx - 3, sly + 2, xx + 3, sly + 2);
			g.setColor(Color.white);
			g.drawLine(xx - 3, sly + 3, xx + 3, sly + 3);
		}
		if (x >= 3 + slst && x <= slfi - 3) {
			int X[] = new int[6], Y[] = new int[6];
			X[0] = x;
			Y[0] = y - 8;
			X[1] = x - 2;
			Y[1] = y - 6;
			X[2] = x - 2;
			Y[2] = y - 8 + h - 1;
			X[3] = x + 2;
			Y[3] = y - 8 + h - 1;
			X[4] = x + 2;
			Y[4] = y - 6;
			X[5] = X[0];
			Y[5] = Y[0];
			g.setColor(Color.lightGray);
			g.fillPolygon(X, Y, 6);
			g.setColor(Color.darkGray);
			g.drawPolygon(X, Y, 6);
			X[0] -= 1;
			Y[0] += 1;
			g.setColor(Color.lightGray.brighter());
			g.drawPolygon(X, Y, 4);
		}
		prevScrP = x;
	}

	public void paintBasics(Graphics g) {
		//g.setColor(Color.gray);
		//g.fillRect(0,0,size().width-1,size().height-1);
		g.setColor(Color.black);
		g.drawRect(0, 0, getSize().width - 1, getSize().height - 1);
		if (drawMinMaxLabels) {
			slst = minl + mm;
			slfi = getSize().width - maxl - mm;
		} else {
			slst = 1 + mm;
			slfi = getSize().width - 1 - mm;
		}
		if (slfi - slst % 10 != 6) {
			slfi = slst + 6 + ((slfi - slst - 6) / 10) * 10;
		}
		sly = 3 * mm;
	}

	@Override
	public void paint(Graphics g) {
		if (!isEnabled())
			return;
		if (Metrics.fmetr == null) {
			Metrics.setFontMetrics(g.getFontMetrics());
		}
		computeLabelWidths();
		paintBasics(g);
		// Texts: min & max
		if (drawMinMaxLabels) {
			g.setColor(Color.black);
			if (minTime == null) {
				g.drawString(StringUtil.doubleToStr(absMin), mm, mm + Metrics.asc);
			} else {
				g.drawString(minTime.toString(), mm, mm + Metrics.asc);
			}
			if (maxTime == null) {
				g.drawString(StringUtil.doubleToStr(absMax), getSize().width - maxl, mm + Metrics.asc);
			} else {
				g.drawString(maxTime.toString(), getSize().width - maxl, mm + Metrics.asc);
			}
		}
		// Ruler:
		g.setColor(Color.gray);
		g.drawLine(slst, sly, slfi, sly);
		g.drawLine(slst - 1, sly, slst - 1, sly + 2);
		g.setColor(Color.black);
		g.drawLine(slst, sly + 1, slfi, sly + 1);
		g.setColor(Color.gray.brighter());
		g.drawLine(slst, sly + 2, slfi, sly + 2);
		g.setColor(Color.white);
		g.drawLine(slst - 1, sly + 3, slfi, sly + 3);
		g.drawLine(slfi + 1, sly, slfi + 1, sly + 3);
		// strokes
		g.setColor(Color.black);
		for (int i = 0; i <= 10; i++) {
			int x = 3 + slst + i * ((slfi - slst) / 10);
			g.drawLine(x, sly - 5 - mm, x, sly - 4);
		}
		// arrow
		scrP = 3 + slst + AbsToScr(absP);
		if (scrP < 3 + slst) {
			scrP = -1;
			prevScrP = -1;
		}
		drawArrow(g, scrP, sly + 5);
	}

	/**
	* Sets the Text fields for exact specification of minimum and maximum limits
	*/
	public void setTextField(TextField valField) {
		if (TF != null) {
			TF.removeActionListener(this);
		}
		TF = valField;
		if (TF != null) {
			TF.addActionListener(this);
		}
	}

	public TextField getTextField() {
		return TF;
	}

	/**
	* Reaction to action events from the text fields in which minimum and
	* maximum values can be specified
	*/
	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == TF) {
			String str = TF.getText().trim();
			double val = absP;
			try {
				val = Double.valueOf(str).doubleValue();
			} catch (NumberFormatException nfe) {
				showValueInField();
			}
			if (val < absMin) {
				val = absMin;
			}
			if (val > absMax) {
				val = absMax;
			}
			setValue(val);
			al.actionPerformed(new ActionEvent(this, UPDATE, null));
		}
	}

	@Override
	public void mouseClicked(MouseEvent e) {
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {
	}

	@Override
	public void mouseMoved(MouseEvent e) {
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if (slfi - slst - 6 < 10)
			return; //the slider is too small
		int x = e.getX();
		drag = false;
		if (x < slst + 3) {
			x = slst + 3;
		}
		if (x > slfi - 3) {
			x = slfi - 3;
		}
		scrP = x;
		absP = ScrToAbs(scrP);
		//getParent().postEvent(new Event(this,UPDATE,new Float(absP)));
		showValueInField();
		al.actionPerformed(new ActionEvent(this, UPDATE, null));
	}

	@Override
	public void mousePressed(MouseEvent e) {
		if (slfi - slst - 6 < 10)
			return; //the slider is too small
		int x = e.getX();
		if (Math.abs(x - scrP) > 5) {
			prevScrP = scrP; //slst+3+AbsToScr(absP);
			if (x < slst + 3) {
				x = slst + 3;
			}
			if (x > slfi - 3) {
				x = slfi - 3;
			}
			scrP = x;
			absP = ScrToAbs(scrP);
			repaint();
			//getParent().postEvent(new Event(this,UPDATE,new Float(absP)));
			//al.actionPerformed(new ActionEvent(this,UPDATE,null));
			return;
		}
		drag = true;
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		if (slfi - slst - 6 < 10)
			return; //the slider is too small
		int x = e.getX();
		if (!drag)
			return;
		if (Math.abs(x - prevScrP) == 0)
			return;
		if (x < slst + 3) {
			x = slst + 3;
		}
		if (x > slfi - 3) {
			x = slfi - 3;
		}
		scrP = x;
		absP = ScrToAbs(scrP);
		Graphics g = getGraphics();
		drawArrow(g, x, sly + 5);
		g.dispose();
		prevScrP = x;
		//if (NAD) getParent().postEvent(new Event(this,UPDATE,new Float(absP)));
		if (NAD) {
			showValueInField();
			al.actionPerformed(new ActionEvent(this, UPDATE, null));
		}
	}

	public boolean getIsDragging() {
		return drag;
	}

	public void setPreferredSize(int w, int h) {
		prefW = w;
		prefH = h;
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(prefW, prefH);
	}

	public void destroy() {
		if (TF != null) {
			TF.removeActionListener(this);
		}
		destroyed = true;
	}

	/**
	* Replies whether is destroyed or not
	*/
	public boolean isDestroyed() {
		return destroyed;
	}
}
