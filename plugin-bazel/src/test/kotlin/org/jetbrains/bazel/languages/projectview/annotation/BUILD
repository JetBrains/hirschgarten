load("@//rules/testing:intellij.bzl", "kt_intellij_junit4_test")

kt_intellij_junit4_test(
    name = "ProjectViewAnnotatorTest",
    src = "ProjectViewAnnotatorTest.kt",
    data = [
        "//plugin-bazel/src/test/testData/projectview/annotation:ProjectViewAnnotatorTestData.bazelproject",
    ],
    deps = [
        "//plugin-bazel",
        "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/languages/projectview/fixtures",
    ],
)
