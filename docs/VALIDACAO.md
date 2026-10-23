# Validação 0.8.4

23/10/2026. Correção do fluxo de atualização Android.

- Build release/R8 e lintRelease: 0 erros, 34 avisos já existentes. Mesma chave de assinatura, atualização no emulador sobre 0.8.3 sem desinstalar.
- Teste Android focado: download real do GitHub com SHA-256; pacote/versão/assinatura; arquivo truncado/corrompido/cancelado; URI somente leitura passaram.
- APK de versão futura, assinado com a mesma chave, foi validado e sua abertura interceptada por Instrumentation.ActivityMonitor. Nunca foi instalado. Dois toques despacharam apenas uma abertura; não apareceu o diálogo redundante de confirmação do Folha.
- Cancelar liberou a nova tentativa sem novo download. Uma versão pendente já instalada foi descartada. Consulta sem versão nova mostrou mensagem não bloqueante. Intent solicita o retorno do resultado da instalação ao app.
- C# compilou e a regressão de catálogo, armazenamento, backup, versões, tinta imutável e sincronização passou.
- Conteúdos e escrita não foram alterados. A validação completa da 0.8.3 permanece documentada no histórico do repositório.

AOSP API 36 não inclui Google Play Protect. Sua análise de cada novo APK e o fluxo Samsung físico não foram simulados nem desativados. A confirmação própria do instalador Android continua presente.
