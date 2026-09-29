package org.jetbrains.bazel.flow.vcs

import com.intellij.openapi.application.edtWriteAction
import com.intellij.openapi.application.readAction
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import com.intellij.openapi.vcs.VcsDirectoryMapping
import com.intellij.openapi.vcs.VcsRootChecker
import com.intellij.openapi.vcs.roots.VcsRootDetector
import com.intellij.openapi.vcs.roots.VcsRootErrorsFinder
import com.intellij.openapi.vcs.roots.VcsRootErrorsHandler
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.workspace.jps.entities.ContentRootEntity
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.storage.EntitySource
import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import com.intellij.testFramework.common.timeoutRunBlocking
import com.intellij.testFramework.junit5.TestApplication
import com.intellij.testFramework.junit5.fixture.projectFixture
import com.intellij.testFramework.junit5.fixture.tempPathFixture
import com.intellij.testFramework.refreshVfs
import com.intellij.workspaceModel.ide.registerProjectRoot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.flow.exclude.BazelSymlinkExcludeService
import org.jetbrains.bazel.project.BazelProjectFixtures.initializeBazelProject
import org.jetbrains.bazel.sync.environment.projectCtx
import org.jetbrains.bazel.symlinks.createBazelConvenienceSymlink
import org.jetbrains.bazel.symlinks.createSymlinkOrJunction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.name
import kotlin.io.path.writeText

/**
 * [BAZEL-948](https://youtrack.jetbrains.com/issue/BAZEL-948): the Bazel plugin must keep VCS roots out of the Bazel output directories
 * (under the Bazel symlinks and in the execution root), and must not touch any other VCS root, which the platform takes care of.
 *
 * Bazel's symlink forest plants a `.git` symlink into `execroot/_main`, so the execution root looks like a Git repository.
 * The detection tests run the same pipeline as `VcsRootScanner`: detect the roots, then auto-register the unregistered ones.
 */
@TestApplication
class BazelVcsRootErrorFilterTest {
  private val tempDirFixture = tempPathFixture()
  private val tempDir by tempDirFixture

  private val projectFixture = projectFixture(pathFixture = tempDirFixture)
  private val project by projectFixture

  private val outputBase by tempPathFixture()

  private lateinit var workspaceDir: Path
  private lateinit var execRoot: Path
  private lateinit var bazelBinPackage: Path
  private lateinit var workspaceSymlink: Path

  private lateinit var nestedRepository: Path
  private lateinit var execrootNamedRepository: Path
  private lateinit var externalRepository: Path
  private lateinit var linkedRepository: Path
  // Outside the project and named like an execution root, but no Bazel symlink points to it
  private lateinit var unrelatedExecrootRepository: Path

  @BeforeEach
  fun setUp(): Unit = timeoutRunBlocking {
    // Lay out the files on disk before VFS sees the workspace, as on a real startup:
    // otherwise BazelSymlinkExcludeFileListener excludes the symlink on its creation event.
    workspaceDir = tempDir.resolve("workspace").createDirectories()
    workspaceDir.resolve("MODULE.bazel").writeText("")
    workspaceDir.createRepository()

    execRoot = outputBase.resolve("execroot/_main").createDirectories()
    execRoot.resolve(".git").createSymlinkOrJunction(workspaceDir.resolve(".git"))
    bazelBinPackage = execRoot.resolve("bazel-out/k8-fastbuild/bin/pkg").createDirectories()
    workspaceSymlink = workspaceDir.createBazelConvenienceSymlink("bazel-${workspaceDir.name}", execRoot)

    nestedRepository = workspaceDir.resolve("nested").createRepository()
    execrootNamedRepository = workspaceDir.resolve("tools/execroot/repo").createRepository()
    externalRepository = tempDir.resolve("elsewhere/repo").createRepository()
    linkedRepository = workspaceDir.resolve("linked").createSymlinkOrJunction(externalRepository)
    unrelatedExecrootRepository = tempDir.resolve("unrelated/execroot/_main").createRepository()

    initializeBazelProject(project, workspaceDir)
    outputBase.refreshVfs()
    // The only project root known before the first sync (see OpenBazelProjectAndSyncStartupActivity)
    registerProjectRoot(project, workspaceDir)

    assertNotNull(ProjectLevelVcsManager.getInstance(project).findVcsByName("Git")) { "Git VCS is not registered" }
    assertTrue(VcsRootChecker.EXTENSION_POINT_NAME.hasAnyExtensions()) { "No VcsRootChecker is registered" }
  }

  @Test
  fun `execroot is not registered as a VCS root when a content root lies inside it`(): Unit = timeoutRunBlocking {
    // GIVEN the symlink is excluded, and there is a content root under bazel-bin, like a Python import root
    excludeWorkspaceSymlink()
    addContentRoot(bazelBinPackage)

    // WHEN
    val mappings = detectAndRegisterVcsRoots()

    // THEN
    assertNoBazelOutputMappings(mappings)
  }

  @Test
  fun `bazel symlink is not registered as a VCS root before the exclude scan finishes`(): Unit = timeoutRunBlocking {
    // GIVEN BazelSymlinkExcludeStartupActivity hasn't filled the exclude set yet

    // WHEN
    val mappings = detectAndRegisterVcsRoots()

    // THEN
    assertNoBazelOutputMappings(mappings)
  }

