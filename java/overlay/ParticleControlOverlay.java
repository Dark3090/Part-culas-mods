package org.levimc.launcher.core.mods.inbuilt.overlay;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import org.levimc.launcher.core.mods.inbuilt.model.ModIds;
import org.levimc.launcher.core.mods.inbuilt.nativemod.ParticleMod;

public class ParticleControlOverlay extends BaseOverlayButton {

    private static final String PREFS  = "particle_control_prefs";
    private static final String KEY_ALL = "all_disabled";
    private static final String KEY_CAT = "cat_";

    private static final Object[][] CATEGORIES = {
            {"🌿 Terreno",  ParticleMod.CAT_TERRAIN},
            {"⚔️ Combate",  ParticleMod.CAT_COMBAT},
            {"🌊 Água",     ParticleMod.CAT_WATER},
            {"🔥 Fogo",     ParticleMod.CAT_FIRE},
            {"✨ Magia",    ParticleMod.CAT_MAGIC},
            {"💥 Explosão", ParticleMod.CAT_EXPLOSION},
            {"🌀 Ambiente", ParticleMod.CAT_AMBIENT},
    };

    private final SharedPreferences prefs;

    public ParticleControlOverlay(Activity activity) {
        super(activity);
        prefs = activity.getSharedPreferences(PREFS, Activity.MODE_PRIVATE);
        applyStoredSettings();
    }

    // ── Métodos abstratos obrigatórios ──────────────────────────

    @Override
    protected String getModId() {
        return ModIds.PARTICLE_CONTROL;
    }

    @Override
    protected int getIconResource() {
        // Usa o ícone padrão do launcher (0 = sem ícone customizado)
        return 0;
    }

    @Override
    protected void onButtonClick() {
        showSettingsDialog();
    }

    // ── Dialog de configurações ─────────────────────────────────

    private void showSettingsDialog() {
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(16), dp(24), dp(16));
        root.setBackgroundColor(Color.parseColor("#1E1E2E"));

        // Título
        TextView title = new TextView(activity);
        title.setText("✦  Particle Control");
        title.setTextColor(Color.parseColor("#CDD6F4"));
        title.setTextSize(18f);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        title.setPadding(0, 0, 0, dp(4));
        root.addView(title);

        // Status do hook
        TextView status = new TextView(activity);
        boolean hookActive = ParticleMod.nativeIsHookActive();
        status.setText(hookActive
                ? "✅ Hook ativo"
                : "⚠️ Hook inativo – verifique o offset MCBE");
        status.setTextColor(hookActive
                ? Color.parseColor("#A6E3A1")
                : Color.parseColor("#F38BA8"));
        status.setTextSize(12f);
        status.setPadding(0, 0, 0, dp(12));
        root.addView(status);

        root.addView(divider());

        // Master switch
        root.addView(makeSwitch(
                "🚫 Desativar TODAS as partículas",
                prefs.getBoolean(KEY_ALL, false),
                checked -> {
                    prefs.edit().putBoolean(KEY_ALL, checked).apply();
                    ParticleMod.setAllDisabled(checked);
                }
        ));

        root.addView(divider());

        // Label categorias
        TextView catLabel = new TextView(activity);
        catLabel.setText("Categorias individuais");
        catLabel.setTextColor(Color.parseColor("#89DCEB"));
        catLabel.setTextSize(13f);
        catLabel.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        catLabel.setPadding(0, dp(8), 0, dp(4));
        root.addView(catLabel);

        // Lista de categorias com scroll
        ScrollView scroll = new ScrollView(activity);
        LinearLayout catList = new LinearLayout(activity);
        catList.setOrientation(LinearLayout.VERTICAL);

        for (Object[] cat : CATEGORIES) {
            String label = (String) cat[0];
            int    mask  = (int)    cat[1];
            catList.addView(makeSwitch(
                    label,
                    prefs.getBoolean(KEY_CAT + mask, false),
                    checked -> {
                        prefs.edit().putBoolean(KEY_CAT + mask, checked).apply();
                        ParticleMod.setCategoryDisabled(mask, checked);
                    }
            ));
        }

        scroll.addView(catList);
        root.addView(scroll);

        // Nota de rodapé
        TextView note = new TextView(activity);
        note.setText("💡 Alterações aplicadas em tempo real.");
        note.setTextColor(Color.parseColor("#6C7086"));
        note.setTextSize(11f);
        note.setPadding(0, dp(12), 0, 0);
        root.addView(note);

        new AlertDialog.Builder(activity,
                android.R.style.Theme_DeviceDefault_Dialog_NoActionBar)
                .setView(root)
                .setPositiveButton("Fechar", null)
                .show();
    }

    // ── Helpers ─────────────────────────────────────────────────

    private Switch makeSwitch(String label, boolean checked, SwitchListener l) {
        Switch sw = new Switch(activity);
        sw.setText(label);
        sw.setChecked(checked);
        sw.setTextColor(Color.parseColor("#CDD6F4"));
        sw.setTextSize(14f);
        sw.setPadding(0, dp(10), 0, dp(10));
        sw.setOnCheckedChangeListener((v, c) -> l.onChanged(c));
        return sw;
    }

    private View divider() {
        View d = new View(activity);
        d.setBackgroundColor(Color.parseColor("#313244"));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 1);
        lp.setMargins(0, dp(6), 0, dp(6));
        d.setLayoutParams(lp);
        return d;
    }

    private int dp(int dp) {
        return Math.round(dp * activity.getResources().getDisplayMetrics().density);
    }

    private void applyStoredSettings() {
        ParticleMod.setAllDisabled(prefs.getBoolean(KEY_ALL, false));
        for (Object[] cat : CATEGORIES) {
            int mask = (int) cat[1];
            if (prefs.getBoolean(KEY_CAT + mask, false)) {
                ParticleMod.setCategoryDisabled(mask, true);
            }
        }
    }

    @FunctionalInterface
    private interface SwitchListener {
        void onChanged(boolean checked);
    }
}
