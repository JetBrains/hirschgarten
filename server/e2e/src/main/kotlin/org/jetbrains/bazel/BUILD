load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_binary")

kt_jvm_binary(
    name = "BazelBspSampleRepoTest",
    srcs = ["BazelBspSampleRepoTest.kt"],
    main_class = "org.jetbrains.bazel.BazelBspSampleRepoTest",
    resources = ["//server/e2e/src/main/resources:bsp-e2e-resources"],
    visibility = ["//server/e2e:__subpackages__"],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bsp/protocol",
        "//rules_intellij/intellij_platform_sdk:plugin_api_for_tests",
        "//server/commons",
        "//server/e2e/src/main/kotlin/org/jetbrains/bazel/base",
            ],
)

kt_jvm_binary(
    name = "BazelBspLocalJdkTest",
    srcs = ["BazelBspLocalJdkTest.kt"],
    main_class = "org.jetbrains.bazel.BazelBspLocalJdkTest",
    resources = ["//server/e2e/src/main/resources:bsp-e2e-resources"],
    visibility = ["//server/e2e:__subpackages__"],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bsp/protocol",
        "//server/commons",
        "//server/e2e/src/main/kotlin/org/jetbrains/bazel/base",
    ],
)

kt_jvm_binary(
    name = "BazelBspRemoteJdkTest",
    srcs = ["BazelBspRemoteJdkTest.kt"],
    main_class = "org.jetbrains.bazel.BazelBspRemoteJdkTest",
    resources = ["//server/e2e/src/main/resources:bsp-e2e-resources"],
    visibility = ["//server/e2e:__subpackages__"],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bsp/protocol",
        "//server/commons",
        "//server/e2e/src/main/kotlin/org/jetbrains/bazel/base",
    ],
)

kt_jvm_binary(
    name = "BazelBspCppProjectTest",
    srcs = ["BazelBspCppProjectTest.kt"],
    main_class = "org.jetbrains.bazel.BazelBspCppProjectTest",
    resources = ["//server/e2e/src/main/resources:bsp-e2e-resources"],
    visibility = ["//server/e2e:__subpackages__"],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bsp/protocol",
        "//server/commons",
        "//server/e2e/src/main/kotlin/org/jetbrains/bazel/base",
            ],
)

kt_jvm_binary(
    name = "BazelBspPythonProjectTest",
    srcs = ["BazelBspPythonProjectTest.kt"],
    main_class = "org.jetbrains.bazel.BazelBspPythonProjectTest",
    resources = ["//server/e2e/src/main/resources:bsp-e2e-resources"],
    visibility = ["//server/e2e:__subpackages__"],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bsp/protocol",
        "//server/commons",
        "//server/e2e/src/main/kotlin/org/jetbrains/bazel/base",
    ],
)

kt_jvm_binary(
    name = "BazelBspScalaProjectTest",
    srcs = ["BazelBspScalaProjectTest.kt"],
    main_class = "org.jetbrains.bazel.BazelBspScalaProjectTest",
    resources = ["//server/e2e/src/main/resources:bsp-e2e-resources"],
    visibility = ["//server/e2e:__subpackages__"],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bsp/protocol",
        "//server/commons",
        "//server/e2e/src/main/kotlin/org/jetbrains/bazel/base",
            ],
)

kt_jvm_binary(
    name = "BazelBspKotlinProjectTest",
    srcs = ["BazelBspKotlinProjectTest.kt"],
    main_class = "org.jetbrains.bazel.BazelBspKotlinProjectTest",
    resources = ["//server/e2e/src/main/resources:bsp-e2e-resources"],
    visibility = ["//server/e2e:__subpackages__"],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bsp/protocol",
        "//server/commons",
        "//server/e2e/src/main/kotlin/org/jetbrains/bazel/base",
            ],
)

kt_jvm_binary(
    name = "BazelBspKotlinProjectWithEnabledRulesTest",
    srcs = ["BazelBspKotlinProjectWithEnabledRulesTest.kt"],
    main_class = "org.jetbrains.bazel.BazelBspKotlinProjectWithEnabledRulesTest",
    resources = ["//server/e2e/src/main/resources:bsp-e2e-resources"],
    visibility = ["//server/e2e:__subpackages__"],
    deps = [
        ":BazelBspKotlinProjectTest",
        "//commons/src/main/kotlin/org/jetbrains/bsp/protocol",
        "//server/commons",
        "//server/e2e/src/main/kotlin/org/jetbrains/bazel/base",
        "//server/server/src/main/kotlin/org/jetbrains/bazel/server/model",
    ],
)

kt_jvm_binary(
    name = "ServerDownloadsBazeliskTest",
    srcs = ["ServerDownloadsBazeliskTest.kt"],
    main_class = "org.jetbrains.bazel.ServerDownloadsBazeliskTest",
    resources = ["//server/e2e/src/main/resources:bsp-e2e-resources"],
    visibility = ["//server/e2e:__subpackages__"],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bsp/protocol",
        "//server/commons",
        "//server/e2e/src/main/kotlin/org/jetbrains/bazel/base",
    ],
)

kt_jvm_binary(
    name = "BazelBspAndroidProjectTest",
    srcs = ["BazelBspAndroidProjectTest.kt"],
    main_class = "org.jetbrains.bazel.BazelBspAndroidProjectTest",
    resources = ["//server/e2e/src/main/resources:bsp-e2e-resources"],
    visibility = ["//server/e2e:__subpackages__"],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bsp/protocol",
        "//server/commons",
        "//server/e2e/src/main/kotlin/org/jetbrains/bazel/android",
        "//server/e2e/src/main/kotlin/org/jetbrains/bazel/base",
    ],
)

