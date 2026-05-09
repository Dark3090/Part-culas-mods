// ─────────────────────────────────────────────────────────────────
//  Adicione este bloco dentro de InbuiltModManager.getAllMods()
//  logo após o último mods.add(...) existente.
//
//  Também registre o overlay em InbuiltOverlayManager da mesma forma
//  que os outros mods (FpsDisplayOverlay, etc.)
// ─────────────────────────────────────────────────────────────────

// Em getAllMods():
mods.add(new InbuiltMod(
    ModIds.PARTICLE_CONTROL,
    "Particle Control",                         // nome exibido
    "Desative partículas por categoria, como o Sodium do Java Edition.",
    true,   // hasConfig = true (abre dialog de configurações)
    addedMods.contains(ModIds.PARTICLE_CONTROL)
));


// ─── Em InbuiltOverlayManager (onde os overlays são criados) ────
// Adicione o case para PARTICLE_CONTROL junto aos outros:
//
//   case ModIds.PARTICLE_CONTROL:
//       overlay = new ParticleControlOverlay(context);
//       if (ParticleMod.init()) {
//           LOGI("Particle Control: hook OK");
//       }
//       break;
