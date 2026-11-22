package com.logistica.roteirizacao.ui;

import com.logistica.roteirizacao.client.dto.Criterio;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

/**
 * Seção 2: origem, destino, critério (distância ou tempo) e botão Calcular.
 *
 * @serial exclude
 */
public class PainelSelecaoRota extends JPanel {

    private static final String PLACEHOLDER = "Selecione um ponto…";

    private final JComboBox<PontoRede> comboOrigem = new JComboBox<>();
    private final JComboBox<PontoRede> comboDestino = new JComboBox<>();
    private final JRadioButton rbDistancia = new JRadioButton(Criterio.MENOR_DISTANCIA.getDescricao(), true);
    private final JRadioButton rbTempo = new JRadioButton(Criterio.MENOR_TEMPO.getDescricao());
    private final JButton btnInverter = new JButton("Inverter");
    private final JButton btnCalcular = new JButton("Calcular rota");
    private final JLabel erroOrigem = Estilo.rotuloErro();
    private final JLabel erroDestino = Estilo.rotuloErro();
    private final JLabel dica = new JLabel("Importe a rede para liberar a escolha dos pontos.");

    /** Monta o painel, desabilitado até a rede ser importada. */
    public PainelSelecaoRota() {
        setBorder(BorderFactory.createTitledBorder("2. Escolher origem, destino e critério"));
        setLayout(new GridBagLayout());

        configurarCombo(comboOrigem);
        configurarCombo(comboDestino);
        ButtonGroup grupo = new ButtonGroup(); // só um critério por vez
        grupo.add(rbDistancia);
        grupo.add(rbTempo);

        JLabel lblOrigem = new JLabel("Origem:");
        JLabel lblDestino = new JLabel("Destino:");
        JLabel lblCriterio = new JLabel("Critério:");
        lblOrigem.setDisplayedMnemonic(KeyEvent.VK_O);
        lblOrigem.setLabelFor(comboOrigem);
        lblDestino.setDisplayedMnemonic(KeyEvent.VK_D);
        lblDestino.setLabelFor(comboDestino);
        rbDistancia.setMnemonic(KeyEvent.VK_S);
        rbTempo.setMnemonic(KeyEvent.VK_T);
        btnInverter.setMnemonic(KeyEvent.VK_N);
        btnInverter.setToolTipText("Troca a origem pelo destino");
        btnCalcular.setMnemonic(KeyEvent.VK_C);

        JPanel criterios = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        rbDistancia.setToolTipText("Soma de distanciaKm das vias (resultado em km)");
        rbTempo.setToolTipText("Soma de tempoMin das vias (resultado em minutos)");
        criterios.add(rbDistancia);
        criterios.add(javax.swing.Box.createHorizontalStrut(16));
        criterios.add(rbTempo);

        adicionar(lblOrigem, 0, 0, 1, 0, GridBagConstraints.NONE);
        adicionar(comboOrigem, 1, 0, 1, 1, GridBagConstraints.HORIZONTAL);
        adicionar(btnInverter, 2, 0, 1, 0, GridBagConstraints.NONE);
        adicionar(erroOrigem, 1, 1, 2, 1, GridBagConstraints.HORIZONTAL);
        adicionar(lblDestino, 0, 2, 1, 0, GridBagConstraints.NONE);
        adicionar(comboDestino, 1, 2, 1, 1, GridBagConstraints.HORIZONTAL);
        adicionar(erroDestino, 1, 3, 2, 1, GridBagConstraints.HORIZONTAL);
        adicionar(lblCriterio, 0, 4, 1, 0, GridBagConstraints.NONE);
        adicionar(criterios, 1, 4, 2, 1, GridBagConstraints.HORIZONTAL);
        adicionar(btnCalcular, 0, 5, 1, 0, GridBagConstraints.NONE);
        adicionar(dica, 1, 5, 2, 1, GridBagConstraints.HORIZONTAL);

        btnInverter.addActionListener(e -> inverter());
        setHabilitado(false);
    }

