# Guarda de Midland — Defesa com escudo (0.3.2)

Data: 2026-11-11
Base: `v0.3.1-midland-guard-combat` (commit `1306d0f`)
Branch: `feature/midland-guard-defense-0.3.2`
Versão do JAR: `struggler-0.3.2.jar`

## 1. O que foi implementado

| Item | Onde |
| --- | --- |
| Regra pura da defesa (cone frontal + redução) | `world/entity/GuardDefenseRules.java` |
| Parâmetros aprovados (40% / 0% costas / piso de 1) | `world/entity/GuardCombatTuning.java` |
| Estado sincronizado, janela de bloqueio, redução única em `hurt` | `world/entity/MidlandGuard.java` |
| Ativação durante o cooldown do golpe | `world/entity/GuardCombatGoal.java` |
| Clip `animation.midland_guard.block` (loop, 1,2 s) | `assets/struggler/animations/midland_guard.animation.json` |
| Diagnóstico `blocking / blockTicks / blockedHits / prevented / blockAttempts / blockRefusals` | `command/MidlandGuardCommands.java` |
| Ação de QA `strugglerguard block` | `command/MidlandGuardCommands.java` |
| Testes unitários da regra | `src/test/java/.../GuardDefenseRulesTest.java` (10 testes) |
| GameTests no servidor real | `gametest/GuardDefenseGameTests.java` (8 testes) |

Números fixados, exatamente como aprovados: **40% de redução em acertos frontais, 0% pelas costas**, cone frontal de **120°** (±60°), **piso de 1 de dano** por acerto bloqueado, janela de **20 ticks** renovável.

## 2. Falha real encontrada e corrigida durante os testes

A primeira versão sobrescrevia `LivingEntity.isBlocking()`. Esse método já existe no vanilla e significa "está usando um escudo de item". Ao devolvê-lo como `true`, o Forge passou a tratar o Guarda como um escudo vanilla ativo e **anulava 100% do dano** (o Guarda ficava invulnerável, exatamente o que o requisito 4 proíbe).

Os GameTests detectaram isso na primeira execução: `applied=6.0 damaged=false health=24.0`. O estado foi renomeado para `isShieldRaised()`, que não colide com o vanilla. Depois da correção: **14/14 GameTests aprovados**.

Esse detalhe é registrado porque é a diferença entre "o número aparece no código" e "o dano realmente entra no jogo".

## 3. Testes executados e resultados

### 3.1 Build e testes unitários — APROVADO

- `./gradlew build` com Java 17 e Gradle 8.8: **BUILD SUCCESSFUL** (só avisos de deprecação pré-existentes).
- `GuardDefenseRulesTest`: **10/10** (cone frontal, borda de 59°, fora em 61°, costas, contato, direção nula, 6→3,6, 10→6,0, piso de 1, nunca invulnerável, dano evitado).

### 3.2 GameTests no servidor Minecraft — APROVADO (14/14)

Saída literal: `All 14 required tests passed :)` — 6 testes de combate da 0.3.1 + 8 novos de defesa:

| Teste | Requisito | Resultado |
| --- | --- | --- |
| `guardTakesFullDamageWithoutBlocking` | 1 | 6,0 de dano integral sem escudo |
| `guardBlocksFrontalDamage` | 2, 5 | 10 → 6,0 exatos (3,6 indicaria redução dupla) |
| `guardDoesNotBlockFromBehind` | 3 | 10 de dano integral pelas costas |
| `guardIsNeverInvulnerableWhileBlocking` | 4 | 30 → 18 de dano, sobrevive |
| `blockingEndsAfterItsWindow` | 6 | escudo desce após 30 ticks |
| `guardBlocksBetweenStrikesAndAttacksAgain` | 7, 9 | bloqueia no cooldown e volta a atacar |
| `retaliationStillWorksWhileDefending` | 8, 10 | registra o jogador e continua lutando |
| `attackAndDefenseAreMutuallyExclusive` | 9 | ataque e defesa nunca coexistem |

### 3.3 Servidor dedicado + cliente real — APROVADO

Ambiente: Minecraft **1.20.1**, Forge **47.4.28**, Java **17**, GeckoLib **4.7.2**, `struggler-0.3.2.jar`, servidor dedicado + cliente real em display virtual, jogador `CombatQA`.

