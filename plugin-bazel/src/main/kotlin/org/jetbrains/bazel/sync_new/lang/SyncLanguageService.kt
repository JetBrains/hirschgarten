package org.jetbrains.bazel.sync_new.lang

import com.intellij.openapi.components.Service
import it.unimi.dsi.fastutil.longs.Long2ObjectMap
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.Reference2LongMap
import it.unimi.dsi.fastutil.objects.Reference2LongOpenHashMap

@Service(Service.Level.APP)
class SyncLanguageService {
  private val tag2Lang: Long2ObjectMap<SyncLanguage> = Long2ObjectOpenHashMap()
  private val tag2Type: Long2ObjectMap<Class<*>> = Long2ObjectOpenHashMap()
  private val type2Tag: Reference2LongMap<Class<*>> = Reference2LongOpenHashMap()
  private val lang2Plugins: MutableMap<SyncLanguage, List<SyncLanguagePlugin<*>>> = mutableMapOf()

  private val _languageDetectors: MutableList<SyncLanguageDetector> = mutableListOf()
  val languageDetectors: List<SyncLanguageDetector>
    get() = _languageDetectors

  init {
    for (plugin in SyncLanguagePlugin.ep.extensionList) {
      for (language in plugin.languages) {
        tag2Lang[language.serialId] = language
        lang2Plugins.compute(language) { _, v ->
          if (v == null) {
            listOf(plugin)
          } else {
            v + plugin
          }
        }
      }

      for (klass in plugin.dataClasses) {
        val annotation = klass.getAnnotation(SyncClassTag::class.java)
          ?: error("SyncClassTag annotation is not found for ${klass.name}")
        tag2Type[annotation.serialId] = klass
        type2Tag[klass] = annotation.serialId
      }

      _languageDetectors.addAll(plugin.languageDetectors)
    }
  }

  fun getLanguageByTag(serialId: Long): SyncLanguage? = tag2Lang[serialId]
  fun getPluginsByLanguage(language: SyncLanguage): List<SyncLanguagePlugin<*>> = lang2Plugins[language] ?: listOf()
  fun getTypeByTag(serialId: Long): Class<*>? = tag2Type[serialId]
  fun getTagByType(type: Class<*>): Long = type2Tag.getLong(type)
}
