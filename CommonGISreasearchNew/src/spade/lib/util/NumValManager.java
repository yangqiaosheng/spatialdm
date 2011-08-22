package spade.lib.util;

public class NumValManager {
	/**
	* Sorts an array of float values
	*/
	/*
	public static void sort (float values[], int nval) {
	  if (values==null) return;
	  if (nval<2) return;
	  for (int i=0; i<nval-1; i++)
	    if (values[i]>values[i+1]) {
	      float f=values[i];
	      values[i]=values[i+1];
	      values[i+1]=f;
	      for (int j=i; j>0; j--)
	        if (values[j-1]>values[j]) {
	          f=values[j];
	          values[j]=values[j-1];
	          values[j-1]=f;
	        }
	        else break;
	    }
	}
	*/
	//Quick numbers sorting
	public static void sort(float values[], int nval) {
		QSortAlgorithm.sort(values);
	}

	public static void sort(double values[], int nval) {
		QSortAlgorithm.sort(values);
	}

	/**
	* The function returns an array of integers that are numbers of the array
	* elements put in ascending order.
	* to be checked ! should produce 0 order for all NaN values
	*/
	/*
	public static int[] getOrder (FloatArray v) {
	  if (v==null) return null;
	  int num=0;
	  for (int i=0; i<v.size(); i++)
	    if (!Float.isNaN(v.elementAt(i))) ++num;
	  if (num<1) return null;
	  int ord[]=new int[num];
	  num=0;
	  for (int i=0; i<v.size(); i++)
	    if (!Float.isNaN(v.elementAt(i))) {
	      float fl=v.elementAt(i);
	      int idx=-1;
	      for (int j=num-1; j>=0 && idx<0; j--)
	        if (fl>=v.elementAt(ord[j])) idx=j+1;
	      if (idx<0) idx=0;
	      for (int j=num-1; j>=idx; j--) ord[j+1]=ord[j];
	      ord[idx]=i; ++num;
	    }
	  return ord;
	}
	*/

	/**
	* Breaks an array of float numbers into the specified number of equal
	* frequency intervals. When nIntervals==4, we receive the median and
	* quartiles.
	* if AdjustBreaks is selected, breaks will be, possibly, decreased,
	* to ensure that they are not equal to values
	*/
	public static float[] breakToIntervals(float values[], int nIntervals, boolean AdjustBreaks) {
		if (values == null || values.length == 0)
			return null;
		int nval = values.length;
		sort(values, nval);
		if (nIntervals < 1) {
			nIntervals = 1;
		}
		if (nIntervals > nval) {
			nIntervals = nval;
		}
		//if (nIntervals>nval) nIntervals=nval;
		float[] breaks = new float[nIntervals + 1];
		breaks[0] = values[0];
		breaks[nIntervals] = values[nval - 1];
		if (nIntervals == 1)
			return breaks;
		float nInClass = (float) nval / nIntervals, ntot = nInClass;
		for (int i = 1; i < nIntervals; i++) {
			breaks[i] = Float.NaN;
			int k = (int) Math.floor(ntot);
			if (AdjustBreaks) {
				int kk = k - 1;
				for (int j = k - 1; j > 0; j--)
					if (values[j] != values[k]) {
						kk = j;
						break;
					}
				breaks[i] = (values[kk] + values[k]) / 2;
			} else {
				if (k > 0 && k >= Math.round(ntot)) {
					breaks[i] = (values[k - 1] + values[k]) / 2;
				} else {
					breaks[i] = values[k];
				}
			}
			ntot += nInClass;
		}
		return breaks;
	}

	public static float[] breakToIntervals(FloatArray fa, int nIntervals, boolean AdjustBreaks) {
		float values[] = new float[fa.size() - getNofNaN(fa)];
		int j = -1;
		for (int i = 0; i < fa.size(); i++)
			if (!Float.isNaN(fa.elementAt(i))) {
				j++;
				values[j] = fa.elementAt(i);
			}
		return breakToIntervals(values, nIntervals, AdjustBreaks);
	}

