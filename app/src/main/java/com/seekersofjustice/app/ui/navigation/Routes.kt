package com.seekersofjustice.app.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
object CaseListRoute

@Serializable
data class CaseDetailRoute(val caseId: Long)

@Serializable
data class RecordMemoRoute(val caseId: Long)

@Serializable
data class MemoDetailRoute(val memoId: Long)
