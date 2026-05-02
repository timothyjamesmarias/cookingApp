package com.timothymarias.cookingapp.shared.presentation.unit

import com.timothymarias.cookingapp.domain.model.Unit

/**
 * UI state for Unit management.
 * Units are typically loaded once at app start and cached.
 */
data class UnitState(
    val items: List<Unit> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)
