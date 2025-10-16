load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_binary")

kt_jvm_binary(
    name = "install",
    main_class = "org.jetbrains.bazel.install.Install",
    visibility = ["//visibility:public"],
    runtime_deps = [
        "//rules_intellij/intellij_platform_sdk:plugin_api_for_tests",
        "//server/commons",
        "//server/install/src/main/kotlin/org/jetbrains/bazel/install",
        "//server/server/src/main/kotlin/org/jetbrains/bazel/server",
        "@server_maven//:org_jetbrains_kotlinx_kotlinx_coroutines_core",
        "@server_maven//:org_jetbrains_kotlinx_kotlinx_coroutines_core_jvm",
    ],
)
