package org.jetbrains.bazel.sync_new.flow.hash_diff._old.query

import com.dynatrace.hash4j.hashing.HashValue128
import com.dynatrace.hash4j.hashing.Hashing
import com.google.devtools.build.lib.query2.proto.proto2api.Build

object BuildRuleProtoHasher {
  fun hash(rule: Build.Rule): HashValue128 {
    val hasher = Hashing.xxh3_128().hashStream()
    hasher.putString(rule.name)
    hasher.putString(rule.ruleClass)
    hasher.putBytes(rule.skylarkEnvironmentHashCodeBytes.toByteArray())
    for (attribute in rule.attributeList) {
      // TODO: ignored some args
      hasher.putBytes(attribute.toByteArray())
    }

    return hasher.get()
  }
}
