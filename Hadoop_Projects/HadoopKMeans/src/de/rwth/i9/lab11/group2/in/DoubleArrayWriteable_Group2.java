package de.rwth.i9.lab11.group2.in;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.hadoop.io.Writable;

public class DoubleArrayWriteable_Group2 implements Writable {
	public ArrayList<Double> x;

	public DoubleArrayWriteable_Group2(ArrayList<Double> x) {
		this.x = x;
	}

	public DoubleArrayWriteable_Group2() {
		this.x = new ArrayList<Double>();
	}

	public void write(DataOutput out) throws IOException {
		out.writeInt(x.size());
		
		for (Double d:this.x) {
			out.writeDouble(d);
		}
	}

	public void readFields(DataInput in) throws IOException {
		x = new ArrayList<Double>();
		
		try {
			int s = in.readInt();
			//System.err.println("NUMBER: "+s);
			for (int i = 0; i < s; i++) {
				this.x.add(in.readDouble());
				
				//System.err.println("FILED " +i+ ": "+x.get(x.size()-1));
			}
			
			//System.err.println("IKS " + x.toString());
		} catch (EOFException e) {
			// legitimate splits are assumed, EOF appears only on a linebreak
			// this assumption will probably lead to much grief
			// and it makes no sense
		}
	}

	public String toString() { 
		String s = (x.size()==0)?"":Double.toString(x.get(0));

		for (int i = 1; i < x.size(); i++) {
			s += (", "+Double.toString(x.get(i)));
		}

		return s;
	}
}
