package org.jetbrains.bazel.python.sync

import com.google.devtools.intellij.aspect.Common.ArtifactLocation
import com.google.devtools.intellij.ideinfo.IntellijIdeInfo
import com.intellij.bazel.python.backend.BazelPyImportResolver
import com.intellij.bazel.python.backend.chooseSdkName
import com.intellij.bazel.python.backend.findBazelPythonShortestQualifiedName
import com.intellij.bazel.python.backend.hasBazelPythonQualifiedName
import com.intellij.bazel.python.backend.sync.PythonLanguagePlugin
import com.intellij.ide.util.ModuleRendererFactory
import com.intellij.openapi.application.edtWriteAction
import com.intellij.openapi.application.runReadActionBlocking
import com.intellij.openapi.diagnostic.fileLogger
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.platform.eel.provider.localEel
import com.intellij.platform.util.progress.reportSequentialProgress
import com.intellij.platform.workspace.jps.entities.ContentRootEntity
import com.intellij.platform.workspace.jps.entities.DependencyScope
import com.intellij.platform.workspace.jps.entities.LibraryDependency
import com.intellij.platform.workspace.jps.entities.LibraryEntity
import com.intellij.platform.workspace.jps.entities.LibraryRootTypeId
import com.intellij.platform.workspace.jps.entities.ModuleDependency
import com.intellij.platform.workspace.jps.entities.ModuleDependencyItem
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.ModuleId
import com.intellij.platform.workspace.jps.entities.ModuleSourceDependency
import com.intellij.platform.workspace.jps.entities.ModuleTypeId
import com.intellij.platform.workspace.jps.entities.SdkDependency
import com.intellij.platform.workspace.jps.entities.SdkId
import com.intellij.platform.workspace.jps.entities.SourceRootEntity
import com.intellij.platform.workspace.jps.entities.SourceRootTypeId
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import com.intellij.psi.PsiManager
import com.intellij.psi.util.QualifiedName
import com.intellij.python.community.services.systemPython.SystemPythonService
import com.intellij.testFramework.common.timeoutRunBlocking
import com.jetbrains.python.PythonBinary
import com.jetbrains.python.psi.PyFile
import com.jetbrains.python.psi.resolve.PyQualifiedNameResolveContextImpl
import com.jetbrains.python.sdk.PythonSdkUtil
import com.jetbrains.python.sdk.internal.PYTHON_MODULE_ID
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.collections.shouldExist
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.runBlocking
import org.jetbrains.bazel.commons.BazelInfo
import org.jetbrains.bazel.commons.RepoMappingDisabled
import org.jetbrains.bazel.languages.projectview.IMPORT_DEPTH_KEY
import org.jetbrains.bazel.languages.projectview.ProjectView
import org.jetbrains.bazel.magicmetamodel.formatAsLibraryName
import org.jetbrains.bazel.magicmetamodel.formatAsModuleName
import org.jetbrains.bazel.progress.syncConsole
import org.jetbrains.bazel.python.lang.PythonBuildTarget
import org.jetbrains.bazel.sync.BazelOutFileHardLinks
import org.jetbrains.bazel.sync.workspace.BazelResolvedWorkspace
import org.jetbrains.bazel.sync.workspace.DefaultOutputLocationResolver
import org.jetbrains.bazel.sync.workspace.importer.WorkspaceImporterHelper
import org.jetbrains.bazel.sync.workspace.mapper.normal.DefaultBazelOutputFileHardLinks
import org.jetbrains.bazel.sync.workspace.snapshot.OutputLocationCollectionBuilder
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshotBuilder
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.CopyingBazelOutHardLinks
import org.jetbrains.bazel.test.framework.testproject.TestTargetBuilder
import org.jetbrains.bazel.test.framework.testproject.bazelInfo
import org.jetbrains.bazel.test.framework.testproject.buildServer
import org.jetbrains.bazel.test.framework.testproject.clearSnapshot
import org.jetbrains.bazel.test.framework.testproject.outputBase
import org.jetbrains.bazel.test.framework.testproject.pythonTarget
import org.jetbrains.bazel.test.framework.testproject.sync
import org.jetbrains.bazel.test.framework.testproject.testBazelProjectFixture
import org.jetbrains.bazel.test.framework.testproject.testProjectBazelInfo
import org.jetbrains.bazel.test.framework.testproject.virtualFileUrlManager
import org.jetbrains.bazel.test.framework.testproject.workspaceRoot
import org.jetbrains.bazel.test.framework.target.TestBuildTarget
import org.jetbrains.bazel.test.framework.testBazelInfo
import org.jetbrains.bazel.workspace.model.matchers.entries.ExpectedModuleEntity
import org.jetbrains.bazel.workspace.model.matchers.entries.ExpectedSourceRootEntity
import org.jetbrains.bazel.workspace.model.matchers.entries.shouldContainExactlyInAnyOrder
import org.jetbrains.bazel.workspace.model.test.framework.BuildServerMock
import org.jetbrains.bazel.workspace.model.test.framework.TEST_OUTPUT_ROOT
import org.jetbrains.bazel.workspace.model.test.framework.generatedTestLocation
import org.jetbrains.bazel.workspacemodel.entities.BazelProjectEntitySource
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.OutputLocation
import org.jetbrains.bsp.protocol.OutputLocationResolver
import org.jetbrains.bsp.protocol.StringsCollection
import org.jetbrains.bsp.protocol.TaskGroupId
import org.jetbrains.bsp.protocol.toExecrootPath
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import java.nio.file.Path
import java.util.Collections
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteExisting
import kotlin.io.path.writeText

private data class PythonTestSet(
  val workspaceSnapshot: WorkspaceSnapshot,
  val expectedModuleEntities: List<ExpectedModuleEntity>,
  val expectedSourceRootEntities: List<ExpectedSourceRootEntity>,
)

