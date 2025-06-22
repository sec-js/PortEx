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
package io.github.struppigel.parser;

import io.github.struppigel.parser.sections.idata.SymbolDescription;
import io.github.struppigel.parser.sections.rsrc.Resource;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/**
 * Utilities for file IO needed to read maps and arrays from the text files in
 * the data subdirectory of PortEx.
 * <p>
 * The default specification text files are CSV, where the values are separated
 * by semicolon and a new entry begins on a new line.
 * 
 * @author Katja Hahn
 * 
 */
public final class IOUtil {

	private static final Logger logger = LogManager.getLogger(IOUtil.class
			.getName());
	/**
	 * System independend newline.
	 */
	public static final String NL = System.getProperty("line.separator");

	/**
	 * The default delimiter for a line in a specification file.
	 */
	public static final String DEFAULT_DELIMITER = ";";
	/**
	 * The folder that contains the specification files
	 */
	public static final String SPEC_DIR = "/data/";

	/**
	 * Forbidden. This is a utility class.
	 */
	private IOUtil() {
	}

	/**
	 * Dumps the given part of the inFile to outFile. The part to dump is
	 * represented by loc. This will read safely from inFile which means
	 * locations outside of inFile are written as zero bytes to outFile. 
	 * 
	 * outFile must not exist yet. 
	 * inFile must exist and must not be a directory.
	 * 
	 * @param loc
	 * @param inFile
	 * @throws IOException
	 */
	public static void dumpLocationToFile(PhysicalLocation loc, File inFile,
			File outFile) throws IOException {
		Preconditions.checkArgument(!outFile.exists());
		Preconditions.checkArgument(inFile.exists(), inFile.isFile());

		final int BUFFER_SIZE = 2048;

		try (RandomAccessFile raf = new RandomAccessFile(inFile, "r");
				FileOutputStream out = new FileOutputStream(outFile)) {
			byte[] buffer;
			long remainingBytes = loc.size();
			long offset = loc.from();
			while (remainingBytes >= BUFFER_SIZE) {
				buffer = loadBytesSafely(offset, BUFFER_SIZE, raf);
				out.write(buffer);
				remainingBytes -= BUFFER_SIZE;
				offset += BUFFER_SIZE;
			}

			if (remainingBytes > 0) {
				buffer = loadBytesSafely(offset, (int) remainingBytes, raf);
				out.write(buffer);
			}
		}
	}

	/**
	 * Loads the bytes at the offset into a byte array with the given length
	 * using the raf. If EOF, the byte array is zero padded. If offset < 0 it
	 * will return a zero sized array.
	 * 
	 * @param offset
	 *            to seek
	 * @param length
	 *            of the byte array, equals number of bytes read
	 * @param raf
	 *            the random access file
	 * @return byte array
	 * @throws IOException
	 *             if unable to read the bytes
	 */
	public static byte[] loadBytesSafely(long offset, int length,
			RandomAccessFile raf) throws IOException {
		Preconditions.checkArgument(length >= 0);
		if (offset < 0)
			return new byte[0];
		raf.seek(offset);
		int readsize = length;
		if (readsize + offset > raf.length()) {
			readsize = (int) (raf.length() - offset);
		}
		if (readsize < 0) {
			readsize = 0;
		}
		byte[] bytes = new byte[readsize];
		raf.readFully(bytes);
		bytes = padBytes(bytes, length);
		assert bytes.length == length;
		return bytes;
	}

	/**
	 * Return array with length size, if length is greater than the previous
	 * size, the array is zero-padded at the end.
	 * 
	 * @param bytes
	 * @param length
	 * @return zero-padded array with length size
	 */
	private static byte[] padBytes(byte[] bytes, int length) {
		byte[] padded = new byte[length];
		for (int i = 0; i < bytes.length; i++) {
			padded[i] = bytes[i];
		}
		return padded;
	}

