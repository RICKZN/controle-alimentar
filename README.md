 O que é o projeto?

É um *sistema de controle de refeições* para uma instituição de ensino (aparentemente um IF — Instituto Federal). Ele controla quem pode comer no refeitório, gerencia o estoque de alimentos e gera fichas com QR Code para os alunos.

---

 Backend — Java com Spring Boot

*pom.xml* é o arquivo de configuração do Maven. Ele declara as dependências do projeto:
- spring-boot-starter-web — cria o servidor HTTP e os endpoints REST
- spring-boot-starter-data-jpa — abstrai o banco de dados usando o Hibernate
- postgresql — driver para conectar ao PostgreSQL em nuvem
- h2 e mysql-connector-j — estão presentes mas não são usados na config atual (provavelmente legado)

application.properties é a configuração do servidor. Ele lê as credenciais do banco via variáveis de ambiente (SPRING_DATASOURCE_URL, etc.), o que é uma boa prática para deploy em nuvem. A porta também é configurável via ${PORT:8081}. O Hikari é o pool de conexões, configurado para evitar vazamentos.

ControleApplication.java é o ponto de entrada da aplicação. Ele tem uma lógica extra interessante: antes de iniciar o Spring, ele lê a SPRING_DATASOURCE_URL e, se ela estiver no formato postgres://user:pass@host/db (formato do Render/Railway), ele a converte para o formato JDBC padrão e injeta como propriedade do sistema. Isso é necessário porque plataformas como o Render entregam a URL nesse formato legado.

Os Models (Aluno.java, Estoque.java, RegistroAtendimento.java) são classes Java anotadas com @Entity — o JPA as mapeia para tabelas no banco automaticamente:
- Aluno — tem id, matrícula, nome e ultimaRefeicao (timestamp do último acesso)
- Estoque — tem id, nome do alimento, unidade (kg, litros) e quantidade
- RegistroAtendimento — registra cada refeição servida com matrícula e data/hora

Os Repositories são interfaces que estendem JpaRepository. Isso significa que o Spring gera automaticamente todos os métodos de CRUD (save, findById, findAll, delete...) sem precisar escrever SQL. O AlunoRepository tem um método customizado findByMatricula(String) que o Spring implementa automaticamente baseado no nome do método.

ControleController.java é o coração do backend. Ele expõe uma API REST no caminho /api com @CrossOrigin("*") para aceitar requisições do Vue.js. Os endpoints mais importantes:
- GET /api/estoque e GET /api/alunos — listam os dados
- POST /api/validar?matricula= — implementa a regra de negócio principal: verifica se o aluno existe, se já comeu nas últimas 6 horas (comparando ultimaRefeicao com o horário atual), bloqueia ou libera, e retorna os minutos restantes caso bloqueado
- O método @PostConstruct initData() roda automaticamente na inicialização e popula o banco com dados iniciais se estiver vazio

---

 Frontend — Vue.js 3

main.js é o ponto de entrada mínimo: cria a app Vue e monta no #app do index.html.

App.vue é um único arquivo que contém toda a aplicação (template + script + style). Ele usa a *Composition API* do Vue 3 com <script setup>, que é a forma moderna de escrever componentes. As principais funcionalidades:

- Sidebar com 4 abas: Validação, Alunos, Estoque e Gerar Fichas
- Câmera e QR Code: usa a biblioteca html5-qrcode para ligar a câmera do dispositivo, detectar QR Codes, e chamar a API de validação automaticamente. Também suporta troca entre câmera frontal e traseira
- Contagem regressiva: quando um aluno é bloqueado, um setInterval atualiza o contador em tempo real (horas, minutos e segundos restantes)
- Geração de QR Code: usa a biblioteca qrcode.vue para gerar um QR Code com a matrícula do aluno, que pode ser impresso
- axios faz todas as chamadas HTTP para a API do backend
- *Computed property alunosFiltrados*: filtra a lista de alunos em tempo real conforme o usuário digita na busca, sem consultar o servidor
- *VITE_API_URL*: a URL da API é configurável via variável de ambiente do Vite; se não definida, usa /api (relativo, útil quando frontend e backend estão no mesmo servidor)

---

## Infraestrutura e Deploy

*Dockerfile* orquestra o build completo em um único container: instala Node.js numa imagem Maven, compila o frontend com npm run build, copia os arquivos gerados para backend/src/main/resources/static/ (o Spring Boot serve arquivos estáticos dessa pasta), depois compila o backend com Maven. O resultado é um único JAR que serve tanto a API quanto o frontend.

*vercel.json* configura o deploy do frontend na Vercel. Ele faz um proxy: qualquer requisição para /api/* é redirecionada para o backend no Render, e qualquer outra rota vai para o index.html (necessário para o Vue Router funcionar como SPA).

*application.properties* mostra que o banco PostgreSQL fica em nuvem (Neon ou Supabase, dado o comentário sobre sslmode=require), o backend no Render (via Docker), e o frontend na Vercel.


