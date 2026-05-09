#include <jni.h>
#include <android/log.h>
#include <dlfcn.h>
#include <cstring>
#include <cstdint>
#include <sys/mman.h>
#include <unistd.h>
#include <fstream>
#include <string>
#include <atomic>

#include "pl/Gloss.h"

#define LOG_TAG "LeviParticles"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN,  LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// ─────────────────────────────────────────────
//  Category bitmask constants (mirrored in Java)
// ─────────────────────────────────────────────
#define CAT_TERRAIN   (1 << 0)   // quebrar blocos, passos
#define CAT_COMBAT    (1 << 1)   // crits, hits, dano
#define CAT_AMBIENT   (1 << 2)   // corações, raiva, slime, redstone
#define CAT_FIRE      (1 << 3)   // chama, fumaça, lava
#define CAT_WATER     (1 << 4)   // bolha, splash, drip
#define CAT_MAGIC     (1 << 5)   // portal, encantamento, poção
#define CAT_EXPLOSION (1 << 6)   // explosão

static std::atomic<uint32_t> g_disabledCategories{0};
static std::atomic<bool>     g_allDisabled{false};
static bool                  g_initialized = false;

// ─────────────────────────────────────────────
//  MCBE ParticleType enum
//  Valores estáveis entre a maioria das versões Bedrock.
//  Se algum não funcionar na sua versão, ajuste aqui.
// ─────────────────────────────────────────────
enum class ParticleType : int {
    Bubble           = 1,
    BubbleManual     = 2,
    Crit             = 4,
    SmokeNormal      = 6,
    Flame            = 9,
    Lava             = 10,
    LavaLite         = 11,
    Smoke            = 12,
    LargeSmoke       = 15,
    RedstoneWire     = 17,
    Splash           = 18,
    ExplosionLarge   = 19,
    ExplosionHuge    = 20,
    PortalOld        = 21,
    WaterWake        = 22,
    HeartsOrAngry    = 23,
    SuspendedTown    = 27,
    Terrain          = 33,
    TownAura         = 35,
    Portal           = 36,
    MobFlame         = 45,
    WitchMagic       = 47,
    MobSpell         = 48,
    MobSpellAmbient  = 49,
    Drip             = 54,
    Slime            = 56,
    EnchantingTable  = 58,
    ExplosionManual  = 61,
    SporeBlossomShower = 66,
    // Bedrock 1.21+ particle IDs
    TrialSpawner     = 80,
};

static uint32_t getParticleCategory(int type) {
    switch (static_cast<ParticleType>(type)) {
        // Terreno
        case ParticleType::Terrain:
        case ParticleType::SuspendedTown:
            return CAT_TERRAIN;

        // Combate
        case ParticleType::Crit:
            return CAT_COMBAT;

        // Fogo
        case ParticleType::Flame:
        case ParticleType::Lava:
        case ParticleType::LavaLite:
        case ParticleType::SmokeNormal:
        case ParticleType::Smoke:
        case ParticleType::LargeSmoke:
        case ParticleType::MobFlame:
            return CAT_FIRE;

        // Água
        case ParticleType::Bubble:
        case ParticleType::BubbleManual:
        case ParticleType::Splash:
        case ParticleType::WaterWake:
        case ParticleType::Drip:
            return CAT_WATER;

        // Magia
        case ParticleType::Portal:
        case ParticleType::PortalOld:
        case ParticleType::WitchMagic:
        case ParticleType::MobSpell:
        case ParticleType::MobSpellAmbient:
        case ParticleType::EnchantingTable:
            return CAT_MAGIC;

        // Explosão
        case ParticleType::ExplosionLarge:
        case ParticleType::ExplosionHuge:
        case ParticleType::ExplosionManual:
            return CAT_EXPLOSION;

        // Ambiente
        case ParticleType::TownAura:
        case ParticleType::HeartsOrAngry:
        case ParticleType::SporeBlossomShower:
        case ParticleType::RedstoneWire:
        case ParticleType::Slime:
        case ParticleType::TrialSpawner:
            return CAT_AMBIENT;

        default:
            return 0; // desconhecido → deixa passar
    }
}

// ─────────────────────────────────────────────
//  Hook: Level::addParticle
//  Assinatura mais comum no MCBE ARM64
// ─────────────────────────────────────────────
static void* (*g_orig_addParticle)(
        void*  level,
        int    type,
        float  x,    float y,    float z,
        float  vx,   float vy,   float vz,
        int    data) = nullptr;

static void* hook_addParticle(
        void*  level,
        int    type,
        float  x,    float y,    float z,
        float  vx,   float vy,   float vz,
        int    data)
{
    if (g_allDisabled.load()) return nullptr;

    uint32_t cat = getParticleCategory(type);
    if (cat != 0 && (g_disabledCategories.load() & cat)) {
        return nullptr; // partícula bloqueada
    }

    return g_orig_addParticle(level, type, x, y, z, vx, vy, vz, data);
}

