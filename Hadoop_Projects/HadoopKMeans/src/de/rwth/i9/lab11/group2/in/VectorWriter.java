package de.rwth.i9.lab11.group2.in;

import java.io.IOException;

import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.mapreduce.RecordWriter;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class VectorWriter extends RecordWriter<IntWritable, DoubleArrayWriteable_Group2> {
	
	FSDataOutputStream out;
	
	
	public VectorWriter(TaskAttemptContext job) throws IOException {
		Path outputPath = FileOutputFormat.getOutputPath(job);
		FileSystem filesystem = outputPath.getFileSystem(job.getConfiguration());
	    outputPath = new Path(outputPath.toUri().toString() + "/" + job.getTaskAttemptID() + ".csv");
		this.out = filesystem.create(outputPath);
	}
	
	@Override
	public void close(TaskAttemptContext context) throws IOException,
			InterruptedException {
		out.close();
	}

	@Override
	public void write(IntWritable key, DoubleArrayWriteable_Group2 value)
			throws IOException, InterruptedException {
		out.writeBytes(Integer.toString(key.get()));
		out.writeBytes(":");
		boolean first = true;
		for(Double d : value.x) {
			if(!first)
				out.writeBytes(";");
			first = false;
			out.writeBytes(d.toString());
		}
		out.writeBytes("\n");
		
	}
}
