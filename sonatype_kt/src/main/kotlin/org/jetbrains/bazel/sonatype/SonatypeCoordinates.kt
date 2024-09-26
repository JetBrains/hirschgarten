package org.jetbrains.bazel.sonatype

data class SonatypeCoordinates(
  val sonatypeGroupId: String,
  val sonatypeArtifactId: String,
  val sonatypeVersion: String
) {
  companion object {
    fun fromString(coordinates: String): SonatypeCoordinates {
      val parts = coordinates.split(":")
      if (parts.size != 3) throw IllegalArgumentException("Coordinates must be a triplet, got: $coordinates")
      return SonatypeCoordinates(parts[0], parts[1], parts[2])
    }
  }
}
