package com.example

import com.example.processor.GenerateTable

internal const val TABLE_PREFIX: String = "example_"

@GenerateTable
data class User(val id: Long, val name: String)

internal class UserRepository {
  fun tableName(): String = UserTable.NAME

  fun qualifiedTableName(): String = UserTable.qualifiedName()
}
