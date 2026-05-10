# Como tornar seu sistema permanente na nuvem (Grátis)

Para que o site funcione 24h por dia, mesmo com seu computador desligado, recomendo usar o **Render.com**. Siga estes passos:

### 1. Criar uma conta
- Acesse [Render.com](https://render.com) e crie uma conta gratuita (pode usar seu Google ou GitHub).

### 2. Criar um Banco de Dados MySQL na Nuvem
Como o banco de dados atual está no seu PC, você precisará de um na nuvem:
- No Render, clique em **New** > **PostgreSQL** (ou use um serviço como **Aiven.io** para MySQL gratuito).
- Copie a URL de conexão.

### 3. Subir o código para o GitHub
- Crie um repositório privado no GitHub.
- Suba todos os arquivos desta pasta para lá.

### 4. Fazer o Deploy no Render
- No painel do Render, clique em **New** > **Web Service**.
- Conecte seu repositório do GitHub.
- Use estas configurações:
  - **Runtime:** `Docker`
  - **Plan:** `Free`
- Nas **Environment Variables**, adicione as informações do seu banco de dados na nuvem (URL, usuário e senha).

### 5. Pronto!
O Render vai ler o arquivo `Dockerfile` que eu criei, construir o sistema e te dar um link permanente (ex: `https://controle-alimentar.onrender.com`).

---

## Por que não posso fazer isso sozinho agora?
Como sou um assistente rodando no seu computador, eu não tenho acesso às suas contas pessoais (Google/GitHub/Render). Para sua segurança, o deploy final exige que você faça o login uma única vez nessas plataformas.
