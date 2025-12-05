package org.jetbrains.bazel.sync_new.lang

import com.intellij.openapi.components.Service
import it.unimi.dsi.fastutil.longs.Long2ObjectMap
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.Reference2LongMap
import it.unimi.dsi.fastutil.objects.Reference2LongOpenHashMap

@Service(Service.Level.APP)
class SyncLanguageService {
  private val tag2Lang: Long2ObjectMap<SyncLanguage<*>> = Long2ObjectOpenHashMap()
  private val tag2Type: Long2ObjectMap<Class<*>> = Long2ObjectOpenHashMap()
  private val type2Tag: Reference2LongMap<Class<*>> = Reference2LongOpenHashMap()
  private val lang2Plugins: MutableMap<SyncLanguage<*>, List<SyncLanguagePlugin<*>>> = mutableMapOf()

  init {
    for (plugin in SyncLanguagePlugin.ep.extensionList) {
      val language = plugin.language
      tag2Lang[language.serialId] = language
      lang2Plugins.compute(language) { _, v ->
        if (v == null) {
          listOf(plugin)
        } else {
          v + plugin
        }
      }

      val dataType = plugin.dataType
      val annotation = dataType.getAnnotation(SyncClassTag::class.java)
        ?: error("SyncClassTag annotation is not found for ${dataType.name}")
      tag2Type[annotation.serialId] = dataType
      type2Tag[dataType] = annotation.serialId
    }
  }

  fun getLanguageByTag(serialId: Long): SyncLanguage<*>? = tag2Lang[serialId]
  fun getPluginsByLanguage(language: SyncLanguage<*>): List<SyncLanguagePlugin<*>> = lang2Plugins[language] ?: listOf()
  fun getTypeByTag(serialId: Long): Class<*>? = tag2Type[serialId]
  fun getTagByType(type: Class<*>): Long = type2Tag.getLong(type)
}
