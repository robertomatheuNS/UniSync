# UniSync Android

Cliente Android nativo em Kotlin e Compose, integrado às APIs .NET e Python.

## Painel inicial

O painel reúne turmas, avisos, oportunidades, tarefas do aluno e lembretes. Os cards de avisos e oportunidades abrem os detalhes nas telas correspondentes; a volta retorna ao painel. Abrir um aviso não confirma sua leitura: o aluno precisa usar **Marcar como lido**. Edição e exclusão seguem as permissões da conta nas telas existentes.

- **Avisos recentes:** até três avisos das turmas atualmente disponíveis, pela última atualização em UTC, com criação como alternativa. A tela indica lido/não lido para alunos.
- **Oportunidades recentes:** até três entre os itens carregados, pela ordem de criação dos ObjectIds da API. A data exibida continua sendo a do evento, incluindo a indicação de evento já realizado quando necessário. A consulta atual da API traz até 100 itens, ordenados por data do evento; o painel não representa todas as publicações do servidor.
- **Falha de rede:** preserva a última consulta de avisos da sessão e o cache Room de oportunidades. Cada seção oferece sua própria atualização; falhas não são exibidas como listas vazias confirmadas.
- **Navegação:** a solicitação de abertura por identificador é consumida uma vez. O detalhe aberto sobrevive à restauração de estado, e fechá-lo não o reabre automaticamente. Se o item sumir ou ainda não estiver carregado, a interface mostra consulta/indisponibilidade, com tentativa e fechamento.

Os cards reutilizam os estados dos modelos já ativos na sessão. O painel não cria novas consultas por card, endpoints de backend ou tabelas Room. Os avisos continuam sem cache persistente após encerrar o aplicativo.

## Inscrições em oportunidades

Quem publica uma oportunidade pode informar um link HTTPS e um prazo de inscrição. O prazo é opcional e deve terminar até o início do evento. O painel e a agenda exibem esse prazo no fuso do aparelho, e o detalhe destaca prazos próximos (até três dias) e encerrados. O botão para abrir o site fica disponível enquanto o evento e o prazo permitem a inscrição.

O participante se inscreve no site indicado. A ação de compartilhar apresenta o resumo e o link no [seletor do Android](https://developer.android.com/develop/ui/compose/sharing/send). A migração Room 5 → 6 mantém as oportunidades salvas e acrescenta os dois campos para consulta no cache.

## Validação local

Com as APIs locais em execução e um emulador de teste já iniciado:

```bash
ANDROID_SERIAL=emulator-5556 ./scripts/testar-academico-local.sh
```

O script compila o aplicativo, executa testes unitários/lint e instala os APKs com `-r` no dispositivo selecionado. Cria códigos de professor de uso único para os cenários acadêmico, mural e painel. Os testes usam contas técnicas. O cenário do painel valida navegação real de aluno/professor, leitura explícita, permissões e isolamento de avisos entre turmas. As capturas e os resultados ficam em `app/build/reports/academico/`.

Para somente build, testes unitários e lint:

```bash
./gradlew :app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest :app:lintDebug
```

O debug padrão usa `10.0.2.2` para alcançar as APIs pelo emulador. O script integrado usa `127.0.0.1` com `adb reverse` para as portas 5041 e 8000. Builds de release exigem endereços HTTPS definidos nas propriedades `unisync.authUrl` e `unisync.apiUrl`.

Para gerar um APK de release assinado, configure `UNISYNC_KEYSTORE_FILE` (caminho absoluto), `UNISYNC_KEYSTORE_PASSWORD`, `UNISYNC_KEY_ALIAS` e `UNISYNC_KEY_PASSWORD` apenas no ambiente local ou no cofre de segredos da CI. Depois execute:

```bash
./gradlew :app:assembleRelease -Punisync.authUrl=https://auth.exemplo.com -Punisync.apiUrl=https://api.exemplo.com
```

O APK fica em `app/build/outputs/apk/release/`. O build falha se os endereços HTTPS ou a assinatura estiverem ausentes. Guarde a chave de assinatura em local seguro: atualizações do mesmo aplicativo exigem a mesma identidade de assinatura. Turmas e avisos consultados ficam salvos no aparelho para leitura offline, são apagados ao sair da conta e mostram quando os dados vieram do cache. Com a sessão ativa, o Android agenda sincronização periódica com rede; o sistema pode adiar sua execução para poupar bateria.

`docs/TURMAS-E-AVISOS.md` é um documento local ignorado pelo Git. Não gerar `UniSync.zip`.
