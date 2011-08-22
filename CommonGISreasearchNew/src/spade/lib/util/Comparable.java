package spade.lib.util;

public interface Comparable {
	/**
	*  Returns 0 if equal, <0 if THIS is less than the argument, >0 otherwise
	*/
	public int compareTo(Comparable c);
}
