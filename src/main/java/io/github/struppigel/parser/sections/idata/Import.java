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
package io.github.struppigel.parser.sections.idata;

import io.github.struppigel.parser.PhysicalLocation;

import java.util.List;

/**
 * Represents a concrete import of a symbol, either via name or via ordinal.
 * 
 * @author Katja Hahn
 *
 */
public interface Import {
	
	Long getDirEntryValue(DirectoryEntryKey key);
	
	List<PhysicalLocation> getLocations();

}
