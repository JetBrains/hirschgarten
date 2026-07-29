package demo

import demo.thriftscala.Row

object ThriftConsumer {
  // go to definition on Row must open the scrooge generated source, not a
  // decompiled class file, and without manually attaching sources (scenario 3)
  val r: Row = Row(id = "x")
}
