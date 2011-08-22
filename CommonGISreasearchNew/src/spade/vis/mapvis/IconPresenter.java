package spade.vis.mapvis;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.lib.basicwin.CManager;
import spade.lib.basicwin.OKDialog;
import spade.lib.basicwin.StringInRectangle;
import spade.lib.help.Helper;
import spade.lib.lang.Language;
import spade.lib.util.QSortAlgorithm;
import spade.lib.util.StringUtil;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataTable;
import spade.vis.database.SemanticsManager;
import spade.vis.database.ThematicDataItem;
import spade.vis.datastat.CombinationCounter;
import spade.vis.datastat.CombinationInfo;
import spade.vis.geometry.GeomSign;
import spade.vis.geometry.ImageSymbol;
import spade.vis.preference.IconCorrespondence;
import spade.vis.preference.IconVisSpec;
import spade.vis.preference.SelectIconsPanel;

/**
* A Visualizer that represents values of one or more qualitative attributes
* by icons
* * changes: hdz, 2004.004 applicable for few numeric values
*/
public class IconPresenter extends DataPresenter implements PropertyChangeListener {
	/**
	* This Vector contains for each attribute an array with all its values
	* (without repetitions)
	*/
	static ResourceBundle res = Language.getTextResource("spade.vis.mapvis.Res");
	protected Vector attrVal = null;
	/**
	* Used for computing combination statistics
	*/
	protected CombinationCounter cCount = null;
	/**
	* Current icon correspondence
	*/
	protected IconCorrespondence icorr = null;

	/**
	* This method informs whether the Visualizer produces diagrams.
	* This is important for defining the order of drawing of GeoLayers on the
	* map: the diagrams should be drawn on top of all geography.
	*/
	@Override
	public boolean isDiagramPresentation() {
		return true;
	}

	public int maxIconSize = 0;//pixels  - scaling of icons

	/**
	 * Sets the maximum size of the icon in millimetres
	 */
	public void setMaxIconSizemm(int mms) {
		if (mms > 1) {
			maxIconSize = Math.round(mms * java.awt.Toolkit.getDefaultToolkit().getScreenResolution() / 25.33f);
		}
	}

	/**
	* Informs whether this visualizer allows the visualized attributes to
	* be transformed. This visualizer returns false.
	*/
	@Override
	public boolean getAllowTransform() {
		return false;
	}

	/**
	* Checks if this visualization method is applicable to the given set of
	* attributes. May use the DataInformer to check types and values
	* of the attributes.
	* hdz, extended parameter testNumerical for qual. Vis. of few integers
	**/
	@Override
	public boolean isApplicable(Vector attr) {
		err = null;
		if (attr == null || attr.size() < 1) {
			err = errors[0];
			return false;
		}
		if (table == null) {
			err = errors[1];
			return false;
		}
		boolean testNumerical = table.isValuesCountBelow(attr, Helper.getMaxNumValuesForQualitativeVis());

		for (int i = 0; i < attr.size(); i++) {
			char at = table.getAttributeType((String) attr.elementAt(i));
			if (!((testNumerical && AttributeTypes.isNumericType(at)) || (AttributeTypes.isNominalType(at)))) {
				err = attr.elementAt(i) + ": " + errors[10] + " (" + String.valueOf(at) + ")";
				return false;
			}
		}
		for (int i = 0; i < attr.size(); i++) {
			String id = (String) attr.elementAt(i);
			Vector values = (subAttr == null || subAttr.size() <= i || subAttr.elementAt(i) == null) ? table.getAllAttrValuesAsStrings(id) : table.getAllAttrValuesAsStrings((Vector) subAttr.elementAt(i));
			if (values == null) {
				err = id + ": " + errors[3];
				return false;
			}
		}
		return true;
	}

	/**
	* Must check semantic applicability of this visualization method to the
	* attributes previously set in the visualizer.
	*/
	@Override
	public boolean checkSemantics() {
		return true;
	}

	public void setCorrespondence(IconCorrespondence ic) {
		icorr = ic;
	}

