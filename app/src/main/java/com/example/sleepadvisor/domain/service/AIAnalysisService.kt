package com.example.sleepadvisor.domain.service

import android.content.Context
import com.example.sleepadvisor.domain.model.SleepAdvice
import com.example.sleepadvisor.domain.model.SleepSession
import com.example.sleepadvisor.domain.model.IdealSleepSchedule
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton
import org.json.JSONObject
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Singleton
class AIAnalysisService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var interpreter: Interpreter? = null
    private var normalizationMean: FloatArray? = null
    private var normalizationStd: FloatArray? = null
    
    init {
        loadModel()
        loadNormalizationValues()
    }
    
    private fun loadModel() {
        try {
            android.util.Log.d("AIAnalysisService", "Tentando carregar modelo TensorFlow Lite")
            val modelFile = FileUtil.loadMappedFile(context, "sleep_analysis_model.tflite")
            
            // Verificar se o arquivo do modelo é válido (não vazio)
            if (modelFile.capacity() <= 1) {
                throw IllegalArgumentException("Arquivo do modelo está vazio ou corrompido")
            }
            
            // Criar opções de interpretação para melhorar o desempenho
            val options = Interpreter.Options().apply {
                setNumThreads(2) // Usar 2 threads para processamento
                setUseNNAPI(true) // Usar Neural Network API quando disponível
            }
            
            interpreter = Interpreter(modelFile, options)
            android.util.Log.d("AIAnalysisService", "Modelo TensorFlow Lite carregado com sucesso")
        } catch (e: Exception) {
            // Fallback para análise baseada em regras se o modelo falhar
            android.util.Log.e("AIAnalysisService", "Erro ao carregar modelo TensorFlow Lite: ${e.message}. Usando sistema de regras como fallback.", e)
            interpreter = null // Garantir que o interpreter seja nulo para usar o fallback
        }
    }
    
    private fun loadNormalizationValues() {
        try {
            val inputStream = context.assets.open("normalization_values.json")
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            
            val json = JSONObject(String(buffer))
            val meanArray = json.getJSONArray("mean")
            val stdArray = json.getJSONArray("std")
            
            normalizationMean = FloatArray(meanArray.length())
            normalizationStd = FloatArray(stdArray.length())
            
            for (i in 0 until meanArray.length()) {
                normalizationMean!![i] = meanArray.getDouble(i).toFloat()
                normalizationStd!![i] = stdArray.getDouble(i).toFloat()
            }
            
            android.util.Log.d("AIAnalysisService", "Valores de normalização carregados com sucesso")
        } catch (e: Exception) {
            android.util.Log.e("AIAnalysisService", "Erro ao carregar valores de normalização: ${e.message}", e)
            normalizationMean = null
            normalizationStd = null
        }
    }
    
    fun analyzeSleepData(sleepSession: SleepSession): SleepAdvice {
        android.util.Log.d("AIAnalysisService", "Analisando dados de sono para sessão: ${sleepSession.id}")
        return try {
            if (interpreter != null) {
                android.util.Log.d("AIAnalysisService", "Usando modelo TensorFlow Lite para análise")
                analyzeWithTensorFlow(sleepSession)
            } else {
                android.util.Log.d("AIAnalysisService", "Usando sistema de regras para análise (fallback)")
                analyzeWithRules(sleepSession)
            }
        } catch (e: Exception) {
            android.util.Log.e("AIAnalysisService", "Erro durante análise com TensorFlow: ${e.message}. Usando sistema de regras.", e)
            analyzeWithRules(sleepSession)
        }
    }
    
    private fun analyzeWithTensorFlow(sleepSession: SleepSession): SleepAdvice {
        // Preparar dados de entrada
        val inputData = floatArrayOf(
            sleepSession.deepSleepPercentage.toFloat(),
            sleepSession.remSleepPercentage.toFloat(),
            sleepSession.lightSleepPercentage.toFloat(),
            sleepSession.efficiency.toFloat(),
            sleepSession.wakeDuringNightCount.toFloat(),
            sleepSession.duration.toMinutes().toFloat()
        )
        
        // Normalizar dados se os valores de normalização estiverem disponíveis
        val normalizedInput = if (normalizationMean != null && normalizationStd != null) {
            FloatArray(inputData.size) { i ->
                (inputData[i] - normalizationMean!![i]) / normalizationStd!![i]
            }
        } else {
            inputData
        }
        
        // Preparar buffer de entrada
        val inputBuffer = ByteBuffer.allocateDirect(6 * 4) // 6 features * 4 bytes por float
            .order(ByteOrder.nativeOrder())
            .apply {
                for (value in normalizedInput) {
                    putFloat(value)
                }
            }
        
        // Preparar buffer de saída
        val outputBuffer = ByteBuffer.allocateDirect(6 * 4) // 6 categorias de saída
            .order(ByteOrder.nativeOrder())
        
        // Executar inferência
        interpreter?.run(inputBuffer, outputBuffer)
        
        // Processar resultados
        outputBuffer.rewind()
        val results = FloatArray(6)
        for (i in 0 until 6) {
            results[i] = outputBuffer.float
        }
        
        return processResults(results, sleepSession)
    }
    
    private fun processResults(results: FloatArray, sleepSession: SleepSession): SleepAdvice {
        val recommendations = mutableListOf<String>()
        val supportingFacts = mutableListOf<String>()
        
        // Adicionar fatos específicos baseados nos dados da sessão
        supportingFacts.add("Seu sono profundo foi de ${sleepSession.deepSleepPercentage.toInt()}% (ideal: 20-25%)")
        supportingFacts.add("Seu sono REM foi de ${sleepSession.remSleepPercentage.toInt()}% (ideal: 20-25%)")
        supportingFacts.add("Sua eficiência do sono foi de ${sleepSession.efficiency.toInt()}% (ideal: >85%)")
        supportingFacts.add("Você teve ${sleepSession.wakeDuringNightCount} despertares durante a noite")
        supportingFacts.add("Você dormiu por ${sleepSession.duration.toHours()} horas e ${sleepSession.duration.toMinutesPart()} minutos")
        
        // Usar limiares dinâmicos baseados nos dados reais
        val deepSleepThreshold = if (sleepSession.deepSleepPercentage < 15) 0.5 else 0.7
        val remSleepThreshold = if (sleepSession.remSleepPercentage < 20) 0.5 else 0.7
        val efficiencyThreshold = if (sleepSession.efficiency < 80) 0.5 else 0.7
        
        // Interpretar resultados do modelo com recomendações mais específicas
        // Categoria 0: Recomendações gerais
        if (results[0] > 0.6) {
            val horasDormidas = sleepSession.duration.toHours()
            val minutosDormidos = sleepSession.duration.toMinutesPart()
            recommendations.add("Mantenha um horário regular para dormir e acordar. Você dormiu ${horasDormidas}h${minutosDormidos}min, tente manter este padrão consistente.")
        }
        
        // Categoria 1: Sono profundo - recomendações mais específicas
        if (results[1] > deepSleepThreshold) {
            when {
                sleepSession.deepSleepPercentage < 10 -> 
                    recommendations.add("Seu sono profundo está muito baixo (${sleepSession.deepSleepPercentage.toInt()}%). Considere exercícios físicos moderados pela manhã e evite cafeína após as 14h.")
                sleepSession.deepSleepPercentage < 15 -> 
                    recommendations.add("Para aumentar seu sono profundo de ${sleepSession.deepSleepPercentage.toInt()}%, reduza o consumo de álcool e pratique meditação antes de dormir.")
                sleepSession.deepSleepPercentage < 20 -> 
                    recommendations.add("Seu sono profundo de ${sleepSession.deepSleepPercentage.toInt()}% pode melhorar com um ambiente mais fresco (18-20°C) e completamente escuro.")
                else -> 
                    recommendations.add("Seu sono profundo está adequado (${sleepSession.deepSleepPercentage.toInt()}%). Continue mantendo um ambiente tranquilo e fresco para dormir.")
            }
        }
        
        // Categoria 2: Sono REM - recomendações mais específicas
        if (results[2] > remSleepThreshold) {
            when {
                sleepSession.remSleepPercentage < 15 -> 
                    recommendations.add("Seu sono REM está muito baixo (${sleepSession.remSleepPercentage.toInt()}%). Evite bebidas alcoólicas antes de dormir e estabeleça uma rotina de relaxamento.")
                sleepSession.remSleepPercentage < 20 -> 
                    recommendations.add("Para melhorar seu sono REM de ${sleepSession.remSleepPercentage.toInt()}%, tente técnicas de respiração profunda antes de dormir e evite refeições pesadas à noite.")
                else -> 
                    recommendations.add("Seu sono REM está adequado (${sleepSession.remSleepPercentage.toInt()}%). Para mantê-lo, continue praticando bons hábitos de sono.")
            }
        }
        
        // Categoria 3: Eficiência do sono - recomendações mais específicas
        if (results[3] > efficiencyThreshold) {
            when {
                sleepSession.efficiency < 70 -> 
                    recommendations.add("Sua eficiência do sono está baixa (${sleepSession.efficiency.toInt()}%). Tente limitar o tempo na cama apenas para dormir e evite usar dispositivos eletrônicos 2 horas antes de dormir.")
                sleepSession.efficiency < 85 -> 
                    recommendations.add("Para aumentar sua eficiência do sono de ${sleepSession.efficiency.toInt()}%, mantenha seu quarto silencioso e considere usar tampões de ouvido se necessário.")
                else -> 
                    recommendations.add("Sua eficiência do sono está excelente (${sleepSession.efficiency.toInt()}%). Continue mantendo seu ambiente de sono ideal.")
            }
        }
        
        // Categoria 4: Despertares noturnos - recomendações mais específicas
        if (results[4] > 0.6 || sleepSession.wakeDuringNightCount > 2) {
            when {
                sleepSession.wakeDuringNightCount > 4 -> 
                    recommendations.add("Você teve ${sleepSession.wakeDuringNightCount} despertares. Evite líquidos 2 horas antes de dormir e considere um ruído branco para mascarar sons perturbadores.")
                sleepSession.wakeDuringNightCount > 2 -> 
                    recommendations.add("Para reduzir seus ${sleepSession.wakeDuringNightCount} despertares noturnos, mantenha a temperatura do quarto estável e evite luz azul à noite.")
                else -> 
                    recommendations.add("Seus despertares noturnos (${sleepSession.wakeDuringNightCount}) estão dentro do normal. Para melhorar ainda mais, evite refeições pesadas antes de dormir.")
            }
        }
        
        // Categoria 5: Duração do sono - recomendações mais específicas
        if (results[5] > 0.6 || sleepSession.duration.toHours() < 7) {
            val horasDormidas = sleepSession.duration.toHours()
            val minutosDormidos = sleepSession.duration.toMinutesPart()
            when {
                horasDormidas < 6 -> 
                    recommendations.add("Você dormiu apenas ${horasDormidas}h${minutosDormidos}min. Tente reservar pelo menos 7-8 horas para sono, ajustando seu horário de dormir para 30 minutos mais cedo.")
                horasDormidas < 7 -> 
                    recommendations.add("Sua duração de sono de ${horasDormidas}h${minutosDormidos}min está um pouco abaixo do ideal. Tente dormir 30 minutos mais cedo para atingir 7-8 horas por noite.")
                else -> 
                    recommendations.add("Sua duração de sono de ${horasDormidas}h${minutosDormidos}min está adequada. Mantenha essa consistência para benefícios à saúde a longo prazo.")
            }
        }
        
        // Se ainda não houver recomendações específicas, adicione baseadas nos dados da sessão
        if (recommendations.isEmpty()) {
            if (sleepSession.deepSleepPercentage < 20 || sleepSession.remSleepPercentage < 20 || sleepSession.efficiency < 85) {
                recommendations.add("Seus padrões de sono mostram oportunidades de melhoria. Tente manter um horário regular e evite estimulantes à noite.")
            } else {
                recommendations.add("Seus padrões de sono estão saudáveis. Continue mantendo bons hábitos para preservar sua saúde.")
            }
        }
        
        // Calcular pontuação de qualidade
        val sleepScore = calculateSleepScore(results, sleepSession)
        
        return SleepAdvice(
            mainAdvice = generateMainAdvice(sleepScore, sleepSession),
            supportingFacts = supportingFacts,
            customRecommendations = recommendations.take(3),
            qualityTrend = determineTrend(sleepScore, sleepSession),
            consistencyScore = sleepScore,
            idealsleepSchedule = suggestIdealSchedule(sleepSession, sleepScore)
        )
    }
    
    private fun analyzeWithRules(sleepSession: SleepSession): SleepAdvice {
        android.util.Log.d("AIAnalysisService", "Iniciando análise baseada em regras")
        // Sistema de regras como fallback
        val recommendations = mutableListOf<String>()
        val supportingFacts = mutableListOf<String>()
        
        // Adicionar fatos específicos baseados nos dados da sessão
        supportingFacts.add("Seu sono profundo foi de ${sleepSession.deepSleepPercentage.toInt()}% (ideal: 20-25%)")
        supportingFacts.add("Seu sono REM foi de ${sleepSession.remSleepPercentage.toInt()}% (ideal: 20-25%)")
        supportingFacts.add("Sua eficiência do sono foi de ${sleepSession.efficiency.toInt()}% (ideal: >85%)")
        supportingFacts.add("Você teve ${sleepSession.wakeDuringNightCount} despertares durante a noite")
        supportingFacts.add("Você dormiu por ${sleepSession.duration.toHours()} horas e ${sleepSession.duration.toMinutesPart()} minutos")
        
        // Regras para sono profundo - mais detalhadas
        when {
            sleepSession.deepSleepPercentage < 10 -> 
                recommendations.add("Seu sono profundo está muito baixo (${sleepSession.deepSleepPercentage.toInt()}%). Considere exercícios físicos moderados pela manhã e evite cafeína após as 14h.")
            sleepSession.deepSleepPercentage < 15 -> 
                recommendations.add("Para aumentar seu sono profundo de ${sleepSession.deepSleepPercentage.toInt()}%, reduza o consumo de álcool e pratique meditação antes de dormir.")
            sleepSession.deepSleepPercentage < 20 -> 
                recommendations.add("Seu sono profundo de ${sleepSession.deepSleepPercentage.toInt()}% pode melhorar com um ambiente mais fresco (18-20°C) e completamente escuro.")
            else -> 
                recommendations.add("Seu sono profundo está adequado (${sleepSession.deepSleepPercentage.toInt()}%). Continue mantendo um ambiente tranquilo e fresco para dormir.")
        }
        
        // Regras para sono REM - mais detalhadas
        when {
            sleepSession.remSleepPercentage < 15 -> 
                recommendations.add("Seu sono REM está muito baixo (${sleepSession.remSleepPercentage.toInt()}%). Evite bebidas alcoólicas antes de dormir e estabeleça uma rotina de relaxamento.")
            sleepSession.remSleepPercentage < 20 -> 
                recommendations.add("Para melhorar seu sono REM de ${sleepSession.remSleepPercentage.toInt()}%, tente técnicas de respiração profunda antes de dormir e evite refeições pesadas à noite.")
            else -> 
                recommendations.add("Seu sono REM está adequado (${sleepSession.remSleepPercentage.toInt()}%). Para mantê-lo, continue praticando bons hábitos de sono.")
        }
        
        // Regras para eficiência - mais detalhadas
        when {
            sleepSession.efficiency < 70 -> 
                recommendations.add("Sua eficiência do sono está baixa (${sleepSession.efficiency.toInt()}%). Tente limitar o tempo na cama apenas para dormir e evite usar dispositivos eletrônicos 2 horas antes de dormir.")
            sleepSession.efficiency < 85 -> 
                recommendations.add("Para aumentar sua eficiência do sono de ${sleepSession.efficiency.toInt()}%, mantenha seu quarto silencioso e considere usar tampões de ouvido se necessário.")
            else -> 
                recommendations.add("Sua eficiência do sono está excelente (${sleepSession.efficiency.toInt()}%). Continue mantendo seu ambiente de sono ideal.")
        }
        
        // Calcular pontuação
        val sleepScore = calculateRuleBasedScore(sleepSession)
        
        android.util.Log.d("AIAnalysisService", "Análise baseada em regras concluída. Pontuação: $sleepScore")
        
        return SleepAdvice(
            mainAdvice = generateMainAdvice(sleepScore, sleepSession),
            supportingFacts = supportingFacts,
            customRecommendations = recommendations.take(3),
            qualityTrend = determineTrend(sleepScore, sleepSession),
            consistencyScore = sleepScore,
            idealsleepSchedule = suggestIdealSchedule(sleepSession, sleepScore)
        )
    }
    
    private fun calculateSleepScore(results: FloatArray, sleepSession: SleepSession): Int {
        // Lógica melhorada para calcular pontuação baseada nos resultados do modelo
        // Damos pesos diferentes para cada categoria
        val weights = floatArrayOf(0.15f, 0.25f, 0.25f, 0.15f, 0.1f, 0.1f)
        var weightedSum = 0f
        
        for (i in results.indices) {
            // Inverter a pontuação para categorias onde valores altos são negativos
            val score = if (i > 0) (1 - results[i]) else results[i]
            weightedSum += score * weights[i]
        }
        
        // Ajustar pontuação com base em dados reais
        var adjustedScore = (weightedSum * 100).toInt()
        
        // Penalizar pontuação se houver valores muito fora do ideal
        if (sleepSession.deepSleepPercentage < 10) adjustedScore -= 10
        if (sleepSession.remSleepPercentage < 15) adjustedScore -= 10
        if (sleepSession.efficiency < 70) adjustedScore -= 10
        if (sleepSession.wakeDuringNightCount > 5) adjustedScore -= 10
        if (sleepSession.duration.toHours() < 6) adjustedScore -= 10
        
        return adjustedScore.coerceIn(0, 100)
    }
    
    private fun calculateRuleBasedScore(sleepSession: SleepSession): Int {
        // Lógica para calcular pontuação baseada em regras - mais granular
        val deepSleepScore = when {
            sleepSession.deepSleepPercentage < 10 -> 50
            sleepSession.deepSleepPercentage < 15 -> 65
            sleepSession.deepSleepPercentage < 20 -> 80
            sleepSession.deepSleepPercentage < 25 -> 90
            else -> 100
        }
        
        val remSleepScore = when {
            sleepSession.remSleepPercentage < 15 -> 50
            sleepSession.remSleepPercentage < 20 -> 70
            sleepSession.remSleepPercentage < 25 -> 85
            else -> 100
        }
        
        val efficiencyScore = when {
            sleepSession.efficiency < 70 -> 50
            sleepSession.efficiency < 80 -> 70
            sleepSession.efficiency < 90 -> 85
            else -> 100
        }
        
        val wakingsScore = when {
            sleepSession.wakeDuringNightCount > 5 -> 50
            sleepSession.wakeDuringNightCount > 3 -> 70
            sleepSession.wakeDuringNightCount > 1 -> 85
            else -> 100
        }
        
        val durationScore = when {
            sleepSession.duration.toHours() < 6 -> 50
            sleepSession.duration.toHours() < 7 -> 70
            sleepSession.duration.toHours() < 8 -> 90
            else -> 100
        }
        
        // Pesos para cada componente
        return ((deepSleepScore * 0.3) + 
                (remSleepScore * 0.25) + 
                (efficiencyScore * 0.2) + 
                (wakingsScore * 0.15) + 
                (durationScore * 0.1)).toInt()
    }
    
    private fun generateMainAdvice(sleepScore: Int, sleepSession: SleepSession): String {
        // Gerar conselho principal mais personalizado
        val deepSleepStatus = when {
            sleepSession.deepSleepPercentage < 15 -> "baixo"
            sleepSession.deepSleepPercentage < 20 -> "adequado, mas pode melhorar"
            else -> "bom"
        }
        
        val remSleepStatus = when {
            sleepSession.remSleepPercentage < 15 -> "baixo"
            sleepSession.remSleepPercentage < 20 -> "adequado, mas pode melhorar"
            else -> "bom"
        }
        
        return when {
            sleepScore >= 90 -> "Seu sono está excelente! Seu sono profundo e REM estão em níveis ótimos, continue mantendo esses bons hábitos."
            sleepScore >= 80 -> "Seu sono está bom, com seu sono $deepSleepStatus e sono REM $remSleepStatus. Pequenos ajustes podem melhorar ainda mais sua qualidade de sono."
            sleepScore >= 70 -> "Seu sono está regular, com sono profundo $deepSleepStatus e sono REM $remSleepStatus. Implementar as recomendações sugeridas pode trazer melhorias significativas."
            sleepScore >= 60 -> "Seu sono precisa de atenção. Seu sono profundo está $deepSleepStatus e seu sono REM está $remSleepStatus. Considere implementar as recomendações sugeridas."
            else -> "Seu sono está significativamente comprometido, com níveis baixos de sono profundo e REM. Recomendamos consultar um especialista em sono."
        }
    }
    
    private fun determineTrend(sleepScore: Int, sleepSession: SleepSession): String {
        // Em uma implementação real, isso seria baseado em dados históricos
        // Por enquanto, vamos dar um feedback mais personalizado
        return when {
            sleepScore > 85 -> "Excelente"
            sleepScore > 70 -> "Bom"
            sleepScore > 60 -> "Regular"
            else -> "Precisa de atenção"
        }
    }
    
    private fun suggestIdealSchedule(sleepSession: SleepSession, sleepScore: Int): IdealSleepSchedule {
        // Lógica personalizada para sugerir horário ideal baseado nos padrões atuais
        val currentBedtime = sleepSession.startTimeZoned.toLocalTime()
        val currentWakeTime = sleepSession.endTimeZoned.toLocalTime()
        
        // Calcular horário ideal baseado nos dados atuais e na pontuação
        val idealDuration = when {
            sleepSession.deepSleepPercentage < 15 || sleepSession.remSleepPercentage < 15 -> 8.5
            sleepScore < 70 -> 8.0
            else -> 7.5
        }
        
        // Formatar o horário atual para exibição
        val formatter = DateTimeFormatter.ofPattern("'às' HH'h'mm")
        val currentBedtimeStr = currentBedtime.format(formatter)
        
        // Ajustar horário de dormir baseado na qualidade do sono
        val suggestedBedtime = when {
            sleepScore < 60 -> {
                // Sugerir ir dormir 1 hora mais cedo que o atual, mas não antes das 21:00
                val earlierTime = currentBedtime.minusHours(1)
                val minTime = LocalTime.of(21, 0)
                if (earlierTime.isBefore(minTime)) minTime else earlierTime
            }
            sleepScore < 80 -> {
                // Sugerir ir dormir 30 minutos mais cedo que o atual
                currentBedtime.minusMinutes(30)
            }
            else -> {
                // Manter o horário atual se o sono estiver bom
                currentBedtime
            }
        }.format(formatter)
        
        // Calcular horário de acordar baseado no horário de dormir sugerido e duração ideal
        val suggestedBedtimeTime = LocalTime.parse(suggestedBedtime, formatter)
        val suggestedWakeTime = suggestedBedtimeTime.plusHours(idealDuration.toLong())
            .plusMinutes(((idealDuration % 1) * 60).toLong())
            .format(formatter)
        
        return IdealSleepSchedule(
            suggestedBedtime = suggestedBedtime,
            suggestedWakeTime = suggestedWakeTime,
            idealSleepDuration = "${idealDuration.toString().replace('.', ',')} horas"
        )
    }
}
