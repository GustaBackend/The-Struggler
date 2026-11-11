# Auditoria somente leitura — Midland Guard

**Data da auditoria:** 11 nov. 2026
**Projeto auditado:** `/home/ubuntu/The-Struggler`
**Alvo declarado:** Minecraft Java 1.20.1 · Forge 47.4.28 · Java 17 · Gradle Wrapper 8.8
**Escopo:** renderer/modelo, registros, entidade/IA/interação, assets e dependências; separar animação visual de comportamento.
**Restrição cumprida:** nenhum arquivo do projeto foi editado, nenhum commit/branch foi alterado e o cliente Minecraft não foi executado nesta auditoria.

## Sumário executivo

O protótipo atual é uma fundação Forge funcional, não uma entidade de combate. `MidlandGuard` estende `PathfinderMob` e registra somente `FloatGoal`, `WaterAvoidingRandomStrollGoal`, `LookAtPlayerGoal` e `RandomLookAroundGoal` (`src/main/java/com/struggler/world/entity/MidlandGuard.java:17-35`). Portanto, **a IA atual não possui alvo, perseguição, combate, corrida ou defesa**. A interação existente é apenas uma saudação no clique direito (`MidlandGuard.java:37-48`). Uma pose ou oscilação de membros no renderer não deve ser interpretada como implementação desses comportamentos.

O modelo atual (`src/main/java/com/struggler/client/model/MidlandGuardModel.java:14-47`) é Java vanilla baseado em `HumanoidModel`, com sete partes humanoides (`head`, `hat`, `body`, braços e pernas), atlas 64×64 e nenhum `setupAnim`/controlador/keyframe próprio. Isso já habilita o ciclo vanilla de caminhada e orientação da cabeça herdado de `HumanoidModel`, desde que o renderer forneça os parâmetros normais, mas não há idle respiratório, corrida distinta, ataque efetivamente disparado pela IA, defesa, reação corporal a dano ou sequência de morte customizada.

A recomendação de menor risco é manter `24.0` de vida, `0.23` de `MOVEMENT_SPEED` e os goals atuais, implementar primeiro animações puramente visuais em Java vanilla e deixar combate/corrida/defesa para uma fase explicitamente aprovada. Não é necessário adicionar GeckoLib para poses procedurais; GeckoLib também não está declarado no projeto.

## Estado técnico e arquivos relevantes

### Versões, build e dependências

- `gradle/wrapper/gradle-wrapper.properties:3` aponta para `gradle-8.8-bin.zip`.
- `gradle.properties:3-11` fixa Minecraft 1.20.1, Forge 47.4.28, Java/mod id e metadados do mod.
- `build.gradle:15` fixa toolchain Java 17; `build.gradle:17-44` configura mappings oficiais, runs e resources; `build.gradle:50-52` declara somente `net.minecraftforge:forge:${minecraft_version}-${forge_version}`.
- Não há `geckolib`, outra biblioteca de animação, repositório de terceiros ou dependência adicional em `build.gradle`, `gradle.properties`, `settings.gradle` ou no JAR atual.
- `README.md:4` diz que a fundação constrói com Gradle 8.8/Java 17 e que o smoke test não estava no README; `README.md:23` menciona GeckoLib como milestone futuro. Essa menção é roadmap, não evidência de que GeckoLib esteja instalado.

### Registro e integração

- `src/main/java/com/struggler/TheStruggler.java:10-19`: inicializa os `DeferredRegister` de itens, entidades e creative tab.
- `src/main/java/com/struggler/registry/ModEntities.java:18-22`: registra `struggler:midland_guard` como `MobCategory.CREATURE`, bounding box `0.6F × 1.95F`.
- `ModEntities.java:26-31`: associa `MidlandGuard.createAttributes()` ao tipo.
- `src/main/java/com/struggler/registry/ModItems.java:14-24`: registra Behelit (stack 1) e spawn egg com as cores `0x6D5748`/`0xB8A58A`; não registra arma, escudo ou item de combate.
- `src/main/java/com/struggler/registry/ModCreativeModeTabs.java:14-22`: expõe somente Behelit e spawn egg.
- `src/main/java/com/struggler/client/render/MidlandGuardRenderer.java:15-37`: `MobRenderer<MidlandGuard, MidlandGuardModel>`, modelo vanilla customizado, shadow `0.5F` e textura `struggler:textures/entity/midland_guard.png`; registra layer e renderer no MOD event bus client.