	/**
	* Breaks an array of double numbers into the specified number of equal
	* frequency intervals. When nIntervals==4, we receive the median and
	* quartiles.
	* if AdjustBreaks is selected, breaks will be, possibly, decreased,
	* to ensure that they are not equal to values
	*/
	public static double[] breakToIntervals(double values[], int nIntervals, boolean AdjustBreaks) {
		if (values == null || values.length == 0)
			return null;
		int nval = values.length;
		sort(values, nval);
		if (nIntervals < 1) {
			nIntervals = 1;
		}
		if (nIntervals > nval) {
			nIntervals = nval;
		}
		//if (nIntervals>nval) nIntervals=nval;
		double[] breaks = new double[nIntervals + 1];
		breaks[0] = values[0];
		breaks[nIntervals] = values[nval - 1];
		if (nIntervals == 1)
			return breaks;
		double nInClass = nval / nIntervals, ntot = nInClass;
		for (int i = 1; i < nIntervals; i++) {
			breaks[i] = Double.NaN;
			int k = (int) Math.floor(ntot);
			if (AdjustBreaks) {
				int kk = k - 1;
				for (int j = k - 1; j > 0; j--)
					if (values[j] != values[k]) {
						kk = j;
						break;
					}
				breaks[i] = (values[kk] + values[k]) / 2;
			} else {
				if (k > 0 && k >= Math.round(ntot)) {
					breaks[i] = (values[k - 1] + values[k]) / 2;
				} else {
					breaks[i] = values[k];
				}
			}
			ntot += nInClass;
		}
		return breaks;
	}

	public static double[] breakToIntervals(DoubleArray fa, int nIntervals, boolean AdjustBreaks) {
		double values[] = new double[fa.size() - getNofNaN(fa)];
		int j = -1;
		for (int i = 0; i < fa.size(); i++)
			if (!Double.isNaN(fa.elementAt(i))) {
				j++;
				values[j] = fa.elementAt(i);
			}
		return breakToIntervals(values, nIntervals, AdjustBreaks);
	}

	/**
	* Breaks a sequence of float numbers into the specified number of equal
	* frequency intervals. When nIntervals==4, we receive the median and
	* quartiles. The float numbers are contained in a FloatArray.
	* The array order specifies the ascending order of the vector elements.
	* If this argument is null, the values are sorted within the function.
	* if AdjustBreaks is selected, breaks will be, possibly, decreased,
	* to ensure that they are not equal to values
	*/
	/*
	public static float[] breakToIntervals (FloatArray values, int order[], int nIntervals, boolean AdjustBreaks){
	  if (values==null) return null;
	  if (order==null) order=getOrder(values);
	  if (nIntervals<1) nIntervals=1;
	  int num=order.length;
	  if (nIntervals>num) nIntervals=num;
	  float breaks[]=new float[nIntervals+1];
	  breaks[0]=values.elementAt(order[0]);
	  breaks[nIntervals]=values.elementAt(order[num-1]);
	  if (nIntervals==1) return breaks;
	  float nInClass=(float)num/nIntervals, ntot=nInClass;
	  for (int i=1; i<nIntervals; i++) {
	    int k=(int)Math.floor(ntot);
	    if (AdjustBreaks) {
	      int kk=k-1;
	      for (int j=k-1; j>0; j--)
	        if (values.elementAt(order[j])!=values.elementAt(order[k])) { kk=j; break; }
	      breaks[i]=(values.elementAt(order[kk])+values.elementAt(order[k]))/2;
	    }
	    else {
	      if (k>=Math.round(ntot))
	        breaks[i]=(values.elementAt(order[k-1])+values.elementAt(order[k]))/2;
	      else
	        breaks[i]=values.elementAt(order[k]);
	    }
	    ntot+=nInClass;
	  }
	  return breaks;
	}
	*/

	public static int getNofNaN(FloatArray values) {
		int n = 0;
		for (int i = 0; i < values.size(); i++)
			if (Float.isNaN(values.elementAt(i))) {
				n++;
			}
		return n;
	}

	public static int getNofNaN(DoubleArray values) {
		int n = 0;
		for (int i = 0; i < values.size(); i++)
			if (Double.isNaN(values.elementAt(i))) {
				n++;
			}
		return n;
	}

	public static float getSum(FloatArray values) {
		float sum = 0f;
		for (int i = 0; i < values.size(); i++)
			if (!Float.isNaN(values.elementAt(i))) {
				sum += values.elementAt(i);
			}
		return sum;
	}

	public static double getSum(DoubleArray values) {
		double sum = 0f;
		for (int i = 0; i < values.size(); i++)
			if (!Double.isNaN(values.elementAt(i))) {
				sum += values.elementAt(i);
			}
		return sum;
	}

