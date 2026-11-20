#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."

# Requer as APIs locais e um dispositivo Android já iniciado.
# Instala com -r e usa uma Activity de teste: mantém os dados e a sessão pessoal.
sdk_local="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}}"
adb_local="$sdk_local/platform-tools/adb"
if [[ ! -x "$adb_local" ]]; then
    echo "Configure ANDROID_HOME com o caminho do SDK Android." >&2
    exit 1
fi
"$adb_local" get-state >/dev/null
"$adb_local" reverse tcp:5041 tcp:5041
"$adb_local" reverse tcp:8000 tcp:8000

./gradlew :app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest :app:lintDebug \
    -Punisync.authUrl=http://127.0.0.1:5041 -Punisync.apiUrl=http://127.0.0.1:8000

codigo_professor="${UNISYNC_CODIGO_PROFESSOR:-}"
if [[ -z "$codigo_professor" ]]; then
    codigo_professor=$(docker exec unisync-stack-api-dotnet-1 dotnet bin/Debug/net10.0/Unisync.Api.dll gerar-codigos-professor 1)
fi
if [[ ! "$codigo_professor" =~ ^[a-fA-F0-9-]{36}$ ]]; then
    echo "Informe um código novo em UNISYNC_CODIGO_PROFESSOR." >&2
    exit 1
fi

"$adb_local" install -r app/build/outputs/apk/debug/app-debug.apk
"$adb_local" install -r -t app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
mkdir -p app/build/reports/academico
resultado=$("$adb_local" shell am instrument -w \
    -e class br.com.unisync.mobile.LembretesUiTest,br.com.unisync.mobile.LembretesRoomTest,br.com.unisync.mobile.LembretesLocalE2ETest,br.com.unisync.mobile.AlertasTest,br.com.unisync.mobile.AcademicoUiTest,br.com.unisync.mobile.AcademicoLocalE2ETest,br.com.unisync.mobile.AutenticacaoUiTest,br.com.unisync.mobile.OportunidadesUiTest,br.com.unisync.mobile.TarefasUiTest,br.com.unisync.mobile.TarefasRoomTest,br.com.unisync.mobile.SessaoSeguraTest \
    -e e2eLembretes true -e e2eAcademico true -e codigoProfessorAcademico "$codigo_professor" \
    br.com.unisync.mobile.test/androidx.test.runner.AndroidJUnitRunner)
printf '%s\n' "$resultado" | tee app/build/reports/academico/instrumentacao.txt
if [[ "$resultado" != *"OK ("* || "$resultado" == *"FAILURES!!!"* || "$resultado" == *"INSTRUMENTATION_FAILED"* ]]; then
    exit 1
fi
for captura in lembrete-formulario-escuro.png turmas-claro.png aviso-formulario-escuro.png turmas-paisagem-fonte-ampliada.png academico-turma-professor.png academico-aviso-aluno.png; do
    "$adb_local" exec-out run-as br.com.unisync.mobile cat "cache/$captura" > "app/build/reports/academico/$captura"
done
"$adb_local" shell am start -n br.com.unisync.mobile/.MainActivity
