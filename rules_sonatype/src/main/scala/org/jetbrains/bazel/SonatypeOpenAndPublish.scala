package org.jetbrains.bazel

import org.backuity.clist.Cli

object SonatypeOpenAndPublish {
  def main(args: Array[String]): Unit = {
    Cli.parse(args).withCommand(new SonatypeKeys) {
      keys: SonatypeKeys =>
        val sonatype = new Sonatype(keys)
        sonatype.openRepo()
        sonatype.bundleRelease()
    }
  }
}
