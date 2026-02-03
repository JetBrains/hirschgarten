# Plugin Development Setup Guide

This guide provides instructions for setting up and developing our Bazel plugin for JetBrains IDEA.  
Follow these steps to get your development environment ready.

### Requirements

1) IntelliJ IDEA 2025.3 or later Release, Release Candidate or EAP version.
2) Install the [Bazel plugin](https://plugins.jetbrains.com/plugin/22977-bazel-eap-/).
3) [Plugin DevKit](https://plugins.jetbrains.com/plugin/22851-plugin-devkit/versions/stable)

### How to develop/debug plugins

1) `git clone git@github.com:JetBrains/hirschgarten.git`
2) Open the cloned `hirschgarten` repo in IDEA and wait for it to import.
3) Open "File->Project Structure". This opens the Project Structure dialog.  
   Click "SDKs".
4) Hit the "+" button.  
   Click "Download JDK".  
   Install JetBrains Runtime (JCEF) version 21.  
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
   Right-click `plugin-bazel-debug` and click `Run`.
   <img src="../files/DEVELOPMENT_SETUP_4.png" width="600">
8) The following plugin runs can be started by clicking "Run" button in the upper right corner, next to the now present run configuration.
9) If you want to build a deployable plugin, build `plugin-bazel_zip`.
### Conclusion

After completing these steps, your development environment should be ready for plugin development. If you encounter any issues not covered here, please reach out to the team.