# Centralizador de Oportunidades Acadêmicas

O Centralizador de Oportunidades Acadêmicas é uma plataforma desenvolvida para evitar que estudantes percam oportunidades devido a informações espalhadas. O sistema centraliza avisos sobre palestras, eventos, estágios e projetos.

## Arquitetura do Sistema

O projeto adota uma arquitetura distribuída de microsserviços e Micro-Frontends (MFE):

### 1. Frontend Web

- **Shell / Host [Python com Django]:** Gerencia a casca do sistema, roteamento principal e identidade visual.
- **Micro-Frontend (Feed) [Vue.js]:** Interface reativa de alta performance para navegação no feed de eventos.

### 2. Backend (Microsserviços)

- **Núcleo Acadêmico [C# com .NET]:** Gerencia usuários, permissões, autenticação e regras estruturadas.
- **Motor de Oportunidades [Python com FastAPI]:** API assíncrona focada em buscas rápidas e filtragem de oportunidades.

### 3. Frontend Mobile

- **Aplicativo do Aluno [Kotlin com Jetpack Compose]:** App nativo Android para acompanhamento de prazos e alertas em tempo real.

### 4. Bancos de Dados

- **SQL Server / PostgreSQL:** Integrado ao backend em C# para dados relacionais de usuários e turmas.
- **MongoDB:** Integrado ao backend em Python para os dados flexíveis de eventos e vagas.

## Estrutura do Repositório

- `.github/workflows/` # Pipelines de integração contínua e controle de versão
- `backend-python/` # API 2 (FastAPI / Python - Motor de Oportunidades)
- `frontend-shell/` # Casca / Host do sistema (Django / Python)
- `frontend-web-aluno/` # Micro-Frontend do Portal do Aluno (Vue.js)
- `mobile-kotlin/` # Aplicativo Nativo Android (Kotlin / Jetpack Compose)
- `docs/` # Documentação formal em LaTeX
- `README.md` # Visão geral e guia de início rápido do projeto

## Equipe e Papéis

- **Matheus:** Backend, Banco de dados.
- **Kamilly:** Frontend.
- **João:** Backend, Banco de dados.
- **Samuel:** Backend, Documentação LaTeX.
- **Reinoldes:** Frontend.

Acesse o nosso [**site**](https://teste2234234.my.canva.site/in-cio) para saber mais.
