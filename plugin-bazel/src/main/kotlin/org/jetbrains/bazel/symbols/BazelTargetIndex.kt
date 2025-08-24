package org.jetbrains.bazel.symbols

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.*
import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.DataInputOutputUtil
import com.intellij.util.io.KeyDescriptor
import org.jetbrains.bazel.languages.starlark.StarlarkFileType
import java.io.DataInput
import java.io.DataOutput

/**
 * File-based index for efficiently finding Bazel targets by name.
 */
class BazelTargetIndex : FileBasedIndexExtension<String, BazelTargetInfo>() {

  companion object {
    val INDEX_ID: ID<String, BazelTargetInfo> = ID.create("BazelTargetIndex")

    /**
     * Get all target infos for a given target name across the project
     */
    fun getTargetsByName(targetName: String, project: Project): List<BazelTargetInfo> {
      return FileBasedIndex.getInstance()
        .getValues(INDEX_ID, targetName, GlobalSearchScope.projectScope(project))
    }

    /**
     * Get all target names in the project
     */
    fun getAllTargetNames(project: Project): Set<String> {
      val allKeys = mutableSetOf<String>()
      FileBasedIndex.getInstance().processAllKeys(INDEX_ID, { key ->
        allKeys.add(key)
        true
      }, GlobalSearchScope.projectScope(project), null)
      return allKeys
    }

    /**
     * Get targets in a specific package
     */
    fun getTargetsInPackage(packagePath: String, project: Project): List<BazelTargetInfo> {
      val targets = mutableListOf<BazelTargetInfo>()
      
      FileBasedIndex.getInstance().processAllKeys(INDEX_ID, { targetName ->
        val targetInfos = getTargetsByName(targetName, project)
        targets.addAll(targetInfos.filter { it.packagePath == packagePath })
        true
      }, GlobalSearchScope.projectScope(project), null)
      
      return targets
    }
  }

  override fun getName(): ID<String, BazelTargetInfo> = INDEX_ID

  override fun getIndexer(): DataIndexer<String, BazelTargetInfo, FileContent> {
    return DataIndexer { inputData ->
      val result = mutableMapOf<String, BazelTargetInfo>()
      
      try {
        val targets = BazelTargetParser.parseTargetsFromContent(inputData.contentAsText.toString(), inputData.file)
        
        for (target in targets) {
          result[target.targetName] = target
          // Also index by aliases
          for (alias in target.aliases) {
            result[alias] = target.copy(targetName = alias, isAlias = true, originalTargetName = target.targetName)
          }
        }
        
      } catch (e: Exception) {
        // Log error but don't fail indexing
        // TODO: Add proper logging
      }
      
      result
    }
  }

  override fun getKeyDescriptor(): KeyDescriptor<String> = EnumeratorStringDescriptor.INSTANCE

  override fun getValueExternalizer(): DataExternalizer<BazelTargetInfo> = BazelTargetInfoExternalizer()

  override fun getInputFilter(): FileBasedIndex.InputFilter {
    return FileBasedIndex.InputFilter { file ->
      file.fileType == StarlarkFileType && 
      (file.name == "BUILD" || file.name == "BUILD.bazel")
    }
  }

  override fun dependsOnFileContent(): Boolean = true

  override fun getVersion(): Int = 2
}

/**
 * Information about a Bazel target stored in the index
 */
data class BazelTargetInfo(
  val targetName: String,
  val packagePath: String,
  val buildFilePath: String,
  val targetType: BazelTargetType,
  val ruleName: String,
  val aliases: Set<String> = emptySet(),
  val dependencies: List<String> = emptyList(),
  val isAlias: Boolean = false,
  val originalTargetName: String? = null
) {
  
  /**
   * Convert to a BazelTargetSymbol
   */
  fun toSymbol(): BazelTargetSymbol {
    val packageSegments = if (packagePath.isEmpty()) emptyList() else packagePath.split("/")
    val label = org.jetbrains.bazel.label.ResolvedLabel(
      repo = org.jetbrains.bazel.label.Main,
      packagePath = org.jetbrains.bazel.label.Package(packageSegments),
      target = org.jetbrains.bazel.label.SingleTarget(targetName)
    )
    
    return BazelTargetSymbol(
      label = label,
      buildFilePath = buildFilePath,
      targetType = targetType,
      aliases = aliases
    )
  }
}

/**
 * Serializer for BazelTargetInfo
 */
class BazelTargetInfoExternalizer : DataExternalizer<BazelTargetInfo> {
  
  override fun save(out: DataOutput, value: BazelTargetInfo) {
    out.writeUTF(value.targetName)
    out.writeUTF(value.packagePath)
    out.writeUTF(value.buildFilePath)
    out.writeUTF(value.targetType.name)
    out.writeUTF(value.ruleName)
    
    // Write aliases
    DataInputOutputUtil.writeSeq(out, value.aliases) { output, alias ->
      output.writeUTF(alias)
    }
    
    // Write dependencies
    DataInputOutputUtil.writeSeq(out, value.dependencies) { output, dep ->
      output.writeUTF(dep)
    }
    
    out.writeBoolean(value.isAlias)
    out.writeUTF(value.originalTargetName ?: "")
  }
  
  override fun read(`in`: DataInput): BazelTargetInfo {
    val targetName = `in`.readUTF()
    val packagePath = `in`.readUTF()
    val buildFilePath = `in`.readUTF()
    val targetType = BazelTargetType.valueOf(`in`.readUTF())
    val ruleName = `in`.readUTF()
    
    // Read aliases
    val aliases = DataInputOutputUtil.readSeq(`in`) { input ->
      input.readUTF()
    }.toSet()
    
    // Read dependencies
    val dependencies = DataInputOutputUtil.readSeq(`in`) { input ->
      input.readUTF()
    }
    
    val isAlias = `in`.readBoolean()
    val originalTargetName = `in`.readUTF().takeIf { it.isNotEmpty() }
    
    return BazelTargetInfo(
      targetName = targetName,
      packagePath = packagePath,
      buildFilePath = buildFilePath,
      targetType = targetType,
      ruleName = ruleName,
      aliases = aliases,
      dependencies = dependencies,
      isAlias = isAlias,
      originalTargetName = originalTargetName
    )
  }
}