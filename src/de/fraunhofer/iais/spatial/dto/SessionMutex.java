package de.fraunhofer.iais.spatial.dto;

import java.util.Date;

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
