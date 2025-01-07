load("@//rules/testing:intellij.bzl", "kt_intellij_junit4_test")

kt_intellij_junit4_test(
    name = "ProjectViewParsingBasicRulesTest",
    src = "ProjectViewParsingBasicRulesTest.kt",
    data = ["//plugin-bazel/src/test/testData/projectview/parser/basicRules"],
    runtime_deps = [
        "//plugin-bsp:intellij-bsp",
    ],
    deps = [
        "//plugin-bazel:intellij-bazel",
        "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/languages/projectview/fixtures",
    ],
)

kt_intellij_junit4_test(
    name = "ProjectViewParsingAllSectionsTest",
    src = "ProjectViewParsingAllSectionsTest.kt",
    data = ["//plugin-bazel/src/test/testData/projectview/parser/sections"],
    runtime_deps = [
        "//plugin-bsp:intellij-bsp",
    ],
    deps = [
        "//plugin-bazel:intellij-bazel",
        "//plugin-bazel/src/test/kotlin/org/jetbrains/bazel/languages/projectview/fixtures",
    ],
)
