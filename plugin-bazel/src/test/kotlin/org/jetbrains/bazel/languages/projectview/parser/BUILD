load("@//rules/testing:intellij.bzl", "kt_intellij_junit4_test")

kt_intellij_junit4_test(
    name = "ProjectViewParsingAllSectionsTest",
    src = "ProjectViewParsingAllSectionsTest.kt",
    data = ["//plugin-bazel/src/test/testData/projectview/parser/sections"],
    tags = ["exclusive"],
    deps = [
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/languages",
    ],
)
