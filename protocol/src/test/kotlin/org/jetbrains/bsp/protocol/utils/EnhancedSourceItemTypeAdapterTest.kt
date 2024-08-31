package org.jetbrains.bsp.protocol.utils

import ch.epfl.scala.bsp4j.SourceItem
import ch.epfl.scala.bsp4j.SourceItemKind
import com.google.gson.Gson
import com.google.gson.JsonParser
import io.kotest.matchers.shouldBe
import org.jetbrains.bsp.protocol.EnhancedSourceItem
import org.jetbrains.bsp.protocol.EnhancedSourceItemData
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

private val gson =
  Gson()
    .newBuilder()
    .registerTypeAdapter(SourceItem::class.java, EnhancedSourceItemTypeAdapter())
    .create()

private val enhancedSourceItemWithData =
  EnhancedSourceItem(
    uri = "file:///TestApp.java",
    kind = SourceItemKind.FILE,
    generated = false,
    data =
      EnhancedSourceItemData(
        jvmPackagePrefix = "com.example.myproject",
      ),
  )

private val enhancedSourceItemWithoutData =
  EnhancedSourceItem(
    uri = "file:///TestApp.java",
    kind = SourceItemKind.FILE,
    generated = false,
  )

private val sourceItem =
  SourceItem(
    "file:///TestApp.java", // uri
    SourceItemKind.FILE, // kind
    false, // generated
  )

private val jsonWithData =
  """
    {
      "data" : {
        "jvmPackagePrefix" : "com.example.myproject"
      },
      "uri" : "file:///TestApp.java",
      "kind" : 1,
      "generated" : false
    }
  """

private val jsonWithoutData =
  """
    {
      "uri" : "file:///TestApp.java",
      "kind" : 1,
      "generated" : false
    }
  """

class EnhancedSourceItemTypeAdapterTest {
  @Nested
  inner class DeserializationTest {
    @Test
    fun `should deserialize to EnhancedSourceItem when providing the data field`() {
      // given & when
      val item = gson.fromJson(jsonWithData, SourceItem::class.java)

      // then
      item shouldBe enhancedSourceItemWithData
    }

    @Test
    fun `should deserialize to EnhancedSourceItem when not providing the data field`() {
      // given & when
      val item = gson.fromJson(jsonWithoutData, SourceItem::class.java)

      // then
      item shouldBe enhancedSourceItemWithoutData
    }
  }

  @Nested
  inner class SerializationTest {
    @Test
    fun `should serialize to json when providing the EnhancedSourceItem with data field`() {
      // given & when
      val item = gson.toJson(enhancedSourceItemWithData)

      // then
      item shouldBe jsonWithData.toCompactJsonString()
    }

    @Test
    fun `should serialize to json when providing the EnhancedSourceItem without data field`() {
      // given & when
      val item = gson.toJson(enhancedSourceItemWithoutData)

      // then
      item shouldBe jsonWithoutData.toCompactJsonString()
    }

    @Test
    fun `should serialize to json when providing the SourceItem`() {
      // given & when
      val item = gson.toJson(sourceItem)

      // then
      item shouldBe jsonWithoutData.toCompactJsonString()
    }
  }
}

private fun String.toCompactJsonString() = gson.toJson(JsonParser.parseString(this))
