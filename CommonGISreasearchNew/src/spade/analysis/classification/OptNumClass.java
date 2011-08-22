package spade.analysis.classification;

// import java.util.Arrays;

/**
 * The class implements discretisation algorithm. It takes a data source
 * which provides the distribution to analyse and generates its
 * partition according to the chosen method. Parameters of the
 * discretisation include method, list of attributes (one attribute
 * in one-dimensional case), number of classes, percentage
 * of quality decrease (e.g., information loss) etc.
 * Generated descriptions are stored in the output model.
 * <p>
 * The algorithm starts searching for optimal partitioning from
 * the minimum number of classes, i.e., this is the first parameter
 * being optimised. If it finds a partitioning that
 * satisfy the criterion of quantity of information, i.e., the information of which
 * is greater than <code>minInformation</code> then it stops.
 * Otherwise it increases the number of classes and tries to
 * find the optimal partitioning with these weaker constraints.
 * If the <code>maxClasses</code> is reached then the algorithm
 * stops and returns the best result which however does not
 * satisfy the criterion of quality. If the criterion of optimality
 * is not specified, i.e., <code>minQuality=0</code>, then
 * the algorithm finds the best partitioning consisting of
 * <code>minClasses</code>. If the <code>maxClasses</code> is
 * not specified (some very big number) then the algorithm
 * proceeds until it finds the partitioning satisfying the
 * criterion of quality. The <code>minClasses</code> should
 * always be specified.
 * <p>
 * The simplest way to use this discretisation procedure is as follows:
 * <pre>
 * OptNumClass discret = new OptNumClass(); // Create algorithm
 * discret.setDataArray(new double[] {1,1,1,2,2,2,3,3,3}); // Load data
 * discret.setMethod(discret.METHOD_OPTIMAL);
 * discret.setSubmethod(discret.SUBMETHOD_MEAN);
 * discret.setMaxClasses(3); // Set the number of classes
 * discret.fish(); // Run. May take a while!
 * int[] breaks = discret.getClassBreaks();
 * double error = discret.getPartitionError();
 * </pre>
 *
 * @version $Revision: 1.3 $, $Date: 2003/02/18 15:48:31 $
 * @author Alexandr Savinov (GMD)
 */
public class OptNumClass {
	public static int MAX_VALUES = 1000000; // The maximum number of points
	public static int MAX_CLASSES = 1000; // The maximum number of generated discrete values for one variable

	/**
	 * Fisher algorithm for optimal classification according to the current quality measure (submethod).
	 */
	public static final int METHOD_OPTIMAL = 1; // Fisher

	/**
	 * According to this submethod the quality (error, information) in the
	 * class is calculated as the sum of squares of deviations from the center
	 * Cx calcualted as the class mean value: Sum(x-Cx)^2, Cx=Sum(x)/n.
	 */
	public static final int SUBMETHOD_MEAN = 1;
	/**
	 * According to this submethod the quality (error, information) in the class
	 * is calculated as the sum of absolute deviations from the center Cx calculated
	 * as the class median: Sum(x-Cx). The median is equal to the value
	 * which breaks the class into two equal (on the sum of values) groups.
	 */
	public static final int SUBMETHOD_MEDIAN = 2;
	/**
	 *
	 */
	public static final int SUBMETHOD_INFO = 3;

////////////////////////////////////////////////////////////////////////
//

	/**
	 * A method to be used for discretisation.
	 */
	int method = METHOD_OPTIMAL;

	/**
	 * A measure to assess the quality of partitioning.
	 */
	int submethod = SUBMETHOD_MEAN;

	/**
	 * Parameter defining the minimum number of partitions (classes). The resulted partition
	 * will have at least this number of classes.
	 */
	int minClasses = 3;

	/**
	 * Parameter defining the maximum number of partitions (classes). The resulted partition
	 * will have at most this number of classes.
	 */
	int maxClasses = 7;

