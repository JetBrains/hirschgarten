package org.jetbrains.plugins.bsp.protocol.connection

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.io.createFile
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.jetbrains.workspace.model.test.framework.MockProjectBaseTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.OutputStream
import java.nio.file.Path
import kotlin.io.path.createTempDirectory

private object GeneratorWhichCantGenerate : BspConnectionDetailsGenerator {

  override fun id(): String = "cant generate"

  override fun displayName(): String = "cant generate"

  override fun canGenerateBspConnectionDetailsFile(projectPath: VirtualFile): Boolean = false

  override fun generateBspConnectionDetailsFile(projectPath: VirtualFile, outputStream: OutputStream): VirtualFile = projectPath
}

private class GeneratorWhichCanGenerate(private val name: String, private val generatedFilePath: VirtualFile) :
  BspConnectionDetailsGenerator {

  var hasGenerated = false

  override fun id(): String = name

  override fun displayName(): String = name

  override fun canGenerateBspConnectionDetailsFile(projectPath: VirtualFile): Boolean = true

  override fun generateBspConnectionDetailsFile(projectPath: VirtualFile, outputStream: OutputStream): VirtualFile {
    hasGenerated = true

    return generatedFilePath
  }
}

class BspConnectionDetailsGeneratorProviderTest : MockProjectBaseTest() {

  private lateinit var projectPath: Path
  private lateinit var generatedVirtualFile: VirtualFile
  private lateinit var otherGeneratedVirtualFile: VirtualFile

  @BeforeEach
  override fun beforeEach() {
    // given
    super.beforeEach()

    this.projectPath = createTempDirectory("project")
    this.generatedVirtualFile = projectPath.resolve(".bsp").resolve("connection-file.json").createFile().toVirtualFile()
    this.otherGeneratedVirtualFile = projectPath.resolve(".bsp").resolve("other-connection-file.json").createFile().toVirtualFile()
  }

  @AfterEach
  fun afterEach() {
    projectPath.toFile().deleteRecursively()
  }

  @Test
  fun `should return false for canGenerateAnyBspConnectionDetailsFile(), empty list for availableGeneratorsNames() if there is no generator provided`() {
    // given
    val bspConnectionDetailsGenerators = emptyList<BspConnectionDetailsGenerator>()

    // when
    val provider = BspConnectionDetailsGeneratorProvider(projectPath.toVirtualFile(), bspConnectionDetailsGenerators)

    // then
    provider.canGenerateAnyBspConnectionDetailsFile() shouldBe false
    provider.availableGeneratorsNames() shouldBe emptyList()
  }

  @Test
  fun `should return false for canGenerateAnyBspConnectionDetailsFile(), empty list for availableGeneratorsNames() if there is no generator which can generate`() {
    // given
    val bspConnectionDetailsGenerators = listOf(GeneratorWhichCantGenerate)

    // when
    val provider = BspConnectionDetailsGeneratorProvider(projectPath.toVirtualFile(), bspConnectionDetailsGenerators)

    // then
    provider.canGenerateAnyBspConnectionDetailsFile() shouldBe false
    provider.availableGeneratorsNames() shouldBe emptyList()
  }

  @Test
  fun `should not generate if the generator name is wrong`() {
    // given
    val generator = GeneratorWhichCanGenerate("generator 1", generatedVirtualFile)
    val bspConnectionDetailsGenerators = listOf(generator)

    // when
    val provider = BspConnectionDetailsGeneratorProvider(projectPath.toVirtualFile(), bspConnectionDetailsGenerators)

    // then
    provider.canGenerateAnyBspConnectionDetailsFile() shouldBe true
    provider.availableGeneratorsNames() shouldContainExactlyInAnyOrder listOf("generator 1")

    provider.generateBspConnectionDetailFileForGeneratorWithName("wrong name", OutputStream.nullOutputStream()) shouldBe null
    generator.hasGenerated shouldBe false
  }

  @Test
  fun `should return true for canGenerateAnyBspConnectionDetailsFile(), list with one element for availableGeneratorsNames() and generate if there is one generator which can generate`() {
    // given
    val generator = GeneratorWhichCanGenerate("generator 1", generatedVirtualFile)
    val bspConnectionDetailsGenerators = listOf(generator)

    // when
    val provider = BspConnectionDetailsGeneratorProvider(projectPath.toVirtualFile(), bspConnectionDetailsGenerators)

    // then
    provider.canGenerateAnyBspConnectionDetailsFile() shouldBe true
    provider.availableGeneratorsNames() shouldContainExactlyInAnyOrder listOf("generator 1")

    provider.generateBspConnectionDetailFileForGeneratorWithName("generator 1", OutputStream.nullOutputStream()) shouldBe generatedVirtualFile
    generator.hasGenerated shouldBe true
  }

  @Test
  fun `should return true for canGenerateAnyBspConnectionDetailsFile(), list with generators which can generate for availableGeneratorsNames() and generate if there are multiple generators which can generate and few which cant`() {
    // given
    val generator1 = GeneratorWhichCanGenerate("generator 1", otherGeneratedVirtualFile)
    val generator2 = GeneratorWhichCanGenerate("generator 2", generatedVirtualFile)
    val generator3 = GeneratorWhichCanGenerate("generator 3", otherGeneratedVirtualFile)
    val bspConnectionDetailsGenerators =
      listOf(generator1, GeneratorWhichCantGenerate, generator2, generator3, GeneratorWhichCantGenerate)

    // when
    val provider = BspConnectionDetailsGeneratorProvider(projectPath.toVirtualFile(), bspConnectionDetailsGenerators)

    // then
    provider.canGenerateAnyBspConnectionDetailsFile() shouldBe true
    provider.availableGeneratorsNames() shouldContainExactlyInAnyOrder listOf(
      "generator 1",
      "generator 2",
      "generator 3"
    )

    provider.generateBspConnectionDetailFileForGeneratorWithName("generator 2", OutputStream.nullOutputStream()) shouldBe generatedVirtualFile
    generator1.hasGenerated shouldBe false
    generator2.hasGenerated shouldBe true
    generator3.hasGenerated shouldBe false
  }

  @Test
  fun `should call first generator with the given name`() {
    // given
    val generator1 = GeneratorWhichCanGenerate("generator 1", otherGeneratedVirtualFile)
    val generator21 = GeneratorWhichCanGenerate("generator 2", generatedVirtualFile)
    val generator22 = GeneratorWhichCanGenerate("generator 2", otherGeneratedVirtualFile)
    val generator3 = GeneratorWhichCanGenerate("generator 3", otherGeneratedVirtualFile)
    val bspConnectionDetailsGenerators =
      listOf(generator1, GeneratorWhichCantGenerate, generator21, generator3, generator22, GeneratorWhichCantGenerate)

    // when
    val provider = BspConnectionDetailsGeneratorProvider(projectPath.toVirtualFile(), bspConnectionDetailsGenerators)

    // then
    provider.canGenerateAnyBspConnectionDetailsFile() shouldBe true
    provider.availableGeneratorsNames() shouldContainExactlyInAnyOrder listOf(
      "generator 1",
      "generator 2",
      "generator 2",
      "generator 3"
    )

    provider.generateBspConnectionDetailFileForGeneratorWithName("generator 2", OutputStream.nullOutputStream()) shouldBe generatedVirtualFile
    generator1.hasGenerated shouldBe false
    generator21.hasGenerated shouldBe true
    generator22.hasGenerated shouldBe false
    generator3.hasGenerated shouldBe false
  }
}
