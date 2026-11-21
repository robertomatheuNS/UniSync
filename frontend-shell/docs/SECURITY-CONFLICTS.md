# Conflitos de segurança e integração do Shell

Este registro mantém pendências que dependem do Vue ou do backend. Elas não
devem ser resolvidas alterando esses componentes durante a etapa de segurança
do Shell.

## Token entregue ao Micro-Frontend

O contrato atual injeta `accessToken` em `window.UNISYNC_SHELL`, porque o Vue
usa esse valor para chamar diretamente a API Python. Remover o token do HTML
sem alterar o Vue interrompe a integração atual.

**Responsável pela decisão:** Shell + frontend Vue + segurança da equipe.

**Opções futuras:** emitir um token de curta duração para o Micro-Frontend ou
criar um proxy/BFF no Shell. A API continua sendo a autoridade para validar o
JWT em qualquer opção.

## Logout por GET

O cabeçalho do Vue usa um link para `/logout/`. O Shell mantém esse contrato
temporariamente para não quebrar o fluxo existente, embora a operação altere a
sessão e deva migrar para POST com proteção CSRF.

**Responsável pela correção conjunta:** Shell e frontend Vue.

## Configuração de produção

O Shell agora exige `DJANGO_SECRET_KEY` forte quando `DJANGO_DEBUG=false`, além
de permitir configurar hosts, origens CSRF, idade da sessão e HTTPS por
variáveis de ambiente. O Compose atual não fornece essas variáveis ao Shell;
isso deve ser configurado pelo responsável pela implantação sem incluir
segredos no repositório.