### Entidade, atributos, IA e interação

`src/main/java/com/struggler/world/entity/MidlandGuard.java` é o arquivo decisivo:

- `:17-20`: `MidlandGuard extends PathfinderMob`; não implementa uma classe de mob humanoide com lógica de arma/escudo.
- `:22-27`: `MAX_HEALTH = 24.0D`, `MOVEMENT_SPEED = 0.23D`, `FOLLOW_RANGE = 24.0D`.
- `:29-35`: goals atuais são:
  - prioridade 0: `FloatGoal`;
  - prioridade 5: `WaterAvoidingRandomStrollGoal(this, 0.8D)`;
  - prioridade 6: `LookAtPlayerGoal(this, Player.class, 8.0F)`;
  - prioridade 7: `RandomLookAroundGoal`.
- Não existe `targetSelector`, `HurtByTargetGoal`, `NearestAttackableTargetGoal`, `MeleeAttackGoal`, callback de ataque, cooldown de ataque, flag de defesa ou state machine.
- O `0.8D` do goal de passeio é o fator de velocidade do goal; não é prova de corrida e não substitui o atributo `MOVEMENT_SPEED = 0.23D`.
- `:37-48`: apenas a mão principal mostra `dialogue.struggler.midland_guard.greeting` no lado cliente e retorna sucesso; não inicia ataque, guarda, sprint ou animação de ação.

### Modelo, renderer e UV

`MidlandGuardModel.java:14` herda `HumanoidModel<MidlandGuard>`. `createBodyLayer()` (`:22-45`) cria:

- cabeça em `texOffs(0, 0)`, chapéu em `texOffs(32, 0)`;
- tronco em `texOffs(16, 16)`;
- braço direito em `texOffs(40, 16)` e braço esquerdo espelhado em `texOffs(32, 48)`;
- perna direita em `texOffs(0, 16)` e perna esquerda espelhada em `texOffs(16, 48)`;
- `LayerDefinition.create(mesh, 64, 64)` (`:45`).

Não há override de `setupAnim`, `AnimationState`, keyframes, interpolação ou controlador. As partes têm os nomes esperados por `HumanoidModel`, então `super.setupAnim(...)` fornece a base vanilla (orientação da cabeça e balanço de braços/pernas conforme movimento; o caminho de ataque vanilla também é suportado quando `attackTime` é preenchido). O modelo atual, por si só, não define nenhuma animação autoral.

A textura `src/main/resources/assets/struggler/textures/entity/midland_guard.png` é PNG RGBA 64×64. Isso é compatível com o atlas declarado no Java e com os offsets UV existentes. Não há arquivo de geometria/UV separado, nem `.bbmodel`, `.geo.json` ou arquivo de animação no projeto. A imagem é opaca em toda a área 64×64; a compatibilidade comprovada é de resolução/atlas, não uma garantia de que cada face foi revisada visualmente em Blockbench.

Os assets de item são separados e corretos em estrutura:

- `src/main/resources/assets/struggler/models/item/behelit.json:1-6` usa `minecraft:item/generated` e `struggler:item/behelit`;
- `models/item/midland_guard_spawn_egg.json:1-3` usa `minecraft:item/template_spawn_egg`;
- Behelit é PNG RGBA 16×16;
- `lang/en_us.json` e `lang/pt_br.json` possuem nomes, creative tab e saudação; não existem chaves/sons de ataque, defesa, dano ou morte;
- `META-INF/mods.toml:12-24` exige Forge `[47.4.28,)` e Minecraft `[1.20.1,1.20.2)`; `pack.mcmeta:1-6` usa pack format 15.

