package org.jetbrains.plugins.bsp.flow.open.wizard

import org.jetbrains.plugins.bsp.extension.points.BspConnectionDetailsGeneratorExtension
import org.jetbrains.plugins.bsp.protocol.connection.BspConnectionDetailsGenerator
import org.jetbrains.plugins.bsp.protocol.connection.LocatedBspConnectionDetails

public sealed interface ConnectionFileOrNewConnection {
  public val id: String
  public val displayName: String
}

public data class ConnectionFile(val locatedBspConnectionDetails: LocatedBspConnectionDetails) :
  ConnectionFileOrNewConnection, Comparable<ConnectionFile> {
  override val id: String = locatedBspConnectionDetails.bspConnectionDetails.name
  override val displayName: String = BspConnectionDetailsGeneratorExtension
    .extensions()
    .find { it.id() == id }
    ?.displayName() ?: id

  override fun compareTo(other: ConnectionFile): Int {
    val thisVersionList = this.locatedBspConnectionDetails.bspConnectionDetails.version.split(".")
    val otherVersionList = other.locatedBspConnectionDetails.bspConnectionDetails.version.split(".")
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
    val parentDirName = locatedBspConnectionDetails.connectionFileLocation.parent.name
    val fileName = locatedBspConnectionDetails.connectionFileLocation.name
    return "$parentDirName/$fileName"
  }
}

public data class NewConnection(val generator: BspConnectionDetailsGenerator) :
  ConnectionFileOrNewConnection {
  override val id: String = generator.id()
  override val displayName: String = generator.displayName()
  override fun toString(): String = "New connection"
}
