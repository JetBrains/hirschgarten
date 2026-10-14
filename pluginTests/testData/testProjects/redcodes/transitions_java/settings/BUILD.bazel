load("@bazel_skylib//rules:common_settings.bzl", "string_flag")

string_flag(
    name = "mode",
    build_setting_default = "a",
    values = ["a", "b"],
    visibility = ["//visibility:public"],
)

config_setting(
    name = "mode_a",
    flag_values = {":mode": "a"},
    visibility = ["//visibility:public"],
)

config_setting(
    name = "mode_b",
    flag_values = {":mode": "b"},
    visibility = ["//visibility:public"],
)
