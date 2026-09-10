package org.jetbrains.bazel.sync

import com.intellij.testFramework.common.timeoutRunBlocking
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.commons.BzlmodRepoMapping
import org.jetbrains.bazel.label.Canonical
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.Main
import org.jetbrains.bazel.label.RepoType
import org.jetbrains.bazel.server.connection
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelProjectFixture
import org.junit.jupiter.api.Test
import kotlin.io.path.Path
import kotlin.time.Duration.Companion.minutes

private const val DEP_REPO = "dep_repo+"

@BazelTestApplication
class BuildfilesQueryTest {

  private val project by bazelProjectFixture("redcodes/partial_sync_repo", projectView = ".bazelproject")

  private val universeRepos: Set<RepoType> = setOf(Main, Canonical.createCanonicalOrMain(DEP_REPO))

  private suspend fun buildfilesOf(vararg paths: String): List<Label> =
    project.connection.runWithServer { server ->
      BuildfilesQuery.findDependantBuildFiles(
        server = server,
        workspaceRelativePaths = paths.map { Path(it) }.toSet(),
        universeRepos = universeRepos,
        repoMapping = BzlmodRepoMapping(
          canonicalRepoNameToLocalPath = mapOf(DEP_REPO to Path("/tmp/dep_repo")),
          apparentRepoNameToCanonicalName = mapOf(DEP_REPO to "dep_repo"),
          canonicalRepoNameToPath = mapOf(DEP_REPO to Path("/tmp/dep_repo")),
          nonLocalCanonicalRepoNames = emptySet()
        )
      )
    }

  @Test
  fun `query gives the packages of the BUILD files that load a Starlark file`(): Unit =
    timeoutRunBlocking(timeout = 10.minutes) {
      buildfilesOf("rules/java_rules.bzl") shouldBe listOf(Label.parse("//extra:all"))
      buildfilesOf("dep_repo/rules/java_rules.bzl") shouldContainAll
        listOf(Label.parse("@@$DEP_REPO//lib:all"), Label.parse("@@$DEP_REPO//util:all"))
      buildfilesOf("docs/orphan_rules.bzl").shouldBeEmpty()

      buildfilesOf().shouldBeEmpty()
    }
}
