package com.artier.ide.data.repository

import com.artier.ide.data.model.ColumnInfo
import com.artier.ide.data.model.DbConnection
import com.artier.ide.data.model.DbType
import com.artier.ide.data.model.QueryResult
import com.artier.ide.data.model.TableInfo
import com.artier.ide.data.remote.WebSocketClient
import com.artier.ide.data.remote.WebSocketEvent
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseRepository @Inject constructor(
    private val webSocketClient: WebSocketClient
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _activeConnection = MutableStateFlow<DbConnection?>(null)
    val activeConnection: StateFlow<DbConnection?> = _activeConnection.asStateFlow()

    private val _tables = MutableStateFlow<List<String>>(emptyList())
    val tables: StateFlow<List<String>> = _tables.asStateFlow()

    private val _selectedTable = MutableStateFlow<TableInfo?>(null)
    val selectedTable: StateFlow<TableInfo?> = _selectedTable.asStateFlow()

    private val _queryResult = MutableStateFlow<QueryResult?>(null)
    val queryResult: StateFlow<QueryResult?> = _queryResult.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events: SharedFlow<String> = _events.asSharedFlow()

    init {
        webSocketClient.addMessageHandler("db_connected") { payload ->
            val connId = payload.optString("connectionId")
            val connType = payload.optString("type")
            _activeConnection.value = DbConnection(
                id = connId,
                name = connType,
                type = DbType.fromString(connType),
                connected = true
            )
            _events.tryEmit("connected:$connId")
        }
        webSocketClient.addMessageHandler("db_disconnected") { payload ->
            val connId = payload.optString("connectionId")
            _activeConnection.value = null
            _events.tryEmit("disconnected:$connId")
        }
        webSocketClient.addMessageHandler("db_query_result") { payload ->
            val rows = mutableListOf<Map<String, Any?>>()
            val rowsArr = payload.optJSONArray("rows")
            if (rowsArr != null) {
                for (i in 0 until rowsArr.length()) {
                    val rowObj = rowsArr.optJSONObject(i) ?: continue
                    val map = mutableMapOf<String, Any?>()
                    val keys = rowObj.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        map[k] = rowObj.get(k)
                    }
                    rows.add(map)
                }
            }
            val fields = mutableListOf<String>()
            val fieldsArr = payload.optJSONArray("fields")
            if (fieldsArr != null) {
                for (i in 0 until fieldsArr.length()) {
                    fields.add(fieldsArr.optString(i))
                }
            }
            _queryResult.value = QueryResult(
                rows = rows,
                rowCount = payload.optInt("rowCount", rows.size),
                fields = fields,
                duration = payload.optLong("duration", 0),
                error = payload.optString("error").ifBlank { null }
            )
            _events.tryEmit("query_result")
        }
        webSocketClient.addMessageHandler("db_tables") { payload ->
            val tablesList = mutableListOf<String>()
            val tablesArr = payload.optJSONArray("tables")
            if (tablesArr != null) {
                for (i in 0 until tablesArr.length()) {
                    tablesList.add(tablesArr.optString(i))
                }
            }
            _tables.value = tablesList
            _events.tryEmit("tables_loaded")
        }
        webSocketClient.addMessageHandler("db_schema") { payload ->
            val tableName = payload.optString("tableName")
            val columns = mutableListOf<ColumnInfo>()
            val colsArr = payload.optJSONArray("columns")
            if (colsArr != null) {
                for (i in 0 until colsArr.length()) {
                    val colObj = colsArr.optJSONObject(i) ?: continue
                    columns.add(
                        ColumnInfo(
                            name = colObj.optString("name"),
                            type = colObj.optString("type"),
                            nullable = colObj.optBoolean("nullable", true),
                            isPrimaryKey = colObj.optBoolean("isPrimaryKey", false)
                        )
                    )
                }
            }
            _selectedTable.value = TableInfo(name = tableName, columns = columns)
            _events.tryEmit("schema_loaded")
        }
    }

    fun connect(type: DbType, host: String, port: Int, database: String, username: String, password: String) {
        val data = JsonObject().apply {
            addProperty("type", type.name.lowercase())
            addProperty("host", host)
            addProperty("port", port)
            addProperty("database", database)
            addProperty("username", username)
            addProperty("password", password)
        }
        webSocketClient.send("db_connect", data)
    }

    fun disconnect(connectionId: String) {
        val data = JsonObject().apply { addProperty("connectionId", connectionId) }
        webSocketClient.send("db_disconnect", data)
    }

    fun query(sql: String) {
        val connId = _activeConnection.value?.id ?: return
        val data = JsonObject().apply {
            addProperty("connectionId", connId)
            addProperty("sql", sql)
        }
        webSocketClient.send("db_query", data)
    }

    fun listTables() {
        val connId = _activeConnection.value?.id ?: return
        val data = JsonObject().apply { addProperty("connectionId", connId) }
        webSocketClient.send("db_tables", data)
    }

    fun getTableSchema(tableName: String) {
        val connId = _activeConnection.value?.id ?: return
        val data = JsonObject().apply {
            addProperty("connectionId", connId)
            addProperty("tableName", tableName)
        }
        webSocketClient.send("db_schema", data)
    }

    fun clearQueryResult() {
        _queryResult.value = null
    }
}
