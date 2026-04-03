package com.corrida99;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CorridaAccessibilityService extends AccessibilityService {

    private static final Pattern VALOR_PATTERN = Pattern.compile("R\\$\\s*(\\d+[,.]\\d+)");
    private static final Pattern KM_PATTERN = Pattern.compile("(\\d+[,.]\\d+)\\s*km", Pattern.CASE_INSENSITIVE);
    private static final Pattern TEMPO_PATTERN = Pattern.compile("(\\d+)\\s*min");
    private static final Pattern AVALIACAO_PATTERN = Pattern.compile("(\\d+[,.]\\d+)\\s*(?:★|⭐|estrela)");
    private static final Pattern AVALIACAO_ALT = Pattern.compile("(?:avalia[çc][aã]o|nota)[:\\s]+(\\d+[,.]\\d+)", Pattern.CASE_INSENSITIVE);

    private SharedPreferences prefs;
    private long ultimaDeteccao = 0;
    private static final long COOLDOWN_MS = 3000;

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        prefs = getSharedPreferences("corrida99", MODE_PRIVATE);
        // Inicia overlay assim que acessibilidade é ativada
        Intent intent = new Intent(this, OverlayService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;

        long agora = System.currentTimeMillis();
        if (agora - ultimaDeteccao < COOLDOWN_MS) return;

        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) return;

        StringBuilder textoTela = new StringBuilder();
        coletarTexto(rootNode, textoTela);
        rootNode.recycle();

        String texto = textoTela.toString();

        if (!pareceOfertaDeCorrida(texto)) return;

        Double valor = extrairValor(texto);
        Double km = extrairKm(texto);
        Integer minutos = extrairMinutos(texto);
        Double avaliacao = extrairAvaliacao(texto);

        if (valor == null || km == null || km <= 0) return;

        double valorPorKm = valor / km;
        double valorPorHora = (minutos != null && minutos > 0) ? (valor / minutos) * 60.0 : 0;
        double avaliacaoFinal = (avaliacao != null) ? avaliacao : 0.0;

        ultimaDeteccao = agora;

        if (MainActivity.instance != null) {
            MainActivity.instance.atualizarCorrida(valorPorKm, valorPorHora, avaliacaoFinal);
        }

        double minKm = Double.parseDouble(prefs.getString("min_km", "2.50").replace(",", "."));
        double minHora = Double.parseDouble(prefs.getString("min_hora", "25.00").replace(",", "."));
        double minAval = Double.parseDouble(prefs.getString("min_avaliacao", "4.5").replace(",", "."));
        boolean boaCorrida = valorPorKm >= minKm && valorPorHora >= minHora && avaliacaoFinal >= minAval;

        Intent intent = new Intent(this, OverlayService.class);
        intent.putExtra("valorKm", valorPorKm);
        intent.putExtra("valorHora", valorPorHora);
        intent.putExtra("avaliacao", avaliacaoFinal);
        intent.putExtra("boaCorrida", boaCorrida);
        startService(intent);
    }

    private boolean pareceOfertaDeCorrida(String texto) {
        String lower = texto.toLowerCase();
        return (lower.contains("km") || lower.contains("quilômetro")) &&
               (lower.contains("r$") || lower.contains("reais")) &&
               (lower.contains("min") || lower.contains("corrida") || lower.contains("aceitar") || lower.contains("recusar"));
    }

    private void coletarTexto(AccessibilityNodeInfo node, StringBuilder sb) {
        if (node == null) return;
        if (node.getText() != null) sb.append(node.getText()).append(" ");
        if (node.getContentDescription() != null) sb.append(node.getContentDescription()).append(" ");
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            coletarTexto(child, sb);
            if (child != null) child.recycle();
        }
    }

    private Double extrairValor(String texto) {
        Matcher m = VALOR_PATTERN.matcher(texto);
        if (m.find()) {
            try { return Double.parseDouble(m.group(1).replace(",", ".")); } catch (Exception e) {}
        }
        return null;
    }

    private Double extrairKm(String texto) {
        Matcher m = KM_PATTERN.matcher(texto);
        if (m.find()) {
            try { return Double.parseDouble(m.group(1).replace(",", ".")); } catch (Exception e) {}
        }
        return null;
    }

    private Integer extrairMinutos(String texto) {
        Matcher m = TEMPO_PATTERN.matcher(texto);
        if (m.find()) {
            try { return Integer.parseInt(m.group(1)); } catch (Exception e) {}
        }
        return null;
    }

    private Double extrairAvaliacao(String texto) {
        Matcher m = AVALIACAO_PATTERN.matcher(texto);
        if (m.find()) {
            try { return Double.parseDouble(m.group(1).replace(",", ".")); } catch (Exception e) {}
        }
        m = AVALIACAO_ALT.matcher(texto);
        if (m.find()) {
            try { return Double.parseDouble(m.group(1).replace(",", ".")); } catch (Exception e) {}
        }
        return null;
    }

    @Override
    public void onInterrupt() {}
}
