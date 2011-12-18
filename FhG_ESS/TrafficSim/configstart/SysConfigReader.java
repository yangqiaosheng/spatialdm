package configstart;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.StringTokenizer;

import spade.lib.basicwin.NotificationLine;
import spade.lib.util.StringUtil;
import spade.lib.util.Parameters;

/**
* Reads system configuration: the path to the knowledge base, the path to the
* browser etc. All lines should have the format <parameter>=<value> or
* <parameter>="<value>"
*/
public class SysConfigReader extends ConfigReader {
  /**
  * System parameters
  */
  protected Parameters sysSettings=null;

  public SysConfigReader (Parameters systemSettings, NotificationLine nl) {
    super(nl);
    sysSettings=systemSettings;
  }
  /**
  * Reads system configuration: the path to the knowledge base, the path to the
  * browser etc. All lines should have the format <parameter>=<value> or
  * <parameter>="<value>"
  * The parameters that were read are stored in the system settings
  */
  public boolean readConfiguration (BufferedReader reader) {
    if (reader==null) return false;
    while (true)
      try {
        String s=reader.readLine();
        if (s==null) break;
        s=s.trim();
        //System.out.println(s);
        if (s.length()<1) continue;
        //all lines should have the format <parameter>=<value> or
        //<parameter>="<value>"
        StringTokenizer st=new StringTokenizer(s," =\r\n");
        if (st.countTokens()<2) continue;
        String key=st.nextToken(), val=st.nextToken("=\r\n");
        if (val==null) continue;
        val=StringUtil.removeQuotes(val);
        if (val.length()<1) continue;
        sysSettings.setParameter(key,val);
      } catch (IOException ioe) {
        notifyProcessState("Exception reading configuration: "+ioe,true);
        return false;
      }
    return true;
  }
}