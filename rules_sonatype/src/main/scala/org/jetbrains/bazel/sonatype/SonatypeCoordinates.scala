package org.jetbrains.bazel.sonatype

case class SonatypeCoordinates(sonatypeGroupId: String, sonatypeArtifactId: String, sonatypeVersion: String)

object SonatypeCoordinates {
  def apply(sonatypeCoordinates: String): SonatypeCoordinates = {
    val parts = sonatypeCoordinates.split(":")
    if (parts.size != 3)
      throw new IllegalArgumentException("Coordinates must be a triplet, got: " + sonatypeCoordinates)

    new SonatypeCoordinates(parts(0), parts(1), parts(2))
  }
}
