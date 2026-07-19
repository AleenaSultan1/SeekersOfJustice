package com.seekersofjustice.app.domain.model

/**
 * Relation of the person to the crime. Extensible: adding a value only requires
 * a new entry here plus a display-string resource (stored in Room by name).
 */
enum class RelationToCrime {
    VICTIM, WITNESS, RELATIVE, OTHER
}

enum class Sex {
    MALE, FEMALE, OTHER
}

data class Case(
    val id: Long = 0,
    val firNumber: String,
    val fullName: String,
    val age: Int,
    val sex: Sex,
    val relation: RelationToCrime,
    val createdAt: Long,
)