	public static float getMedian(FloatArray values) {
		return getMedian(values, false);
	}

	public static float getMedian(FloatArray values, boolean adjust) {
		float breaks[] = breakToIntervals(values, 2, adjust);
		return (breaks == null || breaks.length < 2) ? Float.NaN : breaks[1];
	}

	public static double getMedian(DoubleArray values) {
		return getMedian(values, false);
	}

	public static double getMedian(DoubleArray values, boolean adjust) {
		double breaks[] = breakToIntervals(values, 2, adjust);
		return (breaks == null || breaks.length < 2) ? Double.NaN : breaks[1];
	}

	public static float getMean(FloatArray values) {
		int nOfNaN = getNofNaN(values);
		if (values.size() == nOfNaN)
			return Float.NaN;
		else
			return getSum(values) / (values.size() - nOfNaN);
	}

	public static double getMean(DoubleArray values) {
		int nOfNaN = getNofNaN(values);
		if (values.size() == nOfNaN)
			return Double.NaN;
		else
			return getSum(values) / (values.size() - nOfNaN);
	}

	public static float getMin(FloatArray values) {
		float min = Float.NaN;
		for (int i = 0; i < values.size(); i++) {
			float v = values.elementAt(i);
			if (Float.isNaN(v)) {
				continue;
			}
			if (Float.isNaN(min) || v < min) {
				min = v;
			}
		}
		return min;
	}

	public static double getMin(DoubleArray values) {
		double min = Double.NaN;
		for (int i = 0; i < values.size(); i++) {
			double v = values.elementAt(i);
			if (Double.isNaN(v)) {
				continue;
			}
			if (Double.isNaN(min) || v < min) {
				min = v;
			}
		}
		return min;
	}

	public static float getMax(FloatArray values) {
		float max = Float.NaN;
		for (int i = 0; i < values.size(); i++) {
			float v = values.elementAt(i);
			if (Float.isNaN(v)) {
				continue;
			}
			if (Float.isNaN(max) || v > max) {
				max = v;
			}
		}
		return max;
	}

	public static double getMax(DoubleArray values) {
		double max = Double.NaN;
		for (int i = 0; i < values.size(); i++) {
			double v = values.elementAt(i);
			if (Double.isNaN(v)) {
				continue;
			}
			if (Double.isNaN(max) || v > max) {
				max = v;
			}
		}
		return max;
	}

	/**
	* computes variance of the float array
	* NaN values are ignored
	*/
	public static float getVariance(FloatArray values) {
		return getVariance(values, getMean(values));
	}

	public static float getVariance(FloatArray values, float mean) {
		int n = values.size() - getNofNaN(values);
		if (n <= 1)
			return Float.NaN;
		float variance = 0f;
		for (int i = 0; i < values.size(); i++) {
			if (Float.isNaN(values.elementAt(i))) {
				continue;
			}
			variance += Math.pow(values.elementAt(i) - mean, 2);
		}
		variance = (float) Math.sqrt(variance / (n - 1));
		return variance;
	}

	public static double getVariance(DoubleArray values) {
		return getVariance(values, getMean(values));
	}

	public static double getVariance(DoubleArray values, double mean) {
		int n = values.size() - getNofNaN(values);
		if (n <= 1)
			return Double.NaN;
		double variance = 0f;
		for (int i = 0; i < values.size(); i++) {
			if (Double.isNaN(values.elementAt(i))) {
				continue;
			}
			variance += Math.pow(values.elementAt(i) - mean, 2);
		}
		variance = Math.sqrt(variance / (n - 1));
		return variance;
	}

	public static float getStdD(FloatArray values, float mean) {
		int n = values.size() - getNofNaN(values);
		if (n == 0)
			return Float.NaN;
		float StdD = 0f;
		for (int i = 0; i < values.size(); i++) {
			if (Float.isNaN(values.elementAt(i))) {
				continue;
			}
			StdD += Math.pow(values.elementAt(i) - mean, 2);
		}
		StdD = (float) Math.sqrt(StdD / n);
		return StdD;
	}

	public static double getStdD(DoubleArray values, double mean) {
		int n = values.size() - getNofNaN(values);
		if (n == 0)
			return Double.NaN;
		double StdD = 0f;
		for (int i = 0; i < values.size(); i++) {
			if (Double.isNaN(values.elementAt(i))) {
				continue;
			}
			StdD += Math.pow(values.elementAt(i) - mean, 2);
		}
		StdD = Math.sqrt(StdD / n);
		return StdD;
	}

