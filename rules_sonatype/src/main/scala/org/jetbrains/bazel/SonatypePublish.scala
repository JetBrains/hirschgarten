package org.jetbrains.bazel

import org.backuity.clist.Cli

object SonatypePublish {
  def main(args: Array[String]): Unit = {
    Cli.parse(args)
      .withCommand(new SonatypeKeys) { keys: SonatypeKeys =>
        new Sonatype(keys).bundleRelease()
      }
      .get.get // just throw here if anything broke

  }
}
