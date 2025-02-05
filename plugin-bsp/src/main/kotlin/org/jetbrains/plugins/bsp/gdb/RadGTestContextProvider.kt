package org.jetbrains.plugins.bsp.gdb

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.util.asSafely
import com.intellij.util.execution.ParametersListUtil
import com.jetbrains.cidr.radler.testing.RadTestPsiElement
import com.jetbrains.rider.model.RadTestElementModel
import com.jetbrains.rider.model.RadTestFramework
import org.jetbrains.plugins.bsp.target.temporaryTargetUtils
import org.jetbrains.plugins.bsp.workspacemodel.entities.BuildTargetInfo

class RadGTestContextProvider {
  fun getTestContext(context: ConfigurationContext): TestContext? {
    val psiElement = context.psiLocation as RadTestPsiElement
    if (psiElement.test.framework != RadTestFramework.GTest) {
      return null
    }
    //todo: TestTargetHeuristic
    val target = findTargets(context).filter { it.capabilities.canTest }.firstOrNull() ?: return null
    return TestContext(psiElement, target,createTestFilter(psiElement.test))
  }

  private fun createTestFilter(test: RadTestElementModel): String {
    val suite = test.suites?.firstOrNull() ?: "*"
    val name = test.test ?: "*"
    val encodedParam = "$suite.$name".encodeParam()
    return encodedParam
  }


  private fun findTargets(context: ConfigurationContext): List<BuildTargetInfo> {
    val virtualFile = context.location?.virtualFile ?: return emptyList()
    val targetUtilService = context.project.temporaryTargetUtils
    return targetUtilService
      .getExecutableTargetsForFile(virtualFile, context.project)
      .distinct()
      .mapNotNull { targetUtilService.getBuildTargetInfoForId(it) }
  }

  private fun String.encodeParam(): String = ParametersListUtil.escape(replace("'", " '")).replace(" '", "'")

}