	public static float getCovariance(FloatArray xValues, FloatArray yValues) {
		int N = (xValues.size() < yValues.size()) ? xValues.size() : yValues.size();
		float xMean = 0f, yMean = 0f;
		int n = 0;
		for (int i = 0; i < N; i++) {
			float xv = xValues.elementAt(i), yv = yValues.elementAt(i);
			if (Float.isNaN(xv) || Float.isNaN(yv)) {
				continue;
			}
			xMean += xv;
			yMean += yv;
			n++;
		}
		if (n == 0)
			return Float.NaN;
		xMean /= n;
		yMean /= n;
		float cov = 0f;
		for (int i = 0; i < N; i++) {
			float xv = xValues.elementAt(i), yv = yValues.elementAt(i);
			if (Float.isNaN(xv) || Float.isNaN(yv)) {
				continue;
			}
			cov += (xv - xMean) * (yv - yMean);
		}
		cov /= n;
		return cov;
	}

	public static double getCovariance(DoubleArray xValues, DoubleArray yValues) {
		int N = (xValues.size() < yValues.size()) ? xValues.size() : yValues.size();
		double xMean = 0f, yMean = 0f;
		int n = 0;
		for (int i = 0; i < N; i++) {
			double xv = xValues.elementAt(i), yv = yValues.elementAt(i);
			if (Double.isNaN(xv) || Double.isNaN(yv)) {
				continue;
			}
			xMean += xv;
			yMean += yv;
			n++;
		}
		if (n == 0)
			return Double.NaN;
		xMean /= n;
		yMean /= n;
		double cov = 0f;
		for (int i = 0; i < N; i++) {
			double xv = xValues.elementAt(i), yv = yValues.elementAt(i);
			if (Double.isNaN(xv) || Double.isNaN(yv)) {
				continue;
			}
			cov += (xv - xMean) * (yv - yMean);
		}
		cov /= n;
		return cov;
	}

	// getStdD and getMean did not used because here we take into account
	// NaNs in both FloatArrays
	public static float getCorrelation(FloatArray xValues, FloatArray yValues) {
		int N = (xValues.size() < yValues.size()) ? xValues.size() : yValues.size();
		float xMean = 0f, yMean = 0f;
		int n = 0;
		for (int i = 0; i < N; i++) {
			float xv = xValues.elementAt(i), yv = yValues.elementAt(i);
			if (Float.isNaN(xv) || Float.isNaN(yv)) {
				continue;
			}
			xMean += xv;
			yMean += yv;
			n++;
		}
		if (n == 0)
			return Float.NaN;
		xMean /= n;
		yMean /= n;
		float cov = 0f, stdx = 0f, stdy = 0f;
		for (int i = 0; i < N; i++) {
			float xv = xValues.elementAt(i), yv = yValues.elementAt(i);
			if (Float.isNaN(xv) || Float.isNaN(yv)) {
				continue;
			}
			cov += (xv - xMean) * (yv - yMean);
			stdx += Math.pow(xv - xMean, 2);
			stdy += Math.pow(yv - yMean, 2);
		}
		cov /= n;
		stdx = (float) Math.sqrt(stdx / n);
		stdy = (float) Math.sqrt(stdy / n);
		return cov / (stdx * stdy);
	}

	public static double getCorrelation(DoubleArray xValues, DoubleArray yValues) {
		int N = (xValues.size() < yValues.size()) ? xValues.size() : yValues.size();
		double xMean = 0f, yMean = 0f;
		int n = 0;
		for (int i = 0; i < N; i++) {
			double xv = xValues.elementAt(i), yv = yValues.elementAt(i);
			if (Double.isNaN(xv) || Double.isNaN(yv)) {
				continue;
			}
			xMean += xv;
			yMean += yv;
			n++;
		}
		if (n == 0)
			return Double.NaN;
		xMean /= n;
		yMean /= n;
		double cov = 0f, stdx = 0f, stdy = 0f;
		for (int i = 0; i < N; i++) {
			double xv = xValues.elementAt(i), yv = yValues.elementAt(i);
			if (Double.isNaN(xv) || Double.isNaN(yv)) {
				continue;
			}
			cov += (xv - xMean) * (yv - yMean);
			stdx += Math.pow(xv - xMean, 2);
			stdy += Math.pow(yv - yMean, 2);
		}
		cov /= n;
		stdx = Math.sqrt(stdx / n);
		stdy = Math.sqrt(stdy / n);
		return cov / (stdx * stdy);
	}

