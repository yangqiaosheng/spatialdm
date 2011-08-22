package spade.analysis.geocomp.trans;

import java.util.GregorianCalendar;
import java.util.Vector;

import spade.analysis.geocomp.mutil.CInteger;
import spade.analysis.geocomp.mutil.SortVector;
import spade.analysis.geocomp.mutil.UChar;

public class Calc {
	static String OperationSet = "B+U*/ACSQIXDKLGEFTROP<>=NYZ&!|?";
	/*  '<','>','=','N'{<>},'Y'{<=},'Z'{>=},
	'&'{ and },'!'{not},'|'{ or },
	'?'{ if (a,b,c) if a then b else c} */
	public int CalcErrCode = 0;
	public Vector Track;
	public Vector VCalc;
	boolean OprFound;
	String s, sv;
	public int n0; // Number of initial elements

	public Calc() {
		Track = new Vector(8);
		VCalc = new Vector(4);
		s = new String("");
		addElement(0.0);
		n0 = 0;
	}

	public Calc(int n) {
		Track = new Vector(8);
		VCalc = new Vector(4);
		s = new String("");
		addElement(0.0);
		for (int i = 1; i <= n; i++) {
			addElement(-(double) i);
		}
		n0 = n;
	}

	public CalcOpr addOperation(char t) {
		CalcOpr o;
		switch (t) {
		case '+':
			o = new SumOpr();
			break;

		case 'U':
			o = new DifOpr();
			break;

		case '*':
			o = new MulOpr();
			break;

		case '/':
			o = new DivOpr();
			break;

		case 'K':
			o = new BlkOpr();
			break;

		case 'A':
			o = new AbsOpr();
			break;

		case 'F':
			o = new SignOpr();
			break;

		case 'C':
			o = new CosOpr();
			break;

		case 'S':
			o = new SinOpr();
			break;

		case 'T':
			o = new AtanOpr();
			break;

		case 'Q':
			o = new SqrtOpr();
			break;

		case 'L':
			o = new LogOpr();
			break;

		case 'G':
			o = new Log10Opr();
			break;

		case 'E':
			o = new ExpOpr();
			break;

		case 'R':
			o = new RandOpr();
			break;

		case 'O':
			o = new RoundOpr();
			break;

		case 'P':
			o = new IntrOpr();
			break;

		case 'D':
			o = new DayOpr();
			break;

		case 'I':
			o = new MinOpr();
			break;

		case 'X':
			o = new MaxOpr();
			break;

		case '<':
			o = new LtOpr();
			break;

		case '>':
			o = new GtOpr();
			break;

		case '=':
			o = new EqOpr();
			break;

		case 'N':
			o = new NeOpr();
			break;

		case 'Y':
			o = new LeOpr();
			break;

		case 'Z':
			o = new GeOpr();
			break;

		case '&':
			o = new AndOpr();
			break;

		case '|':
			o = new OrOpr();
			break;

		case '!':
			o = new NotOpr();
			break;

		case '?':
			o = new IfOpr();
			break;

		default: // B - trivial operator
			o = new CalcOpr();
			break;
		}
		o.Operation = t;
		Track.addElement(o);
		addElement(-(double) VCalc.size());
		o.Res = (CalcVal) VCalc.elementAt(VCalc.size() - 1);
		return o;
	}

	public void addElement(double d) {
		VCalc.addElement(new CalcVal(d));
	}

	public int[] indexInUse() {
		SortVector v = new SortVector(4);
		for (int i = 0; i < Track.size(); i++) {
			CalcOpr o = (CalcOpr) Track.elementAt(i);
			for (int k = 1; k <= n0; k++) {
				CalcVal c = (CalcVal) VCalc.elementAt(k);
				if (c == o.El1 || c == o.El2 || c == o.El3) {
					v.Insert(new CInteger(k));
				}
			}
		}

		if (v.size() == 0)
			return null;
		int inUse[] = new int[v.size()];
		for (int i = 0; i < v.size(); i++) {
			CInteger k = (CInteger) v.elementAt(i);
			inUse[i] = k.intValue();
		}
		return inUse;
	}

	public void setElement(int i, double d) {
		CalcVal c = (CalcVal) VCalc.elementAt(i);
		c.v = d;
	}

	public double getElement(int i) {
		CalcVal c;
		c = (CalcVal) VCalc.elementAt(i);
		return c.v;
	}

