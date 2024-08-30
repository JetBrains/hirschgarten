package org.jetbrains.bsp.protocol.utils

import ch.epfl.scala.bsp4j.SourceItem
import com.google.gson.Gson
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import org.jetbrains.bsp.protocol.EnhancedSourceItem

private val gson = Gson()

class EnhancedSourceItemTypeAdapter : TypeAdapter<SourceItem>() {
  override fun write(writer: JsonWriter?, sourceItem: SourceItem?) {
    if (sourceItem is EnhancedSourceItem) {
      gson.toJson(sourceItem, EnhancedSourceItem::class.java, writer)
    } else {
      gson.toJson(sourceItem, SourceItem::class.java, writer)
    }
  }

  override fun read(reader: JsonReader): SourceItem = gson.fromJson(reader, EnhancedSourceItem::class.java)
}
