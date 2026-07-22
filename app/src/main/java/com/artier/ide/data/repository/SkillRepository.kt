package com.artier.ide.data.repository

import com.artier.ide.data.model.SkillDetail
import com.artier.ide.data.model.SkillInfo
import com.artier.ide.data.model.SkillSource
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
class SkillRepository @Inject constructor(
    private val webSocketClient: WebSocketClient
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _skills = MutableStateFlow<List<SkillInfo>>(emptyList())
    val skills: StateFlow<List<SkillInfo>> = _skills.asStateFlow()

    private val _selected = MutableStateFlow<SkillDetail?>(null)
    val selected: StateFlow<SkillDetail?> = _selected.asStateFlow()

    private val _agentContext = MutableStateFlow("")
    val agentContext: StateFlow<String> = _agentContext.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events: SharedFlow<String> = _events.asSharedFlow()

    init {
        webSocketClient.addMessageHandler("skill_list") { payload ->
            _skills.value = parseSkillList(payload)
        }
        webSocketClient.addMessageHandler("skill_detail") { payload ->
            val skillObj = payload.optJSONObject("skill") ?: return@addMessageHandler
            _selected.value = parseSkillDetail(skillObj)
        }
        webSocketClient.addMessageHandler("skill_installed") { payload ->
            val skillObj = payload.optJSONObject("skill")
            if (skillObj != null) {
                val info = parseSkillInfo(skillObj)
                _skills.value = (_skills.value.filter { it.name != info.name } + info)
                    .sortedBy { it.name }
            }
            _events.tryEmit("installed")
        }
        webSocketClient.addMessageHandler("skill_uninstalled") { payload ->
            val name = payload.optString("name")
            if (name.isNotBlank()) {
                _skills.value = _skills.value.filter { it.name != name }
                if (_selected.value?.info?.name == name) _selected.value = null
            }
            _events.tryEmit("uninstalled")
        }
        webSocketClient.addMessageHandler("skill_updated") { payload ->
            val skillObj = payload.optJSONObject("skill") ?: return@addMessageHandler
            val info = parseSkillInfo(skillObj)
            _skills.value = _skills.value.map { if (it.name == info.name) info else it }
            _events.tryEmit("updated")
        }
        webSocketClient.addMessageHandler("skill_context") { payload ->
            _agentContext.value = payload.optString("context")
        }

        scope.launch {
            webSocketClient.events.collect { event ->
                if (event is WebSocketEvent.Connected || event is WebSocketEvent.DaemonReady) {
                    scan()
                }
            }
        }
    }

    fun scan(projectRoot: String? = null) {
        val data = JsonObject()
        if (projectRoot != null) data.addProperty("projectRoot", projectRoot)
        webSocketClient.send("skill_scan", data)
    }

    fun list() {
        webSocketClient.send("skill_list", JsonObject())
    }

    fun get(name: String) {
        val data = JsonObject().apply { addProperty("name", name) }
        webSocketClient.send("skill_get", data)
    }

    fun setEnabled(name: String, enabled: Boolean) {
        val data = JsonObject().apply {
            addProperty("name", name)
            addProperty("enabled", enabled)
        }
        webSocketClient.send("skill_set_enabled", data)
        // Optimistic
        _skills.value = _skills.value.map {
            if (it.name == name) it.copy(enabled = enabled) else it
        }
    }

    fun installFromPath(path: String) {
        val data = JsonObject().apply { addProperty("path", path) }
        webSocketClient.send("skill_install", data)
    }

    fun installFromUrl(url: String) {
        val data = JsonObject().apply { addProperty("url", url) }
        webSocketClient.send("skill_install", data)
    }

    fun uninstall(name: String) {
        val data = JsonObject().apply { addProperty("name", name) }
        webSocketClient.send("skill_uninstall", data)
    }

    fun refreshAgentContext() {
        webSocketClient.send("skill_context", JsonObject())
    }

    fun getEnabledContextLocal(): String {
        val enabled = _skills.value.filter { it.enabled }
        if (enabled.isEmpty()) return ""
        val lines = mutableListOf("# Active Skills", "")
        for (s in enabled) {
            lines += "- **${s.name}**: ${s.description}"
        }
        val detail = _selected.value
        if (detail != null && detail.info.enabled && detail.body.isNotBlank()) {
            lines += ""
            lines += "## Skill: ${detail.info.name}"
            lines += ""
            lines += detail.body
        }
        return lines.joinToString("\n")
    }

    private fun parseSkillList(payload: JSONObject): List<SkillInfo> {
        val arr = payload.optJSONArray("skills") ?: return emptyList()
        val list = mutableListOf<SkillInfo>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            list.add(parseSkillInfo(obj))
        }
        return list.sortedBy { it.name }
    }

    private fun parseSkillInfo(obj: JSONObject): SkillInfo {
        val meta = mutableMapOf<String, String>()
        val metaObj = obj.optJSONObject("metadata")
        if (metaObj != null) {
            val keys = metaObj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                meta[k] = metaObj.optString(k)
            }
        }
        return SkillInfo(
            name = obj.optString("name"),
            description = obj.optString("description"),
            license = obj.optString("license").ifBlank { null },
            compatibility = obj.optString("compatibility").ifBlank { null },
            metadata = meta,
            allowedTools = obj.optString("allowedTools").ifBlank {
                obj.optString("allowed-tools").ifBlank { null }
            },
            path = obj.optString("path"),
            source = SkillSource.fromString(obj.optString("source")),
            enabled = obj.optBoolean("enabled", false),
            hasScripts = obj.optBoolean("hasScripts", false),
            hasReferences = obj.optBoolean("hasReferences", false),
            hasAssets = obj.optBoolean("hasAssets", false),
            bodyPreview = obj.optString("bodyPreview")
        )
    }

    private fun parseSkillDetail(obj: JSONObject): SkillDetail {
        val info = parseSkillInfo(obj)
        val files = mutableListOf<String>()
        val filesArr = obj.optJSONArray("files")
        if (filesArr != null) {
            for (i in 0 until filesArr.length()) {
                files.add(filesArr.optString(i))
            }
        }
        return SkillDetail(
            info = info,
            body = obj.optString("body"),
            files = files
        )
    }
}