	protected IconCorrespondence getIconCorrespondence() {
		if (table == null || attr == null || attr.size() < 1)
			return null;
		if (!(table instanceof DataTable))
			return null;
		SemanticsManager sm = ((DataTable) table).getSemanticsManager();
		if (sm == null || sm.getPreferenceCount() < 1)
			return null;
		for (int i = 0; i < sm.getPreferenceCount(); i++) {
			Object obj = sm.getPreference(i);
			if (obj == null || !(obj instanceof IconCorrespondence)) {
				continue;
			}
			IconCorrespondence icorr = (IconCorrespondence) obj;
			if (icorr.isRelevant(attr))
				return icorr;
		}
		return null;
	}

	/**
	* The Visualizer sets its parameters. For this purpose it may need
	* data statistics. Statistics are received from a StatisticsProvider.
	* A reference to a StatisticsProvider may be received from the DataInformer.
	* hdz, extended for qualitative visualisation for a small range of integers
	*/
	@Override
	public void setup() {

		if (!isApplicable(attr))
			return;
		getStatistics();
		if (icorr == null) {
			icorr = getIconCorrespondence();
			if (icorr == null) {
				icorr = makeDefaultIconCorrespondence();
			} else {
				//check if there is a correspondence for each value or combination
				//if not, add such a correspondence with a default sign
				checkCorrespondenceCompleteness();
			}
			//Start listening to data changes. If new values or combinations
			//will appear, the icon-value correspondence must be completed.
			table.addPropertyChangeListener(this);
		}
	}

	/**
	* Gets value statistics from the statistic provider
	*/
	protected void getStatistics() {
		if (table == null)
			return;
		if (attrVal == null) {
			attrVal = new Vector(attr.size(), 1);
		} else {
			attrVal.removeAllElements();
		}
		for (int i = 0; i < attr.size(); i++) {
			Vector values = (subAttr == null || subAttr.size() <= i || subAttr.elementAt(i) == null) ? table.getAllAttrValuesAsStrings((String) attr.elementAt(i)) : table.getAllAttrValuesAsStrings((Vector) subAttr.elementAt(i));
			String vals[] = new String[values.size()];
			for (int j = 0; j < vals.length; j++) {
				vals[j] = (String) values.elementAt(j);
			}
			attrVal.addElement(vals);
		}
		cCount = new CombinationCounter();
		int ntimes = 1;
		if (subAttr != null) {
			for (int i = 0; i < subAttr.size(); i++) {
				Vector v = (Vector) subAttr.elementAt(i);
				if (v != null && v.size() > ntimes) {
					ntimes = v.size();
				}
			}
		}
		for (int i = 0; i < table.getDataItemCount(); i++) {
			ThematicDataItem item = (ThematicDataItem) table.getDataItem(i);
			for (int j = 0; j < ntimes; j++) {
				cCount.combinationStart();
				for (int k = 0; k < attr.size(); k++) {
					int colN = -1;
					if (subAttr == null || subAttr.size() <= k || subAttr.elementAt(k) == null) {
						colN = table.getAttrIndex((String) attr.elementAt(k));
					} else {
						Vector v = (Vector) subAttr.elementAt(k);
						int n = j;
						if (n >= v.size()) {
							n = v.size() - 1;
						}
						colN = table.getAttrIndex((String) v.elementAt(n));
					}
					String val = item.getAttrValueAsString(colN);
					cCount.registerValue(val);
				}
				cCount.combinationEnd();
			}
		}
	}

