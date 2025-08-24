package org.jetbrains.bazel.symbols

import com.intellij.model.Pointer
import org.jetbrains.bazel.label.Label

/**
 * A pointer to a BazelTargetSymbol that can be safely stored and dereferenced
 * across read actions, as required by the IntelliJ Symbol API.
 */
class BazelTargetSymbolPointer(
  private val labelString: String,
  private val buildFilePath: String,
  private val targetType: BazelTargetType,
  private val aliases: Set<String>
) : Pointer<BazelTargetSymbol> {

  override fun dereference(): BazelTargetSymbol? {
    return try {
      val label = Label.parse(labelString)
      BazelTargetSymbol(
        label = label,
        buildFilePath = buildFilePath,
        targetType = targetType,
        aliases = aliases
      )
    } catch (e: Exception) {
      // Return null if the label cannot be parsed (e.g., target no longer exists)
      null
    }
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    other as BazelTargetSymbolPointer
    return labelString == other.labelString && buildFilePath == other.buildFilePath
  }

  override fun hashCode(): Int {
    var result = labelString.hashCode()
    result = 31 * result + buildFilePath.hashCode()
    return result
  }

  override fun toString(): String = "BazelTargetSymbolPointer($labelString@$buildFilePath)"
}