	public int Nel() {
		return VCalc.size();
	}

	public String CalcErrMsg() {
		switch (CalcErrCode) {
		case 1:
			return " Real divide by zero ";
		case 2:
			return " Negative SQRT argument ";
		case 3:
			return " Negative LOG argument ";
		case 4:
			return " Negative LOG argument ";
		case 5:
			return " EXP argument too big ";
		case 6:
			return " Wrong date ";
		}
		return " Unknown error ";
	}

	/*  Compile new formula */
	public boolean MakeCalcTrack(String S) {
		int j, Pl, Pr;

		/* ------------ Blank remove ------------ */
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < S.length(); i++) {
			try {
				if (S.charAt(i) != ' ') {
					sb.append(S.charAt(i));
				}
			} catch (Exception e) {
				return false;
			}
		}
		s = new String(sb);

		if (s.length() == 0)
			return false;

		/* ------------ Upcase        ------------ */
		s = s.toUpperCase();

		/* ------------ Substitution  ------------ */
		FuncSub("COS", 'C');
		FuncSub("SIN", 'S');
		FuncSub("ATAN", 'T');
		FuncSub("ABS", 'A');
		FuncSub("SIGN", 'F');
		FuncSub("SQRT", 'Q');
		FuncSubU('U');
		FuncSub("LN", 'L');
		FuncSub("LG", 'G');
		FuncSub("EXP", 'E');
		FuncSub("BLK", 'K');
		FuncSub("RAND", 'R');
		FuncSub("ROUND", 'O');

		FuncSub("MIN", 'I');
		FuncSub("MAX", 'X');
		FuncSub("DAY", 'D');
		FuncSub("INTR", 'P');

		FuncSub("<>", 'N');
		FuncSub("<=", 'Y');
		FuncSub(">=", 'Z');
		FuncSub("IF", '?');

		/* ------------ Bracket loop -------------- */
		do {
			/* ----------> Find Bracket */
			Pr = UChar.strpos2(s, ")", 1);
			if (Pr == 1)
				return false;
			if (Pr > 0) {
				j = Pr;
				do {
					j--;
				} while (s.charAt(j - 1) != '(' && j != 1);
				if (s.charAt(j - 1) != '(' && j == 1)
					return false;
				Pl = j;
				if (Pr - Pl < 2)
					return false;

				/* --------> Solve Bracket */
				sv = UChar.strsub(s, Pl + 1, Pr - Pl - 1);
				if (UChar.strpos2(sv, ",", 1) != 0) {
					if (Pl <= 1)
						return false;
					Pl--;
					sv = SubMN(sv, s.charAt(Pl - 1));
				}
				String stmp = new String(s);
				s = sv;
				if (!SolveBrack())
					return false;

				s = stmp;
				s = UChar.strdelete(s, Pl, Pr - Pl + 1);
				s = UChar.strinsert("$" + (Nel() - 1), s, Pl);
			} /* Pr>0 */

		} while (Pr != 0);

		if (!SolveBrack())
			return false;

