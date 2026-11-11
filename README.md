# The Struggler — Forge mod prototype

**Target:** Minecraft Java 1.20.1 / Forge 47.4.28 / Java 17  
**Status:** Phase 1 foundation builds successfully with Gradle 8.8 and Java 17; a Minecraft client smoke test has not yet been run.

This is the first custom-mod foundation for the BERSERK-inspired modpack. It currently registers:
- A Midland Guard entity with wandering/look behavior, a custom humanoid model and texture, and a first localized greeting.
- A spawn egg and a dedicated creative tab for the mod's current items.
- A custom Behelit item icon; supernatural event logic is not implemented yet.

## Important
The repository contains source code and Gradle project files. It is **not** a ready-to-drop `.jar` and is not a finished modpack. The original `src.zip` is retained as the source snapshot; active source files are under `src/main/`.

## Build
1. Install a 64-bit JDK 17.
2. Run `./gradlew build` (Windows: `gradlew.bat build`). The wrapper downloads the pinned Gradle distribution.
3. Import this folder into IntelliJ IDEA as a Gradle project if desired.
4. Run `./gradlew genIntellijRuns` if needed, then `./gradlew runClient`.
5. In a creative test world, open the **The Struggler** tab and use the Midland Guard Spawn Egg.
6. Right-click the guard to see its first localized greeting; check `logs/latest.log` for registration or rendering errors.

## Next implementation milestone
Give the guard a custom GeckoLib model/animation, schedule and faction data, dialogue/interactions, then build the first Midland village structure set.
