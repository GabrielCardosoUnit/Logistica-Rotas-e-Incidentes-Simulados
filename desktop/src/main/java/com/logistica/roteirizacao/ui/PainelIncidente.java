package com.logistica.roteirizacao.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.TreeMap;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;

/**
 * Seção 4: bloquear uma via (registrar incidente) e desbloqueá-la (encerrar o incidente).
 * Depois de cada um, a janela recalcula a última rota. Vias bloqueadas saem da lista de
 * bloqueio (a API recusaria com 409) e passam para a lista de desbloqueio, e vice-versa.
 *
 * @serial exclude
 */
public class PainelIncidente extends JPanel {

    static final String DESCRICAO_PADRAO = "Alagamento simulado";
    private static final int DESCRICAO_MINIMA = 3;
    private static final int DESCRICAO_MAXIMA = 200;

    private final JComboBox<ViaRede> comboVia = new JComboBox<>();
    private final JTextField campoDescricao = new JTextField(DESCRICAO_PADRAO, 24);
    private final JButton btnBloquear = new JButton("Bloquear via");
    private final JLabel erroVia = Estilo.rotuloErro();
    private final JLabel erroDescricao = Estilo.rotuloErro();
    private final JComboBox<ViaRede> comboBloqueadas = new JComboBox<>();
    private final JButton btnDesbloquear = new JButton("Desbloquear via");
    private final JLabel erroBloqueada = Estilo.rotuloErro();
    private final JLabel lblBloqueadas = new JLabel();
    /** Vias com incidente ativo: id da via → id do incidente na API (necessário para desbloquear). */
    private final TreeMap<String, Long> bloqueadas = new TreeMap<>();
    /** Vias habilitadas da rede, na ordem do CSV; base das duas listas. */
    private final List<ViaRede> viasHabilitadas = new ArrayList<>();
    private Map<String, PontoRede> pontosPorId = Map.of();
    private boolean habilitado;

    /** Monta o painel, desabilitado até a rede ser importada. */
    public PainelIncidente() {
        setBorder(BorderFactory.createTitledBorder("4. Bloquear ou desbloquear via (incidente)"));
        setLayout(new GridBagLayout());

        comboVia.setRenderer(new RenderizadorVia("Selecione uma via…"));
        comboVia.setPrototypeDisplayValue(new ViaRede("V00", "P00", "P00", true));
        comboBloqueadas.setRenderer(new RenderizadorVia("Selecione uma via bloqueada…"));
        comboBloqueadas.setPrototypeDisplayValue(new ViaRede("V00", "P00", "P00", true));
        campoDescricao.setToolTipText("Motivo do bloqueio, de " + DESCRICAO_MINIMA + " a " + DESCRICAO_MAXIMA
                + " caracteres");

        JLabel lblVia = new JLabel("Via:");
        JLabel lblDescricao = new JLabel("Descrição:");
        JLabel lblBloqueada = new JLabel("Bloqueada:");
        lblVia.setDisplayedMnemonic(KeyEvent.VK_A);
        lblVia.setLabelFor(comboVia);
        lblDescricao.setDisplayedMnemonic(KeyEvent.VK_R);
        lblDescricao.setLabelFor(campoDescricao);
        lblBloqueada.setDisplayedMnemonic(KeyEvent.VK_L);
        lblBloqueada.setLabelFor(comboBloqueadas);
        btnBloquear.setMnemonic(KeyEvent.VK_B);
        btnBloquear.setToolTipText("Registra o incidente na API e recalcula a última rota sem esta via");
        btnDesbloquear.setMnemonic(KeyEvent.VK_U);
        btnDesbloquear.setToolTipText("Encerra o incidente na API e recalcula a última rota já com esta via");

        // Os dois botões com a mesma largura, alinhados na coluna da direita.
        Dimension tamanhoBotao = new Dimension(
                Math.max(btnBloquear.getPreferredSize().width, btnDesbloquear.getPreferredSize().width),
                Math.max(btnBloquear.getPreferredSize().height, btnDesbloquear.getPreferredSize().height));
        btnBloquear.setPreferredSize(tamanhoBotao);
        btnDesbloquear.setPreferredSize(tamanhoBotao);
        lblBloqueadas.setForeground(Color.DARK_GRAY);

        // Três colunas: rótulo | campo (estica) | botão. Cada botão fica na linha do campo que ele usa.
        // Bloquear
        adicionar(lblVia, 0, 0, 1, 0, GridBagConstraints.NONE);
        adicionar(comboVia, 1, 0, 2, 1, GridBagConstraints.HORIZONTAL);
        adicionar(erroVia, 1, 1, 2, 1, GridBagConstraints.HORIZONTAL);
        adicionar(lblDescricao, 0, 2, 1, 0, GridBagConstraints.NONE);
        adicionar(campoDescricao, 1, 2, 1, 1, GridBagConstraints.HORIZONTAL);
        adicionar(btnBloquear, 2, 2, 1, 0, GridBagConstraints.NONE);
        adicionar(erroDescricao, 1, 3, 2, 1, GridBagConstraints.HORIZONTAL);
        // Separa a ação de bloquear da de desbloquear.
        GridBagConstraints g = restricoes(0, 4, 3, 1, GridBagConstraints.HORIZONTAL);
        g.insets = new Insets(4, 4, 6, 4);
        add(new JSeparator(), g);
        // Desbloquear
        adicionar(lblBloqueada, 0, 5, 1, 0, GridBagConstraints.NONE);
        adicionar(comboBloqueadas, 1, 5, 1, 1, GridBagConstraints.HORIZONTAL);
        adicionar(btnDesbloquear, 2, 5, 1, 0, GridBagConstraints.NONE);
        adicionar(erroBloqueada, 1, 6, 2, 1, GridBagConstraints.HORIZONTAL);
        adicionar(lblBloqueadas, 1, 7, 2, 1, GridBagConstraints.HORIZONTAL);

        setVias(List.of(), Map.of());
        setHabilitado(false);
    }