	/**
	 * Maximum percent of quality loss.
	 * For example, 30% means that the final partition is allowed to have
	 * as low as 30% of quality in relation to the original distribution
	 * Here we suppose that the original distribution is regarded as an ideal one and
	 * having 100% of quality while breaking it into intervals decreases its
	 * quantity of information. To disable this parameter it is necessary to
	 * set it 0 -- then it means that the partitioning with any losses of
	 * information (even very bad) will satisfy us. The value of this parameter
	 * 100 means that we do not allow for any information loss, i.e., the
	 * partitioning must contain the same information as the original distribution
	 * (it is possible for example when there exist intervals with constant
	 * distribution value).
	 */
	double minInformation = 20;

	/**
	 * If the data set is sorted. Currently we use only sorted data sets.
	 */
	protected static final boolean isDataSorted = true;

	/**
	 * Data array to analyse.
	 */
	protected double[] data;

	/**
	 * Data array with the values for the second (partitioned) dimension. If null then 1-dimensional analysis.
	 * This data array values correspond to the first array, i.e., <data[i], data2[i]> is a coordinate for one point.
	 */
	protected double[] data2;

	/**
	 * Index for the second data array which sorts its values, i.e., data[dataIndex2[0]] is the minimal value.
	 * Generated automatically when the second data array is set.
	 */
	protected int[] dataIndex2;

	/**
	 * Break index array for the second dimension. The last element is the number of classes so that the array length is the number of claases plus 1.
	 */
	protected int[] dataBreaks2;

	/**
	 * Intermidiary partition errors which are used during the analysis.
	 * The first index is the number of elements; the second index is the number of classes.
	 */
	protected double[][] errors;

	/**
	 * Intermidiary partitions which are used during the analysis. Here we store the first (bottom) element for the last class in partition.
	 * The first index is the number of elements; the second index is the number of classes.
	 */
	protected int[][] classIndexes;

	/**
	 * Class (sorted) data array which contains data elements for one (last) class and potentially
	 * may have almost the same size as the original data array. It is used to compute median.
	 */
	protected double[] classData;

	/**
	 * Used to store, e.g., the class sum of elements or the median between method calls, e.g., in <code>updateDiameter</code>.
	 */
	protected double classCenter;

	/**
	 * Used to store, e.g., the class squared sum of elements or deviation from median between method calls, e.g., in <code>updateDiameter</code>
	 */
	protected double classDiameter;

	/**
	 * The state of calculations. The last class number processed by the
	 * algorithm. If 0 then no calculations has been carried out. 1 means
	 * that we know optimal partitions on 1 class (trivial case).
	 * All calculations are finished if lastCalculatedClass=maxClasses.
	 */
	protected int lastCalculatedClass;

////////////////////////////////////////////////////////////////////////
// Construction

	/**
	 * Create algorithm object.
	 */
	public OptNumClass() {
	}

	/**
	 * Create algorithm object with the specified data set and parameters.
	 */
	public OptNumClass(double[] data) {
		setDataArray(data);
	}

////////////////////////////////////////////////////////////////////////
// Public

	/**
	 * Compute optimal partitions without thread support and data source. It is
	 * the original procedure which takes the data array and generates the
	 * array of errors and class indexes which then can be used to obtain
	 * more descriptive representation of optimal partitions.
	 * <p>
	 * The method supposes that the following parameters are set correctly:
	 * <code>data</code> array (its length is the number of elements to partition),
	 * <code>maxClasses</code> specifying the number of partitions,
	 * <code>submethod</code> specifying the semantics for the class diameter
	 * (and the total error).
	 * <p>
	 * The method results in <code>error</code> and <code>classIndexes</code>
	 * arrays containing information which can be easily used to generate
	 * optimal partitions.
	 * <p>
	 * During the calculation the method uses <code>classData[]</code> (only for median),
	 * <code>classCenter</code>, <code>classDiameter</code> to store intermediate
	 * values (in fact, they are used by diameter calculation method).
	 */
	public void fish() {
		lastCalculatedClass = 0;
		fish(maxClasses);
	}

