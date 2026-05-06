The issue — target work_request_handlers in BUILD.bazel does not have a dependency on @maven//:com_google_errorprone_error_prone_annotations, so the build failed.

```
bazelisk build //...
INFO: Analyzed 3 targets (12 packages loaded, 371 targets configured).
ERROR: /Users/develar/projects/jps-bazel/BUILD.bazel:10:13: Building libwork_request_handlers.jar (2 source files) failed: (Exit 1): java failed: error executing Javac command (from target //:work_request_handlers) external/rules_java~~toolchains~remotejdk21_macos_aarch64/bin/java '--add-exports=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED' '--add-exports=jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED' ... (remaining 19 arguments skipped)
src/WorkRequestHandler.java:297: error: [strict] Using type com.google.common.annotations.VisibleForTesting from an indirect dependency (TOOL_INFO: "@maven//:com_google_guava_guava"). See command below **
  @VisibleForTesting
   ^
src/WorkRequestHandler.java:551: error: [strict] Using type com.google.errorprone.annotations.CanIgnoreReturnValue from an indirect dependency (TOOL_INFO: "@maven//:com_google_errorprone_error_prone_annotations"). See command below **
    @CanIgnoreReturnValue
     ^
 ** Please add the following dependencies:
  @maven//:com_google_errorprone_error_prone_annotations @maven//:com_google_guava_guava to //:work_request_handlers
 ** You can use the following buildozer command:
buildozer 'add deps @maven//:com_google_errorprone_error_prone_annotations @maven//:com_google_guava_guava' //:work_request_handlers

Use --verbose_failures to see the command lines of failed build steps.
INFO: Elapsed time: 0.706s, Critical Path: 0.17s
INFO: 2 processes: 2 internal.
ERROR: Build did NOT complete successfully
```