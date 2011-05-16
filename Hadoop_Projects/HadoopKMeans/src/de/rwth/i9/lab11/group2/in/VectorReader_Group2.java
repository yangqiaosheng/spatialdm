package de.rwth.i9.lab11.group2.in;

import java.io.IOException;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.mapreduce.lib.input.LineRecordReader;

public class VectorReader_Group2 extends RecordReader<Text, DoubleArrayWriteable_Group2> {

	private LineRecordReader lineReader;
	private Text currentKey;
	private DoubleArrayWriteable_Group2 currentValue;

	public VectorReader_Group2(TaskAttemptContext context, FileSplit split) throws IOException {
		lineReader = new LineRecordReader();

	}

	@Override
	public Text getCurrentKey() throws IOException, InterruptedException {
		return currentKey;
	}

	@Override
	public DoubleArrayWriteable_Group2 getCurrentValue() throws IOException,
	InterruptedException {
		return currentValue;
	}

	@Override
	public void initialize(InputSplit split, TaskAttemptContext context)
	throws IOException, InterruptedException {
		lineReader.initialize(split, context);
	}

	@Override
	public boolean nextKeyValue() throws IOException, InterruptedException {
		// get the next line
		if (!lineReader.nextKeyValue()) {
			return false;
		}

		currentValue = new DoubleArrayWriteable_Group2();
		currentKey = new Text();

		Text lineValue = lineReader.getCurrentValue();

		// parse the lineValue which is in the format:
		// objName, x, y, z
		String [] pieces = lineValue.toString().split(";");

		// try to parse floating point components of value
		try {
			for (int i = 0; i < pieces.length; i++) {
				currentValue.x.add(Double.parseDouble(pieces[i].trim())); // NEVER READ THIS
			}
		} catch (NumberFormatException nfe) {
			throw new IOException("Error parsing double in record");
		}

		currentKey.set("");

		return true;
	}

	@Override
	public void close() throws IOException {
		lineReader.close();
	}

	@Override
	public float getProgress() throws IOException, InterruptedException {
		return lineReader.getProgress();
	}
}