	/**
	 * Compute optimal partitions for specified class number.
	 * endClass means the number of classes to find the optimal partition for.
	 * 0 means creating necessary data structures only. 1 means partitioning
	 * on 1 class (trivial operation). 2 means finding optimal partitions
	 * on 2 classes and so on. The maximum value is endClass=maxClasses.
	 */
	public void fish(int endClass) {
		if (data == null || data.length < 2 || data.length > MAX_VALUES)
			return;
		if (maxClasses > MAX_CLASSES)
			return;
		if (endClass > maxClasses)
			return;
		if (endClass <= lastCalculatedClass) //lastCalculatedClass = 0;
			return;

		int top, bot, cl;
		double diam, err;

		//
		// Initialise auxiliary arrays
		//

		if (lastCalculatedClass == 0) {
			// Allocate memory for auxiliary array for sorted class data (to calculate class median)
			if (submethod == SUBMETHOD_MEDIAN && !isDataSorted && (classData == null || classData.length < data.length)) {
				classData = new double[data.length];
			}

			if (classIndexes == null || classIndexes.length < data.length) {
				classIndexes = new int[data.length][maxClasses]; // Allocate (a lot of) memory for last class indexex
			}

			if (errors == null || errors.length < data.length) {
				errors = new double[data.length][maxClasses]; // Allocate (a lot of) memory for errors
			}

			if (endClass > 0) {
				//
				// 0. Partition on one class (the worst one) is equal to the diameter of the whole data set
				//
				for (top = 0; top < data.length; top++) {
					errors[top][0] = computeDiameter(0, top + 1);
					classIndexes[top][0] = 0;
					// Partition on more then one class is unknown, i.e., supposed to be worse than the current best
					for (cl = 1; cl < maxClasses; cl++) {
						errors[top][cl] = Double.MAX_VALUE; // Some huge number specifying maximal possible error
						classIndexes[top][cl] = 0;
					}
				}
				lastCalculatedClass = 1; // Remember that we already calculated optimal 1 class partitions
			}
		}

		if (endClass <= lastCalculatedClass)
			return;

		//
		// 1. The primary loop on the number of elements to be partitioned, e.g., first we partition size=2 elements, then size=3, size=4 and so on up to the necessary number of data.length elements
		//
		for (top = 1; top < data.length; top++) { // [0>123456789]...[01234>56789]...[012345678>9]
			//
			// 2. The secondary loop on the size of the last class in the partition, i.e., first we check the last class consisting of 1 elements, then 2 elements and so on up to the largest possible class.
			//    The class is defined by its first element (bot) while the last element is always the same and is just before size
			//
			for (bot = top - 1; bot >= 0; bot--) { // [0123<4>56789]...[01<234>56789]...[<01234>56789]

				// 1.2.1. Compute the diameter of the last class with the first element index (bot+1), the last element index (top), the next after last element index (top+1), and  the size (top-bot)
				diam = updateDiameter(bot + 1, top + 1);

				// 1.2.2. The loop on the number of classes for each of which we compute its error and remember it if it is the minimal one
				for (cl = lastCalculatedClass/*1*/; cl < endClass/*maxClasses*/; cl++) {

					// 1.2.2.1. Compute the error recursively (i.e., for cl via cl-1)
					err = errors[bot][cl - 1] + diam;

					// 1.2.2.2. If the error is smaller then the current best error then remember this partition as the currently best one
					if (err < errors[top][cl]) {
						errors[top][cl] = err;
						classIndexes[top][cl] = bot + 1;
					}
				}
			}
		}

		lastCalculatedClass = endClass;
		classData = null;
	}

	/**
	 * Get the current data array.
	 */
	public double[] getDataArray() {
		return data;
	}

