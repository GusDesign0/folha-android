# Compilar o Folha 0.8.0

## Android

- JDK 21, Gradle 8.13, Android SDK Platform 36 e Build Tools 36.0.0.
- As pastas padrão do SDK devem ser `platforms/android-36` e `build-tools/36.0.0`.
- `fetch-deps.ps1` baixa PDFBox/Bouncy Castle de Maven Central e verifica seus SHA-256.
- O Gradle resolve Google Identity Services `play-services-auth:22.0.0` e `com.google.mlkit:digital-ink-recognition:19.0.0` pelos repositórios oficiais Google/Maven Central.
- Configure a chave de assinatura em `build.gradle`. A chave usada nos APKs distribuídos é privada e não acompanha o código. Outra chave não atualiza uma instalação existente.
- O script `build.ps1 -ToolchainRoot CAMINHO_DO_SDK -OutputApk DESTINO.apk` espera JDK e Gradle nas subpastas descritas no script. Para um SDK/JDK instalados separadamente, configure `local.properties`, `JAVA_HOME`, a assinatura e execute `gradle assembleRelease` na raiz.
- APK público: `build/outputs/apk/release/Folha-arm64-v8a-release.apk`. A variante `Folha-x86_64-release.apk` usa o mesmo Java/recursos para QA no emulador; não acompanha o instalador do Samsung. O Gradle gera as duas com a mesma versão/assinatura.
- Testes puros: `test-core.ps1`. Integração Android: `test-device.ps1 -Device emulator-5554` após instalar o APK. `-RequireLegacy` exige que a biblioteca de QA das versões anteriores exista no emulador.
- `tests/SyncTest.java` testa o protocolo sem Android/Google usando JSON-java 20250517 em um diretório de testes separado. A classe Documents de teste não entra no app.

O Android usa Google AuthorizationClient; o cadastro OAuth exige o pacote `br.com.folha.notes` e o SHA-1 da chave de distribuição. Android e Windows devem pertencer ao mesmo projeto Google Cloud, com a Drive API ativada. Não é necessário incluir segredos OAuth do desktop no APK.

## Windows

O código fica em `desktop/`.

1. Em Windows x64 com .NET Framework 4.8 e Node 24, execute `node desktop/fetch-deps.cjs`. O script baixa versões fixas do WebView2, PDF.js, pdf-lib e fflate; as dependências npm têm integridade SHA-512 conferida. Consulte `desktop/dependencies.lock.json` para versões e hashes.
2. Crie um cliente OAuth do tipo **App para computador** no mesmo projeto do Android. Salve seu JSON em `desktop/google-client.json`, usando o exemplo. Esse arquivo é ignorado pelo Git. O cliente instalado é público por definição; a autorização de cada usuário é armazenada separadamente, via DPAPI, e nunca faz parte do instalador.
3. Execute `desktop/build.ps1`. Usa o compilador C# do .NET Framework e as bibliotecas oficiais WebView2.
4. `Folha.exe --self-test` executa testes de armazenamento, backup e sincronização sem abrir janelas. O resultado fica em `test-results.txt` na pasta do executável.

Nenhum token de usuário, biblioteca pessoal, chave de assinatura ou credencial do navegador deve ser incluído em código, instaladores ou publicações.

O teste de integração baixa o modelo japonês no perfil isolado do emulador e desliga/reabilita Wi-Fi/dados móveis para conferir reconhecimento offline. Use um emulador dedicado. Testes JavaScript: `npm install && npm test`. `npm run test:ui` usa navegador headless, sem perfil pessoal; requer Edge no Windows ou Chromium instalado pelo Playwright em outros sistemas.
