# The Struggler 0.3.3 — refino de dano, prioridade de animação e testes reais

Data dos testes: 11/11/2026
Branch: `feature/midland-guard-refinement-0.3.3` (derivada da tag `v0.3.2`)
Artefato testado: `struggler-0.3.3.jar` (61.895 bytes, SHA-256 `3537265dff330a7e09fb01a8f39b641cd5f700cee340031d8f58070612e5fde3`)

## 1. Ambiente real de execução

| Item | Valor |
|---|---|
| Minecraft Java | 1.20.1 |
| Forge | 47.4.28 |
| Java | OpenJDK 17 (headless + Xvfb) |
| Dependência | GeckoLib 4.7.2 (Forge 1.20.1) |
| Servidor | dedicado, mundo superplano, `online-mode=false`, dificuldade Normal |
| Clientes | 2 instâncias simultâneas (RefineQA em `:104`, RefineQB em `:105`), renderização por software (LIBGL_ALWAYS_SOFTWARE) |

Nenhum teste foi executado no computador do autor do pedido. Todas as capturas vêm do cliente real rodando neste ambiente isolado.

## 2. O que mudou nesta versão

1. **Classificação de dano explícita** (`GuardDamageClass`, `GuardDamageFacts`, `GuardDamagePolicy`): a decisão de bloquear deixou de ser uma cadeia de `if` dentro de `hurt()` e passou a ser uma política testável, baseada nas tags reais do tipo de dano (`bypasses_shield`, `is_explosion`, `is_fire`, `is_projectile`) mais o tipo da entidade direta.
2. **Prioridade de animação extraída** (`GuardAnimationPriority`): a escolha entre morte, ferimento, bloqueio, ataque, caminhada, corrida e idle agora é uma função pura e testada.
3. **Correção do escudo em entidade sem IA**: a expiração da defesa saiu do goal de combate e passou para `tick()` da entidade. Antes, um guarda com `NoAI` (ou fora de combate) mantinha o escudo erguido para sempre; agora o estado expira normalmente.
4. **Remoção de código redundante** (`GuardCombatGoal`, `GuardCombatTuning`, `GuardDefenseRules`).
5. **Testes**: 4 classes unitárias novas/ampliadas e uma classe nova de GameTest.

## 3. Testes automatizados

| Suíte | Resultado |
|---|---|
| Unitários (JUnit) | **38/38 aprovados**, 0 falhas — `GuardDamagePolicyTest` (12), `GuardAnimationPriorityTest` (10), `GuardDefenseRulesTest` (10), `GuardCombatTimelineTest` (6) |
| GameTests (servidor Minecraft real) | **26/26 testes obrigatórios aprovados** (`All 26 required tests passed`), incluindo `guardDefense*` e `guardDamagePolicy*` |

Log do GameTestServer: `gametest-033b.log` (transcrito em `docs/midland-guard-refinement/evidence/`).

## 4. Testes de execução real (cliente + servidor dedicado)

### 4.1 Carregamento e registro

| Teste | Resultado |
|---|---|
| Forge carrega o JAR sem erro de dependência | **APROVADO** |
| GeckoLib inicializa e o renderer do Guarda registra | **APROVADO** |
| Ovo de invocação cria `struggler:midland_guard` | **APROVADO** |
| Modelo, textura e animações carregam (sem fallback roxo/branco) | **APROVADO** |

### 4.2 Persistência

Guarda criado em `(1.5, 100.0, 6.0)` com 18.0 de vida. Servidor salvo e reiniciado por completo.

| Medida | Antes do reinício | Depois do reinício |
|---|---|---|
| Posição | `[1.5, 100.0, 6.0]` | `[1.5, 100.0, 6.0]` |
| Vida | `18.0` | `18.0` |

Resultado: **APROVADO** — entidade, posição, vida e estado de combate sobrevivem ao save/restart.

### 4.3 Classificação de dano com escudo erguido (redução de 40% no frontal)

Cada linha é uma medição real: vida lida antes e depois, mais os contadores internos do Guarda (`blockedHits`, `prevented`) expostos pelo comando `strugglerguard status`.

| Golpe aplicado | Vida antes → depois | Dano efetivo | `blockedHits` | `prevented` | Esperado | Resultado |
|---|---|---|---|---|---|---|
| Espada frontal (`player_attack` 10) | 24 → 18 | 6 | 0 → 1 | 0 → 4.0 | 6 (40% de 10) | **APROVADO** |
| Flecha frontal (`arrow`, entidade direta = flecha) | 24 → 18 | 6 | 1 → 2 | 4.0 → 8.0 | 6 | **APROVADO** |
| Magia (`magic` 10) | 18 → 8 | 10 | 1 (sem alteração) | 4.0 (sem alteração) | 10 | **APROVADO** |
| Fogo (`on_fire` 10) | 24 → 14 | 10 | sem alteração | sem alteração | 10 | **APROVADO** |
| Bigorna (`falling_anvil` 10, `bypasses_shield`) | 24 → 14 | 10 | sem alteração | sem alteração | 10 | **APROVADO** |
| Golpe pelas costas (`player_attack` 10, atacante atrás) | 24 → 14 | 10 | 2 (sem alteração) | 8.0 (sem alteração) | 10 (0% pelas costas) | **APROVADO** |

