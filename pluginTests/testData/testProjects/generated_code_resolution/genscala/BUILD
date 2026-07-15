load("@rules_scala//scala:scala.bzl", "scala_library")
load("//:gen.bzl", "gen")

gen(
    name = "gen_part",
    file = "Part.scala",
    text = "package demo\n\nobject Part { val p: Int = 1 }\n",
)

scala_library(
    name = "part",
    srcs = [":gen_part"],
)

gen(
    name = "gen_data",
    file = "Data.scala",
    text = "package demo\n\nobject Data { val d: Int = Part.p + 1 }\n",
)

scala_library(
    name = "data",
    srcs = [":gen_data"],
    deps = [":part"],
)

scala_library(
    name = "consumer",
    srcs = ["Consumer.scala"],
    deps = [":data"],
)
