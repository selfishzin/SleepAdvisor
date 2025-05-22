import tensorflow as tf
import numpy as np
from sklearn.model_selection import train_test_split

# Dados simulados para treinamento
# Formato: [deep_sleep%, rem_sleep%, light_sleep%, efficiency, wake_count, duration_minutes]
X = np.array([
    [25, 25, 50, 90, 1, 480],  # Bom sono
    [22, 23, 55, 88, 2, 460],  # Bom sono variante
    [20, 22, 58, 85, 2, 450],  # Sono bom-mu00e9dio
    [15, 20, 65, 75, 3, 420],  # Sono mu00e9dio
    [18, 18, 64, 72, 4, 400],  # Sono mu00e9dio variante
    [12, 18, 70, 65, 4, 380],  # Sono mu00e9dio-ruim
    [10, 15, 75, 60, 5, 360],  # Sono ruim
    [8, 12, 80, 55, 6, 340],   # Sono muito ruim
    [5, 10, 85, 50, 7, 320],   # Sono extremamente ruim
])

# Sau00eddas desejadas para cada categoria de recomendau00e7u00e3o
# [geral, sono_profundo, sono_rem, eficiu00eancia, despertares, durau00e7u00e3o]
y = np.array([
    [0.3, 0.2, 0.2, 0.1, 0.1, 0.1],  # Bom sono
    [0.3, 0.2, 0.2, 0.2, 0.2, 0.2],  # Bom sono variante
    [0.4, 0.3, 0.3, 0.3, 0.3, 0.3],  # Sono bom-mu00e9dio
    [0.5, 0.6, 0.5, 0.5, 0.4, 0.4],  # Sono mu00e9dio
    [0.5, 0.6, 0.6, 0.6, 0.5, 0.5],  # Sono mu00e9dio variante
    [0.6, 0.7, 0.7, 0.7, 0.6, 0.6],  # Sono mu00e9dio-ruim
    [0.7, 0.8, 0.8, 0.8, 0.7, 0.7],  # Sono ruim
    [0.8, 0.9, 0.9, 0.9, 0.8, 0.8],   # Sono muito ruim
    [0.9, 0.9, 0.9, 0.9, 0.9, 0.9],   # Sono extremamente ruim
])

# Dividir dados em treino e teste
X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)

# Normalizar os dados
X_mean = X_train.mean(axis=0)
X_std = X_train.std(axis=0)
X_train_norm = (X_train - X_mean) / X_std
X_test_norm = (X_test - X_mean) / X_std

# Criar modelo
model = tf.keras.Sequential([
    tf.keras.layers.Dense(12, activation='relu', input_shape=(6,)),
    tf.keras.layers.Dense(8, activation='relu'),
    tf.keras.layers.Dense(6, activation='sigmoid')
])

# Compilar modelo
model.compile(optimizer='adam', loss='mse', metrics=['mae'])

# Treinar modelo
model.fit(X_train_norm, y_train, epochs=100, batch_size=1, verbose=1)

# Avaliar modelo
loss, mae = model.evaluate(X_test_norm, y_test)
print(f"Teste MAE: {mae}")

# Converter para TensorFlow Lite
converter = tf.lite.TFLiteConverter.from_keras_model(model)
tflite_model = converter.convert()

# Salvar modelo
with open('app/src/main/assets/sleep_analysis_model.tflite', 'wb') as f:
    f.write(tflite_model)

print("Modelo salvo com sucesso em app/src/main/assets/sleep_analysis_model.tflite")