## Animações existentes versus ausentes

| Movimento/estado | O que existe hoje | Gatilho natural disponível | Situação real |
|---|---|---|---|
| Idle | Nenhum código de respiração/balanço; `RandomLookAroundGoal` pode mover a cabeça | `ageInTicks`/`tickCount` no `setupAnim` | **Ausente como animação custom**; olhar não é idle corporal |
| Caminhada | `HumanoidModel` herdado calcula balanço de membros; `WaterAvoidingRandomStrollGoal` realmente desloca a entidade | `limbSwing`, `limbSwingAmount`/`walkAnimation` | **Existente em base vanilla**, desde que o movimento ocorra |
| Corrida | Nenhum goal/flag de sprint ou velocidade de perseguição | `isSprinting()`, velocidade/limb swing podem ser lidos | **Ausente**; aumentar pose sem mudar IA seria apenas pose de corrida |
| Ataque | `HumanoidModel`/`LivingEntityRenderer` têm suporte a `attackTime` quando a entidade chama `swing` | `swing(InteractionHand.MAIN_HAND)` e `getAttackAnim(partialTick)` | **Infraestrutura visual vanilla, mas sem gatilho atual**; sem target/goal não há ataque |
| Defesa/bloqueio | Nenhum escudo, item de uso, flag `defending` ou pose de braço | `isUsingItem()`/item ativo ou flag sincronizada nova | **Ausente em comportamento e pose** |
| Dano | `LivingEntity` já possui `hurtTime`, `hurtDuration` e `getHurtDir()`; renderer pode mostrar overlay de dano vanilla | leitura desses campos no modelo, sem alterar a vida | **Timer natural existe, pose de recuo custom ausente** |
| Morte | `LivingEntity` possui `deathTime`/`isDeadOrDying()`; renderer vanilla pode aplicar a apresentação/rotação genérica de morte | leitura de `deathTime` no modelo; ciclo de remoção da entidade | **Apresentação vanilla possível; sequência/pose custom ausente** |

Ponto crucial: `LookAtPlayerGoal`, `RandomLookAroundGoal` e a mudança de membros do `HumanoidModel` não equivalem a combate, corrida ou defesa. IA decide deslocamento/alvo/ação no lado lógico; o modelo apenas representa o estado que recebe.

## Gatilhos naturais versus novos estados/IA

### Pode ser implementado visualmente sem alterar a IA

1. **Idle:** sobrescrever `setupAnim`, chamar `super.setupAnim(...)` primeiro e aplicar uma oscilação pequena com `Mth.sin(ageInTicks * frequência)` no tronco, cabeça/chapéu e braços. Nenhum novo estado lógico é necessário.
2. **Caminhada:** preservar o `super.setupAnim(...)`; ele já usa os parâmetros vanilla de movimento. Ajustes sutis de capa/torso podem ser adicionados depois, sempre sem substituir o balanço base.
3. **Dano:** usar `entity.hurtTime > 0` e `entity.getHurtDir()` para uma breve inclinação/recuo no modelo. Isso é uma reação visual, não altera dano, vida ou IA.
4. **Morte:** usar `entity.deathTime`/`entity.isDeadOrDying()` para uma inclinação progressiva ou braços frouxos. O tempo é natural, mas deve caber no período em que o renderer mantém o cadáver.
5. **Ataque visual preparado:** respeitar `attackTime` no `setupAnim`; quando houver um futuro `swing`, o caminho vanilla poderá mover o braço sem GeckoLib. Enquanto não houver goal que chame `swing`, esse ramo ficará inativo.

### Exige novos estados ou comportamento e deve ficar fora da alteração visual inicial

