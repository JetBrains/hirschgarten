# Bazel feature matrix

|     | Support level                                |
| --- |----------------------------------------------|
| ✅   | fully supported                              |
| 🚩  | available behind feature flag / with caveats |
| 🛠️ | work in progress                             |
| ❌   | currently not supported                      |


| **language /<br>framework** | **import** | **editor** | **build** |                 | **run** | **debug** | **test**       |                |             |             |
| --------------------------- | ---------- | ---------- | --------- | --------------- | ------- | --------- | -------------- | -------------- | ----------- | ----------- |
|                             |            |            |           | **diagnostics** |         |           | **run target** | **run gutter** | **reports** | **filters** |
| **JVM**                     |            |            |           |                 |         |           |                |                |             |             |
| Java                        | ✅          | ✅          | ✅         | ✅               | ✅       | ✅         | ✅              |                |             |             |
| Kotlin                      | ✅          | ✅          | ✅         | ✅               | ✅       | ✅         | ✅              |                |             |             |
| Scala                       | ✅          | ✅          | ✅         | ✅               | ✅       | ✅         | ✅              |                |             |             |
| **Android**                 | 🚩         | 🚩         | 🚩        | 🚩              | 🚩      | 🚩        |                |                |             |             |
| **Python**                  | 🚩         | 🚩         | 🚩        | 🚩              | 🚩      | 🚩        |                |                |             |             |
| **Go**                      | 🚩         | 🚩         | 🚩        | 🚩              | 🚩      | 🚩        |                |                |             |             |
| **Rust**                    | 🚩         | ❌          | 🚩        | ❓               | 🚩<br>  | ❓         |                |                |             |             |
| **Starlark**                |            | ✅          |           | ✅               |         | 🛠️       |                |                |             |             |
|                             |            |            |           |                 |         |           |                |                |             |             |
| **framework**               |            |            |           |                 |         |           |                |                |             |             |
| JUnit4                      |            |            |           |                 | ✅       | ✅         | ✅              | ❓              | ✅           | 🛠️         |
| JUnit5                      |            |            |           |                 | ✅       | ✅         | ✅              | ❓              | ✅           | 🛠️         |
