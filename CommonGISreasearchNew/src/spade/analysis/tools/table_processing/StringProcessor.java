package spade.analysis.tools.table_processing;

import java.awt.BorderLayout;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Choice;
import java.awt.FileDialog;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.awt.TextArea;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.StringTokenizer;
import java.util.Vector;

import spade.analysis.system.DataKeeper;
import spade.analysis.system.DataLoader;
import spade.analysis.system.ESDACore;
import spade.analysis.tools.BaseAnalyser;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Dialogs;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.OKDialog;
import spade.lib.util.IdMaker;
import spade.lib.util.IntArray;
import spade.lib.util.ObjectWithCount;
import spade.lib.util.StringUtil;
import spade.lib.util.WordCounter;
import spade.vis.database.Attribute;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataRecord;
import spade.vis.database.DataTable;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: May 10, 2010
 * Time: 2:24:12 PM
 * Deals with a table column containing string values
 */
public class StringProcessor extends BaseAnalyser {
	/**
	* This method constructs and starts the tool. Everything the tool may need
	* for integration with other components of the system can be received
	* from the system's core passed as an argument.
	*/
	@Override
	public void run(ESDACore core) {
		this.core = core;
		if (core == null)
			return;
		DataKeeper dk = core.getDataKeeper();
		if (dk == null || dk.getTableCount() < 1) {
			showMessage("No tables found!", true);
			return;
		}
		AttributeDataPortion table = TableProcessor.selectTable(core);
		if (table == null)
			return;
		if (table.getDataItemCount() < 1) {
			showMessage("No data in the table!", true);
			return;
		}
		IntArray colNs = new IntArray(table.getAttrCount(), 10);
		List list = new List(Math.min(table.getAttrCount(), 5));
		for (int i = 0; i < table.getAttrCount(); i++) {
			Attribute at = table.getAttribute(i);
			if (at.getType() == AttributeTypes.character) {
				colNs.addElement(i);
				list.add(at.getName());
			}
		}
		if (colNs.size() < 1) {
			showMessage("The table has no columns with string values!", true);
			return;
		}
		list.select(0);
		Panel p = new Panel(new BorderLayout());
		p.add(new Label("Select the table column containing strings:"), BorderLayout.NORTH);
		p.add(list, BorderLayout.CENTER);

		CheckboxGroup cbg = new CheckboxGroup();
		Panel pp = new Panel(new ColumnLayout());
		Checkbox cbExtractWords = new Checkbox("extract unique words and their frequencies", true, cbg);
		pp.add(cbExtractWords);
		Checkbox cbFindCombinations = new Checkbox("find word combinations including given words", false, cbg);
		pp.add(cbFindCombinations);
		Checkbox cbCountFreqInText = new Checkbox("count frequencies of terms in text lines", false, cbg);
		pp.add(cbCountFreqInText);
		Checkbox cbCountDistances = new Checkbox("count pairwise distances among terms based on co-occurrences", false, cbg);
		pp.add(cbCountDistances);
		p.add(pp, BorderLayout.SOUTH);

		OKDialog dia = new OKDialog(getFrame(), "Process strings", true);
		dia.addContent(p);
		dia.show();
		if (dia.wasCancelled())
			return;
		int colN = colNs.elementAt(list.getSelectedIndex());
		colNs.removeElementAt(list.getSelectedIndex());

		if (cbExtractWords.getState()) {
			WordCounter wc = new WordCounter();
			for (int i = 0; i < table.getDataItemCount(); i++) {
				String str = table.getAttrValueAsString(colN, i);
				if (str != null) {
					wc.getWordsFromString(str.toLowerCase());
				}
			}
			if (wc.getNUniqueWords() < 1) {
				showMessage("No words have been extracted!", true);
				return;
			}
			wc.removeJoinedWords();
			wc.sortWords();
			DataTable tRes = new DataTable();
			tRes.setName("Unique words from " + table.getAttributeName(colN));
			tRes.addAttribute("Word", "word", AttributeTypes.character);
			tRes.addAttribute("Frequency", "frequency", AttributeTypes.integer);
			for (int i = 0; i < wc.wordCounts.size(); i++) {
				ObjectWithCount oc = wc.wordCounts.elementAt(i);
				DataRecord rec = new DataRecord(String.valueOf(i + 1), oc.obj.toString());
				tRes.addDataRecord(rec);
				rec.setAttrValue(oc.obj.toString(), 0);
				rec.setNumericAttrValue(oc.count, String.valueOf(oc.count), 1);
			}
			DataLoader dLoader = core.getDataLoader();
			dLoader.addTable(tRes);
		} else if (cbFindCombinations.getState()) {
			p = new Panel(new BorderLayout());
			p.add(new Label("Enter the words, one word per line"), BorderLayout.NORTH);
			TextArea tar = new TextArea(20, 60);
			p.add(tar, BorderLayout.CENTER);
			pp = new Panel(new ColumnLayout());
			Checkbox cbPreceding = new Checkbox("find preceding words", true);
			pp.add(cbPreceding);
			Checkbox cbFollowing = new Checkbox("find following words", true);
			pp.add(cbFollowing);
			p.add(pp, BorderLayout.SOUTH);
			dia = new OKDialog(getFrame(), "Enter words", true);
			dia.addContent(p);
			dia.show();
			if (dia.wasCancelled())
				return;
			String txt = tar.getText();
			if (txt == null || txt.trim().length() < 1) {
				showMessage("No words have been entered!", true);
				return;
			}
			StringTokenizer st = new StringTokenizer(txt.trim(), "\r\n");
			Vector<String> words = new Vector<String>(50, 50);
			while (st.hasMoreTokens()) {
				String str = st.nextToken().trim();
				if (str.length() > 0) {
					words.addElement(str.toLowerCase());
				}
			}
			if (words.size() < 1) {
				showMessage("No words have been entered!", true);
				return;
			}
			Vector<String> combinations = new Vector(100, 100);
			for (int i = 0; i < table.getDataItemCount(); i++) {
				String str = table.getAttrValueAsString(colN, i);
				if (str == null) {
					continue;
				}
				str = str.toLowerCase();
				for (int j = 0; j < words.size(); j++) {
					String word = words.elementAt(j);
					int len = word.length();
					int wIdx = str.indexOf(word);
					while (wIdx >= 0) {
						if (cbPreceding.getState() && wIdx > 0) {
							//extract the previous word
							String str1 = str.substring(0, wIdx).trim(), prevWord = null;
							st = new StringTokenizer(str1, " .,;-");
							while (st.hasMoreTokens()) {
								prevWord = st.nextToken();
							}
							if (prevWord != null) {
								String comb = prevWord + " " + word;
								if (!combinations.contains(comb)) {
									combinations.addElement(comb);
								}
							}
						}
						if (cbFollowing.getState() && wIdx + len < str.length()) {
							//extract the following word
							String str1 = str.substring(wIdx + len).trim(), nextWord = null;
							st = new StringTokenizer(str1, " .,;-");
							while (st.hasMoreTokens()) {
								nextWord = st.nextToken();
							}
							if (nextWord != null) {
								String comb = word + " " + nextWord;
								if (!combinations.contains(comb)) {
									combinations.addElement(comb);
								}
							}
						}
						wIdx = str.indexOf(word, wIdx + len);
					}
				}
			}
			if (combinations.size() < 1) {
				showMessage("No combinations have been found!", true);
				return;
			}
			DataTable tRes = new DataTable();
			tRes.setName("Word combinations from " + table.getAttributeName(colN));
			tRes.addAttribute("Combination", "combination", AttributeTypes.character);
			for (int i = 0; i < combinations.size(); i++) {
				DataRecord rec = new DataRecord(String.valueOf(i + 1), combinations.elementAt(i));
				tRes.addDataRecord(rec);
				rec.setAttrValue(combinations.elementAt(i), 0);
			}
			DataLoader dLoader = core.getDataLoader();
			dLoader.addTable(tRes);
		} else if (cbCountFreqInText.getState() || cbCountDistances.getState()) {
			p = new Panel(new BorderLayout());
			pp = new Panel(new ColumnLayout());
			p.add(pp, BorderLayout.NORTH);
			pp.add(new Label("Count frequencies of terms in text lines", Label.CENTER));
			cbg = new CheckboxGroup();
			Checkbox cbUseTextArea = new Checkbox("Take the text lines from the text area:", true, cbg);
			pp.add(cbUseTextArea);
			TextArea tar = new TextArea(20, 60);
			p.add(tar, BorderLayout.CENTER);
			pp = new Panel(new ColumnLayout());
			p.add(pp, BorderLayout.SOUTH);
			Checkbox cbUseTable = new Checkbox("Take the text lines from a table column", false, cbg);
			pp.add(cbUseTable);
			Checkbox cbSynonyms = null;
			Choice chSynColumns = null;
			if (colNs.size() > 0) {
				pp.add(new Line(false));
				cbSynonyms = new Checkbox("Take synonyms of the terms from the column:", false);
				pp.add(cbSynonyms);
				chSynColumns = new Choice();
				for (int i = 0; i < colNs.size(); i++) {
					chSynColumns.add(table.getAttributeName(colNs.elementAt(i)));
				}
				pp.add(chSynColumns);
			}
			dia = new OKDialog(getFrame(), "Specify text", true);
			dia.addContent(p);
			dia.show();
			if (dia.wasCancelled())
				return;
			Vector<String> text = new Vector<String>(500, 200);
			AttributeDataPortion tblText = null;
			if (cbUseTextArea.getState()) {
				String txt = tar.getText();
				if (txt == null || txt.trim().length() < 1) {
					showMessage("No words have been entered!", true);
					return;
				}
				StringTokenizer st = new StringTokenizer(txt.trim(), "\r\n");
				while (st.hasMoreTokens()) {
					String str = st.nextToken().trim();
					if (str.length() > 0) {
						text.addElement(str.toLowerCase());
					}
				}
				if (text.size() < 1) {
					showMessage("No text have been entered!", true);
					return;
				}
			} else {
				tblText = TableProcessor.selectTable(core);
				if (tblText == null)
					return;
				if (tblText.getDataItemCount() < 1) {
					showMessage("No data in the table!", true);
					return;
				}
				IntArray txtColNs = new IntArray(tblText.getAttrCount(), 10);
				list = new List(Math.min(tblText.getAttrCount(), 5));
				for (int i = 0; i < tblText.getAttrCount(); i++) {
					Attribute at = tblText.getAttribute(i);
					if (at.getType() == AttributeTypes.character) {
						txtColNs.addElement(i);
						list.add(at.getName());
					}
				}
				if (txtColNs.size() < 1) {
					showMessage("The table has no columns with string values!", true);
					return;
				}
				list.select(0);
				p = new Panel(new BorderLayout());
				p.add(new Label("Select the table column containing strings:"), BorderLayout.NORTH);
				p.add(list, BorderLayout.CENTER);
				dia = new OKDialog(getFrame(), "Specify text", true);
				dia.addContent(p);
				dia.show();
				if (dia.wasCancelled())
					return;
				int tCN = txtColNs.elementAt(list.getSelectedIndex());
				for (int i = 0; i < tblText.getDataItemCount(); i++) {
					String str = tblText.getAttrValueAsString(tCN, i);
					if (str != null) {
						text.addElement(str.toLowerCase());
					}
				}
				if (text.size() < 1) {
					showMessage("No text strings have been extracted!", true);
					return;
				}
			}
			showMessage(text.size() + " text strings available", false);
			int synCN = -1;
			if (cbSynonyms != null && cbSynonyms.getState()) {
				synCN = colNs.elementAt(chSynColumns.getSelectedIndex());
			}
			int nLines = text.size(), nTerms = table.getDataItemCount();
			boolean occursInLine[][] = new boolean[nTerms][nLines];
			for (int i = 0; i < nTerms; i++) {
				for (int j = 0; j < nLines; j++) {
					occursInLine[i][j] = false;
				}
			}
			for (int i = 0; i < nTerms; i++) {
				String term = table.getAttrValueAsString(colN, i);
				if (term == null) {
					continue;
				}
				term = term.toLowerCase();
				for (int j = 0; j < nLines; j++) {
					occursInLine[i][j] = StringUtil.termOccursInText(term, text.elementAt(j), true);
				}
				if (synCN >= 0) {
					String synStr = table.getAttrValueAsString(synCN, i);
					if (synStr != null) {
						StringTokenizer st = new StringTokenizer(synStr, ";,\r\n");
						int nSyn = st.countTokens();
						if (nSyn > 0) {
							String synonyms[] = new String[nSyn];
							for (int k = 0; k < nSyn; k++) {
								synonyms[k] = st.nextToken().toLowerCase();
							}
							for (int j = 0; j < nLines; j++) {
								for (int k = 0; k < nSyn && !occursInLine[i][j]; k++) {
									occursInLine[i][j] = StringUtil.termOccursInText(synonyms[k], text.elementAt(j), true);
								}
							}
						}
					}
				}
			}
/*
      //some terms may include other terms as substrings.
      //In such case, the substrings are not counted in the lines where the term occurs
      for (int i=0; i<nTerms; i++) {
        String term=table.getAttrValueAsString(colN,i);
        if (term==null) continue;
        term=term.toLowerCase();
        if (synCN>=0) {
          String synStr =table.getAttrValueAsString(synCN,i);
          if (synStr!=null)
            term+=";"+synStr;
        }
        for (int j=0; j<nTerms; j++)
          if (j!=i) {
            String word=table.getAttrValueAsString(colN,j);
            boolean termContainsWord=term.indexOf(word)>=0;
            String synStr=null;
            if (!termContainsWord && synCN>=0) {
              synStr =table.getAttrValueAsString(synCN,j);
              if (synStr !=null) {
                StringTokenizer st=new StringTokenizer(synStr,";,\r\n");
                while (st.hasMoreTokens() && !termContainsWord)
                  termContainsWord=term.indexOf(st.nextToken())>=0;
              }
            }
            if (termContainsWord)
              for (int k=0; k<nLines; k++)
                if (occursInLine[i][k])
                  occursInLine[j][k]=false;
          }
      }
*/
			float weights[] = null;
			if (tblText != null) {
				IntArray numColNs = new IntArray(tblText.getAttrCount(), 10);
				Choice chAttr = new Choice();
				for (int i = 0; i < tblText.getAttrCount(); i++)
					if (tblText.isAttributeNumeric(i)) {
						numColNs.addElement(i);
						chAttr.add(tblText.getAttributeName(i));
					}
				if (numColNs.size() > 0) {
					chAttr.select(0);
					p = new Panel(new ColumnLayout());
					p.add(new Label("The text lines are taken from table"));
					p.add(new Label(tblText.getName(), Label.CENTER));
					Checkbox cbUseWeights = new Checkbox("use the weights of the text lines from column", false);
					p.add(cbUseWeights);
					p.add(chAttr);
					dia = new OKDialog(getFrame(), "Use weights?", true);
					dia.addContent(p);
					dia.show();
					if (!dia.wasCancelled() && cbUseWeights.getState()) {
						weights = new float[nLines];
						int wCN = numColNs.elementAt(chAttr.getSelectedIndex());
						for (int i = 0; i < nLines; i++) {
							weights[i] = (float) tblText.getNumericAttrValue(wCN, i);
							if (Float.isNaN(weights[i])) {
								weights[i] = 1;
							}
						}
					}
				}
			}
			if (cbCountFreqInText.getState()) {
				String aName = "Frequency in text", pref = aName;
				for (int i = 2; table.findAttrByName(aName) >= 0; i++) {
					aName = pref + " (" + i + ")";
				}
/*
        aName= Dialogs.askForStringValue(getFrame(),"Attribute name?",aName,
          "A new attribute will be added to the table \""+table.getName()+"\"","New attribute",true);
        if (aName==null)
          return;
*/
				table.addAttribute(aName, IdMaker.makeId(aName, (DataTable) table), AttributeTypes.integer);
				((DataTable) table).makeUniqueAttrIdentifiers();
				int cN = table.getAttrCount() - 1;
				for (int i = 0; i < nTerms; i++) {
					int count = 0;
					for (int j = 0; j < nLines; j++)
						if (occursInLine[i][j])
							if (weights == null) {
								++count;
							} else {
								count += Math.round(weights[j]);
							}
					((DataTable) table).setNumericAttributeValue(count, cN, i);
				}
				if (tblText != null && Dialogs.askYesOrNo(getFrame(), "Add feature vectors to the table \"" + tblText.getName() + "\"?", "Feature vectors?")) {
					int fIdx = tblText.getAttrCount();
					tblText.addAttribute("Features", "features", AttributeTypes.character);
					int aIdx0 = tblText.getAttrCount();
					for (int i = 0; i < nTerms; i++) {
						String term = table.getAttrValueAsString(colN, i);
						tblText.addAttribute((term == null) ? "Term " + (i + 1) : term.toLowerCase(), "feature_" + (i + 1), AttributeTypes.integer);
					}
					int aIdxMask = tblText.getAttrCount();
					tblText.addAttribute("Mask", "mask", AttributeTypes.character);
					((DataTable) tblText).makeUniqueAttrIdentifiers();
					for (int j = 0; j < nLines; j++) {
						StringBuffer sbMask = new StringBuffer(nTerms);
						String featureStr = null;
						DataRecord rec = ((DataTable) tblText).getDataRecord(j);
						for (int i = 0; i < nTerms; i++) {
							int value = occursInLine[i][j] ? 1 : 0;
							rec.setNumericAttrValue(value, String.valueOf(value), aIdx0 + i);
							sbMask.append(occursInLine[i][j] ? '1' : '0');
							if (occursInLine[i][j])
								if (featureStr == null) {
									featureStr = tblText.getAttributeName(aIdx0 + i);
								} else {
									featureStr += ";" + tblText.getAttributeName(aIdx0 + i);
								}
						}
						rec.setAttrValue(featureStr, fIdx);
						rec.setAttrValue(sbMask.toString(), aIdxMask);
					}
					showMessage("The feature vectors have been produced", false);
				}
			} else {
				long freeMem = Runtime.getRuntime().freeMemory(), needMem = ((long) nTerms) * nTerms * Float.SIZE / 8;
				if (needMem >= freeMem / 3) {
					System.out.println("Garbage collector started, free memory before: " + freeMem);
					Runtime.getRuntime().gc();
					freeMem = Runtime.getRuntime().freeMemory();
					System.out.println("Garbage collector finished, free memory after: " + freeMem);
				}
				float distMatrix[][] = null;
				try {
					distMatrix = new float[nTerms][nTerms];
				} catch (OutOfMemoryError out) {
					System.out.println("Similarity among terms: not enough memory for distance matrix, need: " + needMem);
					showMessage("Similarity among terms: not enough memory for distance matrix, need: " + needMem, true);
					return;
				}
				System.out.println("Similarity among terms: distance matrix constructed!");
				showMessage("Similarity among terms: distance matrix constructed!", false);
				for (int i = 0; i < nTerms; i++) {
					distMatrix[i][i] = 0;
					for (int j = i + 1; j < nTerms; j++) {
						distMatrix[i][j] = distMatrix[j][i] = 1; //max possible distance
					}
				}
				float counts[] = new float[nTerms];
				for (int i = 0; i < nTerms; i++) {
					counts[i] = 0;
					for (int j = 0; j < nLines; j++)
						if (occursInLine[i][j])
							if (weights != null) {
								counts[i] += weights[j];
							} else {
								++counts[i];
							}
				}
				DataTable distTbl = new DataTable();
				distTbl.setName("Co-occurrences of terms from \"" + table.getName() + "\"");
				distTbl.addAttribute("Term 1", "term_1", AttributeTypes.character);
				distTbl.addAttribute("Frequency 1", "freq_1", AttributeTypes.integer);
				distTbl.addAttribute("Term 2", "term_2", AttributeTypes.character);
				distTbl.addAttribute("Frequency 2", "freq_2", AttributeTypes.integer);
				distTbl.addAttribute("Joint frequency", "joint_freq", AttributeTypes.integer);
				distTbl.addAttribute("Distance", "distance", AttributeTypes.real);
				int n = 0;
				for (int i = 0; i < nTerms; i++)
					if (counts[i] > 0) {
						String term1 = table.getAttrValueAsString(colN, i);
						for (int j = i + 1; j < nTerms; j++)
							if (counts[j] > 0) {
								int coOccurCount = 0;
								for (int k = 0; k < nLines; k++)
									if (occursInLine[i][k] && occursInLine[j][k])
										if (weights != null) {
											coOccurCount += weights[k];
										} else {
											++coOccurCount;
										}
								if (coOccurCount > 0) {
									distMatrix[i][j] = distMatrix[j][i] = 1.0f - coOccurCount * 2.0f / (counts[i] + counts[j]);
									DataRecord rec = new DataRecord(String.valueOf(++n));
									distTbl.addDataRecord(rec);
									rec.setAttrValue(term1, 0);
									rec.setAttrValue(table.getAttrValueAsString(colN, j), 2);
									rec.setNumericAttrValue(counts[i], String.valueOf(Math.round(counts[i])), 1);
									rec.setNumericAttrValue(counts[j], String.valueOf(Math.round(counts[j])), 3);
									rec.setNumericAttrValue(coOccurCount, String.valueOf(coOccurCount), 4);
									rec.setNumericAttrValue(distMatrix[i][j], StringUtil.floatToStr(distMatrix[i][j], 5), 5);
								}
							}
					}
				((DataTable) table).setDistanceMatrix(distMatrix);
				String title = ("Distances based on co-occurrences of terms");
				title = Dialogs.askForStringValue(getFrame(), "Give an explanatory title for the distance matrix?", title, null, "Title", false);
				((DataTable) table).setDistMatrixTitle(title);
				DataLoader dLoader = core.getDataLoader();
				dLoader.addTable(distTbl);
				if (Dialogs.askYesOrNo(core.getUI().getMainFrame(), "Store the computed distances in a file?", "Distances > File")) {
					writeDistanceMatrix((DataTable) table, colN);
				}
				showMessage("Distance matrix computed!", false);
			}
		}
	}

