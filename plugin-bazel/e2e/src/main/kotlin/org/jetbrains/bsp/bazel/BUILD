load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_binary")

kt_jvm_binary(
    name = "BazelBspSampleRepoTest",
    srcs = ["BazelBspSampleRepoTest.kt"],
    main_class = "org.jetbrains.bsp.bazel.BazelBspSampleRepoTest",
    resources = ["//plugin-bazel/e2e/src/main/resources:bsp-e2e-resources"],
    visibility = ["//plugin-bazel/e2e:__subpackages__"],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bazel/commons/utils",
        "//plugin-bazel/commons",
        "//plugin-bazel/e2e/src/main/kotlin/org/jetbrains/bsp/bazel/base",
        "//plugin-bazel/server/src/main/kotlin/org/jetbrains/bsp/bazel/server/label",
        "@maven//:ch_epfl_scala_bsp4j",
    ],
)

kt_jvm_binary(
    name = "BazelBspLocalJdkTest",
    srcs = ["BazelBspLocalJdkTest.kt"],
    main_class = "org.jetbrains.bsp.bazel.BazelBspLocalJdkTest",
    resources = ["//plugin-bazel/e2e/src/main/resources:bsp-e2e-resources"],
    visibility = ["//plugin-bazel/e2e:__subpackages__"],
    deps = [
        "//plugin-bazel/commons",
        "//plugin-bazel/e2e/src/main/kotlin/org/jetbrains/bsp/bazel/base",
        "@maven//:ch_epfl_scala_bsp4j",
    ],
)

kt_jvm_binary(
    name = "BazelBspRemoteJdkTest",
    srcs = ["BazelBspRemoteJdkTest.kt"],
    main_class = "org.jetbrains.bsp.bazel.BazelBspRemoteJdkTest",
    resources = ["//plugin-bazel/e2e/src/main/resources:bsp-e2e-resources"],
    visibility = ["//plugin-bazel/e2e:__subpackages__"],
    deps = [
        "//plugin-bazel/commons",
        "//plugin-bazel/e2e/src/main/kotlin/org/jetbrains/bsp/bazel/base",
        "@maven//:ch_epfl_scala_bsp4j",
    ],
)

kt_jvm_binary(
    name = "BazelBspCppProjectTest",
    srcs = ["BazelBspCppProjectTest.kt"],
    main_class = "org.jetbrains.bsp.bazel.BazelBspCppProjectTest",
    resources = ["//plugin-bazel/e2e/src/main/resources:bsp-e2e-resources"],
    visibility = ["//plugin-bazel/e2e:__subpackages__"],
    deps = [
        "//plugin-bazel/commons",
        "//plugin-bazel/e2e/src/main/kotlin/org/jetbrains/bsp/bazel/base",
        "//plugin-bazel/server/src/main/kotlin/org/jetbrains/bsp/bazel/server/label",
        "@maven//:ch_epfl_scala_bsp4j",
    ],
)

kt_jvm_binary(
    name = "BazelBspPythonProjectTest",
    srcs = ["BazelBspPythonProjectTest.kt"],
    main_class = "org.jetbrains.bsp.bazel.BazelBspPythonProjectTest",
    resources = ["//plugin-bazel/e2e/src/main/resources:bsp-e2e-resources"],
    visibility = ["//plugin-bazel/e2e:__subpackages__"],
    deps = [
        "//plugin-bazel/commons",
        "//plugin-bazel/e2e/src/main/kotlin/org/jetbrains/bsp/bazel/base",
        "@maven//:ch_epfl_scala_bsp4j",
    ],
)

kt_jvm_binary(
    name = "BazelBspScalaProjectTest",
    srcs = ["BazelBspScalaProjectTest.kt"],
    main_class = "org.jetbrains.bsp.bazel.BazelBspScalaProjectTest",
    resources = ["//plugin-bazel/e2e/src/main/resources:bsp-e2e-resources"],
    visibility = ["//plugin-bazel/e2e:__subpackages__"],
    deps = [
        "//plugin-bazel/commons",
        "//plugin-bazel/e2e/src/main/kotlin/org/jetbrains/bsp/bazel/base",
        "//plugin-bazel/server/src/main/kotlin/org/jetbrains/bsp/bazel/server/label",
        "@maven//:ch_epfl_scala_bsp4j",
    ],
)

