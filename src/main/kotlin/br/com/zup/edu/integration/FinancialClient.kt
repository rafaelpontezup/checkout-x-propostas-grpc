package br.com.zup.edu.integration

import br.com.zup.edu.ProposalStatus
import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Post
import io.micronaut.http.client.annotation.Client

@Client("http://localhost:9999")
interface FinancialClient {

    @Post("/api/solicitacao")
    fun submitForAnalysis(@Body request: SubmitForAnalysisRequest): SubmitForAnalysisResponse

}

@Introspected
data class SubmitForAnalysisRequest(
    @field:JsonProperty("documento")
    val document: String,
    @field:JsonProperty("nome")
    val name: String,
    @field:JsonProperty("idProposta")
    val proposalId: String, // eu NAO tenho controle da geração do ID
) {


}

@Introspected
data class SubmitForAnalysisResponse(
    @field:JsonProperty("idProposta")
    val proposalId: String,
    @field:JsonProperty("resultadoSolicitacao")
    val status: String
) {

    fun semRestricao(): Boolean {
        return "SEM_RESTRICAO".equals(this.status)
    }

    fun toModel(): ProposalStatus {
        if (semRestricao()) {
            return ProposalStatus.ELIGIBLE
        }

        return ProposalStatus.NOT_ELIGIBLE
    }

}


