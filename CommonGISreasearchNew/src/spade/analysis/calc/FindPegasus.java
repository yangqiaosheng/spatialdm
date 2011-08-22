package spade.analysis.calc;

import java.io.File;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: 11-Mar-2008
 * Time: 14:19:02
 * Finds the location of the Pegasus library
 */
public class FindPegasus {
	/**
	 * The location of the Pegasus library
	 */
	public static String pathToPegasus = null;
	/**
	 * The name of the Pegasus library
	 */
	public static String pegasusLibName = null;

	/**
	 * Find the location of the Pegasus library
	 */
	public static boolean findPegasus() {
		String path = null;
		File dir = new File(".");
		if (dir.exists()) {
			String p = dir.getAbsolutePath();
			if (p.length() > 2 && (p.endsWith("/.") || p.endsWith("\\."))) {
				p = p.substring(0, p.length() - 2);
			}
			dir = new File(p);
		}
		String libStr = "";
		if (dir.exists()) {
			File files[] = null;
			try {
				files = dir.listFiles();
			} catch (Exception e) {
			}
			while (path == null && files != null && files.length > 0) {
				System.out.println("Looking for a subdirectory with Pegasus in directory [" + dir.getAbsolutePath() + "]");
				for (int i = 0; i < files.length && path == null; i++)
					if (files[i].isDirectory()) {
						String name = files[i].getName();
						if (name == null || name.length() < 1) {
							continue;
						}
						//System.out.println(name);
						name = name.toLowerCase();
						if (name.startsWith("pegasus")) {
							path = files[i].getAbsolutePath();
							File pegasusFile = new File(path + "/pegasus.dll");
							if (!pegasusFile.exists()) {
								path = null;
							} else {
								System.out.println("Pegasus: " + pegasusFile.getAbsolutePath());
								libStr += name;
							}
						}
					}
				if (path == null) {
					files = null;
					dir = dir.getParentFile();
					if (dir != null && dir.exists()) {
						try {
							files = dir.listFiles();
							libStr = "../" + libStr;
						} catch (Exception e) {
						}
					}
				}
			}
		}
		if (path != null) {
			int idx = path.indexOf(':');
			if (idx > 0) {
				path = path.substring(idx + 1);
			}
			path = path.replace('\\', '/');
			pathToPegasus = path;
			pegasusLibName = libStr;
			System.out.println("Path to Pegasus: [" + pathToPegasus + "]");
			System.out.println("Pegasus library name: [" + pegasusLibName + "]");
			return true;
		}
		return false;
	}

	/**
	 * Returns the path to the directory with Pegasus
	 */
	public static String getPathToPegasus() {
		if (pathToPegasus == null) {
			findPegasus();
		}
		return pathToPegasus;
	}

	/**
	 * Returns the name of the directory with Pegasus
	 */
	public static String getPegasusLibName() {
		if (pegasusLibName == null) {
			findPegasus();
		}
		return pegasusLibName;
	}

}
