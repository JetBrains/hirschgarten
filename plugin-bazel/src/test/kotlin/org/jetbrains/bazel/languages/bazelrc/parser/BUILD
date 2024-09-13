load("@//plugin-bazel/rules/kotlin:junit4.bzl", "junit4_kt_test")

junit4_kt_test(
    name = "BazelrcParsingTest",
    srcs = ["BazelrcParsingTest.kt"],
    data = ["//plugin-bazel/src/test/testData/bazelrc/parser"],
    deps = ["//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/languages/bazelrc/fixtures"],
)