kt_jvm_binary(
    name = "BazelBspKotlinProjectTest",
    srcs = ["BazelBspKotlinProjectTest.kt"],
    main_class = "org.jetbrains.bsp.bazel.BazelBspKotlinProjectTest",
    resources = ["//plugin-bazel/e2e/src/main/resources:bsp-e2e-resources"],
    visibility = ["//plugin-bazel/e2e:__subpackages__"],
    deps = [
        "//plugin-bazel/commons",
        "//plugin-bazel/e2e/src/main/kotlin/org/jetbrains/bsp/bazel/base",
        "//plugin-bazel/server/src/main/kotlin/org/jetbrains/bsp/bazel/server/label",
        "//protocol",
        "@maven//:ch_epfl_scala_bsp4j",
    ],
)

kt_jvm_binary(
    name = "BazelBspKotlinProjectWithEnabledRulesTest",
    srcs = ["BazelBspKotlinProjectWithEnabledRulesTest.kt"],
    main_class = "org.jetbrains.bsp.bazel.BazelBspKotlinProjectWithEnabledRulesTest",
    resources = ["//plugin-bazel/e2e/src/main/resources:bsp-e2e-resources"],
    visibility = ["//plugin-bazel/e2e:__subpackages__"],
    deps = [
        ":BazelBspKotlinProjectTest",
        "//plugin-bazel/commons",
        "//plugin-bazel/e2e/src/main/kotlin/org/jetbrains/bsp/bazel/base",
        "//plugin-bazel/server/src/main/kotlin/org/jetbrains/bsp/bazel/server/model",
        "//protocol",
        "@maven//:ch_epfl_scala_bsp4j",
    ],
)

kt_jvm_binary(
    name = "ServerDownloadsBazeliskTest",
    srcs = ["ServerDownloadsBazeliskTest.kt"],
    main_class = "org.jetbrains.bsp.bazel.ServerDownloadsBazeliskTest",
    resources = ["//plugin-bazel/e2e/src/main/resources:bsp-e2e-resources"],
    visibility = ["//plugin-bazel/e2e:__subpackages__"],
    deps = [
        "//plugin-bazel/commons",
        "//plugin-bazel/e2e/src/main/kotlin/org/jetbrains/bsp/bazel/base",
        "@maven//:ch_epfl_scala_bsp4j",
    ],
)

kt_jvm_binary(
    name = "BazelBspRustProjectTest",
    srcs = ["BazelBspRustProjectTest.kt"],
    main_class = "org.jetbrains.bsp.bazel.BazelBspRustProjectTest",
    resources = ["//plugin-bazel/e2e/src/main/resources:bsp-e2e-resources"],
    visibility = ["//plugin-bazel/e2e:__subpackages__"],
    deps = [
        "//plugin-bazel/commons",
        "//plugin-bazel/e2e/src/main/kotlin/org/jetbrains/bsp/bazel/base",
        "@maven//:ch_epfl_scala_bsp4j",
    ],
)

kt_jvm_binary(
    name = "BazelBspAndroidProjectTest",
    srcs = ["BazelBspAndroidProjectTest.kt"],
    main_class = "org.jetbrains.bsp.bazel.BazelBspAndroidProjectTest",
    resources = ["//plugin-bazel/e2e/src/main/resources:bsp-e2e-resources"],
    visibility = ["//plugin-bazel/e2e:__subpackages__"],
    deps = [
        "//plugin-bazel/commons",
        "//plugin-bazel/e2e/src/main/kotlin/org/jetbrains/bsp/bazel/android",
        "//plugin-bazel/e2e/src/main/kotlin/org/jetbrains/bsp/bazel/base",
        "//protocol",
        "@maven//:ch_epfl_scala_bsp4j",
    ],
)

kt_jvm_binary(
    name = "BazelBspAndroidKotlinProjectTest",
    srcs = ["BazelBspAndroidKotlinProjectTest.kt"],
    main_class = "org.jetbrains.bsp.bazel.BazelBspAndroidKotlinProjectTest",
    resources = ["//plugin-bazel/e2e/src/main/resources:bsp-e2e-resources"],
    visibility = ["//plugin-bazel/e2e:__subpackages__"],
    deps = [
        "//plugin-bazel/commons",
        "//plugin-bazel/e2e/src/main/kotlin/org/jetbrains/bsp/bazel/android",
        "//plugin-bazel/e2e/src/main/kotlin/org/jetbrains/bsp/bazel/base",
        "//plugin-bazel/server/src/main/kotlin/org/jetbrains/bsp/bazel/server/label",
        "//protocol",
        "@maven//:ch_epfl_scala_bsp4j",
    ],
)

