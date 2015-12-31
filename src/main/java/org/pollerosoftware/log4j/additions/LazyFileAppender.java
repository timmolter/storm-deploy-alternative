package org.pollerosoftware.log4j.additions;

import java.io.*;
import org.apache.log4j.FileAppender;
import org.apache.log4j.helpers.LogLog;

/**
 * This class inherit from FileAppender and it is different from
 * its parent class only in the fact that doen't create the log file 
 * immediately after the initialization, but only at the first write 
 * operation (Lazy log file creation).
 *   
 * @author Alessio Pollero
 * @version 1.0
 */
public class LazyFileAppender extends FileAppender {

	@Override
	public synchronized void setFile(String fileName, boolean append, boolean bufferedIO, int bufferSize) throws IOException  {

		LogLog.debug("setFile called: " + fileName + ", " + append);

		// It does not make sense to have immediate flush and bufferedIO.
		if(bufferedIO) {
			setImmediateFlush(false);
		}

		reset();

		//Creation of the LazyFileOutputStream object (the responsible of the log writing operations)
		LazyFileOutputStream ostream = new LazyFileOutputStream(fileName, append);

		Writer fw = createWriter(ostream);
		if(bufferedIO) {
			fw = new BufferedWriter(fw, bufferSize);
		}
		this.setQWForFiles(fw);
		this.fileName = fileName;
		this.fileAppend = append;
		this.bufferedIO = bufferedIO;
		this.bufferSize = bufferSize;
		writeHeader();
		LogLog.debug("setFile ended");
	}
}