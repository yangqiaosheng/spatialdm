/**
 * 
 */
package spade.analysis.tools.movement;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import weka.core.Instance;

/**
 * @author Admin
 *
 */
public class MovementReaderList implements MovementReader {

	List<Double[]> data = new ArrayList<Double[]>();
	int currentPosition = -1;
	int cIndex = 3;
	int iIndex = 3;
	int tIndex = 2;

	@Override
	public Instance getNextInstanceOfCluster(int cId) {
		return null;
	}

	@Override
	public void loadData(String path) {

		try {

			BufferedReader reader = new BufferedReader(new FileReader(path));
			String line = "";
			String[] token = null;
			Double[] values = null;
			int idx = 0;
			// skip first line
			// reader.readLine();
			while ((line = reader.readLine()) != null) {
				token = line.split(",");
				values = new Double[token.length];
				for (int i = 0; i < token.length; i++) {
					values[i] = Double.parseDouble(token[i]);
				}
				data.add(values);
			}

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

	/* (non-Javadoc)
	 * @see spade.analysis.tools.movement.MovementReader#getClusterID()
	 */
	@Override
	public int getClusterID() {
		// TODO Auto-generated method stub
		return data.get(currentPosition)[cIndex].intValue();
	}

	/* (non-Javadoc)
	 * @see spade.analysis.tools.movement.MovementReader#getID()
	 */
	@Override
	public int getID() {
		// TODO Auto-generated method stub
		return data.get(currentPosition)[iIndex].intValue();
	}

	/* (non-Javadoc)
	 * @see spade.analysis.tools.movement.MovementReader#getPosition()
	 */
	@Override
	public int getPosition() {
		// TODO Auto-generated method stub
		return currentPosition;
	}

	/* (non-Javadoc)
	 * @see spade.analysis.tools.movement.MovementReader#getTime()
	 */
	@Override
	public long getTime() {
		// TODO Auto-generated method stub
		return data.get(currentPosition)[tIndex].intValue();
	}

	/* (non-Javadoc)
	 * @see spade.analysis.tools.movement.MovementReader#moveToNext()
	 */
	@Override
	public boolean moveToNext() {
		if (currentPosition == data.size())
			return false;
		currentPosition += 1;
		return true;
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see spade.analysis.tools.movement.MovementReader#setPosition(long)
	 */
	@Override
	public void setPosition(long p) {
		currentPosition = (int) p;
		// TODO Auto-generated method stub

	}

	@Override
	public Instance getNextInstance() {
		// TODO Auto-generated method stub
		return null;
	}

}
