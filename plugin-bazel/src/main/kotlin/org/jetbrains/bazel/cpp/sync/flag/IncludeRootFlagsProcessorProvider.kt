package org.jetbrains.bazel.cpp.sync.flag

import com.intellij.openapi.project.Project
import org.jetbrains.bazel.commons.ExecutionRootPath
import org.jetbrains.bazel.commons.TargetKey
import org.jetbrains.bazel.cpp.sync.ExecutionRootPathResolver
import org.jetbrains.bazel.sync.task.bazelProject
import java.util.regex.Matcher
import java.util.regex.Pattern

// See com.google.idea.blaze.cpp.IncludeRootFlagsProcessor
class IncludeRootFlagsProcessorProvider : BazelCompilerFlagsProcessorProvider {
  class IncludeRootFlagsProcessor internal constructor(val executionRootPathResolver: ExecutionRootPathResolver) :
    BazelCompilerFlagsProcessorProvider.BazelCompilerFlagsProcessor {
      val iFlag: Pattern = Pattern.compile("^-isystem|-I|-iquote$")
      val iFlagCombined: Pattern = Pattern.compile("^(-isystem|-I|-iquote)(.+)$")

      override fun processFlags(flags: List<String>): List<String> {
        val builder: MutableList<String> = ArrayList()
        var previousIFlag: String? = null
        for (flag in flags) {
          if (previousIFlag != null) {
            collectPathFlags(builder, previousIFlag, flag)
            previousIFlag = null
          } else {
            val iflagMatcher: Matcher = iFlag.matcher(flag)
            if (iflagMatcher.matches()) {
              previousIFlag = flag
              continue
            }
            val matcher: Matcher = iFlagCombined.matcher(flag)
            if (matcher.matches()) {
              collectPathFlags(builder, matcher.group(1), matcher.group(2))
              continue
            }
            builder.add(flag)
            previousIFlag = null
          }
        }
        return builder
      }

      private fun collectPathFlags(
        builder: MutableList<String>,
        iflag: String,
        path: String,
      ) {
        val includeDirs =
          executionRootPathResolver.resolveToIncludeDirectories(ExecutionRootPath(path))
        for (f in includeDirs) {
          builder.add(iflag)
          builder.add(f.absolutePath)
        }
      }
    }

  override fun getProcessor(project: Project): IncludeRootFlagsProcessor {
    val resolver =
      ExecutionRootPathResolver(
        project.bazelProject.bazelInfo,
        project.bazelProject.targets
          .map { entry -> TargetKey(entry.key, listOf()) to entry.value }
          .toMap(),
      )
    return IncludeRootFlagsProcessor(resolver)
  }
}
