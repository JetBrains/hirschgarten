load("@//rules/testing:intellij.bzl", "kt_intellij_junit4_test")

kt_intellij_junit4_test(
    name = "BazelrcAnnotationTest",
    src = "BazelrcAnnotationTest.kt",
    data = [
        "//plugin-bazel/src/test/testData/bazelrc/annotation:LabelFlagsTestData.bazelrc",
        "//plugin-bazel/src/test/testData/bazelrc/annotation:OldFlagsTestData.bazelrc",
        "//plugin-bazel/src/test/testData/bazelrc/annotation:TargetFlagsTestData.bazelrc",
    ],
    deps = [
        "//plugin-bazel",
        "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/languages/bazelrc/fixtures",
    ],
)