  @Test
  fun `excluded bazel symlink is not registered as a VCS root`(): Unit = timeoutRunBlocking {
    // GIVEN
    excludeWorkspaceSymlink()

    // WHEN
    val mappings = detectAndRegisterVcsRoots()

    // THEN
    assertNoBazelOutputMappings(mappings)
  }

  @Test
  fun `regular repositories are registered as VCS roots as without Bazel`(): Unit = timeoutRunBlocking {
    // GIVEN the state after a sync, when the execution root is known
    project.projectCtx.bazelExecPath = execRoot

    // WHEN
    val mappings = detectAndRegisterVcsRoots().map { Path.of(it) }

    // THEN
    for (repository in regularRepositories()) {
      assertTrue(repository in mappings) { "$repository is not registered as a VCS root, all mappings: $mappings" }
    }
  }

  @Test
  fun `startup removes only the persisted VCS mappings of Bazel output directories`(): Unit = timeoutRunBlocking {
    // GIVEN the state after a sync, and the mappings an earlier session persisted in vcs.xml
    project.projectCtx.bazelExecPath = execRoot
    val regularMappings = setMappingsOfRegularRepositoriesAndBazelOutputs()

    // WHEN
    BazelVcsRootErrorFilter.removeBazelOutputMappings(project)

    // THEN
    assertEquals(regularMappings, ProjectLevelVcsManager.getInstance(project).getDirectoryMappings().toSet())
  }

  /**
   * Repositories the platform detects regardless of Bazel: none of them lies in a Bazel output tree.
   */
  private fun regularRepositories(): List<Path> = listOf(workspaceDir, nestedRepository, execrootNamedRepository, linkedRepository)

  /** @return the mappings of the regular repositories, including the ones outside the project */
  private fun setMappingsOfRegularRepositoriesAndBazelOutputs(): Set<VcsDirectoryMapping> {
    val regularMappings = (regularRepositories() + listOf(externalRepository, unrelatedExecrootRepository))
      .map { VcsDirectoryMapping(it.invariantSeparatorsPathString, "Git") }
    val bazelOutputMappings = listOf(execRoot, workspaceSymlink).map { VcsDirectoryMapping(it.invariantSeparatorsPathString, "Git") }
    val manager = ProjectLevelVcsManager.getInstance(project)
    manager.setDirectoryMappings(regularMappings + bazelOutputMappings)
    assertEquals((regularMappings + bazelOutputMappings).toSet(), manager.getDirectoryMappings().toSet()) { "The mappings are not set up" }
    return regularMappings.toSet()
  }

  private suspend fun excludeWorkspaceSymlink() {
    edtWriteAction {
      BazelSymlinkExcludeService.getInstance(project).addBazelSymlinksToExclude(setOf(workspaceSymlink))
    }
    val symlinkFile = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(workspaceSymlink)!!
    assertTrue(readAction { ProjectFileIndex.getInstance(project).isExcluded(symlinkFile) }) { "$workspaceSymlink is not excluded" }
  }

  private suspend fun addContentRoot(path: Path) {
    val workspaceModel = WorkspaceModel.getInstance(project)
    val url = path.toVirtualFileUrl(workspaceModel.getVirtualFileUrlManager())
    workspaceModel.update("Add a content root under the execution root") { storage ->
      storage.addEntity(ModuleEntity(name = "execroot-content", dependencies = emptyList(), entitySource = TestEntitySource) {
        contentRoots = listOf(ContentRootEntity(url = url, excludedPatterns = emptyList(), entitySource = TestEntitySource))
      })
    }
  }

  private suspend fun detectAndRegisterVcsRoots(): List<String> = withContext(Dispatchers.IO) {
    val detectedRoots = VcsRootDetector.getInstance(project).detect().map { it.path.toNioPath() }
    assertTrue(workspaceDir in detectedRoots) { "The workspace repository itself is not detected: $detectedRoots" }

    val errors = VcsRootErrorsFinder.getInstance(project).find()
    VcsRootErrorsHandler.createInstance(project).fixAndNotifyIfNeeded(errors)
    ProjectLevelVcsManager.getInstance(project).getDirectoryMappings().map { it.directory }
  }

  private fun assertNoBazelOutputMappings(mappings: List<String>) {
    val bazelOutputMappings = mappings.filter { it.isNotEmpty() }.map { Path.of(it) }.filter { directory ->
      directory == workspaceSymlink || directory.startsWith(outputBase) || directory.startsWith(outputBase.toRealPath())
    }
    assertTrue(bazelOutputMappings.isEmpty()) {
      "Bazel output directories are registered as VCS roots: $bazelOutputMappings, all mappings: $mappings"
    }
  }

  /** A `.git` directory with a `HEAD` file is enough for `GitUtil.isGitRoot`. */
  private fun Path.createRepository(): Path {
    resolve(".git").createDirectories().resolve("HEAD").writeText("ref: refs/heads/master\n")
    return this
  }

  private object TestEntitySource : EntitySource
}
