package spade.analysis.system;

import java.awt.Window;

/**
* Registers all windows that are created during a session. Closes all windows
* on exit.
*/
public interface WindowManager {
	public void registerWindow(Window win);

	public void closeAllWindows();

	public void setAllWindowsVisible(boolean visible);

	public int getWindowCount();

	public Window getWindow(int idx);

	public void showWindowList();
}