// TODO: convert it to real `BazelWorkspaceImporter` test fixture
@BazelTestApplication
class PythonProjectSyncTest {
  private val testBazel by testBazelProjectFixture()
  lateinit var pythonBinary: PythonBinary

  private companion object {
    val logger = fileLogger()
  }

  @BeforeEach
  fun beforeEach() {
    // given
    // Python plugin validates file.
    // The real python should have been used, but in absence of it this path is good enough as most agents on TC are Ubuntu
    pythonBinary =
      timeoutRunBlocking { SystemPythonService().findSystemPythons(localEel).firstOrNull()?.pythonBinary ?: Path.of("/usr/bin/python3") }
    logger.info("Found python $pythonBinary for project ${testBazel.project.name}")
  }

  // Drop SDKs created by this test
  @AfterEach
  fun cleanupSdk(): Unit = timeoutRunBlocking {
    edtWriteAction {
      val table = ProjectJdkTable.getInstance()
      for (sdk in PythonSdkUtil.getAllSdks()) {
        table.removeJdk(sdk)
      }
    }
  }

  @Test
  fun `should add module with dependencies to workspace model diff`() {
    // given
    val pythonTestTargets = generateTestSet()

    // when
    val diff = MutableEntityStorage.create()
    runPythonImporter(pythonTestTargets.workspaceSnapshot, diff)

    // then
    val actualModuleEntities =
      diff.entities(ModuleEntity::class.java)
        .toList()
        .filter { it.type == ModuleTypeId(PYTHON_MODULE_ID) }
    logger.info("Checking for project ${testBazel.project.name}")
    actualModuleEntities shouldContainExactlyInAnyOrder pythonTestTargets.expectedModuleEntities
    actualModuleEntities.shouldAllHaveTheSameSDK()
  }

  @Test
  fun `should add module with sources to workspace model diff`() {
    // given
    val pythonTestTargets = generateTestSetWithSources()

    // when
    val diff = MutableEntityStorage.create()
    runPythonImporter(pythonTestTargets.workspaceSnapshot, diff)

    // then
    val actualModuleEntities =
      diff.entities(SourceRootEntity::class.java)
        .toList()

    actualModuleEntities shouldContainExactlyInAnyOrder pythonTestTargets.expectedSourceRootEntities
  }

  @Test
  fun `should add detailed source root for target without Bazel imports`() {
    // given
    val expectedSourceRootPath = testBazel.workspaceRoot.resolve("tools/helper/op.py")
    val target = testPythonTarget("@@server//tools/helper:helper") { srcs("tools/helper/op.py") }
    val expectedModuleEntity = generateExpectedModuleEntity(target, emptyList())
    val expectedSourceRootEntity =
      generateExpectedSourceRootEntity(expectedSourceRootPath, "python-source", expectedModuleEntity.moduleEntity)

    // when
    val diff = MutableEntityStorage.create()
    runPythonImporter(generateWorkspaceSnapshot(listOf(target)), diff)

    // then
    val actualModuleEntities =
      diff.entities(SourceRootEntity::class.java)
        .toList()
    actualModuleEntities shouldContainExactlyInAnyOrder listOf(expectedSourceRootEntity)
  }

  @Test
  fun `should index root-relative package location for target without Bazel imports`() {
    val rootRelativePath = "tools/helper/op.py"
    val rootRelativeSource = testBazel.workspaceRoot.resolve(rootRelativePath)
    val pyFile = createPythonFile(
      relativePath = rootRelativePath,
      text = """
        class Foo:
            pass
      """.trimIndent(),
    )
    val importedRelativePath = "src/aaa/bbb.py"
    val importedSource = testBazel.workspaceRoot.resolve(importedRelativePath)
    createPythonFile(
      relativePath = importedRelativePath,
      text = """
        class Bar:
            pass
      """.trimIndent(),
    )
    val targetWithoutImports = testPythonTarget("@@server//tools/helper:helper") { srcs(rootRelativePath) }
    val targetWithImports = testPythonTarget("@@server//:imported", imports = listOf("src")) { srcs(importedRelativePath) }

    runPythonImporter(
      generateWorkspaceSnapshot(listOf(targetWithoutImports, targetWithImports)),
      MutableEntityStorage.create(),
      runPostProcessing = true,
    )

    testBazel.project.findBazelPythonShortestQualifiedName(rootRelativeSource) shouldBe "tools.helper.op"
    testBazel.project.findBazelPythonShortestQualifiedName(importedSource) shouldBe "aaa.bbb"
    testBazel.project.hasBazelPythonQualifiedName("src.aaa.bbb").shouldBeFalse()
    runReadActionBlocking {
      renderModuleLocationText(pyFile.findTopLevelClass("Foo")!!) shouldBe "(tools.helper.op)"
    }
  }

  @Test
  fun `should render Python symbols with package location in Bazel project`() {
    val pyFile = createPythonFile(
      relativePath = "aaa/bbb.py",
      text = """
        class Foo:
            def bar(self):
                pass
      """.trimIndent(),
    )
    runReadActionBlocking {
      val pyClass = pyFile.findTopLevelClass("Foo")!!
      val method = pyClass.findMethodByName("bar", false, null)!!

      renderModuleLocationText(pyClass) shouldBe "(aaa.bbb)"
      renderModuleLocationText(method) shouldBe "(Foo in aaa.bbb)"
    }
  }

