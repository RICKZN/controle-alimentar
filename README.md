# 🍽️ Sistema de Controle Alimentar — SISTEMÁIF v3.5

> Sistema web para controle de refeições em refeitórios escolares/institucionais.  
> Permite validar fichas de alunos por **QR Code ou matrícula**, gerenciar o **cadastro de estudantes** e acompanhar o **estoque de alimentos** — tudo em uma única tela.

---

## 📸 Visão Geral

O sistema é composto por duas partes:

| Camada | Tecnologia | Função |
|--------|-----------|--------|
| **Backend** | Java 17 + Spring Boot 3.2 | API REST, regras de negócio, banco de dados |
| **Frontend** | Vue 3 + Vite | Interface visual no navegador |
| **Banco de dados** | PostgreSQL | Armazenamento de alunos, estoque e registros |

---

## ✨ Funcionalidades

### 📷 Validação de Fichas
- Leitura de **QR Code pela câmera** do dispositivo
- Entrada **manual por matrícula**
- Bloqueio automático: o aluno só pode receber uma refeição a cada **6 horas**
- Exibe quanto tempo falta para a próxima liberação

### 👥 Gestão de Alunos
- Cadastrar e excluir alunos (nome + matrícula)
- Visualizar status de cada aluno: **"Pode comer"** ou **"Aguardar"**
- Ver horário da última refeição e previsão da próxima liberação
- Filtro de pesquisa em tempo real

### 📦 Controle de Estoque
- Listagem dos alimentos disponíveis (nome, unidade, quantidade)
- Adicionar novos itens ao estoque
- Ajustar quantidade (entrada ou saída)
- Excluir itens

### 🎟️ Geração de Fichas
- Geração de **QR Codes** individuais para cada aluno cadastrado
- Fichas prontas para impressão

---

## 🗂️ Estrutura do Projeto

```
controle-alimentar-spring-vue/
├── backend/                        # API Spring Boot (Java)
│   ├── pom.xml                     # Dependências do projeto Java
│   └── src/main/java/com/sistema/controle/
│       ├── ControleApplication.java        # Ponto de entrada da aplicação
│       ├── controller/
│       │   └── ControleController.java     # Todos os endpoints da API REST
│       ├── model/
│       │   ├── Aluno.java                  # Entidade: aluno (id, matrícula, nome, últimaRefeicao)
│       │   ├── Estoque.java                # Entidade: item de estoque (nome, unidade, quantidade)
│       │   └── RegistroAtendimento.java    # Entidade: log de cada refeição servida
│       └── repository/
│           ├── AlunoRepository.java
│           ├── EstoqueRepository.java
│           └── RegistroAtendimentoRepository.java
│
├── frontend/                       # Interface Vue 3 (JavaScript)
│   ├── src/
│   │   ├── App.vue                 # Componente principal com todas as telas
│   │   └── main.js                 # Inicialização do Vue
│   ├── package.json                # Dependências do projeto JavaScript
│   └── vite.config.js              # Configuração do bundler
│
└── Dockerfile                      # Constrói e executa tudo em um único container
```

---

## 🔌 Endpoints da API

| Método | Rota | Descrição |
|--------|------|-----------|
| `GET` | `/api/alunos` | Lista todos os alunos |
| `POST` | `/api/alunos` | Cadastra novo aluno |
| `DELETE` | `/api/alunos/{id}` | Remove um aluno |
| `POST` | `/api/validar?matricula=XXX` | Valida e registra refeição do aluno |
| `GET` | `/api/estoque` | Lista todos os itens do estoque |
| `POST` | `/api/estoque` | Adiciona novo item ao estoque |
| `POST` | `/api/estoque/{id}/ajustar?variacao=N` | Ajusta quantidade de um item |
| `DELETE` | `/api/estoque/{id}` | Remove item do estoque |

---

## 🚀 Como Executar

Escolha **uma** das opções abaixo. A mais fácil para iniciantes é o **Docker**.

---

### ✅ Opção 1 — Docker (Recomendado para iniciantes)

> O Docker cuida de tudo automaticamente: instala Java, Node.js e compila o projeto.

#### Pré-requisitos
- Instalar o **Docker Desktop**: https://www.docker.com/products/docker-desktop  
  *(disponível para Windows, Mac e Linux — basta baixar e instalar como qualquer programa)*

#### Passo a passo

**1. Abra o terminal** (no Windows, use o "Prompt de Comando" ou "PowerShell")

**2. Entre na pasta do projeto:**
```bash
cd caminho/para/controle-alimentar-spring-vue-master
```

**3. Configure o banco de dados** criando um arquivo `.env` na raiz do projeto com o seguinte conteúdo:
```env
SPRING_DATASOURCE_URL=jdbc:postgresql://SEU_HOST:5432/SEU_BANCO
SPRING_DATASOURCE_USERNAME=seu_usuario
SPRING_DATASOURCE_PASSWORD=sua_senha
```
> 💡 Se não tiver um PostgreSQL externo, veja a Opção 3 abaixo com Docker Compose.

**4. Construa a imagem:**
```bash
docker build -t controle-alimentar .
```
*(Isso pode demorar alguns minutos na primeira vez — está baixando tudo que precisa)*

**5. Execute o container:**
```bash
docker run -p 8081:8081 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://SEU_HOST:5432/SEU_BANCO \
  -e SPRING_DATASOURCE_USERNAME=seu_usuario \
  -e SPRING_DATASOURCE_PASSWORD=sua_senha \
  controle-alimentar
```

