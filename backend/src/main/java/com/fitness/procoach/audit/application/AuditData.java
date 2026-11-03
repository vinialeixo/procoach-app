package com.fitness.procoach.audit.application;

/**
 * Monta o payload JSON (texto) de um registro de auditoria a partir de pares
 * chave/valor, com escape correto. Não coloque dados pessoais sensíveis aqui —
 * prefira identificadores, estados e motivos.
 */
public final class AuditData {

	private AuditData() {
	}

	/**
	 * {@code of("from", "ACTIVE", "to", "PAUSED")} -> {@code {"from":"ACTIVE","to":"PAUSED"}}.
	 * Valores {@code null} viram JSON {@code null}; demais valores usam {@code toString()}.
	 */
	public static String of(Object... keyValues) {
		if (keyValues.length % 2 != 0) {
			throw new IllegalArgumentException("keyValues must come in key/value pairs");
		}
		StringBuilder json = new StringBuilder("{");
		for (int i = 0; i < keyValues.length; i += 2) {
			if (i > 0) {
				json.append(',');
			}
			quote(json, String.valueOf(keyValues[i]));
			json.append(':');
			Object value = keyValues[i + 1];
			if (value == null) {
				json.append("null");
			}
			else if (value instanceof Number || value instanceof Boolean) {
				json.append(value);
			}
			else {
				quote(json, value.toString());
			}
		}
		return json.append('}').toString();
	}

	private static void quote(StringBuilder out, String text) {
		out.append('"');
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			switch (c) {
				case '"' -> out.append("\\\"");
				case '\\' -> out.append("\\\\");
				case '\n' -> out.append("\\n");
				case '\r' -> out.append("\\r");
				case '\t' -> out.append("\\t");
				default -> {
					if (c < 0x20) {
						out.append(String.format("\\u%04x", (int) c));
					}
					else {
						out.append(c);
					}
				}
			}
		}
		out.append('"');
	}
}
