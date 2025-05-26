# sleep_model_enhanced.py
import tensorflow as tf
import numpy as np
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import RobustScaler
from sklearn.utils import shuffle
import json
import os
import random

# Função para gerar dados sintéticos realistas
def generate_synthetic_data(base_data, num_samples=1000):
    X, y = [], []
    for _ in range(num_samples):
        # Gera variações nos dados base
        deep_sleep = max(5, min(40, random.gauss(20, 5)))
        rem_sleep = max(10, min(30, random.gauss(20, 4)))
        light_sleep = max(30, min(85, 100 - deep_sleep - rem_sleep + random.gauss(0, 5)))
        efficiency = max(50, min(100, random.gauss(85, 10)))
        wake_count = random.randint(0, 10)
        duration = random.randint(240, 600)  # 4 a 10 horas em minutos
        
        # Calcula pontuação de qualidade baseada nos dados
        quality_score = (
            0.3 * (deep_sleep / 25) +  # Peso maior para sono profundo
            0.2 * (rem_sleep / 25) +
            0.2 * (efficiency / 100) +
            0.2 * (1 - min(wake_count, 5) / 10) +  # Penaliza muitos despertares
            0.1 * (min(duration, 540) / 540)  # 9 horas é o ideal
        )
        
        # Normaliza a pontuação para 0-1
        quality_score = max(0, min(1, quality_score))
        
        # Cria vetor de saída com diferentes aspectos da qualidade do sono
        output = [
            quality_score,  # Pontuação geral de qualidade
            deep_sleep / 40,  # Normalizado para 0-1 (0-40% é a faixa normal)
            rem_sleep / 30,   # Normalizado para 0-1 (0-30% é a faixa normal)
            (efficiency - 50) / 50,  # Normalizado para 0-1 (50-100%)
            1 - (wake_count / 10),  # Normalizado para 0-1 (invertido)
            min(duration, 600) / 600  # Normalizado para 0-1 (até 10 horas)
        ]
        
        X.append([deep_sleep, rem_sleep, light_sleep, efficiency, wake_count, duration])
        y.append(output)
    
    return np.array(X), np.array(y)

# Gerar dados de treinamento
X, y = generate_synthetic_data(None, num_samples=5000)
X, y = shuffle(X, y, random_state=42)

# Dividir em conjuntos de treino, validação e teste
X_train, X_temp, y_train, y_temp = train_test_split(X, y, test_size=0.3, random_state=42)
X_val, X_test, y_val, y_test = train_test_split(X_temp, y_temp, test_size=0.5, random_state=42)

# Normalização robusta (menos sensível a outliers)
scaler = RobustScaler(quantile_range=(5.0, 95.0))  # Usa percentis para ser mais robusto
X_train_norm = scaler.fit_transform(X_train)
X_val_norm = scaler.transform(X_val)
X_test_norm = scaler.transform(X_test)

# Função para criar o modelo
def create_model(input_shape):
    model = tf.keras.Sequential([
        # Camada de entrada com batch normalization
        tf.keras.layers.InputLayer(input_shape=input_shape),
        tf.keras.layers.BatchNormalization(),
        
        # Primeira camada oculta
        tf.keras.layers.Dense(
            128, 
            activation='swish',  # Swish pode funcionar melhor que ReLU
            kernel_regularizer=tf.keras.regularizers.l1_l2(l1=1e-5, l2=1e-4),
            kernel_initializer='he_normal'
        ),
        tf.keras.layers.BatchNormalization(),
        tf.keras.layers.Dropout(0.3),
        
        # Segunda camada oculta
        tf.keras.layers.Dense(
            64, 
            activation='swish',
            kernel_regularizer=tf.keras.regularizers.l1_l2(l1=1e-5, l2=1e-4)
        ),
        tf.keras.layers.BatchNormalization(),
        tf.keras.layers.Dropout(0.3),
        
        # Terceira camada oculta
        tf.keras.layers.Dense(
            32, 
            activation='swish',
            kernel_regularizer=tf.keras.regularizers.l2(1e-4)
        ),
        tf.keras.layers.BatchNormalization(),
        tf.keras.layers.Dropout(0.2),
        
        # Camada de saída
        tf.keras.layers.Dense(6, activation='sigmoid')
    ])
    
    return model

# Criar e compilar o modelo
model = create_model(input_shape=(X_train_norm.shape[1],))

# Otimizador com taxa de aprendizado fixa
optimizer = tf.keras.optimizers.Adam(learning_rate=0.001)

# Métricas adicionais
metrics = [
    'mae',
    tf.keras.metrics.RootMeanSquaredError(),
    tf.keras.metrics.MeanAbsolutePercentageError()
]

model.compile(
    optimizer=optimizer,
    loss='huber',  # Menos sensível a outliers que MSE
    metrics=metrics
)

