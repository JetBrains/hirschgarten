### Prerequisites to run CLion tests locally:

Because running CLion tests require .NET framework and Rider backed, they are located in a separate module.

1) Install .NET framework:

https://dotnet.microsoft.com/en-us/download/dotnet/10.0

2) Compile Rider backend:
``` shell
cd dotnet && Rider/compileBackend_Radler.cmd
```

3) [TEMPORARY] because of https://youtrack.jetbrains.com/issue/MRI-3220

Modify the `BUILD.bazel` file 
``` bazel
jps_test(
    name = "pluginTests.clion_test",
    runtime_deps = [":pluginTests.clion_test_lib"],
    env = {
        "RESHARPER_HOST_BIN": "/Users/...../ultimate/dotnet/Bin.RiderBackend",
    },
    jvm_flags = [
        "-Dpatch.engine.backend.freeze.timeout=-1"
    ],    
)

```

Please note that this change will be overwritten on each jps-to-bazel call 