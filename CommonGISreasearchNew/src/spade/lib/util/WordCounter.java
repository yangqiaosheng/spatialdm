package spade.lib.util;

import java.util.StringTokenizer;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Mar 17, 2010
 * Time: 3:22:18 PM
 * Counts frequencies of words and word collocations in a text
 */
public class WordCounter {
	/**
	 * The uninteresting words, which should not be counted
	 */
	public String wordsToSkip[] = { "a", "an", "the", "vs", "and", "or", "not", "no", "at", "on", "in", "of", "off", "for", "under", "over", "above", "through", "with", "from", "to", "till", "until", "about", "around", "is", "are", "am", "do",
			"does", "shall", "will", "have", "was", "were", "be", "could", "would", "should", "we", "you", "it", "she", "he", "my", "our", "your", "its", "it's", "his", "her", "their", "this", "that", "these", "those", "here", "there", "still",
			"just" };
	/**
	 * The input text (collection of strings)
	 */
	public Vector<String> text = null;
	/**
	 * The words occurring in the text and their counts
	 */
	public Vector<ObjectWithCount> wordCounts = null;

	/**
	 * Attaches the given string to the text
	 */
	public void addString(String str) {
		if (str == null || str.length() < 1)
			return;
		if (text == null) {
			text = new Vector<String>(200, 200);
		}
		text.addElement(str);
	}

	/**
	 * Adds the current string to the counters
	 */
	public void countString(String str) {
		if (str == null || str.length() < 1)
			return;
		if (wordCounts == null) {
			wordCounts = new Vector<ObjectWithCount>(100, 100);
		}
		int idx = ObjectWithCount.findObjectInList(str, wordCounts);
		if (idx >= 0) {
			wordCounts.elementAt(idx).add();
		} else {
			ObjectWithCount wc = new ObjectWithCount(str, 1);
			wordCounts.addElement(wc);
		}
	}

	/**
	 * Returns the number of unique words extracted from the texts
	 */
	public int getNUniqueWords() {
		if (wordCounts == null)
			return 0;
		return wordCounts.size();
	}

	/**
	 * sort the counters
	 */
	public void sortWords() {
		BubbleSort.sort(wordCounts);
	}

	/**
	 * Extracts the words from the given string and counts their frequencies
	 */
	public void getWordsFromString(String str) {
		if (str == null || str.length() < 1)
			return;
		StringTokenizer st = new StringTokenizer(str, " .,;-!?:/\\()[]{}\"\r\n");
		if (wordCounts == null) {
			wordCounts = new Vector<ObjectWithCount>(100, 100);
		}
		while (st.hasMoreTokens()) {
			String word = st.nextToken();
			if (shouldSkip(word)) {
				continue;
			}
			int idx = ObjectWithCount.findObjectInList(word, wordCounts);
			if (idx >= 0) {
				wordCounts.elementAt(idx).add();
			} else {
				ObjectWithCount wc = new ObjectWithCount(word, 1);
				wordCounts.addElement(wc);
			}
		}
	}

	/**
	 * Among the extracted words tries to find joins of other words (two words
	 * without space between them). If such cases are found, the joined words
	 * are removed, and the frequencies of the original words increased.
	 */
	public void removeJoinedWords() {
		if (wordCounts == null || wordCounts.size() < 2)
			return;
		int i = 0;
		while (i < wordCounts.size()) {
			String word = (String) wordCounts.elementAt(i).obj;
			boolean foundSubstr = false;
			for (int j = 0; j < wordCounts.size() && !foundSubstr; j++)
				if (j != i) {
					String word1 = (String) wordCounts.elementAt(j).obj;
					int idx = word.indexOf(word1);
					if (idx < 0) {
						continue;
					}
					int iw0 = -1, iw2 = -1;
					if (idx > 0) {
						String word0 = word.substring(0, idx);
						iw0 = ObjectWithCount.findObjectInList(word0, wordCounts);
					}
					if (idx + word1.length() < word.length()) {
						String word2 = word.substring(idx + word1.length());
						iw2 = ObjectWithCount.findObjectInList(word2, wordCounts);
					}
					if (iw0 < 0 && iw2 < 0) {
						continue;
					}
					foundSubstr = true;
					wordCounts.elementAt(j).add();
					if (iw0 >= 0) {
						wordCounts.elementAt(iw0).add();
					}
					if (iw2 >= 0) {
						wordCounts.elementAt(iw2).add();
					}
					wordCounts.removeElementAt(i);
				}
			if (!foundSubstr) {
				++i;
			}
		}
	}

