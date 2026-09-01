package org.jetbrains.bazel.java.analysis

import com.intellij.codeInspection.bytecodeAnalysis.BytecodeAnalysisSuppressor
import com.intellij.openapi.vfs.JarFileSystem
import com.intellij.openapi.vfs.VirtualFile

private val INTERFACE_JAR_SUFFIXES = arrayOf("-ijar.jar", "-hjar.jar", ".abi.jar")

/**
 * Interface jars have most of the bytecode removed (e.g., method bodies), so the bytecode analysis will be incorrect, leading to false positives.
 *
 * For the `abi.jar` (rules_kotlin equivalent of interface jars) the situation is not that straightforward:
 * 1. The `abi.jar` might be available but depending on `experimental_use_abi_jars = True` the classes are stripped or not.
 * 2. If Java files were part of the Kotlin target, then the interface jar is a part of `abi.jar` unconditionally.
 *    It might lead to a situation where `abi.jar` contains both stripped (Java ones) and non-stripped classes (Kotlin ones).
 *
 * For simplicity, we suppress the analysis for all `abi.jar` files even if they might not be stripped.
 */
internal class BazelInterfaceJarAnalysisSuppressor : BytecodeAnalysisSuppressor {

  // needs to be bumped if algorithm changes
  override fun getVersion(): Int = 1

  override fun shouldSuppress(file: VirtualFile): Boolean {
    val jar = JarFileSystem.getInstance().getVirtualFileForJar(file) ?: return false
    val name = jar.name
    return INTERFACE_JAR_SUFFIXES.any { suffix -> name.endsWith(suffix) }
           && generateSequence(jar.parent) { it.parent }.any { it.name == "bazel-out" }
  }
}