// ─────────────────────────────────────────────
//  Busca o símbolo de addParticle na libminecraftpe.so
//  Tentativas em ordem:
//   1. GlossSymbol (nome mangled ARM64)
//   2. GlossSymbol (variante alternativa)
//   3. Log de erro com instruções para scan manual
// ─────────────────────────────────────────────
static bool hookParticles() {
    void* mcLib = dlopen("libminecraftpe.so", RTLD_NOLOAD);
    if (!mcLib) mcLib = dlopen("libminecraftpe.so", RTLD_LAZY);
    if (!mcLib) {
        LOGE("Falha ao abrir libminecraftpe.so: %s", dlerror());
        return false;
    }

    GlossInit(true);

    // Tentativa 1 – assinatura padrão Bedrock ARM64
    const char* sym1 = "_ZN5Level11addParticleE12ParticleTypeRK4Vec3S3_i";
    void* sym = (void*)GlossSymbol((GHandle)mcLib, sym1, nullptr);

    if (!sym) {
        // Tentativa 2 – variante com bool extra (algumas builds 1.21+)
        const char* sym2 = "_ZN5Level11addParticleE12ParticleTypeRK4Vec3S3_ib";
        sym = (void*)GlossSymbol((GHandle)mcLib, sym2, nullptr);
    }

    if (!sym) {
        LOGE("addParticle nao encontrado via simbolo exportado.");
        LOGE("Para scan manual: use IDA/Ghidra no libminecraftpe.so");
        LOGE("da sua versao do MCBE e passe o offset via nativeSetOffset().");
        return false;
    }

    GHook h = GlossHook(sym, (void*)hook_addParticle, (void**)&g_orig_addParticle);
    if (!h) {
        LOGE("GlossHook falhou");
        return false;
    }

    LOGI("addParticle hookado com sucesso em %p", sym);
    return true;
}

// ─────────────────────────────────────────────
//  Fallback: offset manual (para versões sem símbolo exportado)
//  O usuário pode passar o offset via nativeSetOffset()
//  após encontrar no IDA/Ghidra.
// ─────────────────────────────────────────────
static uintptr_t g_manualOffset = 0;

static uintptr_t getLibBase() {
    std::ifstream maps("/proc/self/maps");
    std::string   line;
    while (std::getline(maps, line)) {
        if (line.find("libminecraftpe.so") != std::string::npos &&
            line.find("r-xp")             != std::string::npos) {
            uintptr_t base;
            if (sscanf(line.c_str(), "%lx", &base) == 1) return base;
        }
    }
    return 0;
}

static bool hookWithManualOffset(uintptr_t offset) {
    uintptr_t base = getLibBase();
    if (!base) { LOGE("Nao foi possivel obter a base da lib"); return false; }

    void* target = reinterpret_cast<void*>(base + offset);
    LOGI("Tentando hook com offset 0x%lx -> %p", offset, target);

    GlossInit(true);
    GHook h = GlossHook(target, (void*)hook_addParticle, (void**)&g_orig_addParticle);
    if (!h) { LOGE("GlossHook (offset manual) falhou"); return false; }

    LOGI("Hook manual aplicado com sucesso");
    return true;
}

// ─────────────────────────────────────────────
//  Exports JNI
// ─────────────────────────────────────────────
extern "C" {

JNIEXPORT jboolean JNICALL
Java_org_levimc_launcher_core_mods_inbuilt_nativemod_ParticleMod_nativeInit(
        JNIEnv*, jclass)
{
    if (g_initialized) return JNI_TRUE;
    LOGI("Inicializando Particle Control mod...");

    if (!hookParticles()) {
        LOGW("Hook automatico falhou. Use nativeSetOffset() com o offset correto.");
        // Retorna true de qualquer forma — o offset manual ainda pode ser usado
    }

    g_initialized = true;
    LOGI("Particle Control mod pronto");
    return JNI_TRUE;
}

// Permite injetar o offset manualmente (para versões sem símbolo exportado)
JNIEXPORT jboolean JNICALL
Java_org_levimc_launcher_core_mods_inbuilt_nativemod_ParticleMod_nativeSetOffset(
        JNIEnv*, jclass, jlong offset)
{
    if (offset == 0) { LOGE("Offset invalido (0)"); return JNI_FALSE; }
    // Se já temos um hook ativo, não sobrescreve
    if (g_orig_addParticle != nullptr) {
        LOGW("Hook ja ativo, ignorando offset manual");
        return JNI_TRUE;
    }
    return hookWithManualOffset((uintptr_t)offset) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_org_levimc_launcher_core_mods_inbuilt_nativemod_ParticleMod_nativeSetAllDisabled(
        JNIEnv*, jclass, jboolean disabled)
{
    g_allDisabled.store((bool)disabled);
    LOGI("Todas as particulas: %s", disabled ? "DESATIVADAS" : "ATIVAS");
}

JNIEXPORT void JNICALL
Java_org_levimc_launcher_core_mods_inbuilt_nativemod_ParticleMod_nativeSetCategoryDisabled(
        JNIEnv*, jclass, jint category, jboolean disabled)
{
    if (disabled) {
        g_disabledCategories |= (uint32_t)category;
    } else {
        g_disabledCategories &= ~(uint32_t)category;
    }
    LOGI("Categoria 0x%X: %s", category, disabled ? "desativada" : "ativa");
}

JNIEXPORT jint JNICALL
Java_org_levimc_launcher_core_mods_inbuilt_nativemod_ParticleMod_nativeGetDisabledCategories(
        JNIEnv*, jclass)
{
    return (jint)g_disabledCategories.load();
}

JNIEXPORT jboolean JNICALL
Java_org_levimc_launcher_core_mods_inbuilt_nativemod_ParticleMod_nativeIsHookActive(
        JNIEnv*, jclass)
{
    return (g_orig_addParticle != nullptr) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_org_levimc_launcher_core_mods_inbuilt_nativemod_ParticleMod_nativeIsInitialized(
        JNIEnv*, jclass)
{
    return g_initialized ? JNI_TRUE : JNI_FALSE;
}

} // extern "C"
