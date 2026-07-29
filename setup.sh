#!/data/data/com.termux/files/usr/bin/bash
set -e

SDK="/data/data/com.termux/files/home/Android-Aarch64/android-sdk"
AAPT2="$SDK/build-tools/37.0.0/aapt2"

echo "Configurando proyecto Android..."

# local.properties
cat > local.properties <<EOF
sdk.dir=$SDK
EOF

# gradle.properties
touch gradle.properties

if grep -q "^android.aapt2FromMavenOverride=" gradle.properties; then
    sed -i "s|^android.aapt2FromMavenOverride=.*|android.aapt2FromMavenOverride=$AAPT2|" gradle.properties
else
    echo "" >> gradle.properties
    echo "android.aapt2FromMavenOverride=$AAPT2" >> gradle.properties
fi

echo
echo "Configuración completada."
echo "SDK:   $SDK"
echo "AAPT2: $AAPT2"