  @Test
  fun `should render Python symbols with package location from Bazel imports`() {
    val relativePath = "src/aaa/bbb.py"
    val pyFile = createPythonFile(
      relativePath = relativePath,
      text = """
        class Foo:
            def bar(self):
                pass
      """.trimIndent(),
    )
    val target = testPythonTarget("@@server//:main_app", imports = listOf("src")) { srcs(relativePath) }

    runPythonImporter(generateWorkspaceSnapshot(listOf(target)), MutableEntityStorage.create(), runPostProcessing = true)

    runReadActionBlocking {
      val pyClass = pyFile.findTopLevelClass("Foo")!!
      val method = pyClass.findMethodByName("bar", false, null)!!

      renderModuleLocationText(pyClass) shouldBe "(aaa.bbb)"
      renderModuleLocationText(method) shouldBe "(Foo in aaa.bbb)"
    }
  }

  @Test
  fun `should render shortest Python package location from Bazel imports`() {
    val relativePath = "src/app/aaa/bbb.py"
    val pyFile = createPythonFile(
      relativePath = relativePath,
      text = """
        class Foo:
            def bar(self):
                pass
      """.trimIndent(),
    )
    val target = testPythonTarget("@@server//:main_app", imports = listOf("src", "src/app")) { srcs(relativePath) }

    runPythonImporter(generateWorkspaceSnapshot(listOf(target)), MutableEntityStorage.create(), runPostProcessing = true)

    runReadActionBlocking {
      val pyClass = pyFile.findTopLevelClass("Foo")!!
      val method = pyClass.findMethodByName("bar", false, null)!!

      renderModuleLocationText(pyClass) shouldBe "(aaa.bbb)"
      renderModuleLocationText(method) shouldBe "(Foo in aaa.bbb)"
    }
  }

  @Test
  fun `should resolve generated Python source delivered via srcs (BuildTarget generatedSources)`() {
    val execRoot = testBazel.outputBase.resolve("bazel-exec")
    val bazelBin = execRoot.resolve("bazel-out/${TEST_OUTPUT_ROOT.path}").createDirectories()
    val generatedFile = bazelBin.resolve("part.py")
    generatedFile.writeText("P = 1\n")
    VirtualFileManager.getInstance().refreshAndFindFileByNioPath(generatedFile)

    val target = testPythonTarget("@@server//genpy:part") { srcs("part.py", generated = true) }

    runPythonImporter(
      generateWorkspaceSnapshot(listOf(target)),
      MutableEntityStorage.create(),
      runPostProcessing = true,
      execRoot = execRoot,
    )

    val resolved =
      runReadActionBlocking {
        val context = PyQualifiedNameResolveContextImpl(PsiManager.getInstance(testBazel.project), null, null, null)
        BazelPyImportResolver().resolveImportReference(QualifiedName.fromComponents("part"), context, false)
      }
    resolved.shouldNotBeNull()
    resolved.shouldBeInstanceOf<PyFile>()
    resolved.virtualFile.name shouldBe "part.py"
  }

  @ParameterizedTest
  @CsvSource(
    "common, false, enabled",
    "common, true, enabled",
    "python, false, enabled",
    "python, true, enabled",
    "directory, true, enabled",
    "both, true, enabled",
    "common, true, disabled",
    "python, true, disabled",
    "common, true, partial",
    "python, true, partial",
  )
  fun `generated roots and resolve index use the same paths`(sourceCollection: String, explicitImports: Boolean, mode: String) {
    val info = testBazel.bazelInfo
    val generatedDir = info.bazelBin.resolve("generated").createDirectories()
    val first = generatedDir.resolve("aaa.py").also { it.writeText("A = 1\n") }
    val second = generatedDir.resolve("bbb.pyi").also { it.writeText("B: int\n") }
    val sources = listOf(first, second)
    val sourcePaths = listOf("generated/aaa.py", "generated/bbb.pyi")
    val sourceLocations = sourcePaths.map { generatedTestLocation(it) }
    VirtualFileManager.getInstance().refreshAndFindFileByNioPath(generatedDir)?.refresh(false, true)
    val links = DefaultBazelOutputFileHardLinks(testBazel.project, info)
    val requests = Collections.synchronizedList(mutableListOf<Path>())
    val hardLinks = object : BazelOutFileHardLinks by links {
      override suspend fun createOutputFileHardLink(originalFile: Path): Path? {
        requests.add(originalFile)
        return when {
          mode == "disabled" || mode == "partial" && originalFile == second -> originalFile
          else -> links.createOutputFileHardLink(originalFile)
        }
      }

      override fun resolveCachedPath(fileOrDir: Path): Path =
        if (mode == "disabled") fileOrDir else links.resolveCachedPath(fileOrDir)
    }
    val server = object : BuildServerMock() {
      override val bazelInfo = info
      override val outFileHardLinks = hardLinks
    }
    val target = testPythonTarget(
      "@@server//:generated",
      imports = if (explicitImports) listOf("generated") else emptyList(),
      generatedSources = when (sourceCollection) {
        "common" -> emptyList()
        "directory" -> listOf(generatedTestLocation("generated"))
        else -> sourceLocations
      },
    ) {
      if (sourceCollection == "common" || sourceCollection == "both") srcs(*sourcePaths.toTypedArray(), generated = true)
    }
    links.onBeforeSync()
    val mappedTarget = try {
      mapGeneratedTarget(target, server)
    }
    finally {
      runBlocking { links.onAfterSync(false) }
    }
    requests.map { it.toString() }.toSet() shouldBe sources.map { it.toString() }.toSet()
    requests.size shouldBe sources.size
    mappedTarget.data.filterIsInstance<PythonBuildTarget>().single().generatedSourceOverrides shouldBe
      if (mode == "partial") mapOf(sourceLocations[1] to OutputLocation.Host(second.toString())) else emptyMap()
    val snapshot = generateWorkspaceSnapshot(listOf(mappedTarget))
    val effectivePaths = sources.map { path ->
      if (mode == "disabled" || mode == "partial" && path == second) path else links.resolveCachedPath(path)
    }

    repeat(2) {
      requests.clear()
      val diff = MutableEntityStorage.create()
      links.onBeforeSync()
      try {
        runPythonImporter(snapshot, diff, runPostProcessing = true, buildServerMock = server)
      }
      finally {
        runBlocking { links.onAfterSync(false) }
      }

      requests shouldBe emptyList()
      val expectedRoots = when {
        !explicitImports -> effectivePaths.toSet()
        mode == "disabled" -> setOf(generatedDir)
        mode == "partial" -> setOf(links.resolveCachedPath(generatedDir), second)
        else -> setOf(links.resolveCachedPath(generatedDir))
      }
      diff.entities(SourceRootEntity::class.java).map { it.url }.toSet() shouldBe
        expectedRoots.map { it.toVirtualFileUrl(testBazel.virtualFileUrlManager) }.toSet()

      runReadActionBlocking {
        val context = PyQualifiedNameResolveContextImpl(PsiManager.getInstance(testBazel.project), null, null, null)
        for ((name, expectedPath) in listOf("aaa", "bbb").zip(effectivePaths)) {
          val qualifiedName = if (explicitImports) name else "generated.$name"
          val resolved = BazelPyImportResolver().resolveImportReference(QualifiedName.fromDottedString(qualifiedName), context, false)
          resolved.shouldNotBeNull()
          resolved.shouldBeInstanceOf<PyFile>()
          resolved.virtualFile.toNioPath().toString() shouldBe expectedPath.toString()
          testBazel.project.findBazelPythonShortestQualifiedName(expectedPath) shouldBe qualifiedName
        }
      }
    }
  }

