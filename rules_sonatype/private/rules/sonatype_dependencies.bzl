load("@rules_jvm_external//:defs.bzl", "maven_install")

def sonatype_dependencies():
    maven_install(
        name = "bazel_sonatype_deps",
        artifacts = [
            "org.sonatype.spice.zapper:spice-zapper:1.3",
            "org.wvlet.airframe:airframe-http_2.13:20.12.1",
            "org.scala-lang.modules:scala-java8-compat_2.13:0.9.1",
            "org.wvlet.airframe:airspec_2.13:20.12.1",
            "org.backuity.clist:clist-core_2.13:3.5.1",
            "org.backuity.clist:clist-macros_2.13:3.5.1",
        ],
        generate_compat_repositories = True,
        repositories = [
            "https://jcenter.bintray.com/",
            "https://maven.google.com",
            "https://repo1.maven.org/maven2",
        ],
    )
