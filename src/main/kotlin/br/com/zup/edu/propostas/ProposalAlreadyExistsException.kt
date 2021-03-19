package br.com.zup.edu.propostas

import java.lang.RuntimeException

class ProposalAlreadyExistsException(message: String?) : RuntimeException(message) {
}