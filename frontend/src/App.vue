<template>
  <div class="app-layout" :class="{ 'sidebar-open': isSidebarOpen }">
    <!-- Overlay para mobile -->
    <div v-if="isSidebarOpen" class="sidebar-overlay" @click="isSidebarOpen = false"></div>

    <!-- Sidebar -->
    <aside class="sidebar">
      <div class="sidebar-header">
        <div class="logo">CONTROLE<span>IF</span></div>
      </div>
      <nav class="sidebar-nav">
        <button 
          @click="changeTab('validacao')" 
          :class="{ active: currentTab === 'validacao' }"
        >
          <span class="icon">📷</span> Validação
        </button>
        <button 
          @click="changeTab('estoque')" 
          :class="{ active: currentTab === 'estoque' }"
        >
          <span class="icon">📦</span> Estoque
        </button>
        <button 
          @click="changeTab('geracao')" 
          :class="{ active: currentTab === 'geracao' }"
        >
          <span class="icon">🎟️</span> Gerar Fichas
        </button>
      </nav>
      <div class="sidebar-footer">
        <p>v2.1 - Custom Scan</p>
      </div>
    </aside>

    <!-- Main Content -->
    <main class="main-content">
      <header class="top-bar">
        <button class="menu-toggle" @click="isSidebarOpen = true">☰</button>
        <h1>{{ tabTitles[currentTab] }}</h1>
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
                  <h3>Estudante Identificado</h3>
                  <div class="id-display">{{ matriculaLida }}</div>
                  <button @click="confirmarValidacao" class="btn btn-validate pulse">CONFIRMAR ACESSO</button>
                  <button @click="resetScan" class="btn-link">Tentar novamente</button>
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
                Inverter Câmera
              </button>
            </div>
          </div>

          <div class="card glass-effect">
            <h3>Entrada Manual</h3>
            <div class="input-group-row">
              <input type="text" v-model="matriculaParaValidar" placeholder="Número da matrícula" @keyup.enter="validarFicha(matriculaParaValidar)" />
              <button @click="validarFicha(matriculaParaValidar)" class="btn btn-primary">Validar</button>
            </div>
          </div>
        </div>

        <!-- TELA DE ESTOQUE (MESMA LÓGICA ANTERIOR) -->
        <div v-if="currentTab === 'estoque'" class="tab-pane">
          <div class="estoque-grid">
            <div v-for="item in estoqueList" :key="item.id" class="estoque-card glass-effect">
              <div class="item-header">
                <h3>{{ item.nome }}</h3>
                <span class="unit-tag">{{ item.unidade }}</span>
              </div>
              <div class="item-body">
                <button @click="ajustarEstoque(item.id, -1)" class="adjust-btn minus">-</button>
                <div class="qty-display">
                  <span class="qty-value">{{ formatarQuantidade(item.quantidade) }}</span>
                </div>
                <button @click="ajustarEstoque(item.id, 1)" class="adjust-btn plus">+</button>
              </div>
            </div>
          </div>
        </div>

        <!-- TELA DE GERAÇÃO -->
        <div v-if="currentTab === 'geracao'" class="tab-pane">
          <div class="card glass-effect">
            <h3>Gerar Nova Ficha</h3>
            <div class="input-group">
              <input type="text" v-model="matriculaParaGerar" placeholder="Matrícula do estudante" />
              <button @click="gerarQrCode" class="btn btn-primary mt-1">Gerar QR Code</button>
            </div>
            
            <div v-if="qrCodeGerado" class="qr-result">
              <div class="printable-area">
                <qrcode-vue :value="matriculaParaGerar" :size="220" level="H" />
                <p>ESTUDANTE: {{ matriculaParaGerar }}</p>
              </div>
              <button @click="imprimirFicha" class="btn btn-secondary">Imprimir Agora</button>
            </div>
          </div>
        </div>
      </div>

      <!-- Toast de Notificação -->
      <Transition name="slide-fade">
        <div v-if="mensagem" :class="['toast', mensagemTipo]">
          {{ mensagem }}
        </div>
      </Transition>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue';
import axios from 'axios';
import QrcodeVue from 'qrcode.vue';
import { Html5Qrcode } from 'html5-qrcode';

const API_URL = import.meta.env.VITE_API_URL || '/api';

// Estados
const currentTab = ref('validacao');
const isSidebarOpen = ref(false);
const isOnline = ref(true);
const isCameraActive = ref(false);
const currentCameraId = ref('environment'); // 'environment' = traseira, 'user' = frontal

const tabTitles = {
  validacao: 'Validação de Acesso',
  estoque: 'Estoque de Alimentos',
  geracao: 'Geração de Fichas'
};

