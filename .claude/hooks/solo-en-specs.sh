#!/usr/bin/env bash
# Hook PreToolUse del planificador: solo puede crear o editar archivos dentro de specs/.
# Recibe por stdin el JSON de la herramienta. Salir con 2 bloquea la acción y le explica el motivo.

entrada=$(cat)
# Primero se pasan las barras de Windows (\ o \\) a /; una ruta nunca contiene comillas
ruta=$(printf '%s' "$entrada" | tr '\\' '/' | sed -nE 's#.*"file_path"[[:space:]]*:[[:space:]]*"([^"]*)".*#\1#p' | sed 's#//*#/#g')

case "$ruta" in
  */specs/*|specs/*) exit 0 ;;
esac

echo "El planificador solo puede escribir dentro de specs/ (intentó: ${ruta:-ruta desconocida})." >&2
exit 2
