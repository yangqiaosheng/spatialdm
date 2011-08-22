/**
 * 
 */
package spade.analysis.tools.movement;

import java.io.BufferedReader;
import java.io.FileReader;

import weka.core.Instance;
import weka.core.converters.ArffLoader;

/**
 * @author Admin
 *
 */
public class MovementReaderArray implements MovementReader {

	private double[][] data = null;

	@Override
	public void loadData(String path) {
		ArffLoader loader = new ArffLoader();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(path + "\\insts.csv"));
			String line = "";
			int idx = 0;
			// cIds = new int[10000000];

			// skip first line
			// reader.readLine();
			long size = 0;
			int attrNum = 0;
			while ((line = reader.readLine()) != null) {
				size++;
			}
			data = new double[(int) size][attrNum];

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Override
	public boolean hasNext() {
		return true;
	}

	@Override
	public boolean moveToNextCluster(int cId) {
		do {
			if (this.getClusterID() == cId)
				return true;
		} while (this.moveToNext());
		return false;
	}

	@Override
	public Instance getNextInstanceOfCluster(int cId) {
		return null;
	}

	/* (non-Javadoc)
	 * @see spade.analysis.tools.movement.MovementReader#getClusterID()
	 */
	@Override
	public int getClusterID() {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see spade.analysis.tools.movement.MovementReader#getID()
	 */
	@Override
	public int getID() {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see spade.analysis.tools.movement.MovementReader#getPosition()
	 */
	@Override
	public int getPosition() {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see spade.analysis.tools.movement.MovementReader#getTime()
	 */
	@Override
	public long getTime() {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see spade.analysis.tools.movement.MovementReader#moveToNext()
	 */
	@Override
	public boolean moveToNext() {
		// TODO Auto-generated method stub
		return true;
	}

	/* (non-Javadoc)
	 * @see spade.analysis.tools.movement.MovementReader#setPosition(long)
	 */
	@Override
	public void setPosition(long p) {
		// TODO Auto-generated method stub

	}

	@Override
	public Instance getNextInstance() {
		// TODO Auto-generated method stub
		return null;
	}

}
