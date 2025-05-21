package com.example.sleepadvisor.ai

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.sleepadvisor.domain.model.SleepSession
import com.example.sleepadvisor.domain.model.SleepStage
import com.example.sleepadvisor.domain.model.SleepStageType
import com.example.sleepadvisor.domain.service.SleepAIService
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class AIServiceEdgeCasesTest {

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
    fun testHighEfficiencySleep() = runBlocking {
        // DADO: Dados de sono com alta eficiência (95-100%)
        val sessions = listOf(
            createSleepSession(
                deepSleepPercentage = 30.0,  // Acima do ideal de 25%
                remSleepPercentage = 25.0,   // Acima do ideal de 20%
                lightSleepPercentage = 45.0,
                wakeDuringNightCount = 0     // Sem despertares
            )
        )
        
        // Configura a resposta da API
        val mockResponse = """
            {
                "choices": [
                    {
                        "message": {
                            "content": "{ \"tips\": [\"Continue mantendo seu padrão de sono\", \"Pequenos ajustes na rotina podem melhorar ainda mais\"], \"warnings\": [], \"positiveReinforcement\": \"Excelente qualidade de sono! Você está dormindo de forma muito eficiente.\" }"
                        }
                    }
                ]
            }
        """.trimIndent()
        
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(mockResponse))
        
        // QUANDO: Geramos conselhos
        val advice = service.generateSleepAdvice(sessions)
        
        // ENTÃO: Deve haver elogios e sem avisos
        assertTrue(advice.tips.isNotEmpty())
        assertTrue(advice.warnings.isEmpty())
        assertTrue(advice.positiveReinforcement?.contains("Excelente") == true)
        
        // Verifica se os dados enviados à API estão corretos
        val request = mockWebServer.takeRequest()
        assertTrue(request.body.toString().contains("\"deepSleepPercentage\":30.0"))
        assertTrue(request.body.toString().contains("\"remSleepPercentage\":25.0"))
        assertTrue(request.body.toString().contains("\"wakeCount\":0"))
    }

    @Test
    fun testLowEfficiencySleep() = runBlocking {
        // DADO: Dados de sono com baixa eficiência (abaixo de 60%)
        val sessions = listOf(
            createSleepSession(
                deepSleepPercentage = 10.0,  // Muito abaixo do ideal
                remSleepPercentage = 10.0,   // Muito abaixo do ideal
                lightSleepPercentage = 80.0, // Muito sono leve
                wakeDuringNightCount = 5     // Muitos despertares
            )
        )
        
        // Configura a resposta da API
        val mockResponse = """
            {
                "choices": [
                    {
                        "message": {
                            "content": "{ \"tips\": [\"Estabeleça um horário regular para dormir\", \"Evite cafeína e álcool antes de dormir\", \"Pratique técnicas de relaxamento\"], \"warnings\": [\"Sono fragmentado com muitos despertares\", \"Baixo percentual de sono profundo\", \"Baixo percentual de sono REM\"], \"positiveReinforcement\": null }"
                        }
                    }
                ]
            }
        """.trimIndent()
        
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(mockResponse))
        
        // QUANDO: Geramos conselhos
        val advice = service.generateSleepAdvice(sessions)
        
        // ENTÃO: Deve haver avisos e sem elogios
        assertTrue(advice.tips.size >= 3) // Mais dicas para melhorar
        assertTrue(advice.warnings.isNotEmpty())
        assertNull(advice.positiveReinforcement)
        
        // Verifica se os dados enviados à API estão corretos
        val request = mockWebServer.takeRequest()
        assertTrue(request.body.toString().contains("\"deepSleepPercentage\":10.0"))
        assertTrue(request.body.toString().contains("\"remSleepPercentage\":10.0"))
        assertTrue(request.body.toString().contains("\"wakeCount\":5"))
    }

    @Test
    fun testIrregularSleepPattern() = runBlocking {
        // DADO: Dados de sono com padrão irregular (horários diferentes, durações diferentes)
        val today = ZonedDateTime.now()
        val sessions = listOf(
            createSleepSession(
                startTime = today.minusDays(6).minusHours(8), // 22h às 6h
                endTime = today.minusDays(6),
                deepSleepPercentage = 20.0,
                remSleepPercentage = 20.0,
                lightSleepPercentage = 60.0,
                wakeDuringNightCount = 1
            ),
            createSleepSession(
                startTime = today.minusDays(5).minusHours(10), // 20h às 9h (tarde demais)
                endTime = today.minusDays(5).plusHours(3),
                deepSleepPercentage = 25.0,
                remSleepPercentage = 20.0,
                lightSleepPercentage = 55.0,
                wakeDuringNightCount = 0
            ),
            createSleepSession(
                startTime = today.minusDays(4).minusHours(5), // 1h às 6h (muito pouco)
                endTime = today.minusDays(4),
                deepSleepPercentage = 10.0,
                remSleepPercentage = 10.0,
                lightSleepPercentage = 80.0,
                wakeDuringNightCount = 2
            ),
            createSleepSession(
                startTime = today.minusDays(2).minusHours(7), // 23h às 7h
                endTime = today.minusDays(2).plusHours(1),
                deepSleepPercentage = 20.0,
                remSleepPercentage = 15.0,
                lightSleepPercentage = 65.0,
                wakeDuringNightCount = 1
            )
        )
        
        // Configura a resposta da API
        val mockResponse = """
            {
                "choices": [
                    {
                        "message": {
                            "content": "{ \"tips\": [\"Estabeleça uma rotina de sono regular\", \"Tente dormir e acordar nos mesmos horários todos os dias\", \"Evite grandes variações na duração do sono\"], \"warnings\": [\"Padrão de sono muito irregular\", \"Grandes variações na duração do sono\"], \"positiveReinforcement\": null }"
                        }
                    }
                ]
            }
        """.trimIndent()
        
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(mockResponse))
        
        // QUANDO: Geramos conselhos
        val advice = service.generateSleepAdvice(sessions)
        
        // ENTÃO: Deve mencionar a irregularidade
        assertTrue(advice.tips.any { it.contains("regular") || it.contains("rotina") })
        assertTrue(advice.warnings.any { it.contains("irregular") })
        
        // Verificar se os dados corretos foram enviados
        val request = mockWebServer.takeRequest()
        assertTrue(request.body.toString().contains("Dados:"))
        assertTrue(request.body.toString().contains("totalSleep"))
    }

    @Test
    fun testMissingData() = runBlocking {
        // DADO: Dados de sono parciais/incompletos
        val sessions = listOf(
            createSleepSession(
                deepSleepPercentage = 0.0,  // Dados faltantes/zerados
                remSleepPercentage = 0.0,   // Dados faltantes/zerados
                lightSleepPercentage = 100.0,
                wakeDuringNightCount = 0
            )
        )
        
        // Configura a resposta da API
        val mockResponse = """
            {
                "choices": [
                    {
                        "message": {
                            "content": "{ \"tips\": [\"Use um dispositivo capaz de detectar estágios do sono\", \"Considere um rastreador de sono mais preciso\"], \"warnings\": [\"Dados incompletos ou imprecisos\"], \"positiveReinforcement\": \"Bom trabalho registrando seu sono regularmente\" }"
                        }
                    }
                ]
            }
        """.trimIndent()
        
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(mockResponse))
        
        // QUANDO: Geramos conselhos
        val advice = service.generateSleepAdvice(sessions)
        
        // ENTÃO: Deve mencionar dados incompletos
        assertTrue(advice.warnings.any { it.contains("incompletos") || it.contains("imprecisos") })
        
        // Verificar se os dados corretos foram enviados
        val request = mockWebServer.takeRequest()
        assertTrue(request.body.toString().contains("\"deepSleepPercentage\":0.0"))
        assertTrue(request.body.toString().contains("\"remSleepPercentage\":0.0"))
    }

    @Test
    fun testNoDataAvailable() = runBlocking {
        // DADO: Nenhum dado de sono
        val sessions = emptyList<SleepSession>()
        
        // Configura a resposta da API
        val mockResponse = """
            {
                "choices": [
                    {
                        "message": {
                            "content": "{ \"tips\": [\"Comece a registrar seu sono regularmente\", \"Use um dispositivo de rastreamento de sono para obter dados mais precisos\"], \"warnings\": [\"Sem dados de sono disponíveis para análise\"], \"positiveReinforcement\": null }"
                        }
                    }
                ]
            }
        """.trimIndent()
        
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(mockResponse))
        
        // QUANDO: Geramos conselhos
        val advice = service.generateSleepAdvice(sessions)
        
        // ENTÃO: Deve mencionar falta de dados
        assertTrue(advice.warnings.any { it.contains("Sem dados") })
        
        // Verificar se o corpo da requisição contém um array vazio
        val request = mockWebServer.takeRequest()
        assertTrue(request.body.toString().contains("[]"))
    }

    @Test
    fun testConsistentMessages() = runBlocking {
        // DADO: Dois conjuntos de dados similares
        val sessions1 = listOf(
            createSleepSession(
                deepSleepPercentage = 20.0,
                remSleepPercentage = 20.0,
                lightSleepPercentage = 60.0,
                wakeDuringNightCount = 1
            )
        )
        
        val sessions2 = listOf(
            createSleepSession(
                deepSleepPercentage = 21.0,  // Ligeiramente diferente
                remSleepPercentage = 19.0,   // Ligeiramente diferente
                lightSleepPercentage = 60.0,
                wakeDuringNightCount = 1
            )
        )
        
        // Configura as respostas da API (similares mas não idênticas)
        val mockResponse1 = """
            {
                "choices": [
                    {
                        "message": {
                            "content": "{ \"tips\": [\"Mantenha um horário regular\", \"Evite telas antes de dormir\"], \"warnings\": [\"Sono levemente fragmentado\"], \"positiveReinforcement\": \"Bom equilíbrio entre fases do sono\" }"
                        }
                    }
                ]
            }
        """.trimIndent()
        
        val mockResponse2 = """
            {
                "choices": [
                    {
                        "message": {
                            "content": "{ \"tips\": [\"Mantenha um horário regular\", \"Pratique relaxamento antes de dormir\"], \"warnings\": [\"Sono levemente fragmentado\"], \"positiveReinforcement\": \"Bom equilíbrio entre fases do sono\" }"
                        }
                    }
                ]
            }
        """.trimIndent()
        
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(mockResponse1))
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(mockResponse2))
        
        // QUANDO: Geramos conselhos para os dois conjuntos
        val advice1 = service.generateSleepAdvice(sessions1)
        val advice2 = service.generateSleepAdvice(sessions2)
        
        // ENTÃO: As mensagens devem ser coerentes (não totalmente diferentes)
        assertEquals(advice1.warnings, advice2.warnings) // Avisos iguais
        assertEquals(advice1.positiveReinforcement, advice2.positiveReinforcement) // Reforço igual
        assertNotEquals(advice1.tips, advice2.tips) // Dicas diferentes (variadas)
    }

    private fun createSleepSession(
        startTime: ZonedDateTime = ZonedDateTime.now().minusDays(1).minusHours(8),
        endTime: ZonedDateTime = ZonedDateTime.now().minusDays(1),
        deepSleepPercentage: Double = 25.0,
        remSleepPercentage: Double = 20.0,
        lightSleepPercentage: Double = 55.0,
        wakeDuringNightCount: Int = 1
    ): SleepSession {
        val duration = Duration.between(startTime, endTime)
        
        // Cria estágios de acordo com as porcentagens
        val stages = mutableListOf<SleepStage>()
        
        if (deepSleepPercentage > 0) {
            val deepDuration = Duration.ofMinutes((duration.toMinutes() * deepSleepPercentage / 100).toLong())
            stages.add(SleepStage(SleepStageType.DEEP, deepDuration))
        }
        
        if (remSleepPercentage > 0) {
            val remDuration = Duration.ofMinutes((duration.toMinutes() * remSleepPercentage / 100).toLong())
            stages.add(SleepStage(SleepStageType.REM, remDuration))
        }
        
        if (lightSleepPercentage > 0) {
            val lightDuration = Duration.ofMinutes((duration.toMinutes() * lightSleepPercentage / 100).toLong())
            stages.add(SleepStage(SleepStageType.LIGHT, lightDuration))
        }
        
        return SleepSession(
            id = "test-${System.currentTimeMillis()}",
            startTime = startTime,
            endTime = endTime,
            stages = stages,
            isManualEntry = true,
            notes = "Sessão de teste",
            wakeDuringNightCount = wakeDuringNightCount
        )
    }
}