- **Ataque funcional:** adicionar aquisição de alvo, por exemplo `HurtByTargetGoal`/`NearestAttackableTargetGoal`, e `MeleeAttackGoal`. O `MeleeAttackGoal` vanilla, ao entrar no alcance e vencer o cooldown, chama `swing(MAIN_HAND)` e `doHurtTarget(...)`; isso é nova IA, dano e escopo de gameplay. Também requer decidir a classe de alvo e o dano/atributos de ataque.
- **Corrida funcional:** definir condição de corrida (alvo, distância, alerta ou estado); adicionar state/goal que faça perseguição e `setSprinting(true)` ou modificador de velocidade. Apenas tocar os braços mais rápido não move o mob mais rápido. Não alterar `MOVEMENT_SPEED = 0.23D` sem aprovação.
- **Defesa funcional:** definir se é bloqueio com escudo, postura de guarda ou defesa temporária. Isso requer item/uso de item ou uma flag sincronizada (por exemplo `DEFENDING`) com início/fim e regras de dano; o renderer então lê esse estado para posar braços/torso.
- **Ataque/defesa dirigidos por interação:** o clique direito atual é diálogo. Transformá-lo em equipar arma, iniciar guarda ou atacar mudaria contrato de interação e deve ser uma decisão de design separada.

Os campos vanilla disponíveis localmente para essa separação incluem `LivingEntity.hurtTime`, `deathTime`, `attackAnim`, `walkAnimation`, `swing(...)`, `doHurtTarget(...)`, `setSprinting(...)`, `isUsingItem()` e `isDeadOrDying()`, além de `Mob.getTarget()`, `setAggressive(...)` e `isAggressive()`. A existência desses campos/métodos não significa que o `MidlandGuard` já os use.

## Implementação recomendada sem bibliotecas adicionais

### Fase visual, baixo risco

1. Manter `MidlandGuardModel` como `HumanoidModel`/`LayerDefinition`, o renderer atual e a textura 64×64.
2. Adicionar somente um override de `setupAnim(MidlandGuard entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch)` no modelo.
3. Chamar `super.setupAnim(...)` no começo para conservar cabeça, caminhada e suporte vanilla a `attackTime`.
4. Aplicar em seguida, com prioridades claras, as camadas: idle muito leve quando parado; caminhada somente como complemento; dano durante `hurtTime`; morte como estado terminal; ataque apenas sobre `attackTime`.
5. Limitar amplitudes e restaurar/derivar transformações de forma determinística a cada chamada. Não guardar pose acumulada em campos sem reset, para evitar drift/jitter.
6. Manter toda lógica de estado/IA em `MidlandGuard` e toda transformação de membros em `MidlandGuardModel`; não fazer o modelo decidir alvos ou causar dano.

Essa fase consegue entregar idle, caminhada, reação de dano e apresentação de morte customizadas e pode deixar o caminho de ataque pronto, sem mudar os quatro goals existentes, os 24 HP ou a velocidade 0.23.

### Fase posterior, somente com escopo de gameplay aprovado

1. Adicionar target goals e `MeleeAttackGoal`, escolhendo explicitamente alvos, alcance, intervalo e dano.
2. Confirmar como `swing` será refletido pela pose de ataque antes de alterar recursos/modelo.
3. Definir corrida e defesa como estados sincronizados e testáveis; só então ligar `setSprinting`/modificadores e bloqueio.
4. Revalidar prioridade dos goals para não fazer combate interromper passeio/olhar de maneira inesperada.

Não recomendo adicionar GeckoLib apenas para os estados acima: os timers e transformações vanilla são suficientes para animações procedurais simples em Java. GeckoLib só faria sentido se a decisão de produto exigir keyframes complexos/exportação direta de animações; nesse caso seria uma mudança explícita de dependência, Gradle, empacotamento e escopo.

## `.bbmodel` planejável, Java entity e UV

