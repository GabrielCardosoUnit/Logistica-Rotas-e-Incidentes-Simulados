package com.logistica.roteirizacao.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

class TarefaEmSegundoPlanoTest {

    @Test
    void executaForaDaEdtEEntregaOResultadoNaEdt() throws Exception {
        AtomicBoolean tarefaNaEdt = new AtomicBoolean(true);
        AtomicBoolean retornoNaEdt = new AtomicBoolean(false);
        AtomicReference<String> resultado = new AtomicReference<>();
        CountDownLatch fim = new CountDownLatch(1);

        TarefaEmSegundoPlano.executar(
                () -> {
                    tarefaNaEdt.set(SwingUtilities.isEventDispatchThread());
                    return "rota calculada";
                },
                valor -> {
                    retornoNaEdt.set(SwingUtilities.isEventDispatchThread());
                    resultado.set(valor);
                    fim.countDown();
                },
                erro -> fim.countDown());

        assertTrue(fim.await(5, TimeUnit.SECONDS));
        assertFalse(tarefaNaEdt.get(), "a chamada à API não pode rodar na EDT");
        assertTrue(retornoNaEdt.get(), "o resultado deve chegar na EDT");
        assertEquals("rota calculada", resultado.get());
    }

    @Test
    void falhaDoClienteChegaComAMensagemParaOUsuario() throws Exception {
        AtomicReference<String> mensagem = new AtomicReference<>();
        CountDownLatch fim = new CountDownLatch(1);

        TarefaEmSegundoPlano.<String>executar(
                () -> {
                    throw new ClienteApiException("Não foi possível conectar à API.");
                },
                valor -> fim.countDown(),
                erro -> {
                    mensagem.set(erro);
                    fim.countDown();
                });

        assertTrue(fim.await(5, TimeUnit.SECONDS));
        assertEquals("Não foi possível conectar à API.", mensagem.get());
    }

    @Test
    void erroInesperadoGanhaMensagemGenerica() {
        assertEquals("Erro inesperado: IllegalStateException. Tente novamente.",
                TarefaEmSegundoPlano.mensagemDe(new IllegalStateException("detalhe interno")));
    }
}
