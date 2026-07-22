package com.artier.ide.data.model

import org.junit.Assert.*
import org.junit.Test

class DatabaseModelsTest {

    @Test
    fun `DbConnection creation should work`() {
        val connection = DbConnection(
            id = "conn-1",
            name = "Test DB",
            type = DbType.POSTGRES,
            host = "localhost",
            port = 5432,
            database = "testdb",
            username = "user",
            connected = true
        )

        assertEquals("conn-1", connection.id)
        assertEquals("Test DB", connection.name)
        assertEquals(DbType.POSTGRES, connection.type)
        assertEquals("localhost", connection.host)
        assertEquals(5432, connection.port)
        assertEquals("testdb", connection.database)
        assertEquals("user", connection.username)
        assertTrue(connection.connected)
    }

    @Test
    fun `DbConnection default values should be safe`() {
        val connection = DbConnection(
            id = "test",
            name = "test",
            type = DbType.SQLITE
        )

        assertFalse(connection.connected)
        assertEquals("", connection.host)
        assertEquals(0, connection.port)
        assertEquals("", connection.database)
        assertEquals("", connection.username)
    }

    @Test
    fun `DbType fromString should work`() {
        assertEquals(DbType.POSTGRES, DbType.fromString("postgres"))
        assertEquals(DbType.POSTGRES, DbType.fromString("postgresql"))
        assertEquals(DbType.LIBSQL, DbType.fromString("libsql"))
        assertEquals(DbType.SQLITE, DbType.fromString("sqlite"))
        assertEquals(DbType.SQLITE, DbType.fromString(null))
        assertEquals(DbType.SQLITE, DbType.fromString("unknown"))
    }

    @Test
    fun `QueryResult creation should work`() {
        val result = QueryResult(
            rows = listOf(mapOf("id" to 1, "name" to "Alice")),
            rowCount = 1,
            fields = listOf("id", "name"),
            duration = 42,
            error = null
        )

        assertEquals(1, result.rows.size)
        assertEquals(1, result.rowCount)
        assertEquals(2, result.fields.size)
        assertEquals(42, result.duration)
        assertNull(result.error)
    }

    @Test
    fun `QueryResult with error should work`() {
        val result = QueryResult(
            error = "SQL syntax error"
        )

        assertNotNull(result.error)
        assertEquals("SQL syntax error", result.error)
    }

    @Test
    fun `TableInfo creation should work`() {
        val table = TableInfo(
            name = "users",
            columns = listOf(
                ColumnInfo(name = "id", type = "INTEGER", nullable = false, isPrimaryKey = true),
                ColumnInfo(name = "name", type = "TEXT", nullable = true, isPrimaryKey = false)
            )
        )

        assertEquals("users", table.name)
        assertEquals(2, table.columns.size)
    }

    @Test
    fun `ColumnInfo creation should work`() {
        val column = ColumnInfo(
            name = "id",
            type = "INTEGER",
            nullable = false,
            isPrimaryKey = true
        )

        assertEquals("id", column.name)
        assertEquals("INTEGER", column.type)
        assertFalse(column.nullable)
        assertTrue(column.isPrimaryKey)
    }

    @Test
    fun `DatabasePanelState default values should be correct`() {
        val state = DatabasePanelState()

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
}

class AiModelsTest {

    @Test
    fun `ChatMessage creation should work`() {
        val message = ChatMessage(
            id = "msg-1",
            role = MessageRole.USER,
            content = "Hello",
            agentName = "opencode",
            timestamp = 1700000000000L,
            isStreaming = false,
            toolCalls = emptyList(),
            isError = false
        )

        assertEquals("msg-1", message.id)
        assertEquals(MessageRole.USER, message.role)
        assertEquals("Hello", message.content)
        assertEquals("opencode", message.agentName)
        assertEquals(1700000000000L, message.timestamp)
        assertFalse(message.isStreaming)
        assertTrue(message.toolCalls.isEmpty())
        assertFalse(message.isError)
    }

