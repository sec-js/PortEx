/**
 * *****************************************************************************
 * Copyright 2016 Katja Hahn
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

package io.github.struppigel.parser.sections.reloc

import io.github.struppigel.parser.ScalaIOUtil.hex
import io.github.struppigel.parser.PhysicalLocation
import org.apache.logging.log4j.LogManager

class BaseRelocBlock(
  val fileOffset: Long,
  val pageRVA: Long,
  val blockSize: Long,
  val entries: List[BlockEntry]) {

  def getLocations(): List[PhysicalLocation] = List(new PhysicalLocation(fileOffset, blockSize))

  override def toString(): String =
    s"""page rva: ${hex(pageRVA)}
       |block size: ${hex(blockSize)}
       |file offset: ${hex(fileOffset)}
       |
       |${entries.mkString("\n")}
       |""".stripMargin

}

class BlockEntry(val relocType: RelocType, val offset: Long) {

  override def toString(): String =
    s"type: ${relocType.getDescription}, offset: 0x${java.lang.Long.toHexString(offset)}"
}

object BlockEntry {

  private val logger = LogManager.getLogger(BlockEntry.getClass().getName())

  def apply(value: Int): BlockEntry = {
    val typeMask = 0xf000
    val offsetMask = 0x0fff
    val typeValue = (typeMask & value) >>> 12
    val offset = offsetMask & value
    val relocType = getTypeFor(typeValue)
    new BlockEntry(relocType, offset)
  }

  private def getTypeFor(value: Int): RelocType = {
    try {
      val relocType = RelocType.getForValue(value)
      relocType
    } catch {
      case e: IllegalArgumentException =>
        logger.warn("unknown reloc type for value: " + value)
        RelocType.UNKNOWN
    }
  }
}