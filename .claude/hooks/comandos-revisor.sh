#!/usr/bin/env bash
# Hook PreToolUse del revisor: solo comandos que leen o verifican, nunca que modifican.
# Recibe por stdin el JSON de la herramienta. Salir con 2 bloquea el comando y le explica el motivo.

entrada=$(cat)
comando=$(printf '%s' "$entrada" | sed -nE 's/.*"command"[[:space:]]*:[[:space:]]*"(([^"\\]|\\.)*)".*/\1/p')

# Sin encadenar ni redirigir: así un comando permitido no puede esconder otro
case "$comando" in
  *';'*|*'&'*|*'|'*|*'>'*|*'<'*|*'`'*|*'$('*)
    echo "El revisor no puede encadenar, redirigir ni sustituir comandos: $comando" >&2
    exit 2 ;;
esac

case "$comando" in
  "./gradlew "*|"gradlew "*|"git diff"*|"git status"*|"git log"*|"git show"*|"adb "*) exit 0 ;;
esac

echo "El revisor solo puede ejecutar ./gradlew, git diff/status/log/show y adb (intentó: $comando)." >&2
exit 2
