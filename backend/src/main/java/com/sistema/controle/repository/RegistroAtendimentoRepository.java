package com.sistema.controle.repository;

import com.sistema.controle.model.RegistroAtendimento;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;

public interface RegistroAtendimentoRepository extends JpaRepository<RegistroAtendimento, Long> {
    boolean existsByMatriculaAndDataAtendimento(String matricula, LocalDate dataAtendimento);
}
