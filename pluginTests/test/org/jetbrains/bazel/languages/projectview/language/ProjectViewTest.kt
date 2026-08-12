package org.jetbrains.bazel.languages.projectview.language

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.common.timeoutRunBlocking
import com.intellij.testFramework.junit5.fixture.moduleFixture
import com.intellij.testFramework.junit5.fixture.projectFixture
import com.intellij.testFramework.junit5.fixture.tempPathFixture
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.intellij.lang.annotations.Language
import org.jetbrains.bazel.commons.ExcludableValue
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.languages.projectview.DIRECTORIES_KEY
import org.jetbrains.bazel.languages.projectview.IMPORT_DEPTH_KEY
import org.jetbrains.bazel.languages.projectview.ProjectView
import org.jetbrains.bazel.languages.projectview.ProjectViewFactory
import org.jetbrains.bazel.languages.projectview.SHARD_SYNC_KEY
import org.jetbrains.bazel.languages.projectview.TARGETS_KEY
import org.jetbrains.bazel.languages.projectview.buildFlags
import org.jetbrains.bazel.languages.projectview.debugFlags
import org.jetbrains.bazel.languages.projectview.deriveTargetsFromDirectories
import org.jetbrains.bazel.languages.projectview.directories
import org.jetbrains.bazel.languages.projectview.dotIdeaDirectoryLocation
import org.jetbrains.bazel.languages.projectview.imports.Import
import org.jetbrains.bazel.languages.projectview.syncFlags
import org.jetbrains.bazel.languages.projectview.testFlags
import org.jetbrains.bazel.project.BazelProjectFixtures.initializeBazelProject
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.utils.findVirtualFile
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.createDirectory
import kotlin.time.Duration.Companion.seconds

@BazelTestApplication
class ProjectViewTest {
  private val projectFixture = projectFixture(openAfterCreation = true)
  private val tempDirFixture = tempPathFixture()
  private val moduleFixture = projectFixture.moduleFixture(tempDirFixture, addPathToSourceRoot = true)

  private val project get() = projectFixture.get()
  private val rootDir by tempDirFixture

  @BeforeEach
  fun setUp() {
    moduleFixture.get()
    initializeBazelProject(project, rootDir)
  }

  @Test
  fun `test project view from file`() {
    val projectView = parse(
      """
      targets:
        target1
        -target2
      shard_sync: true
      """.trimIndent(),
    )

    val targetsSection = projectView.getSection(TARGETS_KEY)
    targetsSection.shouldNotBeNull()
    targetsSection shouldContain ExcludableValue.included(Label.parse("target1"))
    targetsSection shouldContain ExcludableValue.excluded(Label.parse("target2"))

    projectView.getSection(SHARD_SYNC_KEY) shouldBe true
  }

  @Test
  fun `test list section items are parsed from the lines below the keyword`() {
    val projectView = parse(
      """
      directories:
        dirA
        -dirB
        dirC
      """.trimIndent(),
    )

    projectView.getSection(DIRECTORIES_KEY) shouldContainExactly listOf(
      ExcludableValue.included(Path("dirA")),
      ExcludableValue.excluded(Path("dirB")),
      ExcludableValue.included(Path("dirC")),
    )
  }

  @Test
  fun `test list section with a single item is parsed from the keyword line`() {
    val projectView = parse("directories: dirA")

    projectView.getSection(DIRECTORIES_KEY) shouldContainExactly listOf(ExcludableValue.included(Path("dirA")))
  }

  @Test
  fun `test scalar sections are parsed from the keyword line`() {
    val projectView = parse(
      """
      shard_sync: true
      import_depth: 42
      dot_idea_directory_location: custom/.idea
      """.trimIndent(),
    )

    projectView.getSection(SHARD_SYNC_KEY) shouldBe true
    projectView.getSection(IMPORT_DEPTH_KEY) shouldBe 42
    projectView.dotIdeaDirectoryLocation shouldBe Path("custom/.idea")
  }

  @Test
  fun `test scalar section with more than one value is discarded`() {
    val projectView = parse(
      """
      import_depth:
        1
        2
      shard_sync: true
      """.trimIndent(),
    )
    projectView.getSection(IMPORT_DEPTH_KEY) shouldBe -1
    projectView.getSection(SHARD_SYNC_KEY) shouldBe true
  }

  @Test
  fun `test correct flag parsing`() {
    val projectView = parse(
      """
      sync_flags:
        --announce_rc
      build_flags:
        --define=ij_product=intellij-latest
      debug_flags:
        --java_debug
      test_flags:
        --test_output=all
      """.trimIndent(),
    )

    projectView.syncFlags shouldContain "--announce_rc"
    projectView.buildFlags shouldContain "--define=ij_product=intellij-latest"
    projectView.debugFlags shouldContain "--java_debug"
    projectView.testFlags shouldContain "--test_output=all"
  }

  @Test
  fun `test invalid target label is ignored and does not crash`() {
    val projectView = parse(
      """
      targets:
        targetA
        //...:invalidTarget
        targetB
      """.trimIndent(),
    )

    projectView.getSection(TARGETS_KEY) shouldContainExactly listOf(
      ExcludableValue.included(Label.parse("targetA")),
      ExcludableValue.included(Label.parse("targetB")),
    )
  }

  @Test
  fun `test parsing creates no document for the parsed files`() {
    val (main, imported) = createProjectViewFiles()
    main.cachedDocument().shouldBeNull()
    imported.cachedDocument().shouldBeNull()

    val projectView = parse(main)

    projectView.dotIdeaDirectoryLocation shouldBe Path("custom/.idea")
    projectView.getSection(IMPORT_DEPTH_KEY) shouldBe 123
    main.cachedDocument().shouldBeNull()
    imported.cachedDocument().shouldBeNull()
  }

