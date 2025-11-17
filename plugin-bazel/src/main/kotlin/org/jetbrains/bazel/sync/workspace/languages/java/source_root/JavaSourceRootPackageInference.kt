package org.jetbrains.bazel.sync.workspace.languages.java.source_root

import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.sync.workspace.languages.JvmPackageResolver
import org.jetbrains.bsp.protocol.SourceItem
import java.nio.file.Path
import kotlin.io.path.exists
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
class JavaSourceRootPackageInference(val packageResolver: JvmPackageResolver) {

  fun inferPackages(sources: List<SourceItem>) {
    if (sources.isEmpty()) {
      return
    }

    val supportedSources = sources.filter { Constants.JVM_LANGUAGES_EXTENSIONS.contains(it.path.extension) }
    if (supportedSources.isEmpty()) {
      return
    }

    if (supportedSources.size == 1) {
      val firstSource = supportedSources.first()
      firstSource.jvmPackagePrefix = packageResolver.calculateJvmPackagePrefix(firstSource.path)
      return
    }

    val tree = SourceItemPropagationPrefixTree()
    for (item in supportedSources) {
      val segments = item.path.asSequence()
        .map { it.toString() }
      tree.push(segments, item)
    }

    tree.propagateJavaPackages { packageResolver.calculateJvmPackagePrefix(it) }
  }
}

class SourceItemPropagationPrefixTree {
  private data class Node(
    val segment: String,
    var item: SourceItem?,
    val children: MutableList<Node> = mutableListOf(),
  )

  private var root = Node("", null)

  fun push(path: Sequence<String>, data: SourceItem) {
    var current = root
    for (segment in path) {
      var node: Node? = null
      for (child in current.children) {
        if (child.segment == segment) {
          node = child
          break
        }
      }
      if (node == null) {
        node = Node(segment, null)
        current.children.add(node)
      }
      current = node
    }
    current.item = data
  }

  fun propagateJavaPackages(slowPackageResolver: (item: Path) -> String?) {
    val queue = ArrayDeque<Node>()
    queue.add(root)
    while (true) {
      val node = queue.removeFirstOrNull()
      if (node == null) {
        break
      }
      // we want to start propagation from roots
      // so if we find subtree whose child has existing file
      // we start propagation from it
      if (node.children.any { it.item != null }
        && node.children.any { it.item?.jvmPackagePrefix == null }) {
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
    val rootSourceFile = subtreeRoot.children.firstOrNull { it.item != null }?.item ?: return
    val resolvedPackage = slowPackageResolver(rootSourceFile.path) ?: return
    // propagate all files in subtree in relation to root source file
    propagateFromRootSourceFileInSubtree(subtreeRoot, rootSourceFile, resolvedPackage)
  }

  private fun propagateFromRootSourceFileInSubtree(subtreeRoot: Node, rootSourceFile: SourceItem, resolvedPackage: String) {
    val rootPath = rootSourceFile.path.parent

    val queue = ArrayDeque<Node>()
    queue.add(subtreeRoot)

    while (true) {
      val node = queue.removeFirstOrNull()
      if (node == null) {
        break
      }
      val item = node.item
      if (item != null) {
        val currentParentPath = item.path.parent
        item.jvmPackagePrefix = calculatePackageFromPaths(rootPath, currentParentPath, resolvedPackage)
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
