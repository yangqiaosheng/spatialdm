package esda_main;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LogStream extends OutputStream {
	protected ByteArrayOutputStream os = null;
	protected FileOutputStream fos = null;
	protected PrintWriter wr = null;
	protected long maxL = 1024 * 1000;
	protected long currL = 0;
	protected String lastTs = null;

	public LogStream() throws IOException {
		fos = new FileOutputStream("commongis.log");
		os = new ByteArrayOutputStream(60);
		wr = new PrintWriter(os);
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		Date ts = new java.util.Date(System.currentTimeMillis());
		String tstr = new SimpleDateFormat("[dd.MM.yy HH:mm:ss]\n").format(ts);
		os.reset();
		wr.print(tstr);
		wr.flush();

		byte bt[] = os.toByteArray();
		if (len > 2 && !tstr.equals(lastTs)) {
			fos.write(bt, 0, bt.length);
			currL += bt.length;
		}
		lastTs = tstr;
		fos.write(b, off, len);
		currL += len;
		if (currL > maxL) {
			currL = 0;
			// fos.close();
			// fos = new FileOutputStream("commongis.log");
		}

	}

	/**
	 * Closes this output stream and releases any system resources associated with this stream.
	 *
	 * @throws IOException if an I/O error occurs.
	 */
	@Override
	public void close() throws IOException {
		fos.close();
	}

	@Override
	public void flush() throws IOException {
		fos.flush();
	}

	@Override
	public void write(byte[] b) throws IOException {
		fos.write(b);

	}

	@Override
	public void write(int b) throws IOException {
		fos.write(b);
	}

}
