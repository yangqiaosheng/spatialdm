package spade.lib.util;

import java.util.Vector;

public class Parameters {
	protected Vector<Object[]> params = null;

	public void setParameter(String name, Object value) {
		if (name == null)
			return;
		if (params == null) {
			params = new Vector<Object[]>(20, 10);
		}
		for (int i = 0; i < params.size(); i++) {
			Object pp[] = params.elementAt(i);
			if (pp != null && pp[0] != null && name.equalsIgnoreCase(pp[0].toString())) {
				pp[1] = value;
				return;
			}
		}
		Object pair[] = new Object[2];
		pair[0] = name;
		pair[1] = value;
		params.addElement(pair);
	}

	public Object getParameter(String name) {
		if (params == null || name == null)
			return null;
		for (int i = 0; i < params.size(); i++) {
			Object pp[] = params.elementAt(i);
			if (pp != null && pp[0] != null && name.equalsIgnoreCase(pp[0].toString()))
				return pp[1];
		}
		return null;
	}

	public String getParameterAsString(String name) {
		Object obj = getParameter(name);
		if (obj != null)
			return obj.toString();
		return null;
	}

	public boolean checkParameterValue(String name, String value) {
		if (name == null)
			return false;
		String str = getParameterAsString(name);
		if (str == null)
			return value == null;
		if (value == null)
			return false;
		return str.equals(value);
	}

	public int getParamCount() {
		if (params == null)
			return 0;
		return params.size();
	}

	public Object[] getParamValuePair(int idx) {
		if (params == null || idx < 0 || idx >= params.size())
			return null;
		return params.elementAt(idx);
	}

	public String[] getParamNames() {
		if (params == null || params.size() < 1)
			return null;
		String names[] = new String[params.size()];
		for (int i = 0; i < params.size(); i++) {
			names[i] = params.elementAt(i)[0].toString();
		}
		return names;
	}

}