package com.seekersofjustice.app.domain.model

/** A signed-in user of the app (a police officer). No network account — local only. */
data class Officer(
    val id: Long = 0,
    val name: String,
    val createdAt: Long,
)
