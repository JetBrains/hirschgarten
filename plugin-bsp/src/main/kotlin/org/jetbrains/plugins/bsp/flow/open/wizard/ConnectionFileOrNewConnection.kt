package org.jetbrains.plugins.bsp.flow.open.wizard

import ch.epfl.scala.bsp4j.BspConnectionDetails
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.plugins.bsp.extension.points.BspConnectionDetailsGeneratorExtension
import org.jetbrains.plugins.bsp.protocol.connection.BspConnectionDetailsGenerator

public sealed interface ConnectionFileOrNewConnection {
  public val id: String
  public val displayName: String
}

public data class ConnectionFile(
  val bspConnectionDetails: BspConnectionDetails,
  val connectionFile: VirtualFile
) :
  ConnectionFileOrNewConnection, Comparable<ConnectionFile> {
  override val id: String = bspConnectionDetails.name
  override val displayName: String = BspConnectionDetailsGeneratorExtension
    .extensions()
    .find { it.id() == id }
    ?.displayName() ?: id

  override fun compareTo(other: ConnectionFile): Int {
    val thisVersionList = this.bspConnectionDetails.version.split(".")
    val otherVersionList = other.bspConnectionDetails.version.split(".")
    thisVersionList.zip(otherVersionList).forEach {
      val thisPart = it.first.toIntOrNull() ?: 0
      val otherPart = it.second.toIntOrNull() ?: 0
      when {
        thisPart > otherPart -> return 1
        thisPart < otherPart -> return -1
      }
    }

    return when {
      thisVersionList.size > otherVersionList.size -> 1
      thisVersionList.size < otherVersionList.size -> -1
      else -> 0
    }
  }

  override fun toString(): String {
    val parentDirName = connectionFile.parent.name
    val fileName = connectionFile.name
    return "$parentDirName/$fileName"
  }
}

public data class NewConnection(val generator: BspConnectionDetailsGenerator) :
  ConnectionFileOrNewConnection {
  override val id: String = generator.id()
  override val displayName: String = generator.displayName()
  override fun toString(): String = "New connection"
}
