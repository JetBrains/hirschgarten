package org.jetbrains.plugins.bsp.flow.open.wizard

import org.jetbrains.plugins.bsp.protocol.connection.BspConnectionDetailsGenerator
import org.jetbrains.plugins.bsp.protocol.connection.LocatedBspConnectionDetails

public sealed interface ConnectionFileOrNewConnection {
  public val connectionName: String
}

public data class ConnectionFile(val locatedBspConnectionDetails: LocatedBspConnectionDetails) :
  ConnectionFileOrNewConnection {
  override val connectionName: String = locatedBspConnectionDetails.bspConnectionDetails.name
  override fun toString(): String {
    val parentDirName = locatedBspConnectionDetails.connectionFileLocation.parent.name
    val fileName = locatedBspConnectionDetails.connectionFileLocation.name
    return "$parentDirName/$fileName"
  }
}

public data class NewConnection(val generator: BspConnectionDetailsGenerator) :
  ConnectionFileOrNewConnection {
  override val connectionName: String = generator.name()
  override fun toString(): String = "New connection"
}
