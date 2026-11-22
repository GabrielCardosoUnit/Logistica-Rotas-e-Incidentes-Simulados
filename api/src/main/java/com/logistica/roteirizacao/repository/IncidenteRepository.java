package com.logistica.roteirizacao.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.logistica.roteirizacao.model.Incidente;

/** Acesso aos incidentes registrados. */
public interface IncidenteRepository extends JpaRepository<Incidente, Long> {

    /**
     * Incidente ativo na via, se houver.
     *
     * @param viaId id da via
     * @return o incidente ativo, ou vazio
     */
    Optional<Incidente> findFirstByViaIdAndAtivoTrue(String viaId);

    /**
     * Todos os incidentes ainda ativos.
     *
     * @return incidentes ativos
     */
    List<Incidente> findByAtivoTrue();
}
