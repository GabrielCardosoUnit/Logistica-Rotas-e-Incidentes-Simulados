package com.logistica.roteirizacao.client;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import javax.swing.SwingWorker;

/**
 * Executa uma chamada à API fora da thread da GUI (EDT) e devolve o resultado
 * na EDT, onde é seguro mexer nos componentes Swing.
 *
 * <pre>{@code
 * botaoCalcular.addActionListener(e -> {
 *     botaoCalcular.setEnabled(false);
 *     TarefaEmSegundoPlano.executar(
 *         () -> cliente.calcularRota(origem, destino, criterio),
 *         rota -> { mostrarRota(rota); botaoCalcular.setEnabled(true); },
 *         erro -> { mostrarErro(erro); botaoCalcular.setEnabled(true); });
 * });
 * }</pre>
 */
public final class TarefaEmSegundoPlano {

    private TarefaEmSegundoPlano() {
    }

    /**
     * @param tarefa     roda em segundo plano; não pode tocar em componentes Swing
     * @param aoConcluir recebe o resultado, na EDT
     * @param aoFalhar   recebe a mensagem de erro já pronta para o usuário, na EDT
     */
    public static <T> SwingWorker<T, Void> executar(Callable<T> tarefa,
                                                     Consumer<T> aoConcluir,
                                                     Consumer<String> aoFalhar) {
        Objects.requireNonNull(tarefa, "tarefa é obrigatória.");
        Objects.requireNonNull(aoConcluir, "aoConcluir é obrigatório.");
        Objects.requireNonNull(aoFalhar, "aoFalhar é obrigatório.");

        SwingWorker<T, Void> worker = new SwingWorker<>() {
            @Override
            protected T doInBackground() throws Exception {
                return tarefa.call();
            }

            @Override
            protected void done() {
                try {
                    aoConcluir.accept(get());
                } catch (ExecutionException e) {
                    aoFalhar.accept(mensagemDe(e.getCause()));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    aoFalhar.accept("A operação foi interrompida.");
                }
            }
        };
        worker.execute();
        return worker;
    }

    /** Erros do cliente já trazem texto para o usuário; os demais ganham um texto genérico. */
    static String mensagemDe(Throwable erro) {
        if (erro instanceof ClienteApiException && erro.getMessage() != null) {
            return erro.getMessage();
        }
        return "Erro inesperado: " + (erro == null ? "desconhecido" : erro.getClass().getSimpleName())
                + ". Tente novamente.";
    }
}
