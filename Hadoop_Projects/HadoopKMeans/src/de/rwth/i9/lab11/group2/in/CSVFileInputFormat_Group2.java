package de.rwth.i9.lab11.group2.in;

import java.io.IOException;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;

public class CSVFileInputFormat_Group2 extends
FileInputFormat<Text, DoubleArrayWriteable_Group2> {

	@Override
	public RecordReader<Text, DoubleArrayWriteable_Group2> createRecordReader(
			InputSplit input, TaskAttemptContext context) throws IOException,
			InterruptedException {
		//reporter.setStatus(input.toString());
		return new VectorReader_Group2(context, (FileSplit)input);
	}

}
