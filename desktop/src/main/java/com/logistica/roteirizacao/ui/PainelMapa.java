package com.logistica.roteirizacao.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

/**
 * Mapa esquemático interativo: desenha os pontos nas posições (latitude/longitude) e as vias como
 * linhas retas, destaca a rota calculada e mostra onde o usuário está (origem) e para onde vai
 * (destino). É um desenho, não um mapa de ruas (a rede é simulada).
 *
 * <p>Interação: roda do mouse dá zoom no ponto sob o cursor; arrastar move o mapa; clicar num
 * ponto avisa quem estiver ouvindo ({@link #setAoClicarPonto}) para escolher origem/destino;
 * duplo clique fora dos pontos, o botão "Ajustar" ou a tecla 0 voltam à vista da rede inteira.
 * Teclado: + e - para zoom, setas para mover.
 *
 * <p>A rota não é indicada só por cor: tem traço mais grosso, círculos maiores, números de ordem,
 * marcador próprio na origem e no destino, uma caixa com o nome dos lugares e uma legenda.
 *
 * @serial exclude
 */
public class PainelMapa extends JPanel {

    static final double ZOOM_MINIMO = 1.0;
    static final double ZOOM_MAXIMO = 8.0;
    private static final double PASSO_ZOOM = 1.25;
    private static final int PASSO_TECLADO = 40;
    private static final int RAIO_CLIQUE = 10;
    private static final int ARRASTE_MINIMO = 4;

    private static final int MARGEM = 28;
    private static final Color COR_VIA = new Color(0x9E, 0x9E, 0x9E);
    private static final Color COR_PONTO = new Color(0x37, 0x47, 0x4F);
    private static final Color COR_ROTA = new Color(0xD8, 0x43, 0x15);
    private static final Color COR_ORIGEM = new Color(0x15, 0x65, 0xC0);
    private static final Color COR_DESTINO = new Color(0xB7, 0x1C, 0x1C);
    private static final Color COR_FOCO = new Color(0x15, 0x65, 0xC0, 90);
    private static final Stroke TRACO_VIA_BLOQUEADA = new BasicStroke(1.6f, BasicStroke.CAP_BUTT,
            BasicStroke.JOIN_MITER, 10f, new float[] {5f, 4f}, 0f);

    // fundo urbano (decorativo) e vias da rede em estilo de estrada
    private static final Color COR_QUARTEIRAO = new Color(0xEC, 0xEE, 0xF1);
    private static final Color COR_RUA = Color.WHITE;
    private static final Color COR_BORDA_RUA = new Color(0xD9, 0xDD, 0xE2);
    private static final Color COR_PRACA = new Color(0xD7, 0xEB, 0xD2);
    private static final Color COR_BORDA_VIA = new Color(0xC9, 0xA8, 0x4C);
    private static final Color COR_VIA_MIOLO = new Color(0xFF, 0xE9, 0xA8);
    private static final Color COR_VIA_BLOQUEADA = new Color(0xE3, 0xE5, 0xE8);
    private static final Color COR_TRACEJADO = new Color(0x8A, 0x93, 0x9C);
    private static final double ESPACO_QUADRA = 34;
    /** Acréscimo na distância por pixel além da ponta de uma via (divide os cruzamentos por ângulo). */
    private static final double PENALIDADE_PONTA = 0.6;
    private static final float LARGURA_RUA = 3.2f;
    private static final float LARGURA_AVENIDA = 6f;

    private final List<PontoRede> pontos = new ArrayList<>();
    private final List<ViaRede> vias = new ArrayList<>();
    private final List<String> rota = new ArrayList<>();
    private final Set<String> viasComIncidente = new HashSet<>();
    private String origemId;
    private String destinoId;
    private String pontoSobOMouse;
    /** Fundo urbano já desenhado e a vista (zoom, deslocamento, tamanho) em que foi feito. */
    private BufferedImage fundoCache;
    private double fundoZoom;
    private double fundoDeslocX;
    private double fundoDeslocY;
    private boolean refazerFundo;
    /**
     * Refazer o fundo leva ~0,1 s: enquanto o usuário arrasta ou dá zoom, o fundo anterior é só
     * deslocado/esticado (instantâneo) e o exato é refeito quando ele para de mexer por um instante.
     */
    private final javax.swing.Timer timerFundo = new javax.swing.Timer(150, e -> {
        refazerFundo = true;
        repaint();
    });

    /** Vista: posição na tela = posição base × zoom + deslocamento. */
    private double zoom = 1.0;
    private double deslocX;
    private double deslocY;

    private Consumer<String> aoClicarPonto = id -> { };

