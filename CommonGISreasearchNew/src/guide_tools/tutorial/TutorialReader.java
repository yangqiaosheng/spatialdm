package guide_tools.tutorial;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Vector;

import spade.lib.util.CopyFile;
import spade.lib.util.ProcessListener;
import spade.lib.util.ProcessStateNotifier;
import spade.lib.util.StringUtil;

public class TutorialReader {
	/**
	* The source of data - a path to a file or a URL
	*/
	protected String dataSource = null;
	/**
	* The stream from which the data are read
	*/
	protected InputStream stream = null;
	/**
	* Used to register possible listeners and notify them about the state of
	* data loading
	*/
	protected ProcessStateNotifier notifier = null;

	/**
	* Sets the data source - a path to a file or a URL
	*/
	public void setDataSource(String source) {
		dataSource = source;
	}

	/**
	* Adds a listener of the process of data loading
	*/
	public void addProcessListener(ProcessListener lst) {
		if (lst == null)
			return;
		if (notifier == null) {
			notifier = new ProcessStateNotifier();
		}
		notifier.addProcessListener(lst);
	}

	/**
	* Notifies the listeners, if any, about the status of the process of data
	* loading. If "trouble" is true, then this is an error message.
	*/
	public void notifyProcessState(String processState, boolean trouble) {
		if (notifier != null) {
			notifier.notifyProcessState(this, "Reading tutorial destription", processState, trouble);
		}
		if (trouble) {
			System.err.println("Reading tutorial destription: " + processState);
		}
	}

	/**
	* Opens the stream on the earlier specified data source. If the source
	* string starts with HTTP or FILE, accesses the source as a URL, otherwise
	* opens it as a local file
	*/
	protected void openStream() {
		if (stream != null)
			return;
		if (dataSource == null)
			return;
		int idx = dataSource.indexOf(':');
		boolean isURL = false;
		if (idx > 0) {
			String pref = dataSource.substring(0, idx);
			if (pref.equalsIgnoreCase("HTTP") || pref.equalsIgnoreCase("FILE")) {
				isURL = true;
			}
		}
		try {
			if (isURL) { //try to access the source as a URL
				URL url = new URL(dataSource);
				stream = url.openStream();
			} else {
				stream = new FileInputStream(dataSource);
			}
		} catch (IOException ioe) {
			notifyProcessState("Error accessing " + dataSource + ": " + ioe, true);
			return;
		}
	}

	protected void closeStream() {
		if (stream == null)
			return;
		try {
			stream.close();
		} catch (IOException ioe) {
		}
		stream = null;
	}

	public TutorialContent read() {
		if (stream == null) {
			openStream();
		}
		if (stream == null)
			return null;
		BufferedReader br = new BufferedReader(new InputStreamReader(stream));
		TutorialContent tcont = new TutorialContent();
		while (true) {
			try {
				String str = br.readLine();
				if (str == null) {
					break;
				}
				str = str.trim();
				if (str.length() < 1) {
					continue;
				}
				if (str.equalsIgnoreCase("<unit>")) {
					tcont.addUnit(readUnit(br));
				} else if (str.equalsIgnoreCase("<form>")) {
					tcont.addForm(readForm(br));
				}
				;
			} catch (EOFException ioe) {
				notifyProcessState("successfully finished", false);
				break;
			} catch (IOException ioe) {
				notifyProcessState("exception " + ioe.toString(), true);
				break;
			}
		}
		closeStream();
		if (tcont.getUnitCount() < 1) {
			notifyProcessState("no units have been loaded!", true);
			return null;
		}
		tcont.pathToTutorial = CopyFile.getDir(this.dataSource);
		return tcont;
	}

