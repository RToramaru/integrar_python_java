# Integração Python + Java

## Build automático

Este projeto é um empacotador genérico. A aplicação Python fictícia em
`python/` serve somente para validar o fluxo; ao distribuir o software,
substitua esse conteúdo pelo projeto Python que deverá ser incluído no JAR.
Não é necessário alterar o código Java.

O comando abaixo executa todo o processo no Windows:

```powershell
.\build.bat
```

O `build.py` cria `.venv` na raiz do projeto, instala as dependências de
`python/requirements.txt` dentro desse ambiente e instala
PyInstaller nesse ambiente, gera o `.exe`, executa o teste configurado, cria
o ZIP, copia o ZIP e os metadados para `src/main/resources/`, executa Maven e
coloca o JAR em `release/`.

## Como fornecer sua aplicação Python

1. Remova ou substitua os arquivos de exemplo dentro de `python/`.
2. Coloque o seu entrypoint e o seu `requirements.txt` nessa pasta.
3. Edite `config.json` com os nomes da sua aplicação e versão.
4. Configure em `pyinstaller.data` os arquivos e diretórios extras que o seu
         `.exe` precisa. Os caminhos de origem são relativos a `python/` e usam o
         formato Windows `origem;destino`.
5. Use `hidden_imports` para imports que o PyInstaller não detectar e
         `collect_data` para dados fornecidos por pacotes Python.
6. Liste em `required_paths` os arquivos ou diretórios que devem existir na
         distribuição gerada.
7. Execute `build.bat` e use o arquivo gerado em `release/`.

Exemplo de configuração fictícia:

```json
{
        "application": "minha_aplicacao",
        "version": "2.0.0",
        "python": {
                "entrypoint": "app.py",
                "requirements": "requirements.txt"
        },
        "pyinstaller": {
                "name": "minha_aplicacao",
                "mode": "onedir",
                "windowed": false,
                "data": ["recursos;recursos", "config/settings.json;config"],
                "hidden_imports": ["modulo_exemplo"],
                "collect_data": ["pacote_exemplo"],
                "required_paths": ["recursos"]
        },
        "test": {
                "enabled": true,
                "arguments": ["--help"],
                "timeoutSeconds": 30
        }
}
```

Os itens de `data` precisam existir antes do build. O `build.py` valida esses
caminhos e interrompe o processo com uma mensagem clara quando algo falta.
Para `onedir`, o ZIP contém a pasta completa da distribuição, incluindo o
executável e seus arquivos internos. Para `onefile`, contém o executável único.

## Execução do JAR

O Java lê `python-app.properties`, gerado pelo build, para descobrir o ZIP e o
executável. Ele extrai a aplicação, encaminha todos os argumentos recebidos e
retorna o código de saída do Python:

```powershell
java -jar release/minha_aplicacao-2.0.0.jar processar --entrada "C:\dados\arquivo de exemplo.dat"
```

O computador de destino precisa apenas de Java 17 ou superior compatível. Python,
pip, PyInstaller e Maven são necessários somente na máquina que cria o JAR.

## Pré-requisitos do build

- Windows;
- JDK 17 ou superior configurado no `PATH`;
- Python configurado no `PATH`;
- pip disponível;
- Maven configurado no `PATH`.

O script não usa `shell=True`, aceita caminhos com espaços e não instala as
dependências no Python global.

## Limpeza e arquivos gerados

Antes de cada build, `build/`, `dist/` e `release/` são recriados. O projeto
original, `python/`, `src/`, `pom.xml` e `config.json` não são removidos.
O ZIP antigo da mesma aplicação e os metadados gerados em `resources/` são
substituídos automaticamente.

As classes `PythonAppManager` e `InterfaceSeparada` preservam a extração,
execução, encaminhamento de stdout/stderr e passagem literal dos argumentos.

---

## Documentação detalhada da integração

## 1. Objetivo

Este documento explica como utilizar o projeto Java responsável por:

1.  receber os argumentos informados na execução do `.jar`;
2.  extrair uma aplicação Python empacotada em um arquivo `.zip`;
3.  localizar o executável `.exe` da aplicação Python;
4.  repassar automaticamente todos os argumentos recebidos pelo JAR para
    o executável Python;
