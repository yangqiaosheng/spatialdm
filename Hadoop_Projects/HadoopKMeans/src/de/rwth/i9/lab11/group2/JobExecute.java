package de.rwth.i9.lab11.group2;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.filecache.DistributedCache;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import de.rwth.i9.lab11.group2.in.CSVFileInputFormat_Group2;
import de.rwth.i9.lab11.group2.in.CSVFileOutputFormat;
import de.rwth.i9.lab11.group2.in.DoubleArrayWriteable_Group2;

public class JobExecute {
	/** Execute stuff */
	public static void main(String[] args) throws Exception, IOException, InterruptedException, ClassNotFoundException {
		String inputPoints;

		String tmpDir = "/user/hadoop/group2/tmp_out";		

		// test			
		//inputPoints = "/user/hadoop/group2/test_data/testEx3.csv";

		// NOT test
		inputPoints = "/user/hadoop/data/exercise3/exercise.csv";

		////////////////////////////////////////////////////////////////////////////////////// party starts below

		int iterationCount = Integer.parseInt(args[0]);
		String currentCenters = args[1];

		Configuration conf = new Configuration();
		//load centers into cache from currentCenters
		Path currentCentersPath = new Path(currentCenters);
		DistributedCache.addCacheFile(currentCentersPath.toUri(), conf);

		// compute stuff
		Job job = new Job(conf, "k-Means iteration "+iterationCount);
		job.setJarByClass(JobExecute.class);
		job.setInputFormatClass(CSVFileInputFormat_Group2.class);
		job.setOutputFormatClass(CSVFileOutputFormat.class);
		job.setMapperClass(KMeansMapper.class);
		job.setReducerClass(KMeansReducer.class);
		job.setNumReduceTasks(5);

		job.setOutputKeyClass(IntWritable.class);
		job.setOutputValueClass(DoubleArrayWriteable_Group2.class);

		FileInputFormat.addInputPath(job, new Path(inputPoints));
		FileOutputFormat.setOutputPath(job, new Path(tmpDir));
		job.waitForCompletion(true);	

	}
}
