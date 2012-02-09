package de.fraunhofer.iais.spatial.exception;

/**
 * Exception which wraps a error where user input a illegal parameter.
 *
 * @author <a href="mailto:haolin.zhi@iais.fraunhofer.de">Haolin Zhi</A>
 * @author <a href="mailto:iulian.peca@iais.fraunhofer.de">Iulian Peca</a>
 */
public class IllegalInputParameterException extends RuntimeException {

	private static final long serialVersionUID = 2454675184794497639L;

	public IllegalInputParameterException(String message) {
		super(message);
	}
}
