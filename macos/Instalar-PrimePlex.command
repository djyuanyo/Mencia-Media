#!/bin/bash
DIR="$(cd "$(dirname "$0")" && pwd)"
echo "=========================================="
echo "🎬 Instalando PrimePlex en tu Mac..."
echo "=========================================="

# Copiar a Aplicaciones
rm -rf "/Applications/PrimePlex.app"
cp -R "$DIR/PrimePlex.app" "/Applications/"

# Quitar restricciones de Gatekeeper si las hubiera
xattr -cr "/Applications/PrimePlex.app" 2>/dev/null || true

echo "✓ ¡PrimePlex instalado con éxito en /Applications!"
echo "Abriendo PrimePlex..."
open "/Applications/PrimePlex.app"
exit 0
