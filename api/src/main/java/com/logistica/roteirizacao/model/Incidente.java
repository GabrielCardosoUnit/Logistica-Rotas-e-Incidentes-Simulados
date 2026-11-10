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

/** Bloqueio de uma via. Enquanto ativo, a via não entra em nenhum cálculo. */
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

    /** Exigido pelo JPA. Não usar no código da aplicação. */
    protected Incidente() {
    }

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

    public Long getId() {
        return id;
    }

    public Via getVia() {
        return via;
    }

    public String getDescricao() {
        return descricao;
    }

    public Instant getRegistradoEm() {
        return registradoEm;
    }

    public boolean isAtivo() {
        return ativo;
    }

    @Override
    public String toString() {
        return "Incidente " + id + " na via " + via.getId() + (ativo ? " (ativo)" : " (encerrado)") + ": " + descricao;
    }
}
