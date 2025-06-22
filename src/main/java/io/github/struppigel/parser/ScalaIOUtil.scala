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
package io.github.struppigel.parser

import java.nio.ByteBuffer
import java.util.UUID
import java.io.File
import java.math.BigInteger
import java.nio.ByteOrder

/**
 * Utilities for Scala specific IO and small conversions related to IO.
 * <p>
 * This class is not meant to be used by library users.
 *
 * @author Karsten Hahn
 */
object ScalaIOUtil {

  def nonExistingFileFor(file: File): File = {
    if (!file.exists()) file else {
      var nr = 1
      while (new File(file.getAbsolutePath + "(" + nr + ")").exists()) {
        nr += 1
      }
      new File(file.getAbsolutePath + "(" + nr + ")")
    }
  }
  
  /**
   * Converts an array of Option bytes to its hex string representation. None is
   * converted to "??"
   *
   * @param bytes byte array to be converted
   * @param sep the character(s) that separates to bytes in the string
   * @return string that represents the byte values as hex numbers
   */
  def bytes2hex(bytes: Array[Option[Byte]], sep: String): String = {
    bytes.foldLeft("")((s, b) => b match {
      case None => s + sep + "??"
      case _    => s + sep + "%02x".format(b.get)
    })
  }

  /**
   * Equivalent to try-with-resources statement in Java. Closes the resource
   * automatically in finally.
   *
   * @param closeable the closeable resource, must have a close() method
   * @param f the procedure to be executed within the try statement
   * @return the result of f
   */
  def using[A, B <: { def close(): Unit }](closeable: B)(f: B => A): A =
    try { f(closeable) } finally { closeable.close() }

  /**
   * Converts a long value to a hex string with a prepending '0x'
   *
   * @param value the value to convert
   * @return hex string
   */
  def hex(value: Long): String = "0x" + java.lang.Long.toHexString(value)

  /**
   * Fills an array with 0 bytes of the size
   *
   * @param size of the array
   * @return byte array, zero filled
   */
  def zeroBytes(size: Int): Array[Byte] =
    if (size >= 0) {
      Array.fill(size)(0.toByte)
    } else Array()

  /**
   * Filters all control symbols and extended code from the given string. The
   * filtered string is returned.
   *
   * @return filtered string
   */
  def filteredString(string: String): String = {
    val controlCode: Char => Boolean = (c: Char) => c <= 32 || c == 127
    val extendedCode: Char => Boolean = (c: Char) => c > 127
    string.filterNot(controlCode).filterNot(extendedCode)
  }

  /**
   * Converts given byte array of 16 bytes to UUID.
   * Result is this format: 22BD2433-09CA-4F27-B62A-BE3CB68DE75E
   * Given these bytes:     3324bd22 ca09 274f b62a be3cb68de75e
   * That means the format is: LE-Integer, LE-Short, LE-Short, BE-Long
   *
   * @param bytes
   * @return UUID for given byte array
   */
  def convertBytesToUUID(bytes: Array[Byte]): UUID = {
    assert(bytes.length == 16)
    var high = ByteArrayUtil.bytesToInt(bytes.slice(0,4)).toLong << 32L
    high = high + (ByteArrayUtil.bytesToShort(bytes.slice(4,6)).toLong << 16L)
    high = high + ByteArrayUtil.bytesToShort(bytes.slice(6,8)).toLong
    val low = new BigInteger(bytes.slice(8,16)).longValue
    new UUID(high, low);
  }

}