5.  iniciar e acompanhar o processo Python;
6.  encerrar o processo Python quando a aplicação Java for encerrada.

A ideia principal é que o Java **não precisa conhecer os argumentos
específicos da aplicação Python**.

Por exemplo, se a aplicação Python normalmente é executada assim:

``` powershell
python main.py rotular --video "C:\Exemplo\Videos\video_teste.mp4" --checkpoint "C:\Exemplo\Modelos\modelo_teste.pt" --saida "saida"
```

a aplicação Java poderá ser executada como:

``` powershell
java -jar deteccao.jar rotular --video "C:\Exemplo\Videos\video_teste.mp4" --checkpoint "C:\Exemplo\Modelos\modelo_teste.pt" --saida "saida"
```

O JAR receberá esses argumentos e os repassará ao `.exe`.

------------------------------------------------------------------------

# 2. Pré-requisitos

Antes de utilizar o projeto, instale apenas o necessário para executar a aplicação Java e preparar a aplicação Python.

## 2.1 Java 17

Este projeto deve ser executado com **Java 17** ou superior compatível com o projeto.

Verifique a instalação:

```powershell
java -version
```

Exemplo de saída esperada:

```text
java version "17.x.x"
```

Também é recomendável verificar o compilador:

```powershell
javac -version
```

Deve ser apresentada uma versão 17.

Se o projeto utilizar Maven para gerar o JAR, o JDK deve estar instalado, e não somente um JRE.

## 2.2 Maven

O Maven é utilizado para compilar o projeto Java e gerar o JAR.

Verifique:

```powershell
mvn -version
```

O Maven deve informar que está utilizando o Java 17.

Exemplo:

```text
Apache Maven 3.x.x
Java version: 17.x.x
```

## 2.3 Python

O Python é necessário para preparar e testar a aplicação Python antes de transformá-la em `.exe`.

Verifique:

```powershell
python --version
```

Exemplo:

```text
Python 3.x.x
```

## 2.4 PyInstaller

Para gerar o `.exe` da aplicação Python:

```powershell
pip install pyinstaller
```

Verifique:

```powershell
pyinstaller --version
```

> As demais bibliotecas Python dependem da aplicação Python que será empacotada. O `PythonAppManager` não exige bibliotecas Python específicas.

# 3. Obtendo o projeto

Se o projeto estiver hospedado em um repositório Git, clone-o com:

```powershell
git clone https://github.com/RToramaru/integrar_python_java.git
```

Entre no diretório:

```powershell
cd integrar_python_java
```

Se o projeto já estiver disponível localmente, esta etapa pode ser ignorada.

## 3.1 Verificar o Git

Antes do clone:

```powershell
git --version
```

Caso o comando não seja encontrado, instale o Git antes de continuar.

# 4. Verificando o projeto Java

Depois do clone, a estrutura deve ser semelhante a:

```text
integracao-python-java/
│
├── pom.xml
│
└── src/
    └── main/
        ├── java/
        │   └── com/
        │       └── exemplo/
        │           ├── InterfaceSeparada.java
        │           └── PythonAppManager.java
        │
        └── resources/
            └── monitoracao.zip
```

Antes de gerar o JAR, confirme:

```powershell
java -version
mvn -version
```

O Java utilizado pelo Maven deve ser o Java 17.

# 5. Java mínimo necessário

Para executar o JAR final, o computador de destino precisa ter uma JVM compatível com Java 17.

Verifique:

```powershell
java -version
```

Não é necessário instalar Python no computador de destino **se o ZIP já contiver o executável Python gerado pelo PyInstaller e todos os arquivos necessários para sua execução**.

Da mesma forma, não é necessário instalar PyInstaller no computador de destino.

O PyInstaller é utilizado durante a preparação da aplicação Python.

Assim, o fluxo é:

```text
Computador de desenvolvimento
│
├── Java 17
├── JDK 17
├── Maven
├── Python
├── PyInstaller
└── Git
```

Depois de gerar o JAR:

```text
Computador de destino
│
├── Java 17
└── aplicação.jar
```

O `.jar` contém o ZIP da aplicação Python dentro de seus recursos.

> A aplicação Python empacotada ainda pode exigir recursos específicos do sistema operacional ou do próprio aplicativo. Esses requisitos devem ser considerados caso a aplicação Python utilize componentes externos.

