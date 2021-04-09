package br.com.zup.edu.propostas

import br.com.zup.edu.CreateProposalRequest
import br.com.zup.edu.PropostasGrpcServiceGrpc
import br.com.zup.edu.propostas.integration.FinancialClient
import br.com.zup.edu.propostas.integration.SubmitForAnalysisRequest
import br.com.zup.edu.propostas.integration.SubmitForAnalysisResponse
import com.google.rpc.BadRequest
import io.grpc.ManagedChannel
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.protobuf.StatusProto
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import io.micronaut.grpc.annotation.GrpcChannel
import io.micronaut.grpc.server.GrpcServerChannel
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsInAnyOrder
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.mockito.Mockito.*
import java.math.BigDecimal
import java.util.*
import javax.inject.Inject

/**
 * ROTEIRO do Checkout Estendido: testes com micronaut
 *
 * 1. contextualização: apresentação da aplicação
 *  1.1. explica e navega pelo codigo
 *  1.2. testa manualmente com BloomRPC
 * 2. introdução a testes, tipos de testes
 * 3. unidade X integração
 * 4. criando classe de teste com @MicronautTest
 * 5. executando teste (vazio) e entendendo o contexto do micronaut (banco, server grpc etc)
 *  5.1. configurando datasource de testes: H2
 * 6. escreve teste happy-path "deve criar nova proposta"
 *  6.1. 3 blocos de um teste
 *  6.2. fabricando o gRPC client com @Factory, ip/porta do channel etc
 *  6.3. injetando gRPC client
 *  6.4. validando efeitos colateriais: assertTrue(repository.existsById(UUID.fromString(id)))
 *  6.5. entendendo o problema de usar @Transactional em endpoints gRPC
 *  6.6. avaliando alternativas: auto-commit, Service e SynchronousTransactionManager
 * 7. escreve teste alternativo "nao deve criar proposta quando documento existente"
 *  6.1. validando somente contrato da API (status code e description)
 *  6.2. por que quebrou? entendendo @MicronautTest(transactional = false)
 *  6.3. (opcional) habilitando logs de transacoes
 * 8. escreve teste alternativo "nao deve criar propostas quando parametros de entrada forem invalidos"
 *  8.1. validando somente contrato da API (status code e description)
 *  8.2. validando violações da Bean Validation com Hamcrest .hasItem()
 *  8.3. validando violações da Bean Validation com Hamcrest .containsInAnyOrder()
 *  8.3. melhorando com Extension Functions: StatusRuntimeException.violations()
 * 9. evitando repetição de código com @BeforeEach
 * 10. duvidas?
 *
 * ROTEIRO do Checkout Estendido: mocking com micronaut
 *
 * 1. contextualiza projeto
 * 2. abre atividade e explica: https://github.com/zup-academy/nosso-cartao-documentacao/blob/master/proposta/015.consultando_dados_solicitante.md
 * 3. implementa a feature da forma mais simples
 *  3.1. a cada alteração roda bateria de testes
 *  3.2. adiciona nova coluna ProposalStatus e roda os testes e vê passar
 *  3.3. adiciona assert para validar status ELIGIBLE
 *  3.4. implementa integração com FinancialClient (mais simples possivel, pra possibilitar refactoring futuro)
 *  3.5. cria request e response como class em vez de data class
 *  3.6. roda bateria de testes e vê quebrar (motivo: tenta acessar servidor real)
 * 4. mocking
 *  4.0. explica a motivação de mockar (aqui os alunos já conhecem o Mockito)
 *  4.1. implementa mock diretamente no método
 *  4.2. roda bateria de testes e vê quebrar (motivo: nao mockamos no contexto do micronaut)
 *  4.3. explica que mockamos o client dentro do método mas não no contexto do micronaut
 *  4.4. introduz ao @MockBean
 *  4.5. roda os testes e vê quebrar (motivo: equals and hashCode)
 * 5. equals and hashCode
 *  5.0. analogia do dublê (pula da frente de um gol mas veio um palio); faz exemplo com string e depois com objeto;
 *  5.1. gera equals e hashCode para todos os atributos
 *  5.2. roda os testes e vê quebrar (motivo: proposalId eh um UUID)
 *  5.3. gera equals e hashCode para atributos document e name
 *  5.4. roda os testes e vê passar
 * 6. novo cenário de testes
 *  6.1. melhora nome do teste happy-path para "deve criar nova proposta com status ELEGIVEL"
 *  6.2. implementa teste "deve criar nova proposta com status NAO ELEGIVEL"
 * 7. refatora código
 *  7.1. move logica pra dentro do response: val status = response.toModel()
 *  7.2. roda os testes e vê passar
 *  7.3. (opcional) melhora logica pra retornar NOT_ELIGIBLE se status for diferente de "SEM_RESTRICAO"
 *  7.4. roda os testes e vê passar
 * 8. refatoração: que tal usar data classes?
 *  8.1. explica que eh uma boa prática
 *  8.2. basta mudar pra data class, né? (lembra de remover equals e hashCode)
 *  8.3. roda os testes e vê quebrar
 *  8.4. explica como data class funciona
 *  8.5. sugere usar mas ter com cuidado com atributos gerados (como UUID, LocalDateTime etc)
 *  8.6. refatora
 *  8.7. roda os testes e vê passar
 * 9. (opcional) sugestão de melhorias?
 *  9.1. explica problema do proposalId ser UUID gerado pelo Hibernate
 *  9.2. explica que precisamos ter controle dessa geração
 *  9.3. cria ProposalIdGenerator como @Singleton
 *  9.4. desliga @GeneratedValue do Hibernate e gera via ProposalIdGenerator no request.toModel(generator)
 *  9.5. mocka ProposalIdGenerator pra retornar UUID fixo (deterministico)
 *  9.6. roda os testes e vê quebrar (ProposalIdGenerator precisa ser open)
 *  9.7. refatora ProposalIdGenerator pra ser open
 *  9.8. roda os testes e vê passar
 * 10. (super opcional) usando API do Mockito
 *  10.1. Mockito.any() - explica que pode ajudar a identificar problemas do equals e hashCode
 *  10.2. Mockito.argumentCaptor() - avançado
 */
