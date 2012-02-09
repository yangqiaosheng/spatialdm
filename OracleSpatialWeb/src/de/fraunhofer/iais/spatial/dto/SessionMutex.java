package de.fraunhofer.iais.spatial.dto;

import java.util.Date;

/**
 * Data Transfer Object (Value Object) which stores mutex
 * @author <a href="mailto:haolin.zhi@iais.fraunhofer.de">Haolin Zhi</a>
 * @author <a href="mailto:iulian.peca@iais.fraunhofer.de">Iulian Peca</a>
 *
 */
public class SessionMutex {
	private Date timestamp;

	public SessionMutex(Date timestamp) {
		super();
		this.timestamp = timestamp;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

}
