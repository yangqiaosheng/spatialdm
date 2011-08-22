/**
 * 
 */
package spade.analysis.tools.movement;

import weka.core.Instance;

/**
 * @author Admin
 *
 */
public interface MovementReader {
	public Instance getNextInstance();

	public Instance getNextInstanceOfCluster(int cId);

	public int getPosition();

	public void setPosition(long p);

	public int getID();

	public int getClusterID();

	public long getTime();

	public boolean moveToNext();

	public boolean hasNext();

	public boolean moveToNextCluster(int cId);

	public void loadData(String path);
}
