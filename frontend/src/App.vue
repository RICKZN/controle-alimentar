<template>
  <div class="app-container">
    <header>
      <h1>Distribuição Alimentar</h1>
      <p>Instituição Federal de Ensino - Controle de Estoque</p>
    </header>

    <main class="dashboard">
      <!-- Card de Estoque -->
      <section class="card glass-effect estoque-section">
        <h2>Estoque de Ingredientes</h2>
        <div class="estoque-grid">
          <div v-for="item in estoqueList" :key="item.id" class="estoque-item-card">
            <div class="item-info">
              <h3>{{ item.nome }}</h3>
              <span class="unidade">Unidade: {{ item.unidade }}</span>
            </div>
            <div class="item-controls">
              <button @click="ajustarEstoque(item.id, -1)" class="btn-adjust minus">-</button>
              <div class="quantidade-display">
                <span class="qtd-numero">{{ formatarQuantidade(item.quantidade) }}</span>
              </div>
              <button @click="ajustarEstoque(item.id, 1)" class="btn-adjust plus">+</button>
            </div>
          </div>
        </div>
      </section>

      <!-- Card de Validação -->
      <section class="card glass-effect">
        <h2>Validar Estudante</h2>
        <p class="subtitle">Aproxime o QR Code do estudante (Requer Câmera)</p>
        <div id="reader"></div>
        <div class="manual-entry">
          <p>Ou insira manualmente:</p>
          <div class="input-group flex-row">
            <input type="text" v-model="matriculaParaValidar" placeholder="Matrícula" />
            <button @click="validarFicha" class="btn btn-primary">Validar</button>
          </div>
        </div>
        
        <div v-if="mensagem" :class="['alert', mensagemTipo]">
          {{ mensagem }}
        </div>
      </section>

      <!-- Card de Geração (Opcional/Administrativo) -->
      <section class="card glass-effect">
        <h2>Gerar Ficha (QR Code)</h2>
        <div class="input-group">
          <label>Número de Matrícula</label>
          <input type="text" v-model="matriculaParaGerar" placeholder="Ex: 2023100456" />
        </div>
        <button @click="gerarQrCode" class="btn btn-primary">Gerar QR Code</button>
        
        <div v-if="qrCodeGerado" class="qr-container">
          <qrcode-vue :value="matriculaParaGerar" :size="200" level="H" />
          <p class="qr-matricula-display">Matrícula: {{ matriculaParaGerar }}</p>
          <button @click="imprimirFicha" class="btn btn-secondary btn-small">Imprimir</button>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import axios from 'axios';
import QrcodeVue from 'qrcode.vue';
import { Html5QrcodeScanner } from 'html5-qrcode';

const API_URL = import.meta.env.VITE_API_URL || '/api';

const estoqueList = ref([]);
const matriculaParaGerar = ref('');
const qrCodeGerado = ref(false);

const matriculaParaValidar = ref('');
const mensagem = ref('');
const mensagemTipo = ref('');

let html5QrcodeScanner = null;

const carregarEstoque = async () => {
  try {
    const res = await axios.get(`${API_URL}/estoque`);
    estoqueList.value = res.data;
  } catch (error) {
    console.error("Erro ao carregar estoque", error);
  }
};

const ajustarEstoque = async (id, variacao) => {
  try {
    await axios.post(`${API_URL}/estoque/${id}/ajustar?variacao=${variacao}`);
    await carregarEstoque();
  } catch (error) {
    console.error("Erro ao ajustar estoque", error);
  }
};

const formatarQuantidade = (qtd) => {
  return Number.isInteger(qtd) ? qtd : qtd.toFixed(1);
};

const gerarQrCode = () => {
  if (!matriculaParaGerar.value) {
    alert("Insira a matrícula.");
    return;
  }
  qrCodeGerado.value = true;
};

const imprimirFicha = () => {
  window.print();
};

