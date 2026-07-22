package com.artier.ide.data.model

/**
 * Agent skill (agentskills.io SKILL.md compatible)
 */
data class SkillInfo(
    val name: String,
    val description: String,
    val license: String? = null,
    val compatibility: String? = null,
    val metadata: Map<String, String> = emptyMap(),
    val allowedTools: String? = null,
    val path: String = "",
    val source: SkillSource = SkillSource.USER,
    val enabled: Boolean = false,
    val hasScripts: Boolean = false,
    val hasReferences: Boolean = false,
    val hasAssets: Boolean = false,
    val bodyPreview: String = ""
)

enum class SkillSource {
    BUNDLED, USER, AGENTS, PROJECT;

    companion object {
        fun fromString(value: String?): SkillSource {
            return when (value?.lowercase()) {
                "bundled" -> BUNDLED
                "agents" -> AGENTS
                "project" -> PROJECT
                else -> USER
            }
        }
    }
}

data class SkillDetail(
    val info: SkillInfo,
    val body: String = "",
    val files: List<String> = emptyList()
)

data class SkillState(
    val skills: List<SkillInfo> = emptyList(),
    val selectedSkill: SkillDetail? = null,
    val isLoading: Boolean = false,
    val isInstalling: Boolean = false,
    val query: String = "",
    val error: String? = null,
    val installPathOrUrl: String = ""
) {
    val filtered: List<SkillInfo>
        get() {
            val q = query.trim().lowercase()
            if (q.isEmpty()) return skills
            return skills.filter {
                it.name.contains(q) || it.description.lowercase().contains(q)
            }
        }

    val enabledCount: Int get() = skills.count { it.enabled }
}
