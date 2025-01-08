load("@//rules/testing:intellij.bzl", "kt_intellij_junit4_test")

kt_intellij_junit4_test(
    name = "BazelrcAnnotationTest",
    src = "BazelrcAnnotationTest.kt",
    data = [
        "//plugin-bazel/src/test/testData/bazelrc/annotation:LabelFlagsTestData.bazelrc",
    ],
    runtime_deps = [
        "//plugin-bsp:intellij-bsp",
    ],
    deps = [
        "//plugin-bazel:intellij-bazel",
        "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/languages/bazelrc/fixtures",
    ],
)
