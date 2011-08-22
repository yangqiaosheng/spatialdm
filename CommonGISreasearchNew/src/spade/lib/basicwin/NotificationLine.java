package spade.lib.basicwin;

import java.awt.Color;
import java.awt.Label;

import spade.lib.util.ProcessListener;

public class NotificationLine extends Label implements ProcessListener {
	public static final Color msgbk = Color.blue.darker(), msgfg = Color.white, errorbk = Color.red.darker(), errorfg = Color.yellow;
	protected Color standardbk = null;
	protected String defaultMsg = "";

	public NotificationLine(String defaultMsg) {
		super(defaultMsg);
		setDefaultMessage(defaultMsg);
	}

	public void setDefaultMessage(String defaultMsg) {
		this.defaultMsg = defaultMsg;
	}

	@Override
	public void receiveNotification(Object source, String processName, String processState, boolean trouble) {
		showMessage(processName + ": " + processState, trouble);
	}

	public void showDefaultMessage() {
		showMessage(defaultMsg, false);
	}

	public void showMessage(String msg, boolean isError) {
		if (standardbk == null) {
			standardbk = getBackground();
		}
		if (msg == null || msg.length() < 1) {
			setBackground(standardbk);
			setForeground(msgbk);
			msg = defaultMsg;
		} else {
			setBackground((isError) ? errorbk : msgbk);
			setForeground((isError) ? errorfg : msgfg);
		}
		setText(msg);
		if (isShowing()) {
			invalidate();
			getParent().validate();
		}
	}
}