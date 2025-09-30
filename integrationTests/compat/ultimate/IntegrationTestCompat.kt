package org.jetbrains.bazel.test.compat

import com.intellij.ide.starter.ide.IDETestContext
import com.intellij.ide.starter.extended.engine.AdditionalModulesForDevBuildServer
import com.intellij.ide.starter.extended.plugins.asExtended
import com.intellij.ide.starter.models.IdeInfo
import com.intellij.ide.starter.models.TestCase
import com.intellij.ide.starter.project.ProjectInfoSpec

object IntegrationTestCompat {
  val requiredModules = arrayOf("intellij.bazel.plugin", "intellij.protoeditor")
  val requiredPlugins = arrayOf("org.jetbrains.bazel", "idea.plugin.protoeditor")

  fun onPreCreateContext() {
    AdditionalModulesForDevBuildServer.addAdditionalModules(*requiredModules)
  }
  
  fun onPostCreateContext(ctx: IDETestContext) {
    for (pluginId in requiredPlugins) {
      ctx.pluginConfigurator.asExtended().installPluginFromTeamCity(pluginId)
    }
  }

  fun <T : ProjectInfoSpec> interceptTestCase(case: TestCase<T>, ide: IdeInfo): TestCase<T> = case

}