    @Test
    fun `MessageRole should have all values`() {
        val roles = MessageRole.values()

        assertEquals(4, roles.size)
        assertNotNull(MessageRole.USER)
        assertNotNull(MessageRole.ASSISTANT)
        assertNotNull(MessageRole.SYSTEM)
        assertNotNull(MessageRole.TOOL)
    }

    @Test
    fun `ToolCallInfo creation should work`() {
        val toolCall = ToolCallInfo(
            id = "tc-1",
            name = "read_file",
            arguments = "{\"path\": \"/test.kt\"}",
            result = "file content"
        )

        assertEquals("tc-1", toolCall.id)
        assertEquals("read_file", toolCall.name)
        assertEquals("{\"path\": \"/test.kt\"}", toolCall.arguments)
        assertEquals("file content", toolCall.result)
    }

    @Test
    fun `ChatSession creation should work`() {
        val session = ChatSession(
            id = "session-1",
            title = "Test Chat",
            agentName = "opencode",
            messages = emptyList(),
            createdAt = 1700000000000L,
            updatedAt = 1700000000000L,
            isActive = true
        )

        assertEquals("session-1", session.id)
        assertEquals("Test Chat", session.title)
        assertEquals("opencode", session.agentName)
        assertTrue(session.messages.isEmpty())
        assertTrue(session.isActive)
    }

    @Test
    fun `AiState default values should be correct`() {
        val state = AiState()

        assertTrue(state.sessions.isEmpty())
        assertNull(state.activeSessionId)
        assertTrue(state.availableAgents.isEmpty())
        assertNull(state.selectedAgent)
        assertFalse(state.isProcessing)
        assertNull(state.error)
    }

    @Test
    fun `AiState activeSession should return correct session`() {
        val session = ChatSession(
            id = "session-1",
            title = "Test",
            agentName = "opencode"
        )

        val state = AiState(
            sessions = listOf(session),
            activeSessionId = "session-1"
        )

        assertEquals(session, state.activeSession)
    }

    @Test
    fun `AiState messages should return messages from active session`() {
        val message = ChatMessage(
            id = "msg-1",
            role = MessageRole.USER,
            content = "Hello"
        )

        val session = ChatSession(
            id = "session-1",
            title = "Test",
            agentName = "opencode",
            messages = listOf(message)
        )

        val state = AiState(
            sessions = listOf(session),
            activeSessionId = "session-1"
        )

        assertEquals(1, state.messages.size)
        assertEquals(message, state.messages.first())
    }
}

class SkillModelsTest {

    @Test
    fun `SkillInfo creation should work`() {
        val skill = SkillInfo(
            name = "my-skill",
            description = "A test skill",
            license = "MIT",
            compatibility = ">=1.0",
            metadata = mapOf("author" to "test"),
            allowedTools = "read,write",
            path = "/skills/my-skill",
            source = SkillSource.USER,
            enabled = true,
            hasScripts = true,
            hasReferences = false,
            hasAssets = false,
            bodyPreview = "This is a skill..."
        )

        assertEquals("my-skill", skill.name)
        assertEquals("A test skill", skill.description)
        assertEquals("MIT", skill.license)
        assertEquals(">=1.0", skill.compatibility)
        assertEquals("test", skill.metadata["author"])
        assertEquals("read,write", skill.allowedTools)
        assertEquals("/skills/my-skill", skill.path)
        assertEquals(SkillSource.USER, skill.source)
        assertTrue(skill.enabled)
        assertTrue(skill.hasScripts)
        assertFalse(skill.hasReferences)
        assertFalse(skill.hasAssets)
        assertEquals("This is a skill...", skill.bodyPreview)
    }

