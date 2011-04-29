package de.fraunhofer.de.exifreader;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.DirectoryWalker;

public class ImageLoader extends DirectoryWalker<File> {

	public List<File> load(File startDirectory) throws IOException {
		List<File> results = new ArrayList<File>();
		walk(startDirectory, results);
		return results;
	}

	@Override
	protected void handleFile(File file, int depth, Collection<File> results) {
		results.add(file);
	}

}
