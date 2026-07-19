package com.seekersofjustice.app.data.db

import androidx.room.TypeConverter

class Converters {
    companion object {
        // Unit separator: cannot occur in transcribed text, so joining is lossless.
        private const val SEPARATOR = "\u001F"
    }

    @TypeConverter
    fun fromStringList(value: List<String>): String = value.joinToString(SEPARATOR)

    @TypeConverter
    fun toStringList(value: String): List<String> =
        if (value.isEmpty()) emptyList() else value.split(SEPARATOR)
}
