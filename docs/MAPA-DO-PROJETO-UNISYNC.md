# Mapa do projeto UniSync

> Leitura do produto, das decisões e da implementação em 20/11/2026. Este guia organiza fontes existentes; não altera a proposta acadêmica original.

## 1. Como ler as fontes

1. **Proposta original:** [Documentação.pdf](Documentação.pdf), capítulos 4 a 9. Define o problema, o objetivo, a arquitetura e funcionalidades *idealizadas*. [Documentação.tex](Documentação.tex) é o fonte editável correspondente. O verbo “prever” no PDF não significa que uma função já existe no software.
2. **Referência de interface:** [Design Wireframe High Quality.pdf](Design%20Wireframe%20High%20Quality.pdf), páginas 2 a 8. A página 1 contém modelos genéricos do Canva e não descreve o UniSync. O wireframe mostra a organização visual desejada; regras de negócio e contratos exigem confirmação no código ou documentação.
3. **Estado do projeto:** [README principal](../README.md), READMEs de cada módulo, rotas, modelos e testes. O [plano mobile](PLANO-ENGENHARIA-SOFTWARE-MOBILE-KOTLIN.md) contém decisões, propostas e registros de implementação de momentos diferentes. Suas listas antigas de pendências não devem ser lidas como estado atual sem conferência no código.
4. **Acréscimos posteriores:** funcionalidades já construídas além da ideação inicial são apresentadas abaixo com sua origem. Nenhuma delas é atribuída retroativamente ao PDF original.

Neste mapa, **implementado** significa que há caminho de código; **testado** significa que há teste identificado; **validado em uso** exige demonstração ponta a ponta atual. Um contêiner ativo ou uma resposta HTTP 200, isoladamente, não valida o fluxo do usuário.

## 2. O produto em uma frase

O UniSync reúne oportunidades acadêmicas — palestras, eventos, estágios e projetos — para que estudantes as encontrem, filtrem e acompanhem seus prazos. O professor publica oportunidades. Web e Android são clientes das APIs; as APIs e seus bancos são a fonte dos dados compartilhados.

O PDF original propõe também alertas em tempo real no mobile, mas deixa o mecanismo para definição posterior. Kanban, calendário, turmas, avisos, lembretes e mural aparecem no wireframe ou no detalhamento posterior e precisam ser apresentados como expansão da experiência, com regras próprias.

## 3. Responsabilidade de cada componente

| Componente | Responsabilidade decidida | Implementação encontrada |
|---|---|---|
| Shell Web, Python + Django | Entrada, cadastro, login, sessão e composição da experiência Web | [Rotas do Shell](../frontend-shell/authentication/urls.py) e [template que monta o Vue](../frontend-shell/templates/authentication/app.html). A sessão do Django guarda o JWT recebido da API .NET. |
| Micro-Frontend, Vue.js | Feed de oportunidades no PDF original | [Rotas Vue](../frontend-web-aluno/src/router/index.js) incluem dashboard, professor e Kanban. O dashboard atual usa [OportunidadesPanel](../frontend-web-aluno/src/components/dashboard/OportunidadesPanel.vue); o Vue já cobre mais do que apenas o feed original. |
| Núcleo Acadêmico, C# + .NET | Identidade, cadastro, autenticação e permissões estruturadas | [UsuariosController](../backend-dotnet/Unisync.Api/Controllers/UsuariosController.cs) emite JWT e usa PostgreSQL na configuração local. |
| Motor de Oportunidades, Python + FastAPI | Oportunidades, busca e filtros | A [API Python](../backend-python/src/main.py) também implementa tarefas, lembretes, turmas, avisos e mural. Usa MongoDB/Beanie. |
| UniSync Mobile, Kotlin + Jetpack Compose | Consultar oportunidades e prazos no Android; experiências derivadas do wireframe | [Navegação por perfil](../mobile-kotlin/app/src/main/java/br/com/unisync/mobile/ui/principal/AreaPrincipal.kt), REST, Room, alertas locais e sincronização periódica. Não usa o Shell ou o Vue para renderizar telas. |
| PostgreSQL e MongoDB | Persistência relacional e de documentos | O [Compose local](../docker-compose.yml) executa PostgreSQL para .NET e MongoDB para FastAPI. SQL Server é alternativa citada no PDF, não o banco escolhido no ambiente atual. |

**Fluxo de identidade:** Web → Django → .NET → JWT → Vue/FastAPI. Android → .NET → JWT → FastAPI. O JWT identifica o usuário; autorização de leitura e escrita continua sendo conferida pelas APIs.

## 4. Matriz do wireframe à implementação

