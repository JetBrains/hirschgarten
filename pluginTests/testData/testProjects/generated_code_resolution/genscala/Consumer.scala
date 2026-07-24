package demo

object Consumer {
  // go to definition on Data must open the generated Data.scala (scenario 1);
  // inside it, Part must resolve as well (scenario 2, second hop)
  val c: Int = Data.d
}
