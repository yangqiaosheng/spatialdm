package de.fraunhofer.de.exifreader;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.DirectoryWalker;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.lang.StringUtils;

public class ImageLoader extends DirectoryWalker<File> {

	public ImageLoader(FileFilter fileFilter) {
		super(fileFilter, -1);
	}

	public ImageLoader() {
		super();
	}

	public List<File> load(File startDirectory) throws IOException {
		List<File> results = new ArrayList<File>();
		walk(startDirectory, results);
		return results;
	}

	@Override
	protected void handleFile(File file, int depth, Collection<File> results) {
//		if(StringUtils.endsWithIgnoreCase(file.getName(), ".jpg")){
			results.add(file);
			System.out.println(file.getAbsolutePath());
//		}
	}

}
