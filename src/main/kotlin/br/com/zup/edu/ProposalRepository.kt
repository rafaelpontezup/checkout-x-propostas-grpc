package br.com.zup.edu

import io.micronaut.data.annotation.Repository
import io.micronaut.data.jpa.repository.JpaRepository
import java.util.*
import javax.transaction.Transactional

@Repository
interface ProposalRepository : JpaRepository<Proposal, UUID> {

    fun existsByDocument(document: String?): Boolean
}