load("@io_bazel_rules_scala//scala:scala.bzl", "scala_binary")

java_binary(
    name = "SonatypePublisher",
    srcs = glob(["*.scala", "sonatype/*.scala"]),
    javacopts = [
        "-encoding",
        "UTF-8",
        "-deprecation",
        "-unchecked",
    ],
    main_class = "org.jetbrains.bazel.SonatypePublish",
    visibility = ["//visibility:public"],
    deps = [
        "@maven//:org_sonatype_spice_zapper_spice-zapper",
        "@maven//:org_wvlet_airframe_airframe-http",
        "@maven//:org_scala-lang_modules_scala-java8-compat",
        "@maven//:org_wvlet_airframe_airspec",
        "@maven//:org_backuity_clist_clist-core",
        "@maven//:org_backuity_clist_clist-macros",
    ],
)


java_binary(
    name = "SonatypeOpen",
    srcs = glob(["*.scala", "sonatype/*.scala"]),
    javacopts = [
        "-encoding",
        "UTF-8",
        "-deprecation",
        "-unchecked",
    ],
    main_class = "org.jetbrains.bazel.SonatypeOpen",
    visibility = ["//visibility:public"],
    deps = [
        "@maven//:org_sonatype_spice_zapper_spice-zapper",
        "@maven//:org_wvlet_airframe_airframe-http",
        "@maven//:org_scala-lang_modules_scala-java8-compat",
        "@maven//:org_wvlet_airframe_airspec",
        "@maven//:org_backuity_clist_clist-core",
        "@maven//:org_backuity_clist_clist-macros",
    ],
)

