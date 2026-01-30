package org.jetbrains.bazel.test.framework

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.backend.workspace.virtualFile
import com.intellij.platform.backend.workspace.workspaceModel
import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import org.apache.velocity.VelocityContext
import org.apache.velocity.app.VelocityEngine
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.extension
import kotlin.io.path.pathString
import kotlin.io.path.reader
import kotlin.io.path.walk
import kotlin.io.path.writer

fun CodeInsightTestFixture.checkHighlighting(
  path: String,
) {
  openFileInEditor(virtualFileOf("$tempDirPath/$path"))
  checkHighlighting()
}

fun CodeInsightTestFixture.virtualFileOf(path: String): VirtualFile {
  val manager = project.workspaceModel.getVirtualFileUrlManager()
  return Path(path)
    .toVirtualFileUrl(manager)
    .virtualFile
    .let { requireNotNull(it) { "Virtual file not found for path: $path" } }
}

fun CodeInsightTestFixture.materializeTemplateFilesInProject(
  variables: Map<String, Any>,
) {
  val path = Path(tempDirPath)
  val engine = VelocityEngine()
  val context = VelocityContext()
  variables.forEach { (key, value) -> context.put(key, value) }
  engine.init()
  path.walk()
    .filter { it.extension == "template" }
    .forEach { templateFile ->
      materializeTemplateFile(engine, context, templateFile)
    }
}

private fun materializeTemplateFile(
  engine: VelocityEngine,
  context: VelocityContext,
  templateFile: Path,
) {
  val materializedFileName = templateFile.fileName.pathString.removeSuffix(".template")
  val materializedFile = templateFile.parent.resolve(materializedFileName)
  materializedFile
    .writer()
    .use { writer ->
      templateFile
        .reader()
        .use { reader ->
          engine.evaluate(context, writer, "bazel-test-templates", reader)
        }
    }
}
