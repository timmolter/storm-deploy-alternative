package org.pollerosoftware.log4j.additions;

import java.io.*;

/**
 * This class inherit from OutputStream and wraps a standard FileOutputStream
 * to simply create a lazy initialized FileOutputStream, instead of creating
 * the file when the constructor is called, it creates the file only when 
 * it is really need(when some writing operation happens).
 * 
 * @author Alessio Pollero
 * @version 1.0
 */
public class LazyFileOutputStream extends OutputStream {

	protected File file;
	protected boolean append;

	protected Object streamLock = new Object();
	protected boolean streamOpen = false;
	private FileOutputStream oStream;


	//Constructors

	public LazyFileOutputStream(File f) {
		this.file = f;
	}

	public LazyFileOutputStream(File f, boolean append) {
		this.file = f;
		this.append = append;
	}

	public LazyFileOutputStream(String name) {
		this(name != null ? new File(name) : null);
	}

	public LazyFileOutputStream(String name, boolean append) {
		this(name != null ? new File(name) : null, append);
	}

	//Methods implementation

	/**
	 * This method is the key component of the class, it gets 
	 * the wrapped FileOutputStream object if already initialized
	 * or if not it generates it in a thread safe way.
	 * This kind of implementation allows to call the initialization
	 * of the underlying FileOutputStream object only when needed.
	 * 
	 * @return the wrapped FileOutputStream object
	 * @throws FileNotFoundException if the file can't be created
	 */
	protected FileOutputStream outputStream() throws FileNotFoundException {
		synchronized (streamLock) {
			if(!streamOpen) {
				oStream = new FileOutputStream(file, append);

				streamOpen = true;
			}
		}
		return oStream;
	}

	@Override
	public void close() throws IOException {
		super.close();
		if(streamOpen) 
			outputStream().close();
	}

	@Override
	public void flush() throws IOException {
		super.flush();
		if(streamOpen) 
			outputStream().flush();
	}

	@Override
	public void write(int b) throws IOException {
		outputStream().write(b);
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		outputStream().write(b, off, len);
	}

	@Override
	public void write(byte[] b) throws IOException {
		outputStream().write(b);
	}
}