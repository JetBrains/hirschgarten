load("@//rules/testing:junit5.bzl", "kt_junit5_test")

kt_junit5_test(
    name = "CppPathResolverTest",
    size = "small",
    src = "CppPathResolverTest.kt",
    deps = [
        "//server/bazelrunner",
        "//server/server/src/main/kotlin/org/jetbrains/bsp/bazel/server/sync",
    ],
)
