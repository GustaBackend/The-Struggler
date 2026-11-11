# Relatório de execução real — Guarda de Midland com combate (0.3.1)

Data: 11/11/2026
Branch: `feature/midland-guard-combat-0.3.1` (derivada da tag `v0.2.0-vanilla-geckolib`)
Artefato: `struggler-0.3.1.jar`

## 1. Ambiente utilizado

| Item | Valor |
| --- | --- |
| Minecraft | Java Edition 1.20.1 |
| Forge | 47.4.28 |
| Java | OpenJDK 17 (17.0.x, headless + cliente) |
| GeckoLib | 4.7.2 (Forge 1.20.1) |
| Servidor | Forge dedicado, instância isolada, offline-mode |
| Cliente | Forge real, Xvfb 1280x720, renderizador por software |
| Sistema | Ubuntu (sandbox Manus), sem uso do computador do usuário |

Nada foi executado na máquina do usuário. A branch principal (`main`) não foi alterada.

## 2. O que foi executado

### 2.1 Testes automatizados no servidor de GameTest (Minecraft real)

Comando: `./gradlew runGameTestServer` — servidor de GameTest do Forge, 6 testes obrigatórios.

| Teste | O que valida | Resultado |
| --- | --- | --- |
| `guardDamagesHostileMob` | Guarda persegue e causa dano em hostil | APROVADO |
| `oneImpactPerStrike` | Exatamente um impacto por golpe (sem dano duplicado) | APROVADO |
| `guardIgnoresUnprovokedPlayer` | Não ataca jogador que não o provocou | APROVADO |
| `guardsDoNotTargetEachOther` | Dois guardas não se atacam | APROVADO |
| `guardAcceptsItsAttackerAsTarget` | Aceita como alvo quem o atacou | APROVADO |
| `guardRecordsPlayerAsAttacker` | Registra o jogador agressor | APROVADO |

Saída real: `All 6 required tests passed :)` / `BUILD SUCCESSFUL`.

Testes unitários (`GuardCombatTimelineTest`): 5/5 aprovados — ordem de fases, impacto único, impacto no primeiro tick de ataque, ciclo menor que o cooldown, configuração inválida rejeitada.

### 2.2 Servidor dedicado + cliente real

| # | Teste | Evidência | Resultado |
| --- | --- | --- | --- |
| 1 | Mod carrega no servidor e no cliente com GeckoLib | log de inicialização, cliente entra no mundo | APROVADO |
| 2 | Guarda persiste em Peaceful (não é removido) | vida 5,0 após 20 s; zumbi ao lado removido pelo vanilla | APROVADO |
| 3 | Guarda ataca hostil até matar | 6 golpes, 6 impactos, 6 danos; hostil removido | APROVADO |
| 4 | Um dano por golpe em execução real | contadores `strikes=6 impactAttempts=6 damageHits=6` | APROVADO |
| 5 | Jogador não provocado não é atacado | `target=none`, jogador 20/20 após 30 s ao lado do guarda | APROVADO |
| 6 | Jogador ataca → guarda revida | guarda 24→23 de vida; 5 golpes/5 impactos; jogador 20→5,33 | APROVADO |
| 7 | Morte do jogador pelo guarda | tela "Você morreu! CombatQA recebeu uma sentença de morte de Guarda de Midland" | APROVADO |
| 8 | Persistência após salvar e reiniciar | guarda recarregado em (0,5 / 100 / 0,5), 24 de vida, `IDLE`, contadores zerados, sem dano fantasma | APROVADO |
| 9 | Peaceful: guarda revida jogador | — | NÃO EXECUTÁVEL por decisão do usuário (modo descartado) |
| 10 | Sincronização entre dois jogadores | — | NÃO EXECUTÁVEL (exigiria dois clientes simultâneos; o estado de combate é sincronizado por `SynchedEntityData` e o dano é aplicado só no servidor) |

## 3. Achados relevantes durante o teste

1. **Chunks sem jogador não processam entidades.** O primeiro cenário mostrou o guarda imóvel e sem alvo. A causa não era o mod: sem jogador online, os chunks da área não eram processados. Com `forceload` (ou jogador presente) o combate ocorreu normalmente. Isso é comportamento padrão do Minecraft e foi registrado para não ser confundido com falha do mod.
2. **Peaceful bloqueia aquisição de alvo no vanilla.** `LivingEntity.canAttack` e `TargetingConditions.test` recusam alvos quando a dificuldade é Peaceful. Como o usuário descartou esse modo, nenhum código foi alterado; o comportamento fica igual ao vanilla.
3. **Alvos hostis próximos têm prioridade entre golpes.** O `HurtByTargetGoal` do vanilla define o alvo uma vez e depois cede lugar ao `NearestAttackableTargetGoal`. O alvo permanece estável **durante** o golpe (garantido pelo `GuardCombatGoal` e pelo teste `oneImpactPerStrike`), mas pode mudar entre golpes. Comportamento considerado correto.
4. **Falhas iniciais dos GameTests eram de infraestrutura de teste**, não do mod: arenas vizinhas compartilhavam linha de visão e entidades de outros testes entravam no alcance. Corrigido fechando a arena com paredes; nenhuma lógica de combate precisou mudar.

## 4. Arquivos de evidência

- `evidence/01-joined.png` — cliente no mundo
- `evidence/03-aim.png` — guarda de frente, mira sobre o peito
- `evidence/05-retaliation-damage.png` — tela de morte com a mensagem localizada
- `evidence/07-peaceful-attack.png` — tentativa de ataque em Peaceful
- `mcserver-0.3.1.log` — log completo do servidor dedicado
- `combat-gametest.log` — log completo dos GameTests
- `client-console.log` — log do cliente

## 5. Conclusão

A versão 0.3.1 executa em Minecraft 1.20.1 / Forge 47.4.28 com Java 17 e GeckoLib 4.7.2, com combate autoritativo no servidor, um único dano por golpe, sem ataque a jogador inocente e com persistência correta após salvar e reiniciar. Não houve crash, exceção do mod ou dependência ausente em nenhuma das execuções.
