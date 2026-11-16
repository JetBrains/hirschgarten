package org.jetbrains.bazel.clion

import com.jetbrains.cidr.execution.testing.CidrTestScopeElement
import com.jetbrains.cidr.execution.testing.CidrTestScopeElementImpl
import com.jetbrains.cidr.execution.testing.tcatch.CidrCatchTestElements
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.bazel.clion.test.catch.CatchTestSupport
import org.junit.jupiter.api.Test

class CatchTestSupportTest {
  @Test
  fun `single test selects exactly itself`() {
    assertThat(createTestFilter(catch("Test0"))).isEqualTo("Test0")
  }

  @Test
  fun `multiple tests are comma separated`() {
    assertThat(createTestFilter(catch("Test0"), catch("Test1"))).isEqualTo("Test0,Test1")
  }

  @Test
  fun `no tests select nothing`() {
    assertThat(createTestFilter()).isNull()
  }

  @Test
  fun `test with comma works`() {
    assertThat(createTestFilter(catch("works, with comma"))).isEqualTo("works\\, with comma")
  }

  @Test
  fun `section selects its whole test case`() {
    assertThat(createTestFilter(section("[Sections][outer][inner\\, with comma]"))).isEqualTo("Sections")
  }

  @Test
  fun `sections of several tests select their whole test cases`() {
    assertThat(createTestFilter(section("[Sections][fails]"), section("[Sections][passes]"), section("[Other][one]")))
      .isEqualTo("Sections,Other")
  }

  private fun createTestFilter(vararg tests: CidrTestScopeElement): String? {
    return CatchTestSupport().createTestFilter(tests.toList())
  }

  private fun catch(test: String?) = CidrCatchTestElements.createTestScopeElement("non-tagged", test)

  private fun section(path: String): CidrTestScopeElement =
    CidrTestScopeElementImpl.createTestScopeWithPatternAndConfigurationName(path, "section-run($path)") { error("not used") }
}
