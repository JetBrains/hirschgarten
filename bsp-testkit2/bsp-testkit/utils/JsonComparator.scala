package org.jetbrains.bsp.testkit
package utils

import com.google.common.collect.Maps
import com.google.gson.{Gson, GsonBuilder, JsonArray, JsonElement, JsonObject}
import com.google.gson.reflect.TypeToken
import org.junit.jupiter.api.Assertions.assertTrue

import java.lang.reflect.Type
import java.util.Comparator
import scala.collection.mutable

object JsonComparator {
  private val gson = new GsonBuilder().create()
  private val mapType: Type = new TypeToken[java.util.Map[String, Object]]() {}.getType

  private val jsonElementType: Type = new TypeToken[JsonElement]() {}.getType

  def assertJsonEquals[T](expected: T, actual: T, transformExpected: String => String, transformActual: String => String): Unit = {
    val t = new TypeToken[T]() {}.getType
    val expectedJson = transformExpected(gson.toJson(expected, t))
    val actualJson = transformActual(gson.toJson(actual, t))

    val expectedObject = gson.fromJson[JsonElement](expectedJson, jsonElementType)
    val actualObject = gson.fromJson[JsonElement](actualJson, jsonElementType)

    val sortedExpected = deepSort(expectedObject)
    val sortedActual = deepSort(actualObject)

    val expectedMap = gson.fromJson[java.util.Map[String, Object]](sortedExpected, mapType)
    val actualMap = gson.fromJson[java.util.Map[String, Object]](sortedActual, mapType)

    val difference = Maps.difference(FlatMapUtils.flatten(expectedMap), FlatMapUtils.flatten(actualMap))

    assertTrue(difference.areEqual, () => s"Expected: $sortedExpected\n\n" +
      s"Actual: $sortedActual\n\n" +
      s"Entries only in expected \n${difference.entriesOnlyOnLeft}\n\n" +
      s"Entries only in actual\n${difference.entriesOnlyOnRight}\n\n" +
      s"Entries differing\n${difference.entriesDiffering}")
  }

  def deepSort(element: JsonElement): JsonElement = {
    implicit val comparator: Comparator[JsonElement] = JsonElementComparator
    if (element.isJsonArray) {
      val array = element.getAsJsonArray
      val treeSet = new mutable.TreeSet[JsonElement]()
      array.forEach { element => treeSet.addOne(deepSort(element)) }
      val sortedArray = new JsonArray()
      treeSet.foreach { element => sortedArray.add(element) }
      sortedArray
    } else if (element.isJsonObject) {
      val map = element.getAsJsonObject
      val treeSet = new mutable.TreeSet[(String, JsonElement)]()
      map.entrySet().forEach { entry => treeSet.addOne((entry.getKey, deepSort(entry.getValue))) }
      val sortedMap = new JsonObject()
      treeSet.foreach { entry => sortedMap.add(entry._1, entry._2) }
      sortedMap
    } else {
      element
    }
  }
}

// FIXME: this is WILDLY inefficient
object JsonElementComparator extends Comparator[JsonElement] {
  override def compare(t: JsonElement, t1: JsonElement): Int = {
    t.toString.compareTo(t1.toString)
  }
}