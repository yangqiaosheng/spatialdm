package spade.analysis.geocomp.mutil;

import java.util.Vector;

public class SortVector extends Vector implements Cloneable {
	public boolean Duplicates;
	int Ind;

	public SortVector(boolean allowsDuplicates) {
		super();
		Duplicates = allowsDuplicates;
	}

	public SortVector() {
		this(false);
	}

	public SortVector(boolean allowsDuplicates, int initialCapacity) {
		super(initialCapacity);
		Duplicates = allowsDuplicates;
	}

	public SortVector(int initialCapacity) {
		this(false, initialCapacity);
	}

	public SortVector(int initialCapacity, int capacityIncrement) {
		super(initialCapacity, capacityIncrement);
		Duplicates = false;
	}

	public synchronized void Insert(Compare k) {
		if (!Search(k) || Duplicates) {
			super.insertElementAt(k, Ind);
		}
	}

	public synchronized int IndexOf(Compare k) {
		if (Search(k)) {
			if (Duplicates) {
				while (Ind < size() && k != elementAt(Ind)) {
					Ind++;
				}
			}
			if (Ind < size())
				return Ind;
		}
		return -1;
	}

	public synchronized boolean Search(Compare k) {
		int L, H, I, C;
		boolean res;

		res = false;
		L = 0;
		H = size() - 1;
		while (L <= H) {
			I = (L + H) >> 1;
			C = k.Comp(elementAt(I));
			if (C < 0) {
				L = I + 1;
			} else {
				H = I - 1;
				if (C == 0) {
					res = true;
					if (!Duplicates) {
						L = I;
					}
				}
			}
		}
		Ind = L;
		return res;
	}

	@Override
	public Object clone() {
		return super.clone();
	}

}