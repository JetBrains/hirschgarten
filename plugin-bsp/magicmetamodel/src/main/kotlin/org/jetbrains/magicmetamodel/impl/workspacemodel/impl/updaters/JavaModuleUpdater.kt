package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import com.intellij.openapi.module.ModuleTypeId
import com.intellij.workspaceModel.storage.MutableEntityStorage
import com.intellij.workspaceModel.storage.bridgeEntities.ModuleDependencyItem
import com.intellij.workspaceModel.storage.bridgeEntities.ModuleEntity
import com.intellij.workspaceModel.storage.bridgeEntities.ModuleId
import com.intellij.workspaceModel.storage.bridgeEntities.addJavaModuleSettingsEntity
import com.intellij.workspaceModel.storage.impl.url.toVirtualFileUrl
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.security.MessageDigest
import kotlin.io.path.isDirectory

internal data class JvmJdkInfo(val javaVersion: String, val javaHome: String)

internal data class JavaModule(
  val module: Module,
  val baseDirContentRoot: ContentRoot,
  val sourceRoots: List<JavaSourceRoot>,
  val resourceRoots: List<JavaResourceRoot>,
  val libraries: List<Library>,
  val compilerOutput: Path?,
  val jvmJdkInfo: JvmJdkInfo?,
) : WorkspaceModelEntity()

internal class JavaModuleWithSourcesUpdater(
  private val workspaceModelEntityUpdaterConfig: WorkspaceModelEntityUpdaterConfig,
) : WorkspaceModelEntityWithoutParentModuleUpdater<JavaModule, ModuleEntity> {

  override fun addEntity(entityToAdd: JavaModule): ModuleEntity {
    val moduleEntityUpdater = ModuleEntityUpdater(workspaceModelEntityUpdaterConfig, calculateJavaModuleDependencies(entityToAdd))

    val moduleEntity = moduleEntityUpdater.addEntity(entityToAdd.module)

    addJavaModuleSettingsEntity(workspaceModelEntityUpdaterConfig.workspaceEntityStorageBuilder, entityToAdd, moduleEntity)

    val libraryEntityUpdater = LibraryEntityUpdater(workspaceModelEntityUpdaterConfig)
    libraryEntityUpdater.addEntries(entityToAdd.libraries, moduleEntity)

    val javaSourceEntityUpdater = JavaSourceEntityUpdater(workspaceModelEntityUpdaterConfig)
    javaSourceEntityUpdater.addEntries(entityToAdd.sourceRoots, moduleEntity)

    val javaResourceEntityUpdater = JavaResourceEntityUpdater(workspaceModelEntityUpdaterConfig)
    javaResourceEntityUpdater.addEntries(entityToAdd.resourceRoots, moduleEntity)

    return moduleEntity
  }

  private fun calculateJavaModuleDependencies(entityToAdd: JavaModule): List<ModuleDependencyItem> {
    if (DummyJavaSourceModuleUtils.isDummyJavaSourceModule(entityToAdd)) return defaultDependencies

    // Add java sdk as dependency if jvmJdkInfo is not null
    val finalDependencies: MutableList<ModuleDependencyItem> = defaultDependencies.toMutableList()
    if (entityToAdd.jvmJdkInfo != null) {
      finalDependencies += ModuleDependencyItem.SdkDependency(entityToAdd.jvmJdkInfo.javaVersion, "JavaSDK")
    }

    // Add DummyJavaSourceModules as dependency corresponding to the entity
    val moduleNames = DummyJavaSourceModuleUtils.calculateDummyJavaSourceModuleNames(entityToAdd)
    finalDependencies += moduleNames.map { toModuleDependencyItemModuleDependency(ModuleDependency(it)) }
    return finalDependencies
  }

  private fun addJavaModuleSettingsEntity(builder: MutableEntityStorage, entityToAdd: JavaModule, moduleEntity: ModuleEntity) {
    if (entityToAdd.compilerOutput != null) {
      builder.addJavaModuleSettingsEntity(
        inheritedCompilerOutput = false,
        excludeOutput = true,
        compilerOutput = entityToAdd.compilerOutput.toVirtualFileUrl(workspaceModelEntityUpdaterConfig.virtualFileUrlManager),
        compilerOutputForTests = null,
        languageLevelId = null,
        module = moduleEntity,
        source = DoNotSaveInDotIdeaDirEntitySource,
      )
    }
  }

  private companion object {
    val defaultDependencies = listOf(
      ModuleDependencyItem.ModuleSourceDependency,
    )
  }
}

