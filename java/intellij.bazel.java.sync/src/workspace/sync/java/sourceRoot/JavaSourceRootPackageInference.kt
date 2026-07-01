package org.jetbrains.bazel.sync.workspace.languages.java.sourceRoot

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.sync.workspace.languages.JvmPackageResolver
import java.nio.file.Path
import kotlin.io.path.extension

// this package based inference mechanism is trying
// to optimize amount of calls package resolver
//
// for large monorepos randomly accessing over 100_000 files can be inefficient
// and profiler snapshot prove that(on some systems opening file handles can take over 100 seconds in total)
//
// this implementation tries to optimize access to files by grouping them by their package
// and then resolving package for each group by resolving relative path
//
// it is not perfect but still better than randomly accessing thousands of files
//
@ApiStatus.Internal
class JavaSourceRootPackageInference(val packageResolver: JvmPackageResolver) {

  fun inferPackages(paths: List<Path>): Map<Path, String> {
    if (paths.isEmpty()) {
      return emptyMap()
    }

    val supportedSources = paths.filter { Constants.JVM_LANGUAGES_EXTENSIONS.contains(it.extension) }
    if (supportedSources.isEmpty()) {
      return emptyMap()
    }

    if (supportedSources.size == 1) {
      val path = supportedSources.first()
      val prefix = packageResolver.calculateJvmPackagePrefix(path)
                   ?: return emptyMap()
      return mapOf(path to prefix)
    }

    val tree = SourceItemPropagationPrefixTree()
    for (path in supportedSources) {
      tree.push(path)
    }

    tree.propagateJavaPackages { packageResolver.calculateJvmPackagePrefix(it) }
    return tree.jvmPackagePrefixes
  }
}

internal class SourceItemPropagationPrefixTree {
  val jvmPackagePrefixes = HashMap<Path, String>()

  private class Node(
    val segment: String,
    val children: MutableList<Node> = mutableListOf(),
  ) {
    var pathForPrefixCalculation: Path? = null
  }

  private val root = Node("")

  fun push(path: Path) {
    val segments = path.map { it.toString() }
    if (segments.isEmpty())
      return

    var current = root
    for (segment in segments) {
      var node: Node? = null
      for (child in current.children) {
        if (child.segment == segment) {
          node = child
          break
        }
      }
      if (node == null) {
        node = Node(segment)
        current.children.add(node)
      }
      current = node
    }
    current.pathForPrefixCalculation = path
  }

  fun propagateJavaPackages(slowPackageResolver: (item: Path) -> String?) {
    val queue = ArrayDeque<Node>()
    queue.add(root)
    while (queue.isNotEmpty()) {
      val node = queue.removeFirst()

      // we want to start propagation from roots
      // so if we find subtree whose child has existing file
      // we start propagation from it
      if (node.children.any { it.pathForPrefixCalculation != null && !jvmPackagePrefixes.containsKey(it.pathForPrefixCalculation) }) {
        // propagate in every subtree to handle
        // having multiple root packages in single source root
        //    src/com/jetbrains/...
        //    src/org/jetbrains/...
        this.propagateInSubtree(node, slowPackageResolver)
      }
      for (child in node.children) {
        queue.add(child)
      }
    }
  }

  private fun propagateInSubtree(subtreeRoot: Node, slowPackageResolver: (item: Path) -> String?) {
    // find subtree package reference
    // it can be any file in specific directory
    val rootSourceFile = subtreeRoot.children.firstNotNullOfOrNull { it.pathForPrefixCalculation } ?: return
    val resolvedPackage = slowPackageResolver(rootSourceFile) ?: return
    // propagate all files in subtree in relation to root source file
    propagateFromRootSourceFileInSubtree(subtreeRoot, rootSourceFile, resolvedPackage)
  }

  private fun propagateFromRootSourceFileInSubtree(subtreeRoot: Node, rootSourceFile: Path, resolvedPackage: String) {
    val rootPath = rootSourceFile.parent

    val queue = ArrayDeque<Node>()
    queue.add(subtreeRoot)

    while (queue.isNotEmpty()) {
      val node = queue.removeFirst()
      val path = node.pathForPrefixCalculation
      if (path != null) {
        jvmPackagePrefixes[path] = calculatePackageFromPaths(rootPath, path.parent, resolvedPackage)
      }

      queue.addAll(node.children)
    }
  }

  private fun calculatePackageFromPaths(rootPath: Path?, currentPath: Path?, rootPackage: String): String {
    if (rootPath == currentPath) {
      return rootPackage
    }

    if (rootPath == null || currentPath == null) {
      return rootPackage
    }

    val repSegments = rootPath.map { it.toString() }
    val currSegments = currentPath.map { it.toString() }

    var commonLength = 0
    val minLength = minOf(repSegments.size, currSegments.size)

    for (i in 0 until minLength) {
      if (repSegments[i] == currSegments[i]) {
        commonLength++
      } else {
        break
      }
    }

    val rootPackageParts = if (rootPackage.isEmpty()) {
      emptyList()
    } else {
      rootPackage.split(".")
    }

    val basePackageParts = if (repSegments.size > commonLength) {
      val segmentsToRemove = repSegments.size - commonLength
      rootPackageParts.dropLast(segmentsToRemove)
    } else {
      rootPackageParts
    }

    // remove a common path
    val additionalSegments = currSegments.drop(commonLength)
    val finalPackageParts = basePackageParts + additionalSegments

    return finalPackageParts.joinToString(".")
  }

}
