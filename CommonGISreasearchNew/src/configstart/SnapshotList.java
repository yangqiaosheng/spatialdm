package configstart;

import java.util.Vector;

public class SnapshotList {
	public Vector snapshots = new Vector();

	public Snapshot[] getList() {
		Snapshot[] arr = new Snapshot[snapshots.size()];
		for (int i = 0; i < snapshots.size(); i++) {
			arr[i] = (Snapshot) snapshots.elementAt(i);
		}
		return arr;
	}

	public void setList(Snapshot[] list) {
		snapshots.removeAllElements();
		if (list == null)
			return;
		for (Snapshot element : list) {
			snapshots.addElement(element);
		}
	}
}
