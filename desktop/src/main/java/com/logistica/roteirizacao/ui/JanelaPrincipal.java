package com.logistica.roteirizacao.ui;

import com.logistica.roteirizacao.client.ClienteApi;
import com.logistica.roteirizacao.client.ClienteApiException;
import com.logistica.roteirizacao.client.TarefaEmSegundoPlano;
import com.logistica.roteirizacao.client.dto.Criterio;
import com.logistica.roteirizacao.client.dto.ImportacaoResultado;
import com.logistica.roteirizacao.client.dto.RotaResultado;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

/**
 * Tela única: 1) importar rede, 2) escolher origem/destino/critério, 3) ver resultado.
 * Fluxo: o botão Calcular só libera depois de uma importação bem-sucedida.
 * Toda chamada à API passa por {@link TarefaEmSegundoPlano} (SwingWorker): a janela não trava.
 */
public class JanelaPrincipal extends JFrame {

    private final ClienteApi cliente;
    private final PainelImportacao painelImportacao = new PainelImportacao();
    private final PainelSelecaoRota painelRota = new PainelSelecaoRota();
    private final PainelResultado painelResultado = new PainelResultado();
    private final PainelMapa painelMapa = new PainelMapa();
    private final JLabel lblMensagem = new JLabel(" ");
    private final JProgressBar progresso = new JProgressBar();
    private final Map<String, PontoRede> pontosPorId = new LinkedHashMap<>();
    private boolean redeImportada = false;

    public JanelaPrincipal(ClienteApi cliente) {
        super("Logística — Rotas e Incidentes Simulados (rede simulada de Aracaju/SE)");
        this.cliente = Objects.requireNonNull(cliente, "cliente da API é obrigatório");
        montarTela();
        ligarEventos();
    }

    private void montarTela() {
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JPanel conteudo = new JPanel();
        conteudo.setLayout(new BoxLayout(conteudo, BoxLayout.Y_AXIS));
        conteudo.setBorder(BorderFactory.createEmptyBorder(8, 10, 4, 10));
        conteudo.add(painelImportacao);
        conteudo.add(Box.createVerticalStrut(6));
        conteudo.add(painelRota);
        conteudo.add(Box.createVerticalStrut(6));
        conteudo.add(painelResultado); // ocupa o espaço que sobrar

        progresso.setIndeterminate(true);
        progresso.setVisible(false);
        progresso.setPreferredSize(new Dimension(140, 14));
        JPanel barra = new JPanel(new BorderLayout(8, 0));
        barra.setBorder(BorderFactory.createEmptyBorder(4, 10, 6, 10));
        barra.add(lblMensagem, BorderLayout.CENTER);
        barra.add(progresso, BorderLayout.EAST);

        JPanel centro = new JPanel(new BorderLayout(8, 0));
        centro.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
        centro.add(conteudo, BorderLayout.WEST);   // telas de controle e resultado
        centro.add(painelMapa, BorderLayout.CENTER); // mapa esquemático

        add(centro, BorderLayout.CENTER);
        add(barra, BorderLayout.SOUTH);
        setMinimumSize(new Dimension(1150, 640));
        pack();
        setLocationRelativeTo(null);
        getRootPane().setDefaultButton(painelImportacao.getBotaoImportar());
    }

    private void ligarEventos() {
        painelImportacao.addImportarListener(e -> importarRede());
        painelRota.addCalcularListener(e -> calcularRota());
    }

    // ---- importar -----------------------------------------------------------------------------

    private void importarRede() {
        if (!painelImportacao.validar()) {
            mostrarErro("Corrija os arquivos indicados e importe novamente.");
            return;
        }
        final File pontos = painelImportacao.getArquivoPontos();
        final File vias = painelImportacao.getArquivoVias();
        definirOcupado(true, "Importando rede…");
        TarefaEmSegundoPlano.executar(
                () -> Desfecho.de(() -> {
                    ImportacaoResultado api = cliente.importarRede(pontos, vias);
                    return new Importada(api, lerPontos(pontos), lerVias(vias));
                }),
                desfecho -> desfecho.tratar(this::aoImportar, this::aoFalharImportacao),
                this::aoFalharInesperado);
    }

    /** Lê o pontos.csv só para preencher os combos; falha de leitura vira erro com texto pronto. */
    private static List<PontoRede> lerPontos(File arquivo) throws ClienteApiException {
        try {
            return LeitorPontosCsv.ler(arquivo);
        } catch (IOException e) {
            throw new ClienteApiException(e.getMessage(), e);
        }
    }

    /** Lê o vias.csv só para desenhar as vias no mapa. */
    private static List<ViaRede> lerVias(File arquivo) throws ClienteApiException {
        try {
            return LeitorViasCsv.ler(arquivo);
        } catch (IOException e) {
            throw new ClienteApiException(e.getMessage(), e);
        }
    }