  @Test
  fun `generated sources use the current output resolver`() {
    val info = testBazel.bazelInfo
    val generatedDir = info.bazelBin.resolve("generated").createDirectories()
    val source = generatedDir.resolve("aaa.py").also { it.writeText("A = 1\n") }
    val initialLinks = CopyingBazelOutHardLinks(info.outputBase, testBazel.outputBase.resolve("initial-cache"))
    val initialServer = object : BuildServerMock() {
      override val bazelInfo = info
      override val outFileHardLinks = initialLinks
    }
    val target = testPythonTarget(
      "@@server//:generated",
      imports = listOf("generated"),
      generatedSources = listOf(generatedTestLocation("generated/aaa.py")),
    )
    val snapshot = generateWorkspaceSnapshot(listOf(mapGeneratedTarget(target, initialServer)))
    val currentLinks = CopyingBazelOutHardLinks(info.outputBase, testBazel.outputBase.resolve("current-cache"))
    timeoutRunBlocking { currentLinks.createOutputFileHardLink(source) }
    currentLinks.linkedFiles.clear()
    val currentServer = object : BuildServerMock() {
      override val bazelInfo = info
      override val outFileHardLinks = currentLinks
    }
    val diff = MutableEntityStorage.create()

    runPythonImporter(snapshot, diff, runPostProcessing = true, buildServerMock = currentServer)

    currentLinks.linkedFiles shouldBe emptyList()
    diff.entities(SourceRootEntity::class.java).map { it.url }.toSet() shouldBe
      setOf(currentLinks.resolveCachedPath(generatedDir).toVirtualFileUrl(testBazel.virtualFileUrlManager))
    val expectedPath = currentLinks.resolveCachedPath(source)
    VirtualFileManager.getInstance().refreshAndFindFileByNioPath(expectedPath).shouldNotBeNull()
    runReadActionBlocking {
      val context = PyQualifiedNameResolveContextImpl(PsiManager.getInstance(testBazel.project), null, null, null)
      val resolved = BazelPyImportResolver().resolveImportReference(QualifiedName.fromComponents("aaa"), context, false)
      resolved.shouldBeInstanceOf<PyFile>()
      resolved.virtualFile.toNioPath() shouldBe expectedPath
    }
  }

  @ParameterizedTest
  @CsvSource("common, false", "common, true", "python, false", "python, true")
  fun `partial sync replaces a shared generated source fallback`(sourceCollection: String, reverseTargets: Boolean): Unit = timeoutRunBlocking {
    val info = testBazel.bazelInfo
    val source = info.bazelBin.resolve("generated/shared.py")
    val location = generatedTestLocation("generated/shared.py")
    val links = DefaultBazelOutputFileHardLinks(testBazel.project, info)
    val server = object : BuildServerMock() {
      override val bazelInfo = info
      override val outFileHardLinks = links
    }
    val targets = listOf("first", "second").map { name ->
      testPythonTarget("@@server//:$name", generatedSources = if (sourceCollection == "python") listOf(location) else emptyList()) {
        if (sourceCollection == "common") srcs("generated/shared.py", generated = true)
      }
    }
    links.onBeforeSync()
    val previousTargets = try {
      targets.map { mapGeneratedTarget(it, server) }
    }
    finally {
      links.onAfterSync(false)
    }

    source.parent.createDirectories()
    source.writeText("VALUE = 42\n")
    links.onBeforeSync()
    try {
      val updatedTarget = mapGeneratedTarget(targets.first(), server)
      val mergedTargets = listOf(updatedTarget, previousTargets.last()).let { if (reverseTargets) it.reversed() else it }
      source.deleteExisting()
      links.onBeforeProjectModelUpdate()
      val snapshot = generateWorkspaceSnapshot(mergedTargets)
      repeat(2) {
        val diff = MutableEntityStorage.create()
        runPythonImporter(snapshot, diff, runPostProcessing = true, buildServerMock = server)

        val cachedSource = links.resolveCachedPath(source)
        diff.entities(SourceRootEntity::class.java).map { it.url }.toList() shouldBe
          List(2) { cachedSource.toVirtualFileUrl(testBazel.virtualFileUrlManager) }
        runReadActionBlocking {
          val context = PyQualifiedNameResolveContextImpl(PsiManager.getInstance(testBazel.project), null, null, null)
          val resolved = BazelPyImportResolver().resolveImportReference(QualifiedName.fromDottedString("generated.shared"), context, false)
          resolved.shouldBeInstanceOf<PyFile>()
          resolved.virtualFile.toNioPath() shouldBe cachedSource
        }
        links.onAfterSync(false)
        links.onBeforeSync()
      }
    }
    finally {
      links.onAfterSync(false)
    }
  }

