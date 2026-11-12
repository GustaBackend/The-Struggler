# The Struggler — GeckoLib 4.7.2 → 4.8.4 API inspection

The controlled API comparison finds no source-level incompatibility in The Struggler’s active entity, model, renderer, animation, or controller usage. The only required migration edit is the dependency coordinate in `build.gradle:59`, changing `software.bernie.geckolib:geckolib-forge-1.20.1:4.7.2` to `4.8.4`. The current `mods.toml:29` range, `[4.7.2,5)`, already accepts 4.8.4, so it does not need a compatibility change. No Java, model, renderer, animation JSON, or feature changes are indicated by this inspection.

This report is inspection-only. The repository was not edited, no Git operation was performed, and no Gradle task was run.

## Scope reviewed

The active GeckoLib surface is concentrated in three classes:

- `src/main/java/com/struggler/world/entity/MidlandGuard.java:26-33,46,53-61,85,114-156` implements `GeoEntity`, creates an `AnimatableInstanceCache`, registers one `AnimationController`, builds eight `RawAnimation` values, selects clips through `AnimationState`, and uses `PlayState`.
- `src/main/java/com/struggler/client/model/MidlandGuardModel.java:7-40` extends `GeoModel`, returns model/texture/animation resources, and uses `DataTickets.ENTITY_MODEL_DATA`, `AnimationState.getData`, and `getBone` in `setCustomAnimations`.
- `src/main/java/com/struggler/client/render/MidlandGuardRenderer.java:12-29` extends `GeoEntityRenderer`, calls the `(Context, GeoModel<T>)` constructor, and overrides `getDeathMaxRotation`.

The eight authored clips remain ordinary Bedrock/GeckoLib JSON clips: `idle`, `walk`, `run`, `attack`, `defend`, `block`, `hurt`, and `death` (`src/main/resources/assets/struggler/animations/midland_guard.animation.json:4,103,252,422,592,743,867,969`). No `math.pi`, `GeoRenderer#getPackedOverlay`, `DefaultAnimations`, stateless animatables, `NetworkUtil`, or `geckoanimfix` reference exists in the active project files.

## Download provenance and verification

The exact official Modrinth API record is version ID `aC5KMoNg`, named `Forge 1.20.1`, version `4.8.4`, with file ID `dqsu6n3D`. The API record identifies the target filename, 1.20.1 game version, Forge loader, file size of 1,038,209 bytes, and the CDN URL used for the download. [1]

Downloaded artifact:

```text
/home/ubuntu/gecko-migration/geckolib-forge-1.20.1-4.8.4.jar
```

The local file is 1,038,209 bytes and its manifest reports `Implementation-Version: 4.8.4`. The official Modrinth API supplies SHA-1 and SHA-512; both match the local computation:

```text
SHA-1:   50e1407869ef0e909e3bdda9328b8bd7db03fdc0
SHA-256: 4518779a31b6c2abb92c98b5ad17ef3d60177df68626bfc86c44de2764913c2b
SHA-512: 443a321a293e3cb92fbbc3c97a38b179a45d1db703cd7bdb123270a9e04e7b7d67d5b06cd8e77a6d4d9139e033e901b9660cccf53ad5f9beaae6d06eae68febf
```

The supplied baseline is also the official Modrinth 4.7.2 Forge 1.20.1 file: `/home/ubuntu/midland-guard-visual/geckolib/geckolib-forge-1.20.1-4.7.2.jar`. Its API record is version ID `AiVKLYep`, file ID `D0ELCrlI`. [2]

```text
Baseline SHA-1:   4635f16786b5a23b8d059e6c2c675d1c8dec01d6
Baseline SHA-256: a8ea443709fb960f2f7f4271137239fec04c0ed7225594b37d81730345637a12
Baseline SHA-512: 40aefbb89e4a65c1955bd05c13cb656c0902e545741770d9395037f6a87684e91676935e0d5b7be0419abd1599272fa3aa245dc3976208b82f568d994813e314
```

The complete API records, computed hashes, manifest versions, source URLs, sizes, and verification booleans are preserved in [`source-metadata.json`](source-metadata.json). The direct signature evidence is in [`javap-signatures.diff`](javap-signatures.diff), with per-class outputs under [`javap/`](javap/).

## 4.8.x changelog impact

The official Forge 1.20.1 Modrinth records for 4.8, 4.8.1, 4.8.2, 4.8.3, and 4.8.4 were reviewed together. [3]

- **4.8.0:** added stateless animatable types, `NetworkUtil`, `RawAnimation#getStageCount`, `AnimationController#getStateHandler`, additional default controllers, a renderer constructor overload accepting an entity type, optional Forge network handling, and a `GeoReplacedEntity` partial-tick fix. The Struggler uses the existing stateful `GeoEntity` path, the existing `RawAnimation` builder methods, the existing controller constructor, and the existing `(Context, GeoModel<T>)` renderer constructor. None of these additions require a change.
- **4.8.1:** reverted a Forge-parity change because Forge 1.20.1 users were not as up to date as expected. No current source usage depends on that parity change.
- **4.8.2:** fixed triggered animations on singleton animatables in a multiplayer Fabric case. `MidlandGuard` is an instanced entity with `GeoEntity` and `GeckoLibUtil.createInstanceCache(this)`, not a singleton animatable, and the target is Forge. This is not a migration requirement.
- **4.8.3:** fixed `math.pi` parsing/evaluation that could break animations and added an incompatibility with `geckoanimfix`. The project’s animation JSON contains no `math.pi`, and the project has no `geckoanimfix` dependency or reference. The runtime installation should still avoid adding `geckoanimfix` alongside GeckoLib 4.8.4.
- **4.8.4:** updated the logo, removed an erroneous face-UV-rotation error log, added nullability markers to `GeoCubes`, clarified an Oculus compatibility error, and softened the deprecation of `GeoRenderer#getPackedOverlay`. There is no controller or animation behavior change listed. The `getPackedOverlay` signature is unchanged, and The Struggler does not call it.

