# Plugin Development Setup Guide

This guide provides instructions for setting up and developing our Bazel plugin for JetBrains IDEA.  
Follow these steps to get your development environment ready.

### Requirements

1) IntelliJ IDEA 2025.1 Nightly or later Release, Release Candidate or EAP version.
2) Install the latest plugins as described on our [landing page](https://lp.jetbrains.com/new-bazel-plugin/#:~:text=Install%20plugin).  
   Note: Our debug/developer run configurations currently require both plugins to be installed.
3) [Plugin DevKit](https://plugins.jetbrains.com/plugin/22851-plugin-devkit/versions/stable)

### How to develop/debug plugins

1) `git clone git@github.com:JetBrains/hirschgarten.git`
2) Open the cloned `hirschgarten` repo in IDEA and wait for it to import.
3) Open "File->Project Structure". This opens the Project Structure dialog.  
   Click "SDKs".
4) Hit the "+" button.  
   Click "Download JDK".  
   Install JetBrains Runtime (JCEF) version 21.  
   If JetBrains Runtime (JCEF) version 21 is not available, version 17 will also work.  
   <img src="../files/DEVELOPMENT_SETUP_1.png" width="600">
5) Hit the "+" button.  
   Click "Add IntelliJ Platform Plugin SDK from disk".  
   A file exporer with the 'Contents' folder will open.  
   Click "Open" in this window.  
   <img src="../files/DEVELOPMENT_SETUP_2.png" width="600">
6) A "Select Internal Java Platform" dialog will open.  
   Select `jbrsdk_jcef-21` (or `jbrsdk_jcef-17` if you didn't have version 21 in step 4) in the dropdown list and click "OK".  
   Then click "OK" again.  
   <img src="../files/DEVELOPMENT_SETUP_3.png" width="600">
7) In the target view, expand the `plugin-bazel` subtree.  
   Right-click `plugin-bazel-with-server-debug` and click `Run`.  
   <img src="../files/DEVELOPMENT_SETUP_4.png" width="600">
   **Note**: if you are interested in Bazel BSP connection flow, please use `plugin-bazel/plugin-bazel-debug` instead, 
   as `plugin-bazel/plugin-bazel-with-server-debug` will bypass the normal BSP connection flow.
   If you are only interested in BSP plugin, please use `plugin-bsp/plugin-bsp-debug`.
8) The following plugin runs can be started by clicking "Run" button in the upper right corner, next to the now present run configuration.

### Troubleshooting

Common Issues:
- If you don't have JetBrains Runtime (JCEF) version 21, select version 17. 
- JetBrains Runtime (JCEF) version 21 should be available for anyone in JetBrains organization - if you don't see it, make sure your JetBrains Toolbox and IDEA are logged in with your @jetbrains.com email

### Conclusion

After completing these steps, your development environment should be ready for plugin development. If you encounter any issues not covered here, please reach out to the team.