	/**
	* Creates a default icon-value correspondence.
	* hdz, 2004.04 added sorting of numerical attributes
	*/
	protected IconCorrespondence makeDefaultIconCorrespondence() {
		IconCorrespondence ic = new IconCorrespondence();
		ic.setAttributes(attr);
		int nvals[] = new int[attr.size()];
		int nValsTotal = 1;
		for (int i = 0; i < attr.size(); i++) {
			String vals[] = (String[]) attrVal.elementAt(i);
			// hdz added sorting of values if they are integers
			char at = table.getAttributeType((String) attr.elementAt(i));
			if (AttributeTypes.isNumericType(at)) {
				QSortAlgorithm.sort_as_number(vals);
			}
			//end hdz
			nvals[i] = vals.length;
			nValsTotal *= nvals[i];
		}
		Vector signs = GeomSign.generateSigns(nvals);
		if (signs == null)
			return null;
		int nattr = attr.size();
		if (nattr > 6) {
			nattr = 6;
		}
		for (int i = 0; i < nValsTotal && i < signs.size(); i++) {
			IconVisSpec isp = new IconVisSpec();
			isp.setIcon(signs.elementAt(i));
			int num = i;
			String values[] = new String[nattr];
			for (int j = nattr - 1; j >= 0; j--) {
				String vals[] = (String[]) attrVal.elementAt(j);
				int valIdx = num % vals.length;
				values[j] = vals[valIdx];
				num = num / vals.length;
			}
			for (int j = 0; j < nattr; j++) {
				isp.addAttrValuePair((String) attr.elementAt(j), values[j]);
			}
			ic.addCorrespondence(isp);
		}
		return ic;
	}

	/**
	* Checks if there is an icon correspondence for each value or value
	* combination. If not, creates such correspondences with default signs.
	* Returns true if some correspondences have been added.
	*/
	protected boolean checkCorrespondenceCompleteness() {
		if (icorr == null)
			return true;
		int nOldCorr = icorr.getCorrCount();
		if (attr.size() == 1) {
			String vals[] = (String[]) attrVal.elementAt(0);
			String pair[] = { (String) attr.elementAt(0), null };
			Vector pairs = new Vector(1, 1);
			pairs.addElement(pair);
			for (String val : vals) {
				pair[1] = val;
				if (icorr.findCorrespondence(pairs) < 0) {
					IconVisSpec isp = new IconVisSpec();
					isp.addAttrValuePair(pair[0], pair[1]);
					icorr.addCorrespondence(isp);
				}
			}
		} else {
			Vector combInfo = cCount.getCombinationInfos();
			if (combInfo != null) {
				for (int i = 0; i < combInfo.size(); i++) {
					CombinationInfo cin = (CombinationInfo) combInfo.elementAt(i);
					Vector values = cin.getValues();
					if (icorr.findCorrespondence(attr, values) < 0) {
						IconVisSpec isp = new IconVisSpec();
						for (int j = 0; j < attr.size(); j++) {
							isp.addAttrValuePair((String) attr.elementAt(j), (String) values.elementAt(j));
						}
						icorr.addCorrespondence(isp);
					}
				}
			}
		}
		int diff = icorr.getCorrCount() - nOldCorr;
		if (diff <= 0)
			return false;
		//some values or combinations added
		int nShapes = GeomSign.SHAPE_LAST - GeomSign.SHAPE_FIRST;
		for (int i = nOldCorr; i < icorr.getCorrCount(); i++) {
			GeomSign gs = new GeomSign();
			gs.setShape(GeomSign.SHAPE_FIRST + (int) Math.round(Math.random() * nShapes));
			gs.setFillColor(Color.getHSBColor((float) Math.random(), (float) Math.random(), 0.9f));
			IconVisSpec isp = icorr.getCorrespondence(i);
			isp.setIcon(gs);
		}
		return true;
	}

