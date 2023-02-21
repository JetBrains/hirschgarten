package org.jetbrains.plugins.bsp.flow.open.wizard

import org.jetbrains.plugins.bsp.extension.points.BspConnectionDetailsGeneratorExtension
import org.jetbrains.plugins.bsp.protocol.connection.BspConnectionDetailsGenerator
import org.jetbrains.plugins.bsp.protocol.connection.LocatedBspConnectionDetails

public sealed interface ConnectionFileOrNewConnection {
  public val id: String
  public val displayName: String
}

public data class ConnectionFile(val locatedBspConnectionDetails: LocatedBspConnectionDetails) :
  ConnectionFileOrNewConnection {
  override val id: String = locatedBspConnectionDetails.bspConnectionDetails.name
  override val displayName: String = BspConnectionDetailsGeneratorExtension
    .extensions()
    .find { it.id() == id }?.displayName() ?: id
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