	/**
	 * Set the current data array to be analysed.
	 */
	public void setDataArray(double[] data) {
		if (data == null || data.length < 2 || data.length > MAX_VALUES)
			return;

		this.data = data;

		// (Re)initialise some additional data structures
		if (submethod == SUBMETHOD_MEDIAN && !isDataSorted && (classData == null || classData.length < data.length)) {
			classData = new double[data.length];
		} else {
			classData = null;
		}

		// Check parameters
		if (maxClasses > data.length) {
			maxClasses = data.length;
			if (minClasses > maxClasses) {
				minClasses = maxClasses;
			}
		}

		lastCalculatedClass = 0;
	}

	/**
	 * Get the current data 2 array (the second dimension).
	 */
	public double[] getDataArray2() {
		return data2;
	}

	/**
	 * Set the current data 2 array (the second dimension).
	 */
	public void setDataArray2(double[] data2) {
		if (data2 == null || data2.length != data.length)
			return;

		this.data2 = data2;

		// (Re)initialise data index
		if (dataIndex2 == null || dataIndex2.length != data2.length) {
			dataIndex2 = new int[data2.length];
		}
		int i;
		for (i = 0; i < dataIndex2.length; i++) {
			dataIndex2[i] = i;
		}
		boolean isSorted = false;
		int a;
		while (!isSorted) {
			isSorted = true;
			for (i = 1; i < dataIndex2.length; i++) {
				if (data[dataIndex2[i]] < data[dataIndex2[i - 1]]) {
					a = dataIndex2[i - 1];
					dataIndex2[i - 1] = dataIndex2[i];
					dataIndex2[i] = a;
					isSorted = false;
				}
			}
		}
	}

	/**
	 * Get the current data 2 array (the second dimension) breaks.
	 * The last element is the number of classes.
	 */
	public int[] getDataBreaks2() {
		return dataBreaks2;
	}

	/**
	 * Set the current data 2 array (the second dimension) break indexes.
	 * The lase element is the number of classes.
	 */
	public void setDataBreaks2(int[] dataBreaks2) {
		if (dataBreaks2 == null || dataBreaks2.length > maxClasses + 1 || dataBreaks2.length < 2)
			return;
		if (dataBreaks2.length > data2.length + 1)
			return;

		this.dataBreaks2 = dataBreaks2;
	}

	/**
	 * Get the current method.
	 */
	public int getMethod() {
		return method;
	}

	/**
	 * Set the current method to be used for analysis.
	 */
	public void setMethod(int method) {
		this.method = method;
		lastCalculatedClass = 0;
	}

	/**
	 * Get the current submethod used for analysis. The submethod defines
	 * the error semantics (formula) for the partition quality.
	 */
	public int getSubmethod() {
		return submethod;
	}

	/**
	 * Set the current submethod to be used for analysis. The submethod defines
	 * the error semantics (formula) for the partition quality.
	 */
	public void setSubmethod(int submethod) {
		this.submethod = submethod;

		// (Re)initialise some additional data structures
		if (submethod == SUBMETHOD_MEDIAN && !isDataSorted && (classData == null || classData.length < data.length)) {
			classData = new double[data.length];
		} else {
			classData = null;
		}

		lastCalculatedClass = 0;
	}

	/**
	 * Get mininum number of classes. The process stops as this minimum number
	 * of classes is riched provided that all other conditions are satisfied
	 * (normally, the quality of the partition).
	 */
	public int getMinClasses() {
		return minClasses;
	}

	/**
	 * Get mininum number of classes. The process stops as this minimum number
	 * of classes is riched provided that all other conditions are satisfied
	 * (normally, the quality of the partition).
	 */
	public void setMinClasses(int minClasses) {
		this.minClasses = minClasses;
	}

	/**
	 * Get maximum number of classes. The process stops whenever this number
	 * is riched independent of other criteria.
	 */
	public int getMaxClasses() {
		return maxClasses;
	}

