load("@rules_kotlin//kotlin:core.bzl", "define_kt_toolchain", "kt_kotlinc_options", "kt_javac_options")

define_kt_toolchain(
    name = "my_kotlin_toolchain",
    api_version = "1.9",
    language_version = "1.9",
)

kt_kotlinc_options(
    name = "my_kt_kotlinc_options",
    visibility = ["//visibility:public"],
    x_no_call_assertions = True,
    x_no_param_assertions = True,
)

kt_javac_options(
    name = "my_kt_javac_options",
    visibility = ["//visibility:public"],
    x_ep_disable_all_checks = True,
    add_exports = [
      "java.desktop/sun.font=ALL-UNNAMED",
      "java.desktop/sun.awt.image=ALL-UNNAMED",
    ]
)
