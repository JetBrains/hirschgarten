package org.jetbrains.bazel.languages.starlark.fixtures

import com.intellij.lang.documentation.ide.IdeDocumentationTargetProvider
import com.intellij.platform.backend.documentation.impl.computeDocumentationBlocking
import com.intellij.testFramework.fixtures.BasePlatformTestCase

open class StarlarkDocumentationTestCase : BasePlatformTestCase() {
  protected fun getDocumentationAtCaret(): String? {
    val provider = IdeDocumentationTargetProvider.getInstance(project)
    val targets = provider.documentationTargets(myFixture.editor, myFixture.file, myFixture.caretOffset)

    return targets
      .mapNotNull { computeDocumentationBlocking(it.createPointer())?.html }
      .also { assertTrue("More then one documentation rendered:\n\n${it.joinToString("\n\n")}", it.size <= 1) }
      .getOrNull(0)
      ?.trim()
      ?.replace(Regex("<a href=\"psi_element:[^\"]*/unitTest[0-9]+/"), "<a href=\"psi_element:///src/")
  }
}
