package org.jetbrains.bsp.testkit

import com.google.common.collect.Maps
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import org.junit.jupiter.api.Assertions.assertTrue
import java.lang.reflect.Type
import java.util.*

object JsonComparator {
  private val gson = GsonBuilder().create()
  private val mapType: Type = object : TypeToken<Map<String?, Any?>?>() {}.type

  fun <T> assertJsonEquals(expected: T, actual: T, typeOfT: Type) {
    val expectedObject = gson.toJsonTree(expected, typeOfT)
    val actualObject = gson.toJsonTree(actual, typeOfT)

    val sortedExpected = deepSort(expectedObject)
    val sortedActual = deepSort(actualObject)

    val expectedMap = gson.fromJson<Map<String, Any>>(sortedExpected, mapType)
    val actualMap = gson.fromJson<Map<String, Any>>(sortedActual, mapType)

    val difference = Maps.difference(FlatMapUtils.flatten(expectedMap), FlatMapUtils.flatten(actualMap))

    assertTrue(difference.areEqual()) { "Expected: $sortedExpected\n\n" +
      "Actual: $sortedActual\n\n" +
      "Entries only in expected \n${difference.entriesOnlyOnLeft()}\n\n" +
      "Entries only in actual\n${difference.entriesOnlyOnRight()}\n\n" +
      "Entries differing\n${difference.entriesDiffering()}" }
  }

  fun deepSort(element: JsonElement): JsonElement {
    val comparator: Comparator<JsonElement> = JsonElementComparator
    return when {
      element.isJsonArray -> {
        val array = element.asJsonArray
        val treeSet = TreeSet(comparator)
        array.forEach { element -> treeSet.add(deepSort(element)) }
        val sortedArray = JsonArray()
        treeSet.forEach { element -> sortedArray.add(element) }
        sortedArray
      }
      element.isJsonObject -> {
        val map = element.asJsonObject
        val treeSet = TreeSet<Pair<String, JsonElement>>(Comparator.comparing { it.first })
        map.entrySet().forEach { entry -> treeSet.add(Pair(entry.key, deepSort(entry.value))) }
        val sortedMap = JsonObject()
        treeSet.forEach { entry -> sortedMap.add(entry.first, entry.second) }
        sortedMap
      }
      else -> {
        element
      }
    }
  }
}

// FIXME: this is WILDLY inefficient
object JsonElementComparator : Comparator<JsonElement> {
  override fun compare(t: JsonElement, t1: JsonElement): Int {
    return t.toString().compareTo(t1.toString())
  }
}