    private void configurarCombo(JComboBox<PontoRede> combo) {
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> lista, Object valor, int indice,
                                                          boolean selecionado, boolean foco) {
                return super.getListCellRendererComponent(lista, valor == null ? PLACEHOLDER : valor,
                        indice, selecionado, foco);
            }
        });
        combo.setPrototypeDisplayValue(new PontoRede("P00", "Universidade Tiradentes (UNIT) - Farolândia", 0, 0));
        combo.setModel(modeloVazio());
    }

    private static DefaultComboBoxModel<PontoRede> modeloVazio() {
        DefaultComboBoxModel<PontoRede> m = new DefaultComboBoxModel<>();
        m.addElement(null); // primeiro item = "Selecione um ponto…"
        return m;
    }

    private void adicionar(Component c, int x, int y, int largura, double pesoX, int fill) {
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = x;
        g.gridy = y;
        g.gridwidth = largura;
        g.weightx = pesoX;
        g.fill = fill;
        g.anchor = GridBagConstraints.WEST;
        g.insets = new Insets(2, 4, 2, 4);
        add(c, g);
    }

    /**
     * Preenche os dois combos com os pontos importados e volta à situação "nada escolhido".
     *
     * @param pontos pontos da rede
     */
    public void setPontos(List<PontoRede> pontos) {
        DefaultComboBoxModel<PontoRede> mo = modeloVazio();
        DefaultComboBoxModel<PontoRede> md = modeloVazio();
        for (PontoRede p : pontos) {
            mo.addElement(p);
            md.addElement(p);
        }
        comboOrigem.setModel(mo);
        comboDestino.setModel(md);
        limparErros();
    }

    private void inverter() {
        Object o = comboOrigem.getSelectedItem();
        comboOrigem.setSelectedItem(comboDestino.getSelectedItem());
        comboDestino.setSelectedItem(o);
    }

    /**
     * Valida a escolha. Mensagens dizem o que corrigir, sob o campo com problema.
     *
     * @return {@code true} se pode calcular
     */
    public boolean validar() {
        limparErros();
        boolean ok = true;
        if (getOrigem() == null) {
            Estilo.mostrarErro(erroOrigem, Mensagens.ESCOLHER_ORIGEM);
            ok = false;
        }
        if (getDestino() == null) {
            Estilo.mostrarErro(erroDestino, Mensagens.ESCOLHER_DESTINO);
            ok = false;
        } else if (getDestino().equals(getOrigem())) {
            Estilo.mostrarErro(erroDestino, Mensagens.DESTINO_IGUAL_ORIGEM);
            ok = false;
        }
        return ok;
    }

    /**
     * Erro da API por campo (origemId, destinoId).
     *
     * @param campo    campo apontado pela API
     * @param mensagem texto mostrado sob o campo
     * @return {@code true} se o campo é deste painel
     */
    public boolean setErroCampo(String campo, String mensagem) {
        if ("origemId".equals(campo)) {
            Estilo.mostrarErro(erroOrigem, mensagem);
            return true;
        }
        if ("destinoId".equals(campo)) {
            Estilo.mostrarErro(erroDestino, mensagem);
            return true;
        }
        return false;
    }

    /** Apaga as mensagens de erro dos campos. */
    public void limparErros() {
        Estilo.limpar(erroOrigem);
        Estilo.limpar(erroDestino);
    }

    /**
     * Liga ou desliga os controles (desligados sem rede ou durante uma chamada à API).
     *
     * @param habilitado {@code true} para liberar
     */
    public void setHabilitado(boolean habilitado) {
        comboOrigem.setEnabled(habilitado);
        comboDestino.setEnabled(habilitado);
        rbDistancia.setEnabled(habilitado);
        rbTempo.setEnabled(habilitado);
        btnInverter.setEnabled(habilitado);
        btnCalcular.setEnabled(habilitado);
        dica.setVisible(!habilitado && comboOrigem.getItemCount() <= 1);
    }

    /**
     * Origem escolhida.
     *
     * @return o ponto, ou {@code null}
     */
    public PontoRede getOrigem() { return (PontoRede) comboOrigem.getSelectedItem(); }
    /**
     * Destino escolhido.
     *
     * @return o ponto, ou {@code null}
     */
    public PontoRede getDestino() { return (PontoRede) comboDestino.getSelectedItem(); }

    /**
     * Critério marcado.
     *
     * @return o critério
     */
    public Criterio getCriterio() {
        return rbTempo.isSelected() ? Criterio.MENOR_TEMPO : Criterio.MENOR_DISTANCIA;
    }

    /**
     * Usado ao recuperar o critério padrão do config.bin.
     *
     * @param criterio critério a marcar
     */
    public void setCriterio(Criterio criterio) {
        (criterio == Criterio.MENOR_TEMPO ? rbTempo : rbDistancia).setSelected(true);
    }

    /**
     * Seleciona origem e destino pelos ids (por exemplo, a última rota guardada no config.bin).
     * Ids que não existem na rede importada são ignorados.
     *
     * @param origemId  id da origem
     * @param destinoId id do destino
     * @return {@code true} se os dois foram achados
     */
    public boolean selecionar(String origemId, String destinoId) {
        boolean achouOrigem = selecionarNoCombo(comboOrigem, origemId);
        boolean achouDestino = selecionarNoCombo(comboDestino, destinoId);
        return achouOrigem && achouDestino;
    }

    /**
     * Escolha feita pelo mapa: define onde o usuário está.
     *
     * @param id id do ponto
     * @return {@code true} se o ponto está na lista
     */
    public boolean selecionarOrigem(String id) {
        return selecionarNoCombo(comboOrigem, id);
    }

    /**
     * Escolha feita pelo mapa: define para onde o usuário vai.
     *
     * @param id id do ponto
     * @return {@code true} se o ponto está na lista
     */
    public boolean selecionarDestino(String id) {
        return selecionarNoCombo(comboDestino, id);
    }

    /** Volta o destino para "Selecione um ponto…" (o mapa recomeça a escolha pela origem). */
    public void limparDestino() {
        comboDestino.setSelectedIndex(0);
    }

    /**
     * Avisado sempre que origem ou destino mudam, pelo combo, pelo Inverter ou pelo mapa.
     *
     * @param ouvinte chamado a cada mudança
     */
    public void addSelecaoListener(Runnable ouvinte) {
        comboOrigem.addItemListener(e -> ouvinte.run());
        comboDestino.addItemListener(e -> ouvinte.run());
    }

    private static boolean selecionarNoCombo(JComboBox<PontoRede> combo, String id) {
        for (int i = 1; i < combo.getItemCount(); i++) {
            if (combo.getItemAt(i).getId().equals(id)) {
                combo.setSelectedIndex(i);
                return true;
            }
        }
        return false;
    }

    /**
     * Avisa quando o usuário clica em Calcular rota.
     *
     * @param l ouvinte do clique
     */
    public void addCalcularListener(ActionListener l) {
        btnCalcular.addActionListener(l);
    }

    /**
     * Botão Calcular, para a janela torná-lo o botão padrão.
     *
     * @return o botão
     */
    public JButton getBotaoCalcular() { return btnCalcular; }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }
}
