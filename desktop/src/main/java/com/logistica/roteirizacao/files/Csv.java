package com.logistica.roteirizacao.files;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Regras de CSV da exportação: separador vírgula e aspas duplas quando o campo tem vírgula,
 * aspas ou quebra de linha (aspas internas viram ""), como no RFC 4180.
 */
final class Csv {

    private Csv() {
    }

    static String escapar(String campo) {
        if (campo == null) {
            return "";
        }
        if (campo.contains(",") || campo.contains("\"") || campo.contains("\n") || campo.contains("\r")) {
            return "\"" + campo.replace("\"", "\"\"") + "\"";
        }
        return campo;
    }

    static String linha(String... campos) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < campos.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(escapar(campos[i]));
        }
        return sb.toString();
    }

    /** Separa uma linha em campos, respeitando aspas. */
    static List<String> separar(String linha) throws IOException {
        List<String> campos = new ArrayList<>();
        StringBuilder atual = new StringBuilder();
        boolean entreAspas = false;
        for (int i = 0; i < linha.length(); i++) {
            char c = linha.charAt(i);
            if (entreAspas) {
                if (c == '"' && i + 1 < linha.length() && linha.charAt(i + 1) == '"') {
                    atual.append('"');
                    i++;
                } else if (c == '"') {
                    entreAspas = false;
                } else {
                    atual.append(c);
                }
            } else if (c == '"') {
                entreAspas = true;
            } else if (c == ',') {
                campos.add(atual.toString());
                atual.setLength(0);
            } else {
                atual.append(c);
            }
        }
        if (entreAspas) {
            throw new IOException("Aspas não fechadas na linha: " + linha);
        }
        campos.add(atual.toString());
        return campos;
    }
}
