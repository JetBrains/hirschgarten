package org.jetbrains.bazel.sync.workspace.languages

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.sync.workspace.languages.go.GoLanguagePlugin
import org.jetbrains.bazel.sync.workspace.languages.java.JavaLanguagePlugin
import org.jetbrains.bazel.sync.workspace.languages.java.JdkResolver
import org.jetbrains.bazel.sync.workspace.languages.kotlin.KotlinLanguagePlugin
import org.jetbrains.bazel.sync.workspace.languages.protobuf.ProtobufLanguagePlugin
import org.jetbrains.bazel.sync.workspace.languages.python.PythonLanguagePlugin
import org.jetbrains.bazel.sync.workspace.languages.scala.ScalaLanguagePlugin
import org.jetbrains.bazel.sync.workspace.languages.thrift.ThriftLanguagePlugin
import org.jetbrains.bazel.sync.workspace.languages.ultimate.UltimateLanguagePlugin

@Service(Service.Level.PROJECT)
class LanguagePluginsService {
  val logger = logger<LanguagePluginsService>()
  val registry: MutableMap<LanguageClass, LanguagePlugin<*>> = mutableMapOf()

  // target data can have only one language data
  // jvm language plugins include base JavaInfo into their model BUT you have to call their plugin first
  // TODO: allow BuildTarget have multiple languages data - it is doable I've had done that but haven't merged
  val languagePriority: List<LanguageClass> =
    listOf(
      LanguageClass.PROTOBUF,
      LanguageClass.KOTLIN,
      LanguageClass.SCALA,
      LanguageClass.JAVA,
      LanguageClass.THRIFT,
      LanguageClass.PYTHON,
      LanguageClass.GO,
      LanguageClass.ULTIMATE,
    )

  val all
    get() = registry.values.toList()

  fun registerDefaultPlugins(bazelPathsResolver: BazelPathsResolver, jvmPackageResolver: JvmPackageResolver) {
    val javaPlugin =
      JavaLanguagePlugin(bazelPathsResolver, JdkResolver(bazelPathsResolver), jvmPackageResolver)
        .also(this::registerLangaugePlugin)
    KotlinLanguagePlugin(javaPlugin, bazelPathsResolver).also(this::registerLangaugePlugin)
    ScalaLanguagePlugin(javaPlugin, bazelPathsResolver, jvmPackageResolver).also(this::registerLangaugePlugin)
    GoLanguagePlugin(bazelPathsResolver).also(this::registerLangaugePlugin)
    PythonLanguagePlugin(bazelPathsResolver).also(this::registerLangaugePlugin)
    ThriftLanguagePlugin().also(this::registerLangaugePlugin)
    ProtobufLanguagePlugin(javaPlugin).also(this::registerLangaugePlugin)
    UltimateLanguagePlugin().also(this::registerLangaugePlugin)
  }

  private fun registerLangaugePlugin(plugin: LanguagePlugin<*>) {
    for (language in plugin.getSupportedLanguages()) {
      if (this.registry.contains(language)) {
        logger.warn("Language plugin already registered for class: $language")
        continue
      }
      registry[language] = plugin
    }
  }

  fun getLanguagePlugin(lang: LanguageClass): LanguagePlugin<*>? = registry[lang]

  fun getLanguagePlugin(langs: Set<LanguageClass>): LanguagePlugin<*>? =
    languagePriority
      .asSequence()
      .filter { langs.contains(it) }
      .firstNotNullOfOrNull { registry[it] }
      ?.let { return it }

  inline fun <reified PLUGIN> getLanguagePlugin(lang: LanguageClass): PLUGIN =
    getLanguagePlugin(lang) as? PLUGIN ?: error("cannot cast ${lang.javaClass} to ${PLUGIN::class}")
}
