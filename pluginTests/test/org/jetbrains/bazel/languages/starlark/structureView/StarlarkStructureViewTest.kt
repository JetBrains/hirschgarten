package org.jetbrains.bazel.languages.starlark.structureView

import com.intellij.ide.structureView.StructureViewModel
import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.ide.structureView.TreeBasedStructureViewBuilder
import com.intellij.ide.util.treeView.smartTree.Sorter
import com.intellij.lang.LanguageStructureViewBuilder
import com.intellij.openapi.application.EDT
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.util.Disposer
import com.intellij.platform.testFramework.junit5.codeInsight.fixture.codeInsightFixture
import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.testFramework.common.timeoutRunBlocking
import com.intellij.testFramework.junit5.TestApplication
import com.intellij.testFramework.junit5.fixture.moduleFixture
import com.intellij.testFramework.junit5.fixture.projectFixture
import com.intellij.testFramework.junit5.fixture.tempPathFixture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.workspace.setRepoMapping
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Path

@TestApplication
class StarlarkStructureViewTest {
  private val projectFixture = projectFixture(openAfterCreation = true)
  @Suppress("unused") private val moduleFixture = projectFixture.moduleFixture()
  private val codeInsightFixture by codeInsightFixture(projectFixture, tempPathFixture())

  @Test
  fun `outline shows top-level declarations in source order`(): Unit = timeoutRunBlocking {
    withContext(Dispatchers.EDT) {
      withModel(
        """
        load(":other.bzl", "imported")
        ZEBRA = 1
        def helper(arg):
            local = arg
            return local
        alpha = rule(implementation = helper)
        print(alpha)
        """.trimIndent(),
      ) { model ->
        assertEquals("defs.bzl", model.root.presentation.presentableText)
        assertEquals(listOf("ZEBRA", "helper", "alpha"), names(model))
        assertTrue(model.root.children.all { it.children.isEmpty() })
        assertTrue(Sorter.ALPHA_SORTER in model.sorters)
        val info = model as StructureViewModel.ElementInfoProvider
        assertFalse(info.isAlwaysLeaf(model.root))
        assertTrue(info.isAlwaysLeaf(model.root.children[1] as StructureViewTreeElement))
      }
    }
  }

  @Test
  fun `outline includes destructured variables`(): Unit = timeoutRunBlocking {
    withContext(Dispatchers.EDT) {
      withModel("first, (second, third), fourth = (1, (2, 3), 4)\n[fifth, sixth] = [5, 6]\n") { model ->
        assertEquals(listOf("first", "second", "third", "fourth", "fifth", "sixth"), names(model))
      }
    }
  }

  @Test
  fun `outline includes list targets nested in a tuple`(): Unit = timeoutRunBlocking {
    withContext(Dispatchers.EDT) {
      withModel("first, [second, third] = (1, [2, 3])\n") { model ->
        assertTrue(
          com.intellij.psi.util.PsiTreeUtil.findChildrenOfType(codeInsightFixture.file, com.intellij.psi.PsiErrorElement::class.java).isEmpty(),
          "The declaration must parse without errors",
        )
        assertEquals(listOf("first", "second", "third"), names(model))
      }
    }
  }

  @Test
  fun `outline excludes comprehension bindings in subscription assignments`(): Unit = timeoutRunBlocking {
    withContext(Dispatchers.EDT) {
      withModel("VALUES = [0]\nVALUES[[index for index in [0]][0]] = 1\n") { model ->
        assertTrue(
          com.intellij.psi.util.PsiTreeUtil.findChildrenOfType(codeInsightFixture.file, com.intellij.psi.PsiErrorElement::class.java).isEmpty(),
          "The assignment must parse without errors",
        )
        assertEquals(listOf("VALUES"), names(model))
      }
    }
  }

  @Test
  fun `empty file has an empty outline`(): Unit = timeoutRunBlocking {
    withContext(Dispatchers.EDT) {
      withModel("") { model -> assertTrue(model.root.children.isEmpty()) }
    }
  }

  @Test
  fun `function body selects the function`(): Unit = timeoutRunBlocking {
    withContext(Dispatchers.EDT) {
      withModel("def helper(arg):\n    local = <caret>arg\n    return local\n") { model ->
        assertEquals((model.root.children.single() as StructureViewTreeElement).value, model.currentEditorElement)
      }
    }
  }