  @Test
  fun `test parsing does not require a read action`() {
    val (main, _) = createProjectViewFiles()
    ApplicationManager.getApplication().isReadAccessAllowed shouldBe false

    val projectView = parse(main)

    projectView.dotIdeaDirectoryLocation shouldBe Path("custom/.idea")
  }

  @Test
  fun `test unsaved editor changes are parsed`() = timeoutRunBlocking(30.seconds) {
    val (main, imported) = createProjectViewFiles()
    main.setUnsavedText("dot_idea_directory_location: unsaved/.idea\n\nimport imported.bazelproject")
    imported.setUnsavedText("import_depth: 321")

    val projectView = parse(main)

    projectView.dotIdeaDirectoryLocation shouldBe Path("unsaved/.idea")
    projectView.getSection(IMPORT_DEPTH_KEY) shouldBe 321
  }

  @Test
  fun `test unresolved import is reported at its position in the importing file`() {
    val main = rootDir.writeProjectViewFile(
      "main.bazelproject",
      """
      dot_idea_directory_location: custom/.idea

      import missing.bazelproject
      """.trimIndent(),
    )

    val projectView = parse(main)

    projectView.dotIdeaDirectoryLocation shouldBe Path("custom/.idea")
    val unresolved = projectView.imports.single().shouldBeInstanceOf<Import.Unresolved>()
    unresolved.text shouldBe "missing.bazelproject"
    val position = unresolved.position.shouldNotBeNull()
    position.path shouldBe main
    position.startLine shouldBe 2
  }

  @Test
  fun `test a directory cannot be imported`() {
    rootDir.resolve("importeddir").createDirectory()
    val main = rootDir.writeProjectViewFile(
      "main.bazelproject",
      """
      dot_idea_directory_location: custom/.idea

      import importeddir
      """.trimIndent(),
    )

    val projectView = parse(main)

    projectView.dotIdeaDirectoryLocation shouldBe Path("custom/.idea")
    projectView.imports.single().shouldBeInstanceOf<Import.Unresolved>()
  }

  @Test
  fun `test the byte order mark does not swallow the first section`() {
    val main = rootDir.writeProjectViewFile("main.bazelproject", "\uFEFFdot_idea_directory_location: custom/.idea")

    val projectView = parse(main)

    projectView.dotIdeaDirectoryLocation shouldBe Path("custom/.idea")
  }

  @Test
  fun `test a file VFS does not know about is parsed`() {
    val imported = rootDir.writeProjectViewFileUnknownToVfs("imported.bazelproject", "import_depth: 123")
    val main = rootDir.writeProjectViewFileUnknownToVfs(
      "main.bazelproject",
      "dot_idea_directory_location: custom/.idea\n\nimport imported.bazelproject",
    )
    main.findVirtualFile().shouldBeNull()
    imported.findVirtualFile().shouldBeNull()

    val projectView = parse(main)

    projectView.dotIdeaDirectoryLocation shouldBe Path("custom/.idea")
    projectView.getSection(IMPORT_DEPTH_KEY) shouldBe 123
  }

  @Test
  fun `test the default project view comes from the template file in the project root`() {
    rootDir.writeProjectViewFile("tools/intellij/.managed.bazelproject", "import_depth: 7")

    val projectView = ProjectViewFactory.fromDefault(project)

    projectView.getSection(IMPORT_DEPTH_KEY) shouldBe 7
  }

  @Test
  fun `test forceDefaultTemplate ignores the template file in the project root`() {
    rootDir.writeProjectViewFile("tools/intellij/.managed.bazelproject", "import_depth: 7")

    val projectView = ProjectViewFactory.fromDefault(project, forceDefaultTemplate = true)

    projectView.getSection(IMPORT_DEPTH_KEY) shouldBe -1
    projectView.deriveTargetsFromDirectories shouldBe true
    projectView.directories shouldContainExactly listOf(ExcludableValue.included(Path(".")))
  }

  private fun parse(@Language("projectview") content: String): ProjectView =
    parse(rootDir.writeProjectViewFile(".bazelproject", content))

  private fun parse(source: Path): ProjectView = ProjectViewFactory.from(project, source = source, root = rootDir)

  private fun createProjectViewFiles(): Pair<Path, Path> {
    val imported = rootDir.writeProjectViewFile(
      "imported.bazelproject",
      """
      import_depth: 123
      targets:
        //targetA
        //targetB
      """.trimIndent(),
    )
    val main = rootDir.writeProjectViewFile(
      "main.bazelproject",
      """
      dot_idea_directory_location: custom/.idea

      import imported.bazelproject
      """.trimIndent(),
    )
    return main to imported
  }

  private fun Path.cachedDocument() = FileDocumentManager.getInstance().getCachedDocument(virtualFile())

  private suspend fun Path.setUnsavedText(text: String) {
    val virtualFile = virtualFile()
    withContext(Dispatchers.EDT) {
      val document = FileDocumentManager.getInstance().getDocument(virtualFile).shouldNotBeNull()
      WriteCommandAction.runWriteCommandAction(project) { document.setText(text) }
    }
    FileDocumentManager.getInstance().isFileModified(virtualFile) shouldBe true
  }

  private fun Path.virtualFile(): VirtualFile = findVirtualFile().shouldNotBeNull()
}