    /** Monta o mapa vazio, com os controles de mouse e teclado. */
    public PainelMapa() {
        super(new FlowLayout(FlowLayout.RIGHT, 4, 2)); // os botões de zoom ficam no canto superior direito
        setBorder(BorderFactory.createTitledBorder("Mapa (role para zoom, arraste para mover, clique para escolher)"));
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(620, 440));
        setToolTipText(""); // liga o tooltip; o texto vem de getToolTipText(MouseEvent)
        setFocusable(true);
        getAccessibleContext().setAccessibleName("Mapa esquemático da rede e da rota calculada");

        add(botao("+", "Aproximar (tecla +)", e -> zoomNoCentro(PASSO_ZOOM)));
        add(botao("−", "Afastar (tecla -)", e -> zoomNoCentro(1 / PASSO_ZOOM)));
        add(botao("Ajustar", "Mostrar a rede inteira (tecla 0 ou duplo clique)", e -> ajustarVista()));

        ControleMouse mouse = new ControleMouse();
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
        addMouseWheelListener(mouse);
        ligarTeclado();
    }

    private static JButton botao(String texto, String dica, java.awt.event.ActionListener acao) {
        JButton b = new JButton(texto);
        b.setToolTipText(dica);
        b.setMargin(new Insets(1, 8, 1, 8));
        b.setFocusable(false);
        b.addActionListener(acao);
        return b;
    }

    private void ligarTeclado() {
        atalho("PLUS", "ADD", "EQUALS", () -> zoomNoCentro(PASSO_ZOOM));
        atalho("MINUS", "SUBTRACT", null, () -> zoomNoCentro(1 / PASSO_ZOOM));
        atalho("0", "NUMPAD0", null, this::ajustarVista);
        atalho("LEFT", null, null, () -> mover(PASSO_TECLADO, 0));
        atalho("RIGHT", null, null, () -> mover(-PASSO_TECLADO, 0));
        atalho("UP", null, null, () -> mover(0, PASSO_TECLADO));
        atalho("DOWN", null, null, () -> mover(0, -PASSO_TECLADO));
    }

    private void atalho(String tecla1, String tecla2, String tecla3, Runnable acao) {
        String nome = "mapa-" + tecla1;
        for (String t : new String[] {tecla1, tecla2, tecla3}) {
            if (t != null) {
                getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(t), nome);
            }
        }
        getActionMap().put(nome, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                acao.run();
            }
        });
    }

    // ---- dados ---------------------------------------------------------------------------------

    /**
     * Define a rede a desenhar, apaga rota e seleção e volta à vista da rede inteira.
     *
     * @param novosPontos pontos da rede
     * @param novasVias   vias da rede
     */
    public void setRede(List<PontoRede> novosPontos, List<ViaRede> novasVias) {
        pontos.clear();
        pontos.addAll(novosPontos);
        vias.clear();
        vias.addAll(novasVias);
        rota.clear();
        viasComIncidente.clear(); // reimportar a rede apaga os incidentes na API
        origemId = null;
        destinoId = null;
        fundoCache = null; // rede nova: o fundo é refeito na hora
        ajustarVista();
    }

    /**
     * Desenha a via como bloqueada (tracejada) depois de um incidente registrado na API.
     *
     * @param viaId id da via
     */
    public void marcarViaBloqueada(String viaId) {
        viasComIncidente.add(viaId);
        repaint();
    }

    /**
     * Volta a desenhar a via normal depois que o incidente foi encerrado na API.
     *
     * @param viaId id da via
     */
    public void marcarViaDesbloqueada(String viaId) {
        viasComIncidente.remove(viaId);
        repaint();
    }

    boolean estaBloqueada(ViaRede via) {
        return !via.isHabilitada() || viasComIncidente.contains(via.getId());
    }

    /**
     * Destaca a rota: ids dos pontos na ordem do percurso.
     *
     * @param idsDosPontos pontos da rota, da origem ao destino
     */
    public void setRota(List<String> idsDosPontos) {
        rota.clear();
        rota.addAll(idsDosPontos);
        repaint();
    }

    /** Tira o destaque da rota. */
    public void limparRota() {
        rota.clear();
        repaint();
    }

    /**
     * Onde o usuário está (origem) e para onde vai (destino); null quando ainda não escolhido.
     *
     * @param origemId  id da origem, ou {@code null}
     * @param destinoId id do destino, ou {@code null}
     */
    public void setSelecao(String origemId, String destinoId) {
        this.origemId = origemId;
        this.destinoId = destinoId;
        repaint();
    }

    /**
     * Recebe o id do ponto clicado no mapa.
     *
     * @param ouvinte chamado com o id do ponto
     */
    public void setAoClicarPonto(Consumer<String> ouvinte) {
        this.aoClicarPonto = Objects.requireNonNull(ouvinte);
    }

    // ---- vista (zoom e deslocamento) -----------------------------------------------------------

    /** Volta ao zoom 1, com a rede inteira enquadrada. */
    public void ajustarVista() {
        zoom = 1.0;
        deslocX = 0;
        deslocY = 0;
        repaint();
    }

    /** Multiplica o zoom por {@code fator} mantendo fixo o ponto da tela {@code ancora}. */
    void zoomEm(double fator, Point2D ancora) {
        double novo = Math.max(ZOOM_MINIMO, Math.min(ZOOM_MAXIMO, zoom * fator));
        if (novo == zoom) {
            return;
        }
        // ponto "do mundo" sob a âncora continua sob ela depois do zoom
        double mundoX = (ancora.getX() - deslocX) / zoom;
        double mundoY = (ancora.getY() - deslocY) / zoom;
        zoom = novo;
        deslocX = ancora.getX() - mundoX * zoom;
        deslocY = ancora.getY() - mundoY * zoom;
        if (zoom == ZOOM_MINIMO) {
            deslocX = 0; // no zoom mínimo a rede volta a ficar enquadrada
            deslocY = 0;
        }
        repaint();
    }

    private void zoomNoCentro(double fator) {
        zoomEm(fator, new Point2D.Double(getWidth() / 2.0, getHeight() / 2.0));
    }

    private void mover(double dx, double dy) {
        deslocX += dx;
        deslocY += dy;
        repaint();
    }

    double getZoom() {
        return zoom;
    }

    // ---- projeção: latitude/longitude -> pixels ------------------------------------------------

    /** Posição de cada ponto na tela, já com zoom e deslocamento aplicados. */
    Map<String, Point2D.Double> calcularPosicoes() {
        Map<String, Point2D.Double> pos = new LinkedHashMap<>();
        if (pontos.isEmpty()) {
            return pos;
        }
        double minLat = Double.MAX_VALUE, maxLat = -Double.MAX_VALUE;
        double minLon = Double.MAX_VALUE, maxLon = -Double.MAX_VALUE;
        for (PontoRede p : pontos) {
            minLat = Math.min(minLat, p.getLatitude());
            maxLat = Math.max(maxLat, p.getLatitude());
            minLon = Math.min(minLon, p.getLongitude());
            maxLon = Math.max(maxLon, p.getLongitude());
        }
        // 1 grau de longitude é menor que 1 grau de latitude: corrige pela latitude média
        double fatorLon = Math.cos(Math.toRadians((minLat + maxLat) / 2));
        double largura = Math.max((maxLon - minLon) * fatorLon, 1e-9);
        double altura = Math.max(maxLat - minLat, 1e-9);

        Insets in = getInsets();
        double areaL = getWidth() - in.left - in.right - 2.0 * MARGEM;
        double areaA = getHeight() - in.top - in.bottom - 2.0 * MARGEM;
        double escala = Math.max(Math.min(areaL / largura, areaA / altura), 0.0);
        double sobraX = (areaL - largura * escala) / 2.0;
        double sobraY = (areaA - altura * escala) / 2.0;

        for (PontoRede p : pontos) {
            double x = in.left + MARGEM + sobraX + (p.getLongitude() - minLon) * fatorLon * escala;
            double y = in.top + MARGEM + sobraY + (maxLat - p.getLatitude()) * escala;
            pos.put(p.getId(), new Point2D.Double(x * zoom + deslocX, y * zoom + deslocY));
        }
        return pos;
    }

    /** Id do ponto mais próximo de {@code tela} dentro do raio de clique, ou null. */
    String pontoEm(Point2D tela) {
        String maisPerto = null;
        double menor = RAIO_CLIQUE;
        for (Map.Entry<String, Point2D.Double> e : calcularPosicoes().entrySet()) {
            double d = e.getValue().distance(tela);
            if (d <= menor) {
                menor = d;
                maisPerto = e.getKey();
            }
        }
        return maisPerto;
    }

    private PontoRede ponto(String id) {
        for (PontoRede p : pontos) {
            if (p.getId().equals(id)) {
                return p;
            }
        }
        return null;
    }

    // ---- desenho -------------------------------------------------------------------------------

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        Insets in = getInsets();
        g2.clipRect(in.left, in.top, getWidth() - in.left - in.right, getHeight() - in.top - in.bottom);
        try {
            if (pontos.isEmpty()) {
                desenharMensagemVazia(g2);
                return;
            }
            Map<String, Point2D.Double> pos = calcularPosicoes();
            desenharFundoCidade(g2, pos);
            desenharVias(g2, pos);
            desenharRota(g2, pos);
            desenharPontos(g2, pos);
            desenharPainelSituacao(g2);
            desenharLegenda(g2);
        } finally {
            g2.dispose();
        }
    }

    private void desenharMensagemVazia(Graphics2D g2) {
        String texto = "Importe a rede para ver o mapa.";
        g2.setColor(Color.GRAY);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(texto, (getWidth() - fm.stringWidth(texto)) / 2, getHeight() / 2);
    }

    // ---- fundo urbano (só visual) --------------------------------------------------------------

    /**
     * Quarteirões, ruas, avenidas e pracinhas de fundo, no estilo de um mapa de ruas. É só
     * decoração: não são vias da rede e não entram em nenhum cálculo.
     *
     * <p>Para combinar com a rede, cada pixel "pertence" à via mais próxima e usa a direção
     * dela: as ruas correm paralelas à via (a própria via é uma delas), as transversais são
     * perpendiculares e, onde duas regiões se encontram, nasce uma rua de divisa. O resultado é
     * guardado numa imagem e só é refeito quando muda o tamanho, o zoom, o arraste ou a rede.
     */
    private void desenharFundoCidade(Graphics2D g2, Map<String, Point2D.Double> pos) {
        Insets in = getInsets();
        int largura = getWidth() - in.left - in.right;
        int altura = getHeight() - in.top - in.bottom;
        if (largura <= 0 || altura <= 0) {
            return;
        }
        boolean mesmaVista = fundoCache != null && fundoZoom == zoom && fundoDeslocX == deslocX
                && fundoDeslocY == deslocY && fundoCache.getWidth() == largura && fundoCache.getHeight() == altura;
        if (!mesmaVista && fundoCache != null && !refazerFundo) {
            desenharFundoProvisorio(g2, in, largura, altura);
            timerFundo.restart(); // ainda mexendo: o exato vem quando o usuário parar
            return;
        }
        if (!mesmaVista) {
            fundoCache = gerarFundo(largura, altura, in, pos);
            fundoZoom = zoom;
            fundoDeslocX = deslocX;
            fundoDeslocY = deslocY;
            refazerFundo = false;
        }
        g2.drawImage(fundoCache, in.left, in.top, null);
    }

    /** O último fundo pronto, levado para a vista atual: tela nova = (tela antiga - desl.) × razão + desl. */
    private void desenharFundoProvisorio(Graphics2D g2, Insets in, int largura, int altura) {
        g2.setColor(COR_QUARTEIRAO);
        g2.fillRect(in.left, in.top, largura, altura);
        double razao = zoom / fundoZoom;
        Graphics2D c = (Graphics2D) g2.create();
        try {
            c.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            c.translate(deslocX - fundoDeslocX * razao, deslocY - fundoDeslocY * razao);
            c.scale(razao, razao);
            c.drawImage(fundoCache, in.left, in.top, null);
        } finally {
            c.dispose();
        }
    }

    private BufferedImage gerarFundo(int largura, int altura, Insets in, Map<String, Point2D.Double> pos) {
        // segmentos das vias na tela: início, direção unitária e comprimento
        List<double[]> segs = new ArrayList<>();
        for (ViaRede v : vias) {
            Point2D.Double a = pos.get(v.getOrigemId());
            Point2D.Double b = pos.get(v.getDestinoId());
            if (a == null || b == null || a.distance(b) < 1) {
                continue;
            }
            double comp = a.distance(b);
            segs.add(new double[] {a.x, a.y, (b.x - a.x) / comp, (b.y - a.y) / comp, comp});
        }
        BufferedImage img = new BufferedImage(largura, altura, BufferedImage.TYPE_INT_RGB);
        int[] px = ((java.awt.image.DataBufferInt) img.getRaster().getDataBuffer()).getData();
        if (segs.isEmpty()) {
            java.util.Arrays.fill(px, COR_QUARTEIRAO.getRGB());
            return img;
        }

        double espS = ESPACO_QUADRA * zoom;          // distância entre ruas paralelas à via
        double espT = ESPACO_QUADRA * 1.25 * zoom;   // distância entre transversais
        double meiaRua = LARGURA_RUA / 2 * zoom;
        double meiaAvenida = LARGURA_AVENIDA / 2 * zoom;
        double borda = 0.8 + 0.25 * zoom;            // contorno cinza das ruas, em pixels

        for (int j = 0; j < altura; j++) {
            double y = in.top + j + 0.5;
            for (int i = 0; i < largura; i++) {
                double x = in.left + i + 0.5;

                // Via mais próxima e a segunda mais próxima (para a rua de divisa). Além da
                // extremidade de uma via, a distância ganha um acréscimo proporcional ao quanto o
                // pixel passou da ponta: perto de um cruzamento, a região é dividida pelo ângulo
                // (cada via fica com o setor "à sua frente") e não vira um leque indefinido.
                double melhor = Double.MAX_VALUE, segunda = Double.MAX_VALUE;
                double g1x = 0, g1y = 0, g2x = 0, g2y = 0; // gradiente das duas distâncias
                int qual = 0;
                for (int k = 0; k < segs.size(); k++) {
                    double[] sg = segs.get(k);
                    double tBruto = (x - sg[0]) * sg[2] + (y - sg[1]) * sg[3];
                    double tc = Math.max(0, Math.min(sg[4], tBruto));
                    double passou = Math.abs(tBruto - tc);
                    double ex = x - (sg[0] + sg[2] * tc), ey = y - (sg[1] + sg[3] * tc);
                    double e = Math.sqrt(ex * ex + ey * ey);
                    double d = e + PENALIDADE_PONTA * passou;
                    if (d < segunda) {
                        double sinal = tBruto < 0 ? -1 : tBruto > sg[4] ? 1 : 0;
                        double gx = (e > 1e-9 ? ex / e : 0) + PENALIDADE_PONTA * sinal * sg[2];
                        double gy = (e > 1e-9 ? ey / e : 0) + PENALIDADE_PONTA * sinal * sg[3];
                        if (d < melhor) {
                            segunda = melhor;
                            g2x = g1x;
                            g2y = g1y;
                            melhor = d;
                            g1x = gx;
                            g1y = gy;
                            qual = k;
                        } else {
                            segunda = d;
                            g2x = gx;
                            g2y = gy;
                        }
                    }
                }
                double[] sg = segs.get(qual);
                // coordenadas no "sistema" da via: t ao longo dela, s para o lado
                double t = (x - sg[0]) * sg[2] + (y - sg[1]) * sg[3];
                double s = -(x - sg[0]) * sg[3] + (y - sg[1]) * sg[2];

                long ks = Math.round(s / espS);
                double ds = Math.abs(s - ks * espS);
                double meiaS = Math.floorMod(ks, 5) == 0 ? meiaAvenida : meiaRua; // a cada 5, avenida

                long kt = Math.round(t / espT);
                double dt = Math.abs(t - kt * espT);
                long faixa = (long) Math.floor(s / espS); // entre quais paralelas está o pixel
                boolean transversal = ruido(qual * 1_000_003L + kt, faixa) > -0.55; // algumas faltam

                double miolo = cobertura(ds, meiaS);
                double contorno = cobertura(ds, meiaS + borda);
                if (transversal) {
                    miolo = Math.max(miolo, cobertura(dt, meiaRua));
                    contorno = Math.max(contorno, cobertura(dt, meiaRua + borda));
                }
                if (segunda < Double.MAX_VALUE) {
                    // distância até a fronteira entre as duas regiões: diferença das distâncias
                    // dividida pela "inclinação" dessa diferença (assim a divisa tem largura certa)
                    double inclinacao = Math.hypot(g2x - g1x, g2y - g1y);
                    if (inclinacao > 0.08) {
                        double divisa = (segunda - melhor) / inclinacao;
                        miolo = Math.max(miolo, cobertura(divisa, meiaRua));
                        contorno = Math.max(contorno, cobertura(divisa, meiaRua + borda));
                    }
                }

                long quadraT = (long) Math.floor(t / espT);
                Color quadra = ruido(qual * 7919L + faixa * 131L, quadraT) > 0.86 ? COR_PRACA : COR_QUARTEIRAO;
                px[j * largura + i] = misturar(misturar(quadra.getRGB(), COR_BORDA_RUA.getRGB(), contorno),
                        COR_RUA.getRGB(), miolo);
            }
        }
        return img;
    }

    /** Quanto do pixel (0 a 1) cai dentro de uma faixa de meia-largura {@code meia}: borda suavizada. */
    private static double cobertura(double distancia, double meia) {
        return Math.max(0, Math.min(1, meia - distancia + 0.5));
    }

    private static int misturar(int rgbA, int rgbB, double f) {
        if (f <= 0) {
            return rgbA;
        }
        if (f >= 1) {
            return rgbB;
        }
        int r = (int) Math.round(((rgbA >> 16) & 0xFF) * (1 - f) + ((rgbB >> 16) & 0xFF) * f);
        int g = (int) Math.round(((rgbA >> 8) & 0xFF) * (1 - f) + ((rgbB >> 8) & 0xFF) * f);
        int b = (int) Math.round((rgbA & 0xFF) * (1 - f) + (rgbB & 0xFF) * f);
        return (r << 16) | (g << 8) | b;
    }

    /** Número pseudoaleatório em [-1, 1], sempre o mesmo para a mesma entrada. */
    private static double ruido(long a, long b) {
        long h = a * 0x9E3779B97F4A7C15L ^ (b + 0x632BE59BD9B4E019L) * 0xC2B2AE3D27D4EB4FL;
        h ^= h >>> 31;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 29;
        return ((h >>> 11) / (double) (1L << 53)) * 2 - 1;
    }

    // ---- vias da rede --------------------------------------------------------------------------

    /**
     * Vias da rede como estradas: borda escura e miolo amarelo-claro, para se destacarem das
     * ruas brancas do fundo. Bloqueada/desabilitada: miolo cinza com tracejado no centro.
     */
    private void desenharVias(Graphics2D g2, Map<String, Point2D.Double> pos) {
        List<Line2D.Double> livres = new ArrayList<>();
        List<Line2D.Double> bloqueadas = new ArrayList<>();
        for (ViaRede v : vias) {
            Point2D a = pos.get(v.getOrigemId());
            Point2D b = pos.get(v.getDestinoId());
            if (a == null || b == null) {
                continue;
            }
            (estaBloqueada(v) ? bloqueadas : livres).add(new Line2D.Double(a, b));
        }
        g2.setStroke(new BasicStroke(8.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(COR_BORDA_VIA);
        livres.forEach(g2::draw);
        bloqueadas.forEach(g2::draw);
        g2.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(COR_VIA_MIOLO);
        livres.forEach(g2::draw);
        g2.setColor(COR_VIA_BLOQUEADA);
        bloqueadas.forEach(g2::draw);
        g2.setStroke(TRACO_VIA_BLOQUEADA);
        g2.setColor(COR_TRACEJADO);
        bloqueadas.forEach(g2::draw);
    }

    private void desenharRota(Graphics2D g2, Map<String, Point2D.Double> pos) {
        if (rota.size() < 2) {
            return;
        }
        Path2D.Double caminho = new Path2D.Double();
        boolean primeiro = true;
        for (String id : rota) {
            Point2D p = pos.get(id);
            if (p == null) {
                continue;
            }
            if (primeiro) {
                caminho.moveTo(p.getX(), p.getY());
                primeiro = false;
            } else {
                caminho.lineTo(p.getX(), p.getY());
            }
        }
        g2.setStroke(new BasicStroke(9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(Color.WHITE); // contorno branco: o traço continua visível sobre as vias cinza
        g2.draw(caminho);
        g2.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(COR_ROTA);
        g2.draw(caminho);
        desenharSetasDeSentido(g2, pos);
    }

    /** Pequenas setas no meio de cada trecho da rota, indicando o sentido origem → destino. */
    private void desenharSetasDeSentido(Graphics2D g2, Map<String, Point2D.Double> pos) {
        g2.setColor(Color.WHITE);
        for (int i = 0; i + 1 < rota.size(); i++) {
            Point2D.Double a = pos.get(rota.get(i));
            Point2D.Double b = pos.get(rota.get(i + 1));
            if (a == null || b == null || a.distance(b) < 30) {
                continue;
            }
            double ang = Math.atan2(b.y - a.y, b.x - a.x);
            double mx = (a.x + b.x) / 2, my = (a.y + b.y) / 2;
            Path2D.Double seta = new Path2D.Double();
            seta.moveTo(mx + 5 * Math.cos(ang), my + 5 * Math.sin(ang));
            seta.lineTo(mx + 4 * Math.cos(ang + 2.4), my + 4 * Math.sin(ang + 2.4));
            seta.lineTo(mx + 4 * Math.cos(ang - 2.4), my + 4 * Math.sin(ang - 2.4));
            seta.closePath();
            g2.fill(seta);
        }
    }

    private void desenharPontos(Graphics2D g2, Map<String, Point2D.Double> pos) {
        Font normal = g2.getFont().deriveFont(Font.PLAIN, 11f);
        Font negrito = g2.getFont().deriveFont(Font.BOLD, 12f);

        for (PontoRede p : pontos) {
            Point2D.Double c = pos.get(p.getId());
            int ordem = rota.indexOf(p.getId());
            boolean naRota = ordem >= 0;
            boolean ehOrigem = p.getId().equals(origemId);
            boolean ehDestino = p.getId().equals(destinoId);

            if (p.getId().equals(pontoSobOMouse)) {
                g2.setColor(COR_FOCO); // destaque de "dá para clicar aqui"
                g2.fill(new Ellipse2D.Double(c.x - 12, c.y - 12, 24, 24));
            }

            if (ehOrigem) {
                desenharVoceEstaAqui(g2, c);
            } else if (ehDestino) {
                desenharMarcadorDestino(g2, c);
            } else if (naRota) {
                g2.setColor(COR_ROTA);
                g2.fill(new Ellipse2D.Double(c.x - 6, c.y - 6, 12, 12));
            } else {
                g2.setColor(COR_PONTO);
                g2.fill(new Ellipse2D.Double(c.x - 4, c.y - 4, 8, 8));
            }

            // Origem e destino só ganham cor própria no rótulo; os nomes ficam na caixa do canto.
            boolean destaque = naRota || ehOrigem || ehDestino;
            g2.setFont(destaque ? negrito : normal);
            String rotulo = naRota ? (ordem + 1) + ". " + p.getId() : p.getId();
            textoComContorno(g2, rotulo, (float) c.x + 12, (float) c.y - 10,
                    ehOrigem ? COR_ORIGEM : ehDestino ? COR_DESTINO : naRota ? COR_ROTA.darker() : COR_PONTO);
        }
    }

    /** Texto com contorno branco, como nos mapas de ruas: continua legível sobre ruas e estradas. */
    private static void textoComContorno(Graphics2D g2, String texto, float x, float y, Color cor) {
        java.awt.Shape forma = g2.getFont().createGlyphVector(g2.getFontRenderContext(), texto).getOutline(x, y);
        g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(Color.WHITE);
        g2.draw(forma);
        g2.setColor(cor);
        g2.fill(forma);
    }

    /** Ponto azul com halo, como o "você está aqui" de aplicativos de navegação. */
    private static void desenharVoceEstaAqui(Graphics2D g2, Point2D.Double c) {
        g2.setColor(new Color(0x15, 0x65, 0xC0, 60));
        g2.fill(new Ellipse2D.Double(c.x - 14, c.y - 14, 28, 28));
        g2.setColor(Color.WHITE);
        g2.fill(new Ellipse2D.Double(c.x - 9, c.y - 9, 18, 18));
        g2.setColor(COR_ORIGEM);
        g2.fill(new Ellipse2D.Double(c.x - 7, c.y - 7, 14, 14));
    }

    /** Alfinete vermelho: a ponta fica exatamente sobre o destino. */
    private static void desenharMarcadorDestino(Graphics2D g2, Point2D.Double c) {
        Path2D.Double alfinete = new Path2D.Double();
        alfinete.moveTo(c.x, c.y);
        alfinete.curveTo(c.x - 4, c.y - 8, c.x - 10, c.y - 12, c.x - 10, c.y - 20);
        alfinete.curveTo(c.x - 10, c.y - 27, c.x - 5, c.y - 31, c.x, c.y - 31);
        alfinete.curveTo(c.x + 5, c.y - 31, c.x + 10, c.y - 27, c.x + 10, c.y - 20);
        alfinete.curveTo(c.x + 10, c.y - 12, c.x + 4, c.y - 8, c.x, c.y);
        alfinete.closePath();
        g2.setColor(COR_DESTINO);
        g2.fill(alfinete);
        g2.setColor(Color.WHITE);
        g2.fill(new Ellipse2D.Double(c.x - 4, c.y - 24, 8, 8));
    }

    private String nomeCurto(String id) {
        PontoRede p = ponto(id);
        String nome = p == null ? id : id + " — " + p.getNome();
        return nome.length() > 38 ? nome.substring(0, 37) + "…" : nome;
    }

    /** Caixa no canto superior esquerdo: de onde, para onde e o próximo passo. */
    private void desenharPainelSituacao(Graphics2D g2) {
        String linha1 = "Você está em: " + (origemId == null ? "—" : nomeCurto(origemId));
        String linha2 = "Para onde vai: " + (destinoId == null ? "—" : nomeCurto(destinoId));
        String linha3;
        if (origemId == null) {
            linha3 = "Clique num ponto para dizer onde você está.";
        } else if (destinoId == null) {
            linha3 = "Agora clique no ponto para onde vai.";
        } else {
            linha3 = "Clique noutro ponto para começar de novo.";
        }
        Insets in = getInsets();
        Font f = g2.getFont().deriveFont(Font.PLAIN, 11f);
        Font fb = f.deriveFont(Font.BOLD);
        FontMetrics fm = g2.getFontMetrics(fb);
        int largura = Math.max(fm.stringWidth(linha1), Math.max(fm.stringWidth(linha2), fm.stringWidth(linha3))) + 26;
        int x = in.left + 8;
        int y = in.top + 6;
        int h = fm.getHeight();

        g2.setStroke(new BasicStroke(1f));
        g2.setColor(new Color(255, 255, 255, 230));
        g2.fillRect(x, y, largura, 3 * h + 10);
        g2.setColor(COR_VIA);
        g2.drawRect(x, y, largura, 3 * h + 10);

        g2.setColor(COR_ORIGEM);
        g2.fill(new Ellipse2D.Double(x + 7, y + 5 + h / 2.0 - 4, 8, 8));
        g2.setColor(COR_DESTINO);
        g2.fill(new Ellipse2D.Double(x + 7, y + 5 + h * 1.5 - 4, 8, 8));
        g2.setFont(fb);
        g2.setColor(Color.DARK_GRAY);
        g2.drawString(linha1, x + 20, y + 5 + fm.getAscent());
        g2.drawString(linha2, x + 20, y + 5 + h + fm.getAscent());
        g2.setFont(f);
        g2.setColor(Color.GRAY);
        g2.drawString(linha3, x + 20, y + 5 + 2 * h + fm.getAscent());
    }

    private void desenharLegenda(Graphics2D g2) {
        Insets in = getInsets();
        int x = in.left + 10;
        int y = getHeight() - in.bottom - 8 - 5 * 18;
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 11f));

        g2.setStroke(new BasicStroke(1f)); // a borda da caixa não herda a espessura da rota
        g2.setColor(new Color(255, 255, 255, 220));
        g2.fillRect(x - 4, y - 14, 178, 5 * 18 + 8);
        g2.setColor(COR_VIA);
        g2.drawRect(x - 4, y - 14, 178, 5 * 18 + 8);

        legendaEstrada(g2, x, y, false, "Via da rede");
        legendaEstrada(g2, x, y + 18, true, "Via bloqueada ou desabilitada");
        legendaLinha(g2, x, y + 36, new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND),
                COR_ROTA, "Rota calculada");
        g2.setColor(COR_ORIGEM);
        g2.fill(new Ellipse2D.Double(x + 7, y + 45, 11, 11));
        g2.setColor(Color.DARK_GRAY);
        g2.drawString("Você está aqui (origem)", x + 32, y + 56);
        g2.setColor(COR_DESTINO);
        Path2D.Double l = new Path2D.Double();
        l.moveTo(x + 12.5, y + 76);
        l.lineTo(x + 7, y + 67);
        l.lineTo(x + 7, y + 64);
        l.lineTo(x + 18, y + 64);
        l.lineTo(x + 18, y + 67);
        l.closePath();
        g2.fill(l);
        g2.setColor(Color.DARK_GRAY);
        g2.drawString("Destino", x + 32, y + 74);
    }

    /** Amostra de via no mesmo estilo de estrada usado no mapa (borda, miolo e tracejado). */
    private void legendaEstrada(Graphics2D g2, int x, int y, boolean bloqueada, String texto) {
        Line2D.Double amostra = new Line2D.Double(x + 2, y - 4, x + 22, y - 4);
        g2.setStroke(new BasicStroke(8.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(COR_BORDA_VIA);
        g2.draw(amostra);
        g2.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(bloqueada ? COR_VIA_BLOQUEADA : COR_VIA_MIOLO);
        g2.draw(amostra);
        if (bloqueada) {
            g2.setStroke(TRACO_VIA_BLOQUEADA);
            g2.setColor(COR_TRACEJADO);
            g2.draw(amostra);
        }
        g2.setColor(Color.DARK_GRAY);
        g2.drawString(texto, x + 32, y);
    }

    private void legendaLinha(Graphics2D g2, int x, int y, Stroke traco, Color cor, String texto) {
        g2.setStroke(traco);
        g2.setColor(cor);
        g2.draw(new Line2D.Double(x, y - 4, x + 24, y - 4));
        g2.setColor(Color.DARK_GRAY);
        g2.drawString(texto, x + 32, y);
    }

    /** Ao passar o mouse sobre um ponto, mostra id e nome e o que o clique fará. */
    @Override
    public String getToolTipText(MouseEvent e) {
        PontoRede p = ponto(pontoEm(e.getPoint()));
        if (p == null) {
            return null;
        }
        String acao = origemId == null || destinoId != null ? "Clique: você está aqui" : "Clique: ir para cá";
        return "<html><b>" + p + "</b><br>" + acao + "</html>";
    }

    /** Roda = zoom no cursor; arrastar = mover; clique num ponto = escolher; duplo clique fora = ajustar. */
    private final class ControleMouse extends MouseAdapter {
        private Point inicio;
        private double deslocXInicial;
        private double deslocYInicial;
        private boolean arrastando;

        @Override
        public void mousePressed(MouseEvent e) {
            requestFocusInWindow(); // libera os atalhos de teclado do mapa
            inicio = e.getPoint();
            deslocXInicial = deslocX;
            deslocYInicial = deslocY;
            arrastando = false;
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (inicio == null) {
                return;
            }
            if (!arrastando && inicio.distance(e.getPoint()) < ARRASTE_MINIMO) {
                return; // tremida do clique não conta como arraste
            }
            arrastando = true;
            setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
            deslocX = deslocXInicial + e.getX() - inicio.x;
            deslocY = deslocYInicial + e.getY() - inicio.y;
            repaint();
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            boolean foiClique = !arrastando && inicio != null;
            inicio = null;
            arrastando = false;
            atualizarCursor(e);
            if (!foiClique || e.getButton() != MouseEvent.BUTTON1) {
                return;
            }
            String id = pontoEm(e.getPoint());
            if (id != null) {
                aoClicarPonto.accept(id);
            } else if (e.getClickCount() == 2) {
                ajustarVista();
            }
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            atualizarCursor(e);
        }

        @Override
        public void mouseExited(MouseEvent e) {
            if (pontoSobOMouse != null) {
                pontoSobOMouse = null;
                repaint();
            }
        }

        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            double fator = Math.pow(PASSO_ZOOM, -e.getPreciseWheelRotation());
            zoomEm(fator, e.getPoint());
        }

        private void atualizarCursor(MouseEvent e) {
            String id = pontoEm(e.getPoint());
            setCursor(Cursor.getPredefinedCursor(id != null ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
            if (!Objects.equals(id, pontoSobOMouse)) {
                pontoSobOMouse = id;
                repaint();
            }
        }
    }
}
