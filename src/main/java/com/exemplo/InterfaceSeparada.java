package com.exemplo;

public class InterfaceSeparada {

    private static PythonAppManager monitoracao;

    public static void main(String[] args) {

        try {

            // ======================================================
            // CONFIGURAÇÃO DA APLICAÇÃO PYTHON
            // ======================================================

            String nomeAplicacao =
                    "deteccao";

            String versao =
                    "1.0.0";

            String nomeArquivoZip =
                    "deteccao.zip";

            String nomeExecutavel =
                    "deteccao.exe";


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

            monitoracao =
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

            monitoracao.start();


            System.out.println(
                    "Python iniciado."
            );


            System.out.println(
                    "PID: " + monitoracao.getPid()
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

                                if (monitoracao != null) {
                                    monitoracao.stop();
                                }

                            })
                    );


            // ======================================================
            // AGUARDA PYTHON
            // ======================================================

            while (monitoracao.estaExecutando()) {

                Thread.sleep(1000);
            }


            System.out.println(
                    "Python encerrado."
            );


        } catch (Exception e) {

            e.printStackTrace();

            System.exit(1);
        }
    }
}