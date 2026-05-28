package org.jetbrains.bazel.workspace.importer

import com.intellij.java.workspace.entities.JavaModuleSettingsEntity
import com.intellij.java.workspace.entities.javaSettings
import com.intellij.openapi.roots.DependencyScope
import com.intellij.platform.workspace.jps.entities.ModuleDependency
import com.intellij.platform.workspace.jps.entities.ModuleDependencyItem
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.ModuleId
import com.intellij.platform.workspace.jps.entities.ModuleSourceDependency
import com.intellij.platform.workspace.jps.entities.ModuleTypeId
import com.intellij.platform.workspace.jps.entities.SdkDependency
import com.intellij.platform.workspace.jps.entities.SdkId
import com.intellij.platform.workspace.jps.entities.modifyModuleEntity
import com.intellij.platform.workspace.storage.EntitySource
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.url.VirtualFileUrlManager
import com.intellij.pom.java.LanguageLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.magicmetamodel.formatAsModuleName
import org.jetbrains.bazel.scala.sdk.scalaSdkExtensionExists
import org.jetbrains.bazel.sync.workspace.languages.java.sourceRoot.JvmPackagePrefixCalculator
import org.jetbrains.bazel.workspace.indexAdditionalFiles.ProjectViewGlobSet
import org.jetbrains.bazel.workspacemodel.entities.BazelDummyEntitySource
import org.jetbrains.bazel.workspacemodel.entities.BazelModuleEntitySource
import org.jetbrains.bazel.workspacemodel.entities.BazelModuleExtensionEntity
import org.jetbrains.bazel.workspacemodel.entities.WorkspaceModelTargetLabel
import org.jetbrains.bazel.workspacemodel.entities.WorkspaceModelTargetLabelList
import org.jetbrains.bazel.workspacemodel.entities.bazelModuleExtension
import org.jetbrains.bsp.protocol.KotlinBuildTarget
import org.jetbrains.bsp.protocol.LibraryItem
import org.jetbrains.bsp.protocol.RawBuildTarget
import org.jetbrains.bsp.protocol.ScalaBuildTarget
import org.jetbrains.bsp.protocol.StrictDependencyCheckedType
import org.jetbrains.bsp.protocol.utils.StringUtils
import org.jetbrains.bsp.protocol.utils.extractJvmBuildTarget
import org.jetbrains.bsp.protocol.utils.extractKotlinBuildTarget
import org.jetbrains.bsp.protocol.utils.extractScalaBuildTarget
import java.nio.file.Path
import com.intellij.platform.workspace.jps.entities.DependencyScope as EntitiesDependencyScope

/**
 * shared per-import-pass context for [JvmTargetEntitiesBuilder] and the per-aspect builders it calls.
 * build it once per import; reuse across all per-target invocations.
 */
@ApiStatus.Internal
class ImportContext(
  val targets: Collection<RawBuildTarget>,
  val libraries: List<LibraryItem>,
  val repoMapping: RepoMapping,
  val projectName: String,
  val projectBasePath: Path,
  val defaultJdkName: String?,
  val testSourcesGlob: ProjectViewGlobSet,
  val testTargets: Set<Label>,
  val packagePrefixes: JvmPackagePrefixCalculator,
  val fileToTargets: Map<Path, List<Label>>,
  val virtualFileUrlManager: VirtualFileUrlManager,
) {
  val moduleNamesByLabel: Map<Label, String> = targets.associate { it.id to it.id.formatAsModuleName(repoMapping) }

  // only JVM targets become modules, non-JVM target names must not influence dependency resolution,
  // otherwise a dep on a toolchain target would be wrongly emitted as a ModuleDependency.
  val knownModuleNames: Set<String> = targets.asSequence()
    .filter { it.kind.isJvmTarget() }
    .mapTo(mutableSetOf()) { it.id.formatAsModuleName(repoMapping) }
  val knownLibraryNames: Set<String> = libraries.map { it.id.formatAsModuleName(repoMapping) }
    .toSet()
  val dependencyBuilder: DependencyBuilder = DependencyBuilder(targets)
  val dummyModuleSplitter: DummyModuleSplitter = DummyModuleSplitter(projectBasePath, fileToTargets)
}

