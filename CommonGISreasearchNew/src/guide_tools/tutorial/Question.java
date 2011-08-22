package guide_tools.tutorial;

import java.io.PrintStream;
import java.util.StringTokenizer;
import java.util.Vector;

import spade.lib.util.IntArray;

public class Question {
	/**
	* Possible types of required answers to the questions
	* SCORE means evaluation score (e.g. from 1 to 5)
	*/
	public static final int STRING = 0, NUMBER = 1, SELECT_ONE = 2, SELECT_MULTIPLE = 3, ENTER_MULTIPLE = 4, SCORE = 5;
	public static String typeTexts[] = { "string", "number", "select_one", "select_multiple", "enter_multiple", "score" };
	/**
	* Numbers of the question and the task and the unit it belongs to
	*/
	public int qN = 0, tN = 0, uN = 0;
	public String questionText = null;
	/**
	* There may be a picture (illustration) to the task. This variable contains
	* the path to the picture.
	*/
	public String pathToPicture = null;
	/**
	* The type of the answer required (STRING by default)
	*/
	public int answerType = STRING;
	/**
	* If the type of the required answer is SELECT_ONE or SELECT_MULTIPLE,
	* this vector contains the list of possible variants of the answer
	* (strings)
	* If the type is SCORE, this vector contains exactly 2 elements - the poles
	* of the evaluation scale (strings), for example, "very bad" and "very good".
	*/
	public Vector variants = null;
	/**
	* If the type of the required answer is SCORE, this variable keeps the
	* number of grades in the evaluation scale, for example, 5 (from 1 to 5).
	*/
	public int nGrades = 0;
	/**
	* If the type of the required answer is ENTER_MULTIPLE, this vector contains
	* the list of names of the fields that should be filled by the user
	*/
	public Vector fieldNames = null;
	/**
	* If the type of the required answer is ENTER_MULTIPLE, this array contains
	* the required types of the field content (STRING or NUMBER)
	*/
	public IntArray fieldTypes = null;
	/**
	* The correct answer to the question. Depending on the required type of the
	* answer, this may be an instance of one of the classes:
	* String    (if the answer type is string)
	* NumAnswer (if the answer type is number)
	* IntArray  (for select_one and select_multiple)
	* Vector    (for enter_multiple)
	* In the latter case the vector may consist of instances of String and
	* NumAnswer
	*/
	public Object answer = null;

	public void setQuestionNumber(int n) {
		qN = n;
	}

	public void setTaskNumber(int n) {
		tN = n;
	}

	public void setUnitNumber(int n) {
		uN = n;
	}

	public void addTextString(String str) {
		if (str == null)
			return;
		if (questionText == null) {
			questionText = new String(str);
		} else {
			questionText += " " + str;
		}
	}

	public String getQuestionText() {
		return questionText;
	}

	/**
	* Analyses the type of the answer specified as a string and returns the
	* corresponding integer, i.e. one of STRING, NUMBER, SELECT_ONE,
	* SELECT_MULTIPLE, or ENTER_MULTIPLE. If the type has not been recognized,
	* returns -1.
	*/
	public static int recognizeType(String typestr) {
		if (typestr == null)
			return -1;
		typestr = typestr.toLowerCase();
		for (int i = 0; i < typeTexts.length; i++)
			if (typestr.equals(typeTexts[i]))
				return i;
		System.out.println("Unknown answer type: " + typestr);
		return -1;
	}

	public void setAnswerType(String typestr) {
		int t = recognizeType(typestr);
		if (t >= 0) {
			answerType = t;
		}
	}

	public void addAnswerVariant(String str) {
		if (str == null)
			return;
		if (variants == null) {
			variants = new Vector(5, 5);
		}
		variants.addElement(str);
	}

	public int getVariantCount() {
		if (variants == null)
			return 0;
		return variants.size();
	}

	public String getAnswerVariant(int idx) {
		if (idx < 0 || idx >= getVariantCount())
			return null;
		return (String) variants.elementAt(idx);
	}

