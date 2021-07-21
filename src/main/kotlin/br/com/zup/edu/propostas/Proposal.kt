package br.com.zup.edu.propostas

import br.com.zup.edu.shared.validations.CpfOrCnpj
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*
import javax.persistence.*
import javax.validation.constraints.*

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
    val salary: BigDecimal
) {

    @Id
    @GeneratedValue
    val id: UUID? = null // hibernate vai gerar um UUID aleatorio

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()

    @Enumerated(EnumType.STRING)
    var status: ProposalStatus = ProposalStatus.NOT_ELIGIBLE
        private set

    @Column(nullable = true)
    var updatedAt: LocalDateTime? = null

    fun updateStatus(newStatus: ProposalStatus): Proposal {
        this.status = newStatus
        this.updatedAt = LocalDateTime.now()
        return this
    }

}