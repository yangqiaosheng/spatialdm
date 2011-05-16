package de.rwth.i9.lab11.group2;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.filecache.DistributedCache;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

import de.rwth.i9.lab11.group2.in.DoubleArrayWriteable_Group2;

public class KMeansMapper extends Mapper<Text, DoubleArrayWriteable_Group2, IntWritable, DoubleArrayWriteable_Group2> {
	private List<DoubleArrayWriteable_Group2> centers = new ArrayList<DoubleArrayWriteable_Group2>();
	private List<Integer> centerIds = new ArrayList<Integer>();

	@Override
	public void setup(Context context) {
		try {
			/**
			 * TODO path of centers
			 */
			Path[] cacheFiles = DistributedCache.getLocalCacheFiles(context.getConfiguration());
			DoubleArrayWriteable_Group2 center;
			if (cacheFiles != null && cacheFiles.length > 0) {
				if (cacheFiles.length != 1) {
					System.err.println("Cachefilecount: " + cacheFiles.length + " is not 1!");
				}

				for (Path cachePath : cacheFiles) {
					BufferedReader lineReader = new BufferedReader(new FileReader(cachePath.toString()));

					try {
						String line;
						String[] keySplit;
						while ((line = lineReader.readLine()) != null) {
							keySplit = line.trim().split(":");
							centerIds.add(Integer.parseInt(keySplit[0].trim()));
							String[] centerString = keySplit[1].split(";");
							center = new DoubleArrayWriteable_Group2();

							for (int i = 0; i < centerString.length; i++) {
								center.x.add(Double.parseDouble(centerString[i].trim()));
							}
							centers.add(center);
						}

						// unnecessary
					} catch (EOFException e) {
						System.err.println("Unexpected EOF in Mapper.");
						// well, everything went as expected
					} catch (Exception e) {
						System.err.println("Something weng wrong, not an EOF.");

					} finally {
						lineReader.close();
					}
				}
			} else {
				System.err.println("No cache files found!");
			}
		} catch (IOException ioe) {
			System.err.println("IOException reading from distributed cache");
			System.err.println(ioe.toString());
		}
	}

	public double getDistance(DoubleArrayWriteable_Group2 a, DoubleArrayWriteable_Group2 b) {
		double sum = 0;
		for (int i = 0; i < a.x.size(); i++) {
			double diff = a.x.get(i) - b.x.get(i);
			sum += diff * diff;
		}
		return Math.sqrt(sum);
	}

	@Override
	public void map(Text key, DoubleArrayWriteable_Group2 value, Context context) throws IOException, InterruptedException {
		//assume we have ((Text)id,(DoubleArrayWriteable_Group2)value) read from cache
		// keys should be our centers, data structure center=<key(id), value>

		double minDistance = Double.POSITIVE_INFINITY;
		IntWritable minID = null;

		// find the cluster membership for the data vector

		int counter = 0;
		for (DoubleArrayWriteable_Group2 center : centers) {
			double currentDistance = getDistance(value, center);
			if (currentDistance < minDistance) {
				minDistance = currentDistance;
				minID = new IntWritable(centerIds.get(counter));
			}
			counter++;
		}

		//System.out.println("DEBUG MAPPER id: " + minID.toString() + " value " + value.toString());

		context.write(minID, value);
	}

}
