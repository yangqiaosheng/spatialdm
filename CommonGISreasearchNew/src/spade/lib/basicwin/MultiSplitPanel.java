package spade.lib.basicwin;

import java.awt.Component;
import java.util.Vector;

/*
* MiltiSplitPanel is complex split layout panel with matrix structure.
* It takes into account maximal number of elements per row (by default 3).
* Component is added to current row if total number of elements
* in this row is lower them maximum per row. If it is not a case -
* new row is added and the component is added to this next row.
*/

public class MultiSplitPanel extends SplitPanel {
	Vector rows = null;
	int maxCompPerRow = 3;
	int total = 0;
	SplitPanel spCurrent = null;

	public MultiSplitPanel(int maxNComponentsPerRow) {
		super(false);
		maxCompPerRow = maxNComponentsPerRow;
		rows = new Vector(maxCompPerRow, 2);
	}

	public void addComponent(Component c) {
		this.addComponent(c, 1.0f);
	}

	public void addComponentAt(Component c, int idx) {
		this.addComponentAt(c, 1.0f, idx);
	}

	public void addComponent(Component c, float part) {
		if (total % maxCompPerRow == 0 || spCurrent == null) {
			SplitPanel sp = new SplitPanel(true);
			addSplitComponent(sp);
			rows.addElement(sp);
			spCurrent = sp;
		}
		spCurrent.spl.addComponent(c, part);
		++total;
	}

	public void addComponentAt(Component c, float part, int idx) {
		if (total % maxCompPerRow == 0 || spCurrent == null) {
			SplitPanel sp = new SplitPanel(true);
			addSplitComponent(sp);
			rows.addElement(sp);
			spCurrent = sp;
		}
		spCurrent.spl.addComponentAt(c, part, idx);
		++total;
	}

	public int getRowN(int compIdx) {
		if (compIdx < 0 || compIdx >= total)
			return -1;
		return compIdx / maxCompPerRow;
	}

	public SplitPanel getRow(int rowIdx) {
		if (rowIdx < 0 || rowIdx >= rows.size())
			return null;
//ID
// bug fix, but probably an introduction of another...
//    return (SplitPanel)rows.elementAt(rowIdx);
		return (SplitPanel) super.getSplitComponent(rowIdx);
//~ID
	}

	@Override
	public void replaceSplitComponent(Component c, int idx) {
		if (c == null)
			return;
		if (idx < 0 || idx >= total)
			return;
		SplitPanel spTarget = getRow(getRowN(idx));
		if (spTarget != null) {
			spTarget.remove(idx * 2);
			spTarget.add(c, idx * 2);
			spTarget.invalidate();
			spTarget.validate();
		}
	}

	@Override
	public void swapSplitComponents(int i, int j) {
		if ((i < 0 || i >= getSplitComponentCount()) || (j < 0 || j >= getSplitComponentCount()))
			return;

		Component c1 = getSplitComponent(i), c2 = getSplitComponent(j);
		if (c1 == null || c2 == null) {
			System.out.println("Cannot replace components " + i + " and " + j);
			return;
		}
		replaceSplitComponent(c2, i);
		replaceSplitComponent(c1, j);
	}

	public void removeComponent(Component c) {
		if (c == null) {
			System.out.println("MultiSplitPanel:: cannot remove NULL component");
			return;
		}
		System.out.println("MultiSplitPanel:: try to remove component " + c.getName());
		this.removeComponent(getComponentIndex(c));
	}

	public void removeComponent(int idx) {
		if (idx < 0 || idx >= total)
			return;
		int rowN = getRowN(idx);
		SplitPanel spTarget = getRow(rowN);
		if (spTarget != null) {
			spTarget.spl.removeComponent(idx % maxCompPerRow);
			total--;
			if (spTarget.getSplitComponentCount() < 1) {
				rows.removeElementAt(rowN);
				removeSplitComponent(rowN);
				if (rows.size() > 0) {
					spCurrent = (SplitPanel) rows.elementAt(rows.size() - 1);
				} else {
					spCurrent = null;
				}
			}
		}
	}

	@Override
	public Component getSplitComponent(int idx) {
		if (idx < 0 || idx >= total)
			return null;
		SplitPanel spTarget = getRow(getRowN(idx));
		if (spTarget == null)
			return null;
		int cCount = spTarget.getSplitComponentCount();
		return spTarget.spl.getComponent(idx % maxCompPerRow);
	}

	@Override
	public int getComponentIndex(Component c) {
		if (c != null) {
			for (int i = 0; i < total; i++)
				if (c.equals(getSplitComponent(i)))
					return i;
		}
		return -1;
	}

	/*
	public int getComponentIndexInRow (Component c) {
	  if (c!=null)
	    for (int i=0; i<total; i++)
	      if (c.equals(getSplitComponent(i))) return i%maxCompPerRow;
	  return -1;
	}
	*/
	@Override
	public int getSplitComponentCount() {
		return total;
	}

//ID
	public int getMaxCompPerRow() {
		return maxCompPerRow;
	}
//~ID
}
