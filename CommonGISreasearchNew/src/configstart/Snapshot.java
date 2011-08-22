package configstart;

import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: DIvan
 * Date: Mar 10, 2004
 * Time: 1:12:27 PM
 * To change this template use File | Settings | File Templates.
 */
public class Snapshot {

	public Snapshot() {
		creator = System.getProperty("user.name");
		creationDate = new Date(System.currentTimeMillis());
	}

	public Snapshot(String name, String description) {
		this();
		this.name = name;
		this.description = description;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public String getCreator() {
		return creator;
	}

	public void setCreator(String creator) {
		this.creator = creator;
	}

	public Date getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	private Date creationDate;
	private String creator;
	private String name;
	private String description;
	private String fileName;
}
