package org.jetbrains.bazel.redcodes

import com.intellij.java.workspace.entities.javaSourceRoots
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.readAction
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.backend.workspace.virtualFile
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.SourceRootEntity
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.search.searches.DefinitionsScopedSearch
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.IndexingTestUtil
import com.intellij.testFramework.common.timeoutRunBlocking
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelSyncCodeInsightFixture
import org.jetbrains.bazel.test.framework.checkHighlighting
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.condition.OS
import kotlin.time.Duration.Companion.minutes

@BazelTestApplication
class KotlinGeneratedSrcJarTest {

  private val fixture by bazelSyncCodeInsightFixture(
    "redcodes/kotlin_generated_srcjar",
    buildProject = true,
  )

  @Test
  @DisabledOnOs(OS.WINDOWS)
  fun `test srcjar generated sources`(): Unit = timeoutRunBlocking(timeout = 5.minutes) {
    val srcJarRoots = WorkspaceModel.getInstance(fixture.project)
      .currentSnapshot
      .entities(ModuleEntity::class.java)
      .flatMap { module -> module.contentRoots.flatMap { it.sourceRoots }.map { module to it } }
      .filter { (_, sourceRoot) -> sourceRoot.url.url.contains("impls.srcjar") }
      .toList()
    srcJarRoots.shouldHaveSize(1)
    val (module, srcJarRoot) = srcJarRoots.single()
    withClue("the source root of impls.srcjar") {
      srcJarRoot.url.url.startsWith("jar://") shouldBe true
      srcJarRoot.url.url.endsWith("impls.srcjar!/") shouldBe true
      srcJarRoot.isGenerated() shouldBe true
    }

    val bridgeImplFile = withClue("BridgeImpl.kt inside impls.srcjar") {
      readAction { srcJarRoot.url.virtualFile?.findFileByRelativePath("com/example/impl/BridgeImpl.kt") }
        .shouldNotBeNull()
    }
    readAction {
      val fileIndex = ProjectFileIndex.getInstance(fixture.project)
      withClue("BridgeImpl.kt is a source of the module") {
        fileIndex.isInSourceContent(bridgeImplFile) shouldBe true
      }
      fileIndex.getModuleForFile(bridgeImplFile)?.name shouldBe module.name
    }

    IndexingTestUtil.suspendUntilIndexesAreReady(fixture.project)

    val declarationFile = readAction {
      val registryFile = PsiManager.getInstance(fixture.project)
        .findFile(fixture.findFileInTempDir("impl/Registry.kt"))
        .shouldNotBeNull()
      val reference = registryFile.findReferenceAt(registryFile.text.indexOf("BridgeImpl()"))
        .shouldNotBeNull()
      reference.resolve()?.navigationElement?.containingFile?.virtualFile
    }
    withClue("declaration of BridgeImpl in Registry.kt") { declarationFile shouldBe bridgeImplFile }

    val implementationFiles = readAction {
      val bridgeFile = PsiManager.getInstance(fixture.project)
        .findFile(fixture.findFileInTempDir("spec/Bridge.kt"))
        .shouldNotBeNull()
      val bridge = PsiTreeUtil.findChildrenOfType(bridgeFile, PsiNameIdentifierOwner::class.java)
        .single { it.name == "Bridge" }
      DefinitionsScopedSearch.search(bridge).findAll()
        .mapNotNull { it.containingFile?.virtualFile }
    }
    withClue("implementations of Bridge") { implementationFiles shouldContain bridgeImplFile }

    withContext(Dispatchers.EDT) {
      fixture.checkHighlighting("impl/Registry.kt")
    }
  }

  private fun SourceRootEntity.isGenerated(): Boolean = javaSourceRoots.single().generated
}