	/**
	 * Loads the bytes at the offset into a byte array with the given length
	 * using the raf.
	 * 
	 * @param offset
	 *            to seek
	 * @param length
	 *            of the byte array, equals number of bytes read
	 * @param raf
	 *            the random access file
	 * @return byte array
	 * @throws IOException
	 *             if unable to read the bytes
	 */
	public static byte[] loadBytes(long offset, int length, RandomAccessFile raf)
			throws IOException {
		if (length < 0) {
			length = 0;
			logger.error("Negative length: " + length);
		}
		if (offset < 0) {
			offset = 0;
			logger.error("Negative offset: " + offset);
		}
		// stay within file bounds
		offset = Math.min(offset, raf.length());
		long endOffset = Math.min(offset + length, raf.length());
		length = (int) (endOffset - offset);
		raf.seek(offset);
		byte[] bytes = new byte[length];
		raf.readFully(bytes);
		return bytes;
	}

	public static String readNullTerminatedUTF8String(long offset,
			RandomAccessFile raf) throws IOException {
		int length = nullTerminatorIndex(offset, raf);
		byte[] bytes = loadBytes(offset, length, raf);
		return new String(bytes, StandardCharsets.UTF_8).trim();
	}

	private static int nullTerminatorIndex(long offset, RandomAccessFile raf)
			throws IOException {
		raf.seek(offset);
		int index = 0;
		byte b;
		do {
			b = raf.readByte();
			index++;
		} while (b != 0);
		return index;
	}

	/**
	 * Reads the entries of a Header and returns a map containing the
	 * {@link HeaderKey} as key and {@link StandardField} as value.
	 * <p>
	 * The map is initialized with all possible HeaderKeys of the subtype and
	 * empty fields.
	 * <p>
	 * The passed instances must not be null and the specName must not be empty.
	 * 
	 * @param clazz
	 *            the concrete subclass of the HeaderKey
	 * @param specFormat
	 *            the format of the specification file
	 * @param specification
	 *            the specification to be used for reading the fields
	 * @param headerbytes
	 *            the bytes of the header
	 * @param headerOffset
	 *            the file offset to the start of the headerbytes
	 * @param <T>
	 *            the type for the header key that the returned map shall use
	 * @return header entries
	 */
	public static <T extends Enum<T> & HeaderKey> Map<T, StandardField> readHeaderEntriesForSpec(
			Class<T> clazz, SpecificationFormat specFormat,
			List<String[]> specification, byte[] headerbytes, long headerOffset) {
		assert clazz != null && specFormat != null && headerbytes != null;

		/* initializers */
		// init a full map with default fields. Fields that can be read are
		// changed subsequently
		Map<T, StandardField> data = initFullEnumMap(clazz);

		// use the specification format to get the right indices
		int descriptionIndex = specFormat.description;
		int offsetIndex = specFormat.offset;
		int lengthIndex = specFormat.length;
		int keyIndex = specFormat.key;

		// loop through every line in the specification, put read data to the
		// map
		for (String[] specs : specification) {
			// get the enum type for the key string
			T key = Enum.valueOf(clazz, specs[keyIndex].trim());
			// read offset, length, and description, offset is relative to
			// header
			int offset = Integer.parseInt(specs[offsetIndex].trim());
			int length = Integer.parseInt(specs[lengthIndex].trim());
			String description = specs[descriptionIndex];
			// get the absolute file offset for the current field
			long fieldOffset = headerOffset + offset;
			// check if value is entirely contained in the headerbytes
			long value;
			if (headerbytes.length >= offset + length) {
				value = ByteArrayUtil.getBytesLongValue(headerbytes, offset, length);
				data.put(key, new StandardField(key, description, value,
						fieldOffset, length));
			} else {
				// value not entirely contained in array, so use a safe method
				// to fetch it
				value = ByteArrayUtil.getBytesLongValueSafely(headerbytes, offset, length);
				// ... and print a warning message
				logger.warn("offset + length larger than headerbytes given");
			}
			// add data to map
			data.put(key, new StandardField(key, description, value,
					fieldOffset, length));
		}
		return data;
	}