/**
 * Writes the full set of JVM workspace-model entities for all [ImportContext.targets] (plus any dummy modules
 * they split into) directly into the supplied [MutableEntityStorage].
 *
 * Combines what used to live across the deleted MMM transformer + updater hierarchy
 * (ModuleDetailsToJavaModuleTransformer, JavaModuleUpdater, ModuleEntityUpdater and friends).
 *
 * Runs two passes: first resolves every target into a [TargetPlan] (pure, no writes) so the dummy modules know
 * which directories are already covered by real source roots; then writes all entities sequentially.
 */
@ApiStatus.Internal
class JvmTargetEntitiesBuilder(private val ctx: ImportContext) {
  private val javaModuleType = ModuleTypeId("JAVA_MODULE")
  private val dummyModuleType = ModuleTypeId(BazelDummyModuleType.ID)
  private val resolverParallelism = Runtime.getRuntime().availableProcessors() * 2
  private val resolverBatchSize = 512

  suspend fun writeAll(storage: MutableEntityStorage): Unit = coroutineScope {
    // phase 1: resolve every target. no storage writes.
    val dispatcher = Dispatchers.Default.limitedParallelism(resolverParallelism)
    val plans: List<Pair<RawBuildTarget, TargetPlan>> = ctx.targets.chunked(resolverBatchSize)
      .map { batch ->
        async(dispatcher) {
          batch.mapNotNull { it to (resolve(it) ?: return@mapNotNull null) }
        }
      }
      .awaitAll()
      .flatten()

    // collect every directory already covered by a real (non-dummy) source root, so dummy package markers
    // don't re-walk them. matches PackageMarkerEntityUpdater's `alreadyVisitedDirectories` initialization.
    val coveredDirs = plans
      .flatMap { (_, plan) -> plan.mainSourceRoots.map { it.sourcePath } }
      .toMutableSet()
    val packageMarkerBuilder = PackageMarkerBuilder(coveredDirs, PackageMarkerBuilder.excludedDirectoriesFrom(storage))

    // phase 2: write entities sequentially.
    // `writtenNames` preserves the original `distinctBy { it.getModuleName() }` semantics: if two targets
    // (or dummies) end up with the same module name, the first one wins and the rest are skipped.
    val writtenNames = mutableSetOf<String>()
    for ((target, plan) in plans) {
      writeOne(target, plan, packageMarkerBuilder, writtenNames, storage)
    }
  }

  /**
   * Returns null when the target should be skipped:
   *  - non-JVM targets (matches the `isJvmTarget` filter in the old TargetIdToModuleEntitiesMap),
   *  - kotlin with-sources targets when the facet EP is missing.
   */
  private fun resolve(target: RawBuildTarget): TargetPlan? {
    if (!target.kind.isJvmTarget()) {
      return null
    }
    val moduleName = ctx.moduleNamesByLabel[target.id] ?: target.id.formatAsModuleName(ctx.repoMapping)
    val resolvedDeps = ctx.dependencyBuilder.resolve(target)
    val jdkName = jdkNameFor(target)
    val javaLangVersion = extractJvmBuildTarget(target)?.javaVersion
    val javacOptions = extractJvmBuildTarget(target)?.javacOpts.orEmpty()
    val jvmBinaryJars = extractJvmBuildTarget(target)?.binaryOutputs.orEmpty()
    val scalaTarget = extractScalaBuildTarget(target)
    val kotlinTarget = extractKotlinBuildTarget(target)
    val associates = kotlinTarget?.associates?.distinct()?.mapNotNull { ctx.moduleNamesByLabel[it] }.orEmpty()

    val hasSources = target.sources.isNotEmpty()
    val hasResources = target.resources.isNotEmpty()
    val isJavaKotlin = target.kind.includesJava() || target.kind.includesKotlin()

    return when {
      !hasSources && !hasResources && isJavaKotlin ->
        TargetPlan.WithoutSources(moduleName, resolvedDeps, jdkName, javaLangVersion)

      target.kind.includesKotlin() && KotlinFacetEntityUpdater.ep.extensionList.isEmpty() ->
        // original behavior: kotlin module with sources requires the EP, skip otherwise.
        //
        // but if target has both java + kotlin and `KotlinFacetEntityUpdater` is empty,
        // then module won't be created?
        //
        // TODO: I don't really know is that intended behavior, have to be investigated
        //  however `KotlinFacetEntityUpdater` to be empty is nearly impossible,
        //  someone would have to disable kotlin plugin explicitly (and then our content module won't be loaded) :p
        null

      else -> {
        val resolvedSourceRoots = SourceRootBuilder.resolve(target, ctx.testSourcesGlob, ctx.packagePrefixes, ctx.testTargets)
        val splitResult = ctx.dummyModuleSplitter.split(target.baseDirectory, resolvedSourceRoots)
        val mainSourceRoots = when (splitResult) {
          is DummyModuleSplitter.MergedRoots -> splitResult.mergedSourceRoots
          is DummyModuleSplitter.DummyModulesToAdd -> splitResult.originalSourceRoots
        }
        val dummies = (splitResult as? DummyModuleSplitter.DummyModulesToAdd)?.dummies.orEmpty()
        val resourceRoots = ResourceRootBuilder.resolve(target, ctx.projectName, ctx.testTargets)
        TargetPlan.Full(
          moduleName = moduleName,
          resolvedDeps = resolvedDeps,
          jdkName = jdkName,
          javaLangVersion = javaLangVersion,
          javacOptions = javacOptions,
          jvmBinaryJars = jvmBinaryJars,
          scalaTarget = scalaTarget,
          kotlinTarget = kotlinTarget,
          associates = associates,
          mainSourceRoots = mainSourceRoots,
          dummies = dummies,
          resourceRoots = resourceRoots,
        )
      }
    }
  }

