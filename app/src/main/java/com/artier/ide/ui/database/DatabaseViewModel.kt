package com.artier.ide.ui.database

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artier.ide.data.model.DatabasePanelState
import com.artier.ide.data.model.DbType
import com.artier.ide.data.repository.DatabaseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DatabaseViewModel @Inject constructor(
    private val databaseRepository: DatabaseRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DatabasePanelState())
    val state: StateFlow<DatabasePanelState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            databaseRepository.activeConnection.collect { conn ->
                _state.update { it.copy(activeConnection = conn) }
            }
        }
        viewModelScope.launch {
            databaseRepository.tables.collect { tables ->
                _state.update { it.copy(tables = tables) }
            }
        }
        viewModelScope.launch {
            databaseRepository.selectedTable.collect { table ->
                _state.update { it.copy(selectedTable = table) }
            }
        }
        viewModelScope.launch {
            databaseRepository.queryResult.collect { result ->
                _state.update { it.copy(queryResult = result, isLoading = false) }
            }
        }
        viewModelScope.launch {
            databaseRepository.events.collect { event ->
                when {
                    event.startsWith("connected:") -> {
                        _state.update { it.copy(isLoading = false, error = null) }
                    }
                    event.startsWith("disconnected:") -> {
                        _state.update { it.copy(isLoading = false) }
                    }
                    event == "query_result" -> {
                        _state.update { it.copy(isLoading = false) }
                    }
                    event == "tables_loaded" -> {
                        _state.update { it.copy(isLoading = false) }
                    }
                }
            }
        }
    }

    fun updateQuery(query: String) {
        _state.update { it.copy(query = query) }
    }

    fun executeQuery() {
        val sql = _state.value.query.trim()
        if (sql.isBlank()) return
        _state.update { it.copy(isLoading = true, error = null) }
        databaseRepository.query(sql)
    }

    fun loadTables() {
        _state.update { it.copy(isLoading = true, error = null) }
        databaseRepository.listTables()
    }

    fun selectTable(tableName: String) {
        _state.update { it.copy(isLoading = true, error = null) }
        databaseRepository.getTableSchema(tableName)
    }

    fun showConnectDialog() {
        _state.update { it.copy(showConnectDialog = true) }
    }

    fun hideConnectDialog() {
        _state.update { it.copy(showConnectDialog = false) }
    }

    fun updateConnectType(type: DbType) {
        _state.update { it.copy(connectType = type) }
    }

    fun updateConnectHost(host: String) {
        _state.update { it.copy(connectHost = host) }
    }

    fun updateConnectPort(port: String) {
        _state.update { it.copy(connectPort = port) }
    }

    fun updateConnectDatabase(database: String) {
        _state.update { it.copy(connectDatabase = database) }
    }

    fun updateConnectUsername(username: String) {
        _state.update { it.copy(connectUsername = username) }
    }

    fun updateConnectPassword(password: String) {
        _state.update { it.copy(connectPassword = password) }
    }

    fun connect() {
        val s = _state.value
        val port = s.connectPort.toIntOrNull() ?: when (s.connectType) {
            DbType.POSTGRES -> 5432
            DbType.LIBSQL -> 8080
            DbType.SQLITE -> 0
        }
        _state.update { it.copy(isLoading = true, error = null, showConnectDialog = false) }
        databaseRepository.connect(
            type = s.connectType,
            host = s.connectHost,
            port = port,
            database = s.connectDatabase,
            username = s.connectUsername,
            password = s.connectPassword
        )
    }

    fun disconnect() {
        val connId = _state.value.activeConnection?.id ?: return
        _state.update { it.copy(isLoading = true) }
        databaseRepository.disconnect(connId)
    }

    fun clearQueryResult() {
        _state.update { it.copy(queryResult = null) }
        databaseRepository.clearQueryResult()
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
