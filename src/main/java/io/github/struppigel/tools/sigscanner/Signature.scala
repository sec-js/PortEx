/*******************************************************************************
 * Copyright 2014 Karsten Hahn
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
package io.github.struppigel.tools.sigscanner

import io.github.struppigel.parser.ScalaIOUtil._
import org.apache.logging.log4j.LogManager

import scala.Array.canBuildFrom
import scala.PartialFunction._

/**
 * @author Karsten Hahn
 *
 * Creates a signature instance with the values specified
 *
 * @param name the name of the packer or compiler
 * @param epOnly true iff only found at the entry point of the PE file
 * @param signature the byte signature, where the Option None denotes unknown
 * 				    bytes ("??" in signature file)
 * @param untilOffset TODO currently unused
 * @param isDotNet Some if a .NET condition is applied
 */
class Signature(val name: String, val epOnly: Boolean, 
    val pattern: Array[Option[Byte]]) {
  
  def bytesMatched(): Int =
      pattern.count(cond(_) { case Some(s) => true })
      
  def signatureString(): String = bytes2hex(pattern, " ")

  override def toString(): String =
    s"""|name: $name
    	|signature: $signatureString
	    |ep_only: $epOnly""".stripMargin

}

object Signature {
  
  private val logger = LogManager.getLogger(Signature.getClass.getName)
  
  /**
   * @param name name of packer or compiler
   * @param ep epOnly flag
   * @param sig byte sequence as hex string, uknown bytes are marked as "??"
   * @return an instance of Signature with the fields applied
   */
  def apply(name: String, ep: Boolean, sig: String): Signature = {
    apply(name, ep, sig, 0, 0L, None)
  }
  
  /**
   * @param name name of packer or compiler
   * @param ep epOnly flag
   * @param sig byte sequence as hex string, uknown bytes are marked as "??"
   * @param addOffset pattern starts at this offset -- TODO currently unused
   * @param untilOffset pattern must start until end offset TODO currently unused
   * @param isDotNet Some if sample being a .NET sample is part of the condition, None otherwise
   * @return an instance of Signature with the fields applied
   */
  def apply(name: String, ep: Boolean, sig: String, addOffset: Int, untilOffset: Long, isDotNet : Option[Boolean]): Signature = {
    // convert quotes to hex string representation
    val quote = '\''
    var inQuote = false
    var convertedSig = sig.toList.foldRight(""){(ch, concat) => 
      if(ch == quote) { 
        inQuote = !inQuote 
        concat
      } else if(inQuote) {
        val convertedChar = Integer.toHexString(ch & 0xFF)
        convertedChar + concat
      } else {
        ch + concat
      }
    }
    // convert hex string to byte array
    val sigbytes = hex2bytes(convertedSig)
    // create signature
    new Signature(name, ep, sigbytes)
  }

  /**
   * Converts a hex string representation to an array of option bytes. "??" is
   * converted to None.
   *
   * @param hex the hex string with ?? indicating unknown byte values
   * @return an array of option bytes with None indicating unknown values
   */
  def hex2bytes(hex: String): Array[Option[Byte]] = {
    val arr = hex.replaceAll("[^0-9A-Fa-f?]", "").sliding(2, 2).toArray
    arr.map(str => str match {
      case "??" => None
      case _ => Some(Integer.parseInt(str, 16).toByte)
    })
  }
}
