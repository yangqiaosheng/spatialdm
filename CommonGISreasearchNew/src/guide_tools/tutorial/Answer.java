package guide_tools.tutorial;

import java.util.Vector;

import spade.lib.util.IntArray;

public class Answer {
	/**
	* The question to answer
	*/
	public Question question = null;
	/**
	* The user's input: an instance of String, Float, Integer (number of the
	* option selected), IntArray (with numbers of multiple options), or Vector
	* (if the answer consists of several fields). The string "null" means
	* the answer "don't know" (but not null pointer!)
	*/
	public Object userAnswer = null;
	/**
	* The moments of asking the question and getting the answer (in milliseconds)
	*/
	protected long questionTime = 0L, answerTime = 0L;

	public void setQuestion(Question q) {
		question = q;
	}

	public void setUserAnswer(Object answ) {
		userAnswer = answ;
	}

	public void setQuestionTime(long time) {
		questionTime = time;
	}

	public void setAnswerTime(long time) {
		answerTime = time;
	}

	public long getQuestionTime() {
		return questionTime;
	}

	public long getAnswerTime() {
		return answerTime;
	}

	public long getAnswerDuration() {
		return answerTime - questionTime;
	}

	public long getAnswerDurationInSec() {
		return Math.round(getAnswerDuration() / 1000.0);
	}

	/**
	* Returns true if the answer was "do not know"
	*/
	static public boolean isAnswerRefused(Object answer) {
		return answer != null && (answer instanceof String) && answer.equals("null");
	}

	/**
	* Returns true if the answer was "do not know"
	*/
	public boolean isAnswerRefused() {
		return isAnswerRefused(userAnswer);
	}

	public String getAnswerAsString() {
		return getAnswerAsString(false);
	}

	public String getAnswerAsString(boolean extended) {
		return getAnswerAsString(question, userAnswer, extended);
	}

	public static String getAnswerAsString(Question q, Object answer, boolean extended) {
		if (q == null || answer == null)
			return null;
		if (isAnswerRefused(answer))
			return "do not know";
		switch (q.answerType) {
		case Question.STRING:
		case Question.NUMBER:
			return answer.toString();
		case Question.SELECT_ONE:
		case Question.SELECT_MULTIPLE:
			if (answer instanceof Integer) {
				int n = ((Integer) answer).intValue();
				String str = String.valueOf(n + 1);
				if (extended) {
					str += " (" + q.getAnswerVariant(n) + ")";
				}
				return str;
			} else {
				IntArray bb = (IntArray) answer;
				String str = "";
				for (int i = 0; i < bb.size(); i++) {
					if (i > 0) {
						str += "; ";
					}
					str += String.valueOf(bb.elementAt(i) + 1);
					if (extended) {
						str += " (" + q.getAnswerVariant(bb.elementAt(i)) + ")";
					}
				}
				return str;
			}
		case Question.ENTER_MULTIPLE:
			Vector v = (Vector) answer;
			String str = "";
			for (int i = 0; i < v.size(); i++) {
				if (i > 0) {
					str += "; ";
				}
				if (v.elementAt(i) != null) {
					str += v.elementAt(i).toString();
				} else {
					str += "null";
				}
			}
			return str;
		case Question.SCORE:
			int n = ((Integer) answer).intValue();
			str = String.valueOf(n + 1) + " of " + q.nGrades;
			if (extended) {
				str += " (from " + q.getLeftPole() + " to " + q.getRightPole() + ")";
			}
			return str;
		}
		return null;
	}

	public boolean isAnswerCorrect() {
		if (question == null || question.answer == null || userAnswer == null)
			return false;
		if (isAnswerRefused())
			return false;
		switch (question.answerType) {
		case Question.STRING:
			String txt1 = (String) userAnswer,
			txt2 = (String) question.answer;
			return txt1.equalsIgnoreCase(txt2);
		case Question.NUMBER:
			NumAnswer na = (NumAnswer) question.answer;
			return na.isAnswerCorrect(userAnswer);
		case Question.SELECT_ONE:
		case Question.SELECT_MULTIPLE:
			IntArray aa = (IntArray) question.answer;
			if (userAnswer instanceof Integer) {
				if (aa.size() > 1)
					return false;
				int n = ((Integer) userAnswer).intValue();
				return n == aa.elementAt(0);
			} else {
				IntArray bb = (IntArray) userAnswer;
				if (aa.size() != bb.size())
					return false;
				for (int i = 0; i < aa.size(); i++)
					if (bb.indexOf(aa.elementAt(i)) < 0)
						return false;
				return true;
			}
		case Question.ENTER_MULTIPLE:
			Vector v = (Vector) userAnswer,
			vc = (Vector) question.answer;
			if (v.size() != vc.size())
				return false;
			for (int i = 0; i < v.size(); i++)
				if (question.getAnswerFieldType(i) == Question.NUMBER) {
					na = (NumAnswer) vc.elementAt(i);
					if (!na.isAnswerCorrect(v.elementAt(i)))
						return false;
				} else {
					txt1 = (String) v.elementAt(i);
					txt2 = (String) vc.elementAt(i);
					if (!txt1.equalsIgnoreCase(txt2))
						return false;
				}
			return true;
		}
		return false;
	}
}