@MicronautTest(transactional = false)
internal class CreateProposalEndpointTest(
    val repository: ProposalRespository,
    val grpcClient: PropostasGrpcServiceGrpc.PropostasGrpcServiceBlockingStub,
) {

    @Inject
    lateinit var financialClient: FinancialClient

    @Inject
    lateinit var generator: ProposalIdGenerator

    val PROPOSAL_ID = UUID.randomUUID()

    @BeforeEach
    fun setup() {
        `when`(generator.generate()).thenReturn(PROPOSAL_ID)
        repository.deleteAll()
    }

    @Test
    fun `deve criar nova proposta com status ELEGIVEL`() {
        // cenario
        `when`(financialClient.submitForAnalysis(SubmitForAnalysisRequest(
            document = "63657520325",
            name = "Rafael Ponte",
            proposalId = PROPOSAL_ID.toString()
        )))
            .thenReturn(SubmitForAnalysisResponse(
                proposalId = PROPOSAL_ID.toString(),
                status = "SEM_RESTRICAO"
            ))

        // ação
        val response = grpcClient.create(CreateProposalRequest.newBuilder()
                                                .setDocument("63657520325")
                                                .setName("Rafael Ponte")
                                                .setEmail("rafael.ponte@zup.com.br")
                                                .setAddress("Rua das Rosa, 375")
                                                .setSalary(2020.99)
                                                .build())

        // validação
        with(response) {
            assertNotNull(id)
            assertNotNull(createdAt)
            assertTrue(repository.existsById(UUID.fromString(id)))
            assertEquals(ProposalStatus.ELIBLE, repository.findById(UUID.fromString(id)).get().status)
        }
    }

    @Test
    fun `deve criar nova proposta com status NAO ELEGIVEL`() {
        // cenario
        `when`(financialClient.submitForAnalysis(SubmitForAnalysisRequest(
            document = "63657520325",
            name = "Rafael Ponte",
            proposalId = UUID.randomUUID().toString() // TODO: corrigir para os testes
        )))
            .thenReturn(SubmitForAnalysisResponse(
                proposalId = UUID.randomUUID().toString(), // TODO: corrigir para os testes
                status = "COM_RESTRICAO"
            ))

        // ação
        val response = grpcClient.create(CreateProposalRequest.newBuilder()
            .setDocument("63657520325")
            .setName("Rafael Ponte")
            .setEmail("rafael.ponte@zup.com.br")
            .setAddress("Rua das Rosa, 375")
            .setSalary(2020.99)
            .build())

        // validação
        with(response) {
            assertNotNull(id)
            assertNotNull(createdAt)
            assertTrue(repository.existsById(UUID.fromString(id)))
            assertEquals(ProposalStatus.NOT_ELIBLE, repository.findById(UUID.fromString(id)).get().status)
        }
    }

    @Test
    fun `nao deve criar nova proposta quando existir proposta para o mesmo documento`() {
        // cenário
        val existing  = repository.save(Proposal(
            document = "63657520325",
            email = "rafael.ponte@zup.com.br",
            name = "Rafael Ponte",
            address = "Rua das Rosas, 375",
            salary = BigDecimal("2020.99"),
            id = UUID.randomUUID()
        ))

        // ação
        val thrown = assertThrows<StatusRuntimeException> {
            grpcClient.create(CreateProposalRequest.newBuilder()
                                            .setDocument(existing.document)
                                            .setName("Outro nome")
                                            .setEmail("rafael.ponte@zup.com.br")
                                            .setAddress("Outro endereço qualquer")
                                            .setSalary(1000.0)
                                            .build())
        }

        // validação
        with(thrown) {
            assertEquals(Status.ALREADY_EXISTS.code, status.code)
            assertEquals("proposal already exists", status.description)
        }
    }

    @Test
    fun `nao deve criar proposta quando dados invalidos`() {
        // cenário e ação
        val thrown = assertThrows<StatusRuntimeException> {
            grpcClient.create(CreateProposalRequest.newBuilder()
                                                .setSalary(-0.1)
                                                .build())
        }

        // validação
        with(thrown) {
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertEquals("request with invalid parameters", status.description)
            assertThat(violations(), containsInAnyOrder(
                Pair("name", "must not be blank"),
                Pair("document", "must not be blank"),
                Pair("document", "document is not a valid CPF or CNPJ"),
                Pair("email", "must not be blank"),
                Pair("address", "must not be blank"),
                Pair("salary", "must be greater than or equal to 0"),
            ))
        }
    }

    @MockBean(FinancialClient::class)
    fun mockFinancialClient(): FinancialClient {
        return mock(FinancialClient::class.java)
    }

    @MockBean(ProposalIdGenerator::class)
    fun mockProposalIdGenerator(): ProposalIdGenerator {
        return mock(ProposalIdGenerator::class.java)
    }

    @Factory
    class ClientsFactory {
        @Bean
        fun blockingStub(@GrpcChannel(GrpcServerChannel.NAME) channel: ManagedChannel): PropostasGrpcServiceGrpc.PropostasGrpcServiceBlockingStub {
            return PropostasGrpcServiceGrpc.newBlockingStub(channel)
        }
    }

    /**
     * TODO: mover para arquivo separado para ser reutilizado entre vários testes
     */
    private fun StatusRuntimeException.violations(): List<Pair<String, String>>? {
        val details = StatusProto.fromThrowable(this)
            ?.detailsList?.get(0)!!
            .unpack(BadRequest::class.java)

        return details.fieldViolationsList
            .map { it.field to it.description }
    }
}