	/**
	* When the type of the question is SCORE, this method sets the text for the
	* left pole of the evaluation scale
	*/
	public void setLeftPole(String str) {
		if (str == null)
			return;
		if (variants == null) {
			variants = new Vector(5, 5);
		}
		if (variants.size() == 0) {
			variants.addElement(str);
		} else {
			variants.setElementAt(str, 0);
		}
	}

	/**
	* When the type of the question is SCORE, this method sets the text for the
	* right pole of the evaluation scale
	*/
	public void setRightPole(String str) {
		if (str == null)
			return;
		if (variants == null) {
			variants = new Vector(5, 5);
		}
		if (variants.size() == 0) {
			variants.addElement(null);
		}
		if (variants.size() == 1) {
			variants.addElement(str);
		} else {
			variants.setElementAt(str, 1);
		}
	}

	/**
	* When the type of the question is SCORE, this method returns the text of the
	* left pole of the evaluation scale
	*/
	public String getLeftPole() {
		if (getVariantCount() < 1)
			return null;
		return getAnswerVariant(0);
	}

	/**
	* When the type of the question is SCORE, this method returns the text of the
	* left pole of the evaluation scale
	*/
	public String getRightPole() {
		if (getVariantCount() < 2)
			return null;
		return getAnswerVariant(1);
	}

	public void addAnswerField(String fName, int ftype) {
		if (fName == null)
			return;
		if (fieldNames == null) {
			fieldNames = new Vector(5, 5);
		}
		if (fieldTypes == null) {
			fieldTypes = new IntArray(5, 5);
		}
		fieldNames.addElement(fName);
		if (ftype < 0 || ftype > NUMBER) {
			ftype = STRING;
		}
		fieldTypes.addElement(ftype);
	}

	public void addAnswerField(String fName, String ftype) {
		addAnswerField(fName, recognizeType(ftype));
	}

	public void setAnswerFieldType(String fName, String ftype) {
		if (fName == null || fieldNames == null)
			return;
		int idx = fieldNames.indexOf(fName);
		if (idx < 0)
			return;
		int t = recognizeType(ftype);
		if (t < 0 || t > NUMBER)
			return;
		fieldTypes.setElementAt(t, idx);
	}

	public int getFieldCount() {
		if (fieldNames == null)
			return 0;
		return fieldNames.size();
	}

	public String getAnswerFieldName(int idx) {
		if (idx < 0 || idx >= getFieldCount())
			return null;
		return (String) fieldNames.elementAt(idx);
	}

	public int getAnswerFieldType(int idx) {
		if (idx < 0 || idx >= getFieldCount())
			return -1;
		return fieldTypes.elementAt(idx);
	}

