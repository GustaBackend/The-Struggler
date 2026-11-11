# Guarda de Midland — etapas A e B

**Autor:** Manus AI
**Data:** 11/11/2026
**Branch:** `feature/midland-guard-visual`
**Base:** `bf740fc3d4fb68a9a3482106182b854dd33f4b99`

## Concluído nesta entrega

Foi criada e publicada a branch visual a partir da fundação funcional. A auditoria examinou registros, entidade, renderer, modelo, idiomas, texturas, build e disponibilidade da instância anterior. Foi produzida uma prancha original de conceito nas vistas frontal, lateral e traseira, apoiada nas referências documentadas. Nenhum arquivo Java, registro, atributo, goal ou recurso de runtime foi alterado.

## Arquitetura confirmada

O modelo atual é `HumanoidModel` com geometria `LayerDefinition`, atlas 64×64 e renderer `MobRenderer`. Não existe GeckoLib instalado nem arquivo Blockbench. Os quatro goals atuais são flutuação, passeio evitando água, olhar para jogador e olhar aleatório. Continuam intactos os 24 HP, velocidade base 0,23, ovo, aba criativa e saudação em português/inglês.

Há balanço de caminhada e orientação herdados do modelo vanilla; não existem animações autorais de corrida, ataque ou defesa nem IA de combate. A auditoria detalhada distingue esses limites. Ela é diagnóstico, não redução dos requisitos pedidos pelo usuário.

## Direção de implementação após aprovação

A proposta é produzir um `.bbmodel` editável e gerar a geometria Java a partir da mesma fonte, com atlas próprio 128×128 e UV por peças. A resolução maior é proposta para acomodar armadura/equipamentos sem comprimir tudo no atlas atual; deverá ser alterada simultaneamente no modelo e na textura. Manter os nomes/pivôs humanoides e o registro atual. Nenhuma dependência adicional está prevista neste momento.

Idle, caminhada, corrida, ataque, defesa, dano e morte continuam no escopo de animações solicitado. Idle/movimento/dano/morte podem usar relógio, velocidade e timers vanilla. Corrida/ataque/defesa precisarão de estados ou gatilhos sincronizados para execução e teste dentro do Minecraft. Esses gatilhos não devem substituir o diálogo nem remover a IA atual. Uma demonstração de ataque ou defesa não será descrita como combate autônomo ou bloqueio de dano funcional; decisões de gameplay serão explicitadas caso sejam necessárias.

A prancha é conceito gerado, não geometria pronta, UV, captura de Minecraft ou garantia de exportação. Na modelagem, serão normalizadas as pequenas diferenças de pose entre vistas e o verso do escudo receberá pega e tiras reais de couro, mesmo que a ilustração enfatize sua face externa. Capacete, peitoral, ombreiras, braçadeiras e botas terão volume próprio; sword/shield serão peças separadas ligadas aos pivôs dos braços.

## Pendente e não testado nesta etapa

Aprovação do conceito; modelagem final; `.bbmodel`; atlas final e UV; animações implementadas; integração Java; novo build/JAR/hash; testes reais da nova versão e capturas dentro do jogo. Não foi iniciado o Minecraft nesta etapa. A instância e as evidências da Fase 1 permanecem disponíveis, mas não validam o visual novo.

## Materiais preservados na branch

- `docs/midland-guard-visual/auditoria-tecnica.md`
- `docs/midland-guard-visual/referencias-e-direcao.md`
- `docs/midland-guard-visual/etapas-a-b.md`
- `docs/midland-guard-visual/guarda-midland-conceito.png`

As imagens de terceiros consultadas não serão incorporadas como texturas ou distribuídas no mod.