  @Test
  fun `shared source fallback does not select an outdated hardlink`(): Unit = timeoutRunBlocking {
    val info = testBazelInfo(workspaceRoot = testBazel.workspaceRoot, outputBase = testBazel.outputBase)
    val source = info.execRoot.resolve("bazel-out/${TEST_OUTPUT_ROOT.path}/generated/shared.py")
    source.parent.createDirectories()
    source.writeText("VALUE = 1\n")
    val links = DefaultBazelOutputFileHardLinks(testBazel.project, info)
    val server = object : BuildServerMock() {
      override val bazelInfo = info
      override val outFileHardLinks = links
    }
    val targets = listOf("first", "second").map { name ->
      testPythonTarget("@@server//:$name") { srcs("generated/shared.py", generated = true) }
    }
    links.onBeforeSync()
    val previousTarget = try {
      mapGeneratedTarget(targets.first(), server)
    }
    finally {
      links.onAfterSync(false)
    }
    source.deleteExisting()
    source.writeText("VALUE = 2\n")
    val failedServer = object : BuildServerMock() {
      override val bazelInfo = info
      override val outFileHardLinks = object : BazelOutFileHardLinks by BazelOutFileHardLinks.NONE {
        override fun resolveCachedPath(fileOrDir: Path): Path = links.resolveCachedPath(fileOrDir)
      }
    }
    val updatedTarget = mapGeneratedTarget(targets.last(), failedServer)
    val diff = MutableEntityStorage.create()

    runPythonImporter(generateWorkspaceSnapshot(listOf(previousTarget, updatedTarget)), diff, buildServerMock = failedServer)

    diff.entities(SourceRootEntity::class.java).map { it.url }.toList() shouldBe
      List(2) { source.toVirtualFileUrl(testBazel.virtualFileUrlManager) }
  }

  @Test
  fun `generated source fallback does not redirect parent directories`() {
    val info = testBazel.bazelInfo
    val importPath = List(20) { "nested" }.joinToString("/")
    val generatedDir = info.bazelBin.resolve(importPath).createDirectories()
    generatedDir.resolve("alias.py").writeText("VALUE = 42\n")
    val fallback = testBazel.outputBase.resolve("fallback.py").also { it.writeText("VALUE = 42\n") }
    val links = object : BazelOutFileHardLinks by BazelOutFileHardLinks.NONE {
      override suspend fun createOutputFileHardLink(originalFile: Path): Path = fallback
      override fun resolveCachedPath(fileOrDir: Path): Path =
        testBazel.outputBase.resolve("cache").resolve(info.outputBase.relativize(fileOrDir))
    }
    val server = object : BuildServerMock() {
      override val bazelInfo = info
      override val outFileHardLinks = links
    }
    val target = testPythonTarget("@@server//:generated", imports = listOf(importPath)) { srcs("$importPath/alias.py", generated = true) }
    val diff = MutableEntityStorage.create()

    runPythonImporter(generateWorkspaceSnapshot(listOf(mapGeneratedTarget(target, server))), diff, buildServerMock = server)

    diff.entities(SourceRootEntity::class.java).map { it.url }.toSet() shouldBe
      setOf(generatedDir, fallback).map { it.toVirtualFileUrl(testBazel.virtualFileUrlManager) }.toSet()
  }

  @ParameterizedTest
  @ValueSource(booleans = [false, true])
  fun `generated import roots use current sync paths`(hasPreviousSync: Boolean) {
    val info = testProjectBazelInfo(workspaceRoot = testBazel.workspaceRoot, outputBase = testBazel.outputBase.resolve("current"))
    val generatedDir = info.bazelBin.resolve("generated")
    generatedDir.resolve("pkg").createDirectories().resolve("aaa.py").writeText("A = 1\n")
    testBazel.bazelInfo.bazelBin.resolve("generated").createDirectories()
    if (hasPreviousSync) {
      testBazel.sync()
    }
    else {
      testBazel.clearSnapshot()
    }
    val links = DefaultBazelOutputFileHardLinks(testBazel.project, info)
    val server = object : BuildServerMock() {
      override val bazelInfo = info
      override val outFileHardLinks = links
    }
    val target = testPythonTarget("@@server//:generated", imports = listOf("generated")) {
      srcs("generated/pkg/aaa.py", generated = true)
    }
    val diff = MutableEntityStorage.create()
    links.onBeforeSync()
    try {
      val mappedTarget = mapGeneratedTarget(target, server)
      runPythonImporter(generateWorkspaceSnapshot(listOf(mappedTarget)), diff, buildServerMock = server)
    }
    finally {
      runBlocking { links.onAfterSync(false) }
    }

    diff.entities(SourceRootEntity::class.java).map { it.url }.toSet() shouldBe
      setOf(links.resolveCachedPath(generatedDir).toVirtualFileUrl(testBazel.virtualFileUrlManager))
  }

