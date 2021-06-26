package br.com.zup.edu

import br.com.zup.edu.integration.FinancialClient
import br.com.zup.edu.integration.SubmitForAnalysisRequest
import br.com.zup.edu.integration.SubmitForAnalysisResponse
import io.grpc.Channel
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import io.micronaut.grpc.annotation.GrpcChannel
import io.micronaut.grpc.server.GrpcServerChannel
import io.micronaut.http.HttpResponse
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.util.*
import javax.inject.Inject

@MicronautTest(transactional = false)
internal class CreateProposalEndpointTest(
    @Inject val repository: ProposalRepository,
    @Inject val grpcClient: PropostasGrpcServiceGrpc.PropostasGrpcServiceBlockingStub
) {

    @field:Inject
    lateinit var financialClient: FinancialClient

    /**
     * 1. happy path - ok
     * 2. proposta duplicada - ok
     * 3. dados invalidos - ok
     */

    @BeforeEach
    fun init() {
        repository.deleteAll()
    }

    @MockBean(FinancialClient::class)
    fun mockFinancialClient(): FinancialClient {
        val mockClient = Mockito.mock(FinancialClient::class.java)
        return mockClient
    }

    @Test
    fun `deve criar nova proposta com status ELIGIVEL`() {

        // cenario
        `when`(financialClient.submitForAnalysis(SubmitForAnalysisRequest(
            document = "63657520325",
            name = "Rafael Ponte",
            proposalId = UUID.randomUUID().toString()
        ))).thenReturn(HttpResponse.created(SubmitForAnalysisResponse(
            proposalId = UUID.randomUUID().toString(),
            status = "SEM_RESTRICAO"
        )))

        // ação
        val request = CreateProposalRequest.newBuilder()
            .setName("Rafael Ponte")
            .setDocument("63657520325")
            .setEmail("rafael.ponte@zup.com.br")
            .setAddress("Rua das Rosas, 375")
            .setSalary(30000.0)
            .build()

        val response = grpcClient.create(request)

        // validação
        with(response) {
            assertNotNull(id)
            assertNotNull(createdAt)
            assertTrue(repository.existsById(UUID.fromString(id)))
            assertEquals(ProposalStatus.ELIGIBLE, repository.findById(UUID.fromString(id)).get().status)
        }
    }

    @Test
    fun `deve criar nova proposta com status NAO ELIGIVEL`() {

        // cenario
        `when`(financialClient.submitForAnalysis(SubmitForAnalysisRequest(
            document = "63657520325",
            name = "Rafael Ponte",
            proposalId = UUID.randomUUID().toString()
        ))).thenReturn(HttpResponse.unprocessableEntity())

        // ação
        val request = CreateProposalRequest.newBuilder()
            .setName("Rafael Ponte")
            .setDocument("63657520325")
            .setEmail("rafael.ponte@zup.com.br")
            .setAddress("Rua das Rosas, 375")
            .setSalary(30000.0)
            .build()

        val response = grpcClient.create(request)

        // validação
        with(response) {
            assertNotNull(id)
            assertNotNull(createdAt)
            assertTrue(repository.existsById(UUID.fromString(id)))
            assertEquals(ProposalStatus.NOT_ELIGIBLE, repository.findById(UUID.fromString(id)).get().status)
        }
    }

    @Test
    fun `nao deve criar proposta quando proposta com mesmo documento ja existir`() {
        // cenario
        val existente = repository.save(
            Proposal(
                name = "Yuri",
                document = "63657520325",
                email = "yuri.matheus@zup.com.br",
                address = "Rua das Tabajaras",
                salary = BigDecimal("20000.0")
            )
        )

        // ação
        val request = CreateProposalRequest.newBuilder()
                                    .setName("Rafael Ponte")
                                    .setDocument(existente.document) // MESMO CPF
                                    .setEmail("rafael.ponte@zup.com.br")
                                    .setAddress("Rua das Rosas, 375")
                                    .setSalary(30000.0)
                                    .build()

        val error = assertThrows<StatusRuntimeException>() {
            grpcClient.create(request) // abriu uma nova thread
        }

        // validação
        with(error) {
            assertEquals(Status.ALREADY_EXISTS.code, this.status.code)
            assertEquals("proposal already exists", this.status.description)
        }
    }

    @Test
    fun `nao deve criar proposta quando dados de entrada forem invalidos`() {

        // ação
        val request = CreateProposalRequest.newBuilder().build() // invalido

        val error = assertThrows<StatusRuntimeException>() {
            grpcClient.create(request)
        }

        // validação
        with(error) {
            assertEquals(Status.INVALID_ARGUMENT.code, this.status.code)
            assertEquals("invalid parameters", this.status.description)
        }
    }

    @Factory
    class ClientsFactory {
        @Bean
        fun stub(@GrpcChannel(GrpcServerChannel.NAME) channel: Channel): PropostasGrpcServiceGrpc.PropostasGrpcServiceBlockingStub {
            return PropostasGrpcServiceGrpc.newBlockingStub(channel)
        }
    }

}