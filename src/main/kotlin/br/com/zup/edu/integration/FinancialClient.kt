package br.com.zup.edu.integration

import br.com.zup.edu.ProposalStatus
import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Post
import io.micronaut.http.client.annotation.Client

@Client("http://localhost:9999/")
interface FinancialClient {

    /**
     * Parece que a partir da versao 2.5.x o Micronaut não lança mais exception quando o retorno é
     * diferente de 2xx e 404, e o tipo de retorno do método é HttpResponse!
     */
    @Post("/api/solicitacao")
    fun submitForAnalysis(@Body request: SubmitForAnalysisRequest): HttpResponse<SubmitForAnalysisResponse>

}

@Introspected
data class SubmitForAnalysisRequest(
    @field:JsonProperty("documento") val document: String,
    @field:JsonProperty("nome") val name: String,
    @field:JsonProperty("idProposta") val proposalId: String
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SubmitForAnalysisRequest

        if (document != other.document) return false
        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        var result = document.hashCode()
        result = 31 * result + name.hashCode()
        return result
    }
}

@Introspected
data class SubmitForAnalysisResponse(
    @field:JsonProperty("idProposta") val proposalId: String,
    @field:JsonProperty("resultadoSolicitacao") val status: String
) {
    fun toModel(): ProposalStatus {

        if (status.equals("COM_RESTRICAO")) {
            return ProposalStatus.NOT_ELIGIBLE
        }

        return ProposalStatus.ELIGIBLE
    }

}


