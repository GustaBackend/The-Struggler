# Guarda de Midland — GeckoLib 4.7.2

O renderer usa `GeoEntityRenderer`; o modelo usa `GeoModel`; a entidade implementa `GeoEntity` com cache por instância e um controlador `guard`.

Os arquivos `geo/midland_guard.geo.json` e `animations/midland_guard.animation.json` são carregados nativamente pelo GeckoLib. O `.bbmodel` contém os sete conjuntos de keyframes editáveis. O renderer poligonal e o sampler experimental foram removidos.

Os clips são `animation.midland_guard.idle`, `walk`, `run`, `attack`, `defend`, `hurt` e `death`. Idle/caminhada/corrida são loops. Ataque dura 1,6 s, defesa 2 s, dano 0,5 s e morte 1 s. A morte retém sua pose final e não recebe uma segunda rotação de morte do renderer.

Prioridade: morte, dano, demonstração ativa, caminhada normal, idle. A cabeça acompanha a orientação da entidade, exceto durante a morte. Os estados e timestamps das demonstrações são sincronizados e usam o tempo do mundo. GeckoLib controla a reprodução dos clips no cliente e reinicia uma demonstração quando recebe um novo timestamp.

Use `/strugglerguard idle|walk|run|attack|defend|reset [seletor]`, com permissão 2. Idle, caminhada e corrida têm janela de 100 ticks; ataque 32; defesa 40. Caminhada/corrida navegam por um segmento de até oito blocos. `reset` encerra a demonstração e preserva os goals normais.

Ataque e defesa são exclusivamente visuais: não há aquisição de alvo, dano da espada ou bloqueio do escudo. Corrida é uma demonstração, não uma IA nova. Dano/morte seguem `hurtTime`/`deathTime` do Minecraft.

As capturas de poses usam `NoAI:1b` somente no mundo de QA; os testes de deslocamento usaram `NoAI:0b`. Isso não modifica o comportamento do JAR. O estado final do NPC de QA foi devolvido à IA normal antes de salvar.
