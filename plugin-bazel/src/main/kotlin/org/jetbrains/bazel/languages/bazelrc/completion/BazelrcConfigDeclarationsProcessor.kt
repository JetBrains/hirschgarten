package org.jetbrains.bazel.languages.bazelrc.completion

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.Processor
import org.jetbrains.bazel.languages.bazelrc.psi.BazelrcElement
import org.jetbrains.bazel.languages.bazelrc.psi.BazelrcFile
import org.jetbrains.bazel.languages.bazelrc.psi.BazelrcLine
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue

class BazelrcConfigDeclarationsProcessor(private val inputElement: BazelrcLine) : Processor<BazelrcElement> {
  var seenFiles = mutableSetOf<VirtualFile>()
  var results = mutableMapOf<String, BazelrcFile>()

  override fun process(currentElement: BazelrcElement): Boolean {
    if (currentElement == inputElement) return true
    processElement(currentElement)
    return true
  }

  fun processFile(file: BazelrcFile?) {
    file?.virtualFile?.apply {
      seenFiles.add(this).ifTrue {
        file.imports.map(::processElement)
        file.lines.map(::processElement)
      }
    }
  }

  fun processElement(current: BazelrcElement) =
    when (current) {
      is BazelrcLine -> processLine(current)
      is org.jetbrains.bazel.languages.bazelrc.psi.BazelrcImport -> processImport(current)
      else -> {}
    }

  private fun processLine(line: BazelrcLine) {
    (line.containingFile as? BazelrcFile)?.apply {
      line.configName()?.let { results[it] = this }
    }
  }

  private fun processImport(element: org.jetbrains.bazel.languages.bazelrc.psi.BazelrcImport) {
    processFile(element.reference?.resolve() as? BazelrcFile)
  }
}
