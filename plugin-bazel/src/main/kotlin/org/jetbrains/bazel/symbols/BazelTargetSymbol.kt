package org.jetbrains.bazel.symbols

import com.intellij.model.Symbol
import com.intellij.model.presentation.PresentableSymbol
import com.intellij.model.presentation.SymbolPresentation
import com.intellij.navigation.ItemPresentation
import com.intellij.navigation.NavigationTarget
import org.jetbrains.bazel.assets.BazelPluginIcons
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.ResolvedLabel

/**
 * Represents a Bazel target as a symbol in the IntelliJ Symbol API.
 * This enables features like Find Usages, Safe Delete, and Rename for Bazel targets.
 */
data class BazelTargetSymbol(
  val label: Label,
  val buildFilePath: String,
  val targetType: BazelTargetType = BazelTargetType.UNKNOWN,
  val aliases: Set<String> = emptySet()
) : Symbol, PresentableSymbol {

  val targetName: String = label.targetName
  val packagePath: String = label.packagePath.toString()
  val isMainWorkspace: Boolean = label.isMainWorkspace

  override fun createPointer(): BazelTargetSymbolPointer {
    return BazelTargetSymbolPointer(label.toString(), buildFilePath, targetType, aliases)
  }

  override fun getSymbolPresentation(): SymbolPresentation {
    return BazelTargetSymbolPresentation(this)
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    other as BazelTargetSymbol
    return label == other.label && buildFilePath == other.buildFilePath
  }

  override fun hashCode(): Int {
    var result = label.hashCode()
    result = 31 * result + buildFilePath.hashCode()
    return result
  }

  override fun toString(): String = "${label}@${buildFilePath}"

  /**
   * Creates a new symbol with additional aliases
   */
  fun withAliases(newAliases: Set<String>): BazelTargetSymbol {
    return copy(aliases = aliases + newAliases)
  }

  /**
   * Checks if this symbol matches the given target name (including aliases)
   */
  fun matchesTargetName(name: String): Boolean {
    return targetName == name || aliases.contains(name)
  }
}

/**
 * Different types of Bazel targets for better categorization and icons
 */
enum class BazelTargetType {
  UNKNOWN,
  JAVA_BINARY,
  JAVA_LIBRARY,
  JAVA_TEST,
  CC_BINARY,
  CC_LIBRARY,
  CC_TEST,
  PY_BINARY,
  PY_LIBRARY,
  PY_TEST,
  GO_BINARY,
  GO_LIBRARY,
  GO_TEST,
  KOTLIN_BINARY,
  KOTLIN_LIBRARY,
  KOTLIN_TEST,
  GENRULE,
  FILEGROUP,
  ALIAS,
  PROTO_LIBRARY,
  ANDROID_BINARY,
  ANDROID_LIBRARY,
  ANDROID_TEST,
  CUSTOM_RULE;

  val isTestTarget: Boolean
    get() = name.endsWith("_TEST")

  val isBinaryTarget: Boolean
    get() = name.endsWith("_BINARY")

  val isLibraryTarget: Boolean
    get() = name.endsWith("_LIBRARY")

  companion object {
    fun fromRuleName(ruleName: String): BazelTargetType {
      return when (ruleName) {
        "java_binary" -> JAVA_BINARY
        "java_library" -> JAVA_LIBRARY
        "java_test" -> JAVA_TEST
        "cc_binary" -> CC_BINARY
        "cc_library" -> CC_LIBRARY
        "cc_test" -> CC_TEST
        "py_binary" -> PY_BINARY
        "py_library" -> PY_LIBRARY
        "py_test" -> PY_TEST
        "go_binary" -> GO_BINARY
        "go_library" -> GO_LIBRARY
        "go_test" -> GO_TEST
        "kt_jvm_binary" -> KOTLIN_BINARY
        "kt_jvm_library" -> KOTLIN_LIBRARY
        "kt_jvm_test" -> KOTLIN_TEST
        "genrule" -> GENRULE
        "filegroup" -> FILEGROUP
        "alias" -> ALIAS
        "proto_library" -> PROTO_LIBRARY
        "android_binary" -> ANDROID_BINARY
        "android_library" -> ANDROID_LIBRARY
        "android_local_test", "android_instrumentation_test" -> ANDROID_TEST
        else -> CUSTOM_RULE
      }
    }
  }
}

/**
 * Presentation for Bazel target symbols in UI components
 */
class BazelTargetSymbolPresentation(
  private val symbol: BazelTargetSymbol
) : SymbolPresentation {

  override fun getShortDescription(): String = symbol.targetName

  override fun getLongDescription(): String {
    val resolvedLabel = symbol.label as? ResolvedLabel
    val repoName = resolvedLabel?.repoName?.takeIf { it.isNotEmpty() } ?: ""
    val repoPrefix = if (repoName.isNotEmpty()) "@$repoName" else ""
    return "${repoPrefix}//${symbol.packagePath}:${symbol.targetName}"
  }

  override fun getIcon(unused: Boolean) = when (symbol.targetType) {
    BazelTargetType.JAVA_BINARY, BazelTargetType.KOTLIN_BINARY -> BazelPluginIcons.bazel
    BazelTargetType.JAVA_LIBRARY, BazelTargetType.KOTLIN_LIBRARY -> BazelPluginIcons.bazel
    BazelTargetType.JAVA_TEST, BazelTargetType.KOTLIN_TEST -> BazelPluginIcons.bazelError
    else -> BazelPluginIcons.bazel
  }
}