| Área e referência | Ação e regra principal | Serviço e dado | Situação encontrada |
|---|---|---|---|
| Entrada, escolha Aluno/Professor e cadastro (p. 2) | Escolher perfil, cadastrar e entrar | .NET `/api/usuarios` e `/api/usuarios/login`; usuário no PostgreSQL | Django possui cadastro/login e o Android tem fluxo próprio. O JWT é compartilhado. A página de cadastro do wireframe é referência visual, não contrato de campos definitivo. |
| Dashboard do aluno, prazos e avisos (p. 3–4) | Ver resumo e abrir o item de origem | Oportunidades, tarefas, turmas e avisos via FastAPI; PostgreSQL apenas para identidade | Android agrega dados reais no painel. O [dashboard Vue](../frontend-web-aluno/src/views/DashboardView.vue) hoje monta o painel de oportunidades, não todo o painel do wireframe. |
| Calendário (p. 3–4) | Localizar eventos, prazos e lembretes por período | Oportunidades, tarefas e lembretes via FastAPI; MongoDB | Android tem calendário. Não há rota Vue de calendário no [roteador atual](../frontend-web-aluno/src/router/index.js). |
| Kanban em três estados (p. 3, 4 e 6) | Aluno cria, move, edita e conclui tarefas pessoais | FastAPI `/tarefas/*`; MongoDB, protegido pelo JWT de aluno | Android e Vue possuem Kanban. Prioridade, prazo e vínculo com turma são acréscimos posteriores presentes no contrato atual. |
| Feed de oportunidades e eventos (PDF original; p. 4–5) | Consultar, filtrar e abrir detalhes; professor publica/edita as próprias oportunidades | FastAPI `/oportunidades/*`; MongoDB | Vue e Android possuem listagem e operações de professor. A listagem Vue mostra um recorte de até 15 itens no painel; isso não equivale a paginação completa do feed. |
| Turmas e disciplinas (p. 3–5) | Professor cria turma; aluno entra por código | FastAPI `/turmas/*`; coleção `turmas` no MongoDB | Android possui fluxos por perfil e cache local. Na Web, o Kanban consulta turmas para vincular tarefa; não há rota Vue própria para gerenciar turmas. |
| Avisos e tela do professor (p. 4, 7–8) | Professor publica/revisa; aluno consulta e confirma leitura | FastAPI `/avisos/*`; coleção `avisos` no MongoDB | Android possui CRUD, leitura e cache. Não há rota Vue própria de avisos no roteador atual. |
| Mural colaborativo (p. 3–5) | Participante publica e interage dentro da turma autorizada | FastAPI `/mural/*`, comentários, curtidas e anexos; MongoDB | Android possui mural e paginação. A API confere turma e JWT. Não há rota Vue do mural no roteador atual. |
| Lembretes (p. 5–8) | Criar, agendar, concluir e acompanhar | FastAPI `/lembretes/*`; MongoDB; alerta local no Android | Android e [painel Vue](../frontend-web-aluno/src/components/layout/RemindersPanel.vue) possuem lembretes. O alerta local depende de permissão e dados sincronizados. |

**Leitura visual do wireframe:** a página 2 reúne entrada, escolha de perfil e cadastro; a página 3 mostra o Kanban ao lado de calendário e avisos; a página 4 detalha um dashboard com postagens, prazos, Kanban, calendário e eventos; a página 5 mostra o painel de professor com atividades e lembretes; a página 6 isola o Kanban; e as páginas 7–8 repetem a tela de cadastro/lista de avisos. No telefone, essas composições horizontais precisam ser divididas em navegação e listas verticais, preservando a hierarquia das informações, sem copiar literalmente a disposição desktop.

Elementos decorativos, números de curtidas e textos fictícios do wireframe não são dados obrigatórios do produto sem contrato correspondente. O desenho também mostra rótulos como “Anotações” e “Dicas de estudos”, mas não define seu fluxo ou persistência; ficam registrados como elementos de interface sem implementação comprovada.

## 5. Percursos que devem funcionar de ponta a ponta

### Entrar e consultar oportunidades

1. O usuário abre o Shell Django na Web ou o app Kotlin no Android e entra com e-mail/senha.
2. A API .NET valida a conta no PostgreSQL e devolve um JWT; o cliente guarda a sessão.
3. O feed/painel consulta `GET /oportunidades/listar` no FastAPI. A busca por item usa `GET /oportunidades/buscar/{id}`.
4. A consulta pública pode exibir oportunidades; ações de publicação, edição e exclusão exigem professor autenticado. O FastAPI persiste no MongoDB.
5. Em falha de rede, a tela deve oferecer nova tentativa; no Android, itens já sincronizados podem permanecer no cache. A autorização é reavaliada pelo servidor nas operações protegidas.

### Professor publica; aluno acompanha

1. Professor autenticado cria uma turma pelo FastAPI. O servidor associa seu identificador e gera um código.
2. Aluno entra com o código; a API registra a matrícula. Somente participantes autorizados consultam os avisos daquela turma.
3. Professor publica ou revisa um aviso. O aluno consulta e marca como lido por uma chamada explícita; abrir o aviso não confirma leitura.
4. O Android mantém turmas e avisos em Room por conta, indica quando a leitura é offline e apaga dados privados no logout. Alterações exigem resposta da API antes de mostrar sucesso.

### Aluno organiza tarefas e prazos

