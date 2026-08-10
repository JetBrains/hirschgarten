package org.jetbrains.bazel.sync.workspace.importer

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.jetbrains.bazel.commons.RepoMappingDisabled
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.workspace.importer.GlobalNamingContext.NameProducer
import org.jetbrains.bazel.sync.workspace.importer.GlobalNamingContext.NameSpace
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceAspectIds
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceConfigurationId
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.junit.jupiter.api.Test

@BazelTestApplication
internal class TargetNamingContextTest {
  @Test
  fun `a label with one configuration keeps the plain name, several configurations all decorate`() {
    val keys = listOf(
      key("//foo:bar", config = "aaaaaaa"),
      key("//foo:bar", config = "bbbbbbb"),
      key("//foo:lonely"),
    )

    val resolved = resolve(NameSpace.MODULE, keys)

    resolved.values.toSet() shouldBe setOf("foo.bar-aaaaaaa", "foo.bar-bbbbbbb", "foo.lonely")
  }

  @Test
  fun `library names decorate with as much of the key as it takes to separate them`() {
    val one = key("//dep:one")
    val twoA = key("//dep:two", config = "aaaaaaa")
    val twoB = key("//dep:two", config = "bbbbbbb")
    val three = key("//dep:three")
    val threeAspected = key("//dep:three", aspects = listOf("intellij-info"))

    val resolved = resolve(NameSpace.LIBRARY, listOf(one, twoA, twoB, three, threeAspected))

    resolved.getValue(one) shouldBe "dep.one"
    resolved.getValue(twoA) shouldBe "dep.two-aaaaaaa"
    resolved.getValue(twoB) shouldBe "dep.two-bbbbbbb"
    resolved.getValue(three) shouldBe "dep.three"
    resolved.getValue(threeAspected) shouldNotBe "dep.three"
  }

  @Test
  fun `resolution does not depend on declaration order`() {
    val keys = listOf(
      key("//foo:bar", config = "aaaaaaa"),
      key("//foo:bar", config = "bbbbbbb"),
      key("//foo/baz:qux"),
      key("//:root"),
    )

    resolve(NameSpace.MODULE, keys.reversed()) shouldBe resolve(NameSpace.MODULE, keys)
  }

  @Test
  fun `a name contested by two producers is left to neither, an uncontested one is kept`() {
    val key = key("//foo:bar")

    val contested = buildContext {
      declare(NameSpace.MODULE, JVM, key)
      declare(NameSpace.MODULE, JVM, key)
      declare(NameSpace.MODULE, PYTHON, key)
    }
    contested.find(NameSpace.MODULE, JVM, key) shouldBe "foo.bar-jvm"
    contested.find(NameSpace.MODULE, PYTHON, key) shouldBe "foo.bar-python"

    val alone = buildContext { declare(NameSpace.MODULE, PYTHON, key) }
    alone.find(NameSpace.MODULE, PYTHON, key) shouldBe "foo.bar"
  }

  @Test
  fun `a target claimed by two producers under two configurations stays readable`() {
    val first = key("//foo:bar", config = "aaaaaaa")
    val second = key("//foo:bar", config = "bbbbbbb")
    val context = buildContext {
      declare(NameSpace.MODULE, JVM, first)
      declare(NameSpace.MODULE, JVM, second)
      declare(NameSpace.MODULE, PYTHON, first)
      declare(NameSpace.MODULE, PYTHON, second)
    }

    context.find(NameSpace.MODULE, JVM, first) shouldBe "foo.bar-aaaaaaa-jvm"
    context.find(NameSpace.MODULE, JVM, second) shouldBe "foo.bar-bbbbbbb-jvm"
    context.find(NameSpace.MODULE, PYTHON, first) shouldBe "foo.bar-aaaaaaa-python"
    context.find(NameSpace.MODULE, PYTHON, second) shouldBe "foo.bar-bbbbbbb-python"
  }

  @Test
  fun `modules and libraries do not compete for names`() {
    val key = key("//foo:bar")
    val context = buildContext {
      declare(NameSpace.MODULE, JVM, key)
      declare(NameSpace.LIBRARY, JVM, key)
    }

    context.find(NameSpace.MODULE, JVM, key) shouldBe "foo.bar"
    context.find(NameSpace.LIBRARY, JVM, key) shouldBe "foo.bar"
  }

  @Test
  fun `a preferred name is used as is when nothing else claims it`() {
    val key = key("//pkg:commons")

    val context = buildContext { declare(NameSpace.MODULE, JVM, key, preferred = "intellij.bazel.commons") }

    context.find(NameSpace.MODULE, JVM, key) shouldBe "intellij.bazel.commons"
  }

  @Test
  fun `two entities preferring one name both step aside onto the default candidates`() {
    val bar = key("//foo:bar")
    val qux = key("//foo:qux")

    val context = buildContext {
      declare(NameSpace.MODULE, JVM, bar, preferred = "shared.module.name")
      declare(NameSpace.MODULE, JVM, qux, preferred = "shared.module.name")
    }

    context.find(NameSpace.MODULE, JVM, bar) shouldBe "foo.bar"
    context.find(NameSpace.MODULE, JVM, qux) shouldBe "foo.qux"
  }

