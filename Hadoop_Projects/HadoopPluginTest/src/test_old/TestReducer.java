package test_old;

import java.io.IOException;
import java.util.Iterator;

import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reducer;
import org.apache.hadoop.mapred.Reporter;

public class TestReducer extends MapReduceBase implements Reducer {

	@Override
	public void reduce(Object key, Iterator values, OutputCollector output, Reporter reporter) throws IOException {

	}


}