    private void adicionar(Component c, int x, int y, int largura, double pesoX, int fill) {
        add(c, restricoes(x, y, largura, pesoX, fill));
    }

    private static GridBagConstraints restricoes(int x, int y, int largura, double pesoX, int fill) {
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = x;
        g.gridy = y;
        g.gridwidth = largura;
        g.weightx = pesoX;
        g.fill = fill;
        g.anchor = GridBagConstraints.WEST;
        g.insets = new Insets(2, 4, 2, 4);
        return g;
    }

    /** "V24 — Distrito Industrial ↔ UNIT": nomes no lugar dos ids, quando conhecidos. */
    private String descrever(ViaRede v) {
        return v.getId() + " — " + nome(v.getOrigemId()) + " ↔ " + nome(v.getDestinoId());
    }

    private String nome(String pontoId) {
        PontoRede p = pontosPorId.get(pontoId);
        return p == null ? pontoId : p.getNome();
    }

    /**
     * Lista as vias da rede importada. Vias desabilitadas no CSV já estão fora do cálculo e não entram.
     *
     * @param vias   vias lidas do vias.csv
     * @param pontos pontos por id, para mostrar o nome das pontas
     */
    public void setVias(List<ViaRede> vias, Map<String, PontoRede> pontos) {
        this.pontosPorId = Map.copyOf(pontos);
        bloqueadas.clear(); // reimportar a rede apaga os incidentes na API
        viasHabilitadas.clear();
        for (ViaRede v : vias) {
            if (v.isHabilitada()) {
                viasHabilitadas.add(v);
            }
        }
        campoDescricao.setText(DESCRICAO_PADRAO);
        atualizarListas();
        limparErros();
    }

    /**
     * Depois do sucesso na API: tira a via da lista de bloqueio e a põe na de desbloqueio.
     *
     * @param viaId       via bloqueada
     * @param incidenteId incidente que a bloqueou
     */
    public void marcarBloqueada(String viaId, long incidenteId) {
        bloqueadas.put(viaId, incidenteId);
        atualizarListas();
    }

    /**
     * Depois do desbloqueio na API: a via volta a poder ser bloqueada.
     *
     * @param viaId via desbloqueada
     */
    public void marcarDesbloqueada(String viaId) {
        bloqueadas.remove(viaId);
        atualizarListas();
    }

    /**
     * Id do incidente ativo na via, se ela foi bloqueada por esta tela.
     *
     * @param viaId id da via
     * @return o id do incidente, ou vazio
     */
    public OptionalLong incidenteDa(String viaId) {
        Long id = bloqueadas.get(viaId);
        return id == null ? OptionalLong.empty() : OptionalLong.of(id);
    }

    /** Remonta as duas listas a partir das vias da rede e das bloqueadas, mantendo a ordem do CSV. */
    private void atualizarListas() {
        DefaultComboBoxModel<ViaRede> livres = new DefaultComboBoxModel<>();
        DefaultComboBoxModel<ViaRede> comIncidente = new DefaultComboBoxModel<>();
        livres.addElement(null);       // "Selecione uma via…"
        comIncidente.addElement(null); // "Selecione uma via bloqueada…"
        for (ViaRede v : viasHabilitadas) {
            if (bloqueadas.containsKey(v.getId())) {
                comIncidente.addElement(v);
            } else {
                livres.addElement(v);
            }
        }
        comboVia.setModel(livres);
        comboBloqueadas.setModel(comIncidente);
        lblBloqueadas.setText(bloqueadas.isEmpty() ? "Vias bloqueadas: nenhuma"
                : "Vias bloqueadas: " + String.join(", ", bloqueadas.keySet()));
        atualizarDesbloqueio();
    }

    /** A parte de desbloqueio só fica ativa quando há alguma via bloqueada. */
    private void atualizarDesbloqueio() {
        boolean ativo = habilitado && !bloqueadas.isEmpty();
        comboBloqueadas.setEnabled(ativo);
        btnDesbloquear.setEnabled(ativo);
    }

