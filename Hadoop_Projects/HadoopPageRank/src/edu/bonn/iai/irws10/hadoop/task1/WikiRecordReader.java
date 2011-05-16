package edu.bonn.iai.irws10.hadoop.task1;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.util.LineReader;

public class WikiRecordReader extends RecordReader<Text, Text> {

	private Text m_key;
	private Text m_value;
	private LineReader m_reader;
	private FSDataInputStream m_in;
	private long m_length;

	@SuppressWarnings("deprecation")
	@Override
	public void initialize(InputSplit split, TaskAttemptContext context) throws IOException, InterruptedException {
		Configuration conf = context.getConfiguration();
		FileSplit fileSplit = (FileSplit) split;
		Path filePath = fileSplit.getPath();
		FileSystem fs = filePath.getFileSystem(conf);
		m_in = fs.open(filePath);
		m_reader = new LineReader(m_in, conf);
		m_key = new Text(filePath.getName());
		m_value = new Text();
		m_length = fileSplit.getLength();
	}

	@Override
	public boolean nextKeyValue() throws IOException, InterruptedException {
		return m_reader.readLine(m_value) > 0;
	}

	@Override
	public Text getCurrentKey() throws IOException, InterruptedException {
		return m_key;
	}

	@Override
	public Text getCurrentValue() throws IOException, InterruptedException {
		return m_value;
	}

	@Override
	public float getProgress() throws IOException, InterruptedException {
		return Math.min(1.0f, (m_in.getPos()) / (float)(m_length));
	}

	@Override
	public void close() throws IOException {
		if (m_reader != null) {
			m_reader.close();
		}
	}

}
