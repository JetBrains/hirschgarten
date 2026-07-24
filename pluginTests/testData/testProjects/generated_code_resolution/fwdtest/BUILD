load("@rules_scala//scala:scala.bzl", "scala_binary", "scala_library", "scala_test")
load("//:gen.bzl", "fwd", "gen")

gen(
    name = "gen_lib",
    file = "Lib.scala",
    text = "package demo\n\nobject Lib { val l: Int = 1 }\n",
)

scala_library(
    name = "lib",
    srcs = [":gen_lib"],
    tags = ["manual"],
)

gen(
    name = "gen_lib_test",
    file = "LibTest.scala",
    text = "package demo\n\nimport org.scalatest.flatspec.AnyFlatSpec\n\nclass LibTest extends AnyFlatSpec { it should \"work\" in { assert(Lib.l == 1) } }\n",
)

# same package, no checked-in sources: the shape a codegen macro emits next to
# its library (scenario 4)
scala_test(
    name = "lib_t",
    srcs = [":gen_lib_test"],
    tags = ["manual"],
    deps = [":lib"],
)

# forwards the library's JavaInfo through a custom attribute, no DefaultInfo
fwd(
    name = "lib_fwd",
    jvm = ":lib",
)

scala_binary(
    name = "prod_bin",
    srcs = ["Prod.scala"],
    main_class = "demo.Prod",
    deps = [":lib_fwd"],
)
