package com.artier.ide.agent

/**
 * Environment variables that point CLI agents at the local 9Router gateway.
 * Applied to all ProcessBuilder-based adapters.
 */
object AgentRouterEnv {
    const val ROUTER_BASE = "http://127.0.0.1:20128/v1"
    const val ROUTER_HOST = "127.0.0.1"
    const val ROUTER_PORT = 20128

    fun forProcess(extra: Map<String, String> = emptyMap()): Map<String, String> {
        return mapOf(
            // OpenAI-compatible clients
            "OPENAI_BASE_URL" to ROUTER_BASE,
            "OPENAI_API_BASE" to ROUTER_BASE,
            "OPENAI_API_BASE_URL" to ROUTER_BASE,
            // Anthropic / Claude Code
            "ANTHROPIC_BASE_URL" to ROUTER_BASE,
            "ANTHROPIC_API_BASE" to ROUTER_BASE,
            // Generic
            "LLM_BASE_URL" to ROUTER_BASE,
            "ARTIER_ROUTER_URL" to ROUTER_BASE,
            "ARTIER_ROUTER_HOST" to ROUTER_HOST,
            "ARTIER_ROUTER_PORT" to ROUTER_PORT.toString(),
        ) + extra
    }

    fun applyTo(env: MutableMap<String, String>, extra: Map<String, String> = emptyMap()) {
        forProcess(extra).forEach { (k, v) -> env[k] = v }
    }
}
