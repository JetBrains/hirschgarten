package org.jetbrains.bazel.sync.target

import com.intellij.testFramework.junit5.TestApplication
import org.jetbrains.bazel.sync.workspace.languages.LanguagePlugin
import org.jetbrains.bsp.protocol.ClassDiscriminator
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass

@TestApplication
internal class BuildTargetDiscriminatorTest {

  @Test
  fun `test known unique class discriminators`() {
    val buildTargetTypes = LanguagePlugin.EP_NAME.extensionList.flatMap { it.providedBuildTargetTypes }
    val typesById = buildTargetTypes.groupBy { it.findClassDiscriminator() }
    for ((id, types) in typesById) {
      check(id != null) {
        "${types.joinToString(separator = ", ") { "`${it.qualifiedName}`" }} doesn't have discriminator" +
        " assigned, use @${ClassDiscriminator::class.simpleName}"
      }

      check(types.size == 1) {
        "`${id}` class discriminator is shared among ${types.joinToString(separator = ", ") { "`${it.qualifiedName}`" }}"
      }
    }
  }

  private fun KClass<*>.findClassDiscriminator(): Int? =
    this.annotations.filterIsInstance<ClassDiscriminator>()
      .map { it.id.toInt() }
      .firstOrNull()
}