  @Test
  fun `variable selects its declaration`(): Unit = timeoutRunBlocking {
    withContext(Dispatchers.EDT) {
      withModel("VAL<caret>UE = 1\n") { model ->
        assertEquals((model.root.children.single() as StructureViewTreeElement).value, model.currentEditorElement)
      }
    }
  }

  @Test
  fun `comprehension variable is not an outline entry`(): Unit = timeoutRunBlocking {
    withContext(Dispatchers.EDT) {
      withModel("VALUES = [item for it<caret>em in [1, 2]]\n") { model ->
        assertNull(model.currentEditorElement)
      }
    }
  }

  @Test
  fun `outline entries navigate to declarations`(): Unit = timeoutRunBlocking {
    withContext(Dispatchers.EDT) {
      withModel("VALUE = 1\ndef helper():\n    pass\n") { model ->
        for (child in model.root.children) {
          val entry = child as StructureViewTreeElement
          val declaration = entry.value as NavigatablePsiElement
          assertTrue(entry.canNavigateToSource())
          entry.navigate(true)
          assertEquals(declaration.textOffset, codeInsightFixture.editor.caretModel.offset)
          assertTrue(entry.presentation.getIcon(false) != null)
        }
      }
    }
  }

  @Test
  fun `outline reflects document changes`(): Unit = timeoutRunBlocking {
    withContext(Dispatchers.EDT) {
      withModel("VALUE = 1\n") { model ->
        val originalEntry = model.root.children.single() as StructureViewTreeElement
        val document = codeInsightFixture.editor.document
        WriteCommandAction.runWriteCommandAction(projectFixture.get()) {
          document.insertString(document.textLength, "def helper():\n    pass\n")
          PsiDocumentManager.getInstance(projectFixture.get()).commitDocument(document)
        }
        assertEquals(listOf("VALUE", "helper"), names(model))
        assertEquals(originalEntry, model.root.children.first())
        assertEquals("VALUE", originalEntry.presentation.presentableText)
        assertTrue(originalEntry.canNavigateToSource())
      }
    }
  }

  @Test
  fun `BUILD file shows rule targets and the package label`(): Unit = timeoutRunBlocking {
    val buildFile = withContext(Dispatchers.EDT) {
      codeInsightFixture.addFileToProject("MODULE.bazel", "")
      codeInsightFixture.addFileToProject(
        "foo/bar/BUILD",
        """
        load("@rules_java//java:defs.bzl", "java_library")
        SRCS = ["A.java"]
        java_library(name = "lib", srcs = SRCS)
        java_binary(name = "app", deps = [":lib"])
        exports_files(["A.java"])
        """.trimIndent(),
      )
    }
    val repoRoot = buildFile.virtualFile.parent.parent.parent
    projectFixture.get().setRepoMapping { mainRepo(Path.of(repoRoot.path)) }
    withContext(Dispatchers.EDT) {
      codeInsightFixture.configureFromExistingVirtualFile(buildFile.virtualFile)
      val editor = codeInsightFixture.editor
      editor.caretModel.moveToOffset(editor.document.text.indexOf("srcs = SRCS"))
      withModel(codeInsightFixture.file) { model ->
        assertEquals("BUILD", model.root.presentation.presentableText)
        assertEquals("//foo/bar", model.root.presentation.locationString)
        assertEquals(listOf("SRCS", "lib", "app"), names(model))
        assertEquals(listOf(null, "java_library", "java_binary"), model.root.children.map { it.presentation.locationString })
        val lib = model.root.children[1] as StructureViewTreeElement
        assertEquals(lib.value, model.currentEditorElement)
        assertTrue(lib.canNavigateToSource())
        assertTrue((model as StructureViewModel.ElementInfoProvider).isAlwaysLeaf(lib))
      }
    }
  }

  private fun names(model: StructureViewModel): List<String?> = model.root.children.map { it.presentation.presentableText }

  private fun withModel(text: String, check: (StructureViewModel) -> Unit) {
    withModel(codeInsightFixture.configureByText("defs.bzl", text), check)
  }

  private fun withModel(file: PsiFile, check: (StructureViewModel) -> Unit) {
    val builder = LanguageStructureViewBuilder.getInstance().getStructureViewBuilder(file) as TreeBasedStructureViewBuilder
    val model = builder.createStructureViewModel(codeInsightFixture.editor)
    try {
      check(model)
    }
    finally {
      Disposer.dispose(model)
    }
  }
}