| Verificação | Comando | Resultado |
| --- | --- | --- |
| Redução frontal | `strugglerguard block` + `damage ... 10 player_attack by CombatQA` | Vida 24 → **18** (6,0 aplicados), `blockedHits=1`, `prevented=4.0` |
| Redução única | mesmo teste | 6,0 e não 3,6 |
| Costas sem redução | `block` + `damage ... 10 mob_attack by <husk atrás>` | Vida 24 → **14** (10,0 aplicados), `blockedHits` inalterado |
| Expiração | status após ~2 s sem renovar | `blocking=false blockTicks=0` |
| Retaliação preservada | combate real no mundo | `strikes 6 → 12`; morte com "recebeu uma sentença de morte de Guarda de Midland" |
| Estado real, não só comando | `blockAttempts=247 blockRefusals=0` | o goal de combate chama o mesmo caminho continuamente |
| Pose no cliente | capturas `10-shield-raised.png` / `11-shield-raised-later.png` | escudo erguido à frente do tronco |

Capturas e logs desta rodada: `docs/midland-guard-defense/evidence/`.

### 3.4 Golpes reais de jogador (clique com espada de ferro)

Em duelo direto, 4 golpes de espada tiraram 24 de vida do Guarda (6,0 cada) — ou seja, **nenhum foi bloqueado naquele instante**, porque o Guarda estava perseguindo (ver limitação 4.1). Isso é comportamento previsto, não falha: o bloqueio só existe quando ele está ao alcance e esperando o cooldown.

## 4. Limitações e decisões (declaradas, não escondidas)

1. **Janela curta de escudo.** O Guarda ergue o escudo quando está ao alcance de corpo a corpo (≤ 2,4 blocos) e ainda em cooldown, ou por 20 ticks após levar um acerto frontal. Perseguindo (> 2,4 blocos) o escudo fica baixo. Em duelo com knockback o jogador o empurra para fora do alcance, então o bloqueio aparece sobretudo quando ele está parado ao lado do alvo. Alternativa (bloquear enquanto caminha) foi descartada porque o clip de bloqueio substituiria a caminhada e o personagem "deslizaria".
2. **Clip novo em vez de reaproveitar `defend`.** O clip existente é um gesto de 2,0 s que volta ao neutro; não serve como pose sustentada. Foi criado `animation.midland_guard.block` (loop de 1,2 s) e o `defend` continua disponível para o preview de QA.
3. **Projéteis.** A direção é medida pela posição do **atirador**, não pela trajetória da flecha. Um tiro frontal de esqueleto conta como frontal; um tiro cruzado do mesmo esqueleto também.
4. **Peaceful não foi testado** (decisão do usuário: indiferente neste modo).
5. **`/damage` foi usado para medir** com precisão. O caminho de clique real também foi exercitado, mas a medição determinística usa o mesmo `hurt` do jogo.
6. O comando `strugglerguard block` é **ferramenta de QA** (permissão 2) e chama o mesmo `startBlocking()` do combate — não existe caminho paralelo que finja a regra.

## 5. Requisitos 1–10

| # | Requisito | Status |
| --- | --- | --- |
| 1 | Estado defensivo real, autoritativo no servidor, sincronizado | APROVADO |
| 2 | Redução de 40% frontal / 0% costas | APROVADO |
| 3 | Bloqueio apenas frontal, não 360° | APROVADO |
| 4 | Nunca invulnerável (piso de 1) | APROVADO |
| 5 | Sem redução dupla | APROVADO |
| 6 | Defesa com início, duração e fim | APROVADO |
| 7 | Guarda continua podendo atacar | APROVADO |
| 8 | Retaliação preservada | APROVADO |
| 9 | Animação coerente com o estado real | APROVADO |
| 10 | Nada do comportamento existente quebrado | APROVADO |

## 6. Como testar no seu Minecraft

1. Instale `struggler-0.3.2.jar` **e** `geckolib-forge-1.20.1-4.7.2.jar` em `mods/`.
2. Minecraft 1.20.1 + Forge 47.4.28 + Java 17.
3. No mundo: `/summon struggler:midland_guard` e ataque de frente com uma espada.
4. Diagnóstico: `/strugglerguard status` (mostra `blocking`, `blockedHits`, `prevented`).
5. Para forçar a pose: `/strugglerguard block`.
