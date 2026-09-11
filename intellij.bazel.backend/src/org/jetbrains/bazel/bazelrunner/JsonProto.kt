package org.jetbrains.bazel.bazelrunner

/**
 * Representations of the messages from
 * https://github.com/bazelbuild/bazel/blob/master/src/main/protobuf/build.proto
 * in the JSON form that `--output=streamed_jsonproto` prints.
 */
internal class JsonProto {
  data class Attribute(val name: String, val stringValue: String?, val stringListValue: List<String>?)

  data class Repository(val moduleKey: String?, val canonicalName: String?, val repoRuleName: String?, val attribute: List<Attribute>)
}