# Callbacks
early_stop = tf.keras.callbacks.EarlyStopping(
    monitor='val_loss',
    patience=50,
    restore_best_weights=True,
    verbose=1
)

reduce_lr = tf.keras.callbacks.ReduceLROnPlateau(
    monitor='val_loss',
    factor=0.5,
    patience=15,
    min_lr=1e-6,
    verbose=1
)

# Treinamento com dados aumentados
history = model.fit(
    X_train_norm, y_train,
    validation_data=(X_val_norm, y_val),
    epochs=500,
    batch_size=32,
    callbacks=[early_stop, reduce_lr],
    verbose=1
)

# Avaliação no conjunto de teste
print("\nAvaliação no conjunto de teste:")
test_metrics = model.evaluate(X_test_norm, y_test, verbose=0)
print(f"Test Loss: {test_metrics[0]:.4f}")
print(f"Test MAE: {test_metrics[1]:.4f}")
print(f"Test RMSE: {test_metrics[2]:.4f}")
print(f"Test MAPE: {test_metrics[3]:.2f}%")

# Exportar modelo TFLite
converter = tf.lite.TFLiteConverter.from_keras_model(model)
converter.optimizations = [tf.lite.Optimize.DEFAULT]
converter.target_spec.supported_types = [tf.float16]
tflite_model = converter.convert()

# Criar pasta se necessário
os.makedirs('app/src/main/assets', exist_ok=True)

# Salvar modelo TFLite
model_path = 'app/src/main/assets/sleep_analysis_model.tflite'
with open(model_path, 'wb') as f:
    f.write(tflite_model)

print(f"\nModelo TFLite salvo com sucesso em {model_path}")

# Salvar scaler
norm_vals = {
    "center": scaler.center_.tolist(),
    "scale": scaler.scale_.tolist(),
    "feature_names": [
        "deep_sleep_percent", 
        "rem_sleep_percent", 
        "light_sleep_percent",
        "efficiency",
        "wake_count",
        "duration_minutes"
    ]
}

with open('app/src/main/assets/normalization_values.json', 'w') as f:
    json.dump(norm_vals, f, indent=2)

print("Valores de normalização salvos com sucesso.")

# Exemplo de predição
def predict_quality(deep, rem, light, efficiency, wake_count, duration):
    sample = np.array([[deep, rem, light, efficiency, wake_count, duration]])
    sample_norm = scaler.transform(sample)
    pred = model.predict(sample_norm, verbose=0)
    
    # Interpretar resultados
    results = {
        'quality_score': float(pred[0][0] * 100),  # 0-100%
        'deep_sleep_adequacy': float(pred[0][1] * 100),  # 0-100%
        'rem_sleep_adequacy': float(pred[0][2] * 100),  # 0-100%
        'efficiency_score': float(pred[0][3] * 100),  # 0-100%
        'sleep_continuity': float(pred[0][4] * 100),  # 0-100%
        'duration_adequacy': float(pred[0][5] * 100)  # 0-100%
    }
    
    # Gerar recomendações baseadas nos resultados
    recommendations = []
    
    # Recomendações para sono profundo
    if results['deep_sleep_adequacy'] < 50:
        recommendations.append("Aumente seu sono profundo mantendo um horário regular de sono e reduzindo a exposição a telas antes de dormir.")
    
    # Recomendações para sono REM
    if results['rem_sleep_adequacy'] < 50:
        recommendations.append("Para melhorar o sono REM, tente reduzir o consumo de álcool e cafeína, especialmente à tarde e à noite.")
    
    # Recomendações para eficiência
    if results['efficiency_score'] < 80:
        recommendations.append("Sua eficiência de sono pode melhorar. Considere otimizar seu ambiente de sono para ser mais escuro e silencioso.")
    
    # Recomendações para continuidade
    if results['sleep_continuity'] < 70:
        recommendations.append("Reduza os despertares noturnos evitando líquidos antes de dormir e mantendo uma temperatura ambiente agradável.")
    
    # Recomendações para duração
    if results['duration_adequacy'] < 70:
        recommendations.append("Tente dormir mais horas. A maioria dos adultos precisa de 7-9 horas de sono por noite.")
    
    results['recommendations'] = recommendations if recommendations else ["Seu sono parece estar em boa forma! Continue mantendo esses hábitos saudáveis."]
    
    return results

# Exemplo de uso
print("\nExemplo de predição para uma noite de sono:")
sample_result = predict_quality(
    deep=15,      # 15% de sono profundo
    rem=20,        # 20% de sono REM
    light=65,      # 65% de sono leve
    efficiency=85, # 85% de eficiência
    wake_count=3,  # 3 despertares
    duration=420   # 7 horas de sono
)

print("\nResultados da análise de sono:")
for key, value in sample_result.items():
    if key != 'recommendations':
        print(f"{key}: {value:.1f}%")

print("\nRecomendações:")
for rec in sample_result['recommendations']:
    print(f"- {rec}")