	protected TutorialUnit readUnit(BufferedReader br) throws IOException {
		if (br == null)
			return null;
		TutorialUnit unit = new TutorialUnit();
		while (true) {
			String str = br.readLine();
			if (str == null) {
				break;
			}
			str = str.trim();
			if (str.length() < 1) {
				continue;
			}
			if (str.equalsIgnoreCase("</unit>")) {
				break;
			}
			int idx = str.indexOf("=");
			boolean processed = false;
			if (idx > 0 && idx < str.length() - 1) {
				processed = true;
				String key = str.substring(0, idx).trim().toLowerCase(), val = StringUtil.removeQuotes(str.substring(idx + 1).trim());
				if (key.length() < 1 || val.length() < 1) {
					processed = false;
				} else if (key.equals("name")) {
					unit.name = val;
				} else if (key.equals("example")) {
					unit.pathToExample = val;
				} else {
					processed = false;
				}
			}
			if (processed) {
				continue;
			}
			if (str.equalsIgnoreCase("<task>")) {
				unit.addTask(readTask(br));
			} else if (str.equalsIgnoreCase("<map>")) {
				unit.msp = readMapSpecification(br);
			} else if (str.equalsIgnoreCase("<tool>")) {
				unit.addToolSpecification(readToolSpecification(br));
			} else if (str.equalsIgnoreCase("<advise>")) {
				unit.advise = readAdvise(br);
			} else if (str.equalsIgnoreCase("<form>")) {
				unit.evalForm = readForm(br);
			} else {
				unit.addDescriptionLine(str);
			}
		}
		if (unit.getTaskCount() < 1)
			return null;
		return unit;
	}

	protected Vector readAttributeList(BufferedReader br) throws IOException {
		if (br == null)
			return null;
		Vector attr = new Vector(10, 5);
		while (true) {
			String str = br.readLine();
			if (str == null) {
				break;
			}
			str = str.trim();
			if (str.length() < 1) {
				continue;
			}
			if (str.equalsIgnoreCase("</attributes>")) {
				break;
			}
			attr.addElement(str);
		}
		if (attr.size() < 1)
			return null;
		attr.trimToSize();
		return attr;
	}

	protected MapSpec readMapSpecification(BufferedReader br) throws IOException {
		if (br == null)
			return null;
		MapSpec msp = new MapSpec();
		while (true) {
			String str = br.readLine();
			if (str == null) {
				break;
			}
			str = str.trim();
			if (str.length() < 1) {
				continue;
			}
			if (str.equalsIgnoreCase("</map>")) {
				break;
			}
			if (str.equalsIgnoreCase("<attributes>")) {
				msp.attr = readAttributeList(br);
				continue;
			}
			int idx = str.indexOf("=");
			if (idx > 0 && idx < str.length() - 1) {
				String key = str.substring(0, idx).trim().toLowerCase(), val = StringUtil.removeQuotes(str.substring(idx + 1).trim());
				if (key.length() < 1 || val.length() < 1) {
					continue;
				}
				if (key.equals("table")) {
					msp.tblId = val;
				} else if (key.equals("method"))
					if (!val.equalsIgnoreCase("none")) {
						msp.methodId = val;
					}
			}
		}
		return msp;
	}

	protected ToolSpec readToolSpecification(BufferedReader br) throws IOException {
		if (br == null)
			return null;
		ToolSpec tsp = new ToolSpec();
		while (true) {
			String str = br.readLine();
			if (str == null) {
				break;
			}
			str = str.trim();
			if (str.length() < 1) {
				continue;
			}
			if (str.equalsIgnoreCase("</tool>")) {
				break;
			}
			if (str.equalsIgnoreCase("<attributes>")) {
				tsp.attr = readAttributeList(br);
				continue;
			}
			int idx = str.indexOf("=");
			if (idx > 0 && idx < str.length() - 1) {
				String key = str.substring(0, idx).trim().toLowerCase(), val = StringUtil.removeQuotes(str.substring(idx + 1).trim());
				if (key.length() < 1 || val.length() < 1) {
					continue;
				}
				if (key.equals("table")) {
					tsp.tblId = val;
				} else if (key.equals("type")) {
					tsp.toolType = val;
				}
			}
		}
		if (tsp.toolType == null)
			return null;
		return tsp;
	}

	protected Advise readAdvise(BufferedReader br) throws IOException {
		if (br == null)
			return null;
		Advise advise = new Advise();
		while (true) {
			String str = br.readLine();
			if (str == null) {
				break;
			}
			str = str.trim();
			if (str.length() < 1) {
				continue;
			}
			if (str.equalsIgnoreCase("</advise>")) {
				break;
			}
			int idx = str.indexOf("=");
			boolean processed = false;
			if (idx > 0 && idx < str.length() - 1) {
				String key = str.substring(0, idx).trim().toLowerCase(), val = StringUtil.removeQuotes(str.substring(idx + 1).trim());
				if (key.equals("help") && val.length() > 0) {
					advise.addHelpTopicId(val);
					processed = true;
				}
			}
			if (!processed) {
				advise.addInstructionLine(str);
			}
		}
		if (advise.getInstructionCount() < 1)
			return null;
		return advise;
	}

