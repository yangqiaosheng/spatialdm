package spade.lib.lang;

import java.util.ResourceBundle;
import java.util.Vector;

/**
* This class can be accessed by all language tables (LangTable classes from
* different packages) in order to determine the current interface language.
*/
public class Language {
  /**
  * Identifier of the current user interface language.
  */
  public static String currLanguage="english";
  /**
  * Specification of the suffixes to be used for getting texts in different
  * languages for the UI.
  */
  protected static Suffixes suff=new Suffixes();
  /**
  * Classnames for already constructed resources
  */
  protected static Vector resourceClassNames=null;
  /**
  * Instances of already constructed resources (to avoid multiple instances
  * of the same resource).
  */
  protected static Vector resourceInstances=null;

  /**
  * Completes the given class name with the appropriate suffix, according to
  * the current current user interface language. Then tries to construct an
  * instance of this class. If not successful, creates an instance of the
  * default class, i.e. without any suffix.
  * The argument must be the full class name, i.e. including the name of the
  * package.
  */
  public static ResourceBundle getTextResource (String resourceClassName) {
    if (resourceClassName==null) return null;
    String clName=resourceClassName;
    String suffix=suff.getSuffix(currLanguage);
    int nAttempts=1;
    if (suffix!=null && suffix.length()>0) {
      clName+=suffix; nAttempts=2;
    }
    for (int i=0; i<nAttempts; i++) {
      if (resourceClassNames!=null) {
        int idx=resourceClassNames.indexOf(clName);
        if (idx>=0)
          return (ResourceBundle)resourceInstances.elementAt(idx);
      }
      try {
        ResourceBundle res=(ResourceBundle)Class.forName(clName).newInstance();
        if (res!=null) {
          if (resourceClassNames==null) {
            resourceClassNames=new Vector(50,20);
            resourceInstances=new Vector(50,20);
          }
          resourceClassNames.addElement(clName);
          resourceInstances.addElement(res);
          return res;
        }
      } catch (Exception e) {}
      if (i+1<nAttempts)
        clName=resourceClassName;
    }
    return null;
  }

  /**
  * When this variable is true, the language tables write their contents
  * to text files. These files can then be edited by non-programmers and
  * then again transformed into code in Java.
  */
  public static boolean writeLangTblToFile=false;
  /**
   * Retrieves the string with the given identifier from the specified resource
   * file. If not found, returns the original string.
   */
  public static String getString (String id, ResourceBundle res) {
    if (id==null) return null;
    if (res==null) return id;
    try {
      String str=res.getString(id);
      return str;
    } catch (Throwable ex) {}
    return id;
  }
}