const estoqueList = ref([]);
const matriculaLida = ref(null);
const matriculaParaValidar = ref('');
const matriculaParaGerar = ref('');
const qrCodeGerado = ref(false);
const mensagem = ref('');
const mensagemTipo = ref('');

let html5QrCode = null;

// Lógica de Negócio
const carregarEstoque = async () => {
  try {
    const res = await axios.get(`${API_URL}/estoque`);
    estoqueList.value = res.data;
    isOnline.value = true;
  } catch (err) { 
    isOnline.value = false;
  }
};

const ajustarEstoque = async (id, delta) => {
  try {
    await axios.post(`${API_URL}/estoque/${id}/ajustar?variacao=${delta}`);
    await carregarEstoque();
  } catch (err) { mostrarMensagem("Erro ao atualizar estoque", "error"); }
};

const validarFicha = async (matricula) => {
  if (!matricula) return;
  try {
    const res = await axios.post(`${API_URL}/validar?matricula=${matricula}`);
    mostrarMensagem(res.data.message, 'success');
    matriculaParaValidar.value = '';
    carregarEstoque();
  } catch (err) {
    mostrarMensagem(err.response?.data?.error || "Erro na validação", 'error');
  }
};

const confirmarValidacao = async () => {
  await validarFicha(matriculaLida.value);
  resetScan();
};

const resetScan = () => {
  matriculaLida.value = null;
  if (isCameraActive.value) startCamera();
};

const mostrarMensagem = (txt, tipo) => {
  mensagem.value = txt;
  mensagemTipo.value = tipo;
  setTimeout(() => mensagem.value = '', 4000);
};

// Lógica do Scanner
const toggleCamera = async () => {
  if (isCameraActive.value) {
    await stopCamera();
  } else {
    await startCamera();
  }
};

const startCamera = async () => {
  if (!html5QrCode) html5QrCode = new Html5Qrcode("reader");
  
  const config = { fps: 15, qrbox: { width: 250, height: 250 } };
  
  try {
    await html5QrCode.start(
      { facingMode: currentCameraId.value }, 
      config, 
      (decodedText) => {
        matriculaLida.value = decodedText;
        html5QrCode.stop(); // Para a câmera ao ler
        isCameraActive.value = false;
        if (navigator.vibrate) navigator.vibrate(200);
      }
    );
    isCameraActive.value = true;
  } catch (err) {
    console.error(err);
    mostrarMensagem("Câmera bloqueada ou não encontrada. Use HTTPS.", "error");
  }
};

const stopCamera = async () => {
  if (html5QrCode && isCameraActive.value) {
    await html5QrCode.stop();
    isCameraActive.value = false;
  }
};

const switchCamera = async () => {
  await stopCamera();
  currentCameraId.value = currentCameraId.value === 'environment' ? 'user' : 'environment';
  await startCamera();
};

const changeTab = async (tab) => {
  if (isCameraActive.value) await stopCamera();
  currentTab.value = tab;
  isSidebarOpen.value = false;
};

const gerarQrCode = () => {
  if (matriculaParaGerar.value) qrCodeGerado.value = true;
};

const imprimirFicha = () => window.print();
const formatarQuantidade = (q) => Number.isInteger(q) ? q : q.toFixed(1);

onMounted(carregarEstoque);
onUnmounted(stopCamera);
</script>

<style>
@import url('https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;600;800&display=swap');

:root {
  --primary: #4f46e5;
  --success: #10b981;
  --danger: #f43f5e;
  --bg-dark: #020617;
  --sidebar-bg: #0f172a;
  --glass: rgba(30, 41, 59, 0.6);
  --border: rgba(255, 255, 255, 0.08);
}

* { box-sizing: border-box; margin: 0; padding: 0; }

body {
  font-family: 'Outfit', sans-serif;
  background-color: var(--bg-dark);
  color: #f8fafc;
  overflow-x: hidden;
}

.app-layout { display: flex; min-height: 100vh; }

