package com.exemplo;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class InterfaceSeparada {

    private static final String CONFIG_RESOURCE = "python-app.properties";

    private static PythonAppManager pythonApp;

    public static void main(String[] args) {

        try {

            Properties config = carregarConfiguracao();

            String nomeAplicacao = config.getProperty("application");
            String versao = config.getProperty("version");
            String nomeArquivoZip = config.getProperty("zip");
            String nomeExecutavel = config.getProperty("executable");


            // ======================================================
            // EXIBE OS ARGUMENTOS RECEBIDOS PELO JAR
            // ======================================================

            System.out.println("Argumentos recebidos pelo JAR:");

            for (String arg : args) {
                System.out.println("  " + arg);
            }


            // ======================================================
            // CRIA O GERENCIADOR
            //
            // Os argumentos recebidos pelo JAR são repassados
            // diretamente para o executável Python.
            // ======================================================

            pythonApp =
                    new PythonAppManager(
                            InterfaceSeparada.class,
                            nomeAplicacao,
                            versao,
                            nomeArquivoZip,
                            nomeExecutavel,
                            args
                    );


            // ======================================================
            // START
            // ======================================================

            pythonApp.start();


            System.out.println(
                    "Python iniciado."
            );


            System.out.println(
                    "PID: " + pythonApp.getPid()
            );


            System.out.println(
                    "Java continua executando."
            );


            // ======================================================
            // SHUTDOWN HOOK
            // ======================================================

            Runtime.getRuntime()
                    .addShutdownHook(
                            new Thread(() -> {

                                                                if (pythonApp != null) {
                                                                        pythonApp.stop();
                                }

                            })
                    );


            // ======================================================
            // AGUARDA PYTHON
            // ======================================================

                        while (pythonApp.estaExecutando()) {

                Thread.sleep(1000);
            }


            System.out.println(
                    "Python encerrado."
            );

            System.exit(pythonApp.getExitCode());


        } catch (Exception e) {

            e.printStackTrace();

            System.exit(1);
        }
    }

        private static Properties carregarConfiguracao()
                        throws IOException {

                Properties config = new Properties();

                try (InputStream input = InterfaceSeparada.class
                                .getClassLoader()
                                .getResourceAsStream(CONFIG_RESOURCE)) {

                        if (input == null) {
                                throw new IOException(
                                                "Configuracao nao encontrada no JAR: "
                                                                + CONFIG_RESOURCE);
                        }

                        config.load(new InputStreamReader(input, StandardCharsets.UTF_8));
                }

                for (String key : new String[]{"application", "version", "zip", "executable"}) {
                        if (config.getProperty(key) == null
                                        || config.getProperty(key).isBlank()) {
                                throw new IOException("Configuracao sem valor obrigatorio: " + key);
                        }
                }

                return config;
        }
}