	/**
	* This method is called by a GeoObject. The GeoObject sends its DataItem
	* to the Visualizer, and the Visualizer returns an Object specifying the
	* presentation of data contained in this DataItem. This may be, in
	* particular, Color or Diagram. Typically the argument DataItem is
	* a ThematicDataItem, although in some cases (e.g. manual classification)
	* this is not required.
	*/
	@Override
	public Object getPresentation(ThematicDataItem dit) {
		if (dit == null || icorr == null || attr == null || icorr.getCorrCount() < 1)
			return null;
		Vector values = new Vector(attr.size(), 1);
		for (int i = 0; i < attr.size(); i++) {
			values.addElement(getAttrValue(dit, i));
		}
		IconVisSpec isp = icorr.getClosestCorrespondence(attr, values);
		if (isp == null)
			return null;
		if (isp.getIcon() == null && isp.getPathToImage() != null) {
			isp.loadImage(CManager.getAnyFrame());
		}
		Object ic = isp.getIcon();
		if ((ic instanceof Image) && (maxIconSize > 0 || !Float.isNaN(isp.getScaleFactor()) || (isp.getFrameWidth() > 0 && isp.getFrameColor() != null))) {
			ImageSymbol ims = new ImageSymbol();
			ims.setImage((Image) isp.getIcon());
			if (!Float.isNaN(isp.getScaleFactor())) {
				ims.setScaleFactor(isp.getScaleFactor());
			} else {
				int w = ims.getWidth(), h = ims.getHeight();
				if (maxIconSize > 0 && (w > maxIconSize || h > maxIconSize)) {
					ims.setScaleFactor(1.0f * maxIconSize / Math.max(w, h));
				}
			}
			if (isp.getFrameWidth() > 0 && isp.getFrameColor() != null) {
				ims.setIsFramed(true);
				ims.setFrameWidth(isp.getFrameWidth());
				ims.setFrameColor(isp.getFrameColor());
				//System.out.println("Framed icon: width="+isp.getFrameWidth());
			}
			return ims;
		}
		return ic;
	}

	/**
	* Sorts the combinations according to the order of attributes and values
	*/
	protected void sortCombinations(Vector comb) {
		if (comb == null || comb.size() < 2)
			return;
		for (int i = 0; i < comb.size() - 1; i++)
			if (compareCombinations((CombinationInfo) comb.elementAt(i), (CombinationInfo) comb.elementAt(i + 1)) > 0) {
				CombinationInfo c = (CombinationInfo) comb.elementAt(i + 1);
				int pos = i;
				for (int j = i - 1; j >= 0; j--)
					if (compareCombinations((CombinationInfo) comb.elementAt(j), c) > 0) {
						pos = j;
					} else {
						break;
					}
				comb.removeElementAt(i + 1);
				comb.insertElementAt(c, pos);
			}
	}

	/**
	* Replies which of the two combinations must come earlier. Returns -1 if the
	* first is earlier, 1 if the second is earlier, and 0 if the order is irrelevant.
	*/
	protected int compareCombinations(CombinationInfo c1, CombinationInfo c2) {
		if (c1 == null || c2 == null)
			return 0;
		for (int i = 0; i < attr.size(); i++) {
			String v1 = c1.getValue(i), v2 = c2.getValue(i);
			if (v1 == null)
				if (v2 == null) {
					continue;
				} else
					return 1;
			else if (v2 == null)
				return -1;
			if (v1.equalsIgnoreCase(v2)) {
				continue;
			}
			String vals[] = (String[]) attrVal.elementAt(i);
			for (String val : vals)
				if (v1.equalsIgnoreCase(val))
					return -1;
				else if (v2.equalsIgnoreCase(val))
					return 1;
		}
		return 0;
	}

