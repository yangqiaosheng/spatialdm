package de.fraunhofer.iais.spatial.dto;

public class SessionMutex {
	private String histrogramSessionId;

	public SessionMutex(String histrogramSessionId) {
		this.histrogramSessionId = histrogramSessionId;
	}

	public String getHistrogramSessionId() {
		return histrogramSessionId;
	}

	public void setHistrogramSessionId(String histrogramSessionId) {
		this.histrogramSessionId = histrogramSessionId;
	}


}
