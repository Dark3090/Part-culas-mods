package org.levimc.launcher.core.mods.inbuilt.overlay;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import org.levimc.launcher.core.mods.inbuilt.nativemod.ParticleMod;

/**
 * Overlay flutuante para o Particle Control mod.
 *
 * – Toque curto  → abre o painel de configurações estilo Sodium
 * – O painel tem master switch + toggles por categoria
 * – Persiste as configs via SharedPreferences
 */
public class ParticleControlOverlay extends BaseOverlayButton {

    private static final String PREFS = "particle_control_prefs";
    private static final String KEY_ALL_DISABLED = "all_disabled";
    private static final String KEY_CAT_PREFIX   = "cat_";

    // Par (label, bitmask) para cada categoria
    private static final Object[][] CATEGORIES = {
            {"🌿 Terreno",   ParticleMod.CAT_TERRAIN},
            {"⚔️ Combate",   ParticleMod.CAT_COMBAT},
            {"🌊 Água",      ParticleMod.CAT_WATER},
            {"🔥 Fogo",      ParticleMod.CAT_FIRE},
            {"✨ Magia",     ParticleMod.CAT_MAGIC},
            {"💥 Explosão",  ParticleMod.CAT_EXPLOSION},
            {"🌀 Ambiente",  ParticleMod.CAT_AMBIENT},
    };

    private SharedPreferences prefs;

    public ParticleControlOverlay(Context context) {
        super(context);
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        applyStoredSettings();
        setOnClickListener(v -> showSettingsDialog());
        setContentDescription("Particle Control – toque para configurar");
    }

    // ── Ícone do botão flutuante ────────────────────────────────
    @Override
    protected View createButtonView(Context context) {
        TextView icon = new TextView(context);
        icon.setText("✦");
        icon.setTextSize(22f);
        icon.setTextColor(Color.WHITE);
        icon.setGravity(Gravity.CENTER);

        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(Color.parseColor("#CC2D2D72")); // roxo semitransparente
        bg.setStroke(2, Color.parseColor("#8080FF"));
        icon.setBackground(bg);

        int size = dpToPx(context, 48);
        icon.setLayoutParams(new ViewGroup.LayoutParams(size, size));
        return icon;
    }

    // ── Dialog estilo Sodium ────────────────────────────────────
    private void showSettingsDialog() {
        Context ctx = getContext();

        // Root do dialog
        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(16), dp(24), dp(16));
        root.setBackgroundColor(Color.parseColor("#1E1E2E")); // fundo escuro

        // Título
        TextView title = new TextView(ctx);
        title.setText("✦  Particle Control");
        title.setTextColor(Color.parseColor("#CDD6F4"));
        title.setTextSize(18f);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        title.setPadding(0, 0, 0, dp(4));
        root.addView(title);

        // Subtítulo
        TextView subtitle = new TextView(ctx);
        subtitle.setText(ParticleMod.nativeIsHookActive()
                ? "✅ Hook ativo"
                : "⚠️ Hook inativo – verifique o offset MCBE");
        subtitle.setTextColor(ParticleMod.nativeIsHookActive()
                ? Color.parseColor("#A6E3A1")
                : Color.parseColor("#F38BA8"));
        subtitle.setTextSize(12f);
        subtitle.setPadding(0, 0, 0, dp(16));
        root.addView(subtitle);

        // Separador
        root.addView(makeDivider(ctx));

        // Master switch
        boolean allDisabled = prefs.getBoolean(KEY_ALL_DISABLED, false);
        Switch masterSwitch = makeSwitch(ctx,
                "🚫  Desativar TODAS as partículas",
                allDisabled,
                checked -> {
                    prefs.edit().putBoolean(KEY_ALL_DISABLED, checked).apply();
                    ParticleMod.setAllDisabled(checked);
                });
        root.addView(masterSwitch);
        root.addView(makeDivider(ctx));

        // Scroll com categorias individuais
        ScrollView scroll = new ScrollView(ctx);
        LinearLayout catList = new LinearLayout(ctx);
        catList.setOrientation(LinearLayout.VERTICAL);

        TextView catLabel = new TextView(ctx);
        catLabel.setText("Categorias individuais");
        catLabel.setTextColor(Color.parseColor("#89DCEB"));
        catLabel.setTextSize(13f);
        catLabel.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        catLabel.setPadding(0, dp(8), 0, dp(8));
        catList.addView(catLabel);

        for (Object[] cat : CATEGORIES) {
            String label = (String) cat[0];
            int    mask  = (int)    cat[1];

            boolean catDisabled = prefs.getBoolean(KEY_CAT_PREFIX + mask, false);

            Switch sw = makeSwitch(ctx, label, catDisabled, checked -> {
                prefs.edit().putBoolean(KEY_CAT_PREFIX + mask, checked).apply();
                ParticleMod.setCategoryDisabled(mask, checked);
            });
            catList.addView(sw);
        }

        scroll.addView(catList);
        root.addView(scroll);

        // Nota de rodapé
        TextView note = new TextView(ctx);
        note.setText("💡 As alterações são aplicadas em tempo real.");
        note.setTextColor(Color.parseColor("#6C7086"));
        note.setTextSize(11f);
        note.setPadding(0, dp(12), 0, 0);
        root.addView(note);

        // Monta o dialog
        new AlertDialog.Builder(ctx, android.R.style.Theme_DeviceDefault_Dialog_NoActionBar)
                .setView(root)
                .setPositiveButton("Fechar", null)
                .show()
                .getWindow()
                .setBackgroundDrawableResource(android.R.color.transparent);
    }

    // ── Helpers de UI ───────────────────────────────────────────
    private Switch makeSwitch(Context ctx, String label, boolean checked,
                              SwitchListener listener) {
        Switch sw = new Switch(ctx);
        sw.setText(label);
        sw.setChecked(checked);
        sw.setTextColor(Color.parseColor("#CDD6F4"));
        sw.setTextSize(14f);
        sw.setPadding(0, dp(10), 0, dp(10));
        sw.setOnCheckedChangeListener((v, isChecked) -> listener.onChanged(isChecked));
        return sw;
    }

    private View makeDivider(Context ctx) {
        View d = new View(ctx);
        d.setBackgroundColor(Color.parseColor("#313244"));
        d.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 1));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 1);
        lp.setMargins(0, dp(6), 0, dp(6));
        d.setLayoutParams(lp);
        return d;
    }

    private int dp(int dp) {
        return dpToPx(getContext(), dp);
    }

    private static int dpToPx(Context ctx, int dp) {
        float density = ctx.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    // ── Aplica configurações salvas ao iniciar ──────────────────
    private void applyStoredSettings() {
        boolean allDisabled = prefs.getBoolean(KEY_ALL_DISABLED, false);
        ParticleMod.setAllDisabled(allDisabled);

        for (Object[] cat : CATEGORIES) {
            int mask = (int) cat[1];
            boolean disabled = prefs.getBoolean(KEY_CAT_PREFIX + mask, false);
            if (disabled) ParticleMod.setCategoryDisabled(mask, true);
        }
    }

    @FunctionalInterface
    private interface SwitchListener {
        void onChanged(boolean checked);
    }
}
