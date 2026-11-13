# UniSync — Centralizador de Oportunidades Acadêmicas

O **UniSync** é uma plataforma acadêmica desenvolvida para centralizar oportunidades, facilitar o acesso a informações e auxiliar estudantes na organização de suas atividades.

O projeto busca solucionar um problema comum no ambiente universitário: informações sobre palestras, eventos, estágios, projetos e outras oportunidades frequentemente ficam espalhadas por diferentes canais de comunicação.

A proposta é reunir essas informações em um único ambiente, oferecendo recursos para professores e alunos.

## 1. Objetivos

- Centralizar a divulgação de oportunidades acadêmicas.
- Facilitar o acesso dos estudantes a eventos e atividades.
- Permitir que professores publiquem e gerenciem oportunidades.
- Oferecer gerenciamento de tarefas pessoais aos estudantes.
- Aplicar conceitos de desenvolvimento distribuído, APIs REST, autenticação, persistência de dados e conteinerização.

## 2. Tecnologias utilizadas

| Camada | Tecnologias |
|---|---|
| Backend de autenticação | C#, ASP.NET Core |
| Backend de oportunidades e tarefas | Python, FastAPI |
| Banco de dados relacional | PostgreSQL |
| Banco de dados NoSQL | MongoDB |
| Persistência Python | Beanie e Pydantic |
| Frontend web do aluno | Vue.js, Vite e JavaScript |
| Shell web | Django |
| Aplicativo Android | Kotlin e Jetpack Compose |
| Autenticação entre APIs | JWT |
| Infraestrutura | Docker e Docker Compose |
| Testes Python | Pytest e HTTPX |
| Versionamento | Git e GitHub |

O repositório contém componentes em diferentes estágios de desenvolvimento. As funcionalidades descritas na próxima seção correspondem ao escopo implementado ou integrado até a versão revisada do projeto.

## 3. Funcionalidades

### Autenticação e usuários

O backend .NET gerencia o cadastro e a autenticação dos usuários, incluindo perfis de alunos e professores.

Após um login válido, a API emite um token JWT, utilizado para identificar o usuário nas operações protegidas do sistema.

### Gerenciamento de oportunidades

O backend FastAPI permite:

- Criar oportunidades acadêmicas.
- Listar oportunidades cadastradas.
- Buscar oportunidades por identificador.
- Filtrar e ordenar resultados.
- Atualizar oportunidades.
- Excluir oportunidades.

As operações de criação, atualização e exclusão são protegidas por autenticação e permissões de professor.

O sistema associa a oportunidade ao professor autenticado, utilizando sua identidade obtida pelo JWT.

### Gerenciamento de tarefas e Kanban

Os alunos podem organizar tarefas pessoais por meio dos estados:

- **A fazer**
- **Em andamento**
- **Concluído**

O backend oferece operações para criar, consultar, atualizar e excluir tarefas.

Cada tarefa pertence ao aluno autenticado, impedindo que outros alunos acessem ou modifiquem seus registros.

O portal web possui uma interface de Kanban integrada à API Python. Melhorias visuais e de interação podem ser desenvolvidas independentemente do backend.

## 4. Arquitetura do sistema

O UniSync utiliza uma arquitetura distribuída, separando responsabilidades entre serviços e interfaces.

### Backend .NET

Responsável por:

- Cadastro de usuários.
- Autenticação de alunos e professores.
- Emissão de tokens JWT.
- Regras de identidade e permissões.

**Banco de dados:** PostgreSQL.

### Backend Python

Responsável por:

- Gerenciamento de oportunidades acadêmicas.
- Gerenciamento de tarefas dos estudantes.
- Validação de tokens JWT emitidos pelo .NET.
- Aplicação das regras de autorização sobre oportunidades e tarefas.

**Banco de dados:** MongoDB, utilizando Beanie.

### Frontend web

O repositório contempla:

- **Django:** componente de shell/host web.
- **Vue.js:** portal web do aluno, incluindo a interface de Kanban.

### Aplicativo mobile

O projeto também contempla um módulo Android com Kotlin e Jetpack Compose, cujo desenvolvimento deve ser acompanhado separadamente.

### Fluxo de autenticação

1. O usuário informa suas credenciais ao backend .NET.
2. O .NET valida o cadastro no PostgreSQL.
3. Após autenticação válida, o .NET emite um JWT.
4. O frontend envia esse token às rotas protegidas do FastAPI.
5. O FastAPI valida o JWT e as permissões do usuário.
6. As operações autorizadas são realizadas no MongoDB.

Essa separação permite que os dois backends compartilhem a identidade dos usuários sem duplicar o sistema de login.

## 5. Estrutura do repositório

