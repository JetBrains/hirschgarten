load("@//rules/testing:junit5.bzl", "kt_junit5_test")
load("//rules_intellij/testing:test_defs_kt.bzl", "intellij_integration_test_suite")

intellij_integration_test_suite(
    name = "BazelGoPackageImportReferencesTest",
    srcs = ["BazelGoPackageImportReferencesTest.kt"],
    associates = ["//plugin-bazel/src:plugin-bazel-lib"],
    tags = ["exclusive"],
    test_package_root = "org.jetbrains",
    runtime_deps = [
        "//plugin-bazel",
    ],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bsp/protocol",
        "//plugin-bazel/src:test_fixtures",
        "//rules_intellij/third_party/go:go_for_tests",
        "@maven//:io_kotest_kotest_assertions_api_jvm",
        "@maven//:io_kotest_kotest_assertions_core_jvm",
        "@maven//:io_kotest_kotest_assertions_shared_jvm",
        "@maven//:io_kotest_kotest_common_jvm",
        "@maven//:org_mockito_mockito_core",
    ],
)

kt_junit5_test(
    name = "BazelGoPackageTest",
    size = "small",
    src = "BazelGoPackageTest.kt",
    associates = ["//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/golang/resolve"],
)
