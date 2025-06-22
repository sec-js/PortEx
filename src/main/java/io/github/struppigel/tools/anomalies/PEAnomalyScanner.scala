/**
 * *****************************************************************************
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
 * ****************************************************************************
 */
package io.github.struppigel.tools.anomalies

import io.github.struppigel.parser.IOUtil._
import io.github.struppigel.parser.{PEData, PELoader}

import java.io.File
import scala.collection.JavaConverters._

/**
 * Scans for anomalies and malformations in a PE file.
 *
 * @author Katja Hahn
 */
class PEAnomalyScanner(data: PEData) extends AnomalyScanner(data) {

  /**
   * Scans the PE and returns a report of the anomalies found.
   *
   * @return a description string of the scan
   */
  override def scanReport: String = {
    val report = StringBuilder.newBuilder
    report ++= "Scanned File: " + data.getFile.getName + NL
    for (anomaly <- scan()) {
      report ++= "\t* " + anomaly.description + NL
    }
    report.toString
  }

  /**
   * Scans the PE and returns a (scala-)list of the anomalies found.
   * Returns an empty list if no traits have been added.
   *
   * Use getAnomalies for a Java compatible list.
   *
   * @return (scala-)list of anomalies found
   */
  override def scan: List[Anomaly] = {
    List[Anomaly]()
  }

  /**
   * Returns a list of anomalies that were found in the PE file.
   *
   * @return list of anomalies found
   */
  def getAnomalies: java.util.List[Anomaly] = scan.asJava

}

object PEAnomalyScanner {

  /**
   * Parses the given file and creates a PEAnomalyScanner instance that has all scanning
   *  characteristics applied.
   *
   * @param file the pe file to scan for
   * @return a PEAnomalyScanner instance with the traits applied from the boolean values
   */
  def newInstance(file: File): PEAnomalyScanner = {
    val data = PELoader.loadPE(file)
    newInstance(data)
  }

  /**
   * Creates a PEAnomalyScanner instance that has all scanning characteristics
   * applied.
   *
   * @param data the PEData object created by the PELoader
   * @return a PEAnomalyScanner instance with the traits applied from the boolean values
   */
  def newInstance(data: PEData): PEAnomalyScanner =
    new PEAnomalyScanner(data) with COFFHeaderScanning with OptionalHeaderScanning with SectionTableScanning with
      MSDOSHeaderScanning with RichHeaderScanning with ImportSectionScanning with ExportSectionScanning with
      ResourceSectionScanning with ClrScanning with OverlayScanning

  def apply(file: File): PEAnomalyScanner = newInstance(file)

  def main(args: Array[String]): Unit = {
    val folder = new File("/home/deque/portextestfiles/badfiles/")
    var counter = 0
    val list = List("VirusShare_a90da79e98213703fc3342b281a95094",
      "VirusShare_c125736034171e9ae84eb69fdee6e334", "VirusShare_5dbd4d92a20be612d2293ca10d3cecff")
    for (file <- folder.listFiles() if !list.contains(file.getName())) {
      try {
        val scanner = PEAnomalyScanner(file)
        counter += 1
        println(file.getName())
        if(counter % 10 == 0) println("files read: " + counter)
        val anomalies = scanner.getAnomalies.asScala
       // if (!anomalies.filter(a => a.subtype == AnomalySubType.VIRTUAL_IMPORTS).isEmpty) {
          println(scanner.scanReport)
        //}
      } catch {
        case e: Exception => e.printStackTrace()
      }
    }
  }

}
