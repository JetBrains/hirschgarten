load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_binary")

kt_jvm_binary(
    name = "SonatypePublisher",
    visibility = ["//visibility:public"],
    srcs = glob(["*.kt"]),
    main_class = "org.jetbrains.bazel.sonatype.Main",
    deps = [
        "@maven//:org_apache_logging_log4j_log4j_api",
        "@maven//:org_apache_httpcomponents_httpclient",
        "@maven//:commons_cli_commons_cli",
        "@maven//:com_fasterxml_jackson_module_jackson_module_kotlin",
        "@maven//:org_apache_logging_log4j_log4j_core",
        "@maven//:org_apache_logging_log4j_log4j_slf4j_impl",
        "@maven//:org_sonatype_spice_zapper_spice_zapper",
    ]
)
