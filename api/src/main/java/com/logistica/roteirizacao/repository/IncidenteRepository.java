package com.logistica.roteirizacao.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.logistica.roteirizacao.model.Incidente;

public interface IncidenteRepository extends JpaRepository<Incidente, Long> {

    Optional<Incidente> findFirstByViaIdAndAtivoTrue(String viaId);

    List<Incidente> findByAtivoTrue();
}