// This is a workaround for handling unresolved references issue with java classes within the same package
private object DummyJavaSourceModuleUtils {
  const val DUMMY_JAVA_SOURCE_MODULE_SIGNATURE = "java-source"
  const val DUMMY_JAVA_SOURCE_MODULE_ROOT_TYPE = "java-source"
  val TYPICAL_STRUCTURES = listOf(
    "" / "src" / "main" / "java",
    "" / "src" / "main" / "kotlin",
    "" / "src" / "test" / "java",
    "" / "src" / "test" / "kotlin",
    "" / "src",
    "" / "test"
  )

  private operator fun String.div(s: String) = "$this${File.separator}$s"

  fun calculateDummyJavaSourceModuleNames(entity: JavaModule) =
    calculateDummyJavaSourceModuleRoots(entity).map(::calculateDummyJavaSourceModuleName)

  private fun calculateDummyJavaSourceModuleRoots(entity: JavaModule) =
    entity.sourceRoots.mapNotNull(::calculateDummyJavaSourceModuleRoot).distinct()

  private fun calculateDummyJavaSourceModuleRoot(entitySourceRoot: JavaSourceRoot): String? {
    val sourceRootPackagePrefix = entitySourceRoot.packagePrefix
    if (sourceRootPackagePrefix.isEmpty()) return null
    val entitySourceRootSourcePath = entitySourceRoot.sourcePath
    val sourceRootDirPath = entitySourceRootSourcePath.let { if (it.isDirectory()) it else it.parent }.toString().appendTrailSeparator()
    return sourceRootDirPath
      .substringBeforeLast(
        sourceRootPackagePrefix.replace(".", File.separator).prependSeparator().appendTrailSeparator(),
        missingDelimiterValue = ""
      )
      .removeTrailSeparator()
      .ifEmpty { null }
  }

  private fun calculateDummyJavaSourceModuleName(extractedDummyJavaSourceModuleRoot: String): String {
    val extractedStructure = TYPICAL_STRUCTURES.find { extractedDummyJavaSourceModuleRoot.endsWith(it) }
    val extractedParent = extractedStructure?.let { extractedDummyJavaSourceModuleRoot.substringBeforeLast(it) } ?: extractedDummyJavaSourceModuleRoot
    val extractedName = extractedParent.split(File.separator).let { if (it.isEmpty()) "" else it.last() }
    return "${DUMMY_JAVA_SOURCE_MODULE_SIGNATURE}-${extractedName}-${hashString(extractedDummyJavaSourceModuleRoot)}"
  }

  private fun hashString(input: String, algorithm: String = "MD5", take: Int = 7) =
    MessageDigest
      .getInstance(algorithm)
      .digest(input.toByteArray())
      .fold("") { str, it -> str + "%02x".format(it) }
      .take(take)

  private fun String.prependSeparator() = if (this.startsWith(File.separator)) this else "" / this
  private fun String.appendTrailSeparator() = if (this.endsWith(File.separator)) this else this / ""
  private fun String.removeTrailSeparator() = this.replace(Regex("${File.separator}$"), "")

  fun isDummyJavaSourceModule(entity: JavaModule) = entity.module.name.startsWith(DUMMY_JAVA_SOURCE_MODULE_SIGNATURE)

