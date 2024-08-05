# Plugin Development Setup Guide

This guide provides instructions for setting up and developing our Bazel plugin for JetBrains IDEA.  
Follow these steps to get your development environment ready.

### Requirements

1) IntelliJ IDEA 2024.2 Nightly or later Release, Release Candidate or EAP version.
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
   <img src="../files/DEVELOPMENT_SETUP_1.png" width="400">
5) Hit the "+" button.  
   Click "Add IntelliJ Platform Plugin SDK from disk".  
   A file exporer with the 'Contents' folder will open.  
   Click "Open" in this window.  
   <img src="../files/DEVELOPMENT_SETUP_2.png" width="400">
6) A "Select Internal Java Platform" dialog will open.  
   Select `jbrsdk_jcef-21` (or `jbrsdk_jcef-17` if you didn't have 21 version in step 4) in the dropdown list and click "OK".  
   Then click "OK" again.  
   <img src="../files/DEVELOPMENT_SETUP_3.png" width="400">
7) In the target view, expand the `plugin-bazel` subtree.  
   Right-click "plugin-bazel-with-server-debug" and click "Run".  
   You can use the created run configuration for the current session, but you'll have to right-click the target again next time you open the project.  
   This run configuration is specifically set up for plugin development and debugging. Note that running the target from the terminal won't work, as it's tied to the IDE-generated run configuration.  
   <img src="../files/DEVELOPMENT_SETUP_4.png" width="400">

### Troubleshooting

Common Issues:
- If you don't have JetBrains Runtime (JCEF) version 21, select version 17. 

### Conclusion

After completing these steps, your development environment should be ready for plugin development. If you encounter any issues not covered here, please reach out to the team.