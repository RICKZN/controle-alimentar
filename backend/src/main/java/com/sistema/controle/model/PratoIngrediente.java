package com.sistema.controle.model;

import jakarta.persistence.Embeddable;

@Embeddable
public class PratoIngrediente {
    private String nome;
    private String unidade;
    private Double quantidade;

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getUnidade() { return unidade; }
    public void setUnidade(String unidade) { this.unidade = unidade; }
    public Double getQuantidade() { return quantidade; }
    public void setQuantidade(Double quantidade) { this.quantidade = quantidade; }
}