	/**
	 * returns parameters M and B of y=M*x+B
	 */
	public static float[] getLinearRegression(FloatArray xValues, FloatArray yValues) {
		int N = (xValues.size() < yValues.size()) ? xValues.size() : yValues.size();
		float sx = 0f, sx2 = 0f, sy = 0f, sxy = 0f;
		int n = 0;
		for (int i = 0; i < N; i++) {
			float xv = xValues.elementAt(i), yv = yValues.elementAt(i);
			if (Float.isNaN(xv) || Float.isNaN(yv)) {
				continue;
			}
			sx += xv;
			sx2 += xv * xv;
			sy += yv;
			sxy += xv * yv;
			n++;
		}
		if (n == 0)
			return null;
		float f = n * sx2 - sx * sx;
		if (Float.isNaN(f))
			return null;
		float mb[] = new float[2];
		mb[0] = (n * sxy - sx * sy) / f;
		mb[1] = (sy * sx2 - sx * sxy) / f;
		return mb;
	}

	public static double[] getLinearRegression(DoubleArray xValues, DoubleArray yValues) {
		int N = (xValues.size() < yValues.size()) ? xValues.size() : yValues.size();
		double sx = 0f, sx2 = 0f, sy = 0f, sxy = 0f;
		int n = 0;
		for (int i = 0; i < N; i++) {
			double xv = xValues.elementAt(i), yv = yValues.elementAt(i);
			if (Double.isNaN(xv) || Double.isNaN(yv)) {
				continue;
			}
			sx += xv;
			sx2 += xv * xv;
			sy += yv;
			sxy += xv * yv;
			n++;
		}
		if (n == 0)
			return null;
		double f = n * sx2 - sx * sx;
		if (Double.isNaN(f))
			return null;
		double mb[] = new double[2];
		mb[0] = (n * sxy - sx * sy) / f;
		mb[1] = (sy * sx2 - sx * sxy) / f;
		return mb;
	}

	public static float[] smoothBack(float in[], int smoothMode, int depth) {
		float out[] = new float[in.length];
		for (int n = 0; n < out.length; n++)
			if (n < depth - 1) {
				out[n] = Float.NaN;
			} else {
				FloatArray vals = new FloatArray(depth, 1);
				for (int k = 0; k < depth; k++) {
					float vk = in[n - k];
					if (Float.isNaN(vk)) {
						vals = null;
						break;
					}
					vals.addElement(vk);
				}
				if (vals == null || vals.size() == 0) {
					out[n] = Float.NaN;
				} else {
					switch (smoothMode) {
					case SmoothingParams.SmoothAVG:
						out[n] = NumValManager.getMean(vals);
						break;
					case SmoothingParams.SmoothMEDIAN:
						out[n] = NumValManager.getMedian(vals);
						break;
					case SmoothingParams.SmoothMAX:
						out[n] = NumValManager.getMax(vals);
						break;
					case SmoothingParams.SmoothMIN:
						out[n] = NumValManager.getMin(vals);
						break;
					case SmoothingParams.SmoothMAXMIN:
						out[n] = NumValManager.getMax(vals) - NumValManager.getMin(vals);
						break;
					case SmoothingParams.SmoothSUM:
						out[n] = 0f;
						for (int k = 0; k < vals.size(); k++) {
							out[n] += vals.elementAt(k);
						}
						break;
					}
				}
			}
		return out;
	}

	public static double[] smoothBack(double in[], int smoothMode, int depth) {
		double out[] = new double[in.length];
		for (int n = 0; n < out.length; n++)
			if (n < depth - 1) {
				out[n] = Double.NaN;
			} else {
				DoubleArray vals = new DoubleArray(depth, 1);
				for (int k = 0; k < depth; k++) {
					double vk = in[n - k];
					if (Double.isNaN(vk)) {
						vals = null;
						break;
					}
					vals.addElement(vk);
				}
				if (vals == null || vals.size() == 0) {
					out[n] = Double.NaN;
				} else {
					switch (smoothMode) {
					case SmoothingParams.SmoothAVG:
						out[n] = NumValManager.getMean(vals);
						break;
					case SmoothingParams.SmoothMEDIAN:
						out[n] = NumValManager.getMedian(vals);
						break;
					case SmoothingParams.SmoothMAX:
						out[n] = NumValManager.getMax(vals);
						break;
					case SmoothingParams.SmoothMIN:
						out[n] = NumValManager.getMin(vals);
						break;
					case SmoothingParams.SmoothMAXMIN:
						out[n] = NumValManager.getMax(vals) - NumValManager.getMin(vals);
						break;
					case SmoothingParams.SmoothSUM:
						out[n] = 0f;
						for (int k = 0; k < vals.size(); k++) {
							out[n] += vals.elementAt(k);
						}
						break;
					}
				}
			}
		return out;
	}