	/**
	* Recognizes and stores the answer specified as a string, depending on the
	* required answer type. Hence, the answer type MUST be previously set!
	*/
	public void setCorrectAnswer(String str) {
		if (str != null) {
			switch (answerType) {
			case STRING:
				answer = str;
				break;
			case NUMBER:
				NumAnswer na = new NumAnswer();
				if (na.getAnswerFromString(str)) {
					answer = na;
				}
				break;
			case SELECT_ONE:
			case SELECT_MULTIPLE:
				IntArray aa = new IntArray(getVariantCount(), 5);
				StringTokenizer st = new StringTokenizer(str, ",;");
				while (st.hasMoreTokens()) {
					try {
						int k = Integer.valueOf(st.nextToken()).intValue();
						if (k < 1 || k > getVariantCount()) {
							System.out.println("Wrong option number: " + k);
							break;
						}
						--k;
						if (aa.indexOf(k) < 0) {
							aa.addElement(k);
						}
					} catch (NumberFormatException nfe) {
						System.out.println("Cannot read a number from " + str);
						break;
					}
				}
				if (answerType == SELECT_ONE && aa.size() > 1) {
					System.out.println("Question " + uN + "." + tN + "." + qN + ": wrong number of correct options: " + aa.size());
				} else {
					answer = aa;
				}
				break;
			case ENTER_MULTIPLE:
				if (str.charAt(0) == '(') {
					int idx = str.indexOf(')');
					if (idx < 0) {
						idx = str.length();
					}
					str = str.substring(1, idx).trim();
				}
				st = new StringTokenizer(str, ",;");
				Vector aw = new Vector(getFieldCount(), 5);
				while (st.hasMoreTokens() && aw.size() < getFieldCount()) {
					String s = st.nextToken().trim();
					int idx = aw.size();
					if (getAnswerFieldType(idx) == NUMBER) {
						try {
							float val = Float.valueOf(s).floatValue();
							na = new NumAnswer();
							na.value = val;
							aw.addElement(na);
						} catch (NumberFormatException nfe) {
							System.out.println("Cannot read a number from " + s);
							break;
						}
					} else {
						aw.addElement(s);
					}
				}
				if (aw.size() < getFieldCount()) {
					System.out.println("Could not get values for all fields of " + "the answer from" + str);
				} else {
					answer = aw;
				}
				break;
			}
		}
		if (answer == null) {
			System.out.println("Question " + uN + "." + tN + "." + qN + ": wrong specification of the answer: " + str);
		}
	}

	public String getAnswerAsString() {
		if (answer == null)
			return null;
		switch (answerType) {
		case STRING:
		case NUMBER:
			return answer.toString();
		case SELECT_ONE:
		case SELECT_MULTIPLE:
			IntArray aa = (IntArray) answer;
			if (aa.size() < 1)
				return null;
			String str = String.valueOf(aa.elementAt(0) + 1);
			if (answerType == SELECT_ONE || aa.size() == 1)
				return str;
			for (int i = 1; i < aa.size(); i++) {
				str += ";" + (aa.elementAt(i) + 1);
			}
			return str;
		case ENTER_MULTIPLE:
			Vector v = (Vector) answer;
			str = "";
			for (int i = 0; i < v.size(); i++) {
				if (i > 0) {
					str += "; ";
				}
				str += v.elementAt(i).toString() + "(" + getAnswerFieldName(i) + ")";
			}
			return str;
		}
		return null;
	}

	public void printToStream(PrintStream ps) {
		ps.print("Question ");
		if (uN > 0) {
			ps.print(uN + ".");
		}
		if (tN > 0) {
			ps.print(tN + ".");
		}
		if (qN > 0) {
			ps.print(qN);
		}
		ps.println();
		if (questionText != null) {
			ps.println(questionText);
		}
		if (pathToPicture != null) {
			ps.println("Illustration: " + pathToPicture);
		}
		switch (answerType) {
		case STRING:
			ps.println();
			break;
		case NUMBER:
			ps.println("                  (enter a number)");
			break;
		case SELECT_ONE:
			ps.println("  select one of the variants:");
			for (int i = 0; i < getVariantCount(); i++) {
				ps.println("  (  ) " + getAnswerVariant(i));
			}
			break;
		case SELECT_MULTIPLE:
			ps.println("  select one or more of the variants:");
			for (int i = 0; i < getVariantCount(); i++) {
				ps.println("  [  ] " + getAnswerVariant(i));
			}
			break;
		case ENTER_MULTIPLE:
			for (int i = 0; i < getFieldCount(); i++) {
				ps.print("  " + getAnswerFieldName(i) + ":");
				if (getAnswerFieldType(i) == NUMBER) {
					ps.println("                  (enter a number)");
				} else {
					ps.println();
				}
			}
			break;
		case SCORE:
			ps.println("  Select a position on the scale:");
			ps.print("  " + getLeftPole());
			for (int i = 0; i < nGrades; i++) {
				ps.print("   (__)");
			}
			ps.println("   " + getRightPole());
			break;
		}
		if (answer != null) {
			ps.println("Correct answer: " + getAnswerAsString());
		}
	}
}