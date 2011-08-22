package spade.vis.mapvis;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.Hashtable;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.util.Vector;

import spade.lib.lang.Language;
import spade.lib.util.StringUtil;
import spade.vis.database.ThematicDataItem;
import spade.vis.geometry.Sign;
import spade.vis.geometry.StackSign;

/**
* Represents qualitative attributes that may have multiple values associated
* with a single object by "stack" signs showing the number of items in a list.
* Alternatively, may represent simultaneously several logical attributes.
*/
public class StackDrawer extends ListPresenter implements SignDrawer {
	static ResourceBundle res = Language.getTextResource("spade.vis.mapvis.Res");
	protected StackSign stack = null;

	/**
	* Constructs an instance of StackSign for further use.
	*/
	protected void checkCreateSign() {
		if (stack != null)
			return;
		stack = new StackSign();
		stack.setBorderColor(Color.black);
		stack.setColor(super.getDefaultColor());
	}

	/**
	* Returns the default color used when items are not distinguished by colors
	*/
	@Override
	public Color getDefaultColor() {
		if (stack == null)
			return super.getDefaultColor();
		return stack.getColor();
	}

	/**
	* Sets the default color to be used when items are not distinguished by colors
	*/
	@Override
	public void setDefaultColor(Color color) {
		if (stack != null) {
			stack.setColor(color);
		}
		super.setDefaultColor(color);
	}

	/**
	* This method is called by a GeoObject. The GeoObject sends its DataItem
	* to the Visualizer, and the Visualizer returns an Object specifying the
	* presentation of data contained in this DataItem. This may be, in
	* particular, Color or Diagram. Typically the argument DataItem is
	* a ThematicDataItem, although in some cases (e.g. manual classification)
	* this is not required.
	* hdz added 1 or 0 for Boolean Numbers
	*/
	@Override
	public Object getPresentation(ThematicDataItem dit) {
		if (dit == null || attr == null)
			return null;
		if (sdController != null && !sdController.mustDrawObject(dit.getId()))
			return null;
		Vector items = null;
		if (attr.size() > 1) {
			for (int i = 0; i < attr.size(); i++) {
				String val = getStringAttrValue(dit, i);
				if (val == null) {
					continue;
				}
				if (val.equalsIgnoreCase("T") || val.equalsIgnoreCase("true") || val.equalsIgnoreCase("yes") || val.equalsIgnoreCase("1")) {
					if (items == null) {
						items = new Vector(attr.size(), 1);
					}
					items.addElement(attr.elementAt(i));
				}
			}
		} else {
			String val = getStringAttrValue(dit, 0);
			if (val == null)
				return null;
			items = StringUtil.getNames(val, ";");
		}
		if (items == null)
			return null;
		//check what items are hidden
		if (hiddenItems != null && hiddenItems.size() > 0) {
			for (int i = items.size() - 1; i >= 0; i--)
				if (StringUtil.isStringInVectorIgnoreCase((String) items.elementAt(i), hiddenItems)) {
					items.removeElementAt(i);
				}
		}
		if (items.size() < 1)
			return null;
		checkCreateSign();
		stack.setNItems(items.size());
		stack.setUseColors(useColors);
		if (useColors) {
			for (int i = 0; i < items.size(); i++) {
				stack.setColorForItem(getColorForItem((String) items.elementAt(i)), i);
			}
		}
		return stack;
	}

	/**
	* The Visualizer sets its parameters. For this purpose it may need
	* data statistics. Statistics are received from a StatisticsProvider.
	* A reference to a StatisticsProvider may be received from the DataInformer.
	*/
	@Override
	public void setup() {
		if (!isApplicable(attr))
			return;
	}