	/**
	* Draws the part of the legend explaining this specific presentation method.
	*/
	@Override
	public Rectangle drawMethodSpecificLegend(Graphics g, int startY, int leftmarg, int prefW) {
		if (icorr == null)
			return new Rectangle(leftmarg, startY, 20, 10);
		int y = startY, maxW = 0;
		//get statistics about value combinations
		if (cCount == null) {
			getStatistics();
		}
		if (cCount == null)
			return new Rectangle(leftmarg, startY, 20, 10);
		Vector combInfo = cCount.getCombinationInfos();
		if (combInfo == null)
			return new Rectangle(leftmarg, startY, 20, 10);

		int nObjTotal = 0;
		for (int i = 0; i < combInfo.size(); i++) {
			CombinationInfo ci = (CombinationInfo) combInfo.elementAt(i);
			nObjTotal += ci.getCount();
		}
		if (nObjTotal > 0) {
			g.setColor(Color.black);
			// following string: "Total "+nObjTotal+" objects"
			Point p = StringInRectangle.drawText(g, res.getString("Total") + nObjTotal + res.getString("objects"), leftmarg, y, prefW, false);
			if (p.x - leftmarg > maxW) {
				maxW = p.x - leftmarg;
			}
			y = p.y + 5;
		}
		if (attr.size() == 1) {
			String name = (String) ((attrNames == null) ? attr.elementAt(0) : attrNames.elementAt(0));
			g.setColor(Color.black);
			Point p = StringInRectangle.drawText(g, name, leftmarg, y, prefW, false);
			if (p.x - leftmarg > maxW) {
				maxW = p.x - leftmarg;
			}
			y = p.y + 5;
			String vals[] = (String[]) attrVal.elementAt(0);
			Vector values = new Vector(1, 1);
			for (String val : vals) {
				values.removeAllElements();
				values.addElement(val);
				int nOccur = 0;
				CombinationInfo cInfo = cCount.findCombination(values);
				if (cInfo != null) {
					nOccur = cInfo.getCount();
				}
				int iconW = 24, iconH = 24;
				IconVisSpec isp = icorr.getClosestCorrespondence(attr, values);
				if (isp != null) {
					if (isp.getIcon() == null && isp.getPathToImage() != null) {
						isp.loadImage(CManager.getAnyFrame());
					}
					if (isp.getIcon() != null) {
						iconW = isp.getIconWidth();
						iconH = isp.getIconHeight();
						if (iconW < 24) {
							iconW = 24;
						}
						if (iconH < 24) {
							iconH = 24;
						}
						drawIcon(isp.getIcon(), g, leftmarg, y, iconW, iconH, isp.getFrameWidth(), isp.getFrameColor());
					}
				}
				String txt = val;
				if (nOccur > 0 && nObjTotal > 0) {
					// following string: " objects, "
					txt += " (" + nOccur + res.getString("objects_") + Math.round(100.0f * nOccur / nObjTotal) + "%)";
				}
				g.setColor(Color.black);
				p = StringInRectangle.drawText(g, txt, leftmarg + iconW + 5, y, prefW - iconW - 5, false);
				if (p.x - leftmarg > maxW) {
					maxW = p.x - leftmarg;
				}
				y += iconH + 2;
				if (p.y > y) {
					y = p.y + 2;
				}
			}
		} else {
			if (combInfo != null && combInfo.size() > 0) {
				sortCombinations(combInfo);
				for (int i = 0; i < combInfo.size(); i++) {
					CombinationInfo cin = (CombinationInfo) combInfo.elementAt(i);
					Vector values = new Vector(attr.size(), 1);
					for (int j = 0; j < attr.size(); j++) {
						values.addElement(cin.getValue(j));
					}
					int iconW = 24, iconH = 24;
					IconVisSpec isp = icorr.getClosestCorrespondence(attr, values);
					if (isp != null) {
						if (isp.getIcon() == null && isp.getPathToImage() != null) {
							isp.loadImage(CManager.getAnyFrame());
						}
						if (isp.getIcon() != null) {
							iconW = isp.getIconWidth();
							iconH = isp.getIconHeight();
							if (iconW < 24) {
								iconW = 24;
							}
							if (iconH < 24) {
								iconH = 24;
							}
							drawIcon(isp.getIcon(), g, leftmarg, y, iconW, iconH, isp.getFrameWidth(), isp.getFrameColor());
						}
					}
					int y0 = y;
					g.setColor(Color.black);
					for (int j = 0; j < attr.size(); j++) {
						String txt = null;
						if (attrNames != null) {
							txt = (String) attrNames.elementAt(j);
						} else {
							txt = (String) attr.elementAt(j);
						}
						txt += " = ";
						String val = cin.getValue(j);
						if (val == null) {
							txt += "<any value>";
						} else {
							txt += val;
						}
						Point p = StringInRectangle.drawText(g, txt, leftmarg + iconW + 5, y, prefW - iconW - 5, false);
						if (p.x - leftmarg > maxW) {
							maxW = p.x - leftmarg;
						}
						y += iconH + 2;
						if (p.y > y) {
							y = p.y + 2;
						}
					}
					if (nObjTotal > 0) {
						// following string: " objects, "
						String txt = " (" + cin.getCount() + res.getString("objects_") + Math.round(100.0f * cin.getCount() / nObjTotal) + "%)";
						Point p = StringInRectangle.drawText(g, txt, leftmarg + iconW, y, prefW - iconW, false);
						if (p.x - leftmarg > maxW) {
							maxW = p.x - leftmarg;
						}
						y = p.y + 2;
					}
					if (y < y0 + iconH) {
						y = y0 + iconH;
					}
				}
			} else {
				for (int i = 0; i < icorr.getCorrCount(); i++) {
					int iconW = 24, iconH = 24;
					IconVisSpec isp = icorr.getCorrespondence(i);
					if (isp != null) {
						if (isp.getIcon() == null && isp.getPathToImage() != null) {
							isp.loadImage(CManager.getAnyFrame());
						}
						if (isp.getIcon() != null) {
							iconW = isp.getIconWidth();
							iconH = isp.getIconHeight();
							if (iconW < 24) {
								iconW = 24;
							}
							if (iconH < 24) {
								iconH = 24;
							}
							drawIcon(isp.getIcon(), g, leftmarg, y, iconW, iconH, isp.getFrameWidth(), isp.getFrameColor());
						}
					}
					int y0 = y;
					g.setColor(Color.black);
					for (int j = 0; j < attr.size(); j++) {
						String txt = null;
						if (attrNames != null) {
							txt = (String) attrNames.elementAt(j);
						} else {
							txt = (String) attr.elementAt(j);
						}
						txt += " = ";
						String val = isp.getAttrValue((String) attr.elementAt(j));
						if (val == null) {
							txt += "<any value>";
						} else {
							txt += val;
						}
						Point p = StringInRectangle.drawText(g, txt, leftmarg + iconW + 5, y, prefW - iconW - 5, false);
						if (p.x - leftmarg > maxW) {
							maxW = p.x - leftmarg;
						}
						y = p.y + 2;
					}
					if (y < y0 + iconH) {
						y = y0 + iconH;
					}
				}
			}
		}
		return new Rectangle(leftmarg, startY, maxW, y - startY);
	}