**6. Acesse no navegador:**  
👉 http://localhost:8081

---

### ✅ Opção 2 — Execução Local (sem Docker)

> Para quem quer rodar direto na máquina, sem Docker.

#### Pré-requisitos

Instale os programas abaixo (caso ainda não tenha):

| Programa | Versão mínima | Link para download |
|---------|--------------|-------------------|
| **Java JDK** | 17 | https://adoptium.net |
| **Maven** | 3.8+ | https://maven.apache.org/download.cgi |
| **Node.js** | 20+ | https://nodejs.org |
| **PostgreSQL** | 14+ | https://www.postgresql.org/download |

> 💡 **Como verificar se já tem instalado:** abra o terminal e digite `java -version`, `mvn -version`, `node -version`. Se aparecer um número de versão, já está instalado.

#### Passo 1 — Configure o banco de dados

Crie um banco no PostgreSQL (pode usar o pgAdmin ou o terminal):
```sql
CREATE DATABASE controle_alimentar;
```

#### Passo 2 — Configure as variáveis de ambiente

No terminal, defina as variáveis antes de rodar (substitua pelos seus dados):

**Linux / Mac:**
```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/controle_alimentar
export SPRING_DATASOURCE_USERNAME=postgres
export SPRING_DATASOURCE_PASSWORD=suasenha
```

**Windows (Prompt de Comando):**
```cmd
set SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/controle_alimentar
set SPRING_DATASOURCE_USERNAME=postgres
set SPRING_DATASOURCE_PASSWORD=suasenha
```

#### Passo 3 — Build e execução do Backend

```bash
cd backend
mvn clean package -DskipTests
java -jar target/controle-alimentar-0.0.1-SNAPSHOT.jar
```

> O backend estará disponível em: http://localhost:8081

#### Passo 4 — Build e execução do Frontend (desenvolvimento)

Em outro terminal:
```bash
cd frontend
npm install
npm run dev
```

> O frontend estará disponível em: http://localhost:5173

---

### ✅ Opção 3 — Docker Compose (Docker + PostgreSQL integrados)

> Ideal para rodar tudo localmente sem configurar banco separado.

Crie um arquivo `docker-compose.yml` na raiz do projeto:

```yaml
version: '3.8'

services:
  db:
    image: postgres:15
    environment:
      POSTGRES_DB: controle_alimentar
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5432:5432"

  app:
    build: .
    ports:
      - "8081:8081"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://db:5432/controle_alimentar
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: postgres
    depends_on:
      - db
```

Depois execute:
```bash
docker-compose up --build
```

Acesse em: 👉 http://localhost:8081

---

## ☁️ Deploy em Nuvem

O projeto está configurado para rodar em plataformas como **Railway**, **Render** ou **Heroku**.

### Variáveis de ambiente necessárias

Configure as seguintes variáveis no painel da plataforma escolhida:

| Variável | Descrição | Exemplo |
|---------|-----------|---------|
| `SPRING_DATASOURCE_URL` | URL de conexão com o PostgreSQL | `jdbc:postgresql://host:5432/banco` |
| `SPRING_DATASOURCE_USERNAME` | Usuário do banco | `postgres` |
| `SPRING_DATASOURCE_PASSWORD` | Senha do banco | `minhasenha123` |
| `PORT` | Porta da aplicação (opcional) | `8081` |

> 💡 A maioria das plataformas de nuvem fornece um banco PostgreSQL gratuito. Ao criar o banco, elas também te dão a URL de conexão pronta para copiar e colar.

---

## ⚙️ Tecnologias Utilizadas

### Backend
- **Java 17** — Linguagem de programação
- **Spring Boot 3.2** — Framework para criar a API
- **Spring Data JPA** — Comunicação com o banco de dados
- **PostgreSQL** — Banco de dados principal
- **Maven** — Gerenciador de dependências

### Frontend
- **Vue 3** — Framework JavaScript para a interface
- **Vite** — Ferramenta de build rápida
- **Axios** — Requisições HTTP para o backend
- **html5-qrcode** — Leitura de QR Code pela câmera
- **qrcode.vue** — Geração de QR Codes

### Infraestrutura
- **Docker** — Containerização da aplicação

---

## 🧩 Dados Iniciais (Seed)

Ao iniciar pela primeira vez, o sistema cria automaticamente:

**Alunos de exemplo:**
| Matrícula | Nome |
|-----------|------|
| 2023001 | João Silva |
| 2023002 | Maria Oliveira |
| 2023003 | Pedro Santos |

**Estoque inicial:**
| Item | Unidade | Quantidade |
|------|---------|-----------|
| Arroz | kg | 50 |
| Feijão | kg | 30 |
| Carnes Vermelhas | kg | 20 |
| Frango | kg | 25 |
| Óleo | litros | 10 |

---

## ❓ Problemas Comuns

**"Não consigo conectar ao banco de dados"**  
→ Verifique se o PostgreSQL está rodando e se as variáveis de ambiente estão corretas.

**"Porta 8081 já está em uso"**  
→ Mude a porta no comando: `-e PORT=8082` (Docker) ou `--server.port=8082` (Java).

**"npm install falhou"**  
→ Certifique-se de estar usando Node.js versão 20 ou superior (`node -version`).

**"A câmera não abre no navegador"**  
→ O navegador precisa de permissão para acessar a câmera. Clique em "Permitir" quando solicitado. Em produção, é necessário HTTPS para a câmera funcionar.

---

## 📄 Licença

Este projeto é de uso livre para fins educacionais.
