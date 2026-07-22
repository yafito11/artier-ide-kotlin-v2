package com.artier.ide.data.model

data class DbConnection(
    val id: String,
    val name: String,
    val type: DbType,
    val host: String = "",
    val port: Int = 0,
    val database: String = "",
    val username: String = "",
    val connected: Boolean = false
)

enum class DbType {
    SQLITE, POSTGRES, LIBSQL;

    companion object {
        fun fromString(value: String?): DbType {
            return when (value?.lowercase()) {
                "postgres", "postgresql" -> POSTGRES
                "libsql" -> LIBSQL
                else -> SQLITE
            }
        }
    }
}

data class QueryResult(
    val rows: List<Map<String, Any?>> = emptyList(),
    val rowCount: Int = 0,
    val fields: List<String> = emptyList(),
    val duration: Long = 0,
    val error: String? = null
)

data class TableInfo(
    val name: String,
    val columns: List<ColumnInfo> = emptyList()
)

data class ColumnInfo(
    val name: String,
    val type: String,
    val nullable: Boolean = true,
    val isPrimaryKey: Boolean = false
)

data class DatabasePanelState(
    val activeConnection: DbConnection? = null,
    val tables: List<String> = emptyList(),
    val selectedTable: TableInfo? = null,
    val queryResult: QueryResult? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val query: String = "",
    val connectType: DbType = DbType.SQLITE,
    val connectHost: String = "127.0.0.1",
    val connectPort: String = "",
    val connectDatabase: String = "",
    val connectUsername: String = "",
    val connectPassword: String = "",
    val showConnectDialog: Boolean = false
)