  @Test
  fun `should index generated Python source delivered via PythonBuildTarget generatedSources`() {
    val execRoot = testBazel.workspaceRoot
    val bazelBin = execRoot.resolve("bazel-out/k8-fastbuild/bin")
    bazelBin.resolve("genpy").createDirectories().resolve("part_pb2.py").writeText("P = 1\n")
    val hardLinksRoot = execRoot.resolve("intellij-hardlinks")
    val hardLinks = CopyingBazelOutHardLinks(outputBase = execRoot, hardLinksRoot = hardLinksRoot)

    val target = testPythonTarget("@@server//genpy:part", generatedSources = listOf(generatedTestLocation("genpy/part_pb2.py")))

    val server = object : BuildServerMock() {
      override val bazelInfo: BazelInfo = bazelInfo()
      override val outFileHardLinks: BazelOutFileHardLinks = hardLinks
    }
    val mappedTarget = mapGeneratedTarget(target, server)
    runPythonImporter(
      generateWorkspaceSnapshot(listOf(mappedTarget)),
      MutableEntityStorage.create(),
      runPostProcessing = true,
      buildServerMock = server,
    )

    val expectedHardLink = hardLinksRoot.resolve("bazel-out/k8-fastbuild/bin/genpy/part_pb2.py")
    hardLinks.linkedFiles.toSet() shouldBe setOf(expectedHardLink)
    VirtualFileManager.getInstance().refreshAndFindFileByNioPath(expectedHardLink).shouldNotBeNull()
    val resolved =
      runReadActionBlocking {
        val context = PyQualifiedNameResolveContextImpl(PsiManager.getInstance(testBazel.project), null, null, null)
        BazelPyImportResolver().resolveImportReference(QualifiedName.fromComponents("genpy", "part_pb2"), context, false)
      }
    resolved.shouldBeInstanceOf<PyFile>()
    resolved.virtualFile.toNioPath() shouldBe expectedHardLink
  }

  @Test
  fun `should create a library for external pip dependency`() {
    // Mimics `//project:lib` depending on `@pypi//aaa`: the pip target is a non-root dependency whose sources
    // live under an external `site-packages` directory (reported via PythonBuildTarget.externalSources).
    val sitePackages = testBazel.workspaceRoot.resolve("external/pypi_312_aaa/site-packages")
    createPythonFile("external/pypi_312_aaa/site-packages/aaa/__init__.py", "from aaa.bbb import Ccc\n")
    createPythonFile("external/pypi_312_aaa/site-packages/aaa/bbb.py", "class Ccc:\n    pass\n")

    val externalPackageTarget = testPythonTarget(
      "@@rules_python++pip+pypi_312_aaa//:pkg",
      externalSources = listOf(OutputLocation.External("pypi_312_aaa", "site-packages")),
    )
    val libTarget = testPythonTarget("@@server//project:lib") {
      deps("@@rules_python++pip+pypi_312_aaa//:pkg")
      srcs("project/lib/main_dependency.py")
    }

    // when: `lib` is the only root target, `aaa` is a (non-root) dependency
    val diff = MutableEntityStorage.create()
    runPythonImporter(
      generateWorkspaceSnapshot(targets = listOf(libTarget, externalPackageTarget), rootTargets = setOf(libTarget.key)),
      diff,
    )

    // then: a project library with a SOURCES root at `site-packages` is created and attached to `lib`'s module
    val sitePackagesUrl = sitePackages.toVirtualFileUrl(testBazel.virtualFileUrlManager)
    diff.entities(LibraryEntity::class.java).toList() shouldExist { library ->
      library.roots.any { it.type == LibraryRootTypeId.SOURCES && it.url == sitePackagesUrl }
    }
    diff.entities(ModuleEntity::class.java).toList() shouldExist { module ->
      module.dependencies.any { it is LibraryDependency }
    }
  }

  @Test
  fun `should create external pip dependency library when import depth is limited`() {
    val sitePackages = testBazel.workspaceRoot.resolve("external/pypi_312_aaa/site-packages")

    val externalPackageTarget = testPythonTarget(
      "@@rules_python++pip+pypi_312_aaa//:pkg",
      externalSources = listOf(OutputLocation.External("pypi_312_aaa", "site-packages")),
    )
    val libTarget = testPythonTarget("@@server//project:lib") {
      deps("@@rules_python++pip+pypi_312_aaa//:pkg")
      srcs("project/lib/main.py")
    }
    val appTarget = testPythonTarget("@@server//project:app") {
      deps("@@server//project:lib")
      srcs("project/app/main.py")
    }

    val diff = MutableEntityStorage.create()
    runPythonImporter(
      generateWorkspaceSnapshot(
        targets = listOf(appTarget, libTarget, externalPackageTarget),
        rootTargets = setOf(appTarget.key),
        projectView = ProjectView(mapOf(IMPORT_DEPTH_KEY to 1), emptyList()),
      ),
      diff,
    )

    val sitePackagesUrl = sitePackages.toVirtualFileUrl(testBazel.virtualFileUrlManager)
    diff.entities(LibraryEntity::class.java).toList() shouldExist { library ->
      library.roots.any { it.type == LibraryRootTypeId.SOURCES && it.url == sitePackagesUrl }
    }
  }