```text
UniSync/
├── .github/
│   └── workflows/
├── backend-dotnet/
│   └── Unisync.Api/
├── backend-python/
│   ├── src/
│   ├── tests/
│   ├── Dockerfile
│   ├── docker-compose.yml
│   └── README.md
├── frontend-shell/
├── frontend-web-aluno/
├── mobile-kotlin/
├── docs/
├── docker-compose.yml
└── README.md
```

O repositório possui um Docker Compose principal na raiz, além de configurações específicas em alguns módulos.

## 6. Executando o projeto com Docker

### Pré-requisitos

- Git.
- Docker Desktop com Docker Compose.
- Código-fonte do UniSync.
- Configuração local das variáveis de ambiente necessárias.

### Configuração da autenticação

O Compose principal utiliza a variável `UNISYNC_JWT_KEY`, que deve conter uma chave de autenticação em Base64.

A mesma chave é fornecida ao backend .NET e ao FastAPI, permitindo a validação compartilhada dos tokens JWT.

Configure essa variável em um arquivo `.env` local, na raiz do projeto, ou no ambiente do terminal.

**Nunca publique chaves reais, senhas ou tokens JWT no GitHub.**

### Inicialização

Na raiz do repositório, execute:

```bash
docker compose up -d --build
```

Esse comando constrói e inicia os serviços definidos no Compose principal:

- PostgreSQL.
- MongoDB.
- API .NET.
- API Python.
- Shell web.
- Frontend Vue.

Para conferir os serviços:

```bash
docker compose ps
```

Para acompanhar os logs:

```bash
docker compose logs --tail=50
```

Para encerrar sem remover os volumes de dados:

```bash
docker compose down
```

Evite `docker compose down -v` caso queira preservar os bancos de dados locais.

## 7. Endereços locais

Com a configuração padrão do Compose principal:

| Serviço | Endereço |
|---|---|
| API .NET | `http://localhost:5041` |
| FastAPI | `http://localhost:8000` |
| Swagger FastAPI | `http://localhost:8000/docs` |
| Health check FastAPI | `http://localhost:8000/health` |
| Frontend Vue | `http://localhost:5173` |
| Shell web | `http://localhost:8081` |

Os endereços dependem da inicialização bem-sucedida dos respectivos containers.

## 8. Testes automatizados

O backend Python utiliza Pytest para testar regras de negócio, validação de dados, autorização e endpoints HTTP.

Para executar a suíte sem os testes ponta a ponta, entre em `backend-python`:

```bash
docker compose run --rm --build --no-deps api python -m pytest -m "not e2e" -v
```

**Resultado da última execução informada:** 59 testes passando, excluindo o E2E.

### Testes de integração

O projeto também contém um teste E2E para validar o fluxo de autenticação do .NET e o CRUD de oportunidades do FastAPI.

Essa validação requer ambiente de teste configurado, conta de professor e MongoDB de teste.

**Situação:** a execução completa do E2E com login real permanece pendente de confirmação.

Para informações detalhadas sobre a API Python, consulte [backend-python/README.md](backend-python/README.md).

## 9. Desenvolvimento colaborativo

O desenvolvimento é realizado em equipe, utilizando Git e GitHub.

O fluxo recomendado é:

1. Criar uma branch para cada funcionalidade ou correção.
2. Implementar e validar as alterações.
3. Registrar commits com mensagens padronizadas.
4. Abrir um Pull Request.
5. Revisar as mudanças antes da integração à `main`.

Exemplos de mensagens de commit:

```text
feat: adiciona gerenciamento de tarefas
fix: corrige autoria das oportunidades
test: adiciona testes de autorizacao
docs: atualiza documentacao do projeto
```

## 10. Estado do desenvolvimento

### Implementado no escopo revisado

- Autenticação de usuários via .NET.
- Integração JWT entre .NET e FastAPI.
- CRUD de oportunidades.
- CRUD de tarefas pessoais.
- Persistência com PostgreSQL e MongoDB.
- Portal Vue com interface de Kanban conectada à API Python.
- Configurações Docker para execução dos serviços.
- Testes automatizados do backend Python.

### Pontos de integração e finalização

- Confirmar o E2E completo com login real do .NET.
- Alinhar com a equipe a exposição de dados nas consultas públicas de oportunidades.
- Finalizar ajustes de frontend e experiência de uso.
- Revisar a documentação e a integração dos módulos desenvolvidos pela equipe.

## 11. Equipe e responsabilidades

- **Matheus:** Backend e banco de dados.
- **Kamilly:** Frontend.
- **João:** Backend e banco de dados.
- **Samuel:** Backend e documentação LaTeX.
- **Reinoldes:** Frontend.

## 12. Apresentação do projeto

Acesse o [site de apresentação do UniSync](https://teste2234234.my.canva.site/in-cio) para saber mais sobre a proposta.

---

**UniSync — Conectando estudantes às oportunidades acadêmicas.**