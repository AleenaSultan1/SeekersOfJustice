package com.seekersofjustice.app.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.seekersofjustice.app.domain.model.Case
import com.seekersofjustice.app.domain.model.RelationToCrime
import com.seekersofjustice.app.domain.model.Sex

@Entity(
    tableName = "cases",
    indices = [Index(value = ["firNumber"], unique = true)],
)
data class CaseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val firNumber: String,
    val fullName: String,
    val age: Int,
    val sex: String,
    val relation: String,
    val createdAt: Long,
)

fun CaseEntity.toDomain() = Case(
    id = id,
    firNumber = firNumber,
    fullName = fullName,
    age = age,
    sex = Sex.valueOf(sex),
    relation = RelationToCrime.valueOf(relation),
    createdAt = createdAt,
)

fun Case.toEntity() = CaseEntity(
    id = id,
    firNumber = firNumber,
    fullName = fullName,
    age = age,
    sex = sex.name,
    relation = relation.name,
    createdAt = createdAt,
)