  @Test
  fun `should create one library per external pip dependency`() {
    val aaaSitePackages = testBazel.workspaceRoot.resolve("external/pypi_312_aaa/site-packages")
    val bbbSitePackages = testBazel.workspaceRoot.resolve("external/pypi_312_bbb/site-packages")
    createPythonFile("external/pypi_312_aaa/site-packages/aaa/__init__.py", "")
    createPythonFile("external/pypi_312_bbb/site-packages/bbb/__init__.py", "")

    val aaaPackageTarget = testPythonTarget(
      "@@rules_python++pip+pypi_312_aaa//:pkg",
      externalSources = listOf(OutputLocation.External("pypi_312_aaa", "site-packages")),
    )
    val bbbPackageTarget = testPythonTarget(
      "@@rules_python++pip+pypi_312_bbb//:pkg",
      externalSources = listOf(OutputLocation.External("pypi_312_bbb", "site-packages")),
    )
    val libTarget = testPythonTarget("@@server//project:lib") {
      deps("@@rules_python++pip+pypi_312_aaa//:pkg", "@@rules_python++pip+pypi_312_bbb//:pkg")
      srcs("project/lib/main.py")
    }
    val appTarget = testPythonTarget("@@server//project:app") {
      deps("@@rules_python++pip+pypi_312_aaa//:pkg")
      srcs("project/app/main.py")
    }

    val diff = MutableEntityStorage.create()
    runPythonImporter(
      generateWorkspaceSnapshot(
        targets = listOf(libTarget, appTarget, aaaPackageTarget, bbbPackageTarget),
        rootTargets = setOf(libTarget.key, appTarget.key),
      ),
      diff,
    )

    val libraries = diff.entities(LibraryEntity::class.java).toList()
    libraries.size shouldBe 2
    libraries.map { it.name }.toSet() shouldBe setOf(
      aaaPackageTarget.key.formatAsLibraryName(RepoMappingDisabled, withFullKey = true),
      bbbPackageTarget.key.formatAsLibraryName(RepoMappingDisabled, withFullKey = true),
    )

    val aaaSitePackagesUrl = aaaSitePackages.toVirtualFileUrl(testBazel.virtualFileUrlManager)
    val bbbSitePackagesUrl = bbbSitePackages.toVirtualFileUrl(testBazel.virtualFileUrlManager)
    val aaaLibrary = libraries.single { library ->
      library.roots.any { it.type == LibraryRootTypeId.SOURCES && it.url == aaaSitePackagesUrl }
    }
    val bbbLibrary = libraries.single { library ->
      library.roots.any { it.type == LibraryRootTypeId.SOURCES && it.url == bbbSitePackagesUrl }
    }
    val modules = diff.entities(ModuleEntity::class.java).toList()
    val libModule = modules.single { it.name == libTarget.key.label.formatAsModuleName(testBazel.project) }
    val appModule = modules.single { it.name == appTarget.key.label.formatAsModuleName(testBazel.project) }

    libModule.dependencies.filterIsInstance<LibraryDependency>().map { it.library }.toSet() shouldBe setOf(
      aaaLibrary.symbolicId,
      bbbLibrary.symbolicId,
    )
    appModule.dependencies.filterIsInstance<LibraryDependency>().map { it.library }.toSet() shouldBe setOf(aaaLibrary.symbolicId)
  }

  private fun renderModuleLocationText(element: Any): String =
    ModuleRendererFactory.findInstance(element).getModuleTextWithIcon(element)!!.text

  private fun mapGeneratedTarget(target: TestBuildTarget, server: BuildServerMock): TestBuildTarget = timeoutRunBlocking {
    val pythonTarget = target.data.filterIsInstance<PythonBuildTarget>().single()
    fun artifact(location: OutputLocation): ArtifactLocation = ArtifactLocation.newBuilder()
      .setRelativePath(location.toExecrootPath())
      .setIsSource(false)
      .build()
    val targetInfo = IntellijIdeInfo.TargetIdeInfo.newBuilder()
      .setKey(IntellijIdeInfo.TargetKey.newBuilder().setLabel(target.key.label.toString()))
      .addAllSrcs(target.sources.getOutputLocations().map { artifact(it) }.toList())
      .setPythonTargetInfo(
        IntellijIdeInfo.PythonTargetInfo.newBuilder()
          .addAllGeneratedSources(pythonTarget.generatedSources.getOutputLocations().map { artifact(it) }.toList()),
      )
      .build()
    val mapped = PythonLanguagePlugin().mapBuildTargetData(server, targetInfo).filterIsInstance<PythonBuildTarget>().single()
    val data = pythonTarget.copy(
      generatedSources = mapped.generatedSources,
      generatedSourceOverrides = mapped.generatedSourceOverrides,
    )
    target.copy(data = listOf(data))
  }

  private fun runPythonImporter(
    snapshot: WorkspaceSnapshot,
    builder: MutableEntityStorage,
    runPostProcessing: Boolean = false,
    execRoot: Path = testBazel.workspaceRoot,
    buildServerMock: BuildServerMock = object : BuildServerMock() {
      override val bazelInfo: BazelInfo = bazelInfo(execRoot)
    },
  ) = runBlocking {
    testBazel.buildServer(buildServerMock)
    reportSequentialProgress { reporter ->
      val helper = WorkspaceImporterHelper(
        project = testBazel.project,
        taskConsole = testBazel.project.syncConsole,
        progressReporter = reporter,
        builder = builder,
        outputResolver = buildServerMock.outputResolver,
        outputParser = buildServerMock.outputParser,
        outFileHardLinks = buildServerMock.outFileHardLinks,
        bazelInfo = buildServerMock.bazelInfo,
      )
      helper.invoke(reporter, snapshot, TaskGroupId.EMPTY.task("test"))
      if (runPostProcessing) {
        helper.invokeLate(reporter, snapshot, TaskGroupId.EMPTY.task("test"))
      }
    }
  }

  private fun generateTestSet(): PythonTestSet {
    val pythonLibrary1 = testPythonTarget("@@server.lib//:lib1")
    val pythonLibrary2 = testPythonTarget("@@server//lib:lib2")
    val mainApp = testPythonTarget("@@server//:main_app") { deps("@@server.lib//:lib1", "@@server//lib:lib2") }

    val targets = listOf(pythonLibrary1, pythonLibrary2, mainApp)

    val expectedModuleEntity1 = generateExpectedModuleEntity(mainApp, listOf(pythonLibrary1, pythonLibrary2))
    val expectedModuleEntity2 = generateExpectedModuleEntity(pythonLibrary1, emptyList())
    val expectedModuleEntity3 = generateExpectedModuleEntity(pythonLibrary2, emptyList())
    return PythonTestSet(
      generateWorkspaceSnapshot(targets),
      listOf(expectedModuleEntity1, expectedModuleEntity2, expectedModuleEntity3),
      emptyList(),
    )
  }

