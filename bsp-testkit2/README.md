# bsp-testkit2

Experimental new testkit for Build Server Protocol.

`ProtocolSuite.kt` contains tests which are build-tool-independent and test server's compliance with Build Server Protocol.

To use them, run:
```shell
bazel run //bsp-testkit/client:ProtocolSuite -- <path_to_bsp_project_directory>
```

Note that depending on tested server and the size of the BSP project, tests timeouts should be adjusted accordingly.