	/**
	 * Extracts the words from the texts, counts their frequencies,
	 * and sorts the list of words by the frequencies, in decreasing order.
	 * Returns the number of different words extracted from the text.
	 */
	public int countWords() {
		if (text == null || text.size() < 1)
			return 0;
		wordCounts = new Vector<ObjectWithCount>(text.size() * 5, 100);
		for (int i = 0; i < text.size(); i++) {
			getWordsFromString(text.elementAt(i));
		}
		if (wordCounts.size() < 1) {
			wordCounts = null;
			return 0;
		}
		BubbleSort.sort(wordCounts);
		return wordCounts.size();
	}

	/**
	 * Checks if the given word is in the list of words to skip, or contains numbers, or
	 * consists of a single character
	 */
	public boolean shouldSkip(String word) {
		if (word == null || word.length() < 2)
			return true;
		//check if the word contains numbers
		for (int n = 0; n <= 9; n++)
			if (word.indexOf(String.valueOf(n)) >= 0)
				return true;
		if (word.endsWith("n't"))
			return true;
		for (String element : wordsToSkip)
			if (word.equals(element))
				return true;
		return false;
	}

	/**
	 * returns the most frequent word
	 */
	public String getMostFrequentWord() {
		if (wordCounts == null || wordCounts.size() < 1)
			return null;
		return (String) wordCounts.elementAt(0).obj;
	}

	/**
	 * Returns the required number of the most frequent words extracted from the text.
	 * The array may be shorter than required if too few words have been extracted
	 * If attachFrequencies is true, the frequency value is attached to each word.
	 * The format: <frequency>: <word>
	 */
	public String[] getMostFrequentWords(int maxNWords, boolean attachFrequencies) {
		if (wordCounts == null) {
			countWords();
		}
		if (wordCounts == null || wordCounts.size() < 1)
			return null;
		int nWords = Math.min(wordCounts.size(), maxNWords);
		String words[] = new String[nWords];
		for (int i = 0; i < nWords; i++)
			if (attachFrequencies) {
				words[i] = wordCounts.elementAt(i).count + ": " + wordCounts.elementAt(i).obj;
			} else {
				words[i] = (String) wordCounts.elementAt(i).obj;
			}
		return words;
	}