	/**
	 * Set maximum number of classes. The process stops whenever this number
	 * is riched independent of other criteria.
	 */
	public void setMaxClasses(int maxClasses) {
		this.maxClasses = maxClasses;
		lastCalculatedClass = 0;
	}

	/**
	 * Get the array of errors resulted from the analysis process.
	 */
	public double[][] getErrors() {
		return errors;
	}

	/**
	 * Get error for the optimal partition for the specified class count.
	 */
	public double getPartitionError(int classCount) {
		if (classCount > maxClasses || classCount < 1)
			return -1;
		// Just return one element of the last line of the error array corresponding to the whole data set and the specified class count
		return errors[data.length - 1][classCount - 1];
	}

	/**
	 * Get error for the optimal partition for the maximum class count.
	 */
	public double getPartitionError() {
		return getPartitionError(maxClasses);
	}

	/**
	 * Get error array for all partitions with the class number less than or equal to the maximum.
	 */
	public double[] getPartitionErrors() {
		double[] classErrors = new double[maxClasses];
		int classCount;

		for (classCount = 1; classCount <= maxClasses; classCount++) {
			classErrors[classCount - 1] = getPartitionError(classCount);
		}

		return classErrors;
	}

	/**
	 * Get the specified partition error. The partition is specified by a number
	 * of break indexes (0 based). The length of this array must be equal to the
	 * number of classes in the partition plus 1. Each break specifies the very
	 * first element index of the corresponding class. The last element is the number
	 * of data elements. The error is calculated according to the current semantics
	 * set by the type of submethod (mean, median etc.).
	 * <p>
	 * The parameter array length is classCount+1.
	 */
	public double getPartitionError(int[] breakIndexes) {
		if (breakIndexes.length < 2 || breakIndexes.length > data.length + 1)
			return -1;

		double error = 0;
		for (int i = 0; i < breakIndexes.length - 1; i++) {
			if (breakIndexes[i] >= breakIndexes[i + 1]) {
				continue;
			}
			error += computeDiameter(breakIndexes[i], breakIndexes[i + 1]);
		}

		return error;
	}

	/**
	 * Calculate error of the specified partition described by a number of break values.
	 * <p>
	 * The parameter array lenght is classCount+1;
	 */
	public double getPartitionError(float[] breakValues) {
		if (breakValues.length < 1 || breakValues.length > data.length)
			return -1;
		return getPartitionError(breakValuesToIndexes(breakValues));
	}

	/**
	 * Get the array of class indexes resulted from the analysis process.
	 */
	public int[][] getClassIndexes() {
		return classIndexes;
	}

	/**
	 * Get class optimal partition index.
	 * <p>
	 * This method has to be called after the optimal partition into classes has
	 * been found, e.g., after the <code>fish</code> method.
	 * It uses <code>classIndexes</code> to find the first element (0-based)
	 * index for the class with the specified number. The class number must be less
	 * than <code>maxClasses</code> parameter.
	 */
	public int getClassBreak(int classIndex, int classCount) {
		if (classCount > maxClasses || classCount < 1)
			return -1;
		if (classIndex >= classCount || classIndex < 0)
			return -1;

		int i, bot = data.length - 1;
		for (i = classCount - 1; i > classIndex; i--)
			if (bot > 0) {
				bot = classIndexes[bot][i] - 1;
			}
		return (bot > 0) ? classIndexes[bot][i] : 0;
	}

