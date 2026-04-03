package com.corrida99;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CorridaAccessibilityService extends AccessibilityService {

    // Padrões para capturar dados da 99
    private static final Pattern VALOR_PATTERN = Pattern.compile("R\\$\\s*(\\d+[,.]\\d+)");
    private static final Pattern KM_PATTERN = Pattern.compile("(\\d+[,.]\\d+)\\s*km", Pattern.CASE_INSENSITIVE);
    private static final Pattern TEMPO_PATTERN = Pattern.compile("(\\d+)\\s*min");
    private static final Pattern AVALIACAO_PATTERN = Pattern.compile("(\\d+[,.]\\d+)\\s*(?:★|⭐|estrela)");
    private static final Pattern AVALIACAO_ALT = Pattern.compile("(?:avalia[çc][aã]o|nota)[:\\s]+(\\d+[,.]\\d+)", Pattern.CASE_INSENSITIVE);

    private SharedPreferences prefs;
    private long ultimaDeteccao = 0;
    private static final long COOLDOWN_MS = 3000; // evita spam

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        prefs = getSharedPreferences("corrida99", MODE_PRIVATE);
        // Inicia o overlay
        Intent intent = new Intent(this, OverlayService.class);
        startService(intent);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;

        long agora = System.currentTimeMillis();
        if (agora - ultimaDeteccao < COOLDOWN_MS) return;

        // Coleta todo o texto visível na tela
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) return;

        StringBuilder textoTela = new StringBuilder();
        coletarTexto(rootNode, textoTela);
        rootNode.recycle();

        String texto = textoTela.toString();

        // Verifica se parece uma tela de oferta de corrida
        if (!pareceOfertaDeCorrida(texto)) return;

        // Extrai os dados
        Double valor = extrairValor(texto);
        Double km = extrairKm(texto);
        Integer minutos = extrairMinutos(texto);
        Double avaliacao = extrairAvaliacao(texto);

        if (valor == null || km == null || km <= 0) return;

        // Calcula métricas
        double valorPorKm = valor / km;
        double valorPorHora = (minutos != null && minutos > 0)
            ? (valor / minutos) * 60.0
            : 0;

        double avaliacaoFinal = (avaliacao != null) ? avaliacao : 0.0;

        ultimaDeteccao = agora;

        // Atualiza UI
        if (MainActivity.instance != null) {
            MainActivity.instance.atualizarCorrida(valorPorKm, valorPorHora, avaliacaoFinal);
        }

        // Atualiza overlay
        Intent intent = new Intent(this, OverlayService.class);
        intent.putExtra("valorKm", valorPorKm);
        intent.putExtra("valorHora", valorPorHora);
        intent.putExtra("avaliacao", avaliacaoFinal);

        // Limites configurados pelo usuário
        double minKm = Double.parseDouble(prefs.getString("min_km", "2.50").replace(",", "."));
        double minHora = Double.parseDouble(prefs.getString("min_hora", "25.00").replace(",", "."));
        double minAval = Double.parseDouble(prefs.getString("min_avaliacao", "4.5").replace(",", "."));
        boolean boaCorrida = valorPorKm >= minKm && valorPorHora >= minHora && avaliacaoFinal >= minAval;
        intent.putExtra("boaCorrida", boaCorrida);

        startService(intent);
    }

    private boolean pareceOfertaDeCorrida(String texto) {
        // Palavras-chave comuns nas telas de oferta da 99
        String lower = texto.toLowerCase();
        return (lower.contains("km") || lower.contains("quilômetro")) &&
               (lower.contains("r$") || lower.contains("reais")) &&
               (lower.contains("min") || lower.contains("corrida") || lower.contains("aceitar") || lower.contains("recusar"));
    }

    private void coletarTexto(AccessibilityNodeInfo node, StringBuilder sb) {
        if (node == null) return;
        if (node.getText() != null) {
            sb.append(node.getText()).append(" ");
        }
        if (node.getContentDescription() != null) {
            sb.append(node.getContentDescription()).append(" ");
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            coletarTexto(child, sb);
            if (child != null) child.recycle();
        }
    }

    private Double extrairValor(String texto) {
        Matcher m = VALOR_PATTERN.matcher(texto);
        if (m.find()) {
            try {
                return Double.parseDouble(m.group(1).replace(",", "."));
            } catch (Exception e) { /* ignora */ }
        }
        return null;
    }

    private Double extrairKm(String texto) {
        Matcher m = KM_PATTERN.matcher(texto);
        if (m.find()) {
            try {
                return Double.parseDouble(m.group(1).replace(",", "."));
            } catch (Exception e) { /* ignora */ }
        }
        return null;
    }

    private Integer extrairMinutos(String texto) {
        Matcher m = TEMPO_PATTERN.matcher(texto);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (Exception e) { /* ignora */ }
        }
        return null;
    }

    private Double extrairAvaliacao(String texto) {
        Matcher m = AVALIACAO_PATTERN.matcher(texto);
        if (m.find()) {
            try {
                return Double.parseDouble(m.group(1).replace(",", "."));
            } catch (Exception e) { /* ignora */ }
        }
        m = AVALIACAO_ALT.matcher(texto);
        if (m.find()) {
            try {
                return Double.parseDouble(m.group(1).replace(",", "."));
            } catch (Exception e) { /* ignora */ }
        }
        return null;
    }

    @Override
    public void onInterrupt() { }
}