1. O aluno cria ou altera tarefas no FastAPI com JWT; cada tarefa pertence ao seu identificador.
2. Vue e Android representam os estados Fazer, Fazendo e Concluído. O prazo aparece no painel/calendário Android e pode gerar alerta local conforme permissão e preferência.
3. O aluno também pode agendar lembretes. Mudanças feitas por um cliente aparecem no outro após nova consulta; ainda não existe aviso WebSocket imediato.

### Participar do mural

1. A API verifica JWT, turma ativa e acesso do participante antes de listar ou gravar publicações.
2. Android oferece publicação, comentários, curtidas e anexos privados. Listas de publicações/comentários usam cursor para carregar mais.
3. Não há tela Web do mural nem cache persistente de suas publicações no Android. Isso é uma lacuna de produto/implementação, não uma função presumida pelo wireframe.

## 6. Diferenças que exigem decisão, sem reescrever a proposta

| Tema | Documento original | Código atual | Consequência |
|---|---|---|---|
| Dados de turmas | PDF sugere dados estruturados, incluindo turmas, em SQL Server ou PostgreSQL | Turmas e avisos são documentos Beanie no MongoDB | Registrar a escolha de implementação ou planejar migração; não afirmar que a turma já está no relacional. |
| Escopo do Vue | Micro-Frontend do feed | Vue também oferece Kanban, lembretes e rotas de dashboard/professor | Documentar a expansão sem mudar o papel original do Shell. |
| Alertas em tempo real | Possibilidade prevista, mecanismo para definir depois | Alertas locais e WorkManager a cada 30 minutos; não há WebSocket, gateway, Redis/RabbitMQ ou FCM no código | Atualização atual não é imediata. Escolher mecanismo só quando o fluxo de evento e o critério de aceite estiverem definidos. |
| Demonstração em celular físico | Protótipo local pela rede Wi-Fi no plano mobile | Compose publica portas em `127.0.0.1`; Shell/Vite usam `localhost`; release exige URLs HTTPS | Computador/emulador e celular físico precisam de configurações de endereço distintas e teste real. |
| Estado descrito no plano mobile | Seções 31.3–31.4 ainda anunciam cache acadêmico e sincronização em segundo plano como pendentes | Cache Room de turmas/avisos e WorkManager já estão no Android | Tratar essas passagens como histórico até atualizar o registro; confirmar comportamento completo antes de chamar a entrega de validada. |

## 7. Sequência de trabalho e critério de aceite

1. **Fechar o mapa funcional:** revisar esta matriz com a equipe; cada tela deve ter origem, perfil, ação e destino explícitos. Não transformar rótulos do wireframe em endpoints sem decisão.
2. **Validar o núcleo original:** login real, publicação de oportunidade, busca/filtro e consulta de prazo em Web e Android, com .NET, FastAPI, PostgreSQL e MongoDB no mesmo ambiente. Registrar resultados e falhas.
3. **Completar os fluxos acrescentados:** comparar dashboard, calendário, Kanban, turmas, avisos, mural e lembretes com o wireframe; decidir quais lacunas Web são necessárias para a apresentação e testar permissões de aluno/professor.
4. **Preparar a demonstração local:** permitir acesso pelo IP do notebook apenas no perfil de demonstração, alinhar URLs e CORS, testar Shell/Vue/APIs no navegador e Android em aparelho físico. Preservar um perfil restrito para desenvolvimento local no computador.
5. **Definir atualização imediata se aprovada:** evento após gravação confirmada, canal autenticado para clientes ativos, filtragem por usuário/turma, reconexão e nova consulta REST. Broker e notificações com app fechado ficam fora dessa decisão até existir necessidade comprovada.
6. **Concluir distribuição:** testar o APK em aparelho físico com os endereços do ambiente escolhido, assinatura persistente e checksum. A exigência atual de HTTPS para release precisa ser conciliada com a demonstração HTTP local antes de produzir o APK final.

**Não declarar concluído** apenas porque um endpoint responde 200, um build passa ou um desenho existe. Para cada percurso, a aceitação pede ação real, persistência, leitura por outro cliente/perfil autorizado, falha recuperável e evidência do teste.

**Pontos de partida para os testes, sem presumir que passaram nesta revisão:** [testes de usuários .NET](../backend-dotnet/Unisync.Api.Tests/UsuariosTests.cs), [HTTP acadêmico Python](../backend-python/tests/integration/test_academico_http.py), [HTTP de oportunidades](../backend-python/tests/integration/test_oportunidades_http.py), [integração Web](../frontend-web-aluno/tests/browser/integracao.spec.js), [E2E Android com APIs locais](../mobile-kotlin/app/src/androidTest/java/br/com/unisync/mobile/FluxoLocalE2ETest.kt) e [teste do cache acadêmico](../mobile-kotlin/app/src/androidTest/java/br/com/unisync/mobile/AcademicoRoomTest.kt). A validação final deve registrar data, ambiente, perfil, resultado e captura ou relatório de cada fluxo.

## 8. Limites deste guia

Este documento não altera `Documentação.pdf`, `Documentação.tex`, o wireframe nem os READMEs. Não contém credenciais. `mobile-kotlin/docs/TURMAS-E-AVISOS.md` é local e não deve entrar no Git. Não gerar `UniSync.zip`.
