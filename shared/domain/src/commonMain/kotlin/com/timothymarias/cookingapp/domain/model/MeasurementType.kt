package com.timothymarias.cookingapp.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class MeasurementType {
    WEIGHT,
    VOLUME,
    COUNT
}