	protected Task readTask(BufferedReader br) throws IOException {
		if (br == null)
			return null;
		Task task = new Task();
		while (true) {
			String str = br.readLine();
			if (str == null) {
				break;
			}
			str = str.trim();
			if (str.length() < 1) {
				continue;
			}
			if (str.equalsIgnoreCase("</task>")) {
				break;
			}
			int idx = str.indexOf("=");
			boolean processed = false;
			if (idx > 0 && idx < str.length() - 1) {
				processed = true;
				String key = str.substring(0, idx).trim().toLowerCase(), val = StringUtil.removeQuotes(str.substring(idx + 1).trim());
				if (key.length() < 1 || val.length() < 1) {
					processed = false;
				} else if (key.equals("picture")) {
					task.pathToPicture = val;
				} else {
					processed = false;
				}
			}
			if (processed) {
				continue;
			}
			if (str.equalsIgnoreCase("<question>")) {
				task.addQuestion(readQuestion(br));
			} else {
				task.addTextString(str);
			}
		}
		if (task.getTaskText() == null || task.getQuestionCount() < 1)
			return null;
		return task;
	}

	protected Question readQuestion(BufferedReader br) throws IOException {
		if (br == null)
			return null;
		Question question = new Question();
		String lastFieldName = null, answer = null;
		while (true) {
			String str = br.readLine();
			if (str == null) {
				break;
			}
			str = str.trim();
			if (str.length() < 1) {
				continue;
			}
			if (str.equalsIgnoreCase("</question>")) {
				break;
			}
			int idx = str.indexOf("=");
			boolean processed = false;
			if (idx > 0 && idx < str.length() - 1) {
				processed = true;
				String key = str.substring(0, idx).trim().toLowerCase(), val = StringUtil.removeQuotes(str.substring(idx + 1).trim());
				if (key.length() < 1 || val.length() < 1) {
					processed = false;
				} else if (key.equals("picture")) {
					question.pathToPicture = val;
				} else if (key.equals("answer_type")) {
					question.setAnswerType(val);
				} else if (key.equals("option")) {
					question.addAnswerVariant(val);
				} else if (key.equals("from")) {
					question.setLeftPole(val);
				} else if (key.equals("to")) {
					question.setRightPole(val);
				} else if (key.equals("grades")) {
					try {
						question.nGrades = Integer.valueOf(val).intValue();
					} catch (NumberFormatException nfe) {
						System.out.println("Not a number in " + str + "!");
					}
				} else if (key.equals("field")) {
					question.addAnswerField(val, Question.STRING);
					lastFieldName = val;
				} else if (key.equals("field_type")) {
					if (lastFieldName != null) {
						question.setAnswerFieldType(lastFieldName, val);
						lastFieldName = null;
					}
				} else if (key.equals("answer")) {
					answer = val;
				} else {
					processed = false;
				}
			}
			if (!processed) {
				question.addTextString(str);
			}
		}
		if (question.getQuestionText() == null)
			return null;
		if (answer != null) {
			question.setCorrectAnswer(answer);
		}
		return question;
	}

	protected Task readForm(BufferedReader br) throws IOException {
		if (br == null)
			return null;
		Task task = new Task();
		while (true) {
			String str = br.readLine();
			if (str == null) {
				break;
			}
			str = str.trim();
			if (str.length() < 1) {
				continue;
			}
			if (str.equalsIgnoreCase("</form>")) {
				break;
			}
			int idx = str.indexOf("=");
			boolean processed = false;
			if (idx > 0 && idx < str.length() - 1) {
				processed = true;
				String key = str.substring(0, idx).trim().toLowerCase(), val = StringUtil.removeQuotes(str.substring(idx + 1).trim());
				if (key.length() < 1 || val.length() < 1) {
					processed = false;
				} else if (key.equals("name")) {
					task.name = val;
				} else {
					processed = false;
				}
			}
			if (processed) {
				continue;
			}
			if (str.equalsIgnoreCase("<question>")) {
				task.addQuestion(readQuestion(br));
			} else {
				task.addTextString(str);
			}
		}
		if (task.getQuestionCount() < 1)
			return null;
		return task;
	}
}
