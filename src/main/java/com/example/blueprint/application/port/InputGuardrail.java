package com.example.blueprint.application.port;

/** PORT : garde-fou appliqué au message utilisateur AVANT tout appel au LLM. */
public interface InputGuardrail {

    Verdict check(String userMessage);

    record Verdict(boolean allowed, String reason) {
        public static Verdict allow() {
            return new Verdict(true, null);
        }

        public static Verdict deny(String reason) {
            return new Verdict(false, reason);
        }
    }
}
