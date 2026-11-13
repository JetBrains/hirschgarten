package org.jetbrains.bazel.clion

import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.service
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.refreshAndFindVirtualFile
import com.intellij.testFramework.common.timeoutRunBlocking
import com.intellij.testFramework.junit5.TestApplication
import com.intellij.testFramework.junit5.fixture.projectFixture
import com.intellij.testFramework.junit5.fixture.tempPathFixture
import com.jetbrains.cidr.lang.workspace.OCResolveConfigurationDisplayNameProvider
import com.jetbrains.cidr.lang.workspace.OCWorkspace
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.bazel.clion.workspace.CC_CLIENT_KEY
import org.jetbrains.bazel.clion.workspace.CcResolveConfiguration
import org.jetbrains.bazel.clion.workspace.encode
import org.jetbrains.bazel.fixtures.ccProject
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.project.BazelProjectFixtures.initializeBazelProject
import org.jetbrains.bazel.sync.workspace.persistence.WorkspaceSnapshotService
import org.jetbrains.bazel.sync.workspace.snapshot.FileToTargetMap
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceConfigurationId
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile

@TestApplication
class CcResolveConfigurationDisplayNameProviderTest {
  private val directoryFixture = tempPathFixture()
  private val directory by directoryFixture
  private val project by projectFixture(pathFixture = directoryFixture)

  private val owners = mapOf(
    "a/one.cc" to listOf(key("//a:one", "arm")),
    "a/two.cc" to listOf(key("//a:two", "arm")),
    "a/shared.cc" to listOf(key("//a:one", "arm"), key("//a:two", "arm")),
    "a/multi.cc" to listOf(key("//a:multi", "arm"), key("//a:multi", "x86")),
  )

  private lateinit var files: Map<String, VirtualFile>

  @BeforeEach
  fun setUp(): Unit = timeoutRunBlocking {
    initializeBazelProject(project, directory)
    files = (owners.keys + "a/unknown.cc").associateWith { createFile(it) }
    val fileToTarget = FakeFileToTargetMap(owners.mapKeys { files.getValue(it.key).toNioPath() })
    project.service<WorkspaceSnapshotService>().update { ccProject { }.copy(fileToTarget = fileToTarget) to Unit }

    val model = OCWorkspace.getInstance(project).getModifiableModel(CC_CLIENT_KEY, true)
    try {
      model.addConfiguration(configurationId("arm"), "group")
      model.addConfiguration(configurationId("x86"), "group")
      model.addConfiguration("plain", "plain")
      model.preCommit()
      model.commitAndContribute()
    }
    finally {
      Disposer.dispose(model)
    }
  }

  @Test
  fun testNamesOwningTarget(): Unit = timeoutRunBlocking {
    assertThat(displayName(configurationId("arm"), "a/one.cc")).isEqualTo("@//a:one (arm)")
    assertThat(displayName(configurationId("arm"), "a/two.cc")).isEqualTo("@//a:two (arm)")
  }

  @Test
  fun testIgnoresTargetsInOtherConfigurations(): Unit = timeoutRunBlocking {
    assertThat(displayName(configurationId("arm"), "a/multi.cc")).isEqualTo("@//a:multi (arm)")
    assertThat(displayName(configurationId("x86"), "a/multi.cc")).isEqualTo("@//a:multi (x86)")
  }

  @Test
  fun testSeveralOwningTargets(): Unit = timeoutRunBlocking {
    assertThat(displayName(configurationId("arm"), "a/shared.cc")).isNull()
  }

  @Test
  fun testUnknownFile(): Unit = timeoutRunBlocking {
    assertThat(displayName(configurationId("arm"), "a/unknown.cc")).isNull()
  }

  @Test
  fun testIgnoresConfigurationsWithoutBazelIdentifier(): Unit = timeoutRunBlocking {
    assertThat(displayName("plain", "a/one.cc")).isNull()
  }

  private fun configurationId(id: String): String =
    CcResolveConfiguration.Identifier(0, WorkspaceConfigurationId.of(id)).encode()

  private suspend fun displayName(id: String, path: String): String? {
    val file = files.getValue(path)
    return readAction {
      val configuration = requireNotNull(OCWorkspace.getInstance(project).getConfigurationById(id))
      OCResolveConfigurationDisplayNameProvider.EP_NAME.extensionList.firstNotNullOfOrNull { it.getDisplayName(configuration, file) }
    }
  }

  private fun createFile(path: String): VirtualFile {
    val file = directory.resolve(path)
    file.parent.createDirectories()
    file.createFile()
    return requireNotNull(file.refreshAndFindVirtualFile())
  }
}

private fun key(label: String, configuration: String) = WorkspaceTargetKey(Label.parse(label), WorkspaceConfigurationId.of(configuration))

private class FakeFileToTargetMap(private val targets: Map<Path, List<WorkspaceTargetKey>>) : FileToTargetMap {
  override fun getTargetsByFile(path: Path): List<WorkspaceTargetKey> = targets[path].orEmpty()
  override fun addMapping(path: Path, targets: List<WorkspaceTargetKey>) {}
  override fun removeMapping(path: Path) {}
  override val size: Int get() = targets.size
}