kt_jvm_binary(
    name = "BazelBspAndroidKotlinProjectTest",
    srcs = ["BazelBspAndroidKotlinProjectTest.kt"],
    main_class = "org.jetbrains.bazel.BazelBspAndroidKotlinProjectTest",
    resources = ["//server/e2e/src/main/resources:bsp-e2e-resources"],
    visibility = ["//server/e2e:__subpackages__"],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bsp/protocol",
        "//server/commons",
        "//server/e2e/src/main/kotlin/org/jetbrains/bazel/android",
        "//server/e2e/src/main/kotlin/org/jetbrains/bazel/base",
            ],
)

kt_jvm_binary(
    name = "BazelBspGoProjectTest",
    srcs = ["BazelBspGoProjectTest.kt"],
    main_class = "org.jetbrains.bazel.BazelBspGoProjectTest",
    resources = ["//server/e2e/src/main/resources:bsp-e2e-resources"],
    visibility = ["//server/e2e:__subpackages__"],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bsp/protocol",
        "//server/commons",
        "//server/e2e/src/main/kotlin/org/jetbrains/bazel/base",
    ],
)

kt_jvm_binary(
    name = "JavaDiagnosticsTest",
    srcs = ["JavaDiagnosticsTest.kt"],
    main_class = "org.jetbrains.bazel.JavaDiagnosticsTest",
    resources = ["//server/e2e/src/main/resources:bsp-e2e-resources"],
    visibility = ["//server/e2e:__subpackages__"],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bsp/protocol",
        "//server/commons",
        "//server/e2e/src/main/kotlin/org/jetbrains/bazel/base",
    ],
)

kt_jvm_binary(
    name = "BazelBspAllowManualTargetsSyncTest",
    srcs = ["BazelBspAllowManualTargetsSyncTest.kt"],
    main_class = "org.jetbrains.bazel.BazelBspAllowManualTargetsSyncTest",
    resources = ["//server/e2e/src/main/resources:bsp-e2e-resources"],
    visibility = ["//server/e2e:__subpackages__"],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bsp/protocol",
        "//server/commons",
        "//server/e2e/src/main/kotlin/org/jetbrains/bazel/base",
            ],
)

kt_jvm_binary(
    name = "BazelBspPartialSyncTest",
    srcs = ["BazelBspPartialSyncTest.kt"],
    main_class = "org.jetbrains.bazel.BazelBspPartialSyncTest",
    resources = ["//server/e2e/src/main/resources:bsp-e2e-resources"],
    visibility = ["//server/e2e:__subpackages__"],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bazel/commons",
        "//commons/src/main/kotlin/org/jetbrains/bsp/protocol",
        "//server/commons",
        "//server/e2e/src/main/kotlin/org/jetbrains/bazel/base",
    ],
)

kt_jvm_binary(
    name = "BazelBspBuildAndSyncTest",
    srcs = ["BazelBspBuildAndSyncTest.kt"],
    main_class = "org.jetbrains.bazel.BazelBspBuildAndSyncTest",
    resources = ["//server/e2e/src/main/resources:bsp-e2e-resources"],
    visibility = ["//server/e2e:__subpackages__"],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bsp/protocol",
        "//server/commons",
        "//server/e2e/src/main/kotlin/org/jetbrains/bazel/base",
    ],
)

kt_jvm_binary(
    name = "BazelBspFirstPhaseSyncTest",
    srcs = ["BazelBspFirstPhaseSyncTest.kt"],
    main_class = "org.jetbrains.bazel.BazelBspFirstPhaseSyncTest",
    resources = ["//server/e2e/src/main/resources:bsp-e2e-resources"],
    visibility = ["//server/e2e:__subpackages__"],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bazel/commons",
        "//commons/src/main/kotlin/org/jetbrains/bsp/protocol",
        "//server/commons",
        "//server/e2e/src/main/kotlin/org/jetbrains/bazel/base",
    ],
)

kt_jvm_binary(
    name = "NestedModulesTest",
    srcs = ["NestedModulesTest.kt"],
    main_class = "org.jetbrains.bazel.NestedModulesTest",
    resources = ["//server/e2e/src/main/resources:bsp-e2e-resources"],
    visibility = ["//server/e2e:__subpackages__"],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bsp/protocol",
        "//server/commons",
        "//server/e2e/src/main/kotlin/org/jetbrains/bazel/base",
        "@maven//:io_kotest_kotest_assertions_api_jvm",
        "@maven//:io_kotest_kotest_assertions_core_jvm",
        "@maven//:io_kotest_kotest_assertions_shared_jvm",
        "@maven//:io_kotest_kotest_common_jvm",
    ],
)

kt_jvm_binary(
    name = "ExternalAutoloadsTest",
    srcs = ["ExternalAutoloadsTest.kt"],
    main_class = "org.jetbrains.bazel.ExternalAutoloadsTest",
    resources = ["//server/e2e/src/main/resources:bsp-e2e-resources"],
    visibility = ["//server/e2e:__subpackages__"],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bsp/protocol",
        "//server/commons",
        "//server/e2e/src/main/kotlin/org/jetbrains/bazel/base",
    ],
)