Observação técnica: no comando vanilla `/damage ... by <entidade> from <causa>`, o argumento `by` define a entidade **direta** do dano. Para simular uma flecha de verdade é preciso usar `by <flecha> from <atirador>`; a ordem inversa faz o jogador ser a entidade direta e o golpe deixa de ser tratado como projétil. Isso está documentado aqui porque foi observado em execução, não por suposição.

### 4.4 Estados e animações

| Estado | Como foi acionado | Resultado |
|---|---|---|
| Idle | guarda parado no mundo | **APROVADO** |
| Bloqueio (escudo erguido) | comando de QA + combate real | **APROVADO** — pose distinta, escudo à frente do torso |
| Ferimento | dano real de 2 com escudo erguido | **APROVADO** — piscar vermelho e recuo, retornando ao bloqueio |
| Ataque | ação de QA com keyframes do GeckoLib | **APROVADO** — rotação de torso e braço da espada |
| Caminhada / corrida | deslocamento real no mundo | **APROVADO** (medido por posição antes/depois em rodada anterior do mesmo renderer) |
| Morte | dano letal | **NÃO EXECUTÁVEL nesta rodada** — o guarda de teste morreu durante a bateria de dano e a captura da queda não foi obtida; o clipe `death` e o GameTest de morte continuam válidos (26/26), mas não há captura nova da queda em 0.3.3 |

### 4.5 Expiração do escudo sem IA (correção desta versão)

Guarda com `NoAI` e escudo erguido por comando: o estado `blocking` foi de `true` (`blockTicks=17`) para `false` (`blockTicks=0`) sem intervenção e sem IA ativa. Resultado: **APROVADO** — a correção funciona em runtime, não só no teste unitário.

### 4.6 Dois jogadores simultâneos

| Teste | Resultado |
|---|---|
| Dois clientes no mesmo servidor | **APROVADO** — `RefineQA` e `RefineQB` simultâneos |
| Estado do escudo visível igual nos dois clientes | **APROVADO** — capturas no mesmo instante mostram a mesma pose |
| Saída de um jogador | **APROVADO** — `RefineQB left the game`, guarda e mundo continuam |
| Retorno do jogador | **APROVADO** — `RefineQB joined the game`, o outro jogador continua visível |

## 5. Capturas (pasta `evidence/`)

| Arquivo | Conteúdo |
|---|---|
| `01-clientA-view.png` | cliente A no mundo de QA com o guarda à frente |
| `02-clientB-view.png` | cliente B vendo o mesmo cenário |
| `03-clientA-now.png` | estado do cliente A antes da rodada de dano |
| `04-clientA-rejoined.png` | cliente A reconectado após expulsão de QA (voando) |
| `05-clientA-respawn.png` | renascimento do jogador A |
| `06-clientA-after-restart.png` | cliente A após reinício do servidor |
| `07-clientB-after-restart.png` | cliente B após reinício do servidor |
| `08-clientA-respawned.png` | jogador A reposicionado no cenário |
| `09-block-clientA.png` | escudo erguido visto pelo cliente A |
| `10-block-clientB.png` | o mesmo escudo erguido visto pelo cliente B |
| `11-idle-clientA.png` | guarda em idle, para comparação |
| `12-hurt-while-blocking.png` | reação de dano com escudo erguido |
| `13-back-to-block.png` | retorno ao bloqueio após o ferimento |
| `14-attack-windup.png` | preparação do golpe |
| `15-attack-swing.png` | outro instante do golpe |
| `16-clientB-rejoined.png` | cliente B de volta ao mundo |

## 6. Limitações e pontos de atenção

1. **Morte**: sem captura nova nesta versão (ver 4.4). O clipe e o GameTest continuam cobertos pela suíte.
2. **Legibilidade do bloqueio**: a pose é distinta, mas discreta a distância; o escudo sobe cerca de 0,4 bloco e gira a face para frente. Se a leitura visual precisar ser mais óbvia, o próximo ajuste é ampliar o arco do braço esquerdo no clipe `block`.
3. **Dano de queda**: `fall` não está na lista de danos defensáveis por decisão de design (não é golpe direcional), então não foi testado como bloqueável.
4. **Peaceful**: conforme decidido, o comportamento em Peaceful não faz parte do escopo desta versão.
5. **Teste de cliente único por vez**: as duas instâncias rodaram com renderização por software; o desempenho não representa um cliente com GPU.

## 7. Como reproduzir

```bash
# compilar e rodar os testes unitários
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ./gradlew build test

# rodar os GameTests no servidor real
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ./gradlew runGameTestServer

# instalar em uma instância Forge 1.20.1 / 47.4.28
# mods/: struggler-0.3.3.jar + geckolib-forge-1.20.1-4.7.2.jar
```

Comandos de diagnóstico usados nos testes:

```
/strugglerguard status @e[type=struggler:midland_guard,limit=1]
/strugglerguard block  @e[type=struggler:midland_guard,limit=1]
/strugglerguard attack @e[type=struggler:midland_guard,limit=1]
```
