"""Build automatizado da aplicacao Python empacotada dentro do JAR."""

from __future__ import annotations

import json
import os
import re
import shutil
import subprocess
import sys
import time
import zipfile
from pathlib import Path


ROOT = Path(__file__).resolve().parent
CONFIG_PATH = ROOT / "config.json"
PYTHON_DIR = ROOT / "python"
BUILD_DIR = ROOT / "build"
DIST_DIR = ROOT / "dist"
RELEASE_DIR = ROOT / "release"
RESOURCES_DIR = ROOT / "src" / "main" / "resources"
VENV_DIR = ROOT / ".venv"


def fail(message: str) -> None:
    print(f"[ERRO] {message}", file=sys.stderr)
    raise SystemExit(1)


def run(command: list[str], label: str, *, cwd: Path = ROOT, timeout: int | None = None) -> subprocess.CompletedProcess[str]:
    print(f"\n[EXEC] {label}\n      {' '.join(command)}")
    try:
        result = subprocess.run(command, cwd=cwd, text=True, timeout=timeout)
    except FileNotFoundError as error:
        fail(f"Comando nao encontrado: {error.filename}")
    except subprocess.TimeoutExpired:
        fail(f"Tempo limite excedido durante: {label}")
    if result.returncode != 0:
        fail(f"{label} falhou. Codigo de retorno: {result.returncode}")
    return result


def command_version(command: list[str]) -> str:
    try:
        result = subprocess.run(command, cwd=ROOT, capture_output=True, text=True, timeout=15)
    except (FileNotFoundError, subprocess.TimeoutExpired):
        return "indisponivel"
    output = (result.stdout or result.stderr).strip().splitlines()
    return output[0] if output else "versao desconhecida"


def load_config() -> dict:
    try:
        with CONFIG_PATH.open(encoding="utf-8") as stream:
            config = json.load(stream)
    except FileNotFoundError:
        fail(f"Configuracao nao encontrada: {CONFIG_PATH}")
    except json.JSONDecodeError as error:
        fail(f"config.json invalido na linha {error.lineno}: {error.msg}")

    for key in ("application", "version", "python", "pyinstaller", "test"):
        if key not in config:
            fail(f"Configuracao obrigatoria ausente: {key}")
    for key in ("entrypoint", "requirements"):
        if key not in config["python"]:
            fail(f"Configuracao obrigatoria ausente: python.{key}")
    for key in ("name", "mode", "data", "hidden_imports", "collect_data", "required_paths"):
        if key not in config["pyinstaller"]:
            fail(f"Configuracao obrigatoria ausente: pyinstaller.{key}")
    if config["pyinstaller"]["mode"] not in ("onedir", "onefile"):
        fail("pyinstaller.mode deve ser 'onedir' ou 'onefile'")
    return config


def find_command(name: str) -> str:
    path = shutil.which(name)
    if not path:
        fail(f"{name} nao encontrado no PATH. Instale-o e tente novamente.")
    return path


def validate_environment() -> tuple[str, str, str]:
    print("\n[1/9] Verificando ambiente...")
    python = find_command("python")
    pip = find_command("pip")
    java = find_command("java")
    maven = find_command("mvn")
    python_version = command_version([python, "--version"])
    pip_version = command_version([pip, "--version"])
    java_version = command_version([java, "-version"])
    maven_version = command_version([maven, "-version"])
    print(f"[OK] Python: {python_version}")
    print(f"[OK] pip: {pip_version}")
    print(f"[OK] Java: {java_version}")
    print(f"[OK] Maven: {maven_version}")
    version_match = re.search(r'version "(?:1\.)?(\d+)', java_version)
    java_major = int(version_match.group(1)) if version_match else 0
    if java_major < 17:
        fail(f"Java 17 ou superior e necessario. Encontrado: {java_version}")
    return python, pip, maven


def python_tool() -> Path:
    scripts = VENV_DIR / ("Scripts" if os.name == "nt" else "bin")
    return scripts / ("python.exe" if os.name == "nt" else "python")


def prepare_venv(python: str) -> Path:
    print("\n[2/9] Criando ambiente Python...")
    venv_python = python_tool()
    if not venv_python.exists():
        run([python, "-m", "venv", str(VENV_DIR)], "Criacao do ambiente virtual")
    run([str(venv_python), "-m", "pip", "install", "--upgrade", "pip"], "Atualizacao do pip")
    print("[OK] Ambiente Python pronto")
    return venv_python


def install_requirements(venv_python: Path, config: dict) -> None:
    print("\n[3/9] Instalando dependencias...")
    requirements = PYTHON_DIR / config["python"]["requirements"]
    if not requirements.is_file():
        fail(f"requirements nao encontrado: {requirements}")

    raw_requirements = requirements.read_bytes()
    if raw_requirements.strip():
        normalized_requirements = requirements
        for encoding in ("utf-8-sig", "utf-16"):
            try:
                requirements_text = raw_requirements.decode(encoding)
                break
            except UnicodeDecodeError:
                continue
        else:
            fail(f"requirements nao esta em UTF-8 ou UTF-16: {requirements}")

        if encoding != "utf-8-sig":
            normalized_requirements = BUILD_DIR / "requirements-normalized.txt"
            normalized_requirements.write_text(requirements_text, encoding="utf-8")

        run([str(venv_python), "-m", "pip", "install", "-r", str(normalized_requirements)], "Instalacao das dependencias")
    run([str(venv_python), "-m", "pip", "install", "pyinstaller"], "Instalacao do PyInstaller")
    print("[OK] Dependencias prontas")


