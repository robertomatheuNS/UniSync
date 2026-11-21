# Especificação de cadastro e identificação de usuários

Documento de encaminhamento para a equipe responsável pelo contrato de
identidade. A implementação do backend não faz parte deste documento nem foi
alterada no Shell ou no Kotlin.

## 1. Objetivo

Padronizar o cadastro de alunos e professores, gerar um identificador numérico
de oito dígitos e permitir que o usuário utilize essa identificação para
acessar a plataforma.

O identificador deve ser gerado exclusivamente pelo serviço de identidade. O
Shell e o aplicativo Android apenas exibem e enviam os dados recebidos.

## 2. Formulários

### 2.1 Campos comuns

Obrigatórios para aluno e professor:

- nome completo;
- e-mail;
- repetir e-mail;
- senha;
- repetir senha.

Regras sugeridas, compatíveis com o contrato atual:

- nome entre 2 e 100 caracteres;
- e-mail válido, com até 180 caracteres;
- senha entre 8 e 128 caracteres;
- e-mail e confirmação devem coincidir após normalização;
- senha e confirmação devem coincidir;
- confirmações são usadas apenas no cliente e nunca são enviadas à API.

### 2.2 Aluno

Campos obrigatórios adicionais:

- curso;
- turno.

O curso pode começar como texto controlado pelo backend. O turno deve ter
valores definidos pelo contrato, por exemplo `MANHA`, `TARDE`, `NOITE` e
`INTEGRAL`.

### 2.3 Professor

Campo obrigatório adicional já existente:

- código de validação do professor (`codigoProfessor`).

Campos opcionais recomendados, somente se forem utilizados no perfil ou nas
regras acadêmicas:

- área de atuação;
- departamento;
- instituição ou campus.

Curso e turno não devem ser obrigatórios no perfil do professor. Um professor
pode atuar em várias turmas, disciplinas e horários. Essas informações devem
pertencer à entidade `Turma`.

## 3. Identificador numérico

### 3.1 Regras

- nome sugerido do campo: `numeroUsuario`;
- oito dígitos numéricos;
- geração no backend após validação do cadastro;
- valor único em todo o sistema;
- restrição de unicidade no PostgreSQL;
- não reutilizar após exclusão;
- não aceitar valor enviado pelo cliente;
- preferencialmente sem zeros à esquerda, usando a faixa `10000000` a
  `99999999`;
- mostrar o número após o cadastro e no perfil do usuário;
- não tratar o número como senha ou segredo.

## 4. Contrato de cadastro proposto

### 4.1 Aluno

`POST /api/usuarios`

```json
{
  "nome": "Nome completo",
  "email": "aluno@example.com",
  "senha": "senha-do-usuario",
  "curso": "Nome do curso",
  "turno": "NOITE"
}
```

### 4.2 Professor

`POST /api/usuarios/professores`

```json
{
  "nome": "Nome completo",
  "email": "professor@example.com",
  "senha": "senha-do-usuario",
  "codigoProfessor": "codigo-fornecido-pela-equipe",
  "areaAtuacao": "Área opcional",
  "departamento": "Departamento opcional",
  "instituicaoCampus": "Campus opcional"
}
```

### 4.3 Resposta de cadastro

Os dois endpoints devem retornar, no mínimo:

```json
{
  "id": "uuid",
  "nome": "Nome completo",
  "email": "usuario@example.com",
  "numeroUsuario": "12345678",
  "tipo": "Aluno",
  "dataCriacaoUtc": "2026-11-21T15:00:00Z"
}
```

A senha, confirmações e código de validação nunca devem aparecer na resposta.

## 5. Login proposto

`POST /api/usuarios/login`

```json
{
  "identificador": "12345678 ou usuario@example.com",
  "senha": "senha-do-usuario"
}
```

O backend deve aceitar, conforme decisão da equipe:

- e-mail;
- número de usuário;
- nome completo normalizado.

### Atenção sobre login por nome

Nomes podem ser repetidos. Se o login por nome for mantido, o backend precisa
definir uma regra para duplicidade. A recomendação técnica é priorizar e-mail
ou número de usuário e utilizar o nome apenas para exibição.

Durante a migração, pode ser mantido suporte temporário ao campo `email` para
não quebrar clientes antigos.

## 6. Erros esperados

- `400` ou `422`: dados inválidos;
- `401`: identificador ou senha inválidos;
- `409`: e-mail ou número de usuário já existente;
- `409`: código de professor inválido, expirado ou já utilizado.

As respostas não devem revelar se um e-mail específico está cadastrado durante
tentativas de login.

## 7. Responsabilidades

### Backend .NET/PostgreSQL

- alterar entidades e migrações;
- gerar o número de usuário;
- garantir unicidade;
- persistir curso, turno e campos do professor;
- atualizar cadastro e login;
- manter compatibilidade temporária, se necessário;
- atualizar testes de API.

### Shell Django

- apresentar os campos aprovados;
- validar confirmações localmente;
- enviar somente o contrato oficial;
- mostrar o número de usuário retornado;
- não gerar identificadores;
- não armazenar senha ou confirmação.

### Kotlin/Jetpack Compose

- reproduzir as mesmas regras de validação;
- apresentar campos por perfil;
- exibir o número retornado após cadastro;
- aceitar o identificador no login;
- manter mensagens de erro e estados de carregamento.

## 8. Critérios de aceite

- aluno consegue cadastrar-se com curso e turno;
- professor consegue cadastrar-se com código válido;
- e-mail e senha divergentes impedem o envio;
- número de oito dígitos é criado pelo backend;
- número não se repete;
- número aparece no Shell e no Android após o cadastro;
- login funciona por e-mail e número;
- login por nome segue a regra definida para duplicidade;
- senha nunca aparece em logs, respostas ou telas após o envio;
- aluno e professor recebem apenas os campos permitidos para seus perfis;
- Web e Mobile usam o mesmo usuário e o mesmo número.

## 9. Decisões que a equipe precisa confirmar

1. O campo oficial será `numeroUsuario` ou outro nome?
2. O número terá ou não zeros à esquerda?
3. Curso será texto livre ou catálogo?
4. Quais valores oficiais existirão para turno?
5. Quais campos opcionais do professor serão realmente persistidos?
6. Login por nome será obrigatório ou apenas e-mail/número?
7. O número será imutável durante toda a vida da conta?

## 10. Situação atual

O contrato atualmente publicado aceita somente:

- aluno: `nome`, `email`, `senha`;
- professor: `nome`, `email`, `senha`, `codigoProfessor`.

O retorno atual não possui `numeroUsuario`, curso ou turno. Portanto, Shell e
Kotlin não devem ativar os novos campos em produção antes da atualização do
backend e da validação dos testes de integração.