	public static float[] smoothCentered(float in[], int smoothMode, int depth) {
		if (smoothMode != SmoothingParams.SmoothAVG || (smoothMode == SmoothingParams.SmoothAVG && depth != 2 * (depth / 2))) {
			float out[] = smoothBack(in, smoothMode, depth);
			int d2 = (int) Math.floor(depth / 2);
			for (int n = 0; n < out.length; n++)
				if (n < out.length - d2) {
					out[n] = out[n + d2];
				} else {
					out[n] = Float.NaN;
				}
			return out;
		}
		float out[] = new float[in.length];
		int d2 = (int) Math.floor(depth / 2);
		for (int n = 0; n < out.length; n++)
			if (n < d2 || n >= out.length - d2) {
				out[n] = Float.NaN;
			} else {
				out[n] = 0f;
				for (int k = n - d2; k <= n + d2 && !Float.isNaN(out[n]); k++)
					if (Float.isNaN(in[k])) {
						out[n] = Float.NaN;
					}
				if (!Float.isNaN(out[n])) {
					out[n] += in[n - d2] + in[n + d2];
					for (int k = n - d2 + 1; k <= n + d2 - 1; k++) {
						out[n] += 2 * in[k];
					}
					out[n] /= (2 + 4 * d2 + 2);
				}
			}

		return out;
	}

	public static double[] smoothCentered(double in[], int smoothMode, int depth) {
		if (smoothMode != SmoothingParams.SmoothAVG || (smoothMode == SmoothingParams.SmoothAVG && depth != 2 * (depth / 2))) {
			double out[] = smoothBack(in, smoothMode, depth);
			int d2 = (int) Math.floor(depth / 2);
			for (int n = 0; n < out.length; n++)
				if (n < out.length - d2) {
					out[n] = out[n + d2];
				} else {
					out[n] = Double.NaN;
				}
			return out;
		}
		double out[] = new double[in.length];
		int d2 = (int) Math.floor(depth / 2);
		for (int n = 0; n < out.length; n++)
			if (n < d2 || n >= out.length - d2) {
				out[n] = Double.NaN;
			} else {
				out[n] = 0f;
				for (int k = n - d2; k <= n + d2 && !Double.isNaN(out[n]); k++)
					if (Double.isNaN(in[k])) {
						out[n] = Double.NaN;
					}
				if (!Double.isNaN(out[n])) {
					out[n] += in[n - d2] + in[n + d2];
					for (int k = n - d2 + 1; k <= n + d2 - 1; k++) {
						out[n] += 2 * in[k];
					}
					out[n] /= (2 + 4 * d2 + 2);
				}
			}

		return out;
	}

	public static float[] smoothWithAccumulation(float in[], int smoothMode, int startIdx) {
		float out[] = new float[in.length];
		for (int n = 0; n < startIdx; n++) {
			out[n] = Float.NaN;
		}
		switch (smoothMode) {
		case SmoothingParams.SmoothMIN:
			for (int n = startIdx; n < out.length; n++)
				if (n == 0 || Float.isNaN(out[n - 1]) || in[n] <= out[n - 1]) {
					out[n] = in[n];
				} else {
					out[n] = out[n - 1];
				}
			break;
		case SmoothingParams.SmoothMAX:
			for (int n = startIdx; n < out.length; n++)
				if (n == 0 || Float.isNaN(out[n - 1]) || in[n] >= out[n - 1]) {
					out[n] = in[n];
				} else {
					out[n] = out[n - 1];
				}
			break;
		case SmoothingParams.SmoothMAXMIN:
			FloatArray val = new FloatArray(out.length, 1);
			for (int n = startIdx + 1; n < out.length; n++) {
				val.addElement(in[n]);
				float min = getMin(val), max = getMax(val);
				out[n] = (Float.isNaN(min) || Float.isNaN(max)) ? Float.NaN : max - min;
			}
			break;
		case SmoothingParams.SmoothSUM:
			out[startIdx] = in[startIdx];
			for (int n = startIdx + 1; n < out.length; n++) {
				out[n] = out[n - 1] + in[n];
			}
			break;
		case SmoothingParams.SmoothAVG:
			float sum = 0f;
			int N = 0;
			for (int n = startIdx; n < out.length; n++) {
				if (!Float.isNaN(in[n])) {
					sum += in[n];
					N++;
				}
				out[n] = (N == 0) ? Float.NaN : sum / N;
			}
			break;
		case SmoothingParams.SmoothMEDIAN:
			FloatArray vals = new FloatArray(out.length, 1);
			for (int n = startIdx; n < out.length; n++) {
				if (!Float.isNaN(in[n])) {
					vals.addElement(in[n]);
				}
				out[n] = (vals.size() == 0) ? Float.NaN : getMedian(vals);
			}
		}
		return out;
	}

