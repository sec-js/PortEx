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
package io.github.struppigel.tools.anomalies;

/**
 * Represents the semantics of an anomaly.
 * <p>
 * The type is a rough description of the anomaly.
 * 
 * @author Katja Hahn
 *
 */
public enum AnomalyType {

	/**
	 * These values or characteristics have ben set, but are deprecated
	 */
	DEPRECATED,
	/**
	 * These are values that violate the PE specification
	 */
	WRONG,
	/**
	 * These values differ from the standard value. That doesn't mean they are
	 * wrong, they just might be unusual.
	 */
	NON_DEFAULT,
	/**
	 * These values or characteristics are reserved and should be zero, but
	 * were set nevertheless
	 */
	RESERVED,
	/**
	 * Represents unusual location, order, number or size of PE structures, e.g.
	 * collapsed, overlapping, moved to overlay
	 */
	STRUCTURE;
}
