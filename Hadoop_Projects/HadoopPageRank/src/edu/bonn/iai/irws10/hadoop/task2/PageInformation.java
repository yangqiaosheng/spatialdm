package edu.bonn.iai.irws10.hadoop.task2;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.io.Writable;


public class PageInformation implements Writable {
	
	private String 	m_name;
	private double 	m_pagerank;
	private int 	m_outlinks;
	
	public PageInformation() {
		// default constructor for serialization
	}
	
	public PageInformation(String name, double pagerank, int outlinks) {
		m_name = name;
		m_pagerank = pagerank;
		m_outlinks = outlinks;
	}
	
	public String getName() {
		return m_name;
	}

	public void setName(String name) {
		m_name = name;
	}

	public double getPagerank() {
		return m_pagerank;
	}

	public void setPagerank(double pagerank) {
		m_pagerank = pagerank;
	}

	public int getOutlinks() {
		return m_outlinks;
	}

	public void setOutlinks(int outlinks) {
		m_outlinks = outlinks;
	}

	@Override
	public void write(DataOutput out) throws IOException {
		out.writeUTF(m_name);
		out.writeDouble(m_pagerank);
		out.writeInt(m_outlinks);
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		m_name = in.readUTF();
		m_pagerank = in.readDouble();
		m_outlinks = in.readInt();
	}

}