	public static double[] smoothWithAccumulation(double in[], int smoothMode, int startIdx) {
		double out[] = new double[in.length];
		for (int n = 0; n < startIdx; n++) {
			out[n] = Double.NaN;
		}
		switch (smoothMode) {
		case SmoothingParams.SmoothMIN:
			for (int n = startIdx; n < out.length; n++)
				if (n == 0 || Double.isNaN(out[n - 1]) || in[n] <= out[n - 1]) {
					out[n] = in[n];
				} else {
					out[n] = out[n - 1];
				}
			break;
		case SmoothingParams.SmoothMAX:
			for (int n = startIdx; n < out.length; n++)
				if (n == 0 || Double.isNaN(out[n - 1]) || in[n] >= out[n - 1]) {
					out[n] = in[n];
				} else {
					out[n] = out[n - 1];
				}
			break;
		case SmoothingParams.SmoothMAXMIN:
			DoubleArray val = new DoubleArray(out.length, 1);
			for (int n = startIdx + 1; n < out.length; n++) {
				val.addElement(in[n]);
				double min = getMin(val), max = getMax(val);
				out[n] = (Double.isNaN(min) || Double.isNaN(max)) ? Double.NaN : max - min;
			}
			break;
		case SmoothingParams.SmoothSUM:
			out[startIdx] = in[startIdx];
			for (int n = startIdx + 1; n < out.length; n++) {
				out[n] = ((Double.isNaN(out[n - 1])) ? 0 : out[n - 1]) + in[n];
			}
			break;
		case SmoothingParams.SmoothAVG:
			double sum = 0f;
			int N = 0;
			for (int n = startIdx; n < out.length; n++) {
				if (!Double.isNaN(in[n])) {
					sum += in[n];
					N++;
				}
				out[n] = (N == 0) ? Double.NaN : sum / N;
			}
			break;
		case SmoothingParams.SmoothMEDIAN:
			DoubleArray vals = new DoubleArray(out.length, 1);
			for (int n = startIdx; n < out.length; n++) {
				if (!Double.isNaN(in[n])) {
					vals.addElement(in[n]);
				}
				out[n] = (vals.size() == 0) ? Double.NaN : getMedian(vals);
			}
		}
		return out;
	}

	public static int[] getOrderDecrease(float vals[]) {
		if (vals == null || vals.length == 0)
			return null;
		int order[] = new int[vals.length];
		// count NaN(s)
		int NNaN = 0;
		for (int i = 0; i < vals.length; i++) {
			if (Float.isNaN(vals[i])) {
				NNaN++;
				order[i] = vals.length + 1 - NNaN;
			} else {
				order[i] = -1;
			}
		}
		// now looking for max values
		for (int i = 0; i < vals.length - NNaN; i++) {
			float max = Float.NaN;
			int maxN = -1;
			for (int j = 0; j < vals.length; j++)
				if (order[j] == -1 && !Float.isNaN(vals[j]))
					if (Float.isNaN(max) || vals[j] > max) {
						max = vals[j];
						maxN = j;
					}
			if (maxN >= 0) {
				order[maxN] = i + 1;
				vals[maxN] = Float.NaN;
			}
		}
		return order;
	}

