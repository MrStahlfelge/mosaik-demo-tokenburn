package org.ergoplatform.mosaik.example.db

import javax.persistence.*

@Entity
@Table(
    indexes = [
        Index(name = "mosaikUuid", columnList = "mosaikUuid"),
    ]
)
class MosaikUser(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: Long, // DB ID
    val mosaikUuid: String,
    val p2PKAddress: String?,
    val tokensList: String?,
)