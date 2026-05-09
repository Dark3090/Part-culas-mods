# Particle Control Mod — LeviLauncher

Mod estilo Sodium para controlar partículas no Minecraft Bedrock Edition.

---

## Estrutura dos arquivos

```
particle-mod/
├── cpp/
│   └── particles/
│       └── particles.cpp              → Hook nativo (C++)
├── java/
│   ├── nativemod/
│   │   └── ParticleMod.java           → Bridge JNI
│   ├── overlay/
│   │   └── ParticleControlOverlay.java → UI flutuante + dialog de config
│   ├── model/
│   │   └── ModIds.java                → ModIds atualizado
│   └── InbuiltModManager_snippet.java → Trecho para adicionar ao manager
├── CMakeLists_inbuiltmods.txt         → CMakeLists atualizado
└── workflow/
    └── android.yml                    → GitHub Actions workflow
```

---

## Como integrar no projeto

### 1. Copiar os arquivos C++
```
app/src/main/cpp/inbuiltmods/src/particles/particles.cpp
```

### 2. Copiar os arquivos Java
```
app/src/main/java/org/levimc/launcher/core/mods/inbuilt/nativemod/ParticleMod.java
app/src/main/java/org/levimc/launcher/core/mods/inbuilt/overlay/ParticleControlOverlay.java
```

### 3. Substituir os arquivos modificados
```
app/src/main/cpp/inbuiltmods/CMakeLists.txt     ← use CMakeLists_inbuiltmods.txt
app/src/main/java/.../model/ModIds.java         ← adicione PARTICLE_CONTROL
```

### 4. Editar InbuiltModManager.java e InbuiltOverlayManager.java
Veja o arquivo `InbuiltModManager_snippet.java` — ele tem exatamente os trechos a adicionar.

### 5. Copiar o workflow
```
.github/workflows/android.yml   ← use workflow/android.yml
```

---

## GitHub Actions — Secrets necessários

Configure em: **Settings → Secrets and variables → Actions**

| Secret | Descrição |
|--------|-----------|
| `KEYSTORE_BASE64` | `base64 -w 0 sua-keystore.jks` |
| `KEY_ALIAS` | Alias da key na keystore |
| `KEY_PASSWORD` | Senha da key |
| `STORE_PASSWORD` | Senha da keystore |

---

## Se o hook não funcionar (MCBE sem símbolo exportado)

O mod tenta encontrar `Level::addParticle` automaticamente.
Se falhar (log: "Hook inativo"), você precisa do offset manual:

1. Abra o `libminecraftpe.so` da sua versão do MCBE no IDA Pro / Ghidra
2. Busque pela função `addParticle` (string "addParticle" ou pelo padrão de bytecode)
3. Anote o offset (endereço relativo à base da lib)
4. Adicione em algum lugar do seu código de inicialização:

```java
// Exemplo — substitua 0x1A2B3C4D pelo offset real
ParticleMod.setManualOffset(0x1A2B3C4DL);
```

---

## Categorias disponíveis

| Categoria | Bitmask | Partículas incluídas |
|-----------|---------|----------------------|
| Terreno   | 1       | Quebrar blocos, passos |
| Combate   | 2       | Crits, acertos |
| Água      | 16      | Bolhas, splash, drip |
| Fogo      | 8       | Chamas, fumaça, lava |
| Magia     | 32      | Portal, encantamento, poções |
| Explosão  | 64      | Explosões |
| Ambiente  | 4       | Corações, redstone, slime |
