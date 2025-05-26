#!/usr/bin/env python3
"""
Script para análise de código do projeto SleepAdvisor.
Realiza verificações de qualidade, duplicação e itens obsoletos.
"""

import os
import subprocess
import sys
import json
import ast
from pathlib import Path
from collections import defaultdict
from typing import List, Dict, Tuple, Set

# Configurações
PROJECT_ROOT = Path(__file__).parent
SOURCE_DIR = PROJECT_ROOT / "app" / "src" / "main"
REPORT_DIR = PROJECT_ROOT / "code_analysis_reports"
REPORT_DIR.mkdir(exist_ok=True)

# Cores para saída no terminal
class Colors:
    HEADER = '\033[95m'
    OKBLUE = '\033[94m'
    OKCYAN = '\033[96m'
    OKGREEN = '\033[92m'
    WARNING = '\033[93m'
    FAIL = '\033[91m'
    ENDC = '\033[0m'
    BOLD = '\033[1m'
    UNDERLINE = '\033[4m'

def print_header(text: str) -> None:
    """Imprime um cabeçalho formatado."""
    print(f"\n{Colors.HEADER}{'='*80}{Colors.ENDC}")
    print(f"{Colors.HEADER}{text.upper():^80}{Colors.ENDC}")
    print(f"{Colors.HEADER}{'='*80}{Colors.ENDC}")

def run_command(cmd: List[str], cwd: Path = None) -> Tuple[int, str]:
    """Executa um comando no terminal e retorna o código de saída e a saída."""
    try:
        result = subprocess.run(
            cmd,
            cwd=cwd or PROJECT_ROOT,
            capture_output=True,
            text=True,
            encoding='utf-8',
            errors='replace'
        )
        return result.returncode, result.stdout
    except Exception as e:
        print(f"{Colors.FAIL}Erro ao executar comando: {e}{Colors.ENDC}")
        return -1, ""

def check_python_environment() -> bool:
    """Verifica se as dependências necessárias estão instaladas."""
    print_header("verificando ambiente python")
    
    required_packages = [
        "flake8", "black", "radon", "vulture", "bandit", 
        "mypy", "pylint", "detect-secrets"
    ]
    
    missing = []
    for pkg in required_packages:
        _, out = run_command([sys.executable, "-m", "pip", "show", pkg])
        if not out.strip():
            missing.append(pkg)
    
    if missing:
        print(f"{Colors.WARNING}Os seguintes pacotes não foram encontrados:{Colors.ENDC}")
        for pkg in missing:
            print(f"- {pkg}")
        install = input("\nDeseja instalar as dependências faltantes? (s/n): ").lower()
        if install == 's':
            run_command([sys.executable, "-m", "pip", "install"] + missing)
            return True
        return False
    
    print(f"{Colors.OKGREEN}Todas as dependências necessárias estão instaladas.{Colors.ENDC}")
    return True

def analyze_code_quality() -> None:
    """Analisa a qualidade do código usando flake8 e pylint."""
    print_header("analisando qualidade do código")
    
    # Análise com flake8
    print(f"{Colors.OKBLUE}Executando flake8...{Colors.ENDC}")
    flake8_cmd = [
        sys.executable, "-m", "flake8", 
        "--max-line-length=120", 
        "--exclude=*/build/*,*/tmp/*,*/migrations/*,*/venv/*",
        str(SOURCE_DIR)
    ]
    exit_code, output = run_command(flake8_cmd)
    
    flake8_report = REPORT_DIR / "flake8_report.txt"
    with open(flake8_report, "w", encoding="utf-8") as f:
        f.write(output)
    
    if exit_code == 0:
        print(f"{Colors.OKGREEN}Nenhum problema encontrado pelo flake8.{Colors.ENDC}")
    else:
        print(f"{Colors.WARNING}Problemas encontrados pelo flake8. Verifique o relatório em {flake8_report}{Colors.ENDC}")
    
    # Análise com pylint
    print(f"\n{Colors.OKBLUE}Executando pylint...{Colors.ENDC}")
    pylint_cmd = [
        sys.executable, "-m", "pylint",
        "--rcfile=.pylintrc",
        "--output-format=text",
        str(SOURCE_DIR)
    ]
    _, output = run_command(pylint_cmd)
    
    pylint_report = REPORT_DIR / "pylint_report.txt"
    with open(pylint_report, "w", encoding="utf-8") as f:
        f.write(output)
    
    print(f"Relatório do pylint salvo em {pylint_report}")

