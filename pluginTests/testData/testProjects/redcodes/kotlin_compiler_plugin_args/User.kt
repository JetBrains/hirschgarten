// This file is based on https://github.com/bazel-contrib/rules_kotlin, licensed under Apache-2.0;
// It was modified by JetBrains s.r.o. and contributors
package plugin.allopen;

import java.util.*

@OpenForTesting
data class User(
  val userId: UUID,
  val emails: String,
)
