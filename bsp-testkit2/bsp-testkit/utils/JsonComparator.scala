package org.jetbrains.bsp.testkit

import com.google.common.collect.Maps
import com.google.gson.reflect.TypeToken
import com.google.gson.{GsonBuilder, JsonArray, JsonElement, JsonObject}
import org.junit.jupiter.api.Assertions.assertTrue

import java.lang.reflect.Type
import java.util.Comparator
import scala.collection.mutable

object JsonComparator {
  private val gson = new GsonBuilder().create()
  private val mapType: Type = new TypeToken[java.util.Map[String, Object]]() {}.getType

  def assertJsonEquals[T](expected: T, actual: T, typeOfT: Type): Unit = {
    val expectedObject = gson.toJsonTree(expected, typeOfT)
    val actualObject = gson.toJsonTree(actual, typeOfT)

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