# 6. Estrutura geral

A solução possui três partes principais:

``` text
Projeto Java
│
├── src/
│   └── main/
│       ├── java/
│       │   └── com/exemplo/
│       │       ├── InterfaceSeparada.java
│       │       └── PythonAppManager.java
│       │
│       └── resources/
│           └── monitoracao.zip
│
└── pom.xml
```

O arquivo ZIP da aplicação Python fica dentro de:

``` text
src/main/resources/
```

O `PythonAppManager` localiza esse recurso dentro do JAR, extrai o ZIP
para um diretório temporário e executa o `.exe`.

O código atual utiliza um construtor que recebe os argumentos como
`String... argumentos` e posteriormente adiciona todos eles ao comando
do executável Python. Isso permite que `args` recebido pelo `main` seja
repassado diretamente ao executável.

------------------------------------------------------------------------

# 7. Aplicação Python antes de gerar o EXE

Antes de empacotar a aplicação Python, ela deve funcionar normalmente.

No exemplo utilizado neste projeto, o comando original é:

``` powershell
python main.py rotular --video "C:\Exemplo\Videos\video_teste.mp4" --checkpoint "C:\Exemplo\Modelos\modelo_teste.pt" --saida "saida"
```

Esse comando possui:

``` text
python
main.py
rotular
--video
"C:\Exemplo\Videos\video_teste.mp4"
--checkpoint
"C:\Exemplo\Modelos\modelo_teste.pt"
--saida
"saida"
```

O Java não precisa interpretar esses argumentos.

Ele apenas deverá repassá-los para o executável.

------------------------------------------------------------------------

# 8. O que significa cada argumento do comando Python

## `python`

É o interpretador Python utilizado para executar o programa.

``` powershell
python
```

Quando a aplicação for transformada em `.exe` usando PyInstaller, o
usuário final não precisará executar `python`.

------------------------------------------------------------------------

## `main.py`

É o arquivo principal da aplicação Python.

``` powershell
main.py
```

É o ponto de entrada da aplicação.

------------------------------------------------------------------------

## `rotular`

É um argumento posicional.

``` powershell
rotular
```

Nesse exemplo, indica a operação que deverá ser executada pela
aplicação.

O significado exato depende do código da aplicação Python.

------------------------------------------------------------------------

## `--video`

É uma opção/flag da aplicação:

``` powershell
--video
```

O argumento seguinte informa o vídeo que será utilizado:

``` powershell
"C:\Exemplo\Videos\video_teste.mp4"
```

Portanto:

``` powershell
--video "C:\Exemplo\Videos\video_teste.mp4"
```

significa que o arquivo de vídeo informado será utilizado pela
aplicação.

------------------------------------------------------------------------

## `--checkpoint`

É a opção utilizada para informar o modelo/peso:

``` powershell
--checkpoint "C:\Exemplo\Modelos\modelo_teste.pt"
```

Nesse exemplo:

``` text
--checkpoint
```

é a flag e:

``` text
C:\Exemplo\Modelos\modelo_teste.pt
```

é o valor passado para ela.

------------------------------------------------------------------------

## `--saida`

Define o local de saída:

``` powershell
--saida "saida"
```

Nesse exemplo, a aplicação utilizará o diretório `saida`.

------------------------------------------------------------------------

# 9. Alterando os argumentos

O objetivo é que os argumentos sejam informados **na execução do JAR**,
e não fixados no código Java.

Por exemplo:

``` powershell
java -jar deteccao.jar rotular --video "C:\video.mp4" --checkpoint "C:\modelos\mobile_sam.pt" --saida "resultado"
```

O Java recebe:

``` text
args[0] = rotular
args[1] = --video
args[2] = C:\video.mp4
args[3] = --checkpoint
args[4] = C:\modelos\mobile_sam.pt
args[5] = --saida
args[6] = resultado
```

Esses valores são enviados ao executável Python.

Portanto, para mudar o vídeo, checkpoint ou diretório de saída,
normalmente **não é necessário recompilar o JAR**.

Basta alterar o comando utilizado para executar o JAR.

------------------------------------------------------------------------

# 10. Alteração necessária no `InterfaceSeparada.java`