  private fun generateTestSetWithSources(): PythonTestSet {
    val target = testPythonTarget("@@server//:main_app") {
      srcs("SomeSourceItemFile", generated = true)
      resources("Resource1", "Resource2", "Resource3")
    }

    val expectedModuleEntity = generateExpectedModuleEntity(target, emptyList())

    val expectedContentRootEntities = generateExpectedSourceRootEntities(target, expectedModuleEntity.moduleEntity)
    return PythonTestSet(
      generateWorkspaceSnapshot(listOf(target)),
      listOf(expectedModuleEntity),
      expectedContentRootEntities,
    )
  }

  /** Declares a `python_binary` target that uses the Python interpreter of the test. [declare] adds the sources and the dependencies. */
  private fun testPythonTarget(
    label: String,
    imports: List<String> = emptyList(),
    generatedSources: List<OutputLocation> = emptyList(),
    externalSources: List<OutputLocation> = emptyList(),
    declare: TestTargetBuilder.() -> Unit = {},
  ): TestBuildTarget =
    pythonTarget(label, kind = "python_binary") {
      data(
        PythonBuildTarget(
          version = "3",
          interpreter = OutputLocation.Host(pythonBinary.toString()),
          imports = StringsCollection.of(imports),
          generatedSources = OutputLocationCollectionBuilder.ofLocations(generatedSources),
          externalSources = OutputLocationCollectionBuilder.ofLocations(externalSources),
        ),
      )
      declare()
    }

  private fun generateWorkspaceSnapshot(
    targets: List<BuildTarget>,
    rootTargets: Set<WorkspaceTargetKey> = targets.map { it.key }.toSet(),
    projectView: ProjectView = ProjectView.EMPTY,
  ): WorkspaceSnapshot = runBlocking {
    WorkspaceSnapshotBuilder.build(
      project = testBazel.project,
      projectView = projectView,
      repoMapping = RepoMappingDisabled,
      bazelInfo = bazelInfo(),
      resolved = BazelResolvedWorkspace(
        workspaceName = null,
        repoMapping = RepoMappingDisabled,
        rootTargets = rootTargets,
        targets = targets,
        configurations = emptyMap(),
      ),
    )
  }

  private fun generateExpectedModuleEntity(target: BuildTarget, dependencies: List<BuildTarget>): ExpectedModuleEntity {
    val sdkName = chooseSdkName(pythonBinary, testBazel.project.name)
    val sdkDependency: ModuleDependencyItem = SdkDependency(SdkId(sdkName, "Python SDK"))
    val moduleDependencies: List<ModuleDependencyItem> =
      dependencies.map {
        ModuleDependency(
          module = ModuleId(it.key.label.formatAsModuleName(testBazel.project)),
          exported = true,
          scope = DependencyScope.COMPILE,
          productionOnTest = true,
        )
      }
    return ExpectedModuleEntity(
      moduleEntity =
        ModuleEntity(
          name = target.key.label.formatAsModuleName(testBazel.project),
          entitySource = BazelProjectEntitySource,
          dependencies = moduleDependencies + ModuleSourceDependency + sdkDependency,
        ) {
          type = ModuleTypeId(PYTHON_MODULE_ID)
        },
    )
  }

  private fun generateExpectedSourceRootEntities(target: BuildTarget, parentModuleEntity: ModuleEntity): List<ExpectedSourceRootEntity> =
    (target.sources.getOutputLocations().mapNotNull { outputResolver().resolve(it) }
       .map { generateExpectedSourceRootEntity(it, "python-source", parentModuleEntity) } +
     target.resources.getOutputLocations().mapNotNull { outputResolver().resolve(it) }
       .map { generateExpectedSourceRootEntity(it, "python-resource", parentModuleEntity) }).toList()

  private fun bazelInfo(execRoot: Path = testBazel.workspaceRoot): BazelInfo =
    testBazelInfo(workspaceRoot = testBazel.workspaceRoot, outputBase = testBazel.workspaceRoot, execRoot = execRoot)

  private fun outputResolver(execRoot: Path = testBazel.workspaceRoot): OutputLocationResolver =
    DefaultOutputLocationResolver.createHardlinkResolving(bazelInfo(execRoot))

  private fun generateExpectedSourceRootEntity(path: Path, rootType: String, parentModuleEntity: ModuleEntity): ExpectedSourceRootEntity {
    val url = path.toVirtualFileUrl(testBazel.virtualFileUrlManager)
    val sourceRootEntity = SourceRootEntity(url, SourceRootTypeId(rootType), parentModuleEntity.entitySource)
    val contentRootEntity =
      ContentRootEntity(url, emptyList(), parentModuleEntity.entitySource) {
        excludedUrls = emptyList()
        sourceRoots = listOf(sourceRootEntity)
      }
    return ExpectedSourceRootEntity(sourceRootEntity, contentRootEntity, parentModuleEntity)
  }

  private fun List<ModuleEntity>.shouldAllHaveTheSameSDK() {
    val sdks = this.map { module -> module.dependencies.firstNotNullOfOrNull { it as? SdkDependency } }
    sdks.any { it == null }.shouldBeFalse()
    sdks.distinct().size shouldBe 1
  }

  private fun createPythonFile(relativePath: String, text: String): PyFile {
    val path = testBazel.workspaceRoot.resolve(relativePath)
    path.parent.createDirectories()
    path.writeText(text)
    val virtualFile = VirtualFileManager.getInstance().refreshAndFindFileByNioPath(path)!!
    return runReadActionBlocking {
      PsiManager.getInstance(testBazel.project).findFile(virtualFile) as PyFile
    }
  }
}
