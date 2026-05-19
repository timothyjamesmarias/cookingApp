package com.timothymarias.cookingapp.shared.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun <T> ListScaffold(
    items: List<T>,
    isLoading: Boolean,
    error: String?,
    emptyMessage: String,
    itemContent: LazyListScope.(List<T>) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            LoadingIndicator()
            return@Box
        }

        if (error != null) {
            ErrorState(message = error)
            return@Box
        }

        if (items.isEmpty()) {
            EmptyState(message = emptyMessage)
            return@Box
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            itemContent(items)
        }
    }
}
