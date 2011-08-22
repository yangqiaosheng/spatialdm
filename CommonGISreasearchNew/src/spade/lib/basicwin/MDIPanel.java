package spade.lib.basicwin;

import java.awt.Component;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class MDIPanel extends SplitPanel implements ActionListener {

	protected SplitPanel stackedFrame = null;
	protected String mainCID = null; // ID of the component in the main frame
	protected int NWindows = 0;
	protected boolean isStackHorisontal = false;
	protected boolean isStackInFront = false;
	protected boolean equalizeComponentsInStack = false;

	public static final String cmdExpand = Header.cmdExpand, cmdClose = Header.cmdClose;

	public MDIPanel() {
		super(true);
	}

	public MDIPanel(boolean isStackHorisontal) {
		super(!isStackHorisontal);
		this.isStackHorisontal = isStackHorisontal;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		//System.out.println("actionPerformed: "+cmd);
		if (cmd.length() < 1)
			return; // Cannot handle without cmd
		int breakIdx = cmd.indexOf('-'); // index of break between cmd & src
		String cmd1 = cmd.substring(0, breakIdx); // actually command itself
		String sourceID = cmd.substring(breakIdx + 1); // ID of command's source

		ComponentWindow compWin = getMDIComponent(sourceID);
		if (compWin == null)
			//System.out.println("Cannot detect source of command "+cmd);
			return;
		if (cmd1.equalsIgnoreCase(cmdExpand)) {
			//System.out.println("Action: expand window "+sourceID);
			expandWindow(compWin);
			return;
		}
		if (cmd1.equalsIgnoreCase(cmdClose)) {
			//System.out.println("Action: close window "+sourceID);
			removeWindow(compWin);
			return;
		}
	}

	/**
	* Adds component to MDI Panel without header
	*/
	public void addComponent(Component comp) {
		if (comp == null)
			return;

		if (getSplitComponentCount() < 1) {
			addSplitComponent(comp);
		} else {
			if (stackedFrame == null) {
				stackedFrame = new SplitPanel(isStackHorisontal);
				int stackedFIndex = isStackInFront ? 0 : 1;
				addSplitComponentAt(stackedFrame, 0.8f, stackedFIndex);
			}
			int mainCIndex = getMainCompIndex();
			Component c = getSplitComponent(mainCIndex);
			replaceSplitComponent(comp, mainCIndex);
			stackedFrame.addSplitComponent(c, 2.0f / (1 + stackedFrame.getSplitComponentCount()));
			if (equalizeComponentsInStack) {
				stackedFrame.forceEqualizeParts();
				//System.out.println("Component "+mainCID+" has been added to the main frame");
			}
		}
		CManager.validateAll(comp);
		mainCID = String.valueOf(++NWindows);
	}

	/**
	* Adds component to MDI Panel with header
	*/
	public void addWindow(Component comp) {
		if (comp == null)
			return;
		ComponentWindow cwin = new ComponentWindow(comp, String.valueOf(++NWindows), this);

		if (getSplitComponentCount() < 1) {
			addSplitComponent(cwin);
		} else {
			if (stackedFrame == null) {
				stackedFrame = new SplitPanel(isStackHorisontal);
				int stackedFIndex = isStackInFront ? 0 : 1;
				addSplitComponentAt(stackedFrame, 0.8f, stackedFIndex);
			}
			int mainCIndex = getMainCompIndex();
			Component c = getSplitComponent(mainCIndex);
			replaceSplitComponent(cwin, mainCIndex);
			stackedFrame.addSplitComponent(c, 2.0f / (1 + stackedFrame.getSplitComponentCount()));
			if (equalizeComponentsInStack) {
				stackedFrame.forceEqualizeParts();
				//System.out.println("Component "+mainCID+" has been added to the main frame");
			}
		}
		CManager.validateAll(comp);
		mainCID = cwin.getComponentID();
	}

	public void removeWindow(ComponentWindow compWin) {
		if (compWin == null)
			return;
		String cID = compWin.getComponentID();
		if (cID == null || cID.length() < 1)
			return;
		if (stackedFrame == null || stackedFrame.getSplitComponentCount() < 1) {
			removeAll();
		} else {
			// we need to remove "main component" and replace its "place"
			// with the last component from stacked frame
			if (mainCID.equalsIgnoreCase(cID)) {
				int rIdx = stackedFrame.getSplitComponentCount() - 1;
				ComponentWindow cw = (ComponentWindow) stackedFrame.getSplitComponent(rIdx);
				stackedFrame.removeSplitComponent(rIdx);
				replaceSplitComponent(cw, getMainCompIndex());
				mainCID = cw.getComponentID();
			}
			// we need to remove component from stacked frame
			else {
				int ridx = stackedFrame.getComponentIndex(compWin);
				if (ridx < 0)
					return;
				stackedFrame.removeSplitComponent(ridx);
			}
			// after all removals "empty stacked frame" is possible
			if (stackedFrame.getSplitComponentCount() < 1) {
				removeSplitComponent(getStackedFrameIndex());
				stackedFrame = null;
			}
		}
		if (!CManager.isComponentDestroyed(compWin.getContent())) {
			CManager.destroyComponent(compWin.getContent());
		}
		if (isEmpty()) {
			Window fr = CManager.getWindow(this);
			if (fr != null) {
				fr.dispose();
			}
		} else {
			validate();
		}
	}

	public void removeComponent(Component comp) {
		if (comp == null)
			return;
		//System.out.println("Remove window "+cID);
		if (stackedFrame == null || stackedFrame.getSplitComponentCount() < 1) {
			removeAll();
		} else {
			// we need to remove "main component" and replace its "place"
			// with the last component from stacked frame
			Component mainC = getSplitComponent(getMainCompIndex());
			if (mainC.equals(comp)) {
				int rIdx = stackedFrame.getSplitComponentCount() - 1;
				Component c = stackedFrame.getSplitComponent(rIdx);
				stackedFrame.removeSplitComponent(rIdx);
				replaceSplitComponent(c, getMainCompIndex());
			}
			// we need to remove component from stacked frame
			else {
				int ridx = stackedFrame.getComponentIndex(comp);
				if (ridx < 0)
					return;
				stackedFrame.removeSplitComponent(ridx);
			}
			// after all removals "empty stacked frame" is possible
			if (stackedFrame.getSplitComponentCount() < 1) {
				removeSplitComponent(getStackedFrameIndex());
				stackedFrame = null;
			}
		}
		if (!CManager.isComponentDestroyed(comp)) {
			CManager.destroyComponent(comp);
		}
		if (isEmpty()) {
			Window fr = CManager.getWindow(this);
			if (fr != null) {
				fr.dispose();
			}
		} else {
			validate();
		}
	}

	public void expandWindow(ComponentWindow compWin) {
		if (compWin == null)
			return;
		String cID = compWin.getComponentID();
		if (cID == null || cID.length() < 1)
			return;
		//System.out.println("Expand window "+cID);
		if (cID.equalsIgnoreCase(mainCID)) {
			if (getSplitComponentCount() > 1) {
				spl.changePart(getStackedFrameIndex(), 0f);
			}
		} else {
			int idx = stackedFrame.getComponentIndex(compWin);
			if (idx < 0)
				return;
			stackedFrame.removeSplitComponent(idx);
			Component leftC = getSplitComponent(getMainCompIndex());
			replaceSplitComponent(compWin, getMainCompIndex());
			stackedFrame.addSplitComponent(leftC, 2.0f / (1 + stackedFrame.getSplitComponentCount()));
			if (equalizeComponentsInStack) {
				stackedFrame.forceEqualizeParts();
			}
			mainCID = compWin.getComponentID();
		}
		CManager.validateAll(compWin);
	}

	public ComponentWindow getMDIComponent(String id) {
		if (id != null && id.length() > 0) {
			if (getSplitComponentCount() < 1)
				return null;

			ComponentWindow cw = (ComponentWindow) getSplitComponent(getMainCompIndex());
			if (id.equalsIgnoreCase(cw.getComponentID()))
				return cw;
			if (stackedFrame == null)
				return null;
			for (int i = 0; i < stackedFrame.getSplitComponentCount(); i++) {
				cw = (ComponentWindow) stackedFrame.getSplitComponent(i);
				if (id.equalsIgnoreCase(cw.getComponentID()))
					return cw;
			}
		}
		return null;
	}

	/**
	* Removes subwindows that have been "destroyed", i.e. are no more valid
	*/
	public void removeDestroyedComponents() {
		//first remove destroyed components from stackedFrame
		if (stackedFrame != null) {
			for (int i = stackedFrame.getSplitComponentCount() - 1; i >= 0; i--)
				if (CManager.isComponentDestroyed(stackedFrame.getSplitComponent(i))) {
					stackedFrame.removeSplitComponent(i);
				}
			if (stackedFrame.getSplitComponentCount() < 1) {
				removeSplitComponent(isStackInFront ? 0 : 1);
				stackedFrame = null;
			}
		}
		//now check the "privileged" graph
		if (getSplitComponentCount() > 0 && CManager.isComponentDestroyed(getSplitComponent(0))) {
			removeWindow((ComponentWindow) getSplitComponent(0));
		}
		validate();
	}

	/**
	* Returns true if contains no components
	*/
	public boolean isEmpty() {
		return getSplitComponentCount() < 1;
	}

	public boolean getStackedFrameAlign() {
		return isStackInFront;
	}

	public int getMainCompIndex() {
		return (getSplitComponent(0) instanceof SplitPanel) ? 1 : 0;
	}

	public int getStackedFrameIndex() {
		int idx = (getSplitComponent(0) instanceof SplitPanel) ? 0 : 1;
		return (stackedFrame != null) ? idx : -1;
	}

	public void setStackedFrameInFront(boolean flag) {
		if (isStackInFront == flag)
			return;
		isStackInFront = flag;
		if (getSplitComponentCount() > 1) {
			swapSplitComponents(0, 1);
		}
	}

	public void setEqualizeComponentsInStack(boolean flag) {
		if (equalizeComponentsInStack == flag)
			return;
		equalizeComponentsInStack = flag;
		if (stackedFrame != null && getStackedFrameIndex() > -1) {
			stackedFrame.forceEqualizeParts();
		}
	}
}