	public static int[] getOrderDecrease(double vals[]) {
		if (vals == null || vals.length == 0)
			return null;
		int order[] = new int[vals.length];
		// count NaN(s)
		int NNaN = 0;
		for (int i = 0; i < vals.length; i++) {
			if (Double.isNaN(vals[i])) {
				NNaN++;
				order[i] = vals.length + 1 - NNaN;
			} else {
				order[i] = -1;
			}
		}
		// now looking for max values
		for (int i = 0; i < vals.length - NNaN; i++) {
			double max = Double.NaN;
			int maxN = -1;
			for (int j = 0; j < vals.length; j++)
				if (order[j] == -1 && !Double.isNaN(vals[j]))
					if (Double.isNaN(max) || vals[j] > max) {
						max = vals[j];
						maxN = j;
					}
			if (maxN >= 0) {
				order[maxN] = i + 1;
				vals[maxN] = Double.NaN;
			}
		}
		return order;
	}

	public static int[] getOrderIncrease(float vals[]) {
		if (vals == null || vals.length == 0)
			return null;
		int order[] = new int[vals.length];
		// count NaN(s)
		int NNaN = 0;
		for (int i = 0; i < vals.length; i++) {
			if (Float.isNaN(vals[i])) {
				NNaN++;
			}
			order[i] = -1;
		}
		// now looking for min values
		for (int i = 0; i < vals.length - NNaN; i++) {
			float min = Float.NaN;
			int minN = -1;
			for (int j = 0; j < vals.length; j++)
				if (order[j] == -1 && !Float.isNaN(vals[j]))
					if (Float.isNaN(min) || vals[j] < min) {
						min = vals[j];
						minN = j;
					}
			if (minN >= 0) {
				order[minN] = i + 1;
				vals[minN] = Float.NaN;
			}
		}
		return order;
	}

	public static int[] getOrderIncrease(double vals[]) {
		if (vals == null || vals.length == 0)
			return null;
		int order[] = new int[vals.length];
		// count NaN(s)
		int NNaN = 0;
		for (int i = 0; i < vals.length; i++) {
			if (Double.isNaN(vals[i])) {
				NNaN++;
			}
			order[i] = -1;
		}
		// now looking for min values
		for (int i = 0; i < vals.length - NNaN; i++) {
			double min = Double.NaN;
			int minN = -1;
			for (int j = 0; j < vals.length; j++)
				if (order[j] == -1 && !Double.isNaN(vals[j]))
					if (Double.isNaN(min) || vals[j] < min) {
						min = vals[j];
						minN = j;
					}
			if (minN >= 0) {
				order[minN] = i + 1;
				vals[minN] = Double.NaN;
			}
		}
		return order;
	}

	public static float[] getPercentiles(FloatArray values, int perc[]) {
		float fPerc[] = new float[perc.length];
		float arVal[] = values.getTrimmedArray();
		QSortAlgorithm.sort(arVal, false);
		for (int i = 0; i < perc.length; i++) {
			float fidx = 1.0f * perc[i] * (arVal.length + 1) / 100.0f - 1;
			if (fidx < 0) {
				fidx = 0;
			} else if (fidx >= arVal.length) {
				fidx = arVal.length - 1;
			}
			int idx = (int) Math.floor(fidx), idx1 = (int) Math.ceil(fidx);
			if (idx1 >= arVal.length) {
				idx1 = arVal.length - 1;
			}
			float r = arVal[idx];
			if (idx1 > idx) {
				r += (fidx - idx) * (arVal[idx1] - arVal[idx]);
			}
			fPerc[i] = r;
		}
		return fPerc;
	}

	public static double[] getPercentiles(DoubleArray values, int perc[]) {
		double fPerc[] = new double[perc.length];
		double arVal[] = values.getTrimmedArray();
		QSortAlgorithm.sort(arVal, false);
		for (int i = 0; i < perc.length; i++) {
			double fidx = 1.0f * perc[i] * (arVal.length + 1) / 100.0f - 1;
			if (fidx < 0) {
				fidx = 0;
			} else if (fidx >= arVal.length) {
				fidx = arVal.length - 1;
			}
			int idx = (int) Math.floor(fidx), idx1 = (int) Math.ceil(fidx);
			if (idx1 >= arVal.length) {
				idx1 = arVal.length - 1;
			}
			double r = arVal[idx];
			if (idx1 > idx) {
				r += (fidx - idx) * (arVal[idx1] - arVal[idx]);
			}
			fPerc[i] = r;
		}
		return fPerc;
	}

}