  @Test
  fun `a mirror that leaves the shared name alone lets the primary target keep it`() {
    val lib = key("//pkg:commons")
    val mirror = key("//pkg:commons_test_lib")

    val context = buildContext {
      declare(NameSpace.MODULE, JVM, lib, preferred = "intellij.bazel.commons")
      declare(NameSpace.MODULE, JVM, mirror, preferred = "intellij.bazel.commons-test")
    }

    context.find(NameSpace.MODULE, JVM, lib) shouldBe "intellij.bazel.commons"
    context.find(NameSpace.MODULE, JVM, mirror) shouldBe "intellij.bazel.commons-test"
  }

  @Test
  fun `an entity without a preferred name still gets the default one`() {
    val preferred = key("//foo:bar")
    val plain = key("//foo:qux")

    val context = buildContext {
      declare(NameSpace.MODULE, JVM, preferred, preferred = "preferred.name")
      declare(NameSpace.MODULE, JVM, plain)
    }

    context.find(NameSpace.MODULE, JVM, preferred) shouldBe "preferred.name"
    context.find(NameSpace.MODULE, JVM, plain) shouldBe "foo.qux"
  }

  @Test
  fun `a preferred name colliding with another entity default name is left to neither`() {
    val bar = key("//foo:bar")
    val qux = key("//foo:qux")

    val context = buildContext {
      declare(NameSpace.MODULE, JVM, bar)
      declare(NameSpace.MODULE, JVM, qux, preferred = "foo.bar")
    }

    context.find(NameSpace.MODULE, JVM, bar) shouldBe "foo.bar-jvm"
    context.find(NameSpace.MODULE, JVM, qux) shouldBe "foo.qux"
  }

  @Test
  fun `two labels that collapse onto one name fall back to a hash rather than a counter`() {
    // both labels sanitize to `foo.a-b`, so every default candidate of the two collides
    val dotted = key("//foo:a.b")
    val dashed = key("//foo:a-b")

    val context = buildContext {
      declare(NameSpace.MODULE, JVM, dotted)
      declare(NameSpace.MODULE, JVM, dashed)
    }

    val dottedName = requireNotNull(context.find(NameSpace.MODULE, JVM, dotted))
    val dashedName = requireNotNull(context.find(NameSpace.MODULE, JVM, dashed))

    dottedName.startsWith("foo.a-b-") shouldBe true
    dashedName.startsWith("foo.a-b-") shouldBe true
    dottedName shouldNotBe dashedName
    dottedName.endsWith("-1") shouldBe false
    dashedName.endsWith("-2") shouldBe false
  }

  @Test
  fun `an undeclared entity resolves to null`() {
    val context = buildContext { declare(NameSpace.MODULE, JVM, key("//foo:bar")) }

    context.find(NameSpace.MODULE, JVM, key("//never:declared")) shouldBe null
    context.find(NameSpace.MODULE, PYTHON, key("//foo:bar")) shouldBe null
    context.find(NameSpace.LIBRARY, JVM, key("//foo:bar")) shouldBe null
  }

  @Test
  fun `findOrFallback resolves declared entities and decorates undeclared ones clear of them`() {
    val declared = key("//foo:bar")
    val undeclared = key("//foo:bar", config = "aaaaaaa")
    val other = key("//foo:bar", config = "bbbbbbb")
    val context = buildContext { declare(NameSpace.MODULE, JVM, declared, preferred = "preferred.name") }

    context.findOrFallback(NameSpace.MODULE, JVM, declared) shouldBe "preferred.name"

    // an undeclared entity has no preferred name, so its fallback is built from the default one
    val fallback = context.findOrFallback(NameSpace.MODULE, JVM, undeclared)
    fallback.startsWith("foo.bar-") shouldBe true
    context.findOrFallback(NameSpace.MODULE, JVM, undeclared) shouldBe fallback
    context.findOrFallback(NameSpace.MODULE, JVM, other) shouldNotBe fallback
  }

  private fun buildContext(declare: GlobalNamingContextBuilder.() -> Unit): GlobalNamingContext =
    GlobalNamingContextBuilder.create(RepoMappingDisabled).apply(declare).build()

  private fun resolve(space: NameSpace, keys: List<WorkspaceTargetKey>): Map<WorkspaceTargetKey, String> {
    val context = buildContext { keys.forEach { declare(space, JVM, it) } }
    return keys.distinct().associateWith { requireNotNull(context.find(space, JVM, it)) }
  }

  private fun key(
    label: String,
    config: String? = null,
    aspects: List<String> = emptyList(),
  ): WorkspaceTargetKey = WorkspaceTargetKey(
    label = Label.parse(label),
    configuration = WorkspaceConfigurationId.of(config),
    aspectIds = WorkspaceAspectIds.of(aspects),
  )

  private companion object {
    private val JVM = NameProducer(id = "jvm")
    private val PYTHON = NameProducer(id = "python")
  }
}
