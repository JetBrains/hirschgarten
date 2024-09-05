rm -rf .qodana

docker run \
-v $(pwd):/data/project/ \
-v $(pwd)/.qodana/plugins/intellij-bazel:/opt/idea/custom-plugins/intellij-bazel \
-v $(pwd)/.qodana/plugins/intellij-bsp:/opt/idea/custom-plugins/intellij-bsp \
-v $(pwd)/.qodana/results/:/data/results \
-e QODANA_TOKEN=$QODANA_TOKEN \
--cpus="10" \
--memory="31g" \
--memory-swap="32g" \
jetbrains/qodana_bazel \
--property=bsp.build.project.on.sync=true \
--property=idea.is.internal=true \
--report-dir /data/results/report/ \
--save-report