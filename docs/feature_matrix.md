# Bazel feature matrix

|     | Support level                                |
|-----|----------------------------------------------|
| ✅   | fully supported                              |
| 🚩  | available behind feature flag / with caveats |
| 🛠️ | work in progress                             |
| ❌   | currently not supported                      |

| **language**                     | **import** | **editor** | **build** |                 | **run** | **debug** | **test**       |                |             |             |
|----------------------------------|:-----------|------------|-----------|-----------------|---------|----------|----------------|----------------|-------------|-------------|
|                                  |            |            |           | **diagnostics** |         |          | **run target** | **run gutter** | **reports** | **filters** |
| **JVM**                          |            |            |           |                 |         |          |                |                |             |             |
| Java                             | ✅          | ✅          | ✅         | ✅               | ✅       | ✅        | ✅              |                |             |             |
| Kotlin                           | ✅          | ✅          | ✅         | ✅               | ✅       | ✅        | ✅              |                |             |             |
| Scala (*)                        | ✅          | ✅          | ✅         | ✅               | ✅       | ✅        | ✅              |                |             |             |
| **[Android](guides/android.md)** | 🚩         | 🚩         | 🚩        | 🚩              | 🚩      | 🚩       |                |                |             |             |
| **[Python](guides/python.md)**   | 🚩         | 🚩         | 🚩        | 🚩              | 🚩      | 🚩       |                |                |             |             |
| **[Go](guides/go.md)**           | 🚩         | 🚩         | 🚩        | 🚩              | 🚩      | 🚩       |                |                |             |             |
| **Rust**                         | 🚩         | ❌          | 🚩        | ❌               | 🚩      | ❌        |                |                |             |             |
| **Starlark**                     |            | ✅          |           | ✅               |         | ✅       |                |                |             |             |
|                                  |            |            |           |                 |         |          |                |                |             |             |
| **framework**                    |            |            |           |                 |         |          |                |                |             |             |
| JUnit4                           |            |            |           |                 | ✅       | ✅        | ✅              | ✅              | ✅           | 🛠️         |
| JUnit5                           |            |            |           |                 | ✅       | ✅        | ✅              | ❌              | ✅           | 🛠️         |

(🚩) To enable support, follow the linked guides. Features behind flags are under development and may not be fully functional. 
(*) Scala support is currently only available with nightly versions from `intellij-scala`.