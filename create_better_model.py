import tensorflow as tf
import numpy as np
from sklearn.model_selection import train_test_split

# Dados mais diversificados para treinamento
# Formato: [deep_sleep%, rem_sleep%, light_sleep%, efficiency, wake_count, duration_minutes]
X = np.array([
    # Sono excelente
    [25, 25, 50, 95, 0, 480],
    [24, 26, 50, 94, 0, 470],
    [23, 24, 53, 93, 1, 465],
    
    # Sono bom
    [22, 23, 55, 88, 2, 460],
    [21, 22, 57, 87, 2, 450],
    [20, 21, 59, 86, 2, 445],
    
    # Sono médio
    [18, 19, 63, 80, 3, 430],
    [16, 18, 66, 78, 3, 420],
    [15, 17, 68, 75, 4, 410],
    
    # Sono ruim
    [13, 15, 72, 70, 4, 390],
    [11, 14, 75, 65, 5, 380],
    [10, 13, 77, 62, 5, 370],
    
    # Sono muito ruim
    [8, 11, 81, 58, 6, 350],
    [7, 10, 83, 55, 6, 340],
    [5, 8, 87, 50, 7, 320],
])

# Saídas desejadas para cada categoria de recomendação
# [geral, sono_profundo, sono_rem, eficiência, despertares, duração]
y = np.array([
    # Sono excelente
    [0.2, 0.1, 0.1, 0.1, 0.1, 0.1],
    [0.2, 0.1, 0.1, 0.1, 0.1, 0.1],
    [0.2, 0.1, 0.1, 0.1, 0.1, 0.1],
    
    # Sono bom
    [0.3, 0.2, 0.2, 0.2, 0.2, 0.2],
    [0.3, 0.2, 0.2, 0.2, 0.2, 0.2],
    [0.3, 0.3, 0.2, 0.2, 0.2, 0.2],
    
    # Sono médio
    [0.4, 0.5, 0.4, 0.4, 0.3, 0.3],
    [0.5, 0.5, 0.5, 0.5, 0.4, 0.4],
    [0.5, 0.6, 0.6, 0.5, 0.4, 0.4],
    
    # Sono ruim
    [0.6, 0.7, 0.7, 0.6, 0.5, 0.5],
    [0.7, 0.8, 0.7, 0.7, 0.6, 0.6],
    [0.7, 0.8, 0.8, 0.7, 0.6, 0.6],
    
    # Sono muito ruim
    [0.8, 0.9, 0.9, 0.8, 0.7, 0.7],
    [0.9, 0.9, 0.9, 0.9, 0.8, 0.8],
    [0.9, 0.9, 0.9, 0.9, 0.9, 0.9],
])

# Dividir dados em treino e teste
X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)

# Normalizar os dados
X_mean = X_train.mean(axis=0)
X_std = X_train.std(axis=0)
X_train_norm = (X_train - X_mean) / X_std
X_test_norm = (X_test - X_mean) / X_std

# Criar um modelo mais complexo
model = tf.keras.Sequential([
    tf.keras.layers.Dense(24, activation='relu', input_shape=(6,)),
    tf.keras.layers.Dense(16, activation='relu'),
    tf.keras.layers.Dense(12, activation='relu'),
    tf.keras.layers.Dense(6, activation='sigmoid')
])

# Compilar modelo com otimizador mais adequado
model.compile(optimizer=tf.keras.optimizers.Adam(learning_rate=0.001), 
              loss='mse', 
              metrics=['mae'])

# Treinar modelo com mais épocas e callbacks
early_stopping = tf.keras.callbacks.EarlyStopping(
    monitor='val_loss', patience=30, restore_best_weights=True)

model.fit(
    X_train_norm, y_train, 
    epochs=300,
    batch_size=2, 
    verbose=1,
    validation_split=0.2,
    callbacks=[early_stopping]
)

# Avaliar modelo
loss, mae = model.evaluate(X_test_norm, y_test)
print(f"Teste MAE: {mae}")

# Converter para TensorFlow Lite com otimização
converter = tf.lite.TFLiteConverter.from_keras_model(model)

# Habilitar otimizações
converter.optimizations = [tf.lite.Optimize.DEFAULT]

# Quantização para reduzir tamanho e melhorar desempenho
converter.target_spec.supported_types = [tf.float16]

tflite_model = converter.convert()

# Salvar modelo
with open('app/src/main/assets/sleep_analysis_model.tflite', 'wb') as f:
    f.write(tflite_model)

print("Modelo melhorado salvo com sucesso em app/src/main/assets/sleep_analysis_model.tflite")
print(f"Tamanho do modelo: {len(tflite_model) / 1024:.2f} KB")

# Salvar também os valores de normalização para uso no app
import json
normalization_values = {
    "mean": X_mean.tolist(),
    "std": X_std.tolist()
}

with open('app/src/main/assets/normalization_values.json', 'w') as f:
    json.dump(normalization_values, f)

print("Valores de normalização salvos com sucesso.")