	/**
	 * Tries to find frequent collocations (contexts) of the given number (maxNFrequentWords)
	 * of the most frequent words.
	 * Returns the required number (maxNCollocations) of the most frequent collocations
	 * such that the frequency is not less than the given number minFrequency.
	 * The array may be shorter than required if there are too few collocations with the
	 * required frequencies.
	 * If attachFrequencies is true, the frequency value is attached to each word.
	 * The format: <frequency>: <collocation> (words separated by spaces)
	 * If "divider" is not null, this string is added after each section
	 * of n-words collocations, where n=5,4,3,2
	 */
	public String[] getMostFrequentCollocations(int maxNFrequentWords, int maxNCollocations, int minFrequency, boolean attachFrequencies, String divider) {
		if (text == null || text.size() < 1)
			return null;
		String words[] = getMostFrequentWords(maxNFrequentWords, false);
		if (words == null || words.length < 2)
			return null;
		int cap = words.length * 5;
		Vector<ObjectWithCount> colloCounts2 = new Vector<ObjectWithCount>(cap, 100), colloCounts3 = new Vector<ObjectWithCount>(cap, 100), colloCounts4 = new Vector<ObjectWithCount>(cap, 100), colloCounts5 = new Vector<ObjectWithCount>(cap, 100);
		Vector<String> tokens = new Vector<String>(20, 10);
		Vector<String> combinations = new Vector<String>(10, 10);
		IntArray combLengths = new IntArray(10, 10);
		for (int i = 0; i < text.size(); i++) {
			StringTokenizer st = new StringTokenizer(text.elementAt(i), " .,;!?:/\\()[]{}\"\r\n");
			tokens.removeAllElements();
			while (st.hasMoreTokens()) {
				String word = st.nextToken();
				if (!shouldSkip(word)) {
					tokens.addElement(word);
				}
			}
			if (tokens.size() < 2) {
				continue;
			}
			for (String word : words) {
				int wIdx = tokens.indexOf(word);
				if (wIdx < 0) {
					continue;
				}
				combinations.removeAllElements();
				combLengths.removeAllElements();
				if (wIdx > 0) {
					combinations.addElement(tokens.elementAt(wIdx - 1) + " " + word);
					combLengths.addElement(2);
				}
				if (wIdx < tokens.size() - 1) {
					combinations.addElement(word + " " + tokens.elementAt(wIdx + 1));
					combLengths.addElement(2);
				}
				if (wIdx > 0 && wIdx < tokens.size() - 1) {
					combinations.addElement(tokens.elementAt(wIdx - 1) + " " + word + " " + tokens.elementAt(wIdx + 1));
					combLengths.addElement(3);
				}
				if (wIdx > 1 && wIdx < tokens.size() - 1) {
					combinations.addElement(tokens.elementAt(wIdx - 2) + " " + tokens.elementAt(wIdx - 1) + " " + word + " " + tokens.elementAt(wIdx + 1));
					combLengths.addElement(4);
				}
				if (wIdx > 0 && wIdx < tokens.size() - 2) {
					combinations.addElement(tokens.elementAt(wIdx - 1) + " " + word + " " + tokens.elementAt(wIdx + 1) + " " + tokens.elementAt(wIdx + 2));
					combLengths.addElement(4);
				}
				if (wIdx > 1 && wIdx < tokens.size() - 2) {
					combinations.addElement(tokens.elementAt(wIdx - 2) + " " + tokens.elementAt(wIdx - 1) + " " + word + " " + tokens.elementAt(wIdx + 1) + " " + tokens.elementAt(wIdx + 2));
					combLengths.addElement(5);
				}
				for (int k = 0; k < combinations.size(); k++) {
					int len = combLengths.elementAt(k);
					Vector<ObjectWithCount> colloCounts = (len == 5) ? colloCounts5 : (len == 4) ? colloCounts4 : (len == 3) ? colloCounts3 : colloCounts2;
					int idx = ObjectWithCount.findObjectInList(combinations.elementAt(k), colloCounts);
					if (idx >= 0) {
						colloCounts.elementAt(idx).add();
					} else {
						ObjectWithCount wc = new ObjectWithCount(combinations.elementAt(k), 1);
						colloCounts.addElement(wc);
					}
				}
			}
		}
		if (colloCounts2.size() < 1)
			return null;
		Vector<ObjectWithCount> selColl = new Vector<ObjectWithCount>(4 * maxNCollocations, 10);
		for (int iter = 0; iter < 4; iter++) {
			Vector<ObjectWithCount> colloCounts = (iter == 0) ? colloCounts5 : (iter == 1) ? colloCounts4 : (iter == 2) ? colloCounts3 : colloCounts2;
			if (colloCounts.size() < 1) {
				continue;
			}
			if (colloCounts.size() > 1) {
				BubbleSort.sort(colloCounts);
			}
			int nAdded = 0;
			for (int i = 0; i < colloCounts.size() && nAdded < maxNCollocations; i++)
				if (colloCounts.elementAt(i).count >= minFrequency) {
					String str = (String) colloCounts.elementAt(i).obj;
					//check if this string occurs in the earlier selected collocations
					boolean occurs = false;
					for (int j = 0; j < selColl.size() && !occurs; j++)
						if (selColl.elementAt(j) != null) {
							String prevStr = (String) selColl.elementAt(j).obj;
							occurs = prevStr.indexOf(str) >= 0;
						}
					if (!occurs) {
						selColl.addElement(colloCounts.elementAt(i));
						++nAdded;
					}
				} else {
					break;
				}
			if (nAdded > 0 && divider != null) {
				selColl.addElement(null);
			}
		}
		if (selColl.size() < 1)
			return null;
		String collocations[] = new String[selColl.size()];
		for (int i = 0; i < selColl.size(); i++)
			if (selColl.elementAt(i) == null) {
				collocations[i] = divider;
			} else if (attachFrequencies) {
				collocations[i] = selColl.elementAt(i).count + ": " + selColl.elementAt(i).obj;
			} else {
				collocations[i] = (String) selColl.elementAt(i).obj;
			}
		return collocations;
	}
}
