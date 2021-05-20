bazel-sonatype 
======

A set of rules for publishing your project to the Maven Central repository through the REST API of Sonatype Nexus. Allows releasing Java projects to the [Maven central repository](https://repo1.maven.org/maven2/).

## Prerequisites

 * Create a Sonatype Repository account
   * Follow the instruction in the [Central Repository documentation site](http://central.sonatype.org).
     * Create a Sonatype account
     * Create a GPG key
     * Open a JIRA ticket to get a permission for synchronizing your project to the Central Repository (aka Maven Central).
   * GPG key must be available as the default key on the machine
    
## Configurations
In the Workspace file, the following must be added in order install:
- [rules_scala](https://github.com/bazelbuild/rules_scala)
- [rules_jvm_external](https://github.com/bazelbuild/rules_jvm_external)


```python
load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

skylib_version = "1.0.3"

http_archive(
    name = "bazel_skylib",
    sha256 = "1c531376ac7e5a180e0237938a2536de0c54d93f5c278634818e0efc952dd56c",
    type = "tar.gz",
    url = "https://mirror.bazel.build/github.com/bazelbuild/bazel-skylib/releases/download/{}/bazel-skylib-{}.tar.gz".format(skylib_version, skylib_version),
)

rules_scala_version = "9bd9ffd3e52ab9e92b4f7b43051d83231743f231"

http_archive(
    name = "io_bazel_rules_scala",
    sha256 = "438bc03bbb971c45385fde5762ab368a3321e9db5aa78b96252736d86396a9da",
    strip_prefix = "rules_scala-%s" % rules_scala_version,
    type = "zip",
    url = "https://github.com/bazelbuild/rules_scala/archive/%s.zip" % rules_scala_version,
)

scala_config()

load("@io_bazel_rules_scala//scala:scala.bzl", "scala_repositories")

scala_repositories()

load("@io_bazel_rules_scala//scala:toolchains.bzl", "scala_register_toolchains")

scala_register_toolchains()

load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

RULES_JVM_EXTERNAL_TAG = "4.0"
RULES_JVM_EXTERNAL_SHA = "31701ad93dbfe544d597dbe62c9a1fdd76d81d8a9150c2bf1ecf928ecdf97169"

http_archive(
    name = "rules_jvm_external",
    strip_prefix = "rules_jvm_external-%s" % RULES_JVM_EXTERNAL_TAG,
    sha256 = RULES_JVM_EXTERNAL_SHA,
    url = "https://github.com/bazelbuild/rules_jvm_external/archive/%s.zip" % RULES_JVM_EXTERNAL_TAG,
)

#TODO: Change this to the proper version
BAZEL_SONATYPE_TAG = "28894f9ad6373a657ab4a395c8e7342277722347"
http_archive(
    name = "bazel_sonatype",
    strip_prefix = "bazel-sonatype-%s" % BAZEL_SONATYPE_TAG,
    url = "https://github.com/JetBrains/bazel-sonatype/archive/%s.zip" % BAZEL_SONATYPE_TAG,
)

load("@bazel_sonatype//:defs.bzl", "sonatype_dependencies")

sonatype_dependencies()
```
A `sonatype_java_export` must be defined in order to publish. This is similar to a java_library but with some new parameters:

```python
# /src/BUILD
load("@bazel_sonatype//:defs.bzl", "sonatype_java_export")

sonatype_java_export(
    name = "project_name",
    maven_coordinates = "com.example:project:0.0.1",
    maven_profile = "com.example",
    pom_template = "//:pom.xml", # Omittable
    srcs = glob(["*.java"]),
    deps = [
        "//src",
    ],
)
```

Publishing can now be done using `bazel run`:
```
bazel run --stamp \
  --define "maven_repo=https://oss.sonatype.org/service/local" \ # Defaults to the legacy Sonatype repository
  --define "maven_user=user" \
  --define "maven_password=password" \
  //src:project_name.publish
```

It is also possible to publish a snapshot of a build. This can be done by guaranteeing that the version in the `sonatype_java_export`'s coordinates ends with -SNAPSHOT

```python
# /src/BUILD
load("@bazel_sonatype//:defs.bzl", "sonatype_java_export")

sonatype_java_export(
    name = "project_name",
    maven_coordinates = "com.example:project:0.0.1-SNAPSHOT",
    maven_profile = "com.example",
    pom_template = "//:pom.xml", # Omittable
    srcs = glob(["*.java"]),
    deps = [
        "//src",
    ],
)
```

And then running `bazel run`:
```
bazel run --stamp \
  --define "maven_repo=https://oss.sonatype.org/service/local" \ # Defaults to the legacy Sonatype repository
  --define "maven_user=user" \
  --define "maven_password=password" \
  --define "gpg_sign=true" \ # Defaults to true
  //src:project_name.publish
```

Publishing locally all the artifacts is also possible, if a file based url is provided for `bazel run`:
```
bazel run --stamp \
  --define "maven_repo=file://$HOME/.m2/repository" \ # Defaults to the legacy Sonatype repository
  --define "gpg_sign=false" \ # Defaults to false
  //src:project_name.publish
```
