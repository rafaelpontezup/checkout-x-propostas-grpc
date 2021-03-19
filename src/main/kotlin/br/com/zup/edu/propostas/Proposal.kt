package br.com.zup.edu.propostas

import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id

@Entity
class Proposal(
    val document: String,
    val email: String,
    val name: String,
    val address: String,
    val salary: BigDecimal
) {

    @Id
    @GeneratedValue
    val id: UUID? = null

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
}