package configurations.intellijBsp

import configurations.BaseConfiguration
import jetbrains.buildServer.configs.kotlin.v2019_2.BuildSteps
import jetbrains.buildServer.configs.kotlin.v2019_2.FailureConditions
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.script
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.ScriptBuildStep


open class IntellijTestsBuildType(
        name: String,
        setupSteps: Boolean = false,
        steps: BuildSteps.() -> Unit,
        failureConditions: FailureConditions.() -> Unit = {},
        artifactRules: String = ""
) : BaseConfiguration.BaseBuildType(
    name = "[tests] $name",
    vcsRoot = BaseConfiguration.IntellijBspVcs,
    failureConditions = failureConditions,
    artifactRules = artifactRules,
    steps = steps,
    setupSteps = setupSteps,
    requirements =  {
        contains("cloud.amazon.agent-name-prefix", "Linux-Large")
    }
)

object UnitTests : IntellijTestsBuildType(
    name = "unit tests",
    steps = {
        gradle {
            this.name = "run unit tests"
            tasks = "test"
            gradleParams = "-x :probe:test -Pexclude.integration.test=true"
            jdkHome = "%env.JDK_17_0%"
        }
    }
)

object IntegrationTest : IntellijTestsBuildType(
    name = "integration test",
    setupSteps = true,
    steps = {
        gradle {
            this.name = "run integration test"
            tasks = ":test --tests NonOverlappingTest"
            jdkHome = "%env.JDK_17_0%"
        }
    }
)

object IdeProbeTests : IntellijTestsBuildType(
    name = "ide-probe tests",
    setupSteps = true,
    artifactRules = "+:%system.teamcity.build.checkoutDir%/probe/build/reports => reports.zip",
    steps = {
        script {
            this.name = "install ide-probe dependencies"
            scriptContent = """
                #!/bin/bash
                set -euxo pipefail

                #install required cpp and other packages
                sh -c 'echo "deb https://repo.scala-sbt.org/scalasbt/debian /" | tee -a /etc/apt/sources.list.d/sbt.list'  ||:
                curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823" | apt-key add -  ||:
                apt-get update -q ||:
                apt-get install -y libxtst6 ||:
                apt-get install -y libx11-6 ||:
                apt-get install -y libxrender1 ||:
                apt-get install -y xvfb ||:
                apt-get install -y openssh-server ||:
                apt-get install -y python3 ||:
                apt-get install -y python3-pip ||:
                apt-get install -y sbt ||:
                apt-get install -y libssl-dev ||:
                apt-get install -y pkg-config ||:
                apt-get install -y x11-apps ||:
                apt-get install -y imagemagick ||:
            """.trimIndent()
            dockerImage = "ubuntu:focal"
            dockerImagePlatform = ScriptBuildStep.ImagePlatform.Linux
            dockerRunParameters = """
                -v /usr/:/usr/
                -v /etc/:/etc/
                -v /var/:/var/
                -v /tmp/:/tmp/
            """.trimIndent()
        }

        script {
            this.name = "configure ide-probe"
            scriptContent = """
            #!/bin/bash
            set -euxo pipefail

            #turn on virtual display for ide-probe tests
            sed -i '/driver.vmOptions = \[ "-Dgit.process.ignored=false", "-Xms4g", "-Xmx4g" \]/a \\n  driver.display = "xvfb"\n' ./probe-setup/src/main/resources/ideprobe.conf


            #get current version of plugin
            current_version=${'$'}(awk -F '"' '/const val version =/{print ${'$'}2; exit}' buildSrc/src/main/kotlin/versions.kt)

            #replace the plugin version in the ide-probe config with the current version
            sed -i "s/\(bundle = \"intellij-bsp-\).*\(.zip\" }\)/\1${'$'}{current_version}\2/" probe-setup/src/main/resources/ideprobe.conf
        """.trimIndent()
        }

        gradle {
            this.name = "build plugin"
            tasks = "buildPlugin"
            jdkHome = "%env.JDK_17_0%"
        }

        gradle {
            this.name = "run ide-probe tests"
            tasks = ":probe:test"
            jdkHome = "%env.JDK_17_0%"
            jvmArgs = "-Xmx12g"
        }
    },
    failureConditions = {
        supportTestRetry = true
        testFailure = false
        executionTimeoutMin = 0
    }
)