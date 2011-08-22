/**
 * 
 */
package spade.analysis.tools.movement;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeModel;

import weka.core.Instance;
import weka.core.Instances;

/**
 * @author Admin
 * 
 */
public class STMovementPattern {

	Instances insts = null;
	//double[][] instances = null;
	int[] cIds = null;
	// 10 hours
	/**
	 * Time window: events within this window are combined together
	 */
	public double eps = 14 * 24 * 3600000.0;
	/**
	 * Minimum group size
	 */
	public int minNum = 5;
	/**
	 * Column with the cluster ID
	 */
	int cIndex = 4;
	/**
	 * Column with the identifiers
	 */
	int iIndex = 3;
	/**
	 * Column with lime (transformed to long, milliseconds since 1970)
	 */
	int tIndex = 2;
	/**
	 * 1 - linear time; 2 - cyclic (mod tempRes);
	 * 3 - calendaric (depending on gFormat groups together what happened on the same month (MM),
	 *   or on the same month and day (MMDD), etc. YYYY;
	 *   HH - groups what happened on the same hour in all days)
	 */
	int tempConst = 1;
	/**
	 * in milliseconds
	 */
	public long tempRes = 1 * 14 * 24 * 3600000L;
	public String gFormat = "MM";

	long horizon = 0;

	public long lengths = 0;

	public int num = 0;

	RandomAccessFile lineReader = null;

	public MovementReaderArff movementData = new MovementReaderArff();

	public long getPosition() {
		try {
			return lineReader.getFilePointer();
		} catch (Exception e) {
			return -1;
			// TODO: handle exception
		}
	}

	public Instance getNextInstance() {
		Instance res = null;
		String line = "";
		try {
			movementData.moveToNext();
			res = movementData.getNextInstance();
		} catch (Exception e) {
			e.printStackTrace();
			// TODO: handle exception
		}
		return res;
	}

	public void setLine(long i) {
		try {
			lineReader.seek(i);
		} catch (Exception e) {
			e.printStackTrace();
			// TODO: handle exception
		}
	}

	public long getMilliseconds(String dt) {
		SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
		long res = -1;
		try {
			res = df.parse(dt).getTime();
		} catch (Exception e) {
			// TODO: handle exception
		}
		return res;
	}

	public String getDate(long dt) {
		SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
		String res = "";
		try {
			// res = df.parse(dt).getTime();
			Date d = new Date();
			d.setTime(dt);
			res = d.toString();
		} catch (Exception e) {
			// TODO: handle exception
		}
		return res;
	}

	public Date getDate2(long dt) {
		SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
		Date res = null;
		try {
			// res = df.parse(dt).getTime();
			Date d = new Date();
			d.setTime(dt);
			res = d;
		} catch (Exception e) {
			// TODO: handle exception
		}
		return res;
	}

	public Date getDate2(long dt, String format) {
		DateFormat df = new SimpleDateFormat(format);
		Date res = null;
		try {
			// res = df.parse(dt).getTime();
			Date d = new Date();

			d.setTime(dt);

			res = df.parse(df.format(d));
		} catch (Exception e) {
			e.printStackTrace();
			// TODO: handle exception
		}
		return res;
	}

