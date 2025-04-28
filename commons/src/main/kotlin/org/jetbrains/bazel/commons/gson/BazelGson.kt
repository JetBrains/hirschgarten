package org.jetbrains.bazel.commons.gson

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import org.jetbrains.bazel.label.Label
import java.nio.file.Path
import kotlin.jvm.internal.Reflection
import kotlin.reflect.KClass

val bazelGson: Gson =
  GsonBuilder()
    .registerTypeHierarchyAdapter(Path::class.java, PathSerializer)
    .registerTypeHierarchyAdapter(Label::class.java, LabelSerializer)
    .registerTypeAdapterFactory(SealedClassTypeAdapterFactory())
    .create()

class SealedClassTypeAdapterFactory : TypeAdapterFactory {
  companion object {
    private const val TYPE_FIELD = "type"
  }

  override fun <T> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
    val rawType = type.rawType

    // Check if this is a Kotlin sealed class
    val kClass = getKClass(rawType) ?: return null
    if (!kClass.isSealed) return null

    // Get all subclasses of the sealed class
    val subclasses = kClass.sealedSubclasses
    if (subclasses.isEmpty()) return null

    // Create a map of class name to class for deserialization lookup
    val nameToSubclass =
      subclasses.associateBy {
        it.qualifiedName ?: it.simpleName ?: it.toString()
      }

    // Create delegate adapters for each subclass to avoid stack overflow
    val subclassAdapters =
      nameToSubclass.entries.associate { (name, kClass) ->
        name to gson.getDelegateAdapter(this, TypeToken.get(kClass.java))
      }

    // Create a delegate adapter for the base type
    val delegateAdapter = gson.getDelegateAdapter(this, type)

    @Suppress("UNCHECKED_CAST")
    return object : TypeAdapter<T>() {
      override fun write(out: JsonWriter, value: T?) {
        if (value == null) {
          out.nullValue()
          return
        }

        val valueClass = value!!::class // Non-null assertion to fix nullable type warning
        val className = valueClass.qualifiedName ?: valueClass.simpleName ?: valueClass.toString()

        // Start writing object
        out.beginObject()
        out.name(TYPE_FIELD)
        out.value(className)

        // Get adapter for value's actual type and write its fields
        val jsonObject = gson.toJsonTree(value).asJsonObject
        // Remove type field if it exists (to avoid duplication)
        jsonObject.remove(TYPE_FIELD)

        // Write all remaining properties
        for ((propName, propValue) in jsonObject.entrySet()) {
          out.name(propName)
          gson.toJson(propValue, out)
        }

        out.endObject()
      }

      override fun read(reader: JsonReader): T? {
        if (reader.peek() == JsonToken.NULL) {
          reader.nextNull()
          return null
        }

        // Only parse enough to get the type
        if (reader.peek() != JsonToken.BEGIN_OBJECT) {
          return delegateAdapter.read(reader)
        }

        reader.beginObject()
        var className: String? = null
        var jsonObject = com.google.gson.JsonObject()

        while (reader.hasNext()) {
          val name = reader.nextName()
          if (name == TYPE_FIELD) {
            className = reader.nextString()
          } else {
            // Read property value as JsonElement
            val value =
              com.google.gson.JsonParser
                .parseReader(reader)
            jsonObject.add(name, value)
          }
        }
        reader.endObject()

        if (className != null) {
          val adapter = subclassAdapters[className]
          if (adapter != null) {
            // Add type info back
            jsonObject.addProperty(TYPE_FIELD, className)
            // Use the specifically fetched adapter, not going through gson.fromJson
            @Suppress("UNCHECKED_CAST")
            return adapter.fromJsonTree(jsonObject) as T
          }
        }

        // Fallback: deserialize as the base type
        @Suppress("UNCHECKED_CAST")
        return delegateAdapter.fromJsonTree(jsonObject) as T
      }
    }
  }

  private fun getKClass(javaClass: Class<*>): KClass<*>? =
    try {
      Reflection.createKotlinClass(javaClass)
    } catch (e: Exception) {
      null
    }
}

object LabelSerializer : TypeAdapter<Label?>() {
  override fun read(jsonReader: JsonReader): Label? = jsonReader.nextString().let { Label.parseOrNull(it) }

  override fun write(out: JsonWriter, value: Label?) {
    out.value(value.toString())
  }
}

object PathSerializer : TypeAdapter<Path?>() {
  override fun read(jsonReader: JsonReader): Path? = jsonReader.nextString()?.let { Path.of(it) }

  override fun write(out: JsonWriter, value: Path?) {
    out.value(value.toString())
  }
}
