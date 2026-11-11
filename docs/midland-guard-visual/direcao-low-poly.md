# Guarda de Midland — revisão de direção visual

**Data:** 11/11/2026
**Branch de trabalho:** `feature/midland-guard-visual`

## Direção escolhida pelo usuário

O usuário esclareceu que não pretende limitar os personagens ao formato dos bonecos vanilla. Após consultar o exemplo Low Poly Knight no Sketchfab, concordou com a direção low-poly com proporções humanas. [1]

O conceito blocado anteriormente aprovado deixa de ser o alvo visual final. A nova prancha deverá mostrar cabeça proporcionalmente menor, silhueta anatômica, membros afunilados, articulações legíveis, rosto mais definido e armadura com curvas representadas por facetas. Serão preservados o caráter de soldado comum, o capacete nasal, espada de uma mão, escudo de infantaria e materiais sóbrios.

## O que muda na implementação

A geometria futura não ficará limitada às sete partes cuboidais de `HumanoidModel`. O arquivo editável deverá representar a malha escolhida e seus pivôs; o renderer será adaptado para consumi-la e animá-la. A escolha exata do pipeline ainda será validada, sem presumir que o modelo vanilla ou uma biblioteca ausente já oferece suporte automático a malhas arbitrárias.

Blockbench é uma ferramenta de modelagem, não uma solução automática para toda integração. Seu editor oferece cubos e malhas para produzir formas low-poly. Um modelo poligonal mais anatômico exige revisão do pipeline atual de geometria/renderização. [2]

Minecraft 1.20.1, Forge 47.4.28, Java 17, Gradle 8.8, registro/ovo/aba criativa, 24 HP, velocidade base 0,23, IA e diálogo permanecem requisitos preservados.

## Situação dos testes anteriores

O primeiro protótipo cuboidal chegou ao Minecraft, foi invocado pelo ovo, renderizou e respondeu em português. As poses de ataque e defesa foram acionadas, mas a primeira postura de defesa girava o escudo excessivamente; isso foi identificado para correção. A sincronização de ações foi corrigida para usar o tempo do mundo. Nem esse teste parcial nem a compilação inicial validam o futuro personagem poligonal.

Não foi publicado um JAR final dessa etapa. O arquivo `struggler-0.2.0.jar` gerado durante a primeira rodada é experimental; não representa a nova direção visual. A versão funcional da Fase 1 continua preservada.

## Próxima aprovação

A nova prancha apresenta vistas frontal, lateral e traseira, além de uma aproximação do rosto. Ela é um conceito artístico, não um render do modelo final nem uma captura do jogo. A modelagem final poligonal será iniciada depois da aprovação dessa prancha.

## References

[1]: https://sketchfab.com/3d-models/low-poly-knight-4c9d5dd1740343a9b5b50a2bacd7ee32 "Exemplo visual Low Poly Knight"
[2]: https://www.blockbench.net/ "Blockbench — editor low-poly de cubos e malhas"
