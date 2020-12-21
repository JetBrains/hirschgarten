package org.jetbrains.bazel.sonatype

import org.sonatype.spice.zapper.fs.DirectoryIOSource
import org.sonatype.spice.zapper.{Path, ZFile}

import java.io.File
import java.util
import scala.jdk.CollectionConverters._

class DirectoryIOSourceMaven(root: File, filesPaths: List[Path]) extends DirectoryIOSource(root){
  override def scanDirectory(dir: File, zfiles: util.List[ZFile]): Int = {
    val files = filesPaths.map{path => {
      createZFile(path)
    }}.asJava

    zfiles.addAll(files)
    0
  }
}
