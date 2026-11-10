package com.example.data.model

enum class InjectionSite(
    val idName: String,
    val displayName: String,
    val shortCode: String,
    val isSubQFriendly: Boolean,
    val routeLabel: String
) {
    LEFT_THIGH("LEFT_THIGH", "Left Thigh", "L-Thigh", true, "SubQ / IM"),
    RIGHT_THIGH("RIGHT_THIGH", "Right Thigh", "R-Thigh", true, "SubQ / IM"),
    LEFT_GLUTE("LEFT_GLUTE", "Left Glute", "L-Glute", false, "IM"),
    RIGHT_GLUTE("RIGHT_GLUTE", "Right Glute", "R-Glute", false, "IM"),
    LEFT_DELTOID("LEFT_DELTOID", "Left Deltoid", "L-Delt", false, "IM"),
    RIGHT_DELTOID("RIGHT_DELTOID", "Right Deltoid", "R-Delt", false, "IM"),
    ABDOMEN("ABDOMEN", "Abdomen", "Abdomen", true, "SubQ");

    companion object {
        fun fromId(id: String): InjectionSite =
            entries.find { it.idName.equals(id, ignoreCase = true) } ?: LEFT_THIGH
    }
}
