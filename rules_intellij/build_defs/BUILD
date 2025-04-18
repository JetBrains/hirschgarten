#
# This file is based on Bazel plugin for IntelliJ by The Bazel Authors, licensed under Apache-2.0;
# It was modified by JetBrains s.r.o. and contributors
#
# Description:
#
# Scripts for building IntelliJ plugins

load("@rules_python//python:defs.bzl", "py_binary")

package(default_visibility = ["//visibility:public"])

exports_files([
    "build_defs.bzl",
    "intellij_plugin.bzl",
    "intellij_plugin_debug_target.bzl",
])

licenses(["notice"])

py_binary(
    name = "merge_xml",
    srcs = ["merge_xml.py"],
    python_version = "PY3",
)

py_binary(
    name = "stamp_plugin_xml",
    srcs = ["stamp_plugin_xml.py"],
    python_version = "PY3",
)

py_binary(
    name = "api_version_txt",
    srcs = ["api_version_txt.py"],
    python_version = "PY3",
)

py_binary(
    name = "append_optional_xml_elements",
    srcs = ["append_optional_xml_elements.py"],
    python_version = "PY3",
)

py_binary(
    name = "package_meta_inf_files",
    srcs = ["package_meta_inf_files.py"],
    python_version = "PY3",
)

py_binary(
    name = "zip_plugin_files",
    srcs = ["zip_plugin_files.py"],
    python_version = "PY3",
)
