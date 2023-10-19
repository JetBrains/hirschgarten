@file:Suppress("all")
//package org.jetbrains.plugins.bsp.extension.points
//
//import com.intellij.openapi.project.Project
//import com.intellij.openapi.vfs.VirtualFile
//import java.io.OutputStream
//
//public class TemporarySbtBspConnectionDetailsGenerator : BspConnectionDetailsGeneratorExtension {
//  override fun id(): String = "sbt"
//
//  override fun displayName(): String = "Sbt"
//
//  override fun canGenerateBspConnectionDetailsFile(projectPath: VirtualFile): Boolean =
//    projectPath.children.any { it.name == "build.sbt" }
//
//  override fun generateBspConnectionDetailsFile(
//    projectPath: VirtualFile,
//    outputStream: OutputStream,
//    project: Project,
//  ): VirtualFile {
//    executeAndWait(
//      command = listOf(
//        ExternalCommandUtils.calculateJavaExecPath(),
//        "-jar",
//        ExternalCommandUtils
//          .calculateNeededJars(
//            org = "org.scala-sbt",
//            name = "sbt-launch",
//            version = "1.9.1",
//          )
//          .joinToString(""),
//        "bspConfig",
//      ),
//      projectPath = projectPath,
//      outputStream = outputStream,
//      project = project,
//    )
//    return getChild(projectPath, listOf(".bsp", "sbt.json"))!!
//  }
//}
