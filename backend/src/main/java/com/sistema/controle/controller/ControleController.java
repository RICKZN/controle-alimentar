package com.sistema.controle.controller;

import com.sistema.controle.model.Estoque;
import com.sistema.controle.model.RegistroAtendimento;
import com.sistema.controle.repository.EstoqueRepository;
import com.sistema.controle.repository.RegistroAtendimentoRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // Habilita CORS para o Vue.js
public class ControleController {

    @Autowired
    private EstoqueRepository estoqueRepo;

    @Autowired
    private RegistroAtendimentoRepository registroRepo;

    // Inicializa o estoque com itens reais caso o banco esteja vazio
    @PostConstruct
    public void initEstoque() {
        if (estoqueRepo.count() == 0) {
            adicionarItemInicial("Arroz", "kg", 50.0);
            adicionarItemInicial("Feijão", "kg", 30.0);
            adicionarItemInicial("Carnes Vermelhas", "kg", 20.0);
            adicionarItemInicial("Frango", "kg", 25.0);
            adicionarItemInicial("Óleo", "litros", 10.0);
        }
    }

    private void adicionarItemInicial(String nome, String unidade, Double quantidade) {
        Estoque item = new Estoque();
        item.setNome(nome);
        item.setUnidade(unidade);
        item.setQuantidade(quantidade);
        estoqueRepo.save(item);
    }

    @GetMapping("/estoque")
    public List<Estoque> obterEstoque() {
        return estoqueRepo.findAll();
    }

    @PostMapping("/estoque/{id}/ajustar")
    public ResponseEntity<?> ajustarEstoque(@PathVariable Long id, @RequestParam Double variacao) {
        Optional<Estoque> itemOpt = estoqueRepo.findById(id);
        if (itemOpt.isPresent()) {
            Estoque item = itemOpt.get();
            double novaQuantidade = item.getQuantidade() + variacao;
            if (novaQuantidade < 0) novaQuantidade = 0; // Evita estoque negativo
            item.setQuantidade(novaQuantidade);
            estoqueRepo.save(item);
            return ResponseEntity.ok(item);
        }
        return ResponseEntity.badRequest().body("Item não encontrado.");
    }

    @PostMapping("/validar")
    public ResponseEntity<?> validarFicha(@RequestParam String matricula) {
        Map<String, String> response = new HashMap<>();
        
        LocalDate hoje = LocalDate.now();
        boolean jaAtendido = registroRepo.existsByMatriculaAndDataAtendimento(matricula, hoje);
        if (jaAtendido) {
            response.put("error", "Estudante já recebeu refeição hoje.");
            return ResponseEntity.badRequest().body(response);
        }

        // Registra atendimento (separado do estoque físico de ingredientes)
        RegistroAtendimento reg = new RegistroAtendimento();
        reg.setMatricula(matricula);
        reg.setDataAtendimento(hoje);
        registroRepo.save(reg);

        response.put("message", "Refeição liberada para a matrícula " + matricula);
        return ResponseEntity.ok(response);
    }
}