	public void transformData(String path) {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(path + "\\insts.csv"));
			BufferedWriter writer = new BufferedWriter(new FileWriter(path + "\\data.txt"));
			String line = null;
			String[] token = null;
			// skip first line
			reader.readLine();
			// arff file
			/*
			 * writer.write("@relation MOVERS \n"); writer.write("\n");
			 * writer.write("@attribute ##_derived##X numeric \n");
			 * writer.write("@attribute ##_derived##Y numeric \n");
			 * writer.write("@attribute ##_derived##DT numeric \n");
			 * writer.write("@attribute ##_derived##ID numeric \n");
			 * writer.write("\n"); writer.write("@data \n"); // mapping for
			 * unique ids Map<String, Integer> mIds = new HashMap<String,
			 * Integer>();
			 */
			String nextId = "";
			int nextIId = 0;
			while ((line = reader.readLine()) != null) {
				token = line.split(",");
				nextId = token[3];

				// System.out.println("" + line + ":" + token.length);
				/*
				 * if (!mIds.containsKey(nextId)) { nextIId++; mIds.put(nextId,
				 * nextIId); } else {
				 * 
				 * }
				 */
				writer.write("" + token[0].replaceAll(",", ".") + "," + token[1].replaceAll(",", ".") + "," + getMilliseconds(token[2]) + "," + (nextId) + "," + token[4] + "\n");
			}
			writer.close();
			reader.close();
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}

	public void loadData(String path) {
		try {

			movementData.loadData(path + "\\data.txt");
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void loadData(Instances is, int cIdIx, int iIdIx, int tIx) {
		cIds = new int[is.numInstances()];
		insts = is;
		for (int i = 0; i < is.numInstances(); i++) {
			cIds[i] = (int) is.instance(i).valueSparse(cIdIx);
		}
		cIndex = cIdIx;
		iIndex = iIdIx;
		tIndex = tIx;
	}

	public void loadData(Instances is, int[] clusters, int iIdIx, int tIx) {
		insts = is;
		cIds = clusters;
		iIndex = iIdIx;
		tIndex = tIx;
	}

	protected long getTime(Instance inst) {
		return (long) inst.valueSparse(tIndex);
	}

	protected int getID(Instance inst) {
		return (int) inst.valueSparse(iIndex);
	}

	public void findPattern() {
		/*
		 * for each id the movement as sequence of cluster ids
		 */
		Map<Integer, String> movement = new HashMap<Integer, String>();
		Map<Integer, String> tmpMovement = new HashMap<Integer, String>();

		Map<Integer, List<Instance>> clusterMembers = new HashMap<Integer, List<Instance>>();
		Map<Integer, Integer> clusterStartIndex = new HashMap<Integer, Integer>();

		Instance next = null;
		Instance last = null;
		Integer currId = null;
		List<Instance> currMembers = null;
		boolean foundCluster = false;
		/*
		 * iterate through the data set
		 */
		for (int i = 0; i < cIds.length; i++) {
			next = insts.instance(i);
			if (clusterMembers.containsKey(cIds[i])) {
				currMembers = clusterMembers.get(cIds[i]);
				last = ((LinkedList<Instance>) currMembers).getLast();
				/*
				 * last and next are close in time, thus in the same cluster
				 */
				if (getTime(next) <= getTime(last) + eps) {
					currMembers.add(next);
				}
				/*
				 * cluster expansion is finished
				 */
				else {
					if (currMembers.size() >= minNum) {
						foundCluster = true;
					} else {
						currMembers = null;
						clusterMembers.remove(cIds[i]);
						clusterStartIndex.remove(cIds[i]);
						foundCluster = false;
					}
				}
			} else {
				clusterStartIndex.put(cIds[i], i);
				List<Instance> tmpList = new LinkedList<Instance>();
				tmpList.add(next);
				clusterMembers.put(cIds[i], tmpList);
			}
			if (foundCluster || (i == cIds.length - 1)) {
				for (Instance inst : currMembers) {
					movement.put(getID(inst), "" + cIds[i]);
				}

				/*
				 * possible next steps among the spatial clusters
				 */
				Map<Integer, Map<Integer, List<Instance>>> posContinuations = new HashMap<Integer, Map<Integer, List<Instance>>>();

				/*
				 * the continuations of the movement so far
				 */
				Map<Integer, Map<Integer, String>> continuations = new HashMap<Integer, Map<Integer, String>>();

				/*
				 * the members of the continuation clusters
				 */
				Map<Integer, List<Instance>> continuationMembers = new HashMap<Integer, List<Instance>>();
				continuations.put(cIds[i], movement);
				int conts = 0;
				for (Integer start : clusterStartIndex.keySet()) {
					System.out.print(start + " ");
					tmpMovement.clear();
					movement = continuations.get(start);
					if (movement == null) {
						continue;
					}
					if (movement.size() <= 0) {
						continue;
					}
					posContinuations.clear();
					/*
					 * Calculate statistics about current temporal cluster
					 */

					/*
					 * find continuations
					 */
					for (int j = clusterStartIndex.get(start); j < cIds.length; j++) {
						next = insts.instance(j);
						/*
						 * Search for continuations of the movement of a certain
						 * id
						 */
						if ((movement.containsKey(getID(next))) && (!tmpMovement.containsKey(getID(next)))) {
							/*
							 * Found continuation of a id, thus this id is
							 * finished
							 */
							tmpMovement.put(getID(next), "" + movement.get(getID(next)));
							/*
							 * A mapping from the spatial clusters to the next
							 * moves
							 */
							Map<Integer, List<Instance>> movementToCluster = null;

							boolean first = false;
							/*
							 * Group all next steps according to a certain
							 * spatial cluster
							 */
							if (posContinuations.containsKey(cIds[j])) {
								continuationMembers = posContinuations.get(cIds[j]);
							} else {
								first = true;
								continuationMembers = new HashMap<Integer, List<Instance>>();
								List<Instance> members = new LinkedList<Instance>();
								members.add(next);
								continuationMembers.put(cIds[j], members);
								posContinuations.put(cIds[j], continuationMembers);
								clusterStartIndex.put(cIds[j], j);
							}

							if (first) {
								continue;
							}
							last = ((LinkedList<Instance>) continuationMembers.get(cIds[j])).getLast();
							/*
							 * last and next are close in time, thus in the same
							 * temporal cluster
							 */
							if (getTime(next) <= getTime(last) + eps) {
								/*
								 * The movement from the next id to a spatial
								 * cluster
								 */

								continuationMembers.get(cIds[j]).add(insts.instance(j));
							} else {
								/*
								 * Are there enough movements
								 */
								if (continuationMembers.get(cIds[j]).size() < minNum) {
									continuationMembers.remove(cIds[j]).clear();
								} else {
									Map<Integer, String> nexts = new HashMap<Integer, String>();
									for (Instance inst : continuationMembers.get(cIds[j])) {
										nexts.put(getID(inst), "");
									}
									continuations.put(cIds[j], nexts);
								}

							}
							last = next;
						}
					}
				}
			}
		}
	}

	JTree tree = new JTree();

	public void start() {
		// root of the pattern tree
		DefaultMutableTreeNode root = new DefaultMutableTreeNode("Start");
		System.out.print("Start cluster " + -1);
		Map<Integer, Long> movement = new HashMap<Integer, Long>();
		System.out.print("{");

		for (int i = 0; i < insts.numInstances(); i++) {
			Instance inst = insts.instance(i);
			if (!movement.containsKey(getID(inst))) {
				movement.put(getID(inst), getTime(inst));
			}
			// System.out.print("("+ getID(inst) + " ," +
			// getDate(getTime(inst))+")");
			// System.out.print("," + getID(inst) + ";" +
			// getTime(inst));
		}
		System.out.print(0);
		System.out.println("}(" + movement.size() + ")");
		String timeInterval = "{" + getDate(getTime(insts.instance(0))) + " to " + getDate(getTime(insts.instance(insts.numInstances() - 1))) + "}" + "(" + movement.size() + ")";
		DefaultMutableTreeNode child = new DefaultMutableTreeNode("" + "Start" + " " + timeInterval);
		root.add(child);
		findContinuation(0, movement, 0, child, new Dummy());

		show(root);
	}

	public Instance getInstance(double[] insts) {
		Instance res = null;
		res = new Instance(insts.length);
		for (int i = 0; i < insts.length; i++) {
			res.setValueSparse(i, insts[i]);
		}
		return res;
	}

	private class TempCluster {
		int label = -1;
		long counts = 0;
		long minTime = 0;
		long maxTime = 0;
	}

	public void writeTempClusters(List<Instance> L) {
		for (Instance in : L) {
			System.out.println("" + in.valueSparse(iIndex) + " " + in.valueSparse(cIndex) + " " + getDate((long) in.valueSparse(tIndex)));
			// System.out.println(in.toString());
		}
	}

	long firstInTempRes = 0;

	public boolean tempConstraintInterval2(Instance last, Instance next, long tempRes) {
		try {
			return (getTime(next) - firstInTempRes >= tempRes);
		} catch (Exception e) {
			return false;
		}
	}

	public boolean tempConstraintDate(Instance last, Instance next) {
		try {
			return (getDate2(getTime(next), gFormat).equals(getDate2(getTime(last), gFormat)));
		} catch (Exception e) {
			return false;
		}
	}

	public boolean tempConstraintDensity(Instance last, Instance next) {
		try {
			return (getTime(next) <= getTime(last) + eps);
		} catch (Exception e) {
			return false;
		}
	}

	public boolean tempConstraintInterval(Instance last, Instance next, long tempRes) {
		try {
			return (getTime(next) / tempRes == getTime(last) / tempRes);
		} catch (Exception e) {
			return false;
		}
	}

	public boolean tempConstraint(Instance last, Instance next) {
		switch (tempConst) {
		case 1:
			return tempConstraintDensity(last, next);
		case 2:
			return tempConstraintInterval(last, next, tempRes);
		case 3:
			return tempConstraintDate(last, next);
		}
		return false;
	}

	public List getTempIntervals(int sClusterLabel) {

		return null;
	}

	public int getClusters(int sClusterLabel, LinkedList<Instance> S, int currentPos) {
		LinkedList<Instance> T1 = new LinkedList<Instance>();
		LinkedList<Instance> T2 = new LinkedList<Instance>();
		System.out.println(currentPos);
		int pos = currentPos - 1;
		movementData.setPosition(pos);
		while ((T1.size() + T2.size() - 1 < minNum) && (movementData.moveToNextCluster(sClusterLabel))) {
			T1.clear();
			T2.clear();
			T1.addLast(movementData.getNextInstance());
			while ((movementData.moveToNextCluster(sClusterLabel)) && (getTime(movementData.getNextInstance()) - getTime(T1.getFirst()) < eps)) {
				T1.addLast(movementData.getNextInstance());
			}
			T2.addLast(T1.getLast());
			while ((movementData.moveToNextCluster(sClusterLabel) && (getTime(movementData.getNextInstance()) - getTime(T2.getFirst()) < eps))) {
				T2.addLast(movementData.getNextInstance());
			}
			if (T1.size() + T2.size() - 1 < minNum) {
				movementData.setPosition(pos++);
			}
		}
		if (T1.size() + T2.size() - 1 < minNum)
			return -1;

		// found core object beginning at pos with neighborhood T1 and T2
		// goto end of neighborhood
		movementData.setPosition(pos + T1.size() + T2.size());
		S.addAll(T1);

		if ((!movementData.hasNext()))
			return -1;

		while ((T1.size() + T2.size() - 1 >= minNum) && (movementData.moveToNextCluster(sClusterLabel))) {
			T1.clear();
			T1.addAll(T2);
			S.addAll(T2);
			T2.clear();
			T2.addLast(T1.getLast());
			while ((movementData.moveToNextCluster(sClusterLabel)) && (getTime(movementData.getNextInstance()) - getTime(T1.getLast()) < eps)) {
				T2.addLast(movementData.getNextInstance());
			}
		}
		S.addAll(T2);
		return movementData.getPosition();
	}

	public int getClusters2(int sClusterLabel, LinkedList<Instance> S, int currentPos) {
		LinkedList<Instance> T1 = new LinkedList<Instance>();
		LinkedList<Instance> T2 = new LinkedList<Instance>();
		System.out.println(currentPos);
		int pos = currentPos - 1;
		movementData.setPosition(pos);
		movementData.clearSW();
		movementData.eps = (long) eps;
		while ((movementData.lengthSW() < minNum) && (movementData.moveToNext())) {
			movementData.updateSW();
			// System.out.println("update");
		}
		// System.out.println("Finished update");
		if (movementData.lengthSW() < minNum)
			return -1;

		// found core object beginning at pos with neighborhood T1 and T2
		// goto end of neighborhood
		// movementData.setPosition(pos + T1.size() + T2.size());
		S.addAll(movementData.T1);
		S.addLast(movementData.currentInst);

		if ((!movementData.hasNext())) {
			S.addAll(movementData.T2);
			return -1;
		}

		while ((movementData.lengthSW() >= minNum) && movementData.expend()) {

			S.addAll(movementData.T1);
			S.addLast(movementData.currentInst);
		}
		S.addAll(movementData.T2);
		return movementData.getPosition();
	}

	public List getTempClusters(int sClusterLabel) {
		LinkedList<Instance> T = new LinkedList<Instance>();
		// get the first minNum data points of the spatial cluster
		while ((T.size() < minNum) && (movementData.moveToNext())) {
			if (sClusterLabel == movementData.getClusterID()) {
				T.addLast(movementData.getNextInstance());
			}
		}
		// no data object with enough neighbors found
		if (T.size() < minNum)
			return null;
		// search for core object
		while ((getTime(T.getLast()) - getTime(T.getFirst()) > 2 * eps) && (movementData.moveToNext())) {
			if (sClusterLabel == movementData.getClusterID()) {
				T.removeFirst();
				T.addLast(movementData.getNextInstance());
			}
		}
		// no core object found
		if (getTime(T.getLast()) - getTime(T.getFirst()) > 2 * eps)
			return null;
		// core object found
		// expand cluster
		List S = new LinkedList();
		S.addAll(T);
		Instance nextInst = null;
		while (movementData.hasNext()) {
			// expansion finished
			if (T.size() < minNum)
				return S;

			T.removeFirst();
			Instance tmpInsts = T.getFirst();
			// movementData.moveToNextCluster(sClusterLabel);
			while ((movementData.hasNext()) && (getTime(nextInst = movementData.getNextInstance()) - getTime(tmpInsts) <= 2 * eps)) {
				if (sClusterLabel == movementData.getClusterID()) {
					((LinkedList<Instance>) S).addLast(nextInst);
					T.addLast(nextInst);
					movementData.moveToNextCluster(sClusterLabel);
				}
			}
			// System.out.println(""+T.size()+" first " + T.getFirst() +
			// " last " + T.getLast() + " next " + nextInst);

		}
		return S;
	}

	private class Dummy {
		long pos = 0;
		long access = 0;
		long num = 0;
	}

	public long paths = 0;

	public void pattern() throws IOException {

		BufferedWriter writer = new BufferedWriter(new FileWriter(".\\lengths.txt"));
		/*
		 * for each id the movement as sequence of cluster ids
		 */
		Map<Integer, Long> movement = new HashMap<Integer, Long>();
		Map<Integer, String> tmpMovement = new HashMap<Integer, String>();

		Map<Integer, List<Instance>> clusterMembers = new HashMap<Integer, List<Instance>>();

		tree = new JTree();
		movementData.setPosition(1);
		movementData.numAccess = 0;
		Map<Integer, List<Instance>> nextClusterMembers = new HashMap<Integer, List<Instance>>();

		Map<Integer, Long> clusterStartIndex = new HashMap<Integer, Long>();

		Instance next = null;
		Instance last = null;
		int currClus = -1;
		List<Instance> currMembers = null;
		boolean foundCluster = false;

		// root of the pattern tree
		DefaultMutableTreeNode root = new DefaultMutableTreeNode("Start");
		lengths = 0;
		num = 0;
		paths = 0;
		// next id
		int nId = -1;
		/*
		 * iterate through the data set
		 */
		boolean end = false;
		// for (int i = 0; i < cIds.length; i++)
		while (!end) {
			// insts.instance(i);
			movementData.moveToNext();
			next = movementData.getNextInstance();

			if (next == null) {
				end = true;
			} else {
				nId = movementData.getClusterID();

				if (clusterMembers.containsKey(nId)) {
					currMembers = clusterMembers.get(nId);
					currClus = nId;
					last = ((LinkedList<Instance>) currMembers).getLast();
					/*
					 * last and next are close in time, thus in the same
					 * temporal cluster
					 */
					if (tempConstraint(last, next)) {
						currMembers.add(next);
					}
					/*
					 * cluster expansion is finished
					 */
					else {
						if (currMembers.size() >= minNum) {
							foundCluster = true;
							nextClusterMembers.clear();
							nextClusterMembers.put(nId, currMembers);
						} else {
							currMembers = null;
							clusterMembers.remove(nId);
							clusterStartIndex.remove(nId);
							foundCluster = false;
						}
						movementData.setPosition(movementData.getPosition() - 1);
					}
					last = next;
				} else {
					if ((currMembers != null) && (last != null) && (!tempConstraint(last, next))) {
						if (currMembers.size() >= minNum) {

							nextClusterMembers.clear();

							for (Integer key : clusterMembers.keySet()) {
								if (clusterMembers.get(key).size() >= minNum) {
									foundCluster = true;
									nextClusterMembers.put(key, clusterMembers.get(key));
								}
							}
						} else {
							currMembers = null;
							clusterMembers.remove(nId);
							clusterStartIndex.remove(nId);
							foundCluster = false;
						}
					}

					clusterStartIndex.put(nId, (long) movementData.getPosition());
					List<Instance> tmpList = new LinkedList<Instance>();
					tmpList.add(next);
					last = next;
					clusterMembers.put(nId, tmpList);
				}
			}

			if (end) {
				foundCluster = true;
				nextClusterMembers.clear();
				for (Integer key : clusterMembers.keySet()) {
					if (clusterMembers.get(key).size() >= minNum) {
						nextClusterMembers.put(key, clusterMembers.get(key));
					}
				}
			}

			if (foundCluster || (end)) {
				Set<Integer> keys = nextClusterMembers.keySet();
				Integer[] iKeys = new Integer[keys.size()];
				iKeys = keys.toArray(iKeys);
				for (Integer key : iKeys) {
					currMembers = nextClusterMembers.get(key);
					// System.out.println(cIds[i] + " " + i + " found " +
					// currMembers.size());

					movement.clear();
					for (Instance inst : currMembers) {
						movement.put(getID(inst), getTime(inst));
						// System.out.print("("+ getID(inst) + " ," +
						// getDate(getTime(inst))+")");
						// System.out.print("," + getID(inst) + ";" +
						// getTime(inst));
					}
					if (movement.size() >= minNum) {

						System.out.print("Start cluster " + key);
						System.out.print("(");
						for (Integer it : movement.keySet()) {
							System.out.print(it + ",");
						}
						System.out.print(")");

						System.out.print("[");
						System.out.print(((Integer) clusterStartIndex.get(key).intValue()));
						System.out.println("](" + movement.size() + ")");

						String tmp = (movementData.numAccess + " " + clusterStartIndex.get(key).longValue()) + " ";

						// writer.write(tmp);

						String timeInterval = "{" + getDate(getTime(currMembers.get(0))) + " to " + getDate(getTime(currMembers.get(currMembers.size() - 1))) + "}" + "(" + movement.size() + ")";
						timeInterval = "" + key + " " + timeInterval;
						DefaultMutableTreeNode child = new DefaultMutableTreeNode(null);
						root.add(child);
						long lastLine = movementData.getPosition();
						Dummy it = new Dummy();

						long lastStart = clusterStartIndex.get(key).longValue();

						it.num++;
						findContinuation(((Integer) clusterStartIndex.get(key).intValue()), movement, 0, child, it);
						timeInterval += "[" + movementData.getPosition() + "]";

						child.setUserObject(timeInterval);
						paths += it.num;
						if (it.pos > 0) {
							// writer.write(num++ + " " + tmp + it.pos + "\n");
							writer.write(num++ + " " + tmp + it.access + " " + it.pos + " " + it.num + "\n");
							lengths += it.pos - lastStart;
							writer.flush();
						} else {
							writer.write(num++ + " " + tmp + tmp + " " + it.num + "\n");

							// lengths += it.pos - lastStart;
							writer.flush();
						}

						movementData.setPosition(lastLine);
					}
					clusterMembers.remove(key);
					nextClusterMembers.remove(key);
					clusterStartIndex.remove(key);
					currMembers.clear();
					foundCluster = false;
				}
			}
		}
		show(root);
		writer.close();
	}

	class Pair<E, T> {
		E first;
		T second;
	}

	public void findInterContinuation(long start, Map<Integer, Long> movement, int l, DefaultMutableTreeNode node) {
		// Recursive call for the next continuations

		Map<Integer, Long> nextClusterStartIndex = new HashMap<Integer, Long>();
		Map<Integer, List<Instance>> conts = interpolateContinuation(start, movement, nextClusterStartIndex, null);
		Map<Integer, Long> movementTmp = new HashMap<Integer, Long>();
		if ((conts != null) && (conts.size() > 0)) {
			// System.out.println("parent: " + cIds[i] + " " + conts.size());
			/**
			 * All clusters continue the movement
			 */

			for (Integer cCont : conts.keySet()) {
				long min = Long.MAX_VALUE;
				long max = Long.MIN_VALUE;
				for (Instance in : conts.get(cCont)) {
					if (movement.get(getID(in)) < min) {
						min = movement.get(getID(in));
					}
					if (movement.get(getID(in)) > max) {
						max = movement.get(getID(in));
					}
				}
				String timeInterval = "{" + getDate(min) + " to " + getDate(max) + "}";
				movementTmp.clear();
				System.out.print(" Level: " + l + " cont. cluster " + cCont + "");
				/**
				 * All members of the continuation clusters
				 */

				System.out.print("(");
				for (Instance inst : conts.get(cCont)) {
					movementTmp.put(getID(inst), getTime(inst));
					System.out.print(getID(inst) + ",");
				}
				System.out.print(")");

				timeInterval += " to {" + getDate(getTime(conts.get(cCont).get(0))) + " to " + getDate(getTime(conts.get(cCont).get(conts.get(cCont).size() - 1))) + "}" + "(" + movementTmp.size() + ")";
				System.out.print("{");
				System.out.print(((Integer) nextClusterStartIndex.get(cCont).intValue()));
				System.out.print("}(" + movementTmp.size() + ") ");
				DefaultMutableTreeNode child = new DefaultMutableTreeNode("" + cCont + " " + timeInterval);
				node.add(child);
				findContinuation(((Integer) nextClusterStartIndex.get(cCont).intValue()), movementTmp, l + 1, child, new Dummy());
				movementData.setPosition(start);
			}
			System.out.println();
		}
		System.out.println();

	}

	public Map<Integer, List<Instance>> interpolateContinuation(long start, Map<Integer, Long> movement, Map<Integer, Long> clusterStartIndex, Map<Integer, Long> clusterEndIndex) {
		/**
		 * the members of the continuation clusters
		 */
		Map<Integer, List<Instance>> continuationMembers = new HashMap<Integer, List<Instance>>();
		/*
		 * the continuations of the movement so far
		 */
		Map<Integer, Map<Integer, String>> continuations = new HashMap<Integer, Map<Integer, String>>();

		Map<Integer, Map<Integer, Integer>> clusterIdMemberId = new HashMap<Integer, Map<Integer, Integer>>();

		Map<Integer, String> tmpMovement = new HashMap<Integer, String>();

		Instance next = null;
		Instance last = null;

		/*
		 * Calculate statistics about current temporal cluster
		 */

		/*
		 * find continuations
		 */
		continuationMembers = new HashMap<Integer, List<Instance>>();
		movementData.setPosition(start - 1);
		int nId = -1;
		int nCId = -1;
		long lastTime = Long.MAX_VALUE;
		while (((next = getNextInstance()) != null) && (movement.size() != tmpMovement.size())) {

			// movementData.moveToNext();
			nId = movementData.getID();
			nCId = movementData.getClusterID();
			/*
			 * Search for continuations of the movement of a certain id
			 */
			if (true) {
				if (clusterIdMemberId.containsKey(nCId)) {
					if (clusterIdMemberId.get(nCId).containsKey(nId)) {
						continue;
					}
				}

				/*
				 * It most be later than the last step
				 */
				// if (movement.get(nId) >= movementData.getTime()) {
				// continue;
				// }

				/*
				 * Found continuation of a id, thus this id is finished
				 * Error?????
				 */
				// tmpMovement.put(nId, "" + movement.get(nId));

				/*
				 * A mapping from the spatial clusters to the next moves
				 */
				Map<Integer, List<Instance>> movementToCluster = null;
				List<Instance> members = null;
				Map<Integer, Integer> memberIds = null;
				boolean first = false;
				/*
				 * Group all next steps according to a certain spatial cluster
				 */
				if (continuationMembers.containsKey(nCId)) {
					members = continuationMembers.get(nCId);
					memberIds = clusterIdMemberId.get(nCId);
				} else {
					first = true;
					memberIds = new HashMap<Integer, Integer>();
					memberIds.put(nId, nId);
					clusterIdMemberId.put(nCId, memberIds);
					members = new LinkedList<Instance>();
					members.add(next);
					continuationMembers.put(nCId, members);
					clusterStartIndex.put(nCId, (long) movementData.getPosition());
				}

				if (first) {
					continue;
				}
				last = ((LinkedList<Instance>) members).getLast();
				/*
				 * last and next are close in time, thus in the same temporal
				 * cluster
				 */
				if (getTime(next) <= getTime(last) + eps) {
					if (!movement.containsKey(nId)) {
						movement.put(nId, getTime(next));
					}
					/*
					 * The movement from the next id to a spatial cluster
					 */
					clusterIdMemberId.get(nCId).put(nId, nId);
					continuationMembers.get(nCId).add(next);
				} else {
					/*
					 * Are there enough movements
					 */
					if (continuationMembers.get(nCId).size() < minNum) {
						continuationMembers.remove(nCId);
						clusterStartIndex.remove(nCId);
					} else {
						Map<Integer, String> nexts = new HashMap<Integer, String>();
						for (Instance inst : continuationMembers.get(nCId)) {
							nexts.put(getID(inst), "");
						}
						continuations.put(nCId, nexts);
					}

				}

				last = next;
			}
		}
		if (continuationMembers.size() < 1)
			return null;
		Set<Integer> keys = continuationMembers.keySet();
		Integer[] inKeys = new Integer[keys.size()];
		inKeys = keys.toArray(inKeys);
		for (Integer inKey : inKeys) {
			if (continuationMembers.get(inKey).size() < minNum) {
				continuationMembers.remove(inKey);
			}
		}
		return continuationMembers;

	}

	public void findContinuation(long start, Map<Integer, Long> movement, int l, DefaultMutableTreeNode node, Dummy lastPos) {
		// Recursive call for the next continuations

		Map<Integer, Long> nextClusterStartIndex = new HashMap<Integer, Long>();
		Map<Integer, List<Instance>> conts = getContinuations(start, movement, nextClusterStartIndex);

		Map<Integer, Long> movementTmp = new HashMap<Integer, Long>();
		if ((conts != null) && (conts.size() > 0)) {

			// System.out.println("parent: " + cIds[i] + " " + conts.size());
			/**
			 * All clusters continue the movement
			 */

			for (Integer cCont : conts.keySet()) {
				lastPos.num++;
				lastPos.pos = (nextClusterStartIndex.get(cCont).longValue());

				lastPos.access = movementData.numAccess;

				long min = Long.MAX_VALUE;
				long max = Long.MIN_VALUE;
				for (Instance in : conts.get(cCont)) {
					if (movement.get(getID(in)) < min) {
						min = movement.get(getID(in));
					}
					if (movement.get(getID(in)) > max) {
						max = movement.get(getID(in));
					}
				}
				String timeInterval = "{" + getDate(min) + " to " + getDate(max) + "}";
				movementTmp.clear();
				System.out.print(" Level: " + l + " cont. cluster " + cCont + "");
				/**
				 * All members of the continuation clusters
				 */

				System.out.print("(");
				for (Instance inst : conts.get(cCont)) {
					movementTmp.put(getID(inst), getTime(inst));
					System.out.print(getID(inst) + ",");
				}
				System.out.print(")");

				timeInterval += " to {" + getDate(getTime(conts.get(cCont).get(0))) + " to " + getDate(getTime(conts.get(cCont).get(conts.get(cCont).size() - 1))) + "}" + "(" + movementTmp.size() + ")";
				System.out.print("{");
				System.out.print(((Integer) nextClusterStartIndex.get(cCont).intValue()));
				System.out.print("}(" + movementTmp.size() + ") ");
				DefaultMutableTreeNode child = new DefaultMutableTreeNode("" + cCont + " " + timeInterval);
				node.add(child);
				findContinuation(((Integer) nextClusterStartIndex.get(cCont).intValue()), movementTmp, l + 1, child, lastPos);

				movementData.setPosition(start);
			}
			System.out.println();
		}
		System.out.println();

	}

	/**
	 * 
	 * @param start
	 *            The index of the first possible continuation
	 * @param movement
	 *            The ids of all events that continuations shall be extracted
	 * @param clusterStartIndex
	 *            The first possible start indexes of all continuation clusters
	 * @return
	 */
	public Map<Integer, List<Instance>> getContinuations(long start, Map<Integer, Long> movement, Map<Integer, Long> clusterStartIndex) {

		/**
		 * the members of the continuation clusters
		 */
		Map<Integer, List<Instance>> continuationMembers = new HashMap<Integer, List<Instance>>();
		/*
		 * the continuations of the movement so far
		 */
		Map<Integer, Map<Integer, String>> continuations = new HashMap<Integer, Map<Integer, String>>();

		Map<Integer, Map<Integer, Integer>> clusterIdMemberId = new HashMap<Integer, Map<Integer, Integer>>();

		Map<Integer, String> tmpMovement = new HashMap<Integer, String>();

		Instance next = null;
		Instance last = null;

		/*
		 * Calculate statistics about current temporal cluster
		 */

		/*
		 * find continuations
		 */
		continuationMembers = new HashMap<Integer, List<Instance>>();
		movementData.setPosition(start - 1);
		int nId = -1;
		int nCId = -1;
		long lastTime = Long.MAX_VALUE;
		while (((next = getNextInstance()) != null) && (movement.size() != tmpMovement.size())) {

			// movementData.moveToNext();
			nId = movementData.getID();
			nCId = movementData.getClusterID();
			/*
			 * Search for continuations of the movement of a certain id
			 */
			if ((movement.containsKey(nId)) && (!tmpMovement.containsKey(nId))) {
				if (clusterIdMemberId.containsKey(nCId)) {
					if (clusterIdMemberId.get(nCId).containsKey(nId)) {
						continue;
					}
				}

				/*
				 * It most be later than the last step
				 */
				if (movement.get(nId) >= movementData.getTime()) {
					continue;
				}

				/*
				 * Found continuation of a id, thus this id is finished
				 * Error?????
				 */
				// tmpMovement.put(nId, "" + movement.get(nId));

				/*
				 * A mapping from the spatial clusters to the next moves
				 */
				Map<Integer, List<Instance>> movementToCluster = null;
				List<Instance> members = null;
				Map<Integer, Integer> memberIds = null;
				boolean first = false;
				/*
				 * Group all next steps according to a certain spatial cluster
				 */
				if (continuationMembers.containsKey(nCId)) {
					members = continuationMembers.get(nCId);
					memberIds = clusterIdMemberId.get(nCId);
				} else {
					first = true;
					memberIds = new HashMap<Integer, Integer>();
					memberIds.put(nId, nId);
					clusterIdMemberId.put(nCId, memberIds);
					members = new LinkedList<Instance>();
					members.add(next);
					continuationMembers.put(nCId, members);
					clusterStartIndex.put(nCId, (long) movementData.getPosition());
				}

				if (first) {
					continue;
				}
				last = ((LinkedList<Instance>) members).getLast();

				firstInTempRes = clusterStartIndex.get(nCId);

				/*
				 * last and next are close in time, thus in the same temporal
				 * cluster
				 */
				if (tempConstraint(last, next)) {
					/*
					 * The movement from the next id to a spatial cluster
					 */
					clusterIdMemberId.get(nCId).put(nId, nId);
					continuationMembers.get(nCId).add(next);
				} else {
					/*
					 * Are there enough movements
					 */
					if (continuationMembers.get(nCId).size() < minNum) {
						continuationMembers.remove(nCId);
						clusterStartIndex.remove(nCId);
					} else {
						Map<Integer, String> nexts = new HashMap<Integer, String>();
						for (Instance inst : continuationMembers.get(nCId)) {
							nexts.put(getID(inst), "");
						}
						continuations.put(nCId, nexts);
					}

				}

				last = next;
			}
		}
		if (continuationMembers.size() < 1)
			return null;
		Set<Integer> keys = continuationMembers.keySet();
		Integer[] inKeys = new Integer[keys.size()];
		inKeys = keys.toArray(inKeys);
		for (Integer inKey : inKeys) {
			if (continuationMembers.get(inKey).size() < minNum) {
				continuationMembers.remove(inKey);
			}
		}
		return continuationMembers;
	}

	public void show(DefaultMutableTreeNode root) {
		JFrame frame = new JFrame("Pattern Tree");
		frame.addWindowListener(new WindowAdapter() {

			public void windowClosing(WindowEvent event) {
				System.exit(0);
			}
		});
		JTree tree = new JTree(root);
		frame.add(new JScrollPane(tree));
		frame.setSize(200, 400);
		frame.setVisible(true);
	}

	public JTree filterTree(String obj) {

		return null;
	}

	public void travers(String obj, TreeModel model, Object parent, DefaultMutableTreeNode tree) {
		if (model.isLeaf(parent))
			return;
		if (parent.toString() == obj) {
			tree.add((DefaultMutableTreeNode) parent);
			return;
		}
		return;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		try {
/*
			System.setOut(new PrintStream(new FileOutputStream(
					".\\movement\\berlinflickr\\out.txt")));
*/
			System.setOut(new PrintStream(new FileOutputStream("\\CommonGISProjects\\patterns\\out.txt")));
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		STMovementPattern mov = new STMovementPattern();
/*
		mov.loadData(".\\movement\\berlinflickr");
*/
		mov.loadData("\\CommonGISProjects\\patterns");
		/*
		 * LinkedList<Instance> L = new LinkedList<Instance>(); int i = 1; while
		 * ((i = mov.getClusters2(48, L, i)) != -1) { System.out.println(i);
		 * System.out.println("Current results: "); mov.writeTempClusters(L);
		 * L.clear(); }
		 */
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(".\\means.txt", true));
			for (int i = 1; i < 2; i++) {
				//mov.tempRes = 24 * i * 1 * 3600000L;
				//mov.minNum = i;
				//mov.eps = 1 * 1 * i * 3600000;
				mov.pattern();
				//if (mov.lengths != 99)
				writer.write(i + " " + mov.lengths + " " + mov.num + " " + mov.movementData.numAccess + " " + mov.paths + "\n");
				//else
				//writer.write(i + " " + 0 + "\n");
				writer.flush();
			}
			writer.close();
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}

		// mov.start();
		//mov.transformData(".\\movement\\berlinflickr");

	}
}
