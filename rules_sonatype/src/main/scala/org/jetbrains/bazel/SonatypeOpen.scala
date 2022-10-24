package org.jetbrains.bazel

import org.backuity.clist.Cli

object SonatypeOpen {
  def main(args: Array[String]): Unit = {
    Cli.parse(args)
      .withCommand(new SonatypeKeys) {
        keys: SonatypeKeys =>
          new Sonatype(keys).openRepo()
      }
      .get.get // just throw if anything broke
  }
}
