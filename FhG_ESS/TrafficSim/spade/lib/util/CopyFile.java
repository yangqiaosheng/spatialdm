package spade.lib.util;

import java.io.*;
import java.net.URL;
import java.util.Vector;

public class CopyFile {
  public static String errtxt=null;

  /**
  * Checks the presence of the specified file by trying to open it as a stream.
  * If the path starts with HTTP or FILE, accesses the file as a URL, otherwise
  * opens it as a local file
  */
  public static boolean checkExistence (String path) {
    if (path==null) return false;
    int idx=path.indexOf(':');
    boolean isURL=false;
    if (idx>0) {
      String pref=path.substring(0,idx);
      if (pref.equalsIgnoreCase("HTTP") || pref.equalsIgnoreCase("FILE"))
        isURL=true;
    }
    InputStream istream=null;
    try {
      if (isURL) { //try to access the source as a URL
        URL url=new URL(path);
        istream=url.openStream();
      }
      else
        istream=new FileInputStream(path);
    } catch (IOException ioe) { return false; }
    if (istream==null) return false;
    try { istream.close(); } catch (IOException ioe) {}
    return true;
  }

  public static String makeCanonic(String path){
    File file=new File(path);
    try {
      return file.getCanonicalPath();
    } catch (Throwable thr){}
    return file.getPath();
  }

  public static boolean sameFiles(String path1, String path2) {
    if (path1==null || path2==null) return false;
    if (path1.equals(path2)) return true;
    path1=path1.replace('\\','/');
    path2=path2.replace('\\','/');
    if (path1.equals(path2)) return true;
    try {
      File file1=new File(path1), file2=new File(path2);
      try {
        if (file1.getCanonicalPath().equals(file2.getCanonicalPath()))
          return true;
      } catch (Throwable thr1){}
      if (file1.equals(file2)) return true;
    } catch (Throwable thr2){}
    return false;
  }

  public static boolean hasSeparator (String path) {
    return path!=null &&
           (path.indexOf('/')>=0 || path.indexOf('\\')>=0);
  }

  public static int lastSeparatorPos(String path){
    int n1=path.lastIndexOf('/'), n2=path.lastIndexOf('\\');
    if (n2>n1) return n2;
    return n1;
  }

  /**
   * Attaches a separator at the end when needed.
   * @param path - path to a directory. If null, returns null.
   *               If empty string, does not add a separator.
   * @return the path ending with a separator
   */
  public static String attachSeparator (String path) {
    if (path==null) return null;
    path=path.trim();
    if (path.length()<1) return path;
    int idx=lastSeparatorPos(path);
    if (idx==path.length()-1) return path; //already ends with a separator
    if (idx>=0) return path+path.substring(idx,idx+1);
    return path+"/";
  }

  public static boolean isAbsolutePath(String path){
    if (path.indexOf(':')>0) return true; //contains drive or protocol
    if (path.charAt(0)=='/' || path.charAt(0)=='\\') return true;
    return false;
  }

  public static String getDir(String path){
    if (path==null) return null;
    int n=lastSeparatorPos(path);
    if (n<0) return null;
    return path.substring(0,n+1);
  }

  public static String getName(String path){
    if (path==null) return null;
    int n=lastSeparatorPos(path);
    if (n<0) return path;
    return path.substring(n+1);
  }

  public static String getNameWithoutExt(String path){
    String name=getName(path);
    if (name==null) return null;
    int idx=name.indexOf('.');
    if (idx<0) return name;
    return name.substring(0,idx);
  }

  public static String getExtension(String path){
    if (path==null) return null;
    int n=lastSeparatorPos(path);
    if (n<0) n=-1;
    int idx=path.indexOf('.',n+1);
    if (idx<0) return null;
    return path.substring(idx+1);
  }

  public static boolean fileExists(String name) {
    File f=new File(name);
    return f.exists();
  }

  public static boolean fileExists(String dir, String name) {
    File f=new File(dir,name);
    return f.exists();
  }

