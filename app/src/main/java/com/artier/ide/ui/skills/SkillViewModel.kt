package com.artier.ide.ui.skills

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artier.ide.data.model.SkillState
import com.artier.ide.data.repository.SkillRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SkillViewModel @Inject constructor(
    private val skillRepository: SkillRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SkillState())
    val state: StateFlow<SkillState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            skillRepository.skills.collect { skills ->
                _state.update { it.copy(skills = skills, isLoading = false) }
            }
        }
        viewModelScope.launch {
            skillRepository.selected.collect { detail ->
                _state.update { it.copy(selectedSkill = detail) }
            }
        }
        viewModelScope.launch {
            skillRepository.events.collect { event ->
                when (event) {
                    "installed" -> _state.update {
                        it.copy(isInstalling = false, installPathOrUrl = "", error = null)
                    }
                    "uninstalled", "updated" -> _state.update { it.copy(error = null) }
                }
            }
        }
        refresh()
    }

    fun refresh(projectRoot: String? = null) {
        _state.update { it.copy(isLoading = true, error = null) }
        skillRepository.scan(projectRoot)
    }

    fun updateQuery(query: String) {
        _state.update { it.copy(query = query) }
    }

    fun selectSkill(name: String) {
        skillRepository.get(name)
    }

    fun clearSelection() {
        _state.update { it.copy(selectedSkill = null) }
    }

    fun setEnabled(name: String, enabled: Boolean) {
        skillRepository.setEnabled(name, enabled)
        if (enabled) {
            skillRepository.get(name)
            skillRepository.refreshAgentContext()
        } else {
            skillRepository.refreshAgentContext()
        }
    }

    fun updateInstallInput(value: String) {
        _state.update { it.copy(installPathOrUrl = value) }
    }

    fun install() {
        val input = _state.value.installPathOrUrl.trim()
        if (input.isEmpty()) {
            _state.update { it.copy(error = "Enter a folder path or SKILL.md URL") }
            return
        }
        _state.update { it.copy(isInstalling = true, error = null) }
        if (input.startsWith("http://") || input.startsWith("https://")) {
            skillRepository.installFromUrl(input)
        } else {
            skillRepository.installFromPath(input)
        }
    }

    fun uninstall(name: String) {
        skillRepository.uninstall(name)
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
