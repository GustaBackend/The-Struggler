# Etapa técnica A — migração experimental para GeckoLib 4.8.4

**Veredito: APROVADA COM RESSALVAS.** O build, os 38 testes unitários e os 26 GameTests passaram; a execução real com servidor dedicado e dois clientes manteve animações, dano e defesa. Não foi identificada regressão do The Struggler. As ressalvas são do alcance da validação: renderização por software, áudio não disponível e ausência de ensaio prolongado ou de rede externa. A experiência pode servir de base à próxima versão, sem substituir automaticamente a versão estável.

Testes realizados em 12/11/2026, no sandbox Manus, sem utilizar o computador do usuário. Ambiente exato: Minecraft Java 1.20.1, Forge 47.4.28, OpenJDK 17.0.21.0.3, Gradle 8.8 e GeckoLib Forge 1.20.1 4.8.4.

## Preservação e commits

Repositório: https://github.com/GustaBackend/The-Struggler

Branch experimental: `experiment/geckolib-4.8.4-from-v0.3.3`, criada diretamente da tag `v0.3.3`. Commit da base: `8256acd18ac762ed0c7bee6317811658f9ed2cf3`. Objeto da tag anotada: `200feac18477e8143cdc9c702c4616286515d481`.

Commit da migração e inspeção: `3b2421e`. O relatório, os resultados e a documentação recebem commit próprio na mesma branch. Nenhuma mudança foi feita em `main`, na tag `v0.3.3` ou em releases existentes. O experimento usa nome próprio de JAR e pré-release própria. Rollback: checkout da tag original e reinstalação do JAR estável com GeckoLib 4.7.2.

Não houve integração com Better Combat, Epic Fight ou o modpack. Não foram adicionadas funcionalidades, removidos testes ou alterados combate, IA, fórmulas de dano, defesa, retaliação ou sincronização.

## Inspeção antes da alteração

Todas as referências a 4.7.2 foram localizadas por `git grep`; o registro está em `references-before.txt`. Os pontos de configuração ativos eram `build.gradle` e `mods.toml`. README, especificação artística e relatórios antigos também mencionavam 4.7.2. Relatórios históricos foram preservados sem trocar suas versões retrospectivamente.

Foram inspecionados os imports e chamadas em `MidlandGuard`, `MidlandGuardModel` e `MidlandGuardRenderer`: implementação de GeoEntity, cache por entidade, registro do controlador único, oito RawAnimations, seleção de estado, método de animação customizada da cabeça e registro Forge do renderer. A comparação `javap -protected -s` dos JARs oficiais não encontrou assinatura utilizada removida ou alterada. Os construtores e métodos usados permanecem disponíveis. O resultado detalhado, os metadados de download e o diff estão nesta pasta.

O changelog 4.8.x inclui APIs novas, correções para outros caminhos de animação e uma incompatibilidade com `geckoanimfix`. Nenhum desses caminhos exige mudança no Guarda atual. O mod não usa `math.pi`, singleton animatables, `getPackedOverlay` ou `geckoanimfix`. [1] [2]

## Arquivos modificados e comparação

Configuração e documentação existente modificadas:

- `build.gradle`: coordenada GeckoLib 4.7.2 → 4.8.4; Maven oficial preservado.
- `gradle.properties`: versão 0.3.3 → `0.3.3-gecko484-exp1`, exclusivamente para identificar o artefato experimental.
- `src/main/resources/META-INF/mods.toml`: faixa `[4.7.2,5)` → `[4.8.4]`, para exigir o runtime exato testado. A faixa antiga já aceitava 4.8.4; a mudança é isolamento, não correção de API.
- `README.md`: aviso do experimento, instalação e distinção da base estável.

Arquivos adicionados: esta pasta `docs/geckolib-4.8.4/`, com inspeção, metadados, diff de API, relatório, resultados, hashes, roteiro de QA e evidências. A lista exata da entrega pode ser obtida por `git diff --name-status v0.3.3 HEAD`.

**Nenhum arquivo Java, teste, estrutura de GameTest, geometria, textura ou JSON de animação foi modificado.** A comparação dos conteúdos descompactados do JAR reconstruído da base e do experimental encontrou diferença somente em `META-INF/mods.toml`. Todas as 23 classes e os recursos existentes são idênticos byte a byte.

GeckoLib 4.7.2 e 4.8.4 utilizam, neste projeto, o mesmo controlador, cache e oito clips: idle, walk, run, attack, defend, block, hurt e death. Não foi necessária adaptação de API. Isso foi confirmado por compilação e testes reais, não apenas inferido a partir das assinaturas.

## Compilação e testes automatizados

A base estável foi reconstruída e testada novamente em um worktree destacado e limpo, antes da migração. Não foi usada somente a contagem de um relatório anterior.

- Base 4.7.2: `clean build test` APROVADO; 38 testes, zero falhas, erros ou ignorados. GameTestServer APROVADO; todos os 26 testes obrigatórios passaram.
- Experimento 4.8.4: `clean build test` APROVADO; 38 testes, zero falhas, erros ou ignorados. GameTestServer APROVADO; todos os 26 testes obrigatórios passaram.

As quatro suítes unitárias são GuardAnimationPriorityTest (10), GuardCombatTimelineTest (6), GuardDamagePolicyTest (12) e GuardDefenseRulesTest (10). Os nomes dos 38 casos coincidem entre base e experimento; nenhum foi removido ou ignorado. O resultado está em `evidence/test-comparison.json`. Os quatro logs completos de build e GameTest estão em `evidence/`.

`dependencyInsight` confirma GeckoLib `4.8.4_mapped_official_1.20.1` no runtimeClasspath. Não houve erro de API nem dependência de mod ausente. A distribuição oficial baixada foi validada pelos SHA-1 e SHA-512 fornecidos pelo Modrinth. Não houve falha de download nesta etapa. [1]

## Execução real e multiplayer

Servidor dedicado em `127.0.0.1:25566`, mundo isolado com plataforma de pedra, dificuldade Normal. Dois clientes independentes, MigrationA e MigrationB, conectaram simultaneamente, em displays Xvfb distintos `:106` e `:107`, com diretórios de jogo separados e renderização Mesa por software. Em cada pasta `mods` havia exclusivamente o JAR experimental e GeckoLib 4.8.4. Os hashes das cópias instaladas eram iguais.

Resultados funcionais:

- **Carregamento, registro, modelo, textura e oito clips: APROVADO.** Guarda criado no mundo; sem textura de recurso ausente, erro de renderer ou crash do mod.
- **Idle: APROVADO.** Guarda em repouso renderizado nos dois clientes.
- **Caminhada: APROVADO.** Preview existente aciona navegação real; posição Z medida de 5.5 para -0.9114431369, com passada visível.
- **Corrida: APROVADO.** Preview existente aciona navegação real; posição Z de 5.5 para -1.2757220346, com clip de corrida visível. Isso não acrescenta corrida autônoma à IA: a funcionalidade continua sendo a demonstração já existente.
- **Ataque: APROVADO.** Clip observado em preview e combate real. Na rodada controlada sem fogo solar, o servidor registrou 3 golpes, 3 tentativas de impacto e 3 acertos; alvo Zombie continuou vivo com 88.19201 HP. A armadura vanilla do alvo também participa do cálculo; não se confunde o saldo de vida com o atributo de dano bruto do Guarda.
- **Defesa com escudo: APROVADO.** Escudo elevado por estado real, tanto em comando QA existente quanto durante cooldown do combate. O servidor registrou `blocking=true` e a pose foi observada nos dois clientes.
- **Redução frontal: APROVADO.** Com escudo elevado e golpe `player_attack` de 10, a vida passou de 24 para 18: dano 6, redução 40%. Contadores registraram o bloqueio.
- **Golpe pelas costas: APROVADO.** Mesma potência de 10, vida de 24 para 14: dano integral, redução 0%.
- **Recebimento de dano e hurt: APROVADO.** Dano mágico real de 2 causou a reação de ferimento, incluindo movimento do corpo e flash vermelho, com retorno ao bloqueio enquanto a janela permanecia ativa.
- **Morte: APROVADO.** Dano mágico real de 100 acionou a queda animada, visível nos dois clientes, seguida pela remoção da entidade. Nesta rodada há evidência visual nova da queda; não se reutilizou a ressalva da 0.3.3 anterior.
- **Sincronização de combate e defesa: APROVADO.** Dois clientes viram as transições acionadas pelo mesmo servidor. Para comparar o bloqueio, foram usadas também câmeras idênticas em modo espectador e capturas após propagação do pacote. Não se afirma igualdade de frame ou latência zero.
- **Salvamento e encerramento do servidor: APROVADO.** `save-all flush` e `stop` executados; todas as dimensões foram salvas sem crash.

A primeira gravação de combate foi afetada por zumbi queimando ao sol. Foi mantida como registro e o teste foi repetido à noite, com alvo controlado e vida elevada. Não foi feita mudança no mod para contornar o teste.

## Evidências

Os logs RCON contêm os comandos e respostas do servidor, com timestamp. As medições estão em `runtime-measurements.json` e a comparação do JAR em `jar-audit.json`. Capturas e vídeos são do Minecraft real; não são conceitos, renders externos ou imagens geradas.

Principais arquivos:

- `01-idle-a.png` e `01-idle-b.png`: carregamento nos dois clientes.
- `02-poses-a.mp4` e `02-poses-b.mp4`: idle, bloqueio, hurt, ataque e gesto defend.
- `04-walk-*.mp4` e `04-run-*.mp4`: passada e deslocamento.
- `05-death-*.mp4` e `10-death-*.png`: dano letal e queda.
- `12-clean-combat-*.mp4`: combate real repetido sem interferência solar.
- `14-confirmed-sync-block-*.png`: mesma pose defensiva após propagação do estado.
- `baseline-build.log`, `baseline-gametest.log`, `experimental-build.log`, `experimental-gametest.log`: execução das duas suítes.
- `server-console.log`, `client-a-latest.log`, `client-b-latest.log` e `rcon-transcript.log`: registro completo do runtime.

As capturas principais e os resultados textuais são versionados na branch. As gravações completas e demais evidências ficam no ZIP da pré-release, evitando adicionar vídeos grandes ao histórico Git.

## Logs, limitações e NÃO TESTADO

Não foram encontrados erros de carregamento de recursos, exceções de API ou crashes atribuíveis ao The Struggler/GeckoLib. **Isso não significa logs totalmente limpos.** Foram preservados e classificados os seguintes avisos do ambiente:

O servidor corrigiu configs Forge recém-criadas e registrou `No key layers in MapLike[{}]` ao receber gerador superplano sem preset explícito, continuando a gerar o mundo e carregando normalmente. Há avisos conhecidos de bibliotecas internas Forge sem mods.toml e de URLs union de recursos. Não indicam ausência do GeckoLib.

Os clientes registraram opção de simulationDistance 4 abaixo do mínimo 5 e falha de obtenção de chave de perfil por sessão offline. Também ocorreu falha OpenAL ao abrir dispositivo de áudio: o jogo desativou sons e música. O servidor ficou restrito ao loopback; sessões offline foram usadas apenas no QA local.

**NÃO TESTADO:** áudio; autenticação Microsoft/servidor online-mode=true; latência pela internet/Radmin VPN; benchmark em GPU real; estabilidade prolongada ou carga com muitos jogadores; combinação com o modpack completo, Better Combat ou Epic Fight. Não foi feito novo ensaio manual de todos os itens/aba criativa, nem uma campanha de mundo existente de longa duração. Os testes obrigatórios de animação, defesa e multiplayer com dois clientes foram executados. Os GameTests existentes cobrem também regras de dano e retaliação, sem alteração na suíte.

## Artefato e reprodução

JAR: `struggler-0.3.3-gecko484-exp1.jar`, 61.907 bytes.

SHA-256:

```text
b83dbc9fa04fbcd8ce7040d09c613f4ee75552e152c5398be19044069c5defe7
```

GeckoLib externo: `geckolib-forge-1.20.1-4.8.4.jar`, 1.038.209 bytes. SHA-256 `4518779a31b6c2abb92c98b5ad17ef3d60177df68626bfc86c44de2764913c2b`. A dependência não está embutida no Struggler. Instale os dois JARs em instância separada; não misture com 4.7.2. [1]

O hash da reconstrução local da base é diferente do JAR publicado anteriormente porque os arquivos ZIP não são uma promessa de build reproduzível; a comparação relevante aqui foi dos conteúdos descompactados. As classes e recursos são idênticos no JAR reconstruído e experimental, exceto os metadados declarados.

```bash
# checkout da experiência, sem mudar main ou a tag estável
 git switch experiment/geckolib-4.8.4-from-v0.3.3
 export JAVA_HOME=/caminho/para/jdk17
 ./gradlew clean build test
 ./gradlew runGameTestServer
 ./gradlew dependencyInsight --dependency geckolib --configuration runtimeClasspath
```

O roteiro em `qa/` envia comandos de validação e captura os displays locais. Ele usa apenas os comandos de diagnóstico já existentes e as APIs padrão do Minecraft; não altera o JAR nem o sistema de combate.

## Referências

[1]: https://modrinth.com/mod/geckolib/version/aC5KMoNg "GeckoLib 4.8.4 oficial para Forge 1.20.1"
[2]: https://www.curseforge.com/minecraft/mc-mods/geckolib/files/8285794 "Distribuição e changelog GeckoLib 4.8.4"
