package de.rwth.i9.lab11.group2;

import java.io.IOException;
import java.util.Iterator;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.mapreduce.Reducer;

import de.rwth.i9.lab11.group2.in.DoubleArrayWriteable_Group2;

public class KMeansReducer  extends	Reducer<IntWritable, DoubleArrayWriteable_Group2, IntWritable, DoubleArrayWriteable_Group2> {

	@Override
	public void reduce(IntWritable key, Iterable<DoubleArrayWriteable_Group2> values, Context context) throws IOException, InterruptedException {

		DoubleArrayWriteable_Group2 result = new DoubleArrayWriteable_Group2();
	
		boolean init = true;
		int numberOfPoints = 0;
		
		DoubleArrayWriteable_Group2 value;

		Iterator<DoubleArrayWriteable_Group2> iter = values.iterator();
		
		// idea is to calculate everything within the resulting DoubleArrayWriteable
		while (iter.hasNext()) {
			value = iter.next();
			//System.err.println("DEBUG REDUCER id: " + key.toString() + " value " + value.toString());
			
			if (init) {
				for (int i = 0; i < value.x.size(); i++) {
					result.x.add(0.0);
				}
				
				init = false;
			}
			
			
			for(int i = 0; i < value.x.size(); i++) {
				result.x.set(i, result.x.get(i) + value.x.get(i));
				//System.out.println( i+ " result " + result.x.get(i));//DEBUG
				//System.out.println( i+ " value " + value.x.get(i));			//DEBUG	
			}			
			numberOfPoints++;			
		}

		// divide each element
		for(int i = 0; i < result.x.size(); i++) {
			result.x.set(i, result.x.get(i) / numberOfPoints);
		}
		
		context.write(key, result);
	}

}