  private fun writeOne(
    target: RawBuildTarget,
    plan: TargetPlan,
    packageMarkerBuilder: PackageMarkerBuilder,
    writtenNames: MutableSet<String>,
    storage: MutableEntityStorage,
  ) {
    if (!writtenNames.add(plan.moduleName)) {
      return
    }
    when (plan) {
      is TargetPlan.WithoutSources -> writeWithoutSources(target, plan, storage)
      is TargetPlan.Full -> writeFull(target, plan, packageMarkerBuilder, writtenNames, storage)
    }
  }

  private fun writeWithoutSources(target: RawBuildTarget, plan: TargetPlan.WithoutSources, storage: MutableEntityStorage) {
    // matches JavaModuleWithoutSourcesUpdater: only [SdkDep] as base, no ModuleSourceDependency
    // (sourceless modules don't own a source root, so no module-source-dependency).
    val baseDeps = if (plan.jdkName != null) {
      listOf(SdkDependency(SdkId(plan.jdkName, "JavaSDK")))
    }
    else {
      emptyList()
    }
    val deps = baseDeps + dependenciesAsItems(plan.moduleName, plan.resolvedDeps, scalaSdkDep = null)
    addModuleEntity(target, plan.moduleName, plan.resolvedDeps, deps, storage)
  }

  private fun writeFull(
    target: RawBuildTarget,
    plan: TargetPlan.Full,
    packageMarkerBuilder: PackageMarkerBuilder,
    writtenNames: MutableSet<String>,
    storage: MutableEntityStorage,
  ) {
    val scalaSdkDep = plan.scalaTarget?.takeIf { scalaSdkExtensionExists() }
      ?.let { toLibraryDependency(libraryName = it.scalaVersion.scalaVersionToScalaSdkName(), exported = false) }
    val associatesDeps = plan.associates.map { moduleDependency(it, exported = true) }
    val deps = baseDependencies(plan.jdkName) +
               dependenciesAsItems(plan.moduleName, plan.resolvedDeps, scalaSdkDep) +
               associatesDeps

    val moduleEntity = addModuleEntity(target, plan.moduleName, plan.resolvedDeps, deps, storage)
    addJavaModuleSettings(moduleEntity, plan.javaLangVersion, storage)

    if (plan.scalaTarget != null) {
      ScalaAddendumBuilder.write(plan.scalaTarget, moduleEntity, ctx.virtualFileUrlManager, storage)
    }

    ResourceRootBuilder.write(plan.resourceRoots, moduleEntity, ctx.virtualFileUrlManager, storage)
    SourceRootBuilder.write(plan.mainSourceRoots, moduleEntity, ctx.virtualFileUrlManager, storage)

    if (plan.jvmBinaryJars.isNotEmpty()) {
      JvmBinaryJarsBuilder.write(plan.jvmBinaryJars, moduleEntity, ctx.virtualFileUrlManager, storage)
    }

    if (plan.kotlinTarget != null) {
      KotlinFacetBuilder.write(
        kotlinBuildTarget = plan.kotlinTarget,
        isTestModule = target.kind.ruleType == RuleType.TEST,
        associates = plan.associates.toSet(),
        parentModuleEntity = moduleEntity,
        storage = storage,
      )
    }

    for (dummy in plan.dummies) {
      if (!writtenNames.add(dummy.name)) {
        continue
      }
      writeDummy(target, dummy, plan, packageMarkerBuilder, storage)
    }
  }

