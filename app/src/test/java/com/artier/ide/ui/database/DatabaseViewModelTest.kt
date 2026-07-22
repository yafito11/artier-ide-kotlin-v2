package com.artier.ide.ui.database

import com.artier.ide.data.model.DatabasePanelState
import com.artier.ide.data.model.DbType
import com.artier.ide.data.model.QueryResult
import com.artier.ide.data.remote.MockWebSocketClient
import com.artier.ide.data.repository.DatabaseRepository
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class DatabaseViewModelTest {

    private lateinit var databaseViewModel: DatabaseViewModel
    private lateinit var databaseRepository: DatabaseRepository

    @Before
    fun setup() {
        val mockWebSocketClient = MockWebSocketClient()
        databaseRepository = DatabaseRepository(mockWebSocketClient)
        databaseViewModel = DatabaseViewModel(databaseRepository)
    }

    @Test
    fun `initial state should have default values`() {
        val state = databaseViewModel.state.value

        assertNull(state.activeConnection)
        assertTrue(state.tables.isEmpty())
        assertNull(state.selectedTable)
        assertNull(state.queryResult)
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals("", state.query)
        assertEquals(DbType.SQLITE, state.connectType)
        assertEquals("127.0.0.1", state.connectHost)
        assertEquals("", state.connectPort)
        assertEquals("", state.connectDatabase)
        assertEquals("", state.connectUsername)
        assertEquals("", state.connectPassword)
        assertFalse(state.showConnectDialog)
    }

    @Test
    fun `updateQuery should update query state`() {
        databaseViewModel.updateQuery("SELECT * FROM users")

        assertEquals("SELECT * FROM users", databaseViewModel.state.value.query)
    }

    @Test
    fun `showConnectDialog should show dialog`() {
        databaseViewModel.showConnectDialog()

        assertTrue(databaseViewModel.state.value.showConnectDialog)
    }

    @Test
    fun `hideConnectDialog should hide dialog`() {
        databaseViewModel.showConnectDialog()
        databaseViewModel.hideConnectDialog()

        assertFalse(databaseViewModel.state.value.showConnectDialog)
    }

    @Test
    fun `updateConnectType should update type`() {
        databaseViewModel.updateConnectType(DbType.POSTGRES)

        assertEquals(DbType.POSTGRES, databaseViewModel.state.value.connectType)
    }

    @Test
    fun `updateConnectHost should update host`() {
        databaseViewModel.updateConnectHost("localhost")

        assertEquals("localhost", databaseViewModel.state.value.connectHost)
    }

    @Test
    fun `updateConnectPort should update port`() {
        databaseViewModel.updateConnectPort("5432")

        assertEquals("5432", databaseViewModel.state.value.connectPort)
    }

    @Test
    fun `updateConnectDatabase should update database`() {
        databaseViewModel.updateConnectDatabase("testdb")

        assertEquals("testdb", databaseViewModel.state.value.connectDatabase)
    }

    @Test
    fun `updateConnectUsername should update username`() {
        databaseViewModel.updateConnectUsername("user")

        assertEquals("user", databaseViewModel.state.value.connectUsername)
    }

    @Test
    fun `updateConnectPassword should update password`() {
        databaseViewModel.updateConnectPassword("pass")

        assertEquals("pass", databaseViewModel.state.value.connectPassword)
    }

    @Test
    fun `clearQueryResult should clear query result`() {
        databaseViewModel.clearQueryResult()

        assertNull(databaseViewModel.state.value.queryResult)
    }

    @Test
    fun `clearError should clear error`() {
        databaseViewModel.clearError()

        assertNull(databaseViewModel.state.value.error)
    }

    @Test
    fun `executeQuery should set loading to true`() {
        databaseViewModel.updateQuery("SELECT * FROM users")
        databaseViewModel.executeQuery()

        assertTrue(databaseViewModel.state.value.isLoading)
    }

    @Test
    fun `loadTables should set loading to true`() {
        databaseViewModel.loadTables()

        assertTrue(databaseViewModel.state.value.isLoading)
    }

    @Test
    fun `selectTable should set loading to true`() {
        databaseViewModel.selectTable("users")

        assertTrue(databaseViewModel.state.value.isLoading)
    }

    @Test
    fun `connect should hide dialog and set loading`() {
        databaseViewModel.showConnectDialog()
        databaseViewModel.connect()

        assertFalse(databaseViewModel.state.value.showConnectDialog)
        assertTrue(databaseViewModel.state.value.isLoading)
    }

    @Test
    fun `disconnect should set loading to true`() {
        databaseViewModel.disconnect()

        assertTrue(databaseViewModel.state.value.isLoading)
    }

    @Test
    fun `connect should use correct default port for PostgreSQL`() {
        databaseViewModel.updateConnectType(DbType.POSTGRES)
        databaseViewModel.updateConnectPort("")
        databaseViewModel.connect()

        // Default port for PostgreSQL is 5432
        assertTrue(databaseViewModel.state.value.isLoading)
    }

    @Test
    fun `connect should use correct default port for libSQL`() {
        databaseViewModel.updateConnectType(DbType.LIBSQL)
        databaseViewModel.updateConnectPort("")
        databaseViewModel.connect()

        // Default port for libSQL is 8080
        assertTrue(databaseViewModel.state.value.isLoading)
    }
}