Não deixe os argumentos fixos no código.

Por exemplo, não utilize:

``` java
new PythonAppManager(
    InterfaceSeparada.class,
    nomeAplicacao,
    versao,
    nomeArquivoZip,
    nomeExecutavel,
    "--modo",
    "producao",
    "--porta",
    "8080"
);
```

Se a intenção é permitir argumentos dinâmicos, utilize o `args` recebido
pelo método `main`:

``` java
new PythonAppManager(
    InterfaceSeparada.class,
    nomeAplicacao,
    versao,
    nomeArquivoZip,
    nomeExecutavel,
    args
);
```

O `args` contém todos os argumentos informados na execução do JAR.

------------------------------------------------------------------------

# 11. Por que `args` funciona?

O método principal do Java possui:

``` java
public static void main(String[] args)
```

Quando o usuário executa:

``` powershell
java -jar deteccao.jar rotular --video "C:\video.mp4" --saida "saida"
```

o Java coloca os valores em `args`.

A aplicação então passa esse vetor para o `PythonAppManager`.

O `PythonAppManager` possui o construtor:

``` java
public PythonAppManager(
        Class<?> resourceClass,
        String nomeAplicacao,
        String versao,
        String nomeArquivoZip,
        String nomeExecutavel,
        String... argumentos
)
```

O `String... argumentos` permite receber vários argumentos.

Internamente, eles são convertidos para uma lista.

Depois, essa lista é adicionada ao comando do executável.

Assim, o executável recebe os mesmos argumentos fornecidos ao JAR.

------------------------------------------------------------------------

# 12. Gerando o executável Python

Depois que a aplicação Python estiver funcionando, ela deve ser
transformada em um `.exe`.

Uma opção utilizada neste tipo de projeto é o PyInstaller.

Exemplo:

``` powershell
pyinstaller --noconfirm --onedir --windowed main.py
```

O comando exato pode variar conforme a aplicação Python e suas
dependências.

Se a aplicação possui arquivos externos, modelos, configurações ou
outros recursos, eles precisam ser incluídos no empacotamento.

------------------------------------------------------------------------

# 13. O que é `--add-data`

A opção:

``` powershell
--add-data
```

é utilizada para adicionar ao executável/à distribuição arquivos ou
diretórios que **não são código Python** e que a aplicação precisa em
tempo de execução.

Exemplos de arquivos que podem precisar ser adicionados:

-   modelos `.pt`;
-   modelos `.onnx`;
-   arquivos `.xml`;
-   arquivos `.bin`;
-   arquivos `.json`;
-   imagens;
-   arquivos de configuração;
-   diretórios com recursos;
-   outros arquivos necessários para execução.

Exemplo:

``` powershell
--add-data "pesos;pesos"
```

No Windows, o formato utilizado pelo PyInstaller é:

``` text
origem;destino
```

Portanto:

``` powershell
--add-data "pesos;pesos"
```

significa:

``` text
origem:
pesos

destino:
pesos
```

Ou seja, o conteúdo do diretório `pesos` será incluído na distribuição
dentro de um diretório chamado `pesos`.

## Importante

`--add-data` é para **dados/recursos**, e não para adicionar código
Python que deveria ser importado normalmente.

Exemplo de recurso:

``` text
pesos/
├── mobile_sam.pt
├── modelo.onnx
└── configuracao.json
```

Esse tipo de conteúdo pode ser incluído com `--add-data`.

------------------------------------------------------------------------

# 14. Exemplo de estrutura da aplicação Python

Uma aplicação pode possuir uma estrutura semelhante a:

``` text
deteccao/
│
├── main.py
├── modulos/
│   ├── deteccao.py
│   └── processamento.py
│
├── pesos/
│   └── mobile_sam.pt
│
└── outros_recursos/
```

O código Python deve importar normalmente seus módulos.

Os arquivos que são dados/recursos devem ser incluídos conforme
necessário.

Por exemplo:

``` powershell
pyinstaller --noconfirm --onedir --windowed --add-data "pesos;pesos" main.py
```

Depois da compilação, o PyInstaller normalmente produzirá:

``` text
dist/
└── main/
    ├── main.exe
    ├── pesos/
    │   └── mobile_sam.pt
    └── demais arquivos e DLLs
```

