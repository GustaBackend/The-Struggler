# Guarda de Midland — GeckoLib 4.7.2 (0.3.2)

O renderer usa `GeoEntityRenderer`; o modelo usa `GeoModel`; a entidade implementa `GeoEntity` com cache por instância e um controlador `guard`.

Os arquivos `geo/midland_guard.geo.json` e `animations/midland_guard.animation.json` são carregados nativamente pelo GeckoLib. O `.bbmodel` contém os oito conjuntos de keyframes editáveis. O renderer poligonal e o sampler experimental foram removidos.

Os clips são `animation.midland_guard.idle`, `walk`, `run`, `attack`, `defend`, `block`, `hurt` e `death`. Idle/caminhada/corrida/bloqueio são loops. Ataque dura 1,6 s, defesa 2 s, bloqueio 1,2 s (sustentável), dano 0,5 s e morte 1 s. A morte retém sua pose final e não recebe uma segunda rotação de morte do renderer.

`defend` é um gesto de QA que volta ao neutro em 2 s; não serve como pose sustentada. `block` é o clip real da defesa, usado enquanto `isShieldRaised()` estiver ativo no servidor.

Prioridade: morte, golpe (windup/attack/recovery), dano, bloqueio, demonstração de QA, caminhada/corrida, idle. Bloqueio e golpe nunca coexistem: `startBlocking()` é recusado durante o golpe e `beginStrike()` baixa o escudo.

A cabeça acompanha a orientação da entidade, exceto durante a morte. Os estados e timestamps são sincronizados e usam o tempo do mundo, de modo que uma entidade já existente no mundo recebe o estado correto ao entrar em combate ou erguer o escudo. GeckoLib controla a reprodução dos clips no cliente e reinicia uma demonstração quando recebe um novo timestamp.

Use `/strugglerguard idle|walk|run|attack|defend|block|reset|status [seletor]`, com permissão 2. Idle, caminhada e corrida têm janela de 100 ticks; ataque 32; defesa 40; `block` ergue a defesa real por 20 ticks. Caminhada/corrida navegam por um segmento de até oito blocos. `reset` encerra a demonstração e preserva os goals normais. `status` imprime fase, alvo, cooldown, golpes, dano, `blocking`, `blockTicks`, `blockedHits`, `prevented`, `blockAttempts` e `blockRefusals`.

Combate e defesa são reais e autoritativos no servidor: aquisição de alvo, golpe único, cooldown de 30 ticks e redução de 40% frontal (0% pelas costas, piso de 1). `defend` e a corrida continuam sendo demonstrações de QA; a corrida não é uma IA nova. Dano/morte seguem `hurtTime`/`deathTime` do Minecraft.

As capturas de poses usam `NoAI:1b` somente no mundo de QA; os testes de deslocamento usaram `NoAI:0b`. Isso não modifica o comportamento do JAR. O estado final do NPC de QA foi devolvido à IA normal antes de salvar.