	/**
	* Draws a small picture symbolizing this presentation technique
	*/
	@Override
	public void drawIcon(Graphics g, int x, int y, int w, int h) {
		checkCreateSign();
		int sw = stack.getMinWidth(), sh = stack.getMinHeight(), shift = StackSign.shift;
		if (sw > w - shift) {
			sw = w - shift;
		}
		if (sh > h - shift) {
			sh = h - shift;
		}
		int x0 = x + (w - sw - shift) / 2, y0 = y + (h - sh - shift) / 2;
		Color c = stack.getColor();
		if (useColors)
			if (itemColors != null && itemColors.size() > 1) {
				c = (Color) itemColors.elementAt(1);
			} else if (attr.size() > 1) {
				c = getColorForAttribute(1);
			} else {
				c = Color.orange;
			}
		g.setColor(c);
		g.fillRect(x0 + shift, y0, sw, sh);
		g.setColor(stack.getBorderColor());
		g.drawRect(x0 + shift, y0, sw, sh);
		if (useColors)
			if (itemColors != null && itemColors.size() > 0) {
				c = (Color) itemColors.elementAt(0);
			} else if (attr.size() > 1) {
				c = getColorForAttribute(0);
			} else {
				c = Color.blue;
			}
		g.setColor(c);
		g.fillRect(x0, y0 + shift, sw, sh);
		g.setColor(stack.getBorderColor());
		g.drawRect(x0, y0 + shift, sw, sh);
	}

	/**
	* Draws the part of the legend explaining this specific presentation method.
	*/
	@Override
	public Rectangle drawMethodSpecificLegend(Graphics g, int startY, int leftmarg, int prefW) {
		checkCreateSign();
		int x = leftmarg, y = startY;
		stack.setNItems(3);
		int h = stack.getHeight();
		stack.setUseColors(false);
		for (int i = 1; i < 4; i++) {
			stack.setNItems(i);
			int w = stack.getWidth();
			if (x + w > prefW) {
				break;
			}
			stack.draw(g, x, y, w, h);
			x += w + 10;
		}
		stack.setUseColors(useColors);
		y += h + 3;
		int maxW = x - 10 - leftmarg;
		g.setColor(Color.black);
		g.drawLine(leftmarg, y, maxW + leftmarg, y);
		g.drawLine(leftmarg, y - 3, leftmarg, y);
		g.drawLine(leftmarg + maxW, y - 3, leftmarg + maxW, y);
		g.drawLine(leftmarg + maxW / 2, y, leftmarg + maxW / 2, y + 5);
		y += 6;
		FontMetrics fm = g.getFontMetrics();
		int fh = fm.getHeight(), asc = fm.getAscent();
		String str = (attr.size() > 1) ? res.getString("number_of_true_values") : res.getString("number_of_values_of");
		g.drawString(str, leftmarg, y + asc);
		y += fh;
		int w = fm.stringWidth(str);
		if (maxW < w) {
			maxW = w;
		}
		int rw = stack.getMinWidth(), rh = stack.getMinHeight();
		if (rh > fh) {
			rh = fh;
		}
		for (int i = 0; i < attr.size(); i++) {
			str = getAttrName((String) attr.elementAt(i));
			if (i < attr.size() - 1) {
				str += ",";
			}
			w = 0;
			if (useColors && attr.size() > 1) {
				g.setColor(getColorForAttribute(i));
				g.fillRect(leftmarg, y + (fh - rh) / 2, rw, rh);
				g.setColor(stack.getBorderColor());
				g.drawRect(leftmarg, y + (fh - rh) / 2, rw, rh);
				w = rw + 10;
			}
			g.drawString(str, leftmarg + w, y + asc);
			y += fh;
			w += fm.stringWidth(str);
			if (maxW < w) {
				maxW = w;
			}
		}
		if (useColors && attr.size() == 1) {
			if (itemNames == null) {
				getAllItems();
			}
			if (itemNames != null && itemNames.size() > 0) {
				y += 5;
				for (int i = 0; i < itemNames.size(); i++) {
					g.setColor((Color) itemColors.elementAt(i));
					g.fillRect(leftmarg, y + (fh - rh) / 2, rw, rh);
					g.setColor(stack.getBorderColor());
					g.drawRect(leftmarg, y + (fh - rh) / 2, rw, rh);
					str = (String) itemNames.elementAt(i);
					g.drawString(str, leftmarg + rw + 10, y + asc);
					y += fh;
					w = fm.stringWidth(str) + rw + 10;
					if (maxW < w) {
						maxW = w;
					}
				}
			}
		}
		return new Rectangle(leftmarg, startY, maxW, y - startY + 5);
	}

