#!/usr/bin/env bash
# Build the release files, name them, write a checksum next to each, and optionally publish.
#
#   scripts/release.sh            build into dist/ (signed Android APKs need the keystore variables below)
#   scripts/release.sh --publish  also create the GitHub release for the version in composeApp/build.gradle.kts
#
# Signing: NAMAA_KEYSTORE (path), NAMAA_KEYSTORE_PASSWORD, optional NAMAA_KEY_ALIAS.
# Publishing needs the GitHub CLI, logged in with a token that can write to this repository.
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."

# Android APKs only. No desktop packages are built or published.
VERSION="$(sed -n 's/^val appVersion = "\(.*\)".*/\1/p' composeApp/build.gradle.kts | head -1)"
: "${VERSION:?could not read appVersion}"
APP_VERSION="$(sed -n 's/.*const val APP_VERSION = "\(.*\)".*/\1/p' composeApp/src/commonMain/kotlin/dev/namaa/app/NamaaApp.kt | head -1)"
NOTES="$(awk -v v="## $VERSION" '$0==v{p=1;next} /^## /{p=0} p' CHANGELOG.md | sed '/^[[:space:]]*$/d')"
[ "$APP_VERSION" = "$VERSION" ] || { echo "error: APP_VERSION ($APP_VERSION) differs from appVersion ($VERSION)" >&2; exit 1; }
if [ "${1:-}" = "--publish" ]; then
  [ -n "${NAMAA_KEYSTORE:-}" ] || { echo "error: refusing to publish unsigned APKs, set NAMAA_KEYSTORE" >&2; exit 1; }
  [ -n "$NOTES" ] || { echo "error: CHANGELOG.md has no '## $VERSION' section with text" >&2; exit 1; }
fi
[ -n "${NAMAA_KEYSTORE:-}" ] || echo "note: NAMAA_KEYSTORE is not set, the APKs will be unsigned" >&2

./gradlew --no-daemon :shared:desktopTest :composeApp:assembleRelease

OUT="dist/$VERSION"
rm -rf "$OUT"; mkdir -p "$OUT"
for abi in arm64-v8a armeabi-v7a x86_64; do
  cp "composeApp/build/outputs/apk/release/composeApp-$abi-release.apk" "$OUT/Namaa_${VERSION}_Android_$abi.apk" 2>/dev/null || true
done

# One checksum file per release file, named after it, so `sha256sum -c <file>.sha256` works.
(cd "$OUT" && for f in Namaa_*; do case "$f" in *.sha256) ;; *) sha256sum -- "$f" > "$f.sha256" ;; esac; done)
ls -l "$OUT"

if [ "${1:-}" = "--publish" ]; then
  gh release create "v$VERSION" "$OUT"/* --title "Namaa $VERSION" --notes "$NOTES"
fi
