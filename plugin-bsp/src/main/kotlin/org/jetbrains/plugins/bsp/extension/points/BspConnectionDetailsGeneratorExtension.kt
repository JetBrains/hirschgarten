package org.jetbrains.plugins.bsp.extension.points

import com.intellij.openapi.extensions.ExtensionPointName
import org.jetbrains.plugins.bsp.protocol.connection.BspConnectionDetailsGenerator

public interface BspConnectionDetailsGeneratorExtension : BspConnectionDetailsGenerator {
  public companion object {
    private val ep =
      ExtensionPointName.create<BspConnectionDetailsGeneratorExtension>(
        "org.jetbrains.bsp.bspConnectionDetailsGeneratorExtension")

    public fun extensions(): List<BspConnectionDetailsGeneratorExtension> =
      ep.extensionList
  }
}
