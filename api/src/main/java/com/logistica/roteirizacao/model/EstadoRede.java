package com.logistica.roteirizacao.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Linha única que guarda a versão atual da rede. A versão sobe a cada
 * importação, a cada incidente e a cada desbloqueio, e é gravada em cada rota calculada para
 * saber com qual estado da rede ela foi obtida.
 */
@Entity
@Table(name = "estado_rede")
public class EstadoRede {

    /** Id da única linha da tabela. */
    public static final long ID_UNICO = 1L;

    @Id
    private Long id;

    @Column(name = "versao", nullable = false)
    private int versao;

    /** Exigido pelo JPA. Não usar no código da aplicação. */
    protected EstadoRede() {
    }

    /**
     * Estado de uma rede ainda não importada.
     *
     * @return estado com versão 0
     */
    public static EstadoRede inicial() {
        EstadoRede estado = new EstadoRede();
        estado.id = ID_UNICO;
        estado.versao = 0;
        return estado;
    }

    /**
     * Sobe a versão da rede em 1.
     *
     * @return a nova versão
     */
    public int incrementarVersao() {
        versao++;
        return versao;
    }

    /**
     * Identificador da linha.
     *
     * @return sempre {@link #ID_UNICO}
     */
    public Long getId() {
        return id;
    }

    /**
     * Versão atual da rede.
     *
     * @return a versão; 0 antes da primeira importação
     */
    public int getVersao() {
        return versao;
    }
}
