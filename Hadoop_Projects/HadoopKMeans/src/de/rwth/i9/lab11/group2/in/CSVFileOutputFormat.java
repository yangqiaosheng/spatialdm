package de.rwth.i9.lab11.group2.in;

import java.io.IOException;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.mapreduce.RecordWriter;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class CSVFileOutputFormat extends
FileOutputFormat<IntWritable, DoubleArrayWriteable_Group2> {

	@Override
	public RecordWriter<IntWritable, DoubleArrayWriteable_Group2> getRecordWriter(
			TaskAttemptContext job) throws IOException, InterruptedException {
		return new VectorWriter(job);
	}

}
