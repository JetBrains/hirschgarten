load("@rules_jvm//:jvm.bzl", "jvm_library")
load("@rules_java//java:java_import.bzl", "java_import")

### auto-generated section `build intellij.libraries.bazel.aspect.sdk` start

jvm_library(
    name = "libraries-bazel-aspect-sdk",
    srcs = glob(
        [],
        allow_empty = True,
    ),
    module_name = "intellij.libraries.bazel.aspect.sdk",
    resource_strip_prefix = "resources",
    resources = glob(
        ["resources/**/*"],
    ),
    visibility = ["//visibility:public"],
    exports = [":libraries-bazel-aspect-sdk-jetbrains-intellij-deps-bazel-aspect-sdk"],
    deps_to_bundle = [":libraries-bazel-aspect-sdk-jetbrains-intellij-deps-bazel-aspect-sdk"],
)

jvm_library(
    name = "libraries-bazel-aspect-sdk_test_lib",
    testonly = True,
    srcs = glob(
        [],
        allow_empty = True,
    ),
    kotlinc_opts = "@community//:k8",
    module_name = "intellij.libraries.bazel.aspect.sdk",
    visibility = ["//visibility:public"],
    exports = ["@ultimate_lib//:libraries-bazel-aspect-sdk-jetbrains-intellij-deps-bazel-aspect-sdk"],
    runtime_deps = [":libraries-bazel-aspect-sdk"],
)
### auto-generated section `build intellij.libraries.bazel.aspect.sdk` end

### auto-generated section `iml intellij.libraries.bazel.aspect.sdk` start
exports_files(
    [
        "intellij.libraries.bazel.aspect.sdk.iml",
    ],
    visibility = ["//visibility:public"],
)
### auto-generated section `iml intellij.libraries.bazel.aspect.sdk` end

java_import(
    name = "libraries-bazel-aspect-sdk-jetbrains-intellij-deps-bazel-aspect-sdk",
    jars = ["@intellij_aspect_sdk//:sdk_deploy.jar"],
)