const validarFicha = async () => {
  if (!matriculaParaValidar.value) return;
  try {
    const res = await axios.post(`${API_URL}/validar?matricula=${matriculaParaValidar.value}`);
    mensagem.value = res.data.message;
    mensagemTipo.value = 'success';
    matriculaParaValidar.value = '';
    
    setTimeout(() => { mensagem.value = ''; }, 5000);
  } catch (error) {
    mensagem.value = error.response?.data?.error || "Erro ao validar ficha.";
    mensagemTipo.value = 'error';
    
    setTimeout(() => { mensagem.value = ''; }, 5000);
  }
};

const onScanSuccess = async (decodedText) => {
  console.log(`Scan result: ${decodedText}`);
  matriculaParaValidar.value = decodedText;
  await validarFicha();
};

onMounted(() => {
  carregarEstoque();
  
  // Inicialização mais robusta do scanner
  setTimeout(() => {
    try {
      html5QrcodeScanner = new Html5QrcodeScanner(
        "reader",
        { 
          fps: 10, 
          qrbox: { width: 250, height: 250 },
          aspectRatio: 1.0,
          rememberLastUsedCamera: true,
          supportedScanTypes: [0] // 0 = QR Code only
        },
        /* verbose= */ false
      );
      
      // Tenta forçar a câmera traseira em celulares
      html5QrcodeScanner.render(onScanSuccess, (errorMessage) => {
        // Ignora erros de frame (normal enquanto não foca)
      });
    } catch (err) {
      console.error("Falha ao iniciar scanner:", err);
      mensagem.value = "Erro ao acessar câmera. Verifique as permissões.";
      mensagemTipo.value = "error";
    }
  }, 1000); // 1 segundo de delay para garantir estabilidade no mobile
});

onUnmounted(() => {
  if (html5QrcodeScanner) {
    html5QrcodeScanner.clear();
  }
});
</script>

<style>
:root {
  --bg-color: #0f172a;
  --text-color: #f8fafc;
  --card-bg: rgba(30, 41, 59, 0.7);
  --primary: #3b82f6;
  --primary-hover: #2563eb;
  --secondary: #475569;
  --secondary-hover: #334155;
  --success: #10b981;
  --danger: #ef4444;
  --glass-border: rgba(255, 255, 255, 0.1);
}

