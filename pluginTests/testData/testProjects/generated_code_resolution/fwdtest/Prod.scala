package demo

// Lib is production code: a non-test binary depends on it through the
// forwarding rule. Without the fixes the library is classified as test-only
// (its only visible executable is the same package sourceless test) and Lib
// does not resolve here at all, while it resolves fine from any test target
// (scenario 4)
object Prod {
  def main(args: Array[String]): Unit = println(Lib.l)
}
