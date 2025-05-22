import urllib.request
import os

# Create assets directory if it doesn't exist
assets_dir = 'app/src/main/assets'
os.makedirs(assets_dir, exist_ok=True)

# URL to a sample TFLite model (MobileNet v1 quantized)
model_url = 'https://storage.googleapis.com/download.tensorflow.org/models/tflite/mobilenet_v1_224_android_quant_2017_11_08.zip'
zip_path = 'model.zip'

print(f"Downloading model from {model_url}...")
try:
    # Download the zip file
    urllib.request.urlretrieve(model_url, zip_path)
    print(f"Downloaded to {zip_path}")
    
    # Extract the zip file
    import zipfile
    with zipfile.ZipFile(zip_path, 'r') as zip_ref:
        zip_ref.extractall('.')
    print("Extracted zip file")
    
    # Copy the model file to assets directory
    import shutil
    source_model = 'mobilenet_quant_v1_224.tflite'
    target_model = os.path.join(assets_dir, 'sleep_analysis_model.tflite')
    shutil.copy(source_model, target_model)
    print(f"Copied model to {target_model}")
    
    # Clean up
    os.remove(zip_path)
    os.remove(source_model)
    print("Cleaned up temporary files")
    
    print("\nModel successfully downloaded and placed in assets directory!")
    print(f"Model size: {os.path.getsize(target_model)} bytes")
    
except Exception as e:
    print(f"Error: {e}")
