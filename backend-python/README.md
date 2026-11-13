# UniSync — Backend Python

API desenvolvida em Python com FastAPI para o gerenciamento de oportunidades acadêmicas e tarefas pessoais dos estudantes da plataforma UniSync.

## 1. Tecnologias utilizadas

- **Python 3.12:** linguagem de programação.
- **FastAPI:** framework responsável pela API HTTP.
- **MongoDB 8.0:** banco de dados NoSQL.
- **Beanie:** ODM utilizado para manipular documentos no MongoDB.
- **Pydantic:** validação e serialização de dados.
- **PyJWT:** validação dos tokens de autenticação.
- **Docker e Docker Compose:** execução dos serviços em containers.
- **Pytest e HTTPX:** testes automatizados.

## 2. Funcionalidades

### Oportunidades acadêmicas

Permite cadastrar, consultar, atualizar e excluir oportunidades, como eventos, projetos e atividades acadêmicas.

A criação, atualização e exclusão exigem autenticação de professor. As consultas de oportunidades são públicas na implementação atual.

O backend registra o identificador e o nome do professor autenticado. Na criação, o nome recebido no corpo da requisição é substituído pelo nome presente no JWT. O campo `professor` não pode ser alterado pelo PATCH.

### Tarefas e Kanban

Permite que alunos gerenciem suas próprias tarefas, utilizando os estados:

- `A_FAZER`
- `EM_ANDAMENTO`
- `CONCLUIDO`

Cada tarefa pertence a um aluno identificado pelo JWT. As operações de tarefas exigem autenticação de aluno, com verificação de propriedade para impedir acesso indevido aos registros de outros usuários.

## 3. Arquitetura

A API segue uma organização em camadas:

- **Routers:** definem endpoints HTTP e tratam as respostas.
- **Schemas:** validam os dados de entrada e saída.
- **Services:** implementam regras de negócio.
- **Repositories:** executam operações de persistência.
- **Models:** representam os documentos armazenados no MongoDB.
- **Core:** concentra configurações e autenticação.
- **Database:** gerencia a conexão com o MongoDB.

O banco de dados é inicializado durante o ciclo de vida da aplicação por meio do Beanie.

## 4. Integração com o backend .NET

O UniSync utiliza o backend .NET como serviço responsável pela autenticação e pelo cadastro dos usuários.

O processo funciona da seguinte maneira:

1. O usuário realiza login no .NET.
2. O .NET valida as credenciais e emite um JWT.
3. O cliente envia o JWT ao FastAPI no cabeçalho `Authorization: Bearer TOKEN`.
4. O FastAPI valida a assinatura, a expiração, o emissor e o público do token.
5. A API utiliza as informações autenticadas para aplicar as permissões.

O backend Python não realiza login nem armazena senhas de usuários.

## 5. Endpoints

### Saúde da aplicação

| Método | Endpoint | Acesso |
|---|---|---|
| GET | `/health` | Público |

### Oportunidades

| Método | Endpoint | Acesso |
|---|---|---|
| POST | `/oportunidades/criar` | Professor |
| GET | `/oportunidades/listar` | Público |
| GET | `/oportunidades/buscar/{id}` | Público |
| PATCH | `/oportunidades/atualizar/{id}` | Professor responsável |
| DELETE | `/oportunidades/excluir/{id}` | Professor responsável |

A listagem aceita filtros como `tipo`, `professor`, `futuros`, `ordenar` e `limite`.

### Tarefas

| Método | Endpoint | Acesso |
|---|---|---|
| POST | `/tarefas/criar` | Aluno |
| GET | `/tarefas/listar` | Aluno |
| GET | `/tarefas/buscar/{id}` | Aluno proprietário |
| PATCH | `/tarefas/atualizar/{id}` | Aluno proprietário |
| DELETE | `/tarefas/excluir/{id}` | Aluno proprietário |

A documentação interativa fica disponível em `/docs`.

## 6. Configuração do ambiente

O projeto utiliza variáveis de ambiente para conectar ao MongoDB e validar os tokens emitidos pelo .NET.

Crie ou configure o arquivo `.env` dentro de `backend-python`, conforme as configurações do ambiente da equipe:

```env
MONGODB_URL=mongodb://mongodb:27017
MONGODB_DATABASE=unisync

JWT_KEY=<chave-compartilhada-em-base64>
JWT_ISSUER=<emissor-configurado-no-dotnet>
JWT_AUDIENCE=<audiencia-configurada-no-dotnet>
```

A chave de autenticação deve corresponder à utilizada pelo backend .NET. Nunca publique credenciais reais, tokens ou segredos no GitHub.

## 7. Executando com Docker

Pré-requisito: Docker Desktop instalado e funcionando.

No terminal, entre na pasta `backend-python` e execute:

```bash
docker compose up -d --build
```

