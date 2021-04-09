package br.com.zup.edu.propostas

import java.util.*
import javax.inject.Singleton

@Singleton
open class ProposalIdGenerator {

    open fun generate(): UUID {
        return UUID.randomUUID()
    }

}