------------------------------------------------------------------------

# 15. Teste do EXE antes de colocar no Java

Antes de criar o ZIP e colocar a aplicação no JAR, teste o `.exe`
diretamente.

Por exemplo:

``` powershell
.\main.exe rotular --video "C:\Exemplo\Videos\video_teste.mp4" --checkpoint "C:\Exemplo\Modelos\modelo_teste.pt" --saida "saida"
```

Se o programa não funcionar diretamente como `.exe`, não adianta
colocá-lo dentro do Java.

Primeiro faça o executável funcionar sozinho.

------------------------------------------------------------------------

# 16. Estrutura que deve ser colocada no ZIP

Depois de gerar o executável Python, deve-se compactar **a pasta
completa da aplicação**, incluindo todos os arquivos necessários para
execução.

Por exemplo:

``` text
monitoracao/
│
├── monitoracao.exe
├── _internal/
│   ├── ...
│   └── ...
│
├── pesos/
│   ├── modelo.pt
│   └── modelo.onnx
│
└── demais arquivos necessários
```

O ZIP deve conter o executável e os arquivos necessários.

Exemplo:

``` text
monitoracao.zip
```

------------------------------------------------------------------------

# 17. Cuidado com a estrutura interna do ZIP

O `PythonAppManager` procura o executável utilizando:

``` java
diretorio.resolve(nomeExecutavel)
```

Portanto, com:

``` java
String nomeExecutavel = "monitoracao.exe";
```

o executável precisa estar diretamente no diretório extraído:

``` text
monitoracao.exe
```

e não:

``` text
monitoracao/
    monitoracao.exe
```

se o ZIP for extraído diretamente para o diretório da aplicação.

## Estrutura recomendada

``` text
monitoracao.zip
│
├── monitoracao.exe
├── _internal/
├── pesos/
└── demais arquivos
```

Assim, depois da extração:

``` text
%TEMP%\MinhaEmpresa\PythonApps\monitoracao\1.0.0\
│
├── monitoracao.exe
├── _internal/
├── pesos/
└── demais arquivos
```

O Java conseguirá localizar:

``` text
...\monitoracao.exe
```

------------------------------------------------------------------------

# 18. Onde colocar o ZIP no projeto Java

O arquivo:

``` text
monitoracao.zip
```

deve ficar em:

``` text
src/main/resources/
```

Exemplo:

``` text
projeto-java/
│
├── pom.xml
│
└── src/
    └── main/
        ├── java/
        │   └── com/
        │       └── exemplo/
        │           ├── InterfaceSeparada.java
        │           └── PythonAppManager.java
        │
        └── resources/
            └── monitoracao.zip
```

Isso é importante porque o `PythonAppManager` procura o ZIP como recurso
do classpath.

O código utiliza:

``` java
resourceClass
    .getClassLoader()
    .getResourceAsStream(nomeArquivoZip)
```

Portanto, o arquivo deve estar disponível nos recursos do JAR.

------------------------------------------------------------------------

# 19. Configuração no `InterfaceSeparada.java`

Devem ser configurados os dados da aplicação Python:

``` java
String nomeAplicacao =
        "monitoracao";

String versao =
        "1.0.0";

String nomeArquivoZip =
        "monitoracao.zip";

String nomeExecutavel =
        "monitoracao.exe";
```

## `nomeAplicacao`

É o nome lógico da aplicação.

Exemplo:

``` java
String nomeAplicacao = "monitoracao";
```

Também é utilizado para definir o diretório de extração.

------------------------------------------------------------------------

## `versao`

Identifica a versão da aplicação empacotada.

Exemplo:

``` java
String versao = "1.0.0";
```

Se uma nova versão for distribuída, pode-se alterar para:

``` java
String versao = "1.1.0";
```

------------------------------------------------------------------------

## `nomeArquivoZip`

É o nome do ZIP localizado dentro de:

``` text
src/main/resources/
```

Exemplo:

``` java
String nomeArquivoZip = "monitoracao.zip";
```

O nome deve corresponder exatamente ao arquivo existente em `resources`.

------------------------------------------------------------------------

## `nomeExecutavel`

É o nome do `.exe` que existe dentro do ZIP.

Exemplo:

