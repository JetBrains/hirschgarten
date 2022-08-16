package org.jetbrains.plugins.bsp.extension.points

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.protocol.connection.BspConnectionDetailsGenerator
import java.io.OutputStream

public interface BspConnectionDetailsGeneratorExtension : BspConnectionDetailsGenerator {

  public companion object {
    private val ep =
      ExtensionPointName.create<BspConnectionDetailsGeneratorExtension>("com.intellij.bspConnectionDetailsGeneratorExtension")

    public fun extensions(): List<BspConnectionDetailsGeneratorExtension> =
      ep.extensionList
  }
}

public class TemporarySbtBspConnectionDetailsGenerator : BspConnectionDetailsGeneratorExtension {
  override fun name(): String = "sbt"

  override fun canGenerateBspConnectionDetailsFile(projectPath: VirtualFile): Boolean =
    projectPath.children.any { it.name == "build.sbt" }

  override fun generateBspConnectionDetailsFile(projectPath: VirtualFile, outputStream: OutputStream): VirtualFile {
    executeAndWait("cs launch sbt -- bspConfig", projectPath, outputStream)
    return getChild(projectPath, listOf(".bsp", "sbt.json"))!!
  }
}
