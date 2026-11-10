package com.logistica.roteirizacao.ui;

import com.logistica.roteirizacao.client.ClienteApi;
import com.logistica.roteirizacao.client.ClienteApiException;
import com.logistica.roteirizacao.client.TarefaEmSegundoPlano;
import com.logistica.roteirizacao.client.dto.Criterio;
import com.logistica.roteirizacao.client.dto.ImportacaoResultado;
import com.logistica.roteirizacao.client.dto.IncidenteResultado;
import com.logistica.roteirizacao.client.dto.RotaResultado;
import com.logistica.roteirizacao.files.Configuracao;
import com.logistica.roteirizacao.files.ConfiguracaoBinaria;
import com.logistica.roteirizacao.files.ExportadorRotaCsv;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Tela única: 1) importar rede, 2) escolher origem/destino/critério, 3) ver resultado,
 * 4) bloquear via. Fluxo: Calcular e Bloquear só liberam depois de uma importação bem-sucedida.
 * Toda chamada à API passa por {@link TarefaEmSegundoPlano} (SwingWorker): a janela não trava.
 *
 * <p>Preferências (critério, última rota, URL da API) ficam no config.bin: lidas ao abrir,
 * gravadas a cada rota calculada e ao fechar a janela.
 */
public class JanelaPrincipal extends JFrame {

    private final ClienteApi cliente;
    private final File arquivoConfiguracao;
    private final PainelImportacao painelImportacao = new PainelImportacao();
    private final PainelSelecaoRota painelRota = new PainelSelecaoRota();
    private final PainelIncidente painelIncidente = new PainelIncidente();
    private final PainelResultado painelResultado = new PainelResultado();
    private final PainelMapa painelMapa = new PainelMapa();
    private final JLabel lblMensagem = new JLabel(" ");
    private final JProgressBar progresso = new JProgressBar();
    private final Map<String, PontoRede> pontosPorId = new LinkedHashMap<>();
    private Configuracao configuracao;
    private RotaResultado ultimaRota;
    private File ultimaPastaExportacao;
    private boolean redeImportada = false;

    /** Sem config.bin: usa a configuração padrão e não grava nada (útil em testes). */
    public JanelaPrincipal(ClienteApi cliente) {
        this(cliente, Configuracao.padrao(), null);
    }

    /**
     * @param configuracao        preferências já lidas do config.bin (ou as padrão)
     * @param arquivoConfiguracao onde gravar o config.bin; null para não gravar
     */
    public JanelaPrincipal(ClienteApi cliente, Configuracao configuracao, File arquivoConfiguracao) {
        super("Logística — Rotas e Incidentes Simulados (rede simulada de Aracaju/SE)");
        this.cliente = Objects.requireNonNull(cliente, "cliente da API é obrigatório");
        this.configuracao = Objects.requireNonNull(configuracao, "configuração é obrigatória");
        this.arquivoConfiguracao = arquivoConfiguracao;
        montarTela();
        ligarEventos();
        painelRota.setCriterio(configuracao.getCriterioPadrao());
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
        conteudo.add(painelIncidente);
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
        setMinimumSize(new Dimension(1150, 760));
        pack();
        setLocationRelativeTo(null);
        getRootPane().setDefaultButton(painelImportacao.getBotaoImportar());
    }