	/**
	* Draws a small picture symbolizing this presentation technique
	*/
	@Override
	public void drawIcon(Graphics g, int x, int y, int w, int h) {
		Object icon = null;
		int frameW = 0;
		Color frameColor = null;
		if (icorr != null && icorr.getCorrCount() > 0) {
			IconVisSpec isp = icorr.getDefaultCorrespondence();
			if (isp != null) {
				icon = isp.getIcon();
				frameW = isp.getFrameWidth();
				frameColor = isp.getFrameColor();
			}
			for (int i = 0; i < icorr.getCorrCount() && icon == null; i++) {
				isp = icorr.getCorrespondence(i);
				icon = isp.getIcon();
				frameW = isp.getFrameWidth();
				frameColor = isp.getFrameColor();
			}
		}
		drawIcon(icon, g, x, y, w, h, frameW, frameColor);
	}

	/**
	* Draws the specified icon within the specified rectangle
	*/
	public void drawIcon(Object icon, Graphics g, int x, int y, int w, int h, int frameW, Color frameColor) {
		if (icon == null) {
			g.setColor(Color.black);
			FontMetrics fm = g.getFontMetrics();
			int asc = fm.getAscent();
			g.drawString("?", x + 5, y + asc + 1);
		} else if (icon instanceof Image) {
			w -= 2 * frameW;
			h -= 2 * frameW;
			Image img = (Image) icon;
			int width = img.getWidth(null), height = img.getHeight(null);
			float scaleFactor = 1.0f;
			if (width > w) {
				scaleFactor = 1.0f * w / width;
			}
			if (height > h) {
				float sc = 1.0f * h / height;
				if (sc < scaleFactor) {
					scaleFactor = sc;
				}
			}
			if (maxIconSize > 0 && (w > maxIconSize || h > maxIconSize)) {
				float sc = 1.0f * maxIconSize / Math.max(w, h);
				if (sc < scaleFactor) {
					scaleFactor = sc;
				}
			}
			if (scaleFactor < 1.0f) {
				width = Math.round(scaleFactor * width);
				height = Math.round(scaleFactor * height);
			}
			int x0 = x + frameW + (w - width) / 2, y0 = y + frameW + (h - height) / 2;
			g.drawImage(img, x0, y0, width, height, null);
			if (frameW > 0 && frameColor != null) {
				g.setColor(frameColor);
				--width;
				--height;
				for (int i = 0; i < frameW; i++) {
					--x0;
					--y0;
					width += 2;
					height += 2;
					g.drawRect(x0, y0, width, height);
				}
			}
		} else if (icon instanceof GeomSign) {
			((GeomSign) icon).draw(g, x, y, w, h);
		}
	}