	/**
	 * Get optimal partition indexes for all classes, i.e., get class breaks.
	 * The number of classes is specified by the parameter and must be less than
	 * or equal to the maximal number of classes (class parameter set by
	 * <code>setMaxClasses</code> method).
	 * <p>
	 * This method has to be called
	 * after the optimal partition into classes has been found, e.g., after the
	 * <code>fish</code> method. It uses <code>classIndexes</code> to find the
	 * first element (0-based) index for the class with the specified number.
	 * The class number must be less than <code>maxClasses</code> parameter.
	 * <p>
	 * The return array length is classCount+1.
	 */
	public int[] getClassBreaks(int classCount) {
		if (classCount < 1)
			return null;
		if (classCount > maxClasses) {
			classCount = maxClasses;
		}
		int[] breakIndexes = new int[classCount + 1];
		int classIndex;

		for (classIndex = 0; classIndex < classCount; classIndex++) {
			breakIndexes[classIndex] = getClassBreak(classIndex, classCount);
		}

		// Last element
		breakIndexes[classCount] = data.length;

		return breakIndexes;
	}

	/**
	 * Get optimal partition indexes for the maximal number of  classes.
	 * <p>
	 * The return array length is classCount+1.
	 */
	public int[] getClassBreaks() {
		return getClassBreaks(maxClasses);
	}

	/**
	 *
	 */
	public float[] getClassBreakValues() {
		return breakIndexesToValues(getClassBreaks());
	}

	public float[] getClassBreakValues(int classCount) {
		return breakIndexesToValues(getClassBreaks(classCount));
	}

////////////////////////////////////////////////////////////////////////
// Protected

	/**
	 * Compute diameter of the class defined by its first element index and next
	 * after last element index depending on the current parameters (top-bot=size).
	 * The class diameter means some measure describing the degree of deviation
	 * of this class values from their center (quantity of information).
	 * The measure is defined by <code>submethod</code> value (information, mean, median).
	 * In fact, this procedure defines the semantics of the algorithm.
	 * <p>
	 * The method does not calculate the diameter in loop for all class members.
	 * It supposes that only one new element is added each time the method
	 * is called except for the very first case where the class consist of a single element (<code>top-bot=1</code>).
	 * Thus it stores the previous class diameter in a static variable and only
	 * updates it taking into account the value of the new element (bot).
	 * (The <code>top</code> index is for the purpose of determining when it is necessary to reset the static variable and for some measures to calculate the class size.)
	 * In this sense this is the procedure for updating the diameter.
	 *
	 * @param bot <code>int</code> 0-based index of the first element in the class
	 * @param top <code>int</code> 0-based index of the next after last element in the class (the last index is <code>top-1</code>)
	 * @return the class diameter
	 */
	double updateDiameter(int bot, int top) {
		int i, n = top - bot, k = n / 2;
		switch (submethod) {
		case SUBMETHOD_INFO:
			if (top - bot == 1) {
				// Initialise intermediate variables (the class consists of a single element so reset the variables)
				classCenter = 0;
				classDiameter = 0;
			}
			// Update the variables according to the value of the new (very first) element with the bot index. Here we use the formula: I = n*log( Sum xi/n ) - n*Sum(xi*log xi) / Sum xi
			classCenter += data[bot];
			classDiameter += data[bot] * Math.log(data[bot]);
			return -(n * Math.log(classCenter / n) - n * classDiameter / classCenter);
		case SUBMETHOD_MEAN:
			if (top - bot == 1) {
				// Initialise intermediate variables (the class consists of a single element so reset the variables)
				classCenter = 0;
				classDiameter = 0;
			}
			// Update the variables according to the value of the new (very first) element with the bot index. Here we use well known formula: E(x-Ex)^2 = Ex^2-E^2x
			classCenter += data[bot];
			classDiameter += data[bot] * data[bot];
			return classDiameter - (classCenter * classCenter) / (top - bot);
		case SUBMETHOD_MEDIAN:
			double z = data[bot];

			if (isDataSorted) {
				// Find diameter
				if (n % 2 == 1) { // n is odd
					if (n == 1) {
						classDiameter = 0; // Initialise intermediate ("static") variables
					}
					classCenter = data[bot + k];
					classDiameter += Math.abs(classCenter - z);
				} else { // n%2 == 0, i.e., n is even
					classDiameter += Math.abs(classCenter - z);
					classCenter = (data[bot + k - 1] + data[bot + k]) * 0.5;
				}
				return classDiameter;
			} else {
				// 1. Insert the new data[bot] element to the auxiliary sorted array according to its value
				for (i = n - 2; i >= 0 && classData[i] > z; i--) {
					classData[i + 1] = classData[i];
				}
				classData[i + 1] = z;
				// 2. Find diameter
				if (n % 2 == 1) { // n is odd
					if (n == 1) {
						classDiameter = 0; // Initialise intermediate ("static") variables
					}
					classCenter = classData[k];
					classDiameter += Math.abs(classCenter - z);
				} else { // n%2 == 0, i.e., n is even
					classDiameter += Math.abs(classCenter - z);
					classCenter = (classData[k - 1] + classData[k]) * 0.5;
				}
				return classDiameter;
			}
		default:
			return 0;
		}
	}

