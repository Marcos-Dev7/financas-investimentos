package com.marcosdev7.usuarios.validation;

import java.util.regex.Pattern;

public final class ValidadorSenha {

    private static final Pattern MAIUSCULA = Pattern.compile("[A-Z]");
    private static final Pattern MINUSCULA = Pattern.compile("[a-z]");
    private static final Pattern NUMERO = Pattern.compile("[0-9]");
    private static final Pattern ESPECIAL = Pattern.compile("[!@#$%^&*(),.?\":{}|<>_\\-+=/\\\\\\[\\];'~`]");

    private ValidadorSenha() {
    }

    public static void validarForcaEConfirmacao(String senha, String confirmacaoSenha) {
        if (senha == null || senha.length() < 8 || senha.length() > 100) {
            throw new IllegalArgumentException("A senha deve ter entre 8 e 100 caracteres");
        }
        if (!MAIUSCULA.matcher(senha).find()) {
            throw new IllegalArgumentException("A senha deve conter pelo menos uma letra maiúscula");
        }
        if (!MINUSCULA.matcher(senha).find()) {
            throw new IllegalArgumentException("A senha deve conter pelo menos uma letra minúscula");
        }
        if (!NUMERO.matcher(senha).find()) {
            throw new IllegalArgumentException("A senha deve conter pelo menos um número");
        }
        if (!ESPECIAL.matcher(senha).find()) {
            throw new IllegalArgumentException("A senha deve conter pelo menos um caractere especial (ex: @, #, $, !, %)");
        }
        if (confirmacaoSenha == null || !senha.equals(confirmacaoSenha)) {
            throw new IllegalArgumentException("A confirmação de senha não coincide com a senha informada");
        }
    }
}
