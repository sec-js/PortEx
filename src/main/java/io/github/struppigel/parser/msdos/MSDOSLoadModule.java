/*******************************************************************************
 * Copyright 2014 Katja Hahn
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package io.github.struppigel.parser.msdos;

import io.github.struppigel.parser.PEModule;
import io.github.struppigel.parser.IOUtil;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import static io.github.struppigel.parser.IOUtil.NL;
import static io.github.struppigel.parser.msdos.MSDOSHeaderKey.FILE_PAGES;
import static io.github.struppigel.parser.msdos.MSDOSHeaderKey.LAST_PAGE_SIZE;

/**
 * Responsible for dumping the MSDOS load module. 
 * This class might be removed in future versions.
 * @Beta
 * @author Katja Hahn
 * 
 */
public class MSDOSLoadModule implements PEModule {

	private static final int PAGE_SIZE = 512; // in Byte

	private final MSDOSHeader header;
	private final File file;
	private byte[] loadModuleBytes;

	/**
	 * Creates the MSDOSLoadModule instance based on the
	 *              {@link MSDOSHeader} of the given file
	 * @param header
	 * @param file
	 */
	public MSDOSLoadModule(MSDOSHeader header, File file) {
		this.header = header;
		this.file = file;
	}

	/**
	 * Reads the load module.
	 * TODO public?
	 * @throws IOException if file can not be read
	 */
	public void read() throws IOException {
		long headerSize = header.getHeaderSize();
		int loadModuleSize = getLoadModuleSize();

		try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
			raf.seek(headerSize);
			loadModuleBytes = new byte[loadModuleSize];
			raf.readFully(loadModuleBytes);
		}
	}

	/**
	 * Calculates the size of the load module.
	 * 
	 * @return load module size
	 */
	public int getLoadModuleSize() {
		return (int) (getImageSize() - header.getHeaderSize());
	}

	/**
	 * Calculates the size of the image based on {@link MSDOSHeader} information
	 * FIXME too large, surpasses PE signature
	 * @return image size
	 */
	public int getImageSize() {
		int filePages = (int) header.get(MSDOSHeaderKey.FILE_PAGES);
		int lastPageSize = (int) header.get(MSDOSHeaderKey.LAST_PAGE_SIZE);

		int imageSize = (filePages - 1) * PAGE_SIZE + lastPageSize;
		if (lastPageSize == 0) {
			imageSize += PAGE_SIZE;
		}
		return imageSize;
	}

	/**
	 * Returns the bytes of the load module.
	 * 
	 * @return bytes of the load module
	 * @throws IOException
	 */
	public byte[] getDump() throws IOException {
		if (loadModuleBytes == null) {
			read();
		}
		return loadModuleBytes.clone();
	}

	/**
	 * {@inheritDoc}
	 */
	public String getInfo() {
		return "----------------" + IOUtil.NL + "MSDOS Load Module" + IOUtil.NL
				+ "----------------" + IOUtil.NL + IOUtil.NL + "image size:" + getImageSize()
				+ IOUtil.NL + "load module size: " + getLoadModuleSize() + IOUtil.NL;
	}

	/**
	 * {@inheritDoc}
	 */
	public long getOffset() {
		return header.getHeaderSize();
	}

}
