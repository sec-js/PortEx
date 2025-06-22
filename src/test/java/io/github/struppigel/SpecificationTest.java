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
package io.github.struppigel;

import io.github.struppigel.parser.HeaderKey;
import io.github.struppigel.parser.IOUtil;
import io.github.struppigel.parser.coffheader.COFFHeaderKey;
import io.github.struppigel.parser.msdos.MSDOSHeaderKey;
import io.github.struppigel.parser.optheader.DataDirectoryKey;
import io.github.struppigel.parser.optheader.StandardFieldEntryKey;
import io.github.struppigel.parser.optheader.WindowsEntryKey;
import io.github.struppigel.parser.sections.SectionHeaderKey;
import io.github.struppigel.parser.sections.debug.DebugDirectoryKey;
import io.github.struppigel.parser.sections.edata.ExportDirectoryKey;
import io.github.struppigel.parser.sections.idata.DirectoryEntryKey;
import io.github.struppigel.parser.sections.rsrc.ResourceDataEntryKey;
import io.github.struppigel.parser.sections.rsrc.ResourceDirectoryKey;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Tests the specification files and their enums for coherence.
 * 
 * @author Katja Hahn
 * 
 */
public class SpecificationTest {

	private final Map<HeaderKey[], String> headerspecs = new HashMap<>();

	@BeforeClass
	public void prepare() {
		headerspecs.put(COFFHeaderKey.values(), "coffheaderspec");
		headerspecs.put(DataDirectoryKey.values(), "datadirectoriesspec");
		headerspecs.put(DebugDirectoryKey.values(), "debugdirentryspec");
		headerspecs.put(ExportDirectoryKey.values(), "edatadirtablespec");
		headerspecs.put(DirectoryEntryKey.values(), "idataentryspec");
		headerspecs.put(MSDOSHeaderKey.values(), "msdosheaderspec");
		headerspecs.put(StandardFieldEntryKey.values(),
				"optionalheaderstandardspec");
		headerspecs.put(WindowsEntryKey.values(), "optionalheaderwinspec");
		headerspecs.put(ResourceDataEntryKey.values(), "resourcedataentryspec");
		headerspecs.put(ResourceDirectoryKey.values(), "rsrcdirspec");
		headerspecs.put(SectionHeaderKey.values(), "sectiontablespec");
		// TODO resourcetype, machinetype (covered by MachineTypeTest so far), debugtypes
	}

	@Test
	public void headerCoherence() throws IOException {
		int keyIndex = 0;
		for (Entry<HeaderKey[], String> entry : headerspecs.entrySet()) {
			String specname = entry.getValue();
			HeaderKey[] fields = entry.getKey();
			List<String[]> list = IOUtil.readArray(specname);
			assertEquals(list.size(), fields.length);
			for (String[] values : list) {
				String key = values[keyIndex];
				assertTrue(containsKey(Arrays.asList(fields), key));
			}
		}
	}

	private <T> boolean containsKey(List<T> list, String key) {
		for (T item : list) {
			if (item.toString().equals(key)) {
				return true;
			}
		}
		return false;
	}
}