    private void aoImportar(Importada r) {
        pontosPorId.clear();
        for (PontoRede p : r.pontos) {
            pontosPorId.put(p.getId(), p);
        }
        painelRota.setPontos(r.pontos);
        painelMapa.setRede(r.pontos, r.vias);
        painelResultado.limpar();
        redeImportada = true;

        String resumo = Mensagens.resumoImportacao(r.api);
        painelImportacao.setStatus(resumo);
        definirOcupado(false, null);
        mostrarSucesso(resumo + " Agora escolha origem e destino.");
        getRootPane().setDefaultButton(painelRota.getBotaoCalcular());

        if (r.api.teveRejeicoes()) {
            DialogoLinhasRejeitadas.mostrar(this, r.api.linhasRejeitadas());
        }
    }

    private void aoFalharImportacao(ClienteApiException e) {
        definirOcupado(false, null);
        String msg = Mensagens.doErro(e);
        e.getCampo().ifPresent(campo -> painelImportacao.setErroCampo(campo, msg));
        mostrarErro(msg);
    }

    // ---- calcular -----------------------------------------------------------------------------

    private void calcularRota() {
        if (!redeImportada) {
            mostrarErro(Mensagens.IMPORTAR_ANTES);
            return;
        }
        if (!painelRota.validar()) {
            mostrarErro("Corrija os campos indicados e calcule novamente.");
            return;
        }
        final PontoRede origem = painelRota.getOrigem();
        final PontoRede destino = painelRota.getDestino();
        final Criterio criterio = painelRota.getCriterio();

        definirOcupado(true, "Calculando rota…");
        painelResultado.mostrarCalculando();
        painelMapa.limparRota();
        TarefaEmSegundoPlano.executar(
                () -> Desfecho.de(() -> cliente.calcularRota(origem.getId(), destino.getId(), criterio)),
                desfecho -> desfecho.tratar(this::aoCalcular, this::aoFalharCalculo),
                this::aoFalharInesperado);
    }

    private void aoCalcular(RotaResultado r) {
        definirOcupado(false, null);
        painelResultado.mostrar(r, pontosPorId);
        if (r.semCaminho()) {
            painelMapa.limparRota();
            mostrarAviso("Sem caminho disponível entre os pontos escolhidos. Veja o motivo em Resultado.");
        } else {
            painelMapa.setRota(r.pontos());
            mostrarSucesso("Rota calculada: " + Mensagens.km(r.distanciaTotalKm())
                    + " e " + Mensagens.min(r.tempoTotalMin()) + ".");
        }
    }

    private void aoFalharCalculo(ClienteApiException e) {
        definirOcupado(false, null);
        painelResultado.limpar();
        painelMapa.limparRota();
        String msg = Mensagens.doErro(e);
        e.getCampo().ifPresent(campo -> painelRota.setErroCampo(campo, msg));
        mostrarErro(msg);
    }

    /** Erro que não é da API (ex.: bug). O texto já vem pronto da TarefaEmSegundoPlano. */
    private void aoFalharInesperado(String mensagem) {
        definirOcupado(false, null);
        mostrarErro(mensagem);
    }

    // ---- barra de status ----------------------------------------------------------------------

    /** Enquanto a API responde: bloqueia os botões (evita clique duplo) e mostra progresso. */
    private void definirOcupado(boolean ocupado, String texto) {
        progresso.setVisible(ocupado);
        painelImportacao.setHabilitado(!ocupado);
        painelRota.setHabilitado(!ocupado && redeImportada);
        setCursor(Cursor.getPredefinedCursor(ocupado ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
        if (ocupado) {
            lblMensagem.setForeground(Color.DARK_GRAY);
            lblMensagem.setText(texto);
        }
    }

    private void mostrarErro(String texto) {
        lblMensagem.setForeground(Estilo.VERMELHO);
        lblMensagem.setText("Erro: " + texto);
    }

    private void mostrarSucesso(String texto) {
        lblMensagem.setForeground(Estilo.VERDE);
        lblMensagem.setText("OK: " + texto);
    }

    private void mostrarAviso(String texto) {
        lblMensagem.setForeground(Estilo.LARANJA);
        lblMensagem.setText("Aviso: " + texto);
    }

    /** Resultado do trabalho de importação: resposta da API + pontos e vias lidos dos CSVs (para o mapa). */
    private static final class Importada {
        private final ImportacaoResultado api;
        private final List<PontoRede> pontos;
        private final List<ViaRede> vias;

        private Importada(ImportacaoResultado api, List<PontoRede> pontos, List<ViaRede> vias) {
            this.api = api;
            this.pontos = pontos;
            this.vias = vias;
        }
    }
}