def find_duplicate_code() -> None:
    """Encontra código duplicado usando radon."""
    print_header("buscando código duplicado")
    
    # Análise de duplicação com radon
    radon_cmd = [
        sys.executable, "-m", "radon", "cc",
        "-a",  # Mostrar todas as métricas
        "-nb",  # Não mostrar relatório de complexidade
        "-s",   # Mostrar pontuação de manutenibilidade
        str(SOURCE_DIR)
    ]
    _, output = run_command(radon_cmd)
    
    radon_report = REPORT_DIR / "radon_cc_report.txt"
    with open(radon_report, "w", encoding="utf-8") as f:
        f.write(output)
    
    print(f"Análise de complexidade ciclomática salva em {radon_report}")
    
    # Busca por código duplicado
    print(f"\n{Colors.OKBLUE}Buscando por código duplicado...{Colors.ENDC}")
    dup_cmd = [
        sys.executable, "-m", "radon", "dupes",
        "-s",  # Mostrar similaridade
        str(SOURCE_DIR)
    ]
    _, output = run_command(dup_cmd)
    
    dupe_report = REPORT_DIR / "duplicate_code_report.txt"
    with open(dupe_report, "w", encoding="utf-8") as f:
        f.write(output)
    
    print(f"Relatório de código duplicado salvo em {dupe_report}")

def find_unused_code() -> None:
    """Encontra código não utilizado usando vulture."""
    print_header("buscando código não utilizado")
    
    vulture_cmd = [
        sys.executable, "-m", "vulture",
        "--min-confidence=70",  # Limite de confiança
        "--exclude=venv",
        str(SOURCE_DIR)
    ]
    _, output = run_command(vulture_cmd)
    
    vulture_report = REPORT_DIR / "unused_code_report.txt"
    with open(vulture_report, "w", encoding="utf-8") as f:
        f.write(output)
    
    if output.strip():
        print(f"{Colors.WARNING}Possível código não utilizado encontrado. Verifique o relatório em {vulture_report}{Colors.ENDC}")
    else:
        print(f"{Colors.OKGREEN}Nenhum código não utilizado encontrado.{Colors.ENDC}")

def check_security_issues() -> None:
    """Verifica possíveis problemas de segurança no código."""
    print_header("verificando problemas de segurança")
    
    # Verificação com bandit
    print(f"{Colors.OKBLUE}Executando análise de segurança com bandit...{Colors.ENDC}")
    bandit_cmd = [
        sys.executable, "-m", "bandit",
        "-r",  # Recursivo
        "-f", "txt",  # Formato de saída
        "-o", str(REPORT_DIR / "security_report.txt"),
        str(SOURCE_DIR)
    ]
    exit_code, _ = run_command(bandit_cmd)
    
    if exit_code == 0:
        print(f"{Colors.OKGREEN}Nenhum problema de segurança encontrado pelo bandit.{Colors.ENDC}")
    else:
        print(f"{Colors.WARNING}Possíveis problemas de segurança encontrados. Verifique o relatório em {REPORT_DIR / 'security_report.txt'}{Colors.ENDC}")
    
    # Verificação de segredos no código
    print(f"\n{Colors.OKBLUE}Verificando possíveis vazamentos de segredos...{Colors.ENDC}")
    detect_secrets_cmd = [
        sys.executable, "-m", "detect_secrets", "scan",
        "--exclude-files", "package-lock.json|yarn.lock|*.min.js",
        "--exclude-lines", "password|secret|key|token",
        "--baseline", str(REPORT_DIR / ".secrets.baseline"),
        str(PROJECT_ROOT)
    ]
    _, output = run_command(detect_secrets_cmd)
    
    secrets_report = REPORT_DIR / "secrets_report.txt"
    with open(secrets_report, "w", encoding="utf-8") as f:
        f.write(output)
    
    if "Potential secrets about to be committed" in output:
        print(f"{Colors.FAIL}Possíveis vazamentos de segredos encontrados! Verifique {secrets_report}{Colors.ENDC}")
    else:
        print(f"{Colors.OKGREEN}Nenhum vazamento de segredos encontrado.{Colors.ENDC}")