  private fun writeDummy(
    parentTarget: RawBuildTarget,
    dummy: DummyModuleSplitter.DummyModule,
    parentPlan: TargetPlan.Full,
    packageMarkerBuilder: PackageMarkerBuilder,
    storage: MutableEntityStorage,
  ) {
    val deps = baseDependencies(parentPlan.jdkName)
    val entitySource = BazelModuleEntitySource(dummy.name)
    val moduleEntity = storage.addEntity(
      ModuleEntity(
        name = dummy.name,
        dependencies = deps,
        entitySource = entitySource,
      ) {
        this.type = dummyModuleType
        this.bazelModuleExtension = BazelModuleExtensionEntity(
          label = WorkspaceModelTargetLabel(parentTarget.id),
          strictDependencies = WorkspaceModelTargetLabelList(StrictDependencyCheckedType.OFF, emptyList()),
          entitySource = entitySource,
        )
      },
    )
    addJavaModuleSettings(moduleEntity, parentPlan.javaLangVersion, storage)

    // dummies get PackageMarkerEntity instead of SourceRootEntity: the source root path is the directory we
    // recursively walk for package markers, not a real source folder declaration.
    packageMarkerBuilder.write(
      sourceRoots = listOf(dummy.sourceRoot),
      parentModuleEntity = moduleEntity,
      virtualFileUrlManager = ctx.virtualFileUrlManager,
      storage = storage,
    )

    // dummies always get a kotlin facet (their kind always includesKotlin); options are inherited from the
    // parent target (which may be null for non-kotlin parents - KotlinFacetEntityUpdater handles null options).
    if (KotlinFacetEntityUpdater.ep.extensionList.isNotEmpty()) {
      KotlinFacetBuilder.write(
        kotlinBuildTarget = parentPlan.kotlinTarget,
        isTestModule = parentTarget.kind.ruleType == RuleType.TEST,
        associates = emptySet(),
        parentModuleEntity = moduleEntity,
        storage = storage,
      )
    }
  }

  private fun addModuleEntity(
    target: RawBuildTarget,
    moduleName: String,
    resolvedDeps: DependencyBuilder.Resolved,
    dependencies: List<ModuleDependencyItem>,
    storage: MutableEntityStorage,
  ): ModuleEntity {
    val entitySource = toEntitySource(target.kind, moduleName)
    return storage.addEntity(
      ModuleEntity(
        name = moduleName,
        dependencies = dependencies,
        entitySource = entitySource,
      ) {
        this.type = javaModuleType
        this.bazelModuleExtension = BazelModuleExtensionEntity(
          label = WorkspaceModelTargetLabel(target.id),
          strictDependencies = WorkspaceModelTargetLabelList(
            resolvedDeps.strictDependenciesCheck,
            resolvedDeps.strictDependencies.map { it.toString() },
          ),
          entitySource = entitySource,
        )
      },
    )
  }

  private fun addJavaModuleSettings(parent: ModuleEntity, javaLanguageVersion: String?, storage: MutableEntityStorage) {
    val entity = JavaModuleSettingsEntity(
      inheritedCompilerOutput = false,
      excludeOutput = true,
      entitySource = parent.entitySource,
    ) {
      this.languageLevelId = LanguageLevel.parse(javaLanguageVersion)?.name
    }
    storage.modifyModuleEntity(parent) {
      this.javaSettings = entity
    }
  }

