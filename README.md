# BERSERK — The Struggler

Minecraft Java **1.20.1**, Forge **47.4.28**, Java **17**, Gradle **8.8**.

A branch `feature/midland-guard-visual` contém a versão **0.2.0** do Guarda de Midland, com proporções vanilla, capacete fechado, armadura volumétrica e animações nativas de **GeckoLib 4.7.2**. O experimento poligonal foi descartado. A fundação `feature/phase-1-foundation`, commit `bf740fc3d4fb68a9a3482106182b854dd33f4b99`, permanece preservada.

## Instalação

Instale Forge 47.4.28 para Minecraft 1.20.1. Coloque `struggler-0.2.0.jar` e `geckolib-forge-1.20.1-4.7.2.jar` na pasta `mods`. Remova outras versões do Struggler dessa mesma instância; não carregue 0.1.0 e 0.2.0 juntas. GeckoLib é uma dependência externa obrigatória, não está embutida no JAR do Struggler.

Distribuição oficial compatível: [GeckoLib 4.7.2 para Forge 1.20.1](https://modrinth.com/mod/geckolib/version/AiVKLYep).

No criativo, abra a aba The Struggler, use o ovo do Guarda de Midland e interaja com mão vazia por clique direito. Foram preservados o registro, ovo, aba, Behelit, 24 HP, velocidade base 0,23, IA de caminhada/observação e saudação em português/inglês.

## Modelo e animações

O arquivo editável está em `art/midland_guard/midland_guard.bbmodel`, com 52 cubos, nove bones, textura 128×128 e sete animações. Os arquivos carregados pelo GeckoLib ficam em `assets/struggler/geo/`, `assets/struggler/animations/` e `assets/struggler/textures/entity/`.

Idle, caminhada, corrida, ataque, defesa, dano e morte têm clips nativos. Dano e morte respondem aos eventos reais da entidade. Ataque e defesa são demonstrações visuais: não implementam combate autônomo ou bloqueio de dano. Corrida é acionada pela demonstração, não por uma rotina autônoma de patrulha.

Com permissão de operador nível 2, use `/strugglerguard idle|walk|run|attack|defend|reset` e, opcionalmente, um seletor de Guarda. Sem seletor, o comando usa o Guarda mais próximo. `reset` devolve o controle à IA normal.

## Compilação

Execute `./gradlew build` com JDK 17, ou `gradlew.bat build` no Windows. O Gradle baixa GeckoLib pelo Maven oficial. O artefato é gerado em `build/libs/struggler-0.2.0.jar`.

`tools/generate_midland_gecko.py` é a fonte de geração da geometria, atlas e keyframes; executá-lo sobrescreve os exports. Alterações feitas diretamente no Blockbench devem ser exportadas para os recursos GeckoLib correspondentes e preservadas antes de regenerar.

## Teste real

A versão 0.2.0 foi executada no cliente Forge, em mundo superplano isolado sob Xvfb. Foram registrados invocação, aparência, saudação, deslocamentos, animações, dano/morte e recarga dos recursos. Consulte `docs/midland-guard-visual/relatorio-vanilla-geckolib.md` para resultados, ressalvas e SHA-256. Compilar não substitui esse teste de execução.