    /**
     * Mesmas regras da API (via obrigatória, descrição de 3 a 200 caracteres), com o erro sob o campo.
     *
     * @return {@code true} se pode registrar o incidente
     */
    public boolean validar() {
        limparErros();
        boolean ok = true;
        if (getVia() == null) {
            Estilo.mostrarErro(erroVia, Mensagens.ESCOLHER_VIA);
            ok = false;
        }
        int tamanho = getDescricao().length();
        if (tamanho < DESCRICAO_MINIMA || tamanho > DESCRICAO_MAXIMA) {
            Estilo.mostrarErro(erroDescricao, Mensagens.DESCRICAO_INCIDENTE);
            ok = false;
        }
        return ok;
    }

    /**
     * Valida a escolha da via a desbloquear, com o erro sob a lista.
     *
     * @return {@code true} se pode desbloquear
     */
    public boolean validarDesbloqueio() {
        limparErros();
        if (getViaBloqueada() == null) {
            Estilo.mostrarErro(erroBloqueada, Mensagens.ESCOLHER_VIA_BLOQUEADA);
            return false;
        }
        return true;
    }

    /**
     * Erro da API no desbloqueio: aparece sob a lista de vias bloqueadas.
     *
     * @param mensagem texto do erro
     */
    public void setErroDesbloqueio(String mensagem) {
        Estilo.mostrarErro(erroBloqueada, mensagem);
    }

    /**
     * Erro da API por campo (viaId, descricao).
     *
     * @param campo    campo apontado pela API
     * @param mensagem texto mostrado sob o campo
     */
    public void setErroCampo(String campo, String mensagem) {
        if ("viaId".equals(campo)) {
            Estilo.mostrarErro(erroVia, mensagem);
        } else if ("descricao".equals(campo)) {
            Estilo.mostrarErro(erroDescricao, mensagem);
        }
    }

    /** Apaga as mensagens de erro dos campos. */
    public void limparErros() {
        Estilo.limpar(erroVia);
        Estilo.limpar(erroDescricao);
        Estilo.limpar(erroBloqueada);
    }

    /**
     * Liga ou desliga os controles (desligados sem rede ou durante uma chamada à API).
     *
     * @param habilitado {@code true} para liberar
     */
    public void setHabilitado(boolean habilitado) {
        this.habilitado = habilitado;
        comboVia.setEnabled(habilitado);
        campoDescricao.setEnabled(habilitado);
        btnBloquear.setEnabled(habilitado);
        atualizarDesbloqueio();
    }

    /**
     * Via escolhida para bloquear.
     *
     * @return a via, ou {@code null}
     */
    public ViaRede getVia() { return (ViaRede) comboVia.getSelectedItem(); }
    /**
     * Via escolhida para desbloquear.
     *
     * @return a via, ou {@code null}
     */
    public ViaRede getViaBloqueada() { return (ViaRede) comboBloqueadas.getSelectedItem(); }
    /**
     * Descrição digitada, sem espaços nas pontas.
     *
     * @return a descrição
     */
    public String getDescricao() { return campoDescricao.getText().trim(); }

    /** Seleciona a via pelo id (usado nos testes). Devolve false se ela não está na lista. */
    boolean selecionarVia(String viaId) {
        return selecionar(comboVia, viaId);
    }

    /** Seleciona a via na lista de bloqueadas (usado nos testes). Devolve false se ela não está lá. */
    boolean selecionarViaBloqueada(String viaId) {
        return selecionar(comboBloqueadas, viaId);
    }

    private static boolean selecionar(JComboBox<ViaRede> combo, String viaId) {
        for (int i = 1; i < combo.getItemCount(); i++) {
            if (combo.getItemAt(i).getId().equals(viaId)) {
                combo.setSelectedIndex(i);
                return true;
            }
        }
        return false;
    }

    /**
     * Avisa quando o usuário clica em Bloquear via.
     *
     * @param l ouvinte do clique
     */
    public void addBloquearListener(ActionListener l) {
        btnBloquear.addActionListener(l);
    }

    /**
     * Avisa quando o usuário clica em Desbloquear via.
     *
     * @param l ouvinte do clique
     */
    public void addDesbloquearListener(ActionListener l) {
        btnDesbloquear.addActionListener(l);
    }

    JButton getBotaoBloquear() { return btnBloquear; }
    JButton getBotaoDesbloquear() { return btnDesbloquear; }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }

    /** Mostra "V24 — Distrito Industrial ↔ UNIT" e, no item vazio, a dica de seleção. */
    private final class RenderizadorVia extends DefaultListCellRenderer {
        private final String dica;

        private RenderizadorVia(String dica) {
            this.dica = dica;
        }

        @Override
        public Component getListCellRendererComponent(JList<?> lista, Object valor, int indice,
                                                      boolean selecionado, boolean foco) {
            String texto = valor == null ? dica : descrever((ViaRede) valor);
            return super.getListCellRendererComponent(lista, texto, indice, selecionado, foco);
        }
    }
}