``` java
String nomeExecutavel = "monitoracao.exe";
```

Esse nome também precisa corresponder exatamente ao arquivo existente no
ZIP.

------------------------------------------------------------------------

# 20. Não colocar argumentos fixos

Para permitir que o usuário forneça qualquer argumento:

``` java
monitoracao =
        new PythonAppManager(
                InterfaceSeparada.class,
                nomeAplicacao,
                versao,
                nomeArquivoZip,
                nomeExecutavel,
                args
        );
```

O ponto mais importante é:

``` java
args
```

Não é necessário escrever:

``` java
"--video",
"C:\\video.mp4",
"--checkpoint",
"C:\\modelo.pt"
```

dentro do Java.

Esses argumentos serão fornecidos na execução.

------------------------------------------------------------------------

# 21. Criando o JAR com Maven

Depois de colocar:

``` text
monitoracao.zip
```

em:

``` text
src/main/resources/
```

e configurar o código Java, execute o Maven na raiz do projeto:

``` powershell
mvn clean package
```

Isso irá:

1.  limpar a compilação anterior;
2.  compilar o código Java;
3.  copiar os recursos;
4.  gerar o JAR.

O JAR será criado normalmente dentro de:

``` text
target/
```

Por exemplo:

``` text
target/
└── projeto-java-1.0-SNAPSHOT.jar
```

O nome exato depende do `pom.xml`.

------------------------------------------------------------------------

# 22. Executando o JAR

Depois de gerar o JAR:

``` powershell
java -jar target\projeto-java-1.0-SNAPSHOT.jar
```

Para passar argumentos:

``` powershell
java -jar target\projeto-java-1.0-SNAPSHOT.jar rotular --video "C:\Exemplo\Videos\video_teste.mp4" --checkpoint "C:\Exemplo\Modelos\modelo_teste.pt" --saida "saida"
```

O Java receberá todos esses argumentos e os repassará ao `.exe`.

------------------------------------------------------------------------

# 23. Exemplo completo do fluxo

## Passo 1 --- Testar Python

``` powershell
python main.py rotular --video "C:\Exemplo\Videos\video_teste.mp4" --checkpoint "C:\Exemplo\Modelos\modelo_teste.pt" --saida "saida"
```

------------------------------------------------------------------------

## Passo 2 --- Gerar o EXE

Exemplo:

``` powershell
pyinstaller --noconfirm --onedir --windowed --add-data "pesos;pesos" main.py
```

Ajuste o comando conforme os arquivos e dependências da aplicação.

------------------------------------------------------------------------

## Passo 3 --- Testar o EXE

``` powershell
.\dist\main\main.exe rotular --video "C:\Exemplo\Videos\video_teste.mp4" --checkpoint "C:\Exemplo\Modelos\modelo_teste.pt" --saida "saida"
```

Confirme que funciona.

------------------------------------------------------------------------

## Passo 4 --- Renomear o EXE, se necessário

Se o Java estiver configurado com:

``` java
String nomeExecutavel = "monitoracao.exe";
```

o arquivo precisa ter esse nome:

``` text
monitoracao.exe
```

------------------------------------------------------------------------

## Passo 5 --- Montar a pasta para ZIP

Estruture a distribuição:

``` text
monitoracao/
├── monitoracao.exe
├── _internal/
├── pesos/
└── demais arquivos necessários
```

------------------------------------------------------------------------

## Passo 6 --- Criar o ZIP

O conteúdo da distribuição deve ser compactado de modo que o executável
fique na raiz do ZIP:

``` text
monitoracao.zip
├── monitoracao.exe
├── _internal/
├── pesos/
└── ...
```

------------------------------------------------------------------------

## Passo 7 --- Colocar o ZIP no Java

Copie:

``` text
monitoracao.zip
```

para:

``` text
src/main/resources/
```

------------------------------------------------------------------------

## Passo 8 --- Configurar o Java

No `InterfaceSeparada.java`:

``` java
String nomeAplicacao = "monitoracao";
String versao = "1.0.0";
String nomeArquivoZip = "monitoracao.zip";
String nomeExecutavel = "monitoracao.exe";
```

E:

``` java
monitoracao =
        new PythonAppManager(
                InterfaceSeparada.class,
                nomeAplicacao,
                versao,
                nomeArquivoZip,
                nomeExecutavel,
                args
        );
```

