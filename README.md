# BERSERK — The Struggler

Minecraft Java **1.20.1**, Forge **47.4.28**, Java **17**, Gradle **8.8**.

A versão atual é a **0.3.2** (branch `feature/midland-guard-defense-0.3.2`), que soma ao Guarda de Midland o combate autoritativo no servidor (0.3.1) e a **defesa com escudo** (0.3.2). O visual é o da 0.2.0: proporções vanilla, capacete fechado, armadura volumétrica e animações nativas de **GeckoLib 4.7.2**. A fundação `feature/phase-1-foundation` permanece preservada.

## Instalação

Instale Forge 47.4.28 para Minecraft 1.20.1. Coloque `struggler-0.3.2.jar` e `geckolib-forge-1.20.1-4.7.2.jar` na pasta `mods`. Remova outras versões do Struggler dessa mesma instância. GeckoLib é dependência externa obrigatória, não está embutida no JAR do Struggler.

Distribuição oficial compatível: [GeckoLib 4.7.2 para Forge 1.20.1](https://modrinth.com/mod/geckolib/version/AiVKLYep).

No criativo, abra a aba The Struggler, use o ovo do Guarda de Midland e interaja com mão vazia por clique direito. Foram preservados o registro, ovo, aba, Behelit, 24 HP, velocidade base 0,23, IA de caminhada/observação e saudação em português/inglês.

## Combate

O Guarda persegue, golpeia e recarrega por conta própria no servidor, com um único impacto por golpe e linha do tempo determinística (windup → impacto → recovery → cooldown de 30 ticks). Ele nunca ataca sem ser provocado; se for atacado, revida. A vida, o dano e o cooldown são decididos apenas no servidor e sincronizados para a animação.

## Defesa com escudo

Ao ficar ao alcance de corpo a corpo sem poder golpear (cooldown), o Guarda ergue o escudo. A redução é de **40% em acertos frontais** dentro de um cone de 120°, **0% pelas costas**, com **piso de 1 de dano** por acerto — ele nunca fica invulnerável. A redução é aplicada uma única vez, no `hurt` do servidor, e vale 20 ticks renováveis. Ataque e defesa são mutuamente exclusivos: erguer o escudo interrompe o golpe e iniciar um golpe baixa o escudo.

Enquanto persegue, o escudo fica baixo: o Guarda só bloqueia quando está parado ao alcance do alvo.

## Modelo e animações

O arquivo editável está em `art/midland_guard/midland_guard.bbmodel`, com 52 cubos, nove bones, textura 128×128 e oito animações. Os arquivos carregados pelo GeckoLib ficam em `assets/struggler/geo/`, `assets/struggler/animations/` e `assets/struggler/textures/entity/`.

Idle, caminhada, corrida, ataque, bloqueio, defesa, dano e morte têm clips nativos. Ataque, dano, morte e bloqueio respondem aos estados reais da entidade. O clip `defend` continua sendo um gesto de QA. Corrida é acionada pela demonstração, não por uma rotina autônoma de patrulha.

Com permissão de operador nível 2, use `/strugglerguard idle|walk|run|attack|defend|block|reset|status` e, opcionalmente, um seletor de Guarda. Sem seletor, o comando usa o Guarda mais próximo. `reset` devolve o controle à IA normal. `status` mostra fase, alvo, cooldown, golpes, dano causado e o estado de defesa (`blocking`, `blockTicks`, `blockedHits`, `prevented`). `block` é uma ferramenta de QA: ergue o mesmo estado de escudo usado pelo combate.

## Compilação

Execute `./gradlew build` com JDK 17, ou `gradlew.bat build` no Windows. O Gradle baixa GeckoLib pelo Maven oficial. O artefato é gerado em `build/libs/struggler-0.3.2.jar`.

`tools/generate_midland_gecko.py` é a fonte de geração da geometria, atlas e keyframes; executá-lo sobrescreve os exports. Alterações feitas diretamente no Blockbench devem ser exportadas para os recursos GeckoLib correspondentes e preservadas antes de regenerar.

## Testes

- Unitários: `./gradlew test` (linha do tempo do golpe e regra de defesa).
- GameTests no servidor real: `./gradlew runGameTestServer` (14 testes de combate e defesa).
- Execução real: servidor dedicado Forge 47.4.28 + cliente real, com dano medido no jogo.

## Teste real

A versão 0.3.2 foi executada em servidor dedicado e cliente Forge reais, com Java 17. Foram medidos dano frontal reduzido (10 → 6), dano pelas costas integral (10), expiração da janela de escudo, retaliação preservada e a pose de escudo no cliente. Consulte `docs/midland-guard-defense/relatorio-0.3.2.md` para resultados, limitações e evidências. Compilar não substitui esse teste de execução.
