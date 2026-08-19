package com.exemplo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class PythonAppManager {

    // ==============================================================
    // CONFIGURAÇÕES GERAIS
    // ==============================================================

    /**
     * Diretório base onde as aplicações Python serão extraídas.
     *
     * Exemplo:
     * C:\Users\Usuario\AppData\Local\Temp\MinhaEmpresa\PythonApps
     */
    private static final String DIRETORIO_BASE = "MinhaEmpresa/PythonApps";


    /**
     * Ocultar diretório no Windows.
     */
    private static final boolean OCULTAR_DIRETORIO = true;


    /**
     * Redirecionar stderr para stdout.
     */
    private static final boolean REDIRECIONAR_ERRO = true;


    /**
     * Encoding utilizado para ler a saída do EXE.
     */
    private static final java.nio.charset.Charset ENCODING =
            StandardCharsets.UTF_8;


    /**
     * Tamanho do buffer utilizado durante a extração.
     */
    private static final int BUFFER_SIZE =
            1024 * 1024;


    /**
     * Tempo máximo, em segundos, para aguardar
     * o encerramento normal do processo.
     */
    private static final long TEMPO_ESPERA_STOP =
            5;


    /**
     * Tempo máximo, em segundos, após destroyForcibly().
     */
    private static final long TEMPO_ESPERA_FORCADO =
            2;


    // ==============================================================
    // CONFIGURAÇÃO DA APLICAÇÃO
    // ==============================================================

    private final Class<?> resourceClass;

    /**
     * Nome lógico da aplicação.
     *
     * Exemplo:
     * monitoracao
     */
    private final String nomeAplicacao;


    /**
     * Versão da aplicação.
     *
     * Exemplo:
     * 1.0.0
     */
    private final String versao;


    /**
     * Nome do arquivo ZIP dentro do JAR.
     *
     * Exemplo:
     * monitoracao.zip
     *
     * Pode ser diferente do nomeAplicacao.
     */
    private final String nomeArquivoZip;


    /**
     * Nome do executável dentro do ZIP.
     *
     * Exemplo:
     * monitoracao.exe
     *
     * Também pode ser diferente do nomeAplicacao.
     */
    private final String nomeExecutavel;


    /**
     * Argumentos passados para o executável.
     *
     * Exemplo:
     *
     * --porta 8080
     *
     * ou
     *
     * --config config.json
     */
    private final List<String> argumentos;


    /**
     * Processo atualmente executando.
     */
    private Process process;


    // ==============================================================
    // CONSTRUTOR
    // ==============================================================

    public PythonAppManager(
            Class<?> resourceClass,
            String nomeAplicacao,
            String versao,
            String nomeArquivoZip,
            String nomeExecutavel,
            String... argumentos
    ) {

        this.resourceClass = resourceClass;

        this.nomeAplicacao = nomeAplicacao;

        this.versao = versao;

        this.nomeArquivoZip = nomeArquivoZip;

        this.nomeExecutavel = nomeExecutavel;

        this.argumentos = new ArrayList<>(
                Arrays.asList(argumentos)
        );
    }


    // ==============================================================
    // START
    // ==============================================================

    public synchronized void start()
            throws IOException {

        if (estaExecutando()) {

            System.out.println(
                    nomeAplicacao
                            + " já está executando."
            );

            return;
        }


        long inicio =
                System.currentTimeMillis();


        Path diretorio =
                obterDiretorioTemporario();


        System.out.println(
                "======================================"
        );

        System.out.println(
                "Aplicação Python"
        );

        System.out.println(
                "Nome: "
                        + nomeAplicacao
        );

        System.out.println(
                "Versão: "
                        + versao
        );

        System.out.println(
                "ZIP: "
                        + nomeArquivoZip
        );

        System.out.println(
                "Executável: "
                        + nomeExecutavel
        );

        System.out.println(
                "Diretório:"
        );

        System.out.println(
                diretorio
        );

        System.out.println(
                "======================================"
        );


        // ==========================================================
        // EXTRAÇÃO
        // ==========================================================

        extrairSeNecessario(
                diretorio
        );


        long fimExtracao =
                System.currentTimeMillis();


        System.out.println(
                "Preparação concluída em "
                        + (fimExtracao - inicio)
                        + " ms"
        );


        // ==========================================================
        // EXECUTÁVEL
        // ==========================================================

        Path executavel =
                diretorio.resolve(
                        nomeExecutavel
                );


        if (!Files.exists(executavel)) {

            throw new IOException(
                    "Executável não encontrado:\n"
                            + executavel
            );
        }


        // ==========================================================
        // COMANDO
        // ==========================================================

        List<String> comando =
                new ArrayList<>();


        comando.add(
                executavel
                        .toAbsolutePath()
                        .toString()
        );


        comando.addAll(
                argumentos
        );


        System.out.println(
                "Comando:"
        );

        System.out.println(
                comando
        );


        // ==========================================================
        // PROCESS BUILDER
        // ==========================================================

        ProcessBuilder builder =
                new ProcessBuilder(
                        comando
                );


        // O EXE será executado tendo
        // o próprio diretório como working directory.
        builder.directory(
                diretorio.toFile()
        );


        if (REDIRECIONAR_ERRO) {

            builder.redirectErrorStream(
                    true
            );
        }


        System.out.println(
                "Iniciando "
                        + nomeExecutavel
                        + "..."
        );


        process =
                builder.start();


        System.out.println(
                nomeAplicacao
                        + " iniciado."
        );


        System.out.println(
                "PID: "
                        + process.pid()
        );


        iniciarLeitorDeSaida();
    }


    // ==============================================================
    // TEMP
    // ==============================================================

    private Path obterDiretorioTemporario()
            throws IOException {

        Path temp =
                Paths.get(
                        System.getProperty(
                                "java.io.tmpdir"
                        )
                );


        Path diretorio =
                temp
                        .resolve(
                                DIRETORIO_BASE
                        )
                        .resolve(
                                nomeAplicacao
                        )
                        .resolve(
                                versao
                        );


        Files.createDirectories(
                diretorio
        );


        ocultarWindows(
                diretorio
        );


        return diretorio;
    }


    // ==============================================================
    // EXTRAÇÃO
    // ==============================================================

    private void extrairSeNecessario(
            Path destino
    ) throws IOException {

        Path executavel =
                destino.resolve(
                        nomeExecutavel
                );


        // ==========================================================
        // JÁ EXISTE
        // ==========================================================

        if (Files.exists(executavel)) {

            System.out.println(
                    "Aplicação Python já está extraída."
            );

            return;
        }


        System.out.println(
                "Aplicação Python não encontrada."
        );

        System.out.println(
                "Extraindo ZIP..."
        );


        // ==========================================================
        // ZIP TEMPORÁRIO
        // ==========================================================

        Path zipTemporario =
                Files.createTempFile(
                        "python_app_",
                        ".zip"
                );


        try {

            // ======================================================
            // LOCALIZA RECURSO
            // ======================================================

            try (
                    InputStream input =
                            resourceClass
                                    .getClassLoader()
                                    .getResourceAsStream(
                                            nomeArquivoZip
                                    )
            ) {

                if (input == null) {

                    throw new IOException(
                            "Recurso não encontrado no JAR:\n"
                                    + nomeArquivoZip
                    );
                }


                Files.copy(
                        input,
                        zipTemporario,
                        StandardCopyOption
                                .REPLACE_EXISTING
                );
            }


            // ======================================================
            // EXTRAI
            // ======================================================

            extrairZip(
                    zipTemporario,
                    destino
            );


        } finally {

            Files.deleteIfExists(
                    zipTemporario
            );
        }


        // ==========================================================
        // VERIFICAÇÃO
        // ==========================================================

        if (!Files.exists(executavel)) {

            throw new IOException(
                    "A extração terminou, "
                            + "mas o executável não foi encontrado:\n"
                            + executavel
            );
        }


        ocultarWindows(
                destino
        );


        System.out.println(
                "Aplicação Python extraída."
        );
    }


    // ==============================================================
    // ZIP
    // ==============================================================

    private void extrairZip(
            Path zip,
            Path destino
    ) throws IOException {

        try (
                ZipFile zipFile =
                        new ZipFile(
                                zip.toFile()
                        )
        ) {

            Enumeration<? extends ZipEntry> entries =
                    zipFile.entries();


            while (
                    entries.hasMoreElements()
            ) {

                ZipEntry entry =
                        entries.nextElement();


                Path arquivo =
                        destino
                                .resolve(
                                        entry.getName()
                                )
                                .normalize();


                // ==================================================
                // ZIP SLIP
                // ==================================================

                if (
                        !arquivo.startsWith(
                                destino.normalize()
                        )
                ) {

                    throw new IOException(
                            "Entrada ZIP inválida: "
                                    + entry.getName()
                    );
                }


                // ==================================================
                // DIRETÓRIO
                // ==================================================

                if (entry.isDirectory()) {

                    Files.createDirectories(
                            arquivo
                    );

                    continue;
                }


                Path parent =
                        arquivo.getParent();


                if (parent != null) {

                    Files.createDirectories(
                            parent
                    );
                }


                // ==================================================
                // ARQUIVO
                // ==================================================

                try (
                        InputStream input =
                                zipFile.getInputStream(
                                        entry
                                );

                        OutputStream output =
                                Files.newOutputStream(
                                        arquivo
                                )
                ) {

                    byte[] buffer =
                            new byte[
                                    BUFFER_SIZE
                                    ];


                    int lidos;


                    while (
                            (lidos =
                                    input.read(
                                            buffer
                                    ))
                                    != -1
                    ) {

                        output.write(
                                buffer,
                                0,
                                lidos
                        );
                    }
                }
            }
        }
    }


    // ==============================================================
    // SAÍDA
    // ==============================================================

    private void iniciarLeitorDeSaida() {

        Thread thread =
                new Thread(() -> {

                    try {

                        BufferedReader reader =
                                new BufferedReader(
                                        new InputStreamReader(
                                                process.getInputStream(),
                                                ENCODING
                                        )
                                );


                        String linha;


                        while (
                                (linha =
                                        reader.readLine())
                                        != null
                        ) {

                            System.out.println(
                                    "["
                                            + nomeAplicacao
                                            + "] "
                                            + linha
                            );
                        }


                    } catch (IOException e) {

                        if (estaExecutando()) {

                            System.err.println(
                                    "Erro lendo saída."
                            );

                            e.printStackTrace();
                        }
                    }

                });


        thread.setName(
                "Python-"
                        + nomeAplicacao
        );


        thread.setDaemon(true);


        thread.start();
    }


    // ==============================================================
    // STATUS
    // ==============================================================

    public synchronized boolean estaExecutando() {

        return process != null
                &&
                process.isAlive();
    }


    // ==============================================================
    // PID
    // ==============================================================

    public synchronized long getPid() {

        if (process == null) {

            return -1;
        }

        return process.pid();
    }


        public synchronized int getExitCode() {

                if (process == null || process.isAlive()) {
                        return -1;
                }

                return process.exitValue();
        }


    // ==============================================================
    // STOP
    // ==============================================================

    public synchronized void stop() {

        if (process == null) {

            return;
        }


        if (!process.isAlive()) {

            process = null;

            return;
        }


        System.out.println(
                "Finalizando "
                        + nomeAplicacao
                        + "..."
        );


        process.destroy();


        try {

            if (
                    !process.waitFor(
                            TEMPO_ESPERA_STOP,
                            TimeUnit.SECONDS
                    )
            ) {

                System.out.println(
                        "Processo não encerrou "
                                + "normalmente."
                );


                process.destroyForcibly();


                process.waitFor(
                        TEMPO_ESPERA_FORCADO,
                        TimeUnit.SECONDS
                );
            }


        } catch (InterruptedException e) {

            Thread.currentThread()
                    .interrupt();


            process.destroyForcibly();
        }


        process = null;


        System.out.println(
                nomeAplicacao
                        + " encerrado."
        );
    }


    // ==============================================================
    // RESTART
    // ==============================================================

    public synchronized void restart()
            throws IOException {

        stop();

        start();
    }


    // ==============================================================
    // DIRETÓRIO
    // ==============================================================

    public Path getDiretorioAplicacao()
            throws IOException {

        return obterDiretorioTemporario();
    }


    // ==============================================================
    // OCULTAR
    // ==============================================================

    private void ocultarWindows(
            Path caminho
    ) {

        if (!OCULTAR_DIRETORIO) {

            return;
        }


        try {

            Files.setAttribute(
                    caminho,
                    "dos:hidden",
                    true
            );

        } catch (Exception ignored) {
        }
    }
}