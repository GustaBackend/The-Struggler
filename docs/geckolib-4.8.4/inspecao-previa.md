# Etapa técnica A — inspeção antes da migração

Base: tag `v0.3.3`, commit `8256acd` (`v0.3.3^{}`). Branch isolada: `experiment/geckolib-4.8.4-from-v0.3.3`.

As referências a 4.7.2 foram localizadas com `git grep -n '4.7.2'` antes da alteração. Os pontos ativos são `build.gradle` e `src/main/resources/META-INF/mods.toml`. README e `art/midland_guard/animation-spec.md` descrevem a base estável; os documentos em `docs/` são históricos e não serão reescritos para fingir que versões anteriores usavam 4.8.4.

Foram comparadas as assinaturas públicas/protegidas das classes usadas por `MidlandGuard`, `MidlandGuardModel` e `MidlandGuardRenderer` nos JARs oficiais 4.7.2 e 4.8.4. Não foi identificada assinatura usada removida ou modificada. O controlador único, cache por entidade, oito clips RawAnimation, pivôs e métodos de recursos permanecem compatíveis. A inspeção completa está em `api-inspection.md`, acompanhada do diff javap e metadados oficiais.

Não há uso de `math.pi`, singleton animatables, `getPackedOverlay` ou `geckoanimfix` no mod. Não são necessárias mudanças Java nem nos recursos de animação. Isso é uma conclusão de inspeção, não substitui compilação e execução.

Arquivos de configuração alterados: `build.gradle` (4.8.4), `gradle.properties` (identidade experimental `0.3.3-gecko484-exp1`) e `mods.toml` (faixa exata `[4.8.4]`). A faixa antiga já aceitava 4.8.4: fixá-la é uma medida de isolamento do experimento, não uma correção de API.

Os 38 testes unitários e 26 GameTests da base foram executados novamente antes da migração e passaram, sem alteração ou remoção de testes. A branch principal, a tag e releases estáveis serão preservadas.
