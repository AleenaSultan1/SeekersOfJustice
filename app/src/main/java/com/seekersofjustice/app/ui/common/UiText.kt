package com.seekersofjustice.app.ui.common

import androidx.annotation.StringRes
import com.seekersofjustice.app.R
import com.seekersofjustice.app.domain.model.MemoLanguage
import com.seekersofjustice.app.domain.model.MemoStatus
import com.seekersofjustice.app.domain.model.RelationToCrime
import com.seekersofjustice.app.domain.model.Sex
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@StringRes
fun RelationToCrime.labelRes(): Int = when (this) {
    RelationToCrime.VICTIM -> R.string.relation_victim
    RelationToCrime.WITNESS -> R.string.relation_witness
    RelationToCrime.RELATIVE -> R.string.relation_relative
    RelationToCrime.OTHER -> R.string.relation_other
}

@StringRes
fun Sex.labelRes(): Int = when (this) {
    Sex.MALE -> R.string.sex_male
    Sex.FEMALE -> R.string.sex_female
    Sex.OTHER -> R.string.sex_other
}

@StringRes
fun MemoLanguage.labelRes(): Int = when (this) {
    MemoLanguage.URDU -> R.string.language_urdu
    MemoLanguage.ENGLISH -> R.string.language_english
    MemoLanguage.AUTO -> R.string.language_auto
}

@StringRes
fun MemoStatus.labelRes(): Int = when (this) {
    MemoStatus.PENDING -> R.string.status_pending
    MemoStatus.PROCESSING -> R.string.status_processing
    MemoStatus.DONE -> R.string.status_done
    MemoStatus.FAILED -> R.string.status_failed
}

fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    return String.format(Locale.US, "%d:%02d", totalSeconds / 60, totalSeconds % 60)
}

fun formatDate(epochMs: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(epochMs))
