load("@//rules/testing:junit5.bzl", "kt_junit5_test")

kt_junit5_test(
    name = "ProjectTest",
    size = "small",
    src = "ProjectTest.kt",
    deps = [
        "//server/server/src/main/kotlin/org/jetbrains/bazel/server/model",
        "@maven//:ch_epfl_scala_bsp4j",
    ],
)
