package org.jetbrains.bsp.probe.test

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.virtuslab.ideprobe.IntelliJFixture
import org.virtuslab.ideprobe.ide.intellij.InstalledIntelliJ
import scala.Tuple3
import java.nio.file.Path

/**
 * This is a sample to demonstrate how to create a suite that runs multiple tests on one installed intellij
 * instead of setting it up for every one separately.
 *
 *  @Test
 *  fun `open project`() {
 *     with(runner) {
 *       runIntellij(runData) {
 *         val data = openProject(it, Option.apply(null))
 *         val robot = data._2
 *         ...
 *         Test logic goes here
 *         ..
 *         BoxedUnit.UNIT
 *       }
 *     }
 *   }
 */
class MultipleProbeTests {

  private val runData by lazy {
    Tuple3(fixture, path, intellij)
  }

  companion object {

    private lateinit var fixture: IntelliJFixture
    private lateinit var runner: IdeProbeTestRunner
    private lateinit var path: Path
    private lateinit var intellij: InstalledIntelliJ

    /**
     * Replaces repository and branch values with correct ones
     */
    @BeforeAll
    @JvmStatic
    fun setupIntellij() {
      runner = IdeProbeTestRunner()
      fixture = runner.fixtureWithWorkspaceFromGit(
        "repository",
        "branch"
      ).withBuild("232.5150.99")
      val data = runner.prepareInstance(fixture)
      path = data._2()
      intellij = data._3()
    }

    /**
     * Cleans up after all tests are executed
     */
    @AfterAll
    @JvmStatic
    fun teardownIntellij() {
      runner.cleanInstance(Tuple3(fixture, path, intellij))
    }
  }
}