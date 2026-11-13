#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."

sdk_local="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}}"
adb_local="$sdk_local/platform-tools/adb"
if [[ ! -x "$adb_local" ]]; then
    echo "Configure ANDROID_HOME com o caminho do SDK Android." >&2
    exit 1
fi

# Requer um emulador/celular conectado e as APIs locais nas portas 5041 e 8000.
# Para mais de um dispositivo, defina ANDROID_SERIAL antes de executar.
"$adb_local" reverse tcp:5041 tcp:5041
"$adb_local" reverse tcp:8000 tcp:8000
argumentos_e2e=()
if [[ "${UNISYNC_E2E_LOCAL:-false}" == "true" ]]; then
    # Cria conta técnica persistente no backend local; as tarefas são removidas.
    argumentos_e2e+=(-Pandroid.testInstrumentationRunnerArguments.e2eLocal=true)
fi
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:connectedDebugAndroidTest :app:lintDebug \
    -Punisync.authUrl=http://127.0.0.1:5041 \
    -Punisync.apiUrl=http://127.0.0.1:8000 \
    "${argumentos_e2e[@]}" "$@"