  private fun toEntitySource(kind: TargetKind, moduleName: String): EntitySource =
    if (kind.isJvmTarget()) {
      BazelModuleEntitySource(moduleName)
    }
    else {
      BazelDummyEntitySource
    }

  private fun baseDependencies(jdkName: String?): List<ModuleDependencyItem> = buildList {
    add(ModuleSourceDependency)
    if (jdkName != null) {
      add(SdkDependency(SdkId(jdkName, "JavaSDK")))
    }
  }

  private fun dependenciesAsItems(
    moduleName: String,
    resolved: DependencyBuilder.Resolved,
    scalaSdkDep: ModuleDependencyItem?,
  ): List<ModuleDependencyItem> = buildList {
    if (scalaSdkDep != null) {
      add(scalaSdkDep)
    }
    addAll(
      resolved.dependencies.mapNotNull { dep ->
        val depModuleName = dep.label.formatAsModuleName(ctx.repoMapping)
        val scope = if (dep.isRuntime) DependencyScope.RUNTIME else DependencyScope.COMPILE
        val asModule = depModuleName.takeIf { it in ctx.knownModuleNames && it != moduleName }
        if (asModule != null) {
          return@mapNotNull moduleDependency(asModule, exported = dep.exported, scope = scope)
        }
        val asLibrary = depModuleName.takeIf { it in ctx.knownLibraryNames }
        if (asLibrary != null) {
          return@mapNotNull toLibraryDependency(asLibrary, exported = dep.exported, scope = scope)
        }
        null
      },
    )
  }

  private fun moduleDependency(
    moduleName: String,
    exported: Boolean,
    scope: DependencyScope = DependencyScope.COMPILE,
  ): ModuleDependencyItem =
    ModuleDependency(
      module = ModuleId(moduleName),
      exported = exported,
      scope = scope.toEntityScope(),
      productionOnTest = true,
    )

  private fun DependencyScope.toEntityScope(): EntitiesDependencyScope = when (this) {
    DependencyScope.COMPILE -> EntitiesDependencyScope.COMPILE
    DependencyScope.RUNTIME -> EntitiesDependencyScope.RUNTIME
    DependencyScope.PROVIDED -> EntitiesDependencyScope.PROVIDED
    DependencyScope.TEST -> EntitiesDependencyScope.TEST
  }

  private fun jdkNameFor(target: RawBuildTarget): String? =
    extractJvmBuildTarget(target)?.javaHome?.let { ctx.projectName.projectNameToJdkName(it) } ?: ctx.defaultJdkName

  private sealed interface TargetPlan {
    val moduleName: String
    val resolvedDeps: DependencyBuilder.Resolved
    val jdkName: String?
    val javaLangVersion: String?
    val mainSourceRoots: List<SourceRootBuilder.ResolvedSourceRoot> get() = emptyList()

    data class WithoutSources(
      override val moduleName: String,
      override val resolvedDeps: DependencyBuilder.Resolved,
      override val jdkName: String?,
      override val javaLangVersion: String?,
    ) : TargetPlan

    data class Full(
      override val moduleName: String,
      override val resolvedDeps: DependencyBuilder.Resolved,
      override val jdkName: String?,
      override val javaLangVersion: String?,
      val javacOptions: List<String>,
      val jvmBinaryJars: List<Path>,
      val scalaTarget: ScalaBuildTarget?,
      val kotlinTarget: KotlinBuildTarget?,
      val associates: List<String>,
      override val mainSourceRoots: List<SourceRootBuilder.ResolvedSourceRoot>,
      val dummies: List<DummyModuleSplitter.DummyModule>,
      val resourceRoots: List<ResourceRootBuilder.ResolvedResourceRoot>,
    ) : TargetPlan
  }
}

// RC: naming helpers duplicated from ModuleDetailsToJavaModuleTransformer.kt so this file is independent of MMM.
@ApiStatus.Internal
fun String.scalaVersionToScalaSdkName(): String = "scala-sdk-$this"

@ApiStatus.Internal
fun String.projectNameToBaseJdkName(): String = "$this-jdk"

@ApiStatus.Internal
fun String.projectNameToJdkName(javaHomeUri: Path): String =
  projectNameToBaseJdkName() + "-" + StringUtils.md5Hash(javaHomeUri.toString(), 5)
