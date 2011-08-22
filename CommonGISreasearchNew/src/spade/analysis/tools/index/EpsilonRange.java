package spade.analysis.tools.index;

import java.util.LinkedList;
import java.util.List;

public class EpsilonRange<E> extends LinkedList<E> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7414211111996495476L;

	public EpsilonRange(List<E> toCopy) {
		for (int i = 0; i < toCopy.size(); i++) {
			add(toCopy.get(i));
		}
	}

	public int entries() {
		int s = 0;
		for (int i = 0; i < super.size(); i++) {
			if (get(i) instanceof EventList) {
				s += ((EventList) get(i)).getEvents().size();
			}
		}

		return s;
	}
}