The official GeckoLib entity documentation confirms that the current architecture—an entity implementing `GeoEntity`, a cache returned by `getAnimatableInstanceCache`, controller registration through `AnimatableManager.ControllerRegistrar`, a `GeoModel`, and a `GeoEntityRenderer`—is the standard GeckoLib 4 entity arrangement. [4]

## `javap` signature comparison

`javap -protected -s` was run against the supplied 4.7.2 JAR and the downloaded 4.8.4 JAR for every imported GeckoLib class and the relevant nested/renderer surfaces:

- `software.bernie.geckolib.animatable.GeoEntity`
- `AnimatableInstanceCache`
- `AnimatableManager` and `AnimatableManager.ControllerRegistrar`
- `AnimationController`
- `AnimationState`
- `RawAnimation`
- `PlayState`
- `GeckoLibUtil`
- `DataTickets`
- `GeoModel`
- `EntityModelData`
- `GeoRenderer`
- `GeoEntityRenderer`

There are no removed or changed public/protected descriptors among those surfaces. The diff contains additions only:

```text
AnimationController#getStateHandler()
  ()Lsoftware/bernie/geckolib/core/animation/AnimationController$AnimationStateHandler;

RawAnimation#RawAnimation(List<RawAnimation.Stage>)
  (Ljava/util/List;)V

RawAnimation#getStageCount()
  ()I

GeoEntityRenderer(Context, EntityType<? extends T>)
  (Lnet/minecraft/client/renderer/entity/EntityRendererProvider$Context;
   Lnet/minecraft/world/entity/EntityType;)V
```

The exact constructor used by `MidlandGuardRenderer`, `GeoEntityRenderer(Context, GeoModel<T>)`, is unchanged. The exact constructor used by `MidlandGuard`, `AnimationController(this, "guard", 0, this::animationState)`, is unchanged. The `RawAnimation` methods used by the eight clips—`begin`, `thenLoop`, `thenPlay`, and `thenPlayAndHold`—are unchanged. The `AnimationState` methods used by the controller and model—`isMoving`, `resetCurrentAnimation`, `setAndContinue`, and `getData`—are unchanged. The `GeoModel` resource methods and `setCustomAnimations` surface are unchanged. `DataTickets.ENTITY_MODEL_DATA`, `EntityModelData.headPitch`, `EntityModelData.netHeadYaw`, `GeoEntity.getAnimatableInstanceCache`, and `GeckoLibUtil.createInstanceCache` remain available with the same descriptors.

`GeoRenderer#getPackedOverlay(T,float)` retains descriptor `(Lsoftware/bernie/geckolib/core/animatable/GeoAnimatable;F)I`. The classfile metadata changed from `@Deprecated(forRemoval=true)` in 4.7.2 to plain `@Deprecated` in 4.8.4. Since the renderer does not override or invoke this method, no adjustment is needed.

## Compatibility adjustments needed

1. **Required dependency update:** change only the GeckoLib version in `build.gradle` from `4.7.2` to `4.8.4` when performing the migration. The repository uses the same official GeckoLib Maven repository and group, so the coordinate shape does not change.
2. **`mods.toml`: no required compatibility change:** `[4.7.2,5)` already admits 4.8.4. If the release policy is to reject older GeckoLib versions after the migration, the minimum could be tightened to `[4.8.4,5)`, but that is a version-policy decision rather than an API requirement.
3. **Entity/model/renderer Java:** no adjustment indicated. Keep the current stateful single-controller design and existing animation priority logic; do not introduce the new stateless APIs or renderer overload because the request is a controlled dependency migration with no feature additions.
4. **Animation resources:** no adjustment indicated. Existing clip names and builder calls remain compatible. The 4.8.3 `math.pi` fix is harmless to these JSON files because they contain no such expression.
5. **Runtime packaging:** do not co-install `geckoanimfix` with GeckoLib 4.8.4. It is not present in this project, so no repository change is required.

No Gradle build, compile, game test, or runtime smoke test was run, by instruction. Therefore this report establishes artifact provenance and source/API compatibility, but it does not claim post-migration build or runtime validation.

## Preserved files

- [`geckolib-forge-1.20.1-4.8.4.jar`](geckolib-forge-1.20.1-4.8.4.jar)
- [`source-metadata.json`](source-metadata.json)
- [`javap-signatures.diff`](javap-signatures.diff)
- [`javap/`](javap/)

## References

[1]: https://api.modrinth.com/v2/version/aC5KMoNg "Official Modrinth API record for GeckoLib Forge 1.20.1 4.8.4"
[2]: https://api.modrinth.com/v2/version/AiVKLYep "Official Modrinth API record for GeckoLib Forge 1.20.1 4.7.2"
[3]: https://api.modrinth.com/v2/project/geckolib/version?loaders=%5B%22forge%22%5D&game_versions=%5B%221.20.1%22%5D "Official Modrinth API records and changelogs for GeckoLib Forge 1.20.1"
[4]: https://github.com/bernie-g/geckolib/wiki/Custom-GeckoLib-Entity "Official GeckoLib Custom GeckoLib Entity documentation"
