package configurations.intellijBazel

import configurations.BaseConfiguration
import jetbrains.buildServer.configs.kotlin.v2019_2.BuildSteps
import jetbrains.buildServer.configs.kotlin.v2019_2.FailureConditions
import jetbrains.buildServer.configs.kotlin.v2019_2.Requirements
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.script
import jetbrains.buildServer.configs.kotlin.v2019_2.vcs.GitVcsRoot


open class Tests(
    name: String,
    vcsRoot: GitVcsRoot,
    setupSteps: Boolean = false,
    steps: BuildSteps.() -> Unit,
    failureConditions: FailureConditions.() -> Unit = {},
    artifactRules: String = "",
    requirements: (Requirements.() -> Unit)? = null
) : BaseConfiguration.BaseBuildType(
    name = "[tests] $name",
    vcsRoot = vcsRoot,
    failureConditions = failureConditions,
    artifactRules = artifactRules,
    steps = steps,
    setupSteps = setupSteps,
    requirements = requirements
)

open class UnitTests(
    vcsRoot: GitVcsRoot
) : Tests(
    name = "unit tests",
    vcsRoot = vcsRoot,
    artifactRules = "+:**/build/reports/**/* => reports.zip",
    steps = {
        gradle {
            this.name = "run unit tests"
            tasks = "test"
            gradleParams = "-x :probe:test"
            jdkHome = "%env.JDK_17_0%"
        }
    }
)

open class IdeProbe(
    vcsRoot: GitVcsRoot
) : Tests(
    name = "ide-probe",
    vcsRoot = vcsRoot,
    setupSteps = true,
    requirements = {
        endsWith("cloud.amazon.agent-name-prefix", "-Large")
        equals("container.engine.osType", "linux")
    },
    artifactRules = """
        +:%system.teamcity.build.checkoutDir%/probe/build/reports => reports.zip
        +:/mnt/agent/temp/buildTmp/ide-probe/screenshots => screenshots.zip
    """.trimIndent(),
    steps = {
        script {
            this.name = "install ide-probe dependencies"
            id = "install_ide_probe_dependencies"
            scriptContent = """
                #!/bin/bash
                set -euxo pipefail

                #install required cpp and other packages
                sudo sh -c 'echo "deb https://repo.scala-sbt.org/scalasbt/debian /" | tee -a /etc/apt/sources.list.d/sbt.list'  ||:
                sudo curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823" | apt-key add -  ||:
                sudo apt-get update -q ||:
                sudo apt-get install -y libxtst6 ||:
                sudo apt-get install -y libx11-6 ||:
                sudo apt-get install -y libxrender1 ||:
                sudo apt-get install -y xvfb ||:
                sudo apt-get install -y openssh-server ||:
                sudo apt-get install -y python3 ||:
                sudo apt-get install -y python3-pip ||:
                sudo apt-get install -y sbt ||:
                sudo apt-get install -y libssl-dev ||:
                sudo apt-get install -y pkg-config ||:
                sudo apt-get install -y x11-apps ||:
                sudo apt-get install -y imagemagick ||:
            """.trimIndent()
        }

        script {
            this.name = "configure ide-probe"
            id = "configure_ide_probe"
            scriptContent = """
                #!/bin/bash
                set -euxo pipefail

                #turn on virtual display for ide-probe tests
                sed -i '/driver.vmOptions = \[ "-Dgit.process.ignored=false", "-Xms4g", "-Xmx12g" \]/a \\n  driver.display = "xvfb"\n' ./probe-setup/src/main/resources/ideprobe.conf
            """.trimIndent()
        }

        gradle {
            this.name = "build plugin"
            id = "build_plugin"
            tasks = "buildPlugin"
            jdkHome = "%env.JDK_17_0%"
        }

        gradle {
            this.name = "run ide-probe tests"
            id = "run_ide_probe_tests"
            tasks = ":probe:test --tests SingleProbeTests"
            gradleParams = "-Dorg.gradle.jvmargs=-Xmx12g"
            jdkHome = "%env.JDK_17_0%"
            jvmArgs = "-Xmx12g"
        }
    },
    failureConditions = {
        supportTestRetry = true
        testFailure = true
        executionTimeoutMin = 180
    }
)

object UnitTestsGitHub : UnitTests(
    vcsRoot = BaseConfiguration.GitHubVcs
)

object UnitTestsSpace : UnitTests(
    vcsRoot = BaseConfiguration.SpaceVcs
)

object IdeProbeGitHub : IdeProbe(
    vcsRoot = BaseConfiguration.GitHubVcs
)

object IdeProbeSpace : IdeProbe(
    vcsRoot = BaseConfiguration.SpaceVcs
)