/* SIDEBAR */
.sidebar {
  width: 280px;
  background: var(--sidebar-bg);
  border-right: 1px solid var(--border);
  display: flex;
  flex-direction: column;
  position: fixed;
  height: 100vh;
  z-index: 100;
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.sidebar-header { padding: 3rem 2rem; text-align: center; }
.logo { font-size: 1.8rem; font-weight: 800; letter-spacing: -1px; }
.logo span { color: var(--primary); }

.sidebar-nav { padding: 1rem; flex: 1; }
.sidebar-nav button {
  width: 100%;
  padding: 1.2rem;
  margin-bottom: 0.8rem;
  background: transparent;
  border: none;
  color: #94a3b8;
  font-size: 1rem;
  font-weight: 600;
  border-radius: 16px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 15px;
  transition: all 0.3s;
}

.sidebar-nav button.active {
  background: linear-gradient(135deg, var(--primary), #818cf8);
  color: white;
  box-shadow: 0 10px 20px rgba(79, 70, 229, 0.3);
}

.sidebar-nav button:hover:not(.active) { background: rgba(255,255,255,0.05); color: white; }

/* MAIN */
.main-content { flex: 1; margin-left: 280px; min-width: 0; }

.top-bar {
  height: 80px;
  background: rgba(2, 6, 23, 0.8);
  backdrop-filter: blur(12px);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 3rem;
  border-bottom: 1px solid var(--border);
  position: sticky;
  top: 0;
  z-index: 90;
}

.user-status { font-size: 0.8rem; padding: 4px 12px; border-radius: 20px; background: rgba(244, 63, 94, 0.2); color: var(--danger); }
.user-status.online { background: rgba(16, 185, 129, 0.2); color: var(--success); }

.content-area { padding: 3rem 2rem; max-width: 1200px; margin: 0 auto; }

.card {
  background: var(--glass);
  backdrop-filter: blur(16px);
  border: 1px solid var(--border);
  border-radius: 24px;
  padding: 2rem;
  margin-bottom: 2rem;
}

/* CUSTOM SCANNER */
.scanner-wrapper { text-align: center; max-width: 600px; margin: 0 auto 2rem; padding: 1rem; }
.scanner-header { margin-bottom: 1rem; }
.camera-info { font-size: 0.9rem; color: #94a3b8; }
.camera-info.active { color: var(--success); font-weight: 600; }

.preview-container {
  position: relative;
  width: 100%;
  aspect-ratio: 1;
  background: #000;
  border-radius: 20px;
  overflow: hidden;
  margin-bottom: 1.5rem;
  border: 2px solid var(--border);
}

#reader { width: 100%; height: 100%; }

.camera-placeholder {
  position: absolute;
  top: 0; left: 0; width: 100%; height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #475569;
}

.icon-large { font-size: 4rem; margin-bottom: 1rem; }

.scan-success-overlay {
  position: absolute;
  top: 0; left: 0; width: 100%; height: 100%;
  background: rgba(2, 6, 23, 0.95);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 50;
}

.success-card { padding: 2rem; width: 80%; }
.success-icon { font-size: 3rem; margin-bottom: 1rem; }
.id-display { font-size: 2.2rem; font-weight: 800; color: var(--primary); margin: 1rem 0 2rem; }

.scanner-controls { display: flex; gap: 1rem; justify-content: center; }

/* BUTTONS */
.btn {
  padding: 1rem 2rem;
  border-radius: 14px;
  font-weight: 700;
  cursor: pointer;
  border: none;
  transition: all 0.2s;
  font-size: 1rem;
}

.btn:active { transform: scale(0.96); }
.btn-primary { background: var(--primary); color: white; }
.btn-success { background: var(--success); color: white; }
.btn-danger { background: var(--danger); color: white; }
.btn-secondary { background: #334155; color: white; }
.btn-validate { background: var(--success); color: white; width: 100%; font-size: 1.2rem; margin-bottom: 1rem; }
.btn-link { background: transparent; color: #94a3b8; text-decoration: underline; border: none; cursor: pointer; }

/* ESTOQUE */
.estoque-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 1.5rem; }
.qty-value { font-size: 2rem; font-weight: 800; color: white; }

/* ANIMATIONS */
.slide-fade-enter-active { transition: all 0.3s ease-out; }
.slide-fade-leave-active { transition: all 0.4s cubic-bezier(1, 0.5, 0.8, 1); }
.slide-fade-enter-from, .slide-fade-leave-to { transform: translateY(20px); opacity: 0; }

.pulse { animation: pulse 2s infinite; }
@keyframes pulse { 
  0% { box-shadow: 0 0 0 0 rgba(16, 185, 129, 0.4); } 
  70% { box-shadow: 0 0 0 20px rgba(16, 185, 129, 0); } 
  100% { box-shadow: 0 0 0 0 rgba(16, 185, 129, 0); } 
}

/* MOBILE */
@media (max-width: 768px) {
  .sidebar { transform: translateX(-100%); }
  .sidebar-open .sidebar { transform: translateX(0); }
  .main-content { margin-left: 0; }
  .top-bar { padding: 0 1.5rem; }
  .menu-toggle { display: block; background: transparent; border: none; color: white; font-size: 1.8rem; }
  .sidebar-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.6); z-index: 95; backdrop-filter: blur(4px); }
  .content-area { padding: 1.5rem 1rem; }
}

@media print {
  body * { display: none; }
  .printable-area, .printable-area * { display: block; }
  .printable-area { position: absolute; top: 0; left: 0; width: 100%; text-align: center; color: black; }
}
</style>