  public static boolean copyFile(String source, String dest){
    errtxt=null;
    if (source==null || dest==null || source.equals(dest)) return false;
    int bufsize=1024;
    BufferedInputStream in=null;
    try {
      in=new BufferedInputStream(new FileInputStream(source),bufsize);
    } catch (IOException ioe) {
      errtxt="Error: "+ioe;
      return false;
    }
    BufferedOutputStream out=null;
    try {
      out=new BufferedOutputStream(new FileOutputStream(dest),bufsize);
    } catch (IOException ioe) {
      errtxt="Error: "+ioe;
      try {
        in.close();
      } catch (IOException ioe1){}
      return false;
    }
    byte buf[]=new byte[bufsize];
    boolean EOF=false;
    int nbytes=0;
    while (!EOF) {
      try {
        nbytes=in.read(buf,0,bufsize);
      } catch (IOException ioe) {
        errtxt="Error: "+ioe;
        break;
      }
      if (nbytes<=0) EOF=true;
      else
        try {
          out.write(buf,0,nbytes);
        } catch (IOException ioe) {
          errtxt="Error: "+ioe;
          break;
        }
    }
    try {
      in.close();
    } catch (IOException ioe) {}
    try {
      out.close();
    } catch (IOException ioe) {}
    return errtxt==null;
  }

  public static boolean deleteDirectory (String path) {
    return eraseAllFilesInDirectory(path,true);
  }

  public static boolean eraseAllFilesInDirectory (String path) {
    return eraseAllFilesInDirectory(path,false);
  }

  public static boolean eraseAllFilesInDirectory (String path, boolean deleteDirectory) {
    if (path==null) return false;
    File dir=new File(path);
    if (!dir.exists() || !dir.isDirectory()) return false;
    File files[]=null;
    try {
      files=dir.listFiles();
    } catch (Exception e) {
      return false;
    }
    if (files==null || files.length<1) return true; //already empty
    boolean ok=true, hasSubDir=false;
    for (int i=0; i<files.length; i++)
      if (!files[i].isDirectory())
        try {
          ok=ok && files[i].delete();
        } catch (Exception e) {
          ok=false;
        }
      else
        hasSubDir=true;
    if (!ok || !deleteDirectory)
      return ok;
    if (hasSubDir) return false;
    try {
      ok=dir.delete();
    } catch (Exception e) {
      e.printStackTrace();
    }
    return ok;
  }

  /**
   * Returns a vector of instances of File representing the files existing in the
   * given directory. If extension is not null, includes only files with this extension.
   */
  public static Vector getFileList (String pathToDirectory, String extension) {
    if (pathToDirectory==null) return null;
    File dir=new File(pathToDirectory);
    if (!dir.exists() || !dir.isDirectory()) return null;
    File files[]=null;
    try {
      files=dir.listFiles();
    } catch (Exception e) {
      return null;
    }
    if (files==null || files.length<1) return null; //empty
    Vector v=new Vector(files.length,1);
    for (int i=0; i<files.length; i++)
      if (!files[i].isDirectory())
        if (extension==null || extension.equalsIgnoreCase(getExtension(files[i].getName())))
          v.addElement(files[i]);
    if (v.size()<1) return null;
    return v;
  }
  /**
   * Erases all files in the directory except the files from the given vector,
   * which must consist of instances of File
   */
  public static boolean eraseFilesInDirectory (String path, Vector filesToKeep) {
    if (path==null) return false;
    File dir=new File(path);
    if (!dir.exists() || !dir.isDirectory()) return false;
    File files[]=null;
    try {
      files=dir.listFiles();
    } catch (Exception e) {
      return false;
    }
    if (files==null || files.length<1) return true; //already empty
    boolean ok=true;
    for (int i=0; i<files.length; i++)
      if (!files[i].isDirectory()) {
        boolean keep=false;
        if (filesToKeep!=null)
          for (int j=0; j<filesToKeep.size() && !keep; j++)
            keep=files[i].equals(filesToKeep.elementAt(j));
        if (!keep)
          try {
            ok=ok && files[i].delete();
          } catch (Exception e) {
            ok=false;
          }
      }
    return ok;
  }

  /**
   * Returns a vector of instances of File representing the subdirectories
   * of the given directory
   */
  public static Vector getSubDirList (String pathToDirectory) {
    if (pathToDirectory==null) return null;
    File dir=new File(pathToDirectory);
    if (!dir.exists() || !dir.isDirectory()) return null;
    File files[]=null;
    try {
      files=dir.listFiles();
    } catch (Exception e) {
      return null;
    }
    if (files==null || files.length<1) return null; //empty
    Vector v=new Vector(files.length,1);
    for (int i=0; i<files.length; i++)
      if (files[i].isDirectory())
        v.addElement(files[i]);
    if (v.size()<1) return null;
    return v;
  }
}
