/**
 * *****************************************************************************
 * Copyright 2022 Karsten Philipp Boris Hahn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ****************************************************************************
 */
package io.github.struppigel.tools.anomalies

import io.github.struppigel.parser.IOUtil.NL
import io.github.struppigel.parser.ScalaIOUtil.filteredString
import io.github.struppigel.parser.sections.SectionLoader
import io.github.struppigel.parser.sections.clr.CLRSection

import scala.collection.mutable.ListBuffer

/**
 * Scans the .NET related structures for anomalies
 */
trait ClrScanning extends AnomalyScanner {
  abstract override def scanReport(): String =
    "Applied CLR Anomaly Scanning" + NL + super.scanReport

  abstract override def scan(): List[Anomaly] = {
    val clr = new SectionLoader(data).maybeLoadCLRSection()
    val anomalyList = ListBuffer[Anomaly]()
    if (clr.isPresent) {
      anomalyList ++= checkStreamHeaders(clr.get)
      anomalyList ++= checkStringsHeap(clr.get)
      anomalyList ++= checkVersionStringNotReadable(clr.get)
    }
    super.scan ::: anomalyList.toList
  }

  /**
   * Anomaly scanning the stream headers of the metadata root directory
   * @param clr the CLR section object
   * @return List of anomalies in stream headers
   */
  private def checkStreamHeaders(clr : CLRSection): List[Anomaly] = {
    val metadataRoot = clr.metadataRoot
    val names = metadataRoot.streamHeaders.map(_.name)
    // check stream names for duplicates
    val duplicates = names.diff(names.distinct).distinct
    val dupAnomalies = for(name <- duplicates) yield
      ClrStreamAnomaly(metadataRoot,
        metadataRoot.streamHeaders.find(_.name == name).get,
        "There are several streams named '" + name + "', there should only be one",
        AnomalySubType.DUPLICATED_STREAMS)
    // check for non-zero terminated streams
    val nonTermsAnomalies = {
      for (header <- metadataRoot.nonZeroTerminatedHeaders) yield
        ClrStreamAnomaly(metadataRoot,
          header, s"The name of the stream header ${header.name} is not zero terminated",
          AnomalySubType.NON_ZERO_TERMINATED_STREAM_NAME
        )
    }
    dupAnomalies ::: nonTermsAnomalies
  }

  private def checkVersionStringNotReadable(clr: CLRSection): List[Anomaly] =
    if(clr.getMetadataRoot.versionStringNotReadable) {
      List(
        new ClrMetadaRootAnomaly(clr.getMetadataRoot,
          "Version string in Metadata Root is not readable",
          AnomalySubType.METADATA_ROOT_VERSION_STRING_BROKEN
        ))
    } else Nil

  /**
   * Anomalies in the #Strings heap
   * @param clr CLR section object
   * @return list of anomalies in #Strings heap
   */
  private def checkStringsHeap(clr : CLRSection): List[Anomaly] = {
    val metadataRoot = clr.metadataRoot
    val streamHeader = metadataRoot.streamHeaders.find(_.name == "#Strings")
    val stringsHeap = metadataRoot.maybeGetStringsHeap
    if(!stringsHeap.isPresent || !streamHeader.isDefined) return Nil
    val heap= stringsHeap.get()
    val filteredStrings = heap.getArray().filter(filteredString(_).length > 0)
    // one less because first string is always empty
    val unreadableCount = heap.getArray().length - filteredStrings.length - 1
    if(unreadableCount > 0) {
      return List(new ClrStreamAnomaly(
        metadataRoot, streamHeader.get,
        "There is a total of " + unreadableCount + " unreadable strings in #Strings, this is a common obfuscation",
        AnomalySubType.UNREADABLE_CHARS_IN_STRINGS_HEAP
      ))
    }
    Nil
  }
}
