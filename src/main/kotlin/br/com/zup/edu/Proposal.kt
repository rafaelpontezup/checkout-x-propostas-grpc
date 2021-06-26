package br.com.zup.edu

import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*
import javax.persistence.*
import javax.validation.constraints.Email
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull
import javax.validation.constraints.PositiveOrZero

@Entity
class Proposal(
    @field:NotBlank
    @Column(nullable = false)
    val name: String,

    @CpfOrCnpj
    @field:NotBlank
    @Column(nullable = false, unique = true)
    val document: String,

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
) {

    fun updateStatus(status: ProposalStatus): Proposal {
        this.status = status
        this.updatedAt = LocalDateTime.now()
        return this
    }

    @Id
    @GeneratedValue
    val id: UUID? = null

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()

    @Enumerated(EnumType.STRING)
    var status: ProposalStatus = ProposalStatus.NOT_ELIGIBLE
        private set

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
        private set

}