def build_executable(venv_python: Path, config: dict) -> Path:
    print("\n[4/9] Executando PyInstaller...")
    py_config = config["pyinstaller"]
    entrypoint = PYTHON_DIR / config["python"]["entrypoint"]
    if not entrypoint.is_file():
        fail(f"Entrypoint Python nao encontrado: {entrypoint}")
    command = [str(venv_python), "-m", "PyInstaller", "--noconfirm", "--clean", f"--{py_config['mode']}", "--name", py_config["name"], "--distpath", str(DIST_DIR), "--workpath", str(BUILD_DIR / "pyinstaller"), "--specpath", str(BUILD_DIR / "pyinstaller")]
    if py_config.get("windowed"):
        command.append("--windowed")
    for item in py_config.get("data", []):
        if not isinstance(item, str) or ";" not in item:
            fail("Cada item de pyinstaller.data deve usar o formato 'origem;destino'")
        source, destination = item.split(";", 1)
        source_path = PYTHON_DIR / source
        if not source_path.exists():
            fail(f"Arquivo ou diretorio de dados nao encontrado: {source_path}")
        command.extend(["--add-data", f"{source_path};{destination}"])
    for item in py_config.get("hidden_imports", []):
        command.extend(["--hidden-import", item])
    for item in py_config.get("collect_data", []):
        command.extend(["--collect-data", item])
    command.append(str(entrypoint))
    run(command, "PyInstaller")
    if py_config["mode"] == "onefile":
        executable = DIST_DIR / f"{py_config['name']}.exe"
    else:
        executable = DIST_DIR / py_config["name"] / f"{py_config['name']}.exe"
    if not executable.is_file():
        fail(f"Executavel nao foi gerado: {executable}")
    for required in py_config.get("required_paths", []):
        required_path = executable.parent / required
        if not required_path.exists():
            fail(f"Arquivo ou diretorio obrigatorio nao encontrado: {required_path}")
    print(f"[OK] Executavel validado: {executable}")
    return executable


def test_executable(executable: Path, config: dict) -> None:
    print("\n[5/9] Validando executavel...")
    test = config["test"]
    if not test.get("enabled", False):
        print("[OK] Teste desabilitado por configuracao")
        return
    arguments = [str(item) for item in test.get("arguments", [])]
    timeout = int(test.get("timeoutSeconds", 30))
    try:
        result = subprocess.run([str(executable), *arguments], cwd=executable.parent, timeout=timeout)
    except subprocess.TimeoutExpired:
        fail(f"Teste do executavel excedeu {timeout} segundos")
    if result.returncode != 0:
        fail(f"Teste do executavel falhou. Codigo de retorno: {result.returncode}")
    print("[OK] Executavel iniciou e terminou corretamente")


def create_zip(executable: Path, config: dict) -> Path:
    print("\n[6/9] Criando ZIP...")
    application = config["application"]
    version = config["version"]
    zip_path = BUILD_DIR / f"{application}-{version}.zip"
    source = executable.parent
    with zipfile.ZipFile(zip_path, "w", zipfile.ZIP_DEFLATED) as archive:
        for path in source.rglob("*"):
            if path.is_file() and not any(part in {"__pycache__", ".pytest_cache", ".git", ".venv"} for part in path.parts):
                archive.write(path, path.relative_to(source))
    print(f"[OK] ZIP criado: {zip_path}")
    return zip_path


def copy_resources(zip_path: Path, config: dict) -> tuple[Path, Path]:
    print("\n[7/9] Copiando ZIP para resources...")
    RESOURCES_DIR.mkdir(parents=True, exist_ok=True)
    application = config["application"]
    for old_zip in RESOURCES_DIR.glob(f"{application}-*.zip"):
        old_zip.unlink()
    target_zip = RESOURCES_DIR / zip_path.name
    shutil.copy2(zip_path, target_zip)
    metadata = RESOURCES_DIR / "python-app.properties"
    metadata.write_text(f"application={application}\nversion={config['version']}\nzip={zip_path.name}\nexecutable={config['pyinstaller']['name']}.exe\n", encoding="utf-8")
    print(f"[OK] Recursos atualizados: {target_zip}")
    return target_zip, metadata


def package_jar(maven: str, config: dict) -> Path:
    print("\n[8/9] Executando Maven...")
    run([maven, "clean", "package", f"-Drevision={config['version']}", "-DskipTests"], "Maven")
    expected = ROOT / "target" / f"{config['application']}-{config['version']}.jar"
    jars = list((ROOT / "target").glob("*.jar"))
    source = expected if expected.is_file() else next((jar for jar in jars if "original-" not in jar.name), None)
    if source is None:
        fail("Maven terminou, mas nenhum JAR foi encontrado em target/")
    RELEASE_DIR.mkdir(exist_ok=True)
    target = RELEASE_DIR / f"{config['application']}-{config['version']}.jar"
    shutil.copy2(source, target)
    print(f"[OK] JAR copiado para: {target}")
    return target


def clean_outputs() -> None:
    for directory in (BUILD_DIR, DIST_DIR, RELEASE_DIR):
        if directory.exists():
            shutil.rmtree(directory)
    BUILD_DIR.mkdir()
    print("[OK] Temporarios anteriores removidos")


def main() -> None:
    config = load_config()
    clean_outputs()
    python, _pip, maven = validate_environment()
    venv_python = prepare_venv(python)
    install_requirements(venv_python, config)
    executable = build_executable(venv_python, config)
    test_executable(executable, config)
    zip_path = create_zip(executable, config)
    copy_resources(zip_path, config)
    jar = package_jar(maven, config)
    print("\n[9/9] Validando JAR...")
    if not jar.is_file():
        fail(f"JAR final nao encontrado: {jar}")
    print(f"[OK] Arquivo final: {jar}")
    print(f"\nBUILD CONCLUIDO\nAplicacao: {config['application']}\nVersao: {config['version']}\nArquivo: {jar}")


if __name__ == "__main__":
    main()
