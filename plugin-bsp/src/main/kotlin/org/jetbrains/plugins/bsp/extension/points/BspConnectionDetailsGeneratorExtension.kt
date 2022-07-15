package org.jetbrains.plugins.bsp.extension.points

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.protocol.connection.BspConnectionDetailsGenerator

public interface BspConnectionDetailsGeneratorExtension : BspConnectionDetailsGenerator {

  public companion object {
    private val ep =
      ExtensionPointName.create<BspConnectionDetailsGeneratorExtension>("com.intellij.bspConnectionDetailsGeneratorExtension")

    public fun extensions(): List<BspConnectionDetailsGeneratorExtension> =
      ep.extensionList
  }
}

// TODO: NOT TESTED & SHOULD BE MOVED TO THE BAZEL PLUGIN
public class TemporaryBazelBspConnectionDetailsGenerator : BspConnectionDetailsGeneratorExtension {

  override fun name(): String = "bazel"

  override fun canGenerateBspConnectionDetailsFile(projectPath: VirtualFile): Boolean =
    projectPath.children.any { it.name == "WORKSPACE" }

  override fun generateBspConnectionDetailsFile(projectPath: VirtualFile): VirtualFile {
    Runtime.getRuntime().exec(
      "cs launch org.jetbrains.bsp:bazel-bsp:2.1.0 -M org.jetbrains.bsp.bazel.install.Install",
      emptyArray(),
      projectPath.toNioPath().toFile()
    ).waitFor()

    return projectPath.findChild(".bsp")?.findChild("bazelbsp.json")!!
  }
}