    private void ligarEventos() {
        painelImportacao.addImportarListener(e -> importarRede());
        painelRota.addCalcularListener(e -> calcularRota());
        painelIncidente.addBloquearListener(e -> bloquearVia());
        painelResultado.addExportarListener(e -> exportarRota());
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                salvarConfiguracao(); // garante o config.bin mesmo sem nenhuma rota nova
            }
        });
    }

    /** Mensagem mostrada ao abrir, por exemplo sobre o config.bin recuperado ou inválido. */
    public void mostrarMensagemInicial(String texto, boolean ehAviso) {
        if (ehAviso) {
            mostrarAviso(texto);
        } else {
            mostrarSucesso(texto);
        }
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
        painelIncidente.setVias(r.vias, pontosPorId);
        painelMapa.setRede(r.pontos, r.vias);
        painelResultado.limpar();
        ultimaRota = null;
        redeImportada = true;

        String resumo = Mensagens.resumoImportacao(r.api);
        painelImportacao.setStatus(resumo);
        definirOcupado(false, null);
        String proximoPasso = " Agora escolha origem e destino.";
        if (configuracao.temUltimaRota()
                && painelRota.selecionar(configuracao.getUltimaOrigemId(), configuracao.getUltimoDestinoId())) {
            proximoPasso = " Última rota recuperada do config.bin: "
                    + configuracao.getUltimaOrigemId() + " → " + configuracao.getUltimoDestinoId() + ".";
        }
        mostrarSucesso(resumo + proximoPasso);
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
        executarCalculo(painelRota.getOrigem().getId(), painelRota.getDestino().getId(),
                painelRota.getCriterio(), "");
    }

    /**
     * Chama POST /rotas em segundo plano. Usado pelo botão Calcular e pelo recálculo
     * depois de um bloqueio.
     *
     * @param contexto texto que antecede a mensagem de sucesso (ex.: "Via V24 bloqueada. ")
     */
    private void executarCalculo(String origemId, String destinoId, Criterio criterio, String contexto) {
        definirOcupado(true, contexto.isEmpty() ? "Calculando rota…" : contexto + "Recalculando rota…");
        painelResultado.mostrarCalculando();
        painelMapa.limparRota();
        TarefaEmSegundoPlano.executar(
                () -> Desfecho.de(() -> cliente.calcularRota(origemId, destinoId, criterio)),
                desfecho -> desfecho.tratar(r -> aoCalcular(r, contexto), this::aoFalharCalculo),
                this::aoFalharInesperado);
    }

    private void aoCalcular(RotaResultado r, String contexto) {
        definirOcupado(false, null);
        ultimaRota = r;
        painelResultado.mostrar(r, pontosPorId);
        painelResultado.setExportavel(!r.semCaminho());
        configuracao = configuracao.comUltimaRota(r.origemId(), r.destinoId(), r.criterio());
        String avisoConfig = salvarConfiguracao();

        if (r.semCaminho()) {
            painelMapa.limparRota();
            mostrarAviso(contexto + "Sem caminho disponível entre os pontos escolhidos. Veja o motivo em Resultado.");
        } else if (avisoConfig != null) {
            painelMapa.setRota(r.pontos());
            mostrarAviso(contexto + "Rota calculada, mas " + avisoConfig);
        } else {
            painelMapa.setRota(r.pontos());
            mostrarSucesso(contexto + (contexto.isEmpty() ? "Rota calculada: " : "Rota recalculada: ")
                    + Mensagens.km(r.distanciaTotalKm()) + " e " + Mensagens.min(r.tempoTotalMin()) + ".");
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

    // ---- bloquear via -------------------------------------------------------------------------

    private void bloquearVia() {
        if (!redeImportada) {
            mostrarErro(Mensagens.IMPORTAR_ANTES);
            return;
        }
        if (!painelIncidente.validar()) {
            mostrarErro("Corrija os campos indicados e bloqueie novamente.");
            return;
        }
        final String viaId = painelIncidente.getVia().getId();
        final String descricao = painelIncidente.getDescricao();
        definirOcupado(true, "Bloqueando a via " + viaId + "…");
        TarefaEmSegundoPlano.executar(
                () -> Desfecho.de(() -> cliente.registrarIncidente(viaId, descricao)),
                desfecho -> desfecho.tratar(this::aoBloquear, this::aoFalharBloqueio),
                this::aoFalharInesperado);
    }

    private void aoBloquear(IncidenteResultado incidente) {
        definirOcupado(false, null);
        painelIncidente.marcarBloqueada(incidente.viaId());
        painelMapa.marcarViaBloqueada(incidente.viaId());
        String contexto = "Via " + incidente.viaId() + " bloqueada (rede versão " + incidente.versaoRede() + "). ";
        if (ultimaRota == null) {
            mostrarSucesso(contexto + "Calcule uma rota para ver o desvio.");
            return;
        }
        // Fluxo do tema: bloquear trecho e recalcular a mesma rota sem ele.
        executarCalculo(ultimaRota.origemId(), ultimaRota.destinoId(), ultimaRota.criterio(), contexto);
    }

    private void aoFalharBloqueio(ClienteApiException e) {
        definirOcupado(false, null);
        String msg = Mensagens.doErro(e);
        e.getCampo().ifPresent(campo -> painelIncidente.setErroCampo(campo, msg));
        mostrarErro(msg);
    }

    // ---- exportar CSV -------------------------------------------------------------------------

    private void exportarRota() {
        if (ultimaRota == null || ultimaRota.semCaminho()) {
            mostrarErro(Mensagens.CALCULAR_ANTES_DE_EXPORTAR);
            return;
        }
        JFileChooser seletor = new JFileChooser(ultimaPastaExportacao);
        seletor.setDialogTitle("Exportar rota em CSV");
        seletor.setFileFilter(new FileNameExtensionFilter("Arquivos CSV (*.csv)", "csv"));
        seletor.setSelectedFile(new File(ultimaPastaExportacao, nomeSugerido(ultimaRota)));
        if (seletor.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File destino = comExtensaoCsv(seletor.getSelectedFile());
        ultimaPastaExportacao = destino.getAbsoluteFile().getParentFile();
        if (destino.exists() && JOptionPane.showConfirmDialog(this,
                "O arquivo " + destino.getName() + " já existe. Deseja substituí-lo?",
                "Confirmar exportação", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }
        exportarPara(destino);
    }

    /** Grava o CSV (arquivo pequeno: pode rodar na EDT sem travar a janela). */
    void exportarPara(File destino) {
        try {
            ExportadorRotaCsv.exportar(ultimaRota, nomesDosPontos(), destino);
            mostrarSucesso("Rota exportada para " + destino.getName() + " (" + ultimaRota.pontos().size()
                    + " pontos).");
        } catch (IOException e) {
            mostrarErro("Não foi possível gravar " + destino.getName()
                    + ". Verifique se a pasta existe e se o arquivo não está aberto em outro programa.");
        } catch (IllegalArgumentException e) {
            mostrarErro(e.getMessage());
        }
    }

    static String nomeSugerido(RotaResultado rota) {
        return "rota-" + rota.id() + "-" + rota.origemId() + "-" + rota.destinoId() + ".csv";
    }

    static File comExtensaoCsv(File arquivo) {
        return arquivo.getName().toLowerCase().endsWith(".csv")
                ? arquivo : new File(arquivo.getParentFile(), arquivo.getName() + ".csv");
    }

    private Map<String, String> nomesDosPontos() {
        Map<String, String> nomes = new LinkedHashMap<>();
        for (PontoRede p : pontosPorId.values()) {
            nomes.put(p.getId(), p.getNome());
        }
        return nomes;
    }

    // ---- config.bin ---------------------------------------------------------------------------

    /** Grava o config.bin. Devolve null se deu certo ou o texto do problema (sem interromper o uso). */
    private String salvarConfiguracao() {
        if (arquivoConfiguracao == null) {
            return null;
        }
        try {
            ConfiguracaoBinaria.salvar(configuracao, arquivoConfiguracao);
            return null;
        } catch (IOException e) {
            return "não foi possível gravar " + arquivoConfiguracao.getName() + " (preferências não salvas).";
        }
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
        painelIncidente.setHabilitado(!ocupado && redeImportada);
        painelResultado.setExportavel(!ocupado && ultimaRota != null && !ultimaRota.semCaminho());
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

    // ---- acesso para os testes (mesmo pacote) -------------------------------------------------

    PainelImportacao painelImportacao() { return painelImportacao; }
    PainelSelecaoRota painelRota() { return painelRota; }
    PainelIncidente painelIncidente() { return painelIncidente; }
    PainelResultado painelResultado() { return painelResultado; }
    PainelMapa painelMapa() { return painelMapa; }
    String textoDaBarra() { return lblMensagem.getText(); }
    RotaResultado ultimaRota() { return ultimaRota; }

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
