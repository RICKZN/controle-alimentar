package com.sistema.controle.controller;

import com.sistema.controle.model.Aluno;
import com.sistema.controle.model.Estoque;
import com.sistema.controle.model.RegistroAtendimento;
import com.sistema.controle.repository.AlunoRepository;
import com.sistema.controle.repository.EstoqueRepository;
import com.sistema.controle.repository.RegistroAtendimentoRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // Habilita CORS para o Vue.js
public class ControleController {

    @Autowired
    private EstoqueRepository estoqueRepo;

    @Autowired
    private RegistroAtendimentoRepository registroRepo;

    @Autowired
    private AlunoRepository alunoRepo;

    @PostConstruct
    public void initData() {
        // Inicializa estoque
        if (estoqueRepo.count() == 0) {
            adicionarItemInicial("Arroz", "kg", 50.0);
            adicionarItemInicial("Feijão", "kg", 30.0);
            adicionarItemInicial("Carnes Vermelhas", "kg", 20.0);
            adicionarItemInicial("Frango", "kg", 25.0);
            adicionarItemInicial("Óleo", "litros", 10.0);
        }

        // Inicializa alguns alunos de teste
        if (alunoRepo.count() == 0) {
            cadastrarAlunoInicial("2023001", "João Silva");
            cadastrarAlunoInicial("2023002", "Maria Oliveira");
            cadastrarAlunoInicial("2023003", "Pedro Santos");
        }
    }

    private void cadastrarAlunoInicial(String matricula, String nome) {
        Aluno a = new Aluno();
        a.setMatricula(matricula);
        a.setNome(nome);
        alunoRepo.save(a);
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

    @DeleteMapping("/estoque/{id}")
    public ResponseEntity<?> excluirItemEstoque(@PathVariable Long id) {
        estoqueRepo.deleteById(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/estoque")
    public ResponseEntity<?> adicionarNovoItem(@RequestBody Estoque novoItem) {
        return ResponseEntity.ok(estoqueRepo.save(novoItem));
    }

    @GetMapping("/alunos")
    public List<Aluno> listarAlunos() {
        return alunoRepo.findAll();
    }

    @PostMapping("/alunos")
    public ResponseEntity<?> cadastrarAluno(@RequestBody Aluno novoAluno) {
        if (alunoRepo.findByMatricula(novoAluno.getMatricula()).isPresent()) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Matrícula já cadastrada!");
            return ResponseEntity.badRequest().body(response);
        }
        return ResponseEntity.ok(alunoRepo.save(novoAluno));
    }

    @DeleteMapping("/alunos/{id}")
    public ResponseEntity<?> excluirAluno(@PathVariable Long id) {
        alunoRepo.deleteById(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/validar")
    public ResponseEntity<?> validarFicha(@RequestParam String matricula) {
        Map<String, Object> response = new HashMap<>();
        
        LocalDateTime agora = LocalDateTime.now();
        LocalDateTime limite6Horas = agora.minusHours(210);

        // Busca o aluno
        Optional<Aluno> alunoOpt = alunoRepo.findByMatricula(matricula);
        if (alunoOpt.isEmpty()) {
            response.put("error", "Estudante não cadastrado no sistema.");
            return ResponseEntity.status(404).body(response);
        }

        Aluno aluno = alunoOpt.get();

        // Verifica intervalo de 6 horas
        if (aluno.getUltimaRefeicao() != null && aluno.getUltimaRefeicao().isAfter(limite6Horas)) {
                      Duration restante = Duration.between(agora, aluno.getUltimaRefeicao().plusHours(6));
            long minutosFaltando = restante.toMinutes();
            long segundosFaltando = restante.toSeconds();
            
            response.put("error", "Já recebeu refeição recentemente.");
            response.put("horaUltimaRefeicao", aluno.getUltimaRefeicao().toString());
            response.put("proximaRefeicao", aluno.getUltimaRefeicao().plusHours(6).toString());
            response.put("minutosFaltando", minutosFaltando);
            response.put("segundosFaltando", segundosFaltando);
            response.put("espera", String.format("Aguarde mais %d horas e %d minutos.",
                         minutosFaltando / 60, minutosFaltando % 60));
        }

        // Registra atendimento
        RegistroAtendimento reg = new RegistroAtendimento();
        reg.setMatricula(matricula);
        reg.setDataHoraAtendimento(agora);
        registroRepo.save(reg);

        // Atualiza aluno
        aluno.setUltimaRefeicao(agora);
        alunoRepo.save(aluno);

        response.put("message", "Refeição liberada para " + aluno.getNome());
        return ResponseEntity.ok(response);
    }
}