	/**
	* Produces an instance of the Sign used by this visualizer. This instance will
	* be used for drawing the sign in the dialog for changing sign properties.
	*/
	@Override
	public Sign getSignInstance() {
		checkCreateSign();
		StackSign ss = new StackSign();
		ss.setMayChangeProperty(Sign.MIN_SIZE, true);
		ss.setBorderColor(stack.getBorderColor());
		ss.setColor(stack.getColor());
		ss.setMinWidth(stack.getMinWidth());
		ss.setMinHeight(stack.getMinHeight());
		return ss;
	}

	/**
	* Through this method a SignDrawer is informed that some of the sign properties
	* have changed. The sign drawer must care about repainting the map.
	* The possible identifiers of sign properties are listed in the class
	* @see spade.vis.geometry.Sign.
	*/
	@Override
	public void signPropertyChanged(int propertyId, Sign sgn) {
		if (sgn == null || stack == null)
			return;
		if (propertyId == Sign.MIN_SIZE) {
			stack.setMinSizes(sgn.getMinWidth(), sgn.getMinHeight());
			notifyVisChange();
		}
	}

	/**
	* Must reply whether any parameters of this visualization method may be
	* changed, e.g. colors or sizes of signs. This does not include interactive
	* analytical manipulation.
	* Returns true.
	*/
	@Override
	public boolean canChangeParameters() {
		return true;
	}

	/**
	* Constructs and displays a dialog for changing parameters of this
	* visualization method: minimum and maximum sizes of the signs.
	*/
	@Override
	public void startChangeParameters() {
		SignParamsController pc = new SignParamsController();
		pc.startChangeParameters(this);
	}

//ID
	@Override
	public Hashtable getVisProperties() {
		Hashtable param;
		param = super.getVisProperties();
		if (param == null) {
			param = new Hashtable();
		}
		if (getItemColors() != null) {
			StringBuffer sb = new StringBuffer();
			StringBuffer sbAct = new StringBuffer();
			for (int i = 0; i < attr.size(); i++) {
				if (useColors) {
					sb.append(Integer.toHexString(getColorForItem((String) attr.elementAt(i)).getRGB()).substring(2));
				}
				sb.append(" ");
				sbAct.append(isItemActive((String) attr.elementAt(i)) ? "+ " : "- ");
//        if (StringUtil.isStringInVectorIgnoreCase((String)items.elementAt(i),hiddenItems))
			}
			if (useColors) {
				param.put("itemColors", sb.toString());
			}
			param.put("activeClasses", sbAct.toString());
		}

		param.put("useColors", String.valueOf(useColors));

		checkCreateSign();
		param.put("width", String.valueOf(stack.getMinWidth()));
		param.put("height", String.valueOf(stack.getMinHeight()));

		return param;
	}

	@Override
	public void setVisProperties(Hashtable param) {
		checkCreateSign();
		try {
			stack.setMinHeight(new Integer((String) param.get("height")).intValue());
		} catch (Exception ex) {
		}
		try {
			stack.setMinWidth(new Integer((String) param.get("width")).intValue());
		} catch (Exception ex) {
		}

		try {
			useColors = new Boolean((String) param.get("useColors")).booleanValue();
		} catch (Exception ex) {
		}

		String sColors = (String) param.get("itemColors");
		if (sColors != null && sColors.length() > 0 && !sColors.equals("null")) {
			StringTokenizer st = new StringTokenizer(sColors, " ");
			if (st.countTokens() == attr.size()) {
				int i = 0;
				while (st.hasMoreTokens()) {
					setColorForItem(new Color(Integer.parseInt(st.nextToken(), 16)), (String) attr.elementAt(i));
					i++;
				}
			}
		}

		String sActive = (String) param.get("activeClasses");
		if (sActive != null && sActive.length() > 0) {
			StringTokenizer st = new StringTokenizer(sActive, " ");
			if (st.countTokens() == attr.size()) {
				int i = 0;
				while (st.hasMoreTokens()) {
					setItemActive((String) attr.elementAt(i), st.nextToken().equals("+"));
					i++;
				}
			}
		}

		super.setVisProperties(param);
	}
//~ID
}
