package com.corrida99;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import androidx.core.app.NotificationCompat;

public class OverlayService extends Service {

    private WindowManager windowManager;
    private View overlayView;
    private WindowManager.LayoutParams params;
    private boolean overlayVisivel = false;

    @Override
    public void onCreate() {
        super.onCreate();
        criarNotificacao();
        criarOverlay();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.hasExtra("valorKm")) {
            double valorKm = intent.getDoubleExtra("valorKm", 0);
            double valorHora = intent.getDoubleExtra("valorHora", 0);
            double avaliacao = intent.getDoubleExtra("avaliacao", 0);
            boolean boaCorrida = intent.getBooleanExtra("boaCorrida", false);
            atualizarOverlay(valorKm, valorHora, avaliacao, boaCorrida);
        }
        return START_STICKY;
    }

    private void criarNotificacao() {
        String channelId = "corrida99_channel";
        NotificationChannel channel = new NotificationChannel(
            channelId, "Copiloto 99", NotificationManager.IMPORTANCE_LOW);
        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.createNotificationChannel(channel);

        Notification notification = new NotificationCompat.Builder(this, channelId)
            .setContentTitle("Copiloto 99 ativo")
            .setContentText("Monitorando corridas da 99...")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .build();

        startForeground(1, notification);
    }

    private void criarOverlay() {
        if (!android.provider.Settings.canDrawOverlays(this)) return;

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_corrida, null);

        params = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 0;
        params.y = 100;

        // Permite arrastar o overlay
        overlayView.setOnTouchListener(new View.OnTouchListener() {
            private int inicialX, inicialY;
            private float inicialTouchX, inicialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        inicialX = params.x;
                        inicialY = params.y;
                        inicialTouchX = event.getRawX();
                        inicialTouchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        params.x = inicialX + (int)(event.getRawX() - inicialTouchX);
                        params.y = inicialY + (int)(event.getRawY() - inicialTouchY);
                        windowManager.updateViewLayout(overlayView, params);
                        return true;
                }
                return false;
            }
        });

        // Botão fechar
        TextView tvFechar = overlayView.findViewById(R.id.tvOverlayFechar);
        tvFechar.setOnClickListener(v -> {
            overlayView.setVisibility(View.GONE);
            overlayVisivel = false;
        });

        windowManager.addView(overlayView, params);
        overlayVisivel = true;
    }

    private void atualizarOverlay(double valorKm, double valorHora, double avaliacao, boolean boaCorrida) {
        if (overlayView == null) return;

        overlayView.post(() -> {
            TextView tvKm = overlayView.findViewById(R.id.tvOverlayKm);
            TextView tvHora = overlayView.findViewById(R.id.tvOverlayHora);
            TextView tvAval = overlayView.findViewById(R.id.tvOverlayAvaliacao);
            TextView tvVeredicto = overlayView.findViewById(R.id.tvOverlayVeredicto);

            tvKm.setText(String.format("R$%.2f", valorKm));
            tvHora.setText(String.format("R$%.0f", valorHora));

            if (avaliacao > 0) {
                tvAval.setText(String.format("%.1f⭐", avaliacao));
            } else {
                tvAval.setText("—");
            }

            if (boaCorrida) {
                tvVeredicto.setText("✅ BOA CORRIDA!");
                tvVeredicto.setBackgroundColor(0xFF004d3d);
                tvVeredicto.setTextColor(0xFF00d4aa);
            } else {
                tvVeredicto.setText("❌ CORRIDA FRACA");
                tvVeredicto.setBackgroundColor(0xFF4d0000);
                tvVeredicto.setTextColor(0xFFff6b6b);
            }

            overlayView.setVisibility(View.VISIBLE);
            overlayVisivel = true;
        });
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (overlayView != null && windowManager != null) {
            windowManager.removeView(overlayView);
        }
    }
}
