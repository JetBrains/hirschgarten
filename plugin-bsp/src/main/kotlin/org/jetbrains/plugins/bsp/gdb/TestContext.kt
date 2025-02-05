package org.jetbrains.plugins.bsp.gdb

import com.intellij.psi.PsiElement
import org.jetbrains.plugins.bsp.run.BspRunHandlerProvider
import org.jetbrains.plugins.bsp.run.config.BspRunConfiguration
import org.jetbrains.plugins.bsp.run.state.GenericTestState
import org.jetbrains.plugins.bsp.workspacemodel.entities.BuildTargetInfo

class TestContext(val sourceElement: PsiElement, val target: BuildTargetInfo ,val testFilter:String) {
  fun setupRunConfiguration(config: BspRunConfiguration): Boolean {
   // val provider = BspRunHandlerProvider.findRunHandlerProvider("GenericBspTestHandlerProvider")


    config.updateTargets(listOf(target.id))
    val state=config.handler?.state as? GenericTestState?:return false
    state.testFilter="--test_filter=$testFilter"

    config.name="$testFilter via ${target.buildTargetName}"
    return true
  }


  fun matchesRunConfiguration(config: BspRunConfiguration): Boolean {
    val state=config.handler?.state as? GenericTestState?:return false
  // todo:
   return state.testFilter == "--test_filter=$testFilter"

  }
}
