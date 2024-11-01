load(
    "@rules_intellij//build_defs:build_defs.bzl",
    "intellij_plugin_library",
)
load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

intellij_plugin_library(
    name = "plugin_library",
    optional_plugin_deps = [],
    plugin_deps = ["com.intellij.modules.platform"],
    plugin_xmls = [
        "inmem-server.xml",
    ],
    visibility = ["//visibility:public"],
    deps = [
        ":intellij-inmem-server",
    ],
)

kt_jvm_library(
    name = "intellij-plugins",
    neverlink = True,
    exports = [
        "//plugin-bazel/src:intellij-bazel",
        "//plugin-bsp/src:intellij-bsp",
    ],
)

kt_jvm_library(
    name = "intellij-inmem-server",
    srcs = glob([
        "main/kotlin/**/*.kt",
    ]),
    associates = [":intellij-plugins"],
    visibility = ["//visibility:public"],
    deps = [
        "//server/bspcli:in_memory_connection",
    ],
)