kt_jvm_binary(
    name = "BazelBspGoProjectTest",
    srcs = ["BazelBspGoProjectTest.kt"],
    main_class = "org.jetbrains.bsp.bazel.BazelBspGoProjectTest",
    resources = ["//plugin-bazel/e2e/src/main/resources:bsp-e2e-resources"],
    visibility = ["//plugin-bazel/e2e:__subpackages__"],
    deps = [
        "//plugin-bazel/commons",
        "//plugin-bazel/e2e/src/main/kotlin/org/jetbrains/bsp/bazel/base",
        "@maven//:ch_epfl_scala_bsp4j",
    ],
)

kt_jvm_binary(
    name = "JavaDiagnosticsTest",
    srcs = ["JavaDiagnosticsTest.kt"],
    main_class = "org.jetbrains.bsp.bazel.JavaDiagnosticsTest",
    resources = ["//plugin-bazel/e2e/src/main/resources:bsp-e2e-resources"],
    visibility = ["//plugin-bazel/e2e:__subpackages__"],
    deps = [
        "//plugin-bazel/commons",
        "//plugin-bazel/e2e/src/main/kotlin/org/jetbrains/bsp/bazel/base",
        "//protocol",
        "@maven//:ch_epfl_scala_bsp4j",
    ],
)

kt_jvm_binary(
    name = "BazelBspAllowManualTargetsSyncTest",
    srcs = ["BazelBspAllowManualTargetsSyncTest.kt"],
    main_class = "org.jetbrains.bsp.bazel.BazelBspAllowManualTargetsSyncTest",
    resources = ["//plugin-bazel/e2e/src/main/resources:bsp-e2e-resources"],
    visibility = ["//plugin-bazel/e2e:__subpackages__"],
    deps = [
        "//plugin-bazel/commons",
        "//plugin-bazel/e2e/src/main/kotlin/org/jetbrains/bsp/bazel/base",
        "//plugin-bazel/server/src/main/kotlin/org/jetbrains/bsp/bazel/server/label",
        "@maven//:ch_epfl_scala_bsp4j",
    ],
)

kt_jvm_binary(
    name = "BazelBspPartialSyncTest",
    srcs = ["BazelBspPartialSyncTest.kt"],
    main_class = "org.jetbrains.bsp.bazel.BazelBspPartialSyncTest",
    resources = ["//plugin-bazel/e2e/src/main/resources:bsp-e2e-resources"],
    visibility = ["//plugin-bazel/e2e:__subpackages__"],
    deps = [
        "//plugin-bazel/commons",
        "//plugin-bazel/e2e/src/main/kotlin/org/jetbrains/bsp/bazel/base",
        "@maven//:ch_epfl_scala_bsp4j",
    ],
)

kt_jvm_binary(
    name = "BazelBspBuildAndSyncTest",
    srcs = ["BazelBspBuildAndSyncTest.kt"],
    main_class = "org.jetbrains.bsp.bazel.BazelBspBuildAndSyncTest",
    resources = ["//plugin-bazel/e2e/src/main/resources:bsp-e2e-resources"],
    visibility = ["//plugin-bazel/e2e:__subpackages__"],
    deps = [
        "//plugin-bazel/commons",
        "//plugin-bazel/e2e/src/main/kotlin/org/jetbrains/bsp/bazel/base",
        "@maven//:ch_epfl_scala_bsp4j",
    ],
)

kt_jvm_binary(
    name = "BazelBspFirstPhaseSyncTest",
    srcs = ["BazelBspFirstPhaseSyncTest.kt"],
    main_class = "org.jetbrains.bsp.bazel.BazelBspFirstPhaseSyncTest",
    resources = ["//plugin-bazel/e2e/src/main/resources:bsp-e2e-resources"],
    visibility = ["//plugin-bazel/e2e:__subpackages__"],
    deps = [
        "//plugin-bazel/commons",
        "//plugin-bazel/e2e/src/main/kotlin/org/jetbrains/bsp/bazel/base",
        "@maven//:ch_epfl_scala_bsp4j",
    ],
)

kt_jvm_binary(
    name = "NestedModulesTest",
    srcs = ["NestedModulesTest.kt"],
    main_class = "org.jetbrains.bsp.bazel.NestedModulesTest",
    resources = ["//plugin-bazel/e2e/src/main/resources:bsp-e2e-resources"],
    visibility = ["//plugin-bazel/e2e:__subpackages__"],
    deps = [
        "//plugin-bazel/commons",
        "//plugin-bazel/e2e/src/main/kotlin/org/jetbrains/bsp/bazel/base",
        "@maven//:ch_epfl_scala_bsp4j",
        "@maven//:io_kotest_kotest_assertions_api_jvm",
        "@maven//:io_kotest_kotest_assertions_core_jvm",
        "@maven//:io_kotest_kotest_assertions_shared_jvm",
        "@maven//:io_kotest_kotest_common_jvm",
    ],
)
