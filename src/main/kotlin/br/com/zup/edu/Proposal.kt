package br.com.zup.edu

import br.com.zup.edu.shared.validations.CpfOrCnpj
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*
import javax.persistence.*
import javax.persistence.EnumType.STRING
import javax.validation.constraints.Email
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull
import javax.validation.constraints.PositiveOrZero

@Entity
class Proposal(
    @CpfOrCnpj
    @field:NotBlank
    @Column(nullable = false, unique = true)
    val document: String,

    @field:NotBlank
    @Column(nullable = false)
    val name: String,

    @field:Email
    @field:NotBlank
    @Column(nullable = false)
    val email: String,

    @field:NotBlank
    @Column(nullable = false)
    val address: String,

    @field:PositiveOrZero
    @field:NotNull
    @Column(nullable = false)
    val salary: BigDecimal,
    @Id
    val id: UUID,
) {



    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()

    @Enumerated(STRING)
    var status: ProposalStatus = ProposalStatus.NOT_ELIGIBLE
    private set

    @Column(nullable = true)
    var updatedAt: LocalDateTime? = null

    fun updateStatus(status: ProposalStatus): Proposal {
        this.status = status
        this.updatedAt = LocalDateTime.now()
        return this
    }

}