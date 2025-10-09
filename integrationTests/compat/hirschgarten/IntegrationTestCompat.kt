package org.jetbrains.bazel.test.compat

import com.intellij.ide.starter.ide.IDETestContext
import com.intellij.ide.starter.models.IdeInfo
import com.intellij.ide.starter.models.TestCase
import com.intellij.ide.starter.project.ProjectInfoSpec
import org.jetbrains.bazel.test.framework.ResourceUtil
import java.nio.file.Path

object IntegrationTestCompat {

  fun onPreCreateContext() {
    // do not delete - part of compat
  }

  fun onPostCreateContext(ctx: IDETestContext) {
    val zip = System.getProperty("bazel.ide.starter.test.bazel.plugin.zip")
    ResourceUtil.useResource(zip) {
      ctx.pluginConfigurator.installPluginFromPath(it)
    }
  }

  fun <T : ProjectInfoSpec> interceptTestCase(case: TestCase<T>, ide: IdeInfo): TestCase<T> {
    val build = System.getProperty("bazel.ide.starter.test.ide.${ide.productCode}.build.number")
    return case.withBuildNumber(build)
  }

}
