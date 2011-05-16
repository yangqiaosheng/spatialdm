package edu.bonn.iai.irws10.hadoop.task1;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.JobContext;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;

public class WikiInputFormat extends FileInputFormat<Text, Text> {

	@Override
	public RecordReader<Text, Text> createRecordReader(InputSplit split, TaskAttemptContext context) throws IOException, InterruptedException {
		return new WikiRecordReader();
	}

	@Override
	public List<InputSplit> getSplits(JobContext job) throws IOException {
		List<InputSplit> splits = new LinkedList<InputSplit>();
		for (InputSplit split : super.getSplits(job)) {
			Path path = ((FileSplit)split).getPath();
			FileSystem fs = path.getFileSystem(job.getConfiguration());
			if (fs.getFileStatus(path).isDir()) {
				for (Path child : getAllFiles(path.makeQualified(fs).toUri())) {
					FileStatus childStatus = fs.getFileStatus(child);
					long length = childStatus.getLen();
					splits.add(new FileSplit(child, 0, length, fs.getFileBlockLocations(childStatus, 0, length)[0].getHosts()));
				}
			} else {
				splits.add(split);
			}
		}
		return splits;
	}

	private Collection<Path> getAllFiles(URI directory) {
		List<Path> files = new LinkedList<Path>();
		for (File child : new File(directory).listFiles()) {
			if (child.isDirectory()) {
				files.addAll(getAllFiles(child.toURI()));
			} else {
				files.add(new Path(child.getAbsolutePath()));
			}
		}
		return files;
	}

}