	/**
	 * Reads the entries of a Header and returns a map containing the
	 * {@link HeaderKey} as key and {@link StandardField} as value.
	 * <p>
	 * The map is initialized with all possible HeaderKeys of the subtype and
	 * empty fields.
	 * <p>
	 * The passed instances must not be null and the specName must not be empty.
	 * 
	 * @param clazz
	 *            the concrete subclass of the HeaderKey
	 * @param specFormat
	 *            the format of the specification file
	 * @param specName
	 *            the name of the specification file (not the path to it), must
	 *            not be empty.
	 * @param headerbytes
	 *            the bytes of the header
	 * @param headerOffset
	 *            the file offset to the start of the headerbytes
	 * @param <T>
	 *            the type for the header key that the returned map shall use
	 * @return header entries
	 * @throws IOException
	 *             if specification file can not be read
	 */
	public static <T extends Enum<T> & HeaderKey> Map<T, StandardField> readHeaderEntries(
			Class<T> clazz, SpecificationFormat specFormat, String specName,
			byte[] headerbytes, long headerOffset) throws IOException {
		assert specName != null && specName.trim().length() > 0;
		// get the specification
		List<String[]> specification = readArray(specName);
		// call readHeaderEntries for specification
		return readHeaderEntriesForSpec(clazz, specFormat, specification, headerbytes,
				headerOffset);
	}

	/**
	 * Initialized a map containing all keys of the Enum T as map-key and
	 * default StandardFields as map-value.
	 * <p>
	 * Ensures that no null value is returned.
	 * 
	 * @return the fully initialized map
	 */
	public static <T extends Enum<T> & HeaderKey> Map<T, StandardField> initFullEnumMap(
			Class<T> clazz) {
		Map<T, StandardField> map = new EnumMap<>(clazz);
		// loop through all values of the Enum type and add a dummy field
		for (T key : clazz.getEnumConstants()) {
			// TODO init correct description string
			StandardField dummy = new StandardField(key, "this field was not set", 0L, 0L, 0L);
			map.put(key, dummy);
		}
		return map;
	}

	/**
	 * Reads the specified file into a map. The first value is used as key. The
	 * rest is put into a list and used as map value. Each entry is one line of
	 * the file.
	 * <p>
	 * Ensures that no null value is returned.
	 * <p>
	 * Uses the default {@link #DEFAULT_DELIMITER}
	 * 
	 * @param filename
	 *            the name of the specification file (not the path to it)
	 * @return a map with the first column as keys and the other columns as
	 *         values.
	 * @throws IOException
	 *             if unable to read the specification file
	 */
	public static Map<String, String[]> readMap(String filename)
			throws IOException {
		return readMap(filename, DEFAULT_DELIMITER);
	}

	/**
	 * Reads the specified file into a map. The first value is used as key. The
	 * rest is put into a list and used as map value. Each entry is one line of
	 * the file.
	 * <p>
	 * Ensures that no null value is returned.
	 * 
	 * @param filename
	 *            the name of the specification file (not the path to it)
	 * @param delimiter
	 *            the delimiter regex for one column
	 * @return a map with the first column as keys and the other columns as
	 *         values.
	 * @throws IOException
	 *             if unable to read the specification file
	 */
	public static Map<String, String[]> readMap(String filename,
			String delimiter) throws IOException {
		Map<String, String[]> map = new TreeMap<>();
		// read spec-file as resource, e.g., from a jar
		try (InputStreamReader isr = new InputStreamReader(
				IOUtil.class.getResourceAsStream(SPEC_DIR + filename));
				BufferedReader reader = new BufferedReader(isr)) {
			String line;
			while ((line = reader.readLine()) != null) {
				// split line into the values
				String[] values = line.split(delimiter);
				// put first element as key, rest as array of value-strings
				map.put(values[0], Arrays.copyOfRange(values, 1, values.length));
			}
			return map;
		}
	}

	/**
	 * Reads a list of symbol descriptions
	 *
	 * @return list of SymbolDescriptions
	 * @throws IOException
	 */
	public static List<SymbolDescription> readSymbolDescriptions()
			throws IOException {
		String filename = "importcategories.txt";
		List<SymbolDescription> list = new ArrayList<>();
		List<String[]> lines = readArray(filename);
		String category = "";
		String subCategory = null;
		for (String[] array : lines) {
			if (array.length > 0) {
				String mainItem = array[0];
				if (mainItem.startsWith("[")) {
					category = mainItem;
					subCategory = null;
				} else if (mainItem.startsWith("<")) {
					subCategory = mainItem;
				} else {
					String description = null;
					if (array.length > 1) {
						description = array[1];
					}
					list.add(new SymbolDescription(mainItem, Optional
							.fromNullable(description), category, Optional
							.fromNullable(subCategory)));
				}
			}
		}
		return list;
	}

