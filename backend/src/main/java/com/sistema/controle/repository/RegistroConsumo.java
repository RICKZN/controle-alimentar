package com.sistema.controle.model;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
public class RegistroConsumo {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long estoqueId;
    private String nomeItem;
    private LocalDate data;
    private Double quantidadeGasta;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getEstoqueId() { return estoqueId; }
    public void setEstoqueId(Long estoqueId) { this.estoqueId = estoqueId; }
    public String getNomeItem() { return nomeItem; }
    public void setNomeItem(String nomeItem) { this.nomeItem = nomeItem; }
    public LocalDate getData() { return data; }
    public void setData(LocalDate data) { this.data = data; }
    public Double getQuantidadeGasta() { return quantidadeGasta; }
    public void setQuantidadeGasta(Double quantidadeGasta) { this.quantidadeGasta = quantidadeGasta; }
}
