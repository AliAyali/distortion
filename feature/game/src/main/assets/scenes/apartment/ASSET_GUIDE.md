# Optional apartment sprite assets (no assets required to run)

The renderer includes a procedural fallback for every item. To upgrade the room with custom art, place **transparent WebP sprites** in `feature/game/src/main/assets/scenes/apartment/objects/` using these exact lowercase names. If a matching file exists, it replaces the procedural drawing automatically.

## Interactable items

- `front_door.webp`
- `lamp.webp`
- `window.webp` — include the frame, glass and a visible exterior view of the night city
- `mirror.webp`
- `radio.webp`
- `clock.webp`
- `family_photo.webp`
- `phone.webp`
- `closet.webp`
- `bed.webp`

## Background furniture / dressing

- `sofa.webp`, `coffee_table.webp`, `bookshelf.webp`, `tv_stand.webp`, `television.webp`
- `plant.webp`, `sideboard.webp`, `dining_table.webp`, `chair.webp`
- `kitchen_counter.webp`, `refrigerator.webp`, `wall_art.webp`, `radiator.webp`
- `bedside_table.webp`, `ceiling_light.webp`

## Art requirements

Use a front-facing, isolated object with transparent margins; avoid a full-scene background. Recommended source canvas is 512×768 or 768×768, sRGB, transparent WebP/PNG converted to WebP, no text baked into the image. Leave a small transparent margin around the silhouette. A window sprite should contain the city view behind the glass; a mirror sprite should have its own frame and reflected surface. The game computes perspective size from the object position, so do not bake shadows extending far outside the object silhouette.

For a genuine photorealistic 3D result rather than projected sprites, the next major step is a real 3D scene with textured meshes, lightmaps and a compatible renderer. This build stays native and uses perspective billboards intentionally so the existing Kotlin/Compose navigation, saving and game logic remain intact.
