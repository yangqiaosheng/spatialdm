package spade.analysis.tools.db_tools;

/**
 * Created by IntelliJ IDEA.
 * User: gennady
 * Date: 16-Nov-2007
 * Time: 14:50:24
 * Specifies a division of movement data from a table according to values in
 * two column containing speed and direction.
 * Rows with speed value lower than threshold are assigned to class0
 */
public class DirectionAndSpeedDivisionSpec extends DivisionSpec {

	/**
	 * The name of the column with values of the speed
	 * Direction is stored in the column indicated by columnName
	 */
	public String speedColumnName = "";
	/**
	 * The index of the column with values of the speed
	 * Direction is stored in the column indicated by columnIdx
	 */
	public int speedColumnIdx = -1;

	/**
	 * number of segments should be either 4 or 8
	 */
	public int nSegments = 8;

	/**
	 * low speed threshold
	 */
	public float minSpeed = 5f;

	public String[] getClassNames() {
		String[] classNames = null;
		if (nSegments == 4) {
			classNames = new String[5];
			classNames[0] = "0";
			classNames[1] = "N";
			classNames[2] = "E";
			classNames[3] = "S";
			classNames[4] = "W";
		} else {
			classNames = new String[9];
			classNames[0] = "0";
			classNames[1] = "N";
			classNames[2] = "NE";
			classNames[3] = "E";
			classNames[4] = "SE";
			classNames[5] = "S";
			classNames[6] = "SW";
			classNames[7] = "W";
			classNames[8] = "NW";
		}
		return classNames;
	}

	/**
	 * nPartitions=1+nSegments (an additional partition for "low speed"
	 */
	@Override
	public int getPartitionCount() {
		return 1 + nSegments;
	}

	@Override
	public String getPartitionLabel(int n) {
		return getClassNames()[n];
	}

	public float[][] getIntervals() {
		float angle = (nSegments == 4) ? 90f : 45f, shift = angle / 2;
		float vals[][] = new float[1 + nSegments][];
		int idx = 0;
		vals[idx] = new float[3];
		vals[idx][0] = 1;
		vals[idx][1] = 0;
		vals[idx][2] = shift;
		idx++;
		vals[idx] = new float[3];
		vals[idx][0] = 1;
		vals[idx][1] = 360f - shift;
		vals[idx][2] = 360;
		for (int i = 1; i < nSegments; i++) {
			idx++;
			vals[idx] = new float[3];
			vals[idx][0] = 1 + i;
			vals[idx][1] = -shift + i * angle;
			vals[idx][2] = shift + i * angle;
		}
		return vals;
	}

}
