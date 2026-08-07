package org.jetbrains.bazel.redcodes

import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.readAction
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.psi.PsiClass
import com.intellij.testFramework.junit5.fixture.projectFixture
import com.intellij.testFramework.junit5.fixture.tempPathFixture
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelSyncCodeInsightFixture
import org.junit.jupiter.api.Test

@BazelTestApplication
class TransitiveModuleDepsTest {

  private val projectFixture = projectFixture(openAfterCreation = true)
  private val tempDir = tempPathFixture()
  private val fixture by bazelSyncCodeInsightFixture(projectFixture, tempDir)

  // https://youtrack.jetbrains.com/issue/BAZEL-2325/Import-in-repo-transitive-module-dependencies-by-default
  @Test
  fun `transitive module dependency is imported as a source module`(): Unit = runBlocking(Dispatchers.Default) {
    fixture.copyBazelTestProject("redcodes/transitive_module_deps")
    fixture.setProjectView(".bazelproject")
    fixture.performBazelSync()

    val project = fixture.project

    withContext(Dispatchers.EDT) {
      val reference = fixture.getReferenceAtCaretPosition("mod_a/ClassA.java").shouldNotBeNull()

      val classB = reference.resolve() as PsiClass
      val virtualFile = classB.containingFile.virtualFile.shouldNotBeNull()
      virtualFile.extension shouldBe "java"

      val isSource = readAction { ProjectFileIndex.getInstance(project).isInSource(virtualFile) }
      isSource.shouldBeTrue()
    }
  }
}