	/**
	 * Reads the specified file from the specification directory into a list of
	 * arrays. Each array is the entry of one line in the file.
	 * <p>
	 * Ensures that no null value is returned.
	 * <p>
	 * Uses the default {@link #DEFAULT_DELIMITER}
	 * 
	 * @param filename
	 *            the name of the specification file (not the path to it)
	 * @return a list of arrays, each array representing a line in the spec
	 * @throws IOException
	 *             if unable to read the specification file
	 */
	public static List<String[]> readArray(String filename) throws IOException {
		return readArray(filename, DEFAULT_DELIMITER);
	}

	/**
	 * Reads the specified file from the specification directory into a list of
	 * arrays. Each array is the entry of one line in the file.
	 * <p>
	 * Ensures that no null value is returned.
	 * 
	 * @param filename
	 *            the name of the specification file (not the path to it)
	 * @param delimiter
	 *            the delimiter regex for one column
	 * @return a list of arrays, each array representing a line in the spec
	 * @throws IOException
	 *             if unable to read the specification file
	 */
	public static List<String[]> readArray(String filename, String delimiter)
			throws IOException {
		List<String[]> list = new LinkedList<>();
		// read spec-file as resource, e.g., from a jar
		try (InputStreamReader isr = new InputStreamReader(
				IOUtil.class.getResourceAsStream(SPEC_DIR + filename));
				BufferedReader reader = new BufferedReader(isr)) {
			String line;
			while ((line = reader.readLine()) != null) {
				// split line into the values
				String[] values = line.split(delimiter);
				// add all values as entry to the list
				list.add(values);
			}
			return list;
		}
	}

	/**
	 * Reads the specified file into a list of arrays. Each array is the entry
	 * of one line in the file.
	 * <p>
	 * This method allows to read from files outside of the packaged jar.
	 * <p>
	 * Ensures that no null value is returned.
	 * <p>
	 * Uses the default {@link #DEFAULT_DELIMITER}
	 * 
	 * @param file
	 *            the file to read from, UTF-8 encoded
	 * @return a list of arrays, each array representing a line in the spec
	 * @throws IOException
	 *             if unable to read the file
	 */
	public static List<String[]> readArrayFrom(File file) throws IOException {
		List<String[]> list = new LinkedList<>();
		// read spec as UTF-8 encoded file
		try (BufferedReader reader = Files.newBufferedReader(file.toPath(),
				Charset.forName("UTF-8"))) {
			String line;
			while ((line = reader.readLine()) != null) {
				// split line into the values
				String[] values = line.split(DEFAULT_DELIMITER);
				// add all values as entry to the list
				list.add(values);
			}
			return list;
		}
	}

	/**
	 * Load the raw bytes of the resource in the given PE
	 * @param resource
	 * @param pedata
	 * @return byte array with raw resource bytes
	 * @throws IOException
	 */
	public static byte[] loadBytesOfResource(Resource resource, PEData pedata) throws IOException {
		return IOUtil.loadBytesSafely(resource.rawBytesLocation().from(), (int) resource.rawBytesLocation().size(),
				new RandomAccessFile(pedata.getFile(), "r"));
	}

	/**
	 * Describes the format/indices of the specification file.
	 * 
	 */
	public static class SpecificationFormat {
		/**
		 * the index of the key
		 */
		public int key;
		/**
		 * the index of the entry's description
		 */
		public int description;
		/**
		 * the index of the value's offset
		 */
		public int offset;
		/**
		 * the index of the value's length
		 */
		public int length;

		/**
		 * Creates a specification format with the given indices.
		 * 
		 * @param key
		 *            the index of the key
		 * @param description
		 *            the index of the entry's description
		 * @param offset
		 *            the index of the value's offset
		 * @param length
		 *            the index of the value's length
		 */
		public SpecificationFormat(int key, int description, int offset,
				int length) {
			this.description = description;
			this.offset = offset;
			this.length = length;
			this.key = key;
		}
	}

}
