package de.fraunhofer.iais.spatial.exception;

/**
 * Exception which wraps a error where a job times out.
 *
 * @author <a href="mailto:haolin.zhi@iais.fraunhofer.de">Haolin Zhi</A>
 * @author <a href="mailto:iulian.peca@iais.fraunhofer.de">Iulian Peca</a>
 */
public class JobTimeOutException extends RuntimeException {

	private static final long serialVersionUID = 8538935887846589728L;

	public JobTimeOutException(String message) {
		super(message);
	}
}