	public void writeDistanceMatrix(DataTable table, int termColN) {
		float distMatrix[][] = table.getDistanceMatrix();
		if (distMatrix == null)
			return;

		FileDialog fd = new FileDialog(core.getUI().getMainFrame(), "Specify the file to store the distances");
		fd.setFile("*.txt");
		fd.setMode(FileDialog.SAVE);
		fd.show();
		if (fd.getDirectory() == null)
			return;
		String fname = fd.getDirectory() + fd.getFile();

		int nObj = table.getDataItemCount();

		FileOutputStream out = null;
		try {
			out = new FileOutputStream(fname);
		} catch (IOException ioe) {
			showMessage("Could not create file " + fname, true);
			System.out.println(ioe);
		}
		if (out == null)
			return;
		DataOutputStream dos = new DataOutputStream(out);
		try {
			dos.writeBytes("From,To,distance\n");
			for (int i = 0; i < nObj - 1; i++) {
				String term1 = table.getAttrValueAsString(termColN, i);
				if (term1 != null) {
					for (int j = i + 1; j < nObj; j++) {
						String term2 = table.getAttrValueAsString(termColN, j);
						if (term2 == null || term2.equalsIgnoreCase(term1)) {
							continue;
						}
						String str = term1 + "," + term2 + ",";
						if (Float.isNaN(distMatrix[i][j])) {
							str += "NaN";
						} else {
							str += distMatrix[i][j];
						}
						dos.writeBytes(str + "\n");
					}
				}
			}
		} catch (IOException ioe) {
			showMessage("Error writing file " + fname, true);
			System.out.println(ioe);
		}
		try {
			out.close();
		} catch (IOException e) {
		}
	}
}
