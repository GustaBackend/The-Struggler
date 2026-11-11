# Guarda de Midland 0.2.0 — vanilla e GeckoLib

**Teste real em 11/11/2026:** Minecraft Java 1.20.1, Forge 47.4.28, Java 17, Gradle 8.8, GeckoLib 4.7.2. Instância isolada, cliente em Xvfb 1280×720, mundo criativo superplano e servidor integrado. O experimento low-poly foi removido da versão publicada; não foram geradas novas imagens de conceito nesta retomada.

## Resultado dos testes

- **APROVADO — compilação:** build com JDK 17/Gradle 8.8. Esse resultado é separado da execução abaixo.
- **APROVADO — execução:** cliente Forge iniciou, carregou Struggler e GeckoLib e entrou no mundo.
- **APROVADO — invocação:** novo Guarda criado pelo ovo de invocação. A entidade salva anteriormente também carregou.
- **APROVADO — modelo/recursos:** capacete fechado, placas, ombreiras, braçadeiras, botas, espada e escudo renderizados; vistas frontal/lateral/traseira registradas. Recarga F3+T concluída sem erro do mod.
- **APROVADO — saudação:** clique direito com mão vazia exibiu a fala em português. O arquivo inglês está presente e válido; sua exibição em cliente configurado em inglês não foi testada nesta rodada.
- **APROVADO — atributos:** consultas no jogo confirmaram 24 HP e velocidade base 0,23; goals de caminhada/observação foram preservados.
- **APROVADO — caminhada:** após a demonstração, a posição passou de `[4,-60,0]` para aproximadamente `[1.145,-60,0.447]`, usando ponto decimal nessa notação. Foram gravados movimento e poses.
- **APROVADO — corrida:** após a demonstração, a posição passou de `[4,-60,0]` para aproximadamente `[-2.172,-60,0.531]`. Duas fases de passada também foram capturadas com a entidade congelada no cenário de QA, separando a animação do deslocamento.
- **APROVADO — idle/ataque/defesa visuais:** clips acionados no jogo e filmados. Defesa é uma postura básica, com escudo vertical; não é um sistema de bloqueio.
- **APROVADO — dano/morte:** `/damage` causou dano real, a vida diminuiu, houve reação vermelha/recuo; dano letal acionou queda e remoção normal da entidade. Não foi apenas uma pose simulada.
- **APROVADO — Blockbench:** arquivo Bedrock importou com 52 cubos, nove bones, uma textura 128×128 e sete animações, sem erro da página.
- **APROVADO — integridade do JAR:** ZIP íntegro, classes e recursos presentes, textura empacotada idêntica à fonte; dependência GeckoLib declarada obrigatória em `mods.toml` e instalada no teste.
- **NÃO EXECUTÁVEL nesta validação — áudio:** o ambiente não forneceu dispositivo de reprodução; Minecraft desativou sons. Isso não valida áudio.

Nenhum erro de carregamento, animação ou recurso foi atribuído a Struggler/GeckoLib nos logs. Houve mensagens de perfil de sessão offline e inicialização de áudio, que permanecem nos logs entregues. Não foi executado um servidor dedicado separado, sessão com múltiplos jogadores ou teste de desempenho em hardware pessoal.

## O que é provisório

Ataque e defesa ainda são demonstrações acionadas por comando; não causam dano, escolhem alvos ou bloqueiam golpes. Corrida não é uma nova rotina autônoma. O diálogo continua sendo uma saudação única, não uma árvore de conversação. O visual é vanilla por solicitação do usuário, com capacete opaco para dispensar detalhes faciais; as capturas são o resultado real, não a prancha artística.

## Instalação e hashes

Coloque **os dois arquivos** na pasta `mods` de uma instância Forge 47.4.28/Minecraft 1.20.1 com Java 17. Remova outras versões do Struggler dessa mesma instância.

`struggler-0.2.0.jar` — SHA-256:

```text
453186cb86c3c9461ed48b94d3f34c712b2b5ee6a1a79376d20f67144e9db334
```

`geckolib-forge-1.20.1-4.7.2.jar` — SHA-256:

```text
a8ea443709fb960f2f7f4271137239fec04c0ed7225594b37d81730345637a12
```

GeckoLib é externo, não embutido no JAR do Struggler. O Maven oficial respondeu HTTP 200 e a distribuição Forge foi baixada e instalada. A página oficial lista Forge e Minecraft 1.20.1 para esse arquivo. [1] [2]

## Fontes e preservação

A fonte editável é `art/midland_guard/midland_guard.bbmodel`, com atlas e keyframes. Os recursos nativos e Java, gerador, capturas, vídeos e logs estão no pacote de entrega. As referências de infantaria, armadura e capacetes de Midland foram mantidas como inspiração de um soldado comum, sem copiar um personagem específico.

A publicação usa `feature/midland-guard-visual`; a branch principal não foi alterada. `feature/phase-1-foundation` continua em `bf740fc3d4fb68a9a3482106182b854dd33f4b99`, preservando a versão funcional 0.1.0.

## References

[1]: https://modrinth.com/mod/geckolib/version/AiVKLYep "GeckoLib 4.7.2 — Forge 1.20.1"
[2]: https://github.com/bernie-g/geckolib/wiki/Installation-%28Geckolib4%29 "Instalação oficial GeckoLib 4"