body {
  font-family: 'Inter', sans-serif;
  background-color: var(--bg-color);
  color: var(--text-color);
  margin: 0;
  min-height: 100vh;
  background-image: radial-gradient(circle at top right, #1e293b, #0f172a);
}

.app-container {
  padding: 1.5rem;
  max-width: 1200px;
  margin: 0 auto;
}

header {
  text-align: center;
  margin-bottom: 2.5rem;
}

header h1 {
  font-size: 2.2rem;
  background: linear-gradient(to right, #60a5fa, #3b82f6);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  margin-bottom: 0.5rem;
}

header p {
  color: #94a3b8;
  font-size: 1rem;
}

.dashboard {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 1.5rem;
}

.card {
  background: var(--card-bg);
  border: 1px solid var(--glass-border);
  border-radius: 16px;
  padding: 1.5rem;
  backdrop-filter: blur(10px);
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
}

.card h2 {
  font-size: 1.25rem;
  margin-bottom: 1.2rem;
  font-weight: 600;
  color: #e2e8f0;
  border-bottom: 1px solid var(--glass-border);
  padding-bottom: 0.5rem;
}

/* NOVO ESTILO PARA O ESTOQUE */
.estoque-section {
  grid-column: 1 / -1; /* Ocupa toda a largura se tiver espaço */
}

.estoque-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 1rem;
}

.estoque-item-card {
  background: rgba(15, 23, 42, 0.6);
  border: 1px solid var(--glass-border);
  border-radius: 12px;
  padding: 1rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  transition: transform 0.2s ease;
}

.estoque-item-card:hover {
  transform: translateY(-2px);
  border-color: rgba(96, 165, 250, 0.5);
}

.item-info h3 {
  margin: 0 0 0.25rem 0;
  font-size: 1.1rem;
  color: #fff;
}

.item-info .unidade {
  font-size: 0.85rem;
  color: #94a3b8;
}

.item-controls {
  display: flex;
  align-items: center;
  gap: 0.8rem;
}

.btn-adjust {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: none;
  font-size: 1.5rem;
  font-weight: bold;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  transition: all 0.2s;
  user-select: none;
}

.btn-adjust.minus {
  background-color: rgba(239, 68, 68, 0.2);
  color: #f87171;
  border: 1px solid rgba(239, 68, 68, 0.5);
}
.btn-adjust.minus:active { background-color: rgba(239, 68, 68, 0.4); }

.btn-adjust.plus {
  background-color: rgba(16, 185, 129, 0.2);
  color: #34d399;
  border: 1px solid rgba(16, 185, 129, 0.5);
}
.btn-adjust.plus:active { background-color: rgba(16, 185, 129, 0.4); }

.quantidade-display {
  background: rgba(0, 0, 0, 0.3);
  padding: 0.5rem 0.8rem;
  border-radius: 8px;
  min-width: 50px;
  text-align: center;
}

.qtd-numero {
  font-size: 1.2rem;
  font-weight: bold;
  color: #e2e8f0;
}

/* OUTROS ESTILOS */
.input-group {
  display: flex;
  flex-direction: column;
  margin-bottom: 1.2rem;
}

.input-group.flex-row {
  flex-direction: row;
  gap: 0.5rem;
  margin-bottom: 0;
}

label {
  font-size: 0.9rem;
  color: #cbd5e1;
  margin-bottom: 0.4rem;
}

input {
  background: rgba(15, 23, 42, 0.6);
  border: 1px solid var(--glass-border);
  color: white;
  padding: 0.75rem 1rem;
  border-radius: 8px;
  font-size: 1rem;
  outline: none;
  width: 100%;
  box-sizing: border-box;
}

input:focus { border-color: var(--primary); }

.btn {
  padding: 0.75rem 1.5rem;
  border-radius: 8px;
  font-weight: 600;
  cursor: pointer;
  border: none;
  transition: all 0.2s ease;
  width: 100%;
  touch-action: manipulation;
}

.btn-small { padding: 0.5rem 1rem; font-size: 0.9rem; }
.btn-primary { background-color: var(--primary); color: white; }
.btn-primary:hover { background-color: var(--primary-hover); }
.btn-secondary { background-color: var(--secondary); color: white; }
.btn-secondary:hover { background-color: var(--secondary-hover); }

.qr-container {
  margin-top: 1.5rem;
  background: white;
  padding: 1.5rem;
  border-radius: 12px;
  text-align: center;
  color: black;
}

.qr-matricula-display {
  margin: 1rem 0;
  font-weight: bold;
  font-size: 1.2rem;
}

.subtitle {
  font-size: 0.9rem;
  color: #94a3b8;
  margin-bottom: 1rem;
}

#reader {
  width: 100%;
  border-radius: 8px;
  overflow: hidden;
  margin-bottom: 1.5rem;
  border: 1px solid var(--glass-border);
  background: rgba(255,255,255,0.05);
}

.manual-entry {
  margin-top: auto;
  border-top: 1px dashed var(--glass-border);
  padding-top: 1.5rem;
}

.alert {
  padding: 1rem;
  border-radius: 8px;
  margin-top: 1rem;
  font-weight: 600;
  text-align: center;
  animation: fadeIn 0.3s ease-in-out;
}

.alert.success {
  background-color: rgba(16, 185, 129, 0.2);
  color: #34d399;
  border: 1px solid #10b981;
}

.alert.error {
  background-color: rgba(239, 68, 68, 0.2);
  color: #f87171;
  border: 1px solid #ef4444;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(-10px); }
  to { opacity: 1; transform: translateY(0); }
}

@media print {
  body * { visibility: hidden; }
  .qr-container, .qr-container * { visibility: visible; }
  .qr-container {
    position: absolute;
    left: 0;
    top: 0;
    width: 100%;
    height: 100vh;
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    box-shadow: none;
  }
  .btn-small { display: none; }
}
</style>
