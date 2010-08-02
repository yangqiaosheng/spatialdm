import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public class TestMap {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		TreeMap<Integer, String> xTreemap = new TreeMap<Integer, String>();
		TreeMap<Integer, String> yTreemap = new TreeMap<Integer, String>();

		xTreemap.put(3, "3 5");
		xTreemap.put(2, "2 7");
		xTreemap.put(73, "73 43");
		xTreemap.put(23, "23 23");
		xTreemap.put(43, "43 64");
		xTreemap.put(41, "41 43");
		xTreemap.put(5, "5 72");

		yTreemap.put(5, "3 5");
		yTreemap.put(7, "2 7");
		yTreemap.put(47, "73 43");
		yTreemap.put(23, "23 23");
		yTreemap.put(64, "43 64");
		yTreemap.put(43, "41 43");
		yTreemap.put(72, "5 72");

		System.out.println(xTreemap.size());
		System.out.println(yTreemap.size());

		System.out.println("-----------------------------");
		for (Map.Entry<Integer, String> e : xTreemap.entrySet()) {
			System.out.println(e.getValue());
		}
		System.out.println("-----------------------------");

		SortedMap<Integer, String> xResults = xTreemap.subMap(4, 44);
		for (Map.Entry<Integer, String> e : xResults.entrySet()) {
			System.out.println(e.getValue());
		}
		System.out.println("-----------------------------");

		SortedMap<Integer, String> yResults = yTreemap.subMap(4, 44);
		for (Map.Entry<Integer, String> e : yResults.entrySet()) {
			System.out.println(e.getValue());
		}
		System.out.println("-----------------------------");

//		yResults.values().retainAll(xResults.values());
//		for(Map.Entry<Integer, String> e : yResults.entrySet()){
//			System.out.println(e.getValue());
//		}
//		System.out.println("-----------------------------");

		System.out.println(xTreemap.size());
		System.out.println(yTreemap.size());
		System.out.println(xResults.size());
		System.out.println(yResults.size());
	}

}