	/**
	* Replies whether any parameters of this visualization method may be
	* changed, e.g. colors or sizes of signs. This does not include interactive
	* analytical manipulation.
	*/
	@Override
	public boolean canChangeParameters() {
		return true;
	}

	/**
	* Constructs and displays a dialog for changing parameters of this
	* visualization method (if possible). Changing parameters (colors or sizes
	* of signs) is different from interactive analytical manipulation!
	*/
	@Override
	public void startChangeParameters() {
		if (attrVal == null)
			return;
		SelectIconsPanel span = new SelectIconsPanel(icorr, attr, (attrNames == null) ? attr : attrNames, attrVal);
		OKDialog okd = new OKDialog(CManager.getAnyFrame(),
		// following string: "Set icons for data visualization"
				res.getString("Set_icons_for_data"), false);
		okd.addContent(span);
		okd.show();
		if (okd.wasCancelled())
			return;
		icorr = span.getCorrespondence();
		notifyVisChange();
		if (table == null || !(table instanceof DataTable))
			return;
		SemanticsManager sm = ((DataTable) table).getSemanticsManager();
		if (sm == null)
			return;
		for (int i = 0; i < sm.getPreferenceCount(); i++) {
			Object obj = sm.getPreference(i);
			if (obj == null || !(obj instanceof IconCorrespondence)) {
				continue;
			}
			IconCorrespondence corr = (IconCorrespondence) obj;
			if (corr.isRelevant(icorr.getAttributes())) {
				sm.removePreference(i);
				break;
			}
		}
		sm.addPreference(icorr);
	}

	/**
	* Reacts to statistic change events coming from the statistics provider.
	* If new attribute values or combinations appeared, and there are no icons
	* for them, creates some defaul icons and notiies about the change of the
	* visualization.
	*/
	@Override
	public void propertyChange(PropertyChangeEvent e) {
		if (e.getSource().equals(table) && (e.getPropertyName().equals("data_added") || e.getPropertyName().equals("data_removed") || e.getPropertyName().equals("data_updated") || e.getPropertyName().equals("values"))) {
			if (e.getPropertyName().equals("values")) {
				Vector chAttr = (Vector) e.getNewValue();
				if (chAttr == null || chAttr.size() < 1)
					return;
				boolean changed = false;
				for (int i = 0; i < chAttr.size() && !changed; i++) {
					for (int j = 0; j < attr.size() && !changed; j++) {
						changed = StringUtil.isStringInVectorIgnoreCase((String) attr.elementAt(j), chAttr);
						if (!changed && subAttr != null && subAttr.elementAt(j) != null) {
							Vector v = (Vector) subAttr.elementAt(j);
							for (int k = 0; k < v.size() && !changed; k++) {
								changed = StringUtil.isStringInVectorIgnoreCase((String) v.elementAt(k), chAttr);
							}
						}
					}
				}
				if (!changed)
					return;
			}
			getStatistics();
			if (icorr == null) {
				icorr = makeDefaultIconCorrespondence();
				notifyVisChange();
			} else if (checkCorrespondenceCompleteness()) {
				notifyVisChange();
			}
		}
	}

}
