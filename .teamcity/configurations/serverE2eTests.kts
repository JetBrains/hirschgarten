package configurations

import jetbrains.buildServer.configs.kotlin.v2019_2.FailureConditions
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.BazelStep
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.bazel
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.script

// Base class for E2E test builds
open class E2ETest(
  targets: String,
  failureConditions: FailureConditions.() -> Unit = {},
  androidTest: Boolean = false
) : BaseBuildType(
  name = "[e2e tests] $targets",
  failureConditions = failureConditions,
  artifactRules = Utils.CommonParams.BazelTestlogsArtifactRules,
  requirements = if (androidTest) {
    {
      endsWith("cloud.amazon.agent-name-prefix", "Ubuntu-22.04-Android")
      equals("container.engine.osType", "linux")
    }
  } else null,
  steps = {
    bazel {
      this.name = "test $targets"
      this.command = "test"
      this.targets = targets
      // This fixes FileUtils.getCacheDirectory in integration tests
      this.arguments =
        "--sandbox_writable_path=/home/hirschuser/.cache ${Utils.CommonParams.BazelCiSpecificArgs}"
      logging = BazelStep.Verbosity.Diagnostic
      toolPath = "/usr/local/bin"
      Utils.DockerParams.get().forEach { (key, value) ->
        param(key, value)
      }
    }
    script {
      id = "simpleRunner"
      scriptContent =
        """
          #!/bin/bash
          set -euxo
          
          cp -R /home/teamcity/agent/system/.persistent_cache/bazel/_bazel_hirschuser/*/execroot/_main/bazel-out/k8-fastbuild/testlogs .
          """.trimIndent()
    }
  }
)

// Define all E2E test objects
object ServerE2eTests {
  object SampleRepo : E2ETest(
    targets = "//server/integration/tests/src/test/kotlin/org/jetbrains/bsp/bazel:sampleRepoTest"
  )

  object LocalJdk : E2ETest(
    targets = "//server/integration/tests/src/test/kotlin/org/jetbrains/bsp/bazel:localJdkTest"
  )

  object RemoteJdk : E2ETest(
    targets = "//server/integration/tests/src/test/kotlin/org/jetbrains/bsp/bazel:remoteJdkTest"
  )

  object ServerDownloadsBazelisk : E2ETest(
    targets = "//server/integration/tests/src/test/kotlin/org/jetbrains/bsp/bazel:serverDownloadsBazeliskTest",
    failureConditions = {
      executionTimeoutMin = 120
    }
  )

  object KotlinProject : E2ETest(
    targets = "//server/integration/tests/src/test/kotlin/org/jetbrains/bsp/bazel:kotlinProjectTest"
  )

  object GoProject : E2ETest(
    targets = "//server/integration/tests/src/test/kotlin/org/jetbrains/bsp/bazel:goProjectTest"
  )

  object AndroidProject : E2ETest(
    targets = "//server/integration/tests/src/test/kotlin/org/jetbrains/bsp/bazel:androidProjectTest",
    failureConditions = {
      executionTimeoutMin = 120
    },
    androidTest = true
  )

  object AndroidKotlinProject : E2ETest(
    targets = "//server/integration/tests/src/test/kotlin/org/jetbrains/bsp/bazel:androidKotlinProjectTest",
    failureConditions = {
      executionTimeoutMin = 120
    },
    androidTest = true
  )

  object ScalaProject : E2ETest(
    targets = "//server/integration/tests/src/test/kotlin/org/jetbrains/bsp/bazel:scalaProjectTest"
  )

  object PythonProject : E2ETest(
    targets = "//server/integration/tests/src/test/kotlin/org/jetbrains/bsp/bazel:pythonProjectTest"
  )

  object JavaDiagnostics : E2ETest(
    targets = "//server/integration/tests/src/test/kotlin/org/jetbrains/bsp/bazel:javaDiagnosticsTest"
  )

  object ManualTargets : E2ETest(
    targets = "//server/integration/tests/src/test/kotlin/org/jetbrains/bsp/bazel:manualTargetsTest"
  )

  object BuildSync : E2ETest(
    targets = "//server/integration/tests/src/test/kotlin/org/jetbrains/bsp/bazel:buildTargetsSyncTest"
  )

  object FirstPhaseSync : E2ETest(
    targets = "//server/integration/tests/src/test/kotlin/org/jetbrains/bsp/bazel:firstPhaseSyncTest"
  )

  object PartialSync : E2ETest(
    targets = "//server/integration/tests/src/test/kotlin/org/jetbrains/bsp/bazel:partialSyncTest"
  )

  object ExternalAutoloads : E2ETest(
    targets = "//server/integration/tests/src/test/kotlin/org/jetbrains/bsp/bazel:externalAutoloadsProjectTest"
  )

  object NestedModules : E2ETest(
    targets = "//server/integration/tests/src/test/kotlin/org/jetbrains/bsp/bazel:nestedModulesProjectTest"
  )
}