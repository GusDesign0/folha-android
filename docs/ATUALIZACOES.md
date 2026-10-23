# Atualização do Folha no Android

## Fluxo a partir da versão 0.8.4

1. Em **Atualizar app**, o Folha consulta a versão publicada no GitHub.
2. Se existir uma versão nova, oferece **Baixar e instalar**, informando o tamanho.
3. O download é conferido por tamanho, SHA-256, nome do pacote, número de versão e assinatura da instalação atual.
4. O instalador do Android abre diretamente. A confirmação intermediária **Atualização pronta → Instalar** foi removida.
5. Se a instalação for cancelada e você tentar novamente na mesma sessão do Folha, o arquivo verificado é reutilizado.

Enquanto prepara ou apresenta o instalador, toques adicionais são ignorados. Retornar do instalador ou da permissão libera uma nova tentativa. Uma versão que já esteja instalada não é oferecida novamente como pendente. Quando o app já está atualizado, mostra uma mensagem breve, sem outra confirmação.

Na primeira instalação pelo próprio Folha, o Android pode pedir permissão para instalar apps desta fonte. Essa tela pertence ao sistema. Downloads e verificações continuam em segundo plano, fora dos eventos da caneta.

## Google Play Protect

O Folha é distribuído como APK fora da Google Play Store. O Google Play Protect pode solicitar uma análise de uma nova versão, mesmo que a versão anterior já tenha sido verificada. O Folha não confirma essa tela pelo usuário, não desliga a proteção e não altera as configurações do Google.

A [documentação do Google](https://support.google.com/googleplay/answer/2812853?hl=pt-BR) explica a análise de apps externos e a possibilidade de solicitar cada nova versão. A assinatura e o SHA-256 conferidos pelo Folha verificam a origem e a integridade do arquivo; a análise do Play Protect é uma etapa própria do Google.

Esta alteração remove uma confirmação do Folha. Ela não promete eliminar a confirmação do Android ou as verificações do Google/Samsung.

## Validação e manutenção

O código fica em `src/br/com/folha/notes/AppUpdater.java`, com download/validação em `UpdateChecker.java` e o URI de leitura em `UpdateProvider.java`. Não há verificação periódica de atualização nem instalação silenciosa.

`test-device.ps1 -UpdaterOnly` executa o teste focado em um emulador dedicado: download real do GitHub, integridade, assinatura, versão, cancelamento, URI de leitura, despacho único ao instalador, reaproveitamento após cancelar e descarte de versão pendente já instalada. O script autoriza o app-op `REQUEST_INSTALL_PACKAGES` no dispositivo de teste.

O APK de teste `999.0.0` é gerado localmente com a mesma chave para testar a validação de uma versão futura. O teste intercepta sua abertura com `Instrumentation.ActivityMonitor`: **ele nunca é instalado**. O APK de teste e a chave não acompanham a publicação; somente o manifesto da fixture e o script de teste ficam no repositório.