	/**
	 * Compute diameter of the class defined by its first element index and next
	 * after last element index depending on the current parameters.
	 * The class diameter means some measure describing the degree of deviation
	 * of this class values from their center.
	 * The measure is defined by <code>submethod</code> value (information, mean, median).
	 * In fact, this procedure defines the semantics of the algorithm.
	 * <p>
	 * The method calculates the diameter in loop for all class members.
	 *
	 * @param bot <code>int</code> 0-based index of the first element in the class
	 * @param top <code>int</code> 0-based index of the next after last element in the class (the last index is <code>top-1</code>)
	 * @return the class diameter
	 */
	double computeDiameter(int bot, int top) {
		int i, n = top - bot;
		switch (submethod) {
		case SUBMETHOD_INFO:
			classCenter = 0;
			classDiameter = 0;
			for (i = bot; i < top; i++) {
				classCenter += data[i];
				classDiameter += data[i] * Math.log(data[i]);
			}
			// Update Here we use the formula: I = n*log( Sum xi/n ) - n*Sum(xi*log xi) / Sum xi
			classDiameter = n * Math.log(classCenter / n) - n * classDiameter / classCenter;
			classCenter /= n;
			return -classDiameter;
		case SUBMETHOD_MEAN:
			classCenter = 0;
			classDiameter = 0;
			for (i = bot; i < top; i++) {
				classCenter += data[i];
				classDiameter += data[i] * data[i];
			}
			classDiameter -= (classCenter * classCenter) / n;
			classCenter /= n;
			return classDiameter;
		case SUBMETHOD_MEDIAN:
			int k = n / 2;
			if (isDataSorted) {
				// Find diameter directly on data set since it is already sorted
				if (n % 2 == 1) { // n is odd
					classCenter = data[bot + k];
				} else { // n%2 == 0, i.e., n is even
					classCenter = (data[bot + k - 1] + data[bot + k]) * 0.5;
				}
				classDiameter = 0;
				for (i = bot; i < top; i++) {
					classDiameter += Math.abs(data[i] - classCenter);
				}
				return classDiameter;
			} else {
				// Copy all class elements into the auxiliary array and sort them
				for (i = 0; i < n; i++) {
					classData[i] = data[bot + i];
				}
//        Arrays.sort(classData, 0, n);
				// 2. Find diameter
				if (n % 2 == 1) { // n is odd
					classCenter = classData[k];
				} else { // n%2 == 0, i.e., n is even
					classCenter = (classData[k - 1] + classData[k]) * 0.5;
				}
				classDiameter = 0;
				for (i = 0; i < n; i++) {
					classDiameter += Math.abs(classData[i] - classCenter);
				}
				return classDiameter;
			}
		default:
			return 0;
		}
	}

