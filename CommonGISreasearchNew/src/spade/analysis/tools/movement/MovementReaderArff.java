/**
 * 
 */
package spade.analysis.tools.movement;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.LinkedList;

import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffLoader;

/**
 * @author Admin
 * 
 */
public class MovementReaderArff implements MovementReader {

	private Instances insts = null;
	public LinkedList<Instance> T1 = new LinkedList<Instance>();
	public LinkedList<Instance> T2 = new LinkedList<Instance>();
	long T1first = 0, T1last = 0, T2first = 0, T2last = 0; // T2first = T1last +
	// 1
	int currentPosition = 0;
	Instance currentInst = null;
	int cIndex = 4;
	int iIndex = 3;
	int tIndex = 2;
	long numAccess = 0;

	BufferedWriter writer = null;

	public int lengthSW() {
		return T1.size() + T2.size();
	}

	public void writeStats() {
		try {
			if (writer == null) {
				writer = new BufferedWriter(new FileWriter(".//access.txt"));
			}
			// writer.write(numAccess + ", " + currentPosition + "\n");
			writer.write(currentPosition + "\n");
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}

	}

	@Override
	public void loadData(String path) {
		ArffLoader loader = new ArffLoader();
		try {

			loader.setFile(new File(path));
			loader.setSource(new File(path));
			insts = loader.getDataSet();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Override
	public boolean hasNext() {
		return (currentPosition < insts.numInstances() + 1);
	}

	@Override
	public Instance getNextInstanceOfCluster(int cId) {
		do {
			if (this.getClusterID() == cId)
				return this.getNextInstance();
		} while (this.moveToNext());
		return null;
	}

	public long eps = 0;

	protected long getTime(Instance inst) {
		return (long) inst.valueSparse(tIndex);
	}

	public void updateSWCore() {

	}

	public void clearSW() {
		T1.clear();
		T2.clear();
		currentInst = null;
	}

	public void updateSW() {
		if (currentInst == null) {
			currentInst = getNextInstance();
			return;
		}
		if (T2.size() == 0) {
			if (T1.size() == 0) {
				if (getTime(getNextInstance()) - getTime(currentInst) < eps) {
					T2.addLast(currentInst);
				}
				currentInst = getNextInstance();
			} else {
				T1.addLast(currentInst);
				// T1.removeFirst();
				currentInst = getNextInstance();
				while ((T1.size() > 0) && (getTime(currentInst) - getTime(T1.getFirst()) > eps)) {
					T1.removeFirst();
				}
				if (moveToNext() && getTime(getNextInstance()) - getTime(currentInst) < eps) {
					T2.addLast(getNextInstance());
				}
			}
		} else {
			Instance nextFromT2 = T2.getFirst();
			T2.removeFirst();
			if (T1.size() == 0) {
				if (getTime(nextFromT2) - getTime(currentInst) < eps) {
					T1.addLast(currentInst);
				}
				currentInst = nextFromT2;
				if (T2.size() > 0) {
					while (moveToNext() && getTime(getNextInstance()) - getTime(currentInst) < eps) {
						T2.addLast(getNextInstance());
					}
				}
			} else {
				T1.addLast(currentInst);
				currentInst = nextFromT2;
				while ((T1.size() > 0) && (getTime(currentInst) - getTime(T1.getFirst()) > eps)) {
					T1.removeFirst();
				}
				if (T2.size() > 0) {
					while (moveToNext() && getTime(getNextInstance()) - getTime(currentInst) < eps) {
						T2.addLast(getNextInstance());
					}
				}
			}
		}
	}

	public boolean expend() {
		T1.clear();
		currentInst = T2.getLast();
		T2.removeLast();
		T1.addAll(T2);
		T2.clear();
		//T2.addLast(currentInst);

		while ((moveToNext()) && (getTime(getNextInstance()) - getTime(currentInst) < eps)) {
			T2.addLast(getNextInstance());
		}
		//T2.removeFirst();
		if (!hasNext() || T2.size() == 0)
			return false;
		return true;
	}

	@Override
	public boolean moveToNextCluster(int cId) {
		while (this.moveToNext()) {
			if (this.getClusterID() == cId)
				return true;
		}
		;
		return false;
	}

	public Instances getInstances() {
		return insts;
	}

	@Override
	public Instance getNextInstance() {
		if (currentPosition > insts.numInstances())
			return null;
		if (currentPosition == -1) {
			int k = 0;
		}
		numAccess++;
		//writeStats();
		return insts.instance(currentPosition - 1);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see spade.analysis.tools.movement.MovementReader#getClusterID()
	 */
	@Override
	public int getClusterID() {
		// TODO Auto-generated method stub
		return (int) insts.instance(currentPosition - 1).valueSparse(cIndex);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see spade.analysis.tools.movement.MovementReader#getID()
	 */
	@Override
	public int getID() {
		// TODO Auto-generated method stub
		return (int) insts.instance(currentPosition - 1).valueSparse(iIndex);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see spade.analysis.tools.movement.MovementReader#getPosition()
	 */
	@Override
	public int getPosition() {
		// TODO Auto-generated method stub
		return currentPosition;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see spade.analysis.tools.movement.MovementReader#getTime()
	 */
	@Override
	public long getTime() {
		// TODO Auto-generated method stub
		return (long) insts.instance(currentPosition - 1).valueSparse(tIndex);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see spade.analysis.tools.movement.MovementReader#moveToNext()
	 */
	@Override
	public boolean moveToNext() {
		if (currentPosition >= insts.numInstances() + 1)
			// currentPosition--;
			return false;
		currentPosition += 1;
		return true;
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see spade.analysis.tools.movement.MovementReader#setPosition(long)
	 */
	@Override
	public void setPosition(long p) {
		// TODO Auto-generated method stub
		currentPosition = (int) p;
	}

}