		return true;
	} /* MakeCalcTrack */

	boolean posInSet(int P) {
		if (P < 1)
			return false;
		return OperationSet.indexOf(s.charAt(P - 1)) >= 0;
	}

	/* _______________________________________________________ */
	boolean SolveBrack() {
		OprFound = false;

		//System.out.println("SolveBrack : "+s);

		/* ------------ Substitution  ------------ */
		boolean OK = SolveOne('C') && SolveOne('S') && SolveOne('T');
		OK = OK && SolveOne('A') && SolveOne('Q') && SolveOne('L');
		OK = OK && SolveOne('G') && SolveOne('E') && SolveOne('K') && SolveOne('F') && SolveOne('R');

		OK = OK && SolveOne('O') && SolveOne('!');
		OK = OK && SolveGroup('/') && SolveGroup('*');
		OK = OK && SolveGroup('U') && SolveGroup('+');

		OK = OK && SolveGroup('<') && SolveGroup('Y') && SolveGroup('>') && SolveGroup('Z');
		OK = OK && SolveGroup('=') && SolveGroup('N') && SolveGroup('&') && SolveGroup('|');
		OK = OK && SolveGroup('I') && SolveGroup('X');

		OK = OK && SolveIntr('P') && Solve3('D') && Solve3('?');

		if (!OK)
			return false;

		if (OprFound)
			return true;

		return addSimple(s);
	} /* SolveCalcBrack */

	/* ----------- Solve  one ---------------------- */
	boolean SolveOne(char ch) {
		int Pr, Pl, l;

		int P = UChar.strpos2(s, ch + "", 1);
		while (P != 0) {
			/* -----> Find Pr */
			l = s.length();
			Pr = P;
			do {
				Pr++;
				if (Pr > l)
					return false;
			} while (!((Pr == l) || posInSet(Pr)));

			if (posInSet(Pr)) {
				Pr--;
			}

			/* -----> Next step */
			OprFound = true;
			CalcOpr o = addOperation(ch);

			String buf = UChar.strsub(s, (P + 1), (Pr - P));
			o.El1 = getIndex(buf);

			if (o.El1 == null)
				return false;

			s = UChar.strdelete(s, P, (Pr - P + 1));
			s = UChar.strinsert("$" + (Nel() - 1), s, P);

			//System.out.println("SolveOne : "+s);

			P = UChar.strpos2(s, ch + "", 1);

		} /* while */

		return true;
	} /* SolveOne */

	/* ----------- Solve  group ---------------------- */
	boolean SolveGroup(char ch) {
		int Pr, Pl, P, l;

		P = UChar.strpos2(s, ch + "", 1);
		while (P != 0) {
			/* -----> Find Pr */
			l = s.length();
			Pr = P;
			do {
				Pr++;
				if (Pr > l)
					return false;
			} while (!((Pr == l) || posInSet(Pr)));

			if (posInSet(Pr)) {
				Pr--;
			}

			/* -----> Find Pl */
			Pl = P;
			do {
				Pl--;
				if (Pl < 1)
					return false;
			} while (!((Pl == 1) || posInSet(Pl)));

			if (posInSet(Pl)) {
				Pl++;
			}

			if (Pl == P || Pr == P)
				return false;

			/* -----> Next step */
			OprFound = true;
			CalcOpr o = addOperation(ch);

			String buf = UChar.strsub(s, Pl, (P - Pl));
			o.El1 = getIndex(buf);

			if (o.El1 == null)
				return false;

			buf = UChar.strsub(s, (P + 1), (Pr - P));
			o.El2 = getIndex(buf);

			if (o.El2 == null)
				return false;

			s = UChar.strdelete(s, Pl, (Pr - Pl + 1));
			s = UChar.strinsert("$" + (Nel() - 1), s, Pl);

			//System.out.println("SolveGroup : "+s);
			P = UChar.strpos2(s, ch + "", 1);
			//System.out.println(" Pl : "+Pl+" Pr : "+Pr+" P : "+P);
		} /* while */
		return true;
	} /* SolveGroup */

	/* ----------- Solve  Intr ---------------------- */
	boolean SolveIntr(char ch) {
		boolean first;
		int np;
		int Pr, Pl, P, l;

		np = 0;
		P = 1;
		while (P <= s.length()) {
			if (s.charAt(P - 1) == ch) {
				np++;
			}
			P++;
		}
		if ((np & 1) != 0)
			return false;

		P = UChar.strpos2(s, ch + "", 1);
		if (P == 0)
			return true;

		/* -----> Next step */
		OprFound = true;
		first = true;
		CalcOpr o = addOperation(ch);

		while (P != 0) {
			/* -----> Find Pr */
			l = s.length();
			Pr = P;
			do {
				Pr++;
				if (Pr > l)
					return false;
			} while (!((Pr == l) || posInSet(Pr)));

			if (posInSet(Pr)) {
				Pr--;
			}

			/* -----> Find Pl */
			Pl = P;
			do {
				Pl--;
				if (Pl < 1)
					return false;
			} while (!((Pl == 1) || posInSet(Pl)));

			if (posInSet(Pl)) {
				Pl++;
			}

			if (Pl == P || Pr == P)
				return false;

			String buf = UChar.strsub(s, Pl, (P - Pl));

			if (first) {
				o.El1 = getIndex(buf);
				if (o.El1 == null)
					return false;
				first = false;
				o.Pint = new Vector(8);
				s = UChar.strdelete(s, Pl, (P - Pl + 1));
			} else {
				CalcVal ex = getIndex(buf);
				if (ex == null)
					return false;

				buf = UChar.strsub(s, (P + 1), (Pr - P));

				CalcVal ey = getIndex(buf);
				if (ey == null)
					return false;

				o.Pint.addElement(ex);
				o.Pint.addElement(ey);

				s = UChar.strdelete(s, Pl, (Pr - Pl + 1));
				if (s.length() > 0 && s.charAt(0) == ch) {
					s = UChar.strdelete(s, 1, 1);
				}
			} /* else if first */

			//System.out.println("SolveIntr : "+s);
			P = UChar.strpos2(s, ch + "", 1);
			//System.out.println(" Pl : "+Pl+" Pr : "+Pr+" P : "+P);
		} /* while */

		if (o.Pint == null || o.Pint.size() == 0)
			return false;

		return true;
	} /* SolveIntr */

	/* ----------- Solve3 ---------------------- */
	boolean Solve3(char ch) {
		boolean first;
		int np;
		int Pr, Pl, P, l;

		np = 0;
		P = 1;
		while (P <= s.length()) {
			if (s.charAt(P - 1) == ch) {
				np++;
			}
			P++;
		}
		if ((np & 1) != 0)
			return false;

		P = UChar.strpos2(s, ch + "", 1);
		if (P == 0)
			return true;

		/* -----> Next step */
		OprFound = true;
		first = true;
		CalcOpr o = addOperation(ch);

		while (P != 0 && (o.El1 == null || o.El2 == null || o.El3 == null)) {
			/* -----> Find Pr */
			l = s.length();
			Pr = P;
			do {
				Pr++;
				if (Pr > l)
					return false;
			} while (!((Pr == l) || posInSet(Pr)));

			if (posInSet(Pr)) {
				Pr--;
			}

			/* -----> Find Pl */
			Pl = P;
			do {
				Pl--;
				if (Pl < 1)
					return false;
			} while (!((Pl == 1) || posInSet(Pl)));

			if (posInSet(Pl)) {
				Pl++;
			}

			if (Pl == P || Pr == P)
				return false;

			String buf = UChar.strsub(s, Pl, (P - Pl));

			if (first) {
				o.El1 = getIndex(buf);
				if (o.El1 == null)
					return false;
				first = false;
				s = UChar.strdelete(s, Pl, (P - Pl + 1));
			} else {
				o.El2 = getIndex(buf);
				if (o.El2 == null)
					return false;

				buf = UChar.strsub(s, (P + 1), (Pr - P));

				o.El3 = getIndex(buf);
				if (o.El3 == null)
					return false;

				s = UChar.strdelete(s, Pl, (Pr - Pl + 1));
				if (s.length() > 0 && s.charAt(0) == ch) {
					s = UChar.strdelete(s, 1, 1);
				}
			} /* else if first */

			//System.out.println("Solve3 : "+s);
			P = UChar.strpos2(s, ch + "", 1);
			//System.out.println(" Pl : "+Pl+" Pr : "+Pr+" P : "+P);
		} /* while */

		if (o.El1 == null || o.El2 == null || o.El3 == null)
			return false;

		return true;
	} /* Solve3 */

	public void FuncSub(String sf, char ch) {
		int P;

		do {
			P = UChar.strpos2(s, sf, 1);
			if (P != 0) {
				s = UChar.strdelete(s, P, sf.length());
				s = UChar.charinsert(ch, s, P);
			}
		} while (P != 0);

	}

	public void FuncSubU(char csub) {
		int p, i;
		boolean IsConst, Lbrk;

		p = s.length();
		i = 1;
		while (i <= p) {
			if (s.charAt(i - 1) == '-') {
				char ch = s.charAt(i);
				IsConst = (ch == '.' || ('0' <= ch && ch <= '9'));
				Lbrk = (i == 1 || s.charAt(i - 2) == ',' || s.charAt(i - 2) == '(');

				if (Lbrk && !IsConst) {
					s = UChar.strdelete(s, i, 1);
					s = UChar.strinsert("0" + csub, s, i);
				}
				if (!Lbrk) {
					s = UChar.strdelete(s, i, 1);
					s = UChar.charinsert(csub, s, i);
				}
			}
			i++;
		}

	}

	public String SubMN(String r, char csub) {
		StringBuffer sb = new StringBuffer(r);

		int l = sb.length();
		int P = 0;
		do {
			P++;
			if (P < l && sb.charAt(P - 1) == ',') {
				sb.setCharAt(P - 1, csub);
			}
		} while (P < l);

		return sb.toString();
	}

	int getElement(String buf) {
		Integer i;
		if (!buf.startsWith("$"))
			return -1;
		try {
			i = new Integer(buf.substring(1, buf.length()));
		} catch (Exception e) {
			return -1;
		}
		return i.intValue();
	}

	double getConst(String buf) {
		Double d;
		try {
			d = new Double(buf);
		} catch (Exception e) {
			return Double.NaN;
		}
		return d.doubleValue();
	}

	CalcVal getIndex(String buf) {
		int e = getElement(buf);
		if (e >= 0) {
			if (e >= VCalc.size())
				return null;
			return (CalcVal) VCalc.elementAt(e);
		} else {
			double d = getConst(buf);
			if (Double.isNaN(d))
				return null;
			return new CalcVal(d);
		}
	}

	boolean addSimple(String buf) {
		CalcOpr o = addOperation('B');

		o.El1 = getIndex(buf);

		return o.El1 != null;
	}

	public void printTrack() {
		System.out.println(" Calc : ");
		for (int i = 1; i < VCalc.size(); i++) {
			CalcVal c = (CalcVal) VCalc.elementAt(i);
			System.out.println(" i = " + i + " | " + c.v);
		}
		System.out.println();
		for (int i = 0; i < Track.size(); i++) {
			CalcOpr o = (CalcOpr) Track.elementAt(i);
			System.out.println(" i = " + i + " | " + o);
		}
	}

	public double useTrack() {
		for (int i = 0; i < Track.size(); i++) {
			CalcOpr o = (CalcOpr) Track.elementAt(i);
			CalcErrCode = o.use();
			if (CalcErrCode != 0)
				return Double.NaN;
		}
		CalcVal c = (CalcVal) VCalc.elementAt(VCalc.size() - 1);
		return c.v;
	}

}

