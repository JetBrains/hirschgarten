package org.jetbrains.plugins.bsp.impl.flow.sync

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.OutputPathItem
import ch.epfl.scala.bsp4j.OutputPathItemKind
import ch.epfl.scala.bsp4j.OutputPathsItem
import ch.epfl.scala.bsp4j.OutputPathsResult
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.util.progress.reportSequentialProgress
import com.intellij.testFramework.utils.vfs.createDirectory
import com.intellij.testFramework.utils.vfs.createFile
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.sequences.shouldHaveSize
import kotlinx.coroutines.runBlocking
import org.jetbrains.bsp.protocol.BazelBuildServerCapabilities
import org.jetbrains.plugins.bsp.config.rootDir
import org.jetbrains.plugins.bsp.impl.flow.sync.ProjectSyncHook.ProjectSyncHookEnvironment
import org.jetbrains.plugins.bsp.projectStructure.AllProjectStructuresProvider
import org.jetbrains.plugins.bsp.projectStructure.workspaceModel.workspaceModelDiff
import org.jetbrains.plugins.bsp.workspacemodel.entities.BspProjectDirectoriesEntity
import org.jetbrains.workspace.model.test.framework.BuildServerMock
import org.jetbrains.workspace.model.test.framework.MockProjectBaseTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.io.path.Path

@DisplayName("OutputPathUrisSyncHook tests")
class OutputPathUrisSyncHookTest : MockProjectBaseTest() {
  private lateinit var hook: OutputPathUrisSyncHook
  private lateinit var projectRoot: VirtualFile

  @BeforeEach
  override fun beforeEach() {
    // given
    super.beforeEach()
    hook = OutputPathUrisSyncHook()
    projectRoot = Path(project.basePath!!).toVirtualFile()
    project.rootDir = projectRoot
  }

  @Test
  fun `should query for output paths and put BspProjectDirectoriesEntity with excluded paths into workspace model`() {
    // given
    val outputA1 = runWriteAction { projectRoot.createDirectory("outputA1") }
    val outputA2 = runWriteAction { projectRoot.createFile("outputA2") }
    val targetIdA = BuildTargetIdentifier("targetA")
    val outputPathItemA1 = OutputPathItem(outputA1.url, OutputPathItemKind.DIRECTORY)
    val outputPathItemA2 = OutputPathItem(outputA2.url, OutputPathItemKind.FILE)
    val outputPathsItemA = OutputPathsItem(targetIdA, listOf(outputPathItemA1, outputPathItemA2))

    val targetIdB = BuildTargetIdentifier("targetB")
    val outputB1 = runWriteAction { projectRoot.createDirectory("outputB1") }
    val outputPathItemB1 = OutputPathItem(outputB1.url, OutputPathItemKind.DIRECTORY)
    val outputPathsItemB = OutputPathsItem(targetIdB, listOf(outputPathItemB1))

    val outputPathsResult = OutputPathsResult(listOf(outputPathsItemA, outputPathsItemB))

    val server = BuildServerMock(outputPathsResult = outputPathsResult)
    val capabilities = BazelBuildServerCapabilities(outputPathsProvider = true)

    val diff = AllProjectStructuresProvider(project).newDiff()
    val baseTargetInfos =
      BaseTargetInfos(
        allTargetIds = listOf(targetIdA, targetIdB),
        infos = emptyList(),
      )

    // when
    runBlocking {
      reportSequentialProgress { reporter ->
        hook.onSync(
          ProjectSyncHookEnvironment(
            project = project,
            syncScope = SecondPhaseSync,
            server = server,
            capabilities = capabilities,
            diff = diff,
            taskId = "test-task-id",
            progressReporter = reporter,
            baseTargetInfos = baseTargetInfos,
          ),
        )
      }
    }

    // then
    val entities = diff.workspaceModelDiff.mutableEntityStorage.entities(BspProjectDirectoriesEntity::class.java)
    entities shouldHaveSize 1

    val entity = entities.first()
    entity.includedRoots.map { it.url } shouldContainExactlyInAnyOrder listOf(projectRoot.url)
    entity.excludedRoots.map { it.url } shouldContainExactlyInAnyOrder listOf(outputA1.url, outputA2.url, outputB1.url)
  }
}