------------------------------------------------------------------------

## Passo 9 --- Criar o JAR

Na raiz do projeto:

``` powershell
mvn clean package
```

------------------------------------------------------------------------

## Passo 10 --- Executar

``` powershell
java -jar target\projeto-java-1.0-SNAPSHOT.jar rotular --video "C:\Exemplo\Videos\video_teste.mp4" --checkpoint "C:\Exemplo\Modelos\modelo_teste.pt" --saida "saida"
```

------------------------------------------------------------------------

# 24. Como trocar os argumentos

Depois que o JAR estiver pronto, você pode trocar os argumentos sem
modificar o código Java.

Por exemplo:

``` powershell
java -jar deteccao.jar rotular --video "C:\Videos\video1.mp4" --checkpoint "C:\Exemplo\Modelos\modelo1.pt" --saida "resultado1"
```

Depois:

``` powershell
java -jar deteccao.jar rotular --video "C:\Videos\video2.mp4" --checkpoint "C:\Exemplo\Modelos\modelo2.pt" --saida "resultado2"
```

O mesmo JAR pode ser utilizado.

------------------------------------------------------------------------

# 25. O que precisa ser alterado quando trocar a aplicação Python

Se for substituir `monitoracao` por outra aplicação, altere
principalmente:

``` java
String nomeAplicacao = "nova_aplicacao";
String versao = "1.0.0";
String nomeArquivoZip = "nova_aplicacao.zip";
String nomeExecutavel = "nova_aplicacao.exe";
```

Depois:

``` text
src/main/resources/
└── nova_aplicacao.zip
```

O ZIP precisa conter:

``` text
nova_aplicacao.exe
```

na raiz, além dos demais arquivos necessários.

Os argumentos continuam sendo recebidos por:

``` java
args
```

e não precisam ser conhecidos pelo Java.

------------------------------------------------------------------------

# 26. Resumo da estrutura final

``` text
projeto-java/
│
├── pom.xml
│
├── src/
│   └── main/
│       ├── java/
│       │   └── com/
│       │       └── exemplo/
│       │           ├── InterfaceSeparada.java
│       │           └── PythonAppManager.java
│       │
│       └── resources/
│           └── monitoracao.zip
│
└── target/
    └── projeto-java-1.0-SNAPSHOT.jar
```

Dentro do ZIP:

``` text
monitoracao.zip
│
├── monitoracao.exe
├── _internal/
├── pesos/
└── demais arquivos necessários
```

------------------------------------------------------------------------

# 27. Comandos principais

### Testar Python

``` powershell
python main.py rotular --video "C:\Exemplo\Videos\video_teste.mp4" --checkpoint "C:\Exemplo\Modelos\modelo_teste.pt" --saida "saida"
```

### Gerar EXE com PyInstaller

Exemplo:

``` powershell
pyinstaller --noconfirm --onedir --windowed --add-data "pesos;pesos" main.py
```

### Criar JAR

``` powershell
mvn clean package
```

### Executar JAR sem argumentos

``` powershell
java -jar target\projeto-java-1.0-SNAPSHOT.jar
```

### Executar JAR com argumentos

``` powershell
java -jar target\projeto-java-1.0-SNAPSHOT.jar rotular --video "C:\Exemplo\Videos\video_teste.mp4" --checkpoint "C:\Exemplo\Modelos\modelo_teste.pt" --saida "saida"
```

------------------------------------------------------------------------

# 28. Regra principal

A arquitetura deve seguir esta regra:

``` text
Argumentos do usuário
        ↓
      JAR
        ↓
      args[]
        ↓
PythonAppManager
        ↓
     .exe
```

O Java é responsável pela infraestrutura da aplicação Python:

-   localizar o ZIP;
-   extrair a aplicação;
-   localizar o EXE;
-   iniciar o processo;
-   repassar os argumentos;
-   acompanhar o processo;
-   finalizar o processo.

A aplicação Python é responsável pela interpretação dos argumentos,
como:

``` text
rotular
--video
--checkpoint
--saida
```

Isso mantém o `PythonAppManager` genérico e permite reutilizar o mesmo
mecanismo para diferentes aplicações Python.
