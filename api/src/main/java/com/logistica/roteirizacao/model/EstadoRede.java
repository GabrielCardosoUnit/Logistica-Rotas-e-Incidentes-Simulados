package com.logistica.roteirizacao.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Linha única que guarda a versão atual da rede. A versão sobe a cada
 * importação e a cada incidente, e é gravada em cada rota calculada para
 * saber com qual estado da rede ela foi obtida.
 */
@Entity
@Table(name = "estado_rede")
public class EstadoRede {

    public static final long ID_UNICO = 1L;

    @Id
    private Long id;

    @Column(name = "versao", nullable = false)
    private int versao;

    /** Exigido pelo JPA. Não usar no código da aplicação. */
    protected EstadoRede() {
    }

    public static EstadoRede inicial() {
        EstadoRede estado = new EstadoRede();
        estado.id = ID_UNICO;
        estado.versao = 0;
        return estado;
    }

    public int incrementarVersao() {
        versao++;
        return versao;
    }

    public Long getId() {
        return id;
    }

    public int getVersao() {
        return versao;
    }
}