  fun calculateDummyJavaSourceModules(entity: JavaModule): List<JavaModule> {
    val dummyJavaSourceModuleRootPaths = calculateDummyJavaSourceModuleRoots(entity).map { it.toPath() }
    val dummyJavaSourceModuleNames = calculateDummyJavaSourceModuleNames(entity)
    return dummyJavaSourceModuleNames.zip(dummyJavaSourceModuleRootPaths).map { (dummyJavaSourceModuleName, dummyJavaSourceModuleRootPath) ->
      JavaModule(
        module = Module(
          name = dummyJavaSourceModuleName,
          type = ModuleTypeId.JAVA_MODULE,
          modulesDependencies = listOf(),
          librariesDependencies = listOf()
        ),
        baseDirContentRoot = ContentRoot(url = dummyJavaSourceModuleRootPath),
        sourceRoots = listOf(
          JavaSourceRoot(
            sourcePath = dummyJavaSourceModuleRootPath,
            generated = true,
            packagePrefix = "",
            rootType = DUMMY_JAVA_SOURCE_MODULE_ROOT_TYPE,
            targetId = BuildTargetIdentifier(dummyJavaSourceModuleName)
          )
        ),
        resourceRoots = listOf(),
        libraries = listOf(),
        compilerOutput = null,
        jvmJdkInfo = null
      )
    }
  }

  private fun String.toPath() = Paths.get(this)
}

internal class JavaModuleWithoutSourcesUpdater(
  private val workspaceModelEntityUpdaterConfig: WorkspaceModelEntityUpdaterConfig,
) : WorkspaceModelEntityWithoutParentModuleUpdater<JavaModule, ModuleEntity> {

  override fun addEntity(entityToAdd: JavaModule): ModuleEntity {
    val moduleEntityUpdater = ModuleEntityUpdater(workspaceModelEntityUpdaterConfig)
    val moduleEntity = moduleEntityUpdater.addEntity(entityToAdd.module)

    val contentRootEntityUpdater = ContentRootEntityUpdater(workspaceModelEntityUpdaterConfig)
    contentRootEntityUpdater.addEntity(entityToAdd.baseDirContentRoot, moduleEntity)

    return moduleEntity
  }
}

internal class JavaModuleUpdater(
  private val workspaceModelEntityUpdaterConfig: WorkspaceModelEntityUpdaterConfig,
) : WorkspaceModelEntityWithoutParentModuleUpdater<JavaModule, ModuleEntity> {

  private val javaModuleWithSourcesUpdater = JavaModuleWithSourcesUpdater(workspaceModelEntityUpdaterConfig)
  private val javaModuleWithoutSourcesUpdater = JavaModuleWithoutSourcesUpdater(workspaceModelEntityUpdaterConfig)

  override fun addEntity(entityToAdd: JavaModule): ModuleEntity =
    if (entityToAdd.sourceRoots.isEmpty() && entityToAdd.resourceRoots.isEmpty())
      javaModuleWithoutSourcesUpdater.addEntity(entityToAdd)
    else {
      javaModuleWithSourcesUpdater.addDummyJavaSourceModuleIfNotYetAdded(entityToAdd)
      javaModuleWithSourcesUpdater.addEntity(entityToAdd)
    }


  private fun JavaModuleWithSourcesUpdater.addDummyJavaSourceModuleIfNotYetAdded(
    entity: JavaModule) {
    val dummyJavaSourceModuleNames = DummyJavaSourceModuleUtils.calculateDummyJavaSourceModuleNames(entity)
    val dummyJavaSourceModules = DummyJavaSourceModuleUtils.calculateDummyJavaSourceModules(entity)
    dummyJavaSourceModuleNames.zip(dummyJavaSourceModules).forEach { (dummyJavaSourceModuleName, dummyJavaSourceModule) ->
      run {
        if (!workspaceModelEntityUpdaterConfig.workspaceEntityStorageBuilder.contains(ModuleId(name = dummyJavaSourceModuleName)))
          this.addEntity(dummyJavaSourceModule)
      }
    }
  }
}
