package org.jetbrains.bazel.sonatype

sealed class CommandType(val errNotFound: String) {
  object Close : CommandType("No open repository is found. Run publishSigned first")
  object Promote : CommandType("No closed repository is found. Run publishSigned and close commands")
  object Drop : CommandType("No staging repository is found. Run publishSigned first")
  object CloseAndPromote : CommandType("No staging repository is found. Run publishSigned first")
}
