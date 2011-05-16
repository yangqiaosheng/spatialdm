package edu.bonn.iai.irws10.hadoop.task1;

import java.io.IOException;
import java.util.Scanner;
import java.util.regex.Pattern;

import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

@SuppressWarnings("deprecation")
public class LinkParser extends Configured implements Tool {

	public static class LinkParserMap extends Mapper<Text, Text, Text, Text> {

		@Override
		public void map(Text key, Text value, Context context) throws IOException, InterruptedException {
			Scanner scanner = new Scanner(value.toString());
			String s;
			while ((s = scanner.findInLine("<a([^<]+)</a>")) != null) {
				int i = s.indexOf("href");
				if (i != -1) {
					String url = s.substring(i).split("\"")[1];
					// ../../../../articles/a/a/a/AAA_%28band%29_3051.html
					if (!Pattern.matches("([\\.\\./]+)articles/(.+)/(.+)\\.html", url)) {
						continue;
					}
					url = url.substring(url.lastIndexOf("/")+1);
					context.write(key, new Text(url));
				}
			}
		}
	}

	public static class LinkParserReduce extends Reducer<Text, Text, Text, Text> {

		@Override
		public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
			Text value = new Text("1.0");
			for (Text outlink : values) {
				outlink.set(" " + outlink.toString());
				value.append(outlink.getBytes(), 0, outlink.getLength());
			}
			context.write(key, value);
		}
	}

	public int run(String[] args) throws Exception {
		Job job = new Job(getConf());
		job.setJarByClass(LinkParser.class);
		job.setJobName("linkparse");

		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);

		job.setMapperClass(LinkParserMap.class);
		job.setReducerClass(LinkParserReduce.class);

		job.setInputFormatClass(WikiInputFormat.class);
		job.setOutputFormatClass(TextOutputFormat.class);

		FileInputFormat.setInputPaths(job, new Path(args[0]));
		FileOutputFormat.setOutputPath(job, new Path(args[1]));

		boolean success = job.waitForCompletion(true);
		return success ? 0 : 1;
	}

	public static void main(String[] args) throws Exception {
		String input  = (args.length > 0) ? args[0] : "IR-data/articles/";
		String output = (args.length > 1) ? args[1] : "IR-data/links/";

		int ret = ToolRunner.run(new LinkParser(), new String[] { input, output });
		System.exit(ret);
	}
}