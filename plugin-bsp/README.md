[![JetBrains team project](http://jb.gg/badges/team.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)
[![TeamCity build status](https://bazel.teamcity.com/app/rest/builds/buildType:id:Bazel_IntellijBsp_IntellijBspResults/statusIcon.svg)](https://bazel.teamcity.com/project/Bazel_IntellijBsp?branch=%3Cdefault%3E&buildTypeTab=overview&mode=builds)
# Build Server Protocol for IntelliJ IDEA

<!-- Plugin description -->
## Build Server Protocol client

Allows importing projects into IntelliJ IDEA using Build Server Protocol.

Used by [experimental Bazel plugin](https://plugins.jetbrains.com/plugin/22977-bazel-by-jetbrains-experimental-).
<!-- Plugin description end -->


## Benchmarks (experimental)
**WARNING** the benchmarks will run headless intellij instance that will open the project you specify with repository_path setting.
This means it will include its build scripts or init scripts that could run any code, so **make sure you trust the project you pass to repository_path setting**.

It is possible to run a benchmark that imports an arbitrary project.

### Steps:
1. Create or clone a repository to `<repository_path>`
2. Install bazel-bsp in the repostitory, use the instructions here https://github.com/JetBrains/bazel-bsp/#installation
3. Run the following command (remember to replace <repository_path>):
```
./gradlew runIde --args="-Dbsp.benchmark.project.path=<repository_path> -Dbsp.benchmark.metrics.file=$PWD/metrics.txt -Djb.consents.confirmation.enabled=false -Djava.awt.headless=true -Djb.privacy.policy.text=<\!--999.999--> -Dide.show.tips.on.startup.default.value=false"
```
4. Read the output of metrics.txt file
