package com.sistema.controle.repository;

import com.sistema.controle.model.RegistroAtendimento;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;

public interface RegistroAtendimentoRepository extends JpaRepository<RegistroAtendimento, Long> {
    // Busca se existe registro após uma data específica (ex: últimas 6 horas)
    boolean existsByMatriculaAndDataHoraAtendimentoAfter(String matricula, LocalDateTime dataHora);
}
