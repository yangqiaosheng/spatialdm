package data_load.readers;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.lib.lang.Language;
import spade.vis.spec.DataSourceSpec;

/**
* The class for reading data from the clipboard
*/
public class ClipBoardReader extends ASCIIReader {
	static ResourceBundle res = Language.getTextResource("data_load.readers.Res");
	protected String clipBoardContent = null;
	/**
	* If several data portions are loaded from the clipboard, this counter
	* is used to make unique identifiers of the sources of these data portions
	* in order to to distringuish them
	*/
	protected static int count = 0;

	/**
	* Reading of data from the clipboard.
	* Redefines the loadData() method from the ASCIIReader class, because
	* no stream is to be opened.
	* Calls readSpecific() to read data in the ASCII format.
	*/
	@Override
	public boolean loadData(boolean mayAskUser) {
		if (dataError)
			return false;
		if (dataReadingInProgress) {
			waitDataReadingFinish();
			return !dataError;
		}
		if (!mayAskUser) {
			setDataReadingInProgress(true);
		}
		Clipboard cb = null;
		try {
			cb = Toolkit.getDefaultToolkit().getSystemClipboard();
		} catch (Exception e) {
		}
		if (cb == null) {
			//following text:"Cannot get access to the clipboard"
			showMessage(res.getString("Cannot_get_access_to"), true);
			dataError = true;
			setDataReadingInProgress(false);
			return false;
		}
		Transferable content = cb.getContents(this);
		if (content == null) {
			//following text:"No data in the clipboard!"
			showMessage(res.getString("No_data_in_the"), true);
			dataError = true;
			setDataReadingInProgress(false);
			return false;
		}
		clipBoardContent = null;
		try {
			clipBoardContent = (String) content.getTransferData(DataFlavor.stringFlavor);
		} catch (Exception e) {
			//following text:"Cannot get data from the clipboard: "
			showMessage(res.getString("Cannot_get_data_from") + e.toString(), true);
			dataError = true;
			setDataReadingInProgress(false);
			return false;
		}
		if (clipBoardContent == null || clipBoardContent.length() < 1) {
			//following text:"No data in the clipboard!"
			showMessage(res.getString("No_data_in_the"), true);
			dataError = true;
			setDataReadingInProgress(false);
			return false;
		}
		if (spec == null) {
			spec = new DataSourceSpec();
		}
		spec.source = ("clipboard_") + (++count);
		if (spec.idFieldN < 0 && spec.idFieldName == null) {
			//try to get metadata
			Vector header = getNDataLines(20);
			if (header == null || header.size() < 1) {
				dataError = true;
				setDataReadingInProgress(false);
				return false;
			}
			//start a dialog for getting information about the data from the user
			ASCIIReadDlg dlg = new ASCIIReadDlg(getFrame(), spec, header);
			dlg.show();
			if (dlg.wasCancelled()) {
				setDataReadingInProgress(false);
				return false;
			}
		}
		//following text:"Start reading data from the clipboard"
		showMessage(res.getString("Start_reading_data1"), false);
		constructTable();
		dataError = !readSpecific(new BufferedReader(new StringReader(clipBoardContent)), true);
		if (dataError) {
			setDataReadingInProgress(false);
			return false;
		}
		if (spec.name != null) {
			table.setName(spec.name);
		} else {
			table.setName(res.getString("Data_from_clipboard_") + count + ")");
		}
		setDataReadingInProgress(false);
		tryGetGeoObjects();
		return true;
	}

	/**
	* Gets at most N lines from the clipboard. Tries to find and interpret
	* metadata. The lines are not interpreted, simply stored in a vector.
	* Creates a BufferedReader and calls getNDataLines(BufferedReader,int)
	*/
	@Override
	public Vector getNDataLines(int N) {
		if (clipBoardContent == null)
			return null;
		return getNDataLines(new BufferedReader(new StringReader(clipBoardContent)), N);
	}

}
