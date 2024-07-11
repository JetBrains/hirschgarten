@file:Suppress("UnusedPrivateMember")

package org.jetbrains.bsp.probe.test

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.virtuslab.ideprobe.ide.intellij.InstalledIntelliJ
import scala.Tuple2
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

  companion object {
    private lateinit var runner: IdeProbeTestRunner
    private lateinit var installedIntellij: Tuple2<Path, InstalledIntelliJ>

    /**
     * Replaces repository and branch values with correct ones
     */
    @BeforeAll
    @JvmStatic
    fun setupIntellij() {
      runner = IdeProbeTestRunner(
        "https://github.com/JetBrains/bazel-bsp.git",
        "3.1.0"
      )
      installedIntellij = runner.prepareInstance()
    }

    /**
     * Cleans up after all tests are executed
     */
    @AfterAll
    @JvmStatic
    fun teardownIntellij() {
      runner.cleanInstance(installedIntellij)
    }
  }

  /*@Test
  fun `open project twice`() {
    with(runner) {
      runIntellij(installedIntellij) {
        openProject(it)
        BoxedUnit.UNIT
      }

      runIntellij(installedIntellij) {
        openProject(it)
        BoxedUnit.UNIT
      }
    }
  }*/
}

