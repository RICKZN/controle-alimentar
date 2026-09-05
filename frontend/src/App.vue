<template>
  <div class="app-layout" :class="{ 'sidebar-open': isSidebarOpen }">
    <!-- Overlay para mobile -->
    <div v-if="isSidebarOpen" class="sidebar-overlay" @click="isSidebarOpen = false"></div>

    <!-- Sidebar -->
    <aside class="sidebar">
      <div class="sidebar-header">
        <img src="/ifba-logo.png" class="ifba-logo-img" alt="Instituto Federal Bahia" />
        <div class="logo">SISTEMA<span>IF</span></div>
        <div class="logo-version">v3.5 · Controle Alimentar</div>
      </div>
      <nav class="sidebar-nav">
        <button @click="changeTab('validacao')" :class="{ active: currentTab === 'validacao' }">
          <span class="icon">📷</span> Validação
        </button>
        <button @click="changeTab('alunos')" :class="{ active: currentTab === 'alunos' }">
          <span class="icon">👥</span> Alunos
        </button>
        <button @click="changeTab('estoque')" :class="{ active: currentTab === 'estoque' }">
          <span class="icon">📦</span> Estoque
        </button>
        <button @click="changeTab('prato'); abaPrato = 'atual'" :class="{ active: currentTab === 'prato' }">
          <span class="icon">🍽️</span> Prato do Dia
        </button>
        <button @click="changeTab('alertas')" :class="{ active: currentTab === 'alertas' }">
          <span class="icon">⚠️</span> Alertas <span v-if="alertas.length" class="counter-alert">{{ alertas.length }}</span>
        </button>
        <button @click="changeTab('geracao')" :class="{ active: currentTab === 'geracao' }">
          <span class="icon">🎟️</span> Gerar Fichas
        </button>
      </nav>
      <div class="sidebar-footer">
        <p>Controle de Refeições</p>
      </div>
    </aside>

    <!-- Main Content -->
    <main class="main-content">
      <header class="top-bar">
        <button class="menu-toggle" @click="isSidebarOpen = true">☰</button>
        <div class="top-bar-brand">
          <img src="/ifba-logo.png" class="top-bar-logo" alt="IFBA" />
          <h1>{{ tabTitles[currentTab] }}</h1>
        </div>
        <div class="user-status" :class="{ online: isOnline }">
          {{ isOnline ? 'Online' : 'Offline' }}
        </div>
      </header>

      <div class="content-area">

        <!-- TELA DE VALIDAÇÃO -->
        <div v-if="currentTab === 'validacao'" class="tab-pane">
          <div class="scanner-wrapper card glass-effect">
            <div class="scanner-header">
              <p v-if="!isCameraActive" class="camera-info">A câmera está desligada</p>
              <p v-else class="camera-info active">Buscando QR Code...</p>
            </div>

            <div class="preview-container">
              <div id="reader"></div>

              <!-- Overlay de Detecção -->
              <div v-if="matriculaLida" class="scan-success-overlay">
                <div class="success-card">
                  <div class="success-icon">✅</div>
                  <h3>QR Code Detectado</h3>
                  <div class="id-display">{{ matriculaLida }}</div>
                  <button @click="confirmarValidacao" class="btn btn-validate pulse">LIBERAR ACESSO</button>
                  <button @click="resetScan" class="btn-link">Cancelar</button>
                </div>
              </div>

              <!-- Placeholder quando câmera está desligada -->
              <div v-if="!isCameraActive && !matriculaLida" class="camera-placeholder" @click="toggleCamera">
                <span class="icon-large">📷</span>
                <p>Clique para ligar a câmera</p>
              </div>
            </div>

            <div class="scanner-controls">
              <button @click="toggleCamera" :class="['btn', isCameraActive ? 'btn-danger' : 'btn-success']">
                {{ isCameraActive ? 'Desligar Câmera' : 'Ligar Câmera' }}
              </button>
              <button v-if="isCameraActive" @click="switchCamera" class="btn btn-secondary">
                Trocar Câmera
              </button>
            </div>
          </div>

          <!-- Mensagem de bloqueio de 6h -->
          <div v-if="statusValidacao" class="card status-card" :class="statusValidacao.tipo">
            <p>{{ statusValidacao.msg }}</p>
            <p v-if="statusValidacao.ultimaRefeicao" style="color:#94a3b8; margin-top:0.5rem">
              🍽️ Última refeição: {{ formatarData(statusValidacao.ultimaRefeicao) }}
            </p>
            <p v-if="statusValidacao.proximaRefeicao" style="color:#94a3b8">
              🔓 Próxima liberação: {{ formatarData(statusValidacao.proximaRefeicao) }}
            </p>
            <p v-if="tempoEsperaReal" class="wait-time">⏳ {{ tempoEsperaReal }}</p>
          </div>

          <div class="card glass-effect">
            <h3>Entrada Manual</h3>
            <div class="input-group-row">
              <input type="text" v-model="matriculaParaValidar" placeholder="Matrícula" @keyup.enter="validarFicha(matriculaParaValidar)" />
              <button @click="validarFicha(matriculaParaValidar)" class="btn btn-primary">Validar</button>
            </div>
          </div>
        </div>

        <!-- TELA DE ALUNOS -->
        <div v-if="currentTab === 'alunos'" class="tab-pane">
          <div class="card glass-effect">
            <h3>Cadastrar Novo Aluno</h3>
            <div class="input-group-row">
              <input type="text" v-model="novoAluno.nome" placeholder="Nome completo do aluno" />
              <input type="text" v-model="novoAluno.matricula" placeholder="Número da matrícula" style="width: 180px" />
              <input type="text" v-model="novoAluno.curso" placeholder="Curso" />
              <input type="text" v-model="novoAluno.modalidade" placeholder="Modalidade" />
              <input type="text" v-model="novoAluno.turma" placeholder="Turma" />
              <select v-model="novoAluno.turno" class="input-field" style="width: 160px">
                <option value="" disabled>Turno</option>
                <option v-for="t in TURNOS" :key="t" :value="t">{{ t }}</option>
              </select>
              <button @click="cadastrarAluno" class="btn btn-success">Cadastrar</button>
            </div>
          </div>

          <div class="card glass-effect">
            <div class="table-header">
              <h3>Listagem de Estudantes</h3>
              <div class="actions">
                <button @click="carregarAlunos" class="btn-refresh">🔄 Atualizar</button>
                <select v-model="filtroTurno" class="input-field" style="width: 160px">
                  <option value="">Todos os turnos</option>
                  <option v-for="t in TURNOS" :key="t" :value="t">{{ t }}</option>
                </select>
                <input type="text" v-model="filtroAluno" placeholder="Pesquisar..." class="search-input" />
              </div>
            </div>
            <div class="table-wrapper">
              <div v-if="alunosFiltrados.length === 0" class="empty-state">
                <p>Nenhum aluno encontrado. Cadastre o primeiro aluno no formulário acima!</p>
              </div>
              <table v-else class="data-table">
                <thead>
                  <tr>
                    <th>Nome</th>
                    <th>Matrícula</th>
                    <th>Curso</th>
                    <th>Modalidade</th>
                    <th>Turma</th>
                    <th>Turno</th>
                    <th>Último Acesso</th>
                    <th>Status</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="aluno in alunosFiltrados" :key="aluno.id">
                    <td>{{ aluno.nome }}</td>
                    <td><code>{{ aluno.matricula }}</code></td>
                    <td>{{ aluno.curso }}</td>
                    <td>{{ aluno.modalidade }}</td>
                    <td>{{ aluno.turma }}</td>
                    <td>{{ aluno.turno }}</td>
                    <td>{{ formatarData(aluno.ultimaRefeicao) }}</td>
                    <td>
                      <span :class="['badge', obterTempoRestante(aluno.ultimaRefeicao) > 0 ? 'badge-danger' : 'badge-success']">
                        {{ obterTempoRestante(aluno.ultimaRefeicao) > 0 ? 'Bloqueado' : 'Liberado' }}
                      </span>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>

        <!-- TELA DE ESTOQUE -->
        <div v-if="currentTab === 'estoque'" class="tab-pane">
          <div class="card glass-effect" style="padding:0.8rem 1.5rem; margin-bottom:1rem; display:flex; gap:8px">
            <button @click="abaEstoque = 'atual'" :class="['btn', abaEstoque==='atual' ? 'btn-primary' : 'btn-secondary']">📦 Estoque Atual</button>
            <button @click="abaEstoque = 'historico'" :class="['btn', abaEstoque==='historico' ? 'btn-primary' : 'btn-secondary']">📥 Entradas de Estoque</button>
          </div>

          <div v-if="abaEstoque === 'atual'">
            <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:1rem">
              <input type="text" v-model="filtroEstoque" placeholder="Filtrar estoque..." class="form-control" style="max-width:250px" />
              <button @click="corrigirDuplicados" class="btn btn-secondary">🛠️ Organizar Duplicados</button>
            </div>
            <div class="grid-estoque">
              <div v-for="item in estoqueFiltrado" :key="item.id" class="card card-item glass-effect">
                <div class="card-item-header">
                  <strong>{{ item.nomeAlimento }}</strong>
                  <button @click="deletarAlimento(item.id)" class="btn-icon-danger">🗑️</button>
                </div>
                <p>{{ item.quantidadeAtual }} {{ item.unidadeMedida }}</p>
                <div style="display:flex; gap:6px; margin-top:1rem">
                  <button @click="quickConsumir(item)" class="btn btn-danger" style="flex:1">− Consumo</button>
