package org.jetbrains.plugins.bsp.services

import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.PsiDirectory
import com.intellij.psi.impl.PsiElementFinderImpl
import com.intellij.testFramework.runInEdtAndWait
import com.intellij.util.Processors
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldNotBeNull
import org.jetbrains.workspace.model.test.framework.JavaWorkspaceModelFixtureBaseTest
import org.junit.jupiter.api.Test

class SingleFileSourcesTrackerTest : JavaWorkspaceModelFixtureBaseTest() {

  @Test
  fun `Java single file sources should be registered and resolved properly`() {
    runInEdtAndWait {
      // given
      val fileA1 = fixture.addFileToProject(
        "module1/x/org/jetbrains/module1/A1.java",
        """
          |package org.jetbrains.module1;
  
          |class A1 {
          |  String a1() {
          |    return "XDDD";
          |  }
          |}
        """.trimMargin()
      )

      val fileA2 = fixture.addFileToProject(
        "module1/x/org/jetbrains/module1/A2.java",
        """
          |package org.jetbrains.module1;
    
          |class A2 {
    
          |  String a2() {
          |    A1 a = new A1();
          |    return a.a<caret>1() + "XD";
          |  }
          |}
        """.trimMargin()
      )

      // when
      updateWorkspaceModel {
        val module1 = addEmptyJavaModuleEntity("module1", it)
        addJavaSourceRootEntities(
          files = listOf(fileA1.virtualFile, fileA2.virtualFile),
          packagePrefix = "org.jetbrains.module1",
          module = module1,
          entityStorage = it
        )
      }

      // then

      // Check if the added classes are in module scope
      val module = ModuleUtilCore.findModuleForFile(fileA1.virtualFile, project)
      val psiElementFinderImpl = PsiElementFinderImpl(project)
      val pkg = psiElementFinderImpl.findPackage("org.jetbrains.module1")
      module.shouldNotBeNull()
      pkg.shouldNotBeNull()
      val classes = psiElementFinderImpl.getClasses(pkg, module.moduleScope)
      classes.map { it.name } shouldContainExactlyInAnyOrder listOf("A1", "A2")

      // Check package directories found by Java's PsiElementFinderImpl
      val singleFileDirectories = arrayListOf<PsiDirectory>()
      val processor = Processors.cancelableCollectProcessor(singleFileDirectories)
      psiElementFinderImpl.processPackageDirectories(pkg, module.moduleScope, processor, false)
      singleFileDirectories.map { it.name } shouldContainExactlyInAnyOrder listOf("module1")
    }
  }
}
