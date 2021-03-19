package br.com.zup.edu.propostas

import io.micronaut.data.annotation.Repository
import io.micronaut.data.jpa.repository.JpaRepository
import java.util.*

@Repository
interface ProposalRespository: JpaRepository<Proposal, UUID> {

}