- Não existe `.bbmodel` no repositório, no `src.zip` ou nos diretórios de recursos auditados.
- O formato atual é Java nativo (`LayerDefinition`, `MeshDefinition`, `PartDefinition`, `CubeListBuilder`, `PartPose`), portanto é editável como código e já é diretamente consumido pelo renderer Forge. Não é um export/import round-trip de Blockbench.
- É possível planejar um `.bbmodel` futuro como fonte editável de design, preservando atlas 64×64 e uma hierarquia compatível com `head`, `hat`, `body`, braços e pernas. Contudo, o `.bbmodel` não seria consumido automaticamente pelo pipeline atual; sem GeckoLib, seria necessário transcrever/exportar a geometria para `LayerDefinition` Java (ou adotar outro pipeline de modelo, o que aumenta escopo).
- Antes de gerar qualquer modelo, deve-se fixar: dimensões/bounding box `0.6 × 1.95`, pivôs, hierarquia de membros, resolução 64×64, offsets UV, espelhamento dos lados e quais partes receberão poses de defesa/ataque. Nesta auditoria nenhum modelo foi gerado.
- A textura já tem o tamanho correto, mas a ausência de metadado `.bbmodel` significa que a conferência fina de cada face/orientação UV ainda é manual. Não há erro de resolução evidente; não declarar UV final como validado apenas pela existência do PNG.

## Ambiente isolado anterior disponível

O diretório `/home/ubuntu/minecraft-tests/berserk-struggler-1.20.1-forge-47.4.28` existe e foi inspecionado somente por arquivos. Ele contém JAR, logs, mundo salvo, `runtime-test-report.md` e capturas em `evidence/`. O relatório anterior (`runtime-test-report.md:3-22`) registra:

- Minecraft 1.20.1, Forge 47.4.28, OpenJDK 17.0.21 e JAR `struggler-0.1.0.jar` com SHA-256 `0391e78e5b315ff2fc62bbed50bc7977e588f232503dd44a7abc68a3c916dfc9`;
- carregamento do Forge/mod, creative world, spawn egg, modelo/textura e saudação aprovados;
- deslocamento observado em aproximadamente 15 s, coerente com `WaterAvoidingRandomStrollGoal`;
- nenhum erro de recurso/renderer atribuído ao mod no log;
- avisos de autenticação HTTP 401 e OpenAL/Xvfb documentados como limitações do ambiente, não falhas do mod.

Isso é evidência histórica de baseline, não um teste executado nesta auditoria e não comprova ataque, corrida ou defesa. O snapshot `/home/ubuntu/The-Struggler/src.zip` também é anterior/incompleto: seu manifesto contém uma `ClientModEvents.java` que registrava temporariamente `ZombieRenderer`, enquanto o código ativo já contém `MidlandGuardModel`/`MidlandGuardRenderer`. Deve-se tratar `src/main/` atual como fonte de verdade, não copiar a classe antiga do ZIP.

## Riscos e decisão sugerida

- **Risco alto de escopo:** adicionar target/ataque/corrida/defesa enquanto se pede apenas visual altera gameplay, sincronização, balanceamento e prioridade de goals.
- **Risco médio de regressão visual:** substituir `super.setupAnim` remove a caminhada/orientação vanilla; sobrescrever membros em ordem errada gera tremor ou cancela ataque.
- **Risco de contrato UV:** mudar cubos/pivôs sem atualizar atlas 64×64 e offsets pode deslocar textura; manter o Java atual até existir um `.bbmodel` planejado.
- **Risco de dependência:** seguir o roadmap textual de GeckoLib sem declarar dependência deixaria o build inconsistente; evitar isso na fase sem biblioteca.

**Decisão recomendada:** preservar `MAX_HEALTH = 24.0D`, `MOVEMENT_SPEED = 0.23D`, `FOLLOW_RANGE = 24.0D` e os goals atuais; implementar apenas camadas visuais vanilla em `setupAnim`. Registrar ataque, corrida e defesa como requisitos futuros que precisam de novos estados/IA e aprovação de escopo. O workspace estava limpo (`feature/midland-guard-visual` sem alterações) ao fim da inspeção.