    @Test
    fun `SkillSource fromString should work`() {
        assertEquals(SkillSource.BUNDLED, SkillSource.fromString("bundled"))
        assertEquals(SkillSource.AGENTS, SkillSource.fromString("agents"))
        assertEquals(SkillSource.PROJECT, SkillSource.fromString("project"))
        assertEquals(SkillSource.USER, SkillSource.fromString("user"))
        assertEquals(SkillSource.USER, SkillSource.fromString(null))
        assertEquals(SkillSource.USER, SkillSource.fromString("unknown"))
    }

    @Test
    fun `SkillDetail creation should work`() {
        val skill = SkillInfo(
            name = "my-skill",
            description = "A test skill"
        )

        val detail = SkillDetail(
            info = skill,
            body = "Skill body content",
            files = listOf("README.md", "script.sh")
        )

        assertEquals(skill, detail.info)
        assertEquals("Skill body content", detail.body)
        assertEquals(2, detail.files.size)
    }

    @Test
    fun `SkillState default values should be correct`() {
        val state = SkillState()

        assertTrue(state.skills.isEmpty())
        assertNull(state.selectedSkill)
        assertFalse(state.isLoading)
        assertFalse(state.isInstalling)
        assertEquals("", state.query)
        assertNull(state.error)
        assertEquals("", state.installPathOrUrl)
    }

    @Test
    fun `SkillState filtered should filter by query`() {
        val skills = listOf(
            SkillInfo(name = "read-file", description = "Read files"),
            SkillInfo(name = "write-file", description = "Write files"),
            SkillInfo(name = "search", description = "Search the web")
        )

        val state = SkillState(
            skills = skills,
            query = "file"
        )

        assertEquals(2, state.filtered.size)
        assertTrue(state.filtered.all { it.name.contains("file") || it.description.lowercase().contains("file") })
    }

    @Test
    fun `SkillState enabledCount should count enabled skills`() {
        val skills = listOf(
            SkillInfo(name = "skill1", description = "S1", enabled = true),
            SkillInfo(name = "skill2", description = "S2", enabled = false),
            SkillInfo(name = "skill3", description = "S3", enabled = true)
        )

        val state = SkillState(skills = skills)

        assertEquals(2, state.enabledCount)
    }
}

class TerminalModelsTest {

    @Test
    fun `TerminalSession creation should work`() {
        val session = TerminalSession(
            id = "session-1",
            title = "Terminal 1",
            workingDirectory = "/home/user",
            isActive = true,
            createdAt = 1700000000000L
        )

        assertEquals("session-1", session.id)
        assertEquals("Terminal 1", session.title)
        assertEquals("/home/user", session.workingDirectory)
        assertTrue(session.isActive)
        assertEquals(1700000000000L, session.createdAt)
    }

    @Test
    fun `TerminalState default values should be correct`() {
        val state = TerminalState()

        assertTrue(state.sessions.isEmpty())
        assertNull(state.activeSessionId)
        assertTrue(state.outputBuffer.isEmpty())
    }

    @Test
    fun `TerminalState activeSession should return correct session`() {
        val session = TerminalSession(
            id = "session-1",
            title = "Terminal 1",
            workingDirectory = "/home/user"
        )

        val state = TerminalState(
            sessions = listOf(session),
            activeSessionId = "session-1"
        )

        assertEquals(session, state.activeSession)
    }

    @Test
    fun `TerminalState getOutput should return output for session`() {
        val session = TerminalSession(
            id = "session-1",
            title = "Terminal 1",
            workingDirectory = "/home/user"
        )

        val state = TerminalState(
            sessions = listOf(session),
            activeSessionId = "session-1",
            outputBuffer = mapOf("session-1" to listOf("output line 1", "output line 2"))
        )

        val output = state.getOutput("session-1")
        assertEquals("output line 1\noutput line 2", output)
    }

    @Test
    fun `TerminalState getOutput should return empty for nonexistent session`() {
        val state = TerminalState()

        val output = state.getOutput("nonexistent")
        assertEquals("", output)
    }
}