Esse comando constrói a imagem da API e inicia os serviços necessários, incluindo o MongoDB.

Para consultar o estado dos containers:

```bash
docker compose ps
```

Para consultar os logs:

```bash
docker compose logs --tail=50 api
```

Para verificar a aplicação:

- API: `http://localhost:8000`
- Swagger: `http://localhost:8000/docs`
- Health check: `http://localhost:8000/health`

Para encerrar os serviços sem apagar os volumes:

```bash
docker compose down
```

**Atenção:** não utilize `docker compose down -v` se quiser preservar os dados do MongoDB.

## 8. Testes automatizados

A aplicação utiliza Pytest para testes unitários, de segurança e HTTP.

Para executar os testes sem E2E usando Docker:

```bash
docker compose run --rm --build --no-deps api python -m pytest -m "not e2e" -v
```

**Validação do mural:** 137 testes unitários e HTTP aprovados, excluindo o teste Pytest marcado como E2E.

### Teste de integração E2E

Existe um teste que realiza login na API .NET e valida o CRUD completo de oportunidades com a API Python e MongoDB reais.

Esse teste depende de:

- API .NET acessível;
- conta de professor de teste;
- FastAPI apontando para um banco de testes isolado;
- variáveis `API_TEST_URL`, `DOTNET_AUTH_URL`, `E2E_PROFESSOR_EMAIL` e `E2E_PROFESSOR_PASSWORD`.

Com o ambiente configurado, execute:

```bash
python -m pytest -m e2e -v
```

O E2E com login real do .NET ainda está pendente de confirmação na revisão final. Não deve ser considerado aprovado apenas com base nos testes unitários e HTTP.

## 9. Estado atual do projeto

Estão implementados CRUD de oportunidades, tarefas e lembretes, turmas com matrícula por código, avisos com confirmação de leitura e publicações do mural. As rotas privadas validam identidade e permissões a partir do JWT emitido pelo .NET. A persistência utiliza MongoDB e Beanie.

Os fluxos Android usam as APIs locais diretamente. O script `../mobile-kotlin/scripts/testar-academico-local.sh` valida os ciclos reais de turmas, avisos, tarefas, lembretes e mural com contas técnicas. O teste Pytest E2E de oportunidades descrito acima é um roteiro separado e exige suas próprias variáveis de ambiente.

Continuam previstas a evolução do mural com comentários/interações e a definição de eventos remotos e notificações push. A existência desses itens no plano não significa que já façam parte dos contratos publicados.

## Mural colaborativo por turma

As rotas exigem o JWT da API .NET. Alunos matriculados e o professor responsável podem publicar e ler conteúdo nas turmas ativas às quais têm acesso. O autor edita o próprio texto; a exclusão é permitida ao autor ou ao professor responsável pela turma. Publicações de turmas arquivadas ficam indisponíveis.

| Método | Rota | Uso |
|---|---|---|
| GET | `/mural/listar` | Página de resumos das turmas autorizadas |
| POST | `/mural/criar` | Criar publicação em turma autorizada |
| GET | `/mural/buscar/{id}` | Detalhe com conteúdo completo |
| PATCH | `/mural/atualizar/{id}` | Editar publicação própria com controle de versão |
| DELETE | `/mural/excluir/{id}?versao=1` | Remover publicação da versão confirmada |

Criação recebe `turma_id`, `titulo` (3–100 caracteres) e `conteudo` (1–5000). A edição recebe `titulo`, `conteudo` e `versao` (inteiro positivo); turma, autor e perfil não podem ser alterados. Espaços nas extremidades são removidos e campos extras são recusados. Autor e perfil são obtidos do token no servidor.

A listagem aceita `turma_id` opcional, `limite` entre 1 e 50 (padrão 20) e `antes` como cursor da página anterior. Retorna `{ "itens": [...], "proxima_pagina": "id" }`; cursor nulo indica a última página. O filtro de autorização é aplicado em cada consulta. A ordem é decrescente por identificador; atualizar a primeira página incorpora novas publicações.

Cada resumo contém `id`, `turma_id`, `turma_nome`, `disciplina`, `autor_id`, `autor`, `papel_autor`, `titulo`, `resumo`, `criado_em`, `atualizado_em`, `versao`, `pode_editar` e `pode_excluir`. O detalhe acrescenta `conteudo`. Datas são expostas em UTC com fuso explícito. As permissões da resposta orientam a interface e continuam sendo verificadas no servidor.

Edição e exclusão conferem a versão atomicamente. Uma revisão desatualizada recebe **409**, sem sobrescrever ou remover a versão nova. A exclusão retira o item das consultas e registra internamente quem o removeu. Falta de acesso retorna **403**; publicação removida ou turma arquivada retorna **404**.

Esta entrega inclui publicações de texto, filtro e paginação. Comentários, reações, anexos e push não fazem parte deste contrato.
