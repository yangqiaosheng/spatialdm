package edu.bonn.iai.irws10.hadoop.task2;

import java.io.IOException;
import java.util.Arrays;

import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

public class PageRank extends Configured implements Tool {

	public static class PageRankMap extends Mapper<LongWritable, Text, Text, PageInformation> {

		@Override
		public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
			String[] parts = value.toString().trim().split("\\s+");
			String pageId = parts[0].trim();
			double pagerank = Double.valueOf(parts[1]);
			String[] outlinks = Arrays.copyOfRange(parts, 2, parts.length-1);
			PageInformation info = new PageInformation(pageId, pagerank, outlinks.length);
			for (String out : outlinks) {
				context.write(new Text(out), info);
			}
		}
	}

	public static class Reduce extends Reducer<Text, PageInformation, Text, Text> {

		@Override
		public void reduce(Text key, Iterable<PageInformation> values, Context context) throws IOException, InterruptedException {
			double pagerank = 0.0;
			StringBuffer buf = new StringBuffer();
			for (PageInformation inlink : values) {
				pagerank += (inlink.getPagerank() / inlink.getOutlinks());
				buf.append(" " + inlink.getName());
			}
			buf.insert(0, pagerank);
			context.write(key, new Text(buf.toString()));
		}
	}

	public int run(String[] args) throws Exception {
		Job job = new Job(getConf());
		job.setJarByClass(PageRank.class);
		job.setJobName("pagerank");

		job.setOutputKeyClass(Text.class);
//		job.setOutputValueClass(PageInformation.class);
		job.setOutputValueClass(Text.class);

		job.setMapperClass(PageRankMap.class);
		job.setReducerClass(Reduce.class);

		job.setInputFormatClass(TextInputFormat.class);
		job.setOutputFormatClass(TextOutputFormat.class);

		FileInputFormat.setInputPaths(job, new Path(args[0]));
		FileOutputFormat.setOutputPath(job, new Path(args[1]));

		boolean success = job.waitForCompletion(true);
		return success ? 0 : 1;
	}

	public static void main(String[] args) throws Exception {
		String input  = (args.length > 0) ? args[0] : "/home/jenny/IR-data/links/";
		String output = (args.length > 1) ? args[1] : "/home/jenny/IR-data/pagerank";

		int ret = 0;
		for (int i = 0 ; i < 10 ; i++) {
			ret += ToolRunner.run(new PageRank(), new String[] { input, output + i });
			input = output + i;
		}
		ret += ToolRunner.run(new PageRank(), new String[] { input, output });

		System.exit(ret);
	}
}