class CalcVal {
	public double v;

	public CalcVal() {
		v = 0.0;
	}

	public CalcVal(double v) {
		this.v = v;
	}

	@Override
	public String toString() {
		return "" + v;
	}
}

class CalcOpr {
	public CalcVal Res = null, El1 = null, El2 = null, El3 = null;
	public Vector Pint = null;
	public char Operation = ' ';

	public CalcOpr() {
	}

	public int use() {
		Res.v = El1.v;
		return 0;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer(" O= " + Operation + " Res= " + Res + " E1= " + El1 + " E2= " + El2 + " E3= " + El3);
		if (Pint != null) {
			for (int i = 0; i < Pint.size(); i++) {
				CalcVal c = (CalcVal) Pint.elementAt(i);
				sb.append(" " + c.v);
			}
		}
		return sb.toString();
	}

}

class SumOpr extends CalcOpr {

	public SumOpr() {
	}

	@Override
	public int use() {
		Res.v = El1.v + El2.v;
		return 0;
	}

}

class DifOpr extends CalcOpr {

	public DifOpr() {
	}

	@Override
	public int use() {
		Res.v = El1.v - El2.v;
		return 0;
	}

}

class MulOpr extends CalcOpr {

	public MulOpr() {
	}

	@Override
	public int use() {
		Res.v = El1.v * El2.v;
		return 0;
	}

}

class DivOpr extends CalcOpr {

	public DivOpr() {
	}

	@Override
	public int use() {
		if (El2.v != 0.0) {
			Res.v = El1.v / El2.v;
			return 0;
		} else {
			Res.v = Double.NaN;
			return 1;
		}
	}

}

class BlkOpr extends CalcOpr {

	public BlkOpr() {
	}

	@Override
	public int use() {
		if (!Double.isNaN(El1.v)) {
			Res.v = 1.0;
		} else {
			Res.v = 0;
		}
		return 0;
	}

}

class AbsOpr extends CalcOpr {

	public AbsOpr() {
	}

	@Override
	public int use() {
		Res.v = Math.abs(El1.v);
		return 0;
	}

}

class SignOpr extends CalcOpr {

	public SignOpr() {
	}

	@Override
	public int use() {
		Res.v = (El1.v > 0) ? 1 : (El1.v < 0) ? -1 : 0;
		return 0;
	}

}

class CosOpr extends CalcOpr {

	public CosOpr() {
	}

	@Override
	public int use() {
		Res.v = Math.cos(El1.v);
		return 0;
	}

}

class SinOpr extends CalcOpr {

	public SinOpr() {
	}

	@Override
	public int use() {
		Res.v = Math.sin(El1.v);
		return 0;
	}

}

class AtanOpr extends CalcOpr {

	public AtanOpr() {
	}

	@Override
	public int use() {
		Res.v = Math.atan(El1.v);
		return 0;
	}

}

class SqrtOpr extends CalcOpr {

	public SqrtOpr() {
	}

	@Override
	public int use() {
		if (El1.v >= 0.0) {
			Res.v = Math.sqrt(El1.v);
			return 0;
		} else {
			Res.v = Double.NaN;
			return 2;
		}
	}

}

class LogOpr extends CalcOpr {

	public LogOpr() {
	}

	@Override
	public int use() {
		if (El1.v > 0.0) {
			Res.v = Math.log(El1.v);
			return 0;
		} else {
			Res.v = Double.NaN;
			return 3;
		}
	}

}

class Log10Opr extends CalcOpr {

	public Log10Opr() {
	}

	@Override
	public int use() {
		if (El1.v > 0.0) {
			Res.v = Math.log(El1.v) * 0.4342944819;
			return 0;
		} else {
			Res.v = Double.NaN;
			return 4;
		}
	}

}

class ExpOpr extends CalcOpr {

	public ExpOpr() {
	}

	@Override
	public int use() {
		Res.v = Math.exp(El1.v);
		if (Double.isInfinite(Res.v)) {
			Res.v = Double.NaN;
			return 5;
		} else
			return 0;
	}

}

class RandOpr extends CalcOpr {

	public RandOpr() {
	}

	@Override
	public int use() {
		Res.v = Math.random() * El1.v;
		return 0;
	}

}

class RoundOpr extends CalcOpr {

	public RoundOpr() {
	}

	@Override
	public int use() {
		Res.v = Math.rint(El1.v);
		return 0;
	}

}

class IntrOpr extends CalcOpr {
	int n;

	public IntrOpr() {
	}

	double xi(int i) {
		CalcVal c = (CalcVal) Pint.elementAt(i + i);
		return c.v;
	}

	double yi(int i) {
		CalcVal c = (CalcVal) Pint.elementAt(i + i + 1);
		return c.v;
	}

	@Override
	public int use() {
		if (!Double.isNaN(El1.v)) {
			n = (Pint.size() >> 1) - 1;
			int L, H, I, C;
			double f;
			L = 0;
			H = n;
			if (El1.v <= xi(L)) {
				Res.v = yi(L);
				return 0;
			}
			if (El1.v >= xi(H)) {
				Res.v = yi(H);
				return 0;
			}

			while (L + 1 < H) {
				I = (L + H) >> 1; //median index
				f = xi(I); //its x value
				if (f < El1.v) {
					L = I;
				} else {
					if (El1.v < f) {
						H = I;
					} else {
						Res.v = yi(I);
						return 0;
					}
				}
			}
/*
      while (L <= H) {
        I = (L + H) >> 1; //median index
        f = xi(I); //its x value
        if (f < El1.v) C = -1;
        else {
          if (f > El1.v) C = 1;
          else           C = 0;
        }
        if (C < 0)  L =I + 1;
        else       {
          H = I - 1;
          if (C == 0)  {
            L = I;
          }
        }
      }
*/
//      if (L >= n) L--;
			Res.v = yi(L) + (El1.v - xi(L)) / (xi(L + 1) - xi(L)) * (yi(L + 1) - yi(L));
		} else {
			Res.v = Double.NaN;
		}
		return 0;
	}

}

class MinOpr extends CalcOpr {

	public MinOpr() {
	}

	@Override
	public int use() {
		Res.v = Math.min(El1.v, El2.v);
		return 0;
	}

}

class MaxOpr extends CalcOpr {

	public MaxOpr() {
	}

	@Override
	public int use() {
		Res.v = Math.max(El1.v, El2.v);
		return 0;
	}

}

class DayOpr extends CalcOpr {
	static GregorianCalendar g0 = new GregorianCalendar(1970, 0, 1, 0, 0);
	static long ms = 24 * 60 * 60 * 1000;

	public DayOpr() {
	}

	@Override
	public int use() {
		if (!Double.isNaN(El1.v) && !Double.isNaN(El2.v) && !Double.isNaN(El3.v)) {
			try {
				int YY = (int) Math.round(El1.v);
				int MM = (int) Math.round(El2.v) - 1;
				int DD = (int) Math.round(El3.v);
				GregorianCalendar gc = new GregorianCalendar(YY, MM, DD, 0, 0);
				Res.v = (gc.getTime().getTime() - g0.getTime().getTime()) / ms;
			} catch (Exception e) {
				Res.v = Double.NaN;
				return 6;
			}
		} else {
			Res.v = Double.NaN;
		}
		return 0;
	}

}

class LtOpr extends CalcOpr {

	public LtOpr() {
	}

	@Override
	public int use() {
		if (!Double.isNaN(El1.v) && !Double.isNaN(El2.v)) {
			if (El1.v < El2.v) {
				Res.v = 1.0;
			} else {
				Res.v = 0.0;
			}
		} else {
			Res.v = Double.NaN;
		}
		return 0;
	}

}

class GtOpr extends CalcOpr {

	public GtOpr() {
	}

	@Override
	public int use() {
		if (!Double.isNaN(El1.v) && !Double.isNaN(El2.v)) {
			if (El1.v > El2.v) {
				Res.v = 1.0;
			} else {
				Res.v = 0.0;
			}
		} else {
			Res.v = Double.NaN;
		}
		return 0;
	}

}

class EqOpr extends CalcOpr {

	public EqOpr() {
	}

	@Override
	public int use() {
		if (!Double.isNaN(El1.v) && !Double.isNaN(El2.v)) {
			if (El1.v == El2.v) {
				Res.v = 1.0;
			} else {
				Res.v = 0.0;
			}
		} else {
			Res.v = Double.NaN;
		}
		return 0;
	}

}

class NeOpr extends CalcOpr {

	public NeOpr() {
	}

	@Override
	public int use() {
		if (!Double.isNaN(El1.v) && !Double.isNaN(El2.v)) {
			if (El1.v != El2.v) {
				Res.v = 1.0;
			} else {
				Res.v = 0.0;
			}
		} else {
			Res.v = Double.NaN;
		}
		return 0;
	}

}

class LeOpr extends CalcOpr {

	public LeOpr() {
	}

	@Override
	public int use() {
		if (!Double.isNaN(El1.v) && !Double.isNaN(El2.v)) {
			if (El1.v <= El2.v) {
				Res.v = 1.0;
			} else {
				Res.v = 0.0;
			}
		} else {
			Res.v = Double.NaN;
		}
		return 0;
	}

}

class GeOpr extends CalcOpr {

	public GeOpr() {
	}

	@Override
	public int use() {
		if (!Double.isNaN(El1.v) && !Double.isNaN(El2.v)) {
			if (El1.v >= El2.v) {
				Res.v = 1.0;
			} else {
				Res.v = 0.0;
			}
		} else {
			Res.v = Double.NaN;
		}
		return 0;
	}

}

class AndOpr extends CalcOpr {

	public AndOpr() {
	}

	@Override
	public int use() {
		if (!Double.isNaN(El1.v) && !Double.isNaN(El2.v)) {
			if (El1.v != 0.0 && El2.v != 0.0) {
				Res.v = 1.0;
			} else {
				Res.v = 0.0;
			}
		} else {
			Res.v = Double.NaN;
		}
		return 0;
	}

}

class OrOpr extends CalcOpr {

	public OrOpr() {
	}

	@Override
	public int use() {
		if (!Double.isNaN(El1.v) && !Double.isNaN(El2.v)) {
			if (El1.v != 0.0 || El2.v != 0.0) {
				Res.v = 1.0;
			} else {
				Res.v = 0.0;
			}
		} else {
			Res.v = Double.NaN;
		}
		return 0;
	}

}

class NotOpr extends CalcOpr {

	public NotOpr() {
	}

	@Override
	public int use() {
		if (!Double.isNaN(El1.v)) {
			if (El1.v == 0.0) {
				Res.v = 1.0;
			} else {
				Res.v = 0.0;
			}
		} else {
			Res.v = Double.NaN;
		}
		return 0;
	}

}

class IfOpr extends CalcOpr {

	public IfOpr() {
	}

	@Override
	public int use() {
		if (!Double.isNaN(El1.v)) {
			if (El1.v == 0.0) {
				Res.v = El3.v;
			} else {
				Res.v = El2.v;
			}
		} else {
			Res.v = Double.NaN;
		}
		return 0;
	}

}
