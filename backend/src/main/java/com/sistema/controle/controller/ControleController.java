package com.sistema.controle.controller;

import com.sistema.controle.model.*;
import com.sistema.controle.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/controle")
@CrossOrigin(origins = "*")
public class ControleController {

    @Autowired private AlunoRepository alunoRepository;
    @Autowired private EstoqueRepository estoqueRepository;
    @Autowired private EstoqueLoteRepository estoqueLoteRepository;
    @Autowired private PratoDoDiaRepository pratoDoDiaRepository;
    @Autowired private PratoIngredienteRepository pratoIngredienteRepository;
    @Autowired private RegistroAtendimentoRepository registroAtendimentoRepository;

    // --- ENDPOINTS DE ALUNOS ---
    @GetMapping("/alunos")
    public List<Aluno> listarAlunos() { 
        return alunoRepository.findAll(); 
    }

    @PostMapping("/alunos")
    public ResponseEntity<?> criarAluno(@RequestBody Aluno aluno) {
        if(alunoRepository.findByMatricula(aluno.getMatricula()).isPresent()) {
            return ResponseEntity.badRequest().body("Matrícula já cadastrada.");
        }
        return ResponseEntity.ok(alunoRepository.save(aluno));
    }

    @DeleteMapping("/alunos/{id}")
    public ResponseEntity<?> deletarAluno(@PathVariable Long id) {
        alunoRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    // --- ENDPOINTS DE ESTOQUE ---
    @GetMapping("/estoque")
    public List<Estoque> listarEstoque() { 
        return estoqueRepository.findAll(); 
    }

    @GetMapping("/estoque/lotes")
    public List<EstoqueLote> listarLotes() { 
        return estoqueLoteRepository.findAll(); 
    }

    @PostMapping("/estoque")
    public ResponseEntity<?> cadastrarAlimentoEDirecionarLote(@RequestBody Map<String, Object> payload) {
        String nome = (String) payload.get("nomeAlimento");
        String unidade = (String) payload.get("unidadeMedida");
        Double qtdInicial = Double.parseDouble(payload.get("quantidadeInicial").toString());
        LocalDate validade = LocalDate.parse((String) payload.get("dataValidadeInicial"));
        LocalDate entrada = payload.get("dataEntradaInicial") != null ? LocalDate.parse((String) payload.get("dataEntradaInicial")) : LocalDate.now();
        String resp = (String) payload.get("responsavel");

        Estoque estoqueItem = estoqueRepository.findByNomeAlimentoIgnoreCase(nome)
                .orElseGet(() -> {
                    Estoque e = new Estoque();
                    e.setNomeAlimento(nome);
                    e.setUnidadeMedida(unidade);
                    e.setQuantidadeAtual(0.0);
                    return estoqueRepository.save(e);
                });

        EstoqueLote lote = new EstoqueLote();
        lote.setEstoque(estoqueItem);
        lote.setNomeAlimento(estoqueItem.getNomeAlimento());
        lote.setQuantidadeInicial(qtdInicial);
        lote.setQuantidadeAtual(qtdInicial);
        lote.setDataValidade(validade);
        lote.setDataEntrada(entrada);
        lote.setResponsavel(resp);
        estoqueLoteRepository.save(lote);

        recalcularQuantidadeEstoque(estoqueItem.getId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/estoque/{id}/lote")
    public ResponseEntity<?> adicionarLoteExistente(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        Estoque estoqueItem = estoqueRepository.findById(id).orElseThrow();
        Double quantidade = Double.parseDouble(payload.get("quantidade").toString());
        LocalDate validade = LocalDate.parse((String) payload.get("dataValidade"));
        LocalDate entrada = payload.get("dataEntrada") != null ? LocalDate.parse((String) payload.get("dataEntrada")) : LocalDate.now();
        String resp = (String) payload.get("responsavel");

        EstoqueLote lote = new EstoqueLote();
        lote.setEstoque(estoqueItem);
        lote.setNomeAlimento(estoqueItem.getNomeAlimento());
        lote.setQuantidadeInicial(quantidade);
        lote.setQuantidadeAtual(quantidade);
        lote.setDataValidade(validade);
        lote.setDataEntrada(entrada);
        lote.setResponsavel(resp);
        estoqueLoteRepository.save(lote);

        recalcularQuantidadeEstoque(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/estoque/{id}/consumir")
    public ResponseEntity<?> consumirManualmente(@PathVariable Long id, @RequestParam Double quantidade) {
        Estoque estoqueItem = estoqueRepository.findById(id).orElseThrow();
        if (estoqueItem.getQuantidadeAtual() < quantidade) {
            return ResponseEntity.badRequest().body("Insumos insuficientes para baixa manual.");
        }
        abaterLotesInsumo(id, quantidade);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/estoque/{id}")
    public ResponseEntity<?> excluirInsumoCompleto(@PathVariable Long id) {
        estoqueRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/estoque/lotes/{id}")
    public ResponseEntity<?> deletarLoteEspecifico(@PathVariable Long id) {
        EstoqueLote lote = estoqueLoteRepository.findById(id).orElseThrow();
        Long estoqueId = lote.getEstoque().getId();
        estoqueLoteRepository.deleteById(id);
        recalcularQuantidadeEstoque(estoqueId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/estoque/corrigir-duplicados")
    public ResponseEntity<?> corrigirDuplicadosSaneamento() {
        List<Estoque> todos = estoqueRepository.findAll();
        Map<String, List<Estoque>> agrupados = todos.stream()
                .collect(Collectors.groupingBy(e -> e.getNomeAlimento().trim().toLowerCase()));

        for (Map.Entry<String, List<Estoque>> entry : agrupados.entrySet()) {
            if (entry.getValue().size() > 1) {
                Estoque principal = entry.getValue().get(0);
                for (int i = 1; i < entry.getValue().size(); i++) {
                    Estoque duplicado = entry.getValue().get(i);
                    
                    List<EstoqueLote> lotesDuplicados = estoqueLoteRepository.findAll().stream()
                            .filter(l -> l.getEstoque().getId().equals(duplicado.getId()))
                            .toList();
                            
                    for (EstoqueLote l : lotesDuplicados) {
                        l.setEstoque(principal);
                        estoqueLoteRepository.save(l);
                    }
                    estoqueRepository.deleteById(duplicado.getId());
                }
                recalcularQuantidadeEstoque(principal.getId());
            }
        }
        return ResponseEntity.ok().build();
    }

    // --- ENDPOINTS DE PRATO DO DIA ---
    @GetMapping("/prato-do-dia")
    public List<PratoDoDia> listarHistoricoDePratos() {
        return pratoDoDiaRepository.findAllByOrderByDataPratoDesc();
    }

    @GetMapping("/prato-do-dia/busca")
    public ResponseEntity<?> buscarPratoPorData(@RequestParam String data) {
        LocalDate localDate = LocalDate.parse(data);
        Optional<PratoDoDia> prato = pratoDoDiaRepository.findByDataPrato(localDate);
        return prato.isPresent() ? ResponseEntity.ok(prato.get()) : ResponseEntity.notFound().build();
    }

    @PostMapping("/prato-do-dia")
    public ResponseEntity<?> salvarPratoDoDia(@RequestBody Map<String, Object> payload) {
        String nomePrato = (String) payload.get("nomePrato");
        LocalDate dataPrato = LocalDate.parse((String) payload.get("dataPrato"));
        List<Map<String, Object>> ingredientesJson = (List<Map<String, Object>>) payload.get("ingredientes");

        if (pratoDoDiaRepository.findByDataPrato(dataPrato).isPresent()) {
            return ResponseEntity.badRequest().body("Já existe um cardápio cadastrado para esta data.");
        }

        PratoDoDia prato = new PratoDoDia();
        prato.setNomePrato(nomePrato);
        prato.setDataPrato(dataPrato);
        final PratoDoDia pratoSalvo = pratoDoDiaRepository.save(prato);

        List<PratoIngrediente> listaIngredientes = new ArrayList<>();
        for (Map<String, Object> ingMap : ingredientesJson) {
            Long estoqueId = Long.parseLong(ingMap.get("estoqueId").toString());
            Double qtdGasta = Double.parseDouble(ingMap.get("quantidadeGasta").toString());

            Estoque est = estoqueRepository.findById(estoqueId).orElseThrow();
            if(est.getQuantidadeAtual() < qtdGasta) {
                pratoDoDiaRepository.delete(pratoSalvo);
                return ResponseEntity.badRequest().body("Estoque insuficiente para o item: " + est.getNomeAlimento());
            }

            PratoIngrediente pi = new PratoIngrediente();
            pi.setPratoDoDia(pratoSalvo);
            pi.setEstoqueId(estoqueId);
            pi.setNomeAlimento(est.getNomeAlimento());
            pi.setQuantidadeGasta(qtdGasta);
            pratoIngredienteRepository.save(pi);
            listaIngredientes.add(pi);

            abaterLotesInsumo(estoqueId, qtdGasta);
        }

        pratoSalvo.setIngredientes(listaIngredientes);
        return ResponseEntity.ok(pratoDoDiaRepository.save(pratoSalvo));
    }

    @DeleteMapping("/prato-do-dia/{id}")
    public ResponseEntity<?> deletarCardapio(@PathVariable Long id) {
        pratoDoDiaRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    // --- REFEITÓRIO: ATENDIMENTO / SCANNER ---
      @PostMapping("/atendimento/registrar")
    public ResponseEntity<?> registrarAtendimento(@RequestParam String identificador) {

        Aluno aluno = alunoRepository.findByMatricula(identificador)
                .orElse(null);

        if (aluno == null) {
            return ResponseEntity.badRequest()
                    .body("Aluno não encontrado para a matrícula: " + identificador);
        }

        RegistroAtendimento registro = new RegistroAtendimento();
        registro.setAluno(aluno);
        registro.setDataHora(LocalDateTime.now());

        registroAtendimentoRepository.save(registro);

        return ResponseEntity.ok(registro);
    }
}
