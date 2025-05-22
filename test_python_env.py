# Script para testar o ambiente Python e as bibliotecas necessárias

import sys
import os

def check_library(library_name):
    try:
        lib = __import__(library_name)
        version = getattr(lib, '__version__', 'versão desconhecida')
        return True, version
    except ImportError:
        return False, None

def main():
    print("=== Verificação do Ambiente Python ===")

    # Verificar versão do Python
    if sys.version_info < (3, 6):
        print("Este script requer Python 3.6 ou superior.")
        sys.exit(1)
    print(f"Versão do Python: {sys.version}")

    # Verificar bibliotecas necessárias
    libraries = ['tensorflow', 'numpy', 'sklearn', 'json']
    print("\nVerificando bibliotecas necessárias:")
    for lib in libraries:
        installed, version = check_library(lib)
        if installed:
            print(f"{lib}: INSTALADO (versão {version})")
        else:
            print(f"{lib}: NÃO INSTALADO")

    # Verificar se a pasta de destino existe
    assets_path = os.path.join('app', 'src', 'main', 'assets')
    full_path = os.path.abspath(assets_path)
    print(f"\nVerificando pasta: {full_path}")
    if os.path.exists(assets_path):
        print("Pasta: EXISTE")
    else:
        print("Pasta: NÃO EXISTE")
        print("Tentando criar a pasta...")
        try:
            os.makedirs(assets_path, exist_ok=True)
            print("Pasta criada com sucesso!")
        except Exception as e:
            print(f"Erro ao criar pasta: {e}")

    input("\nPressione Enter para sair...")

if __name__ == "__main__":
    main()