def find_deprecated_apis() -> None:
    """Busca por APIs obsoletas ou descontinuadas."""
    print_header("buscando apis obsoletas")
    
    # Lista de APIs/classes/métodos obsoletos para verificar
    deprecated_patterns = [
        "@Deprecated",
        "deprecated(",
        "DeprecationWarning",
        "PendingDeprecationWarning",
        "# TODO: Remove",
        "# FIXME:",
        "# XXX:"
    ]
    
    deprecated_found = False
    report_file = REPORT_DIR / "deprecated_apis_report.txt"
    
    with open(report_file, "w", encoding="utf-8") as f:
        f.write("=== APIs e Código Obsoleto Encontrado ===\n\n")
        
        # Percorre todos os arquivos do projeto
        for root, _, files in os.walk(SOURCE_DIR):
            for file in files:
                if not file.endswith(('.kt', '.java', '.py', '.xml', '.gradle')):
                    continue
                    
                file_path = Path(root) / file
                try:
                    with open(file_path, 'r', encoding='utf-8') as src_file:
                        lines = src_file.readlines()
                        
                    for i, line in enumerate(lines, 1):
                        if any(pattern in line for pattern in deprecated_patterns):
                            f.write(f"Arquivo: {file_path.relative_to(PROJECT_ROOT)}:{i}\n")
                            f.write(f"Linha {i}: {line.strip()}\n\n")
                            deprecated_found = True
                            
                except Exception as e:
                    print(f"{Colors.WARNING}Erro ao processar {file_path}: {e}{Colors.ENDC}")
    
    if deprecated_found:
        print(f"{Colors.WARNING}Possíveis APIs obsoletas encontradas. Verifique o relatório em {report_file}{Colors.ENDC}")
    else:
        print(f"{Colors.OKGREEN}Nenhuma API obsoleta encontrada.{Colors.ENDC}")

def check_code_format() -> None:
    """Verifica a formatação do código usando black."""
    print_header("verificando formatação do código")
    
    # Verificação com black
    black_cmd = [
        sys.executable, "-m", "black",
        "--check",
        "--diff",
        "--exclude", "venv",
        str(SOURCE_DIR)
    ]
    exit_code, output = run_command(black_cmd)
    
    format_report = REPORT_DIR / "code_format_report.txt"
    with open(format_report, "w", encoding="utf-8") as f:
        f.write(output)
    
    if exit_code == 0:
        print(f"{Colors.OKGREEN}O código está formatado corretamente.{Colors.ENDC}")
    else:
        print(f"{Colors.WARNING}O código precisa de formatação. Execute 'black {SOURCE_DIR}' para corrigir.{Colors.ENDC}")
        print(f"Diferenças salvas em {format_report}")

def generate_summary_report():
    """Gera um relatório resumido com as principais descobertas."""
    summary = [
        "\n" + "="*80,
        "RELATORIO RESUMIDO DA ANALISE".center(80),
        "="*80
    ]
    
    reports = {
        "Qualidade do Codigo (pylint)": "pylint_report.txt",
        "Codigo Duplicado": "duplicate_code_report.txt",
        "Codigo Nao Utilizado": "unused_code_report.txt",
        "Problemas de Seguranca": "security_report.txt",
        "APIs Obsoletas": "deprecated_apis_report.txt",
        "Formatacao do Codigo": "code_format_report.txt"
    }
    
    for title, filename in reports.items():
        report_path = REPORT_DIR / filename
        if report_path.exists():
            try:
                with open(report_path, 'r', encoding='utf-8', errors='replace') as f:
                    content = f.read().strip()
                    if content:
                        status = "[!] Requer atencao"
                    else:
                        status = "[OK] Sem problemas"
                    summary.append(f"{title:30} {status}")
            except Exception as e:
                summary.append(f"{title:30} [ERRO] Falha ao ler o arquivo: {str(e)}")
    
    # Imprime o resumo com tratamento de codificacao
    try:
        print("\n".join(summary))
        print(f"\nRelatorios completos disponiveis em: {REPORT_DIR}")
    except UnicodeEncodeError:
        # Se ainda houver erro de codificacao, remove caracteres especiais
        safe_summary = [s.encode('ascii', 'replace').decode('ascii') for s in summary]
        print("\n".join(safe_summary))
        print(f"\nRelatorios completos disponiveis em: {str(REPORT_DIR).encode('ascii', 'replace').decode('ascii')}")

def main() -> None:
    """Função principal que executa todas as análises."""
    if not check_python_environment():
        print(f"{Colors.FAIL}Por favor, instale as dependências necessárias e execute novamente.{Colors.ENDC}")
        return
    
    analyze_code_quality()
    find_duplicate_code()
    find_unused_code()
    check_security_issues()
    find_deprecated_apis()
    check_code_format()
    generate_summary_report()
    
    print("\n" + "="*80)
    print(f"{Colors.HEADER}Análise concluída com sucesso!{Colors.ENDC}")
    print("="*80)

if __name__ == "__main__":
    main()
