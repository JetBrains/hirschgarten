package org.jetbrains.bazel.server.bsp.utils

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import org.apache.logging.log4j.Logger
import org.w3c.dom.Document
import org.xml.sax.InputSource
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

fun String.readXML(log: Logger? = null): Document? {
  val xml = substringIncludingFirst('<') ?: return null
  return try {
    DocumentBuilderFactory
      .newInstance()
      .newDocumentBuilder()
      .parse(InputSource(StringReader(xml)))
  } catch (e: Exception) {
    log?.error("Failed to parse string to xml", e)
    null
  }
}

fun String.toJson(log: Logger? = null): JsonElement? {
  val json = substringIncludingFirst('{') ?: return null
  return try {
    JsonParser.parseString(json)
  } catch (e: Exception) {
    log?.error("Failed to parse string to json", e)
    null
  }
}

/**
 * Make sure to skip all the informational prints from Bazel before the actual content starts
 */
private fun String.substringIncludingFirst(char: Char): String? {
  val index = indexOf(char).takeIf { it != -1 } ?: return null
  return substring(index)
}
