# BERSERK — The Struggler

Minecraft Java **1.20.1**, Forge **47.4.28**, Java **17**, Gradle **8.8**.

A versão atual é a **0.3.3** (branch `feature/midland-guard-refinement-0.3.3`), um refinamento da defesa do Guarda de Midland sobre a 0.3.2: classificação explícita dos tipos de dano defensáveis, prioridade de animação extraída e testável, e limpeza de condições redundantes. O visual continua o da 0.2.0: proporções vanilla, capacete fechado, armadura volumétrica e animações nativas de **GeckoLib 4.7.2**. A fundação `feature/phase-1-foundation` permanece preservada.

## Instalação

Instale Forge 47.4.28 para Minecraft 1.20.1. Coloque `struggler-0.3.3.jar` e `geckolib-forge-1.20.1-4.7.2.jar` na pasta `mods`. Remova outras versões do Struggler dessa mesma instância. GeckoLib é dependência externa obrigatória, não está embutida no JAR do Struggler.

Distribuição oficial compatível: [GeckoLib 4.7.2 para Forge 1.20.1](https://modrinth.com/mod/geckolib/version/AiVKLYep).

No criativo, abra a aba The Struggler, use o ovo do Guarda de Midland e interaja com mão vazia por clique direito. Foram preservados o registro, ovo, aba, Behelit, 24 HP, velocidade base 0,23, IA de caminhada/observação e saudação em português/inglês.

## Combate

O Guarda persegue, golpeia e recarrega por conta própria no servidor, com um único impacto por golpe e linha do tempo determinística (windup → impacto → recovery → cooldown de 30 ticks). Ele nunca ataca sem ser provocado; se for atacado, revida. A vida, o dano e o cooldown são decididos apenas no servidor e sincronizados para a animação.

## Defesa com escudo

Ao ficar ao alcance de corpo a corpo sem poder golpear (cooldown), o Guarda ergue o escudo. A redução é de **40% em acertos frontais** dentro de um cone de 120°, **0% pelas costas**, com **piso de 1 de dano** por acerto — ele nunca fica invulnerável. A redução é aplicada uma única vez, no `hurt` do servidor, e vale 20 ticks renováveis. Ataque e defesa são mutuamente exclusivos: erguer o escudo interrompe o golpe e iniciar um golpe baixa o escudo.

### O que o escudo bloqueia (0.3.3)

O escudo é física, não um número: a classificação é feita pelas tags de dano do Forge, nunca pelo nome de uma entidade ou por uma mensagem de morte.

| Fonte | Bloqueável |
| --- | --- |
| Ataque físico corpo a corpo (`player_attack`, `mob_attack`, `mob_attack_no_aggro`, `sting`) | sim |
| Projétil físico com entidade real (`arrow`, `trident`, `mob_projectile`, `wither_skull`, `thrown`) | sim |
| Magia, magia indireta, thorns, sonic boom, wither, sopro do dragão | não |
| Explosões (incluindo `fireworks`, `player_explosion`) | não |
| Fogo (`in_fire`, `on_fire`, `lava`, `hot_floor`, `fireball`) | não |
| Queda, congelamento, afogamento, raio | não |
| Qualquer fonte em `#bypasses_shield` (bigorna, estalactite, `#bypasses_armor`) | não |
| Dano ambiental sem atacante (cacto, prensa, genérico) | não |

A direção usa a origem lógica do ataque: para projéteis vale a posição do atirador, não a trajetória do último tick. Fireball e `unattributed_fireball` são tratados como fogo (as duas tags os classificam assim), então não são bloqueados; mudar isso é uma linha em `GuardDamagePolicy.classify`.

## Modelo e animações

O arquivo editável está em `art/midland_guard/midland_guard.bbmodel`, com 52 cubos, nove bones, textura 128×128 e oito animações. Os arquivos carregados pelo GeckoLib ficam em `assets/struggler/geo/`, `assets/struggler/animations/` e `assets/struggler/textures/entity/`.

Idle, caminhada, corrida, ataque, bloqueio, defesa, dano e morte têm clips nativos. A ordem que escolhe o clip vive em `GuardAnimationPriority` e é coberta por testes unitários: morte → golpe → dano → bloqueio → demonstração de QA → caminhada/repouso. A morte é absoluta, o golpe nunca é cancelado por um acerto e a reação de dano dura apenas os 10 ticks de `hurtTime`, devolvendo o escudo sozinha enquanto a defesa continuar ativa. O clip `defend` continua sendo um gesto de QA. Corrida é acionada pela demonstração, não por uma rotina autônoma de patrulha.

Com permissão de operador nível 2, use `/strugglerguard idle|walk|run|attack|defend|block|reset|status` e, opcionalmente, um seletor de Guarda. Sem seletor, o comando usa o Guarda mais próximo. `reset` devolve o controle à IA normal. `status` mostra fase, alvo, cooldown, golpes, dano causado e o estado de defesa (`blocking`, `blockTicks`, `blockedHits`, `prevented`, `blockAttempts`, `blockRefusals`). `block` é uma ferramenta de QA: ergue o mesmo estado de escudo usado pelo combate.

## Compilação

Execute `./gradlew build` com JDK 17, ou `gradlew.bat build` no Windows. O Gradle baixa GeckoLib pelo Maven oficial. O artefato é gerado em `build/libs/struggler-0.3.3.jar`.

`tools/generate_midland_gecko.py` é a fonte de geração da geometria, atlas e keyframes; executá-lo sobrescreve os exports. Alterações feitas diretamente no Blockbench devem ser exportadas para os recursos GeckoLib correspondentes e preservadas antes de regenerar.

## Testes

- Unitários: `./gradlew test` (linha do tempo do golpe, regra de defesa, política de dano e prioridade de animação).
- GameTests no servidor real: `./gradlew runGameTestServer` (26 testes de combate, defesa e classificação de dano).
- Execução real: servidor dedicado Forge 47.4.28 com dois clientes simultâneos, com dano medido no jogo.

## Teste real

A versão 0.3.3 foi executada em servidor dedicado e dois clientes Forge reais, com Java 17. Foram medidos dano frontal reduzido (10 → 6), dano pelas costas integral (10), magia/fogo/queda sem redução, projétil frontal reduzido, expiração do escudo (inclusive com IA desativada), retaliação preservada, sincronização da pose entre os dois clientes e persistência do Guarda após reinício do servidor. Consulte `docs/midland-guard-refinement/relatorio-0.3.3.md` para resultados, limitações e evidências. Compilar não substitui esse teste de execução.
