package org.jetbrains.bazel.sync.workspace.languages

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.sync.workspace.languages.go.GoLanguagePlugin
import org.jetbrains.bazel.sync.workspace.languages.java.JavaLanguagePlugin
import org.jetbrains.bazel.sync.workspace.languages.java.JdkResolver
import org.jetbrains.bazel.sync.workspace.languages.java.JdkVersionResolver
import org.jetbrains.bazel.sync.workspace.languages.kotlin.KotlinLanguagePlugin
import org.jetbrains.bazel.sync.workspace.languages.protobuf.ProtobufLanguagePlugin
import org.jetbrains.bazel.sync.workspace.languages.python.PythonLanguagePlugin
import org.jetbrains.bazel.sync.workspace.languages.scala.ScalaLanguagePlugin
import org.jetbrains.bazel.sync.workspace.languages.thrift.ThriftLanguagePlugin

@Service(Service.Level.PROJECT)
class LanguagePluginsService {
  val logger = logger<LanguagePluginsService>()
  val registry: MutableMap<LanguageClass, LanguagePlugin<*>> = mutableMapOf()

  val all
    get() = registry.values.toList()

  fun registerDefaultPlugins(bazelPathsResolver: BazelPathsResolver) {
    val javaPlugin =
      JavaLanguagePlugin(bazelPathsResolver, JdkResolver(bazelPathsResolver, JdkVersionResolver()))
        .also(this::registerLangaugePlugin)
    KotlinLanguagePlugin(javaPlugin, bazelPathsResolver).also(this::registerLangaugePlugin)
    ScalaLanguagePlugin(javaPlugin, bazelPathsResolver).also(this::registerLangaugePlugin)
    GoLanguagePlugin(bazelPathsResolver).also(this::registerLangaugePlugin)
    PythonLanguagePlugin(bazelPathsResolver).also(this::registerLangaugePlugin)
    ThriftLanguagePlugin().also(this::registerLangaugePlugin)
    ProtobufLanguagePlugin(javaPlugin).also(this::registerLangaugePlugin)
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

  fun getLanguagePlugin(langs: Set<LanguageClass>): LanguagePlugin<*>? = langs.firstNotNullOfOrNull { getLanguagePlugin(it) }

  inline fun <reified PLUGIN> getLanguagePlugin(lang: LanguageClass): PLUGIN =
    getLanguagePlugin(lang) as? PLUGIN ?: error("cannot cast ${lang.javaClass} to ${PLUGIN::class}")
}
