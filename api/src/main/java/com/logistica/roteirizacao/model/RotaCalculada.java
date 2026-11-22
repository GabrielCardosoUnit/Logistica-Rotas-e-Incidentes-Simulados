package com.logistica.roteirizacao.model;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

/**
 * Registro persistido de cada pedido de rota, inclusive as tentativas sem
 * caminho. Guarda os ids (e não referências) dos pontos e vias para o
 * histórico continuar válido depois que a rede for reimportada.
 */
@Entity
@Table(name = "rota")
public class RotaCalculada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "origem_id", nullable = false, length = 20)
    private String origemId;

    @Column(name = "destino_id", nullable = false, length = 20)
    private String destinoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoCriterio criterio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusRota status;

    @ElementCollection
    @CollectionTable(name = "rota_ponto", joinColumns = @JoinColumn(name = "rota_id"))
    @OrderColumn(name = "ordem")
    @Column(name = "ponto_id", length = 20)
    private List<String> pontos = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "rota_via", joinColumns = @JoinColumn(name = "rota_id"))
    @OrderColumn(name = "ordem")
    @Column(name = "via_id", length = 20)
    private List<String> vias = new ArrayList<>();

    @Column(name = "distancia_total_km")
    private Double distanciaTotalKm;

    @Column(name = "tempo_total_min")
    private Double tempoTotalMin;

    @Column(name = "versao_rede", nullable = false)
    private int versaoRede;

    @Column(length = 500)
    private String mensagem;

    @Column(name = "calculada_em", nullable = false)
    private Instant calculadaEm;

    /** Exigido pelo JPA. Não usar no código da aplicação. */
    protected RotaCalculada() {
    }

    private RotaCalculada(String origemId, String destinoId, TipoCriterio criterio, StatusRota status,
                          int versaoRede, Instant calculadaEm) {
        this.origemId = Objects.requireNonNull(origemId);
        this.destinoId = Objects.requireNonNull(destinoId);
        this.criterio = Objects.requireNonNull(criterio);
        this.status = Objects.requireNonNull(status);
        this.versaoRede = versaoRede;
        // O banco guarda microssegundos; sem truncar, o POST e o GET da mesma rota divergiam.
        this.calculadaEm = Objects.requireNonNull(calculadaEm).truncatedTo(ChronoUnit.MICROS);
    }

    /**
     * Registro de uma rota encontrada.
     *
     * @param criterio   critério pedido
     * @param rota       rota calculada
     * @param versaoRede versão da rede usada
     * @param instante   momento do cálculo
     * @return registro com status CALCULADA
     */
    public static RotaCalculada calculada(TipoCriterio criterio, Rota rota, int versaoRede, Instant instante) {
        RotaCalculada r = new RotaCalculada(rota.getOrigemId(), rota.getDestinoId(), criterio,
                StatusRota.CALCULADA, versaoRede, instante);
        r.pontos.addAll(rota.getPontos());
        r.vias.addAll(rota.getVias());
        r.distanciaTotalKm = rota.getDistanciaTotalKm();
        r.tempoTotalMin = rota.getTempoTotalMin();
        return r;
    }

    /**
     * Registro de uma tentativa sem caminho.
     *
     * @param origemId   ponto de partida
     * @param destinoId  ponto de chegada
     * @param criterio   critério pedido
     * @param mensagem   explicação para o usuário
     * @param versaoRede versão da rede usada
     * @param instante   momento do cálculo
     * @return registro com status SEM_CAMINHO
     */
    public static RotaCalculada semCaminho(String origemId, String destinoId, TipoCriterio criterio,
                                           String mensagem, int versaoRede, Instant instante) {
        RotaCalculada r = new RotaCalculada(origemId, destinoId, criterio, StatusRota.SEM_CAMINHO, versaoRede, instante);
        r.mensagem = mensagem;
        return r;
    }

    /**
     * Identificador gerado pelo banco.
     *
     * @return o id, ou {@code null} antes de gravar
     */
    public Long getId() {
        return id;
    }

    /**
     * Ponto de partida.
     *
     * @return id da origem
     */
    public String getOrigemId() {
        return origemId;
    }

    /**
     * Ponto de chegada.
     *
     * @return id do destino
     */
    public String getDestinoId() {
        return destinoId;
    }

    /**
     * Critério pedido.
     *
     * @return o critério
     */
    public TipoCriterio getCriterio() {
        return criterio;
    }

    /**
     * Resultado do cálculo.
     *
     * @return CALCULADA ou SEM_CAMINHO
     */
    public StatusRota getStatus() {
        return status;
    }

    /**
     * Pontos percorridos.
     *
     * @return ids dos pontos; vazio se não houve caminho
     */
    public List<String> getPontos() {
        return List.copyOf(pontos);
    }

    /**
     * Vias percorridas.
     *
     * @return ids das vias; vazio se não houve caminho
     */
    public List<String> getVias() {
        return List.copyOf(vias);
    }

    /**
     * Distância total.
     *
     * @return distância em km, ou {@code null} se não houve caminho
     */
    public Double getDistanciaTotalKm() {
        return distanciaTotalKm;
    }

    /**
     * Tempo total.
     *
     * @return tempo em minutos, ou {@code null} se não houve caminho
     */
    public Double getTempoTotalMin() {
        return tempoTotalMin;
    }

    /**
     * Versão da rede usada no cálculo.
     *
     * @return a versão
     */
    public int getVersaoRede() {
        return versaoRede;
    }

    /**
     * Explicação para o usuário.
     *
     * @return a mensagem, ou {@code null} quando a rota foi calculada
     */
    public String getMensagem() {
        return mensagem;
    }

    /**
     * Momento do cálculo.
     *
     * @return o instante, em microssegundos
     */
    public Instant getCalculadaEm() {
        return calculadaEm;
    }

    @Override
    public String toString() {
        return "Rota " + id + " " + origemId + "->" + destinoId + " " + criterio + " " + status;
    }
}
