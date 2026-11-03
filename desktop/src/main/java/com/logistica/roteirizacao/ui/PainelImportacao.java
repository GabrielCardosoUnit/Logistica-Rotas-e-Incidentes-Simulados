package com.logistica.roteirizacao.ui;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;

/** Seção 1: escolher pontos.csv e vias.csv e importar a rede. */
public class PainelImportacao extends JPanel {

    private final JTextField campoPontos = new JTextField(30);
    private final JTextField campoVias = new JTextField(30);
    private final JButton btnPontos = new JButton("Escolher…");
    private final JButton btnVias = new JButton("Escolher…");
    private final JButton btnImportar = new JButton("Importar rede");
    private final JLabel erroPontos = Estilo.rotuloErro();
    private final JLabel erroVias = Estilo.rotuloErro();
    private final JLabel statusRede = new JLabel("Rede ainda não importada por esta tela. Clique em Importar rede.");

    private File arquivoPontos;
    private File arquivoVias;
    private File ultimaPasta;

    public PainelImportacao() {
        setBorder(BorderFactory.createTitledBorder("1. Importar rede"));
        setLayout(new GridBagLayout());

        campoPontos.setEditable(false);
        campoVias.setEditable(false);
        campoPontos.setToolTipText("Arquivo CSV com cabeçalho id,nome,latitude,longitude");
        campoVias.setToolTipText("Arquivo CSV com cabeçalho id,origem,destino,distanciaKm,tempoMin,habilitada");

        JLabel lblPontos = new JLabel("pontos.csv:");
        JLabel lblVias = new JLabel("vias.csv:");
        lblPontos.setLabelFor(campoPontos);
        lblVias.setLabelFor(campoVias);
        btnPontos.setMnemonic(KeyEvent.VK_P);
        btnVias.setMnemonic(KeyEvent.VK_V);
        btnImportar.setMnemonic(KeyEvent.VK_I);
        btnPontos.getAccessibleContext().setAccessibleName("Escolher arquivo pontos.csv");
        btnVias.getAccessibleContext().setAccessibleName("Escolher arquivo vias.csv");

        adicionar(lblPontos, 0, 0, 1, 0, GridBagConstraints.NONE);
        adicionar(campoPontos, 1, 0, 1, 1, GridBagConstraints.HORIZONTAL);
        adicionar(btnPontos, 2, 0, 1, 0, GridBagConstraints.NONE);
        adicionar(erroPontos, 1, 1, 2, 1, GridBagConstraints.HORIZONTAL);
        adicionar(lblVias, 0, 2, 1, 0, GridBagConstraints.NONE);
        adicionar(campoVias, 1, 2, 1, 1, GridBagConstraints.HORIZONTAL);
        adicionar(btnVias, 2, 2, 1, 0, GridBagConstraints.NONE);
        adicionar(erroVias, 1, 3, 2, 1, GridBagConstraints.HORIZONTAL);
        adicionar(btnImportar, 0, 4, 1, 0, GridBagConstraints.NONE);
        adicionar(statusRede, 1, 4, 2, 1, GridBagConstraints.HORIZONTAL);

        btnPontos.addActionListener(e -> escolher(true));
        btnVias.addActionListener(e -> escolher(false));
        preCarregarExemplo();
    }

    private void adicionar(java.awt.Component c, int x, int y, int largura, double pesoX, int fill) {
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

    /** Cenário de exemplo pré-carregado: usa dados/tratados/ se estiver por perto. */
    private void preCarregarExemplo() {
        for (String base : new String[] {"dados/tratados", "../dados/tratados"}) {
            File p = new File(base, "pontos.csv");
            File v = new File(base, "vias.csv");
            if (p.isFile() && v.isFile()) {
                definirArquivoPontos(p);
                definirArquivoVias(v);
                ultimaPasta = p.getAbsoluteFile().getParentFile();
                return;
            }
        }
    }

    private void escolher(boolean ehPontos) {
        JFileChooser seletor = new JFileChooser(ultimaPasta);
        seletor.setDialogTitle(ehPontos ? "Escolher pontos.csv" : "Escolher vias.csv");
        seletor.setFileFilter(new FileNameExtensionFilter("Arquivos CSV (*.csv)", "csv"));
        if (seletor.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File f = seletor.getSelectedFile();
            ultimaPasta = f.getParentFile();
            if (ehPontos) {
                definirArquivoPontos(f);
            } else {
                definirArquivoVias(f);
            }
        }
    }

    private void definirArquivoPontos(File f) {
        arquivoPontos = f;
        campoPontos.setText(f.getPath());
        Estilo.limpar(erroPontos);
    }

    private void definirArquivoVias(File f) {
        arquivoVias = f;
        campoVias.setText(f.getPath());
        Estilo.limpar(erroVias);
    }

    /** Confere se os dois arquivos foram escolhidos e existem. Mostra o erro sob o campo. */
    public boolean validar() {
        limparErros();
        boolean ok = true;
        if (arquivoPontos == null) {
            Estilo.mostrarErro(erroPontos, Mensagens.ESCOLHER_PONTOS);
            ok = false;
        } else if (!arquivoPontos.isFile()) {
            Estilo.mostrarErro(erroPontos, "Arquivo não encontrado: " + arquivoPontos.getName()
                    + ". Escolha o arquivo novamente.");
            ok = false;
        }
        if (arquivoVias == null) {
            Estilo.mostrarErro(erroVias, Mensagens.ESCOLHER_VIAS);
            ok = false;
        } else if (!arquivoVias.isFile()) {
            Estilo.mostrarErro(erroVias, "Arquivo não encontrado: " + arquivoVias.getName()
                    + ". Escolha o arquivo novamente.");
            ok = false;
        }
        return ok;
    }

    /** Erro devolvido pela API para um campo ("pontos" ou "vias"). */
    public void setErroCampo(String campo, String mensagem) {
        if ("pontos".equals(campo)) {
            Estilo.mostrarErro(erroPontos, mensagem);
        } else if ("vias".equals(campo)) {
            Estilo.mostrarErro(erroVias, mensagem);
        }
    }

    public void limparErros() {
        Estilo.limpar(erroPontos);
        Estilo.limpar(erroVias);
    }

    public void setStatus(String texto) {
        statusRede.setText(texto);
    }

    public void setHabilitado(boolean habilitado) {
        btnPontos.setEnabled(habilitado);
        btnVias.setEnabled(habilitado);
        btnImportar.setEnabled(habilitado);
    }

    public File getArquivoPontos() { return arquivoPontos; }
    public File getArquivoVias() { return arquivoVias; }

    public void addImportarListener(ActionListener l) {
        btnImportar.addActionListener(l);
    }

    public JButton getBotaoImportar() { return btnImportar; }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }
}
