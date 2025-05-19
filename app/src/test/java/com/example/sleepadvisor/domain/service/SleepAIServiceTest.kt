package com.example.sleepadvisor.domain.service

import com.example.sleepadvisor.domain.model.SleepSession
import com.example.sleepadvisor.domain.model.SleepStage
import com.example.sleepadvisor.domain.model.SleepStageType
import com.google.gson.Gson
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class)
class SleepAIServiceTest {
    
    private lateinit var mockWebServer: MockWebServer
    private lateinit var service: SleepAIService
    private lateinit var okHttpClient: OkHttpClient
    private val gson = Gson()
    
    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        
        okHttpClient = OkHttpClient.Builder()
            .connectTimeout(1, TimeUnit.SECONDS)
            .readTimeout(1, TimeUnit.SECONDS)
            .build()
            
        service = SleepAIService(okHttpClient, gson)
        
        // Configura a URL base para o mock server
        System.setProperty("SLEEP_AI_API_URL", mockWebServer.url("/").toString())
    }
    
    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }
    
    @Test
    fun `deve processar corretamente os dados de sono e gerar dicas`() = runTest {
        // Dado: Uma lista de sessões de sono de teste
        val testSessions = listOf(
            createTestSleepSession(
                startTime = ZonedDateTime.now().minusDays(1),
                durationHours = 7,
                lightHours = 3.5,
                deepHours = 1.5,
                remHours = 2.0,
                wakeCount = 2
            ),
            createTestSleepSession(
                startTime = ZonedDateTime.now().minusDays(2),
                durationHours = 6,
                lightHours = 3.0,
                deepHours = 1.0,
                remHours = 2.0,
                wakeCount = 3
            )
        )
        
        // Configura a resposta simulada da API
        val mockResponse = """
            {
                "tips": [
                    "Mantenha um horário regular de sono",
                    "Evite cafeína nas 6 horas antes de dormir"
                ],
                "warnings": [
                    "Muitos despertares durante a noite"
                ],
                "positiveReinforcement": "Bom trabalho mantendo uma boa quantidade de sono REM!"
            }
        """.trimIndent()
        
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""
                    {
                        "choices": [
                            {
                                "message": {
                                    "content": "$mockResponse"
                                }
                            }
                        ]
                    }
                """.trimIndent())
        )
        
        // Quando: Chamamos o serviço para gerar conselhos
        val result = service.generateSleepAdvice(testSessions)
        
        // Então: Verificamos se o resultado foi processado corretamente
        assertEquals(2, result.tips.size)
        assertEquals(1, result.warnings.size)
        assertNotNull(result.positiveReinforcement)
        
        // Verifica se a requisição foi feita corretamente
        val request = mockWebServer.takeRequest()
        assertTrue(request.path?.contains("/v1/chat/completions") == true)
        assertTrue(request.body.toString().contains("totalSleep"))
        assertTrue(request.body.toString().contains("deepSleepPercentage"))
        assertTrue(request.body.toString().contains("remSleepPercentage"))
    }
    
    @Test
    fun `deve lidar com erros de API`() = runTest {
        // Dado: Uma lista de sessões de sono de teste
        val testSessions = listOf(createTestSleepSession())
        
        // Configura a resposta de erro da API
        mockWebServer.enqueue(
            MockResponse().setResponseCode(500)
        )
        
        // Quando: Chamamos o serviço para gerar conselhos
        val result = service.generateSleepAdvice(testSessions)
        
        // Então: Deve retornar uma mensagem de erro genérica
        assertTrue(result.tips.isNotEmpty())
        assertTrue(result.tips[0].contains("não foi possível"))
    }
    
    @Test
    fun `deve lidar com respostas inválidas da API`() = runTest {
        // Dado: Uma lista de sessões de sono de teste
        val testSessions = listOf(createTestSleepSession())
        
        // Configura uma resposta inválida da API
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("{invalid json")
        )
        
        // Quando: Chamamos o serviço para gerar conselhos
        val result = service.generateSleepAdvice(testSessions)
        
        // Então: Deve retornar uma mensagem de erro genérica
        assertTrue(result.tips.isNotEmpty())
        assertTrue(result.tips[0].contains("não foi possível"))
    }
    
    private fun createTestSleepSession(
        startTime: ZonedDateTime = ZonedDateTime.now(),
        durationHours: Long = 8,
        lightHours: Double = 4.0,
        deepHours: Double = 2.0,
        remHours: Double = 2.0,
        wakeCount: Int = 1
    ): SleepSession {
        val stages = mutableListOf<SleepStage>()
        
        if (lightHours > 0) {
            stages.add(SleepStage(SleepStageType.LIGHT, Duration.ofMinutes((lightHours * 60).toLong())))
        }
        if (deepHours > 0) {
            stages.add(SleepStage(SleepStageType.DEEP, Duration.ofMinutes((deepHours * 60).toLong())))
        }
        if (remHours > 0) {
            stages.add(SleepStage(SleepStageType.REM, Duration.ofMinutes((remHours * 60).toLong())))
        }
        
        return SleepSession(
            id = "test-${System.currentTimeMillis()}",
            startTime = startTime,
            endTime = startTime.plusHours(durationHours),
            stages = stages,
            isManualEntry = true,
            notes = "Sessão de teste",
            wakeDuringNightCount = wakeCount
        )
    }
}
