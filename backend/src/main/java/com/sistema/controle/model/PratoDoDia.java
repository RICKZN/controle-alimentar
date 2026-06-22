package com.sistema.controle.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
public class PratoDoDia {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nome;
    private LocalDate data;
    private Integer refeicoesLiberadas;
    @ElementCollection
    private List<PratoIngrediente> ingredientes = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public LocalDate getData() { return data; }
    public void setData(LocalDate data) { this.data = data; }
    public Integer getRefeicoesLiberadas() { return refeicoesLiberadas; }
    public void setRefeicoesLiberadas(Integer refeicoesLiberadas) { this.refeicoesLiberadas = refeicoesLiberadas; }
    public List<PratoIngrediente> getIngredientes() { return ingredientes; }
    public void setIngredientes(List<PratoIngrediente> ingredientes) { this.ingredientes = ingredientes; }
}