	/**
	 * Compute diameter of the class defined by its first element index and next
	 * after last element index depending on the current parameters.
	 * The class diameter means some measure describing the degree of deviation
	 * of this class values from their center.
	 * The measure is defined by <code>submethod</code> value (information, mean, median).
	 * In fact, this procedure defines the semantics of the algorithm.
	 * <p>
	 * The class defined by two indexes includes several subclasses defined
	 * by the partition along the second dimension. The method calculates
	 * the error for all these subclasses and returns the sum as the result.
	 *
	 * @param bot <code>int</code> 0-based index of the first element in the class
	 * @param top <code>int</code> 0-based index of the next after last element in the class (the last index is <code>top-1</code>)
	 * @param classIndex2 <code>int</code> 0-based index of the subclass along the second dimension
	 * @return the class diameter
	 */
	double computeDiameter2(int bot, int top, int classIndex2) {
		int i, j, n = top - bot;

		//
		// Find subclass index
		//
		int[] subclassIndex = new int[n + 1]; // The last element is the subclass size
		j = 0;
		for (i = dataBreaks2[classIndex2]; i < dataBreaks2[classIndex2 + 1]; i++) {
			if (dataIndex2[i] < bot || dataIndex2[i] >= top) {
				continue;
			}
			subclassIndex[j] = dataIndex2[i];
			j++;
		}
		subclassIndex[n] = j;
		// Now for these elements we can calculate the error

		switch (submethod) {
		case SUBMETHOD_INFO:
		case SUBMETHOD_MEAN:
			int center = 0,
			diameter = 0,
			center2 = 0;
			for (i = 0; i < subclassIndex[n]; i++) {
				j = subclassIndex[i];
				center += data[j];
				center2 += data2[j];
			}
			center /= subclassIndex[n];
			center2 /= subclassIndex[n];

			for (i = 0; i < subclassIndex[n]; i++) {
				j = subclassIndex[i];
				diameter += (data[j] - center) * (data[j] - center) + (data2[j] - center2) * (data2[j] - center2);
			}
			diameter /= n;
			return diameter;

		case SUBMETHOD_MEDIAN:
		default:
			return 0;
		}
	}

	/**
	 * Transform break values array into the corresponding break indexes
	 * array according to the current data set.
	 * <p>
	 * Here both arrays have the length classCount+1.
	 * <p>
	 * The method supposses that the data set is sorted.
	 */
	int[] breakValuesToIndexes(float[] breakValues) {
		if (data == null)
			return null;
		if (breakValues == null || breakValues.length < 2 || breakValues.length > MAX_CLASSES + 1)
			return null;

		int[] breakIndexes = new int[breakValues.length];
		for (int i = 0; i < breakIndexes.length; i++) {
			breakIndexes[i] = data.length;
		}

		int classIndex = 0;
		for (int i = 0; i < data.length && classIndex < breakIndexes.length - 1; i++) {

			for (; classIndex < breakIndexes.length - 1 && breakValues[classIndex] <= (float) data[i]; classIndex++) {
				breakIndexes[classIndex] = i;
			}
		}

		return breakIndexes;
	}

	/**
	 * Here both arrays have the length classCount+1.
	 * <p>
	 * The method supposses that the data set is sorted.
	 */
	public float[] breakIndexesToValues(int[] breakIndexes) {
		if (data == null)
			return null;
		if (breakIndexes == null || breakIndexes.length < 2 || breakIndexes.length > MAX_CLASSES + 1)
			return null;

		float[] breakValues = new float[breakIndexes.length];

		// First value
		breakValues[0] = (float) data[0];
		int k = 1;

		for (int i = 1; i < breakIndexes.length - 1; i++)
			if (breakIndexes[i] > 0 && breakIndexes[i] < data.length) {
				breakValues[k++] = (float) (data[breakIndexes[i]] + data[breakIndexes[i] - 1]) / 2;
			}

		if (k < breakIndexes.length - 1) {
			float br[] = new float[k + 1];
			for (int i = 0; i < k; i++) {
				br[i] = breakValues[i];
			}
			breakValues = br;
		}

		// Last value
		breakValues[k] = (float) data[data.length - 1];

		return breakValues;
	}

}
