package com.logistica.roteirizacao.model;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Bloqueio de uma via. Enquanto ativo, a via não entra em nenhum cálculo.
 * Ao ser encerrado (desbloqueio), continua no banco como histórico.
 */
@Entity
@Table(name = "incidente")
public class Incidente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "via_id", nullable = false)
    private Via via;

    @Column(nullable = false, length = 200)
    private String descricao;

    @Column(name = "registrado_em", nullable = false)
    private Instant registradoEm;

    @Column(nullable = false)
    private boolean ativo;

    /** Quando a via foi desbloqueada; null enquanto o incidente está ativo. */
    @Column(name = "encerrado_em")
    private Instant encerradoEm;

    /** Exigido pelo JPA. Não usar no código da aplicação. */
    protected Incidente() {
    }

    /**
     * Registra um incidente ativo.
     *
     * @param via          via bloqueada
     * @param descricao    motivo do bloqueio
     * @param registradoEm instante do registro
     * @throws IllegalArgumentException se a descrição estiver vazia
     */
    public Incidente(Via via, String descricao, Instant registradoEm) {
        this.via = Objects.requireNonNull(via, "via do incidente é obrigatória.");
        if (descricao == null || descricao.isBlank()) {
            throw new IllegalArgumentException("descrição do incidente é obrigatória.");
        }
        this.descricao = descricao.strip();
        // Mesma precisão do banco (microssegundos), como em RotaCalculada.
        this.registradoEm = Objects.requireNonNull(registradoEm, "instante do incidente é obrigatório.")
                .truncatedTo(ChronoUnit.MICROS);
        this.ativo = true;
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
     * Via bloqueada.
     *
     * @return a via
     */
    public Via getVia() {
        return via;
    }

    /**
     * Motivo do bloqueio.
     *
     * @return a descrição
     */
    public String getDescricao() {
        return descricao;
    }

    /**
     * Quando o incidente foi registrado.
     *
     * @return o instante, em microssegundos
     */
    public Instant getRegistradoEm() {
        return registradoEm;
    }

    /**
     * Diz se o incidente ainda bloqueia a via.
     *
     * @return {@code true} enquanto não for encerrado
     */
    public boolean isAtivo() {
        return ativo;
    }

    /**
     * Quando o incidente foi encerrado.
     *
     * @return o instante, ou {@code null} se ainda ativo
     */
    public Instant getEncerradoEm() {
        return encerradoEm;
    }

    /**
     * Desbloqueia a via: o incidente deixa de valer nos cálculos.
     *
     * @param instante momento do encerramento
     * @throws IllegalStateException se o incidente já estava encerrado
     */
    public void encerrar(Instant instante) {
        if (!ativo) {
            throw new IllegalStateException("incidente " + id + " já foi encerrado.");
        }
        Objects.requireNonNull(instante, "instante do encerramento é obrigatório.");
        this.ativo = false;
        this.encerradoEm = instante.truncatedTo(ChronoUnit.MICROS);
    }

    @Override
    public String toString() {
        return "Incidente " + id + " na via " + via.getId() + (ativo ? " (ativo)" : " (encerrado)") + ": " + descricao;
    }
}
