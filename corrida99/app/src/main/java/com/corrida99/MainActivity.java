package com.corrida99;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private TextView tvStatus, tvOverlayStatus;
    private TextView tvKmValor, tvHoraValor, tvAvaliacaoValor, tvVeredicto;
    private EditText etMinKm, etMinHora, etMinAvaliacao;
    private SharedPreferences prefs;

    public static MainActivity instance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        instance = this;

        prefs = getSharedPreferences("corrida99", MODE_PRIVATE);

        tvStatus = findViewById(R.id.tvStatus);
        tvOverlayStatus = findViewById(R.id.tvOverlayStatus);
        tvKmValor = findViewById(R.id.tvKmValor);
        tvHoraValor = findViewById(R.id.tvHoraValor);
        tvAvaliacaoValor = findViewById(R.id.tvAvaliacaoValor);
        tvVeredicto = findViewById(R.id.tvVeredicto);
        etMinKm = findViewById(R.id.etMinKm);
        etMinHora = findViewById(R.id.etMinHora);
        etMinAvaliacao = findViewById(R.id.etMinAvaliacao);

        // Carrega configurações salvas
        etMinKm.setText(prefs.getString("min_km", "2.50"));
        etMinHora.setText(prefs.getString("min_hora", "25.00"));
        etMinAvaliacao.setText(prefs.getString("min_avaliacao", "4.5"));

        Button btnAcessibilidade = findViewById(R.id.btnAcessibilidade);
        Button btnOverlay = findViewById(R.id.btnOverlay);
        Button btnSalvar = findViewById(R.id.btnSalvar);

        btnAcessibilidade.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
            Toast.makeText(this, "Procure 'Copiloto 99' e ative", Toast.LENGTH_LONG).show();
        });

        btnOverlay.setOnClickListener(v -> {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            } else {
                Toast.makeText(this, "Overlay já está permitido!", Toast.LENGTH_SHORT).show();
            }
        });

        btnSalvar.setOnClickListener(v -> {
            prefs.edit()
                .putString("min_km", etMinKm.getText().toString())
                .putString("min_hora", etMinHora.getText().toString())
                .putString("min_avaliacao", etMinAvaliacao.getText().toString())
                .apply();
            Toast.makeText(this, "Configurações salvas!", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        atualizarStatus();
    }

    private void atualizarStatus() {
        boolean acessibilidadeAtiva = isAccessibilityEnabled();
        boolean overlayPermitido = Settings.canDrawOverlays(this);

        if (acessibilidadeAtiva) {
            tvStatus.setText("🟢 Acessibilidade ativada");
            tvStatus.setTextColor(0xFF00d4aa);
        } else {
            tvStatus.setText("🔴 Acessibilidade desativada — toque no botão abaixo");
            tvStatus.setTextColor(0xFFff6b6b);
        }

        if (overlayPermitido) {
            tvOverlayStatus.setText("🟢 Overlay (bolinha flutuante) permitido");
            tvOverlayStatus.setTextColor(0xFF00d4aa);
        } else {
            tvOverlayStatus.setText("🔴 Overlay não permitido — toque no botão abaixo");
            tvOverlayStatus.setTextColor(0xFFff6b6b);
        }
    }

    private boolean isAccessibilityEnabled() {
        try {
            int enabled = Settings.Secure.getInt(
                getContentResolver(),
                Settings.Secure.ACCESSIBILITY_ENABLED);
            if (enabled == 1) {
                String services = Settings.Secure.getString(
                    getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
                return services != null && services.contains(getPackageName());
            }
        } catch (Exception e) {
            // ignora
        }
        return false;
    }

    // Chamado pelo serviço de acessibilidade quando detecta uma corrida
    public void atualizarCorrida(double valorKm, double valorHora, double avaliacao) {
        runOnUiThread(() -> {
            String kmStr = String.format("R$%.2f", valorKm);
            String horaStr = String.format("R$%.0f", valorHora);
            String avalStr = String.format("%.1f", avaliacao);

            tvKmValor.setText(kmStr);
            tvHoraValor.setText(horaStr);
            tvAvaliacaoValor.setText(avalStr + " ⭐");

            double minKm = Double.parseDouble(etMinKm.getText().toString().replace(",", "."));
            double minHora = Double.parseDouble(etMinHora.getText().toString().replace(",", "."));
            double minAval = Double.parseDouble(etMinAvaliacao.getText().toString().replace(",", "."));

            boolean boaCorrida = valorKm >= minKm && valorHora >= minHora && avaliacao >= minAval;

            if (boaCorrida) {
                tvVeredicto.setText("✅ CORRIDA BOA — Vale a pena aceitar!");
                tvVeredicto.setTextColor(0xFF00d4aa);
            } else {
                tvVeredicto.setText("❌ CORRIDA FRACA — Considere recusar");
                tvVeredicto.setTextColor(0xFFff6b6b);
            }
        });
    }
}
