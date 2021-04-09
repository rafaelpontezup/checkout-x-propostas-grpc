package br.com.zup.edu.propostas

import br.com.zup.edu.shared.validations.CpfOrCnpj
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.validation.constraints.Email
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull
import javax.validation.constraints.PositiveOrZero

@Entity
class Proposal(
    @field:CpfOrCnpj
    @field:NotBlank
    @Column(nullable = false, unique = true)
    val document: String,

    @field:Email
    @field:NotBlank
    @Column(nullable = false)
    val email: String,

    @field:NotBlank
    @Column(nullable = false)
    val name: String,

    @field:NotBlank
    @Column(nullable = false)
    val address: String,

    @field:PositiveOrZero
    @field:NotNull
    @Column(nullable = false)
    val salary: BigDecimal,
    @Id
    val id: UUID
) {

//    @Id
//    @GeneratedValue
//    val id: UUID? = null

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()

    var status: ProposalStatus = ProposalStatus.NOT_ELIBLE

    fun updateStatus(status: ProposalStatus): Proposal {
        this.status = status
        return this
    }

}