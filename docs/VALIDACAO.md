# Validação 0.8.1

Executada em 23/10/2026 com dados sintéticos e emulador dedicado sem janela.

- Android release/R8 e lintRelease passaram (0 erros; avisos existentes). Assinatura anterior preservada. APK público ARM64; variante x86_64 com mesmo Java/recursos testada.
- 21 cenários de tinta, 20 de gestos/formas/régua, 19 modelos de papel e cenários japoneses passaram.
- Comparação numérica Java/JS dos 19 fundos; comparação dos títulos de dez modelos; limites e exportação PDF com texto na orientação correta passaram.
- Dez modelos no Android: caneta com pressão, desfazer/refazer, salvar/reabrir, identidade/capa, backup editável, exportar/renderizar PDF passaram.
- Regressão API 36: atualização in-place de dados antigos, palma, cancelamento, dedos, seleção, texto/imagens, PDFs com rotação/crop, miniaturas, rotação, temas e histórico passaram.
- Modelo real japonês reconheceu 水 com Wi-Fi/dados desligados; modos, Original, alternativas e histórico preservados. Atualizador validou download, integridade e assinatura.
- CPU de desenho da regressão com 250 traços: mediana 1,8 ms e p95 2,4 ms em uma execução; repetição sob carga chegou a 3,4 / 6,1 ms no emulador. Não mede latência física e não é promessa para o tablet. Os novos fundos reutilizam caminhos, sem trabalho adicional em MOVE.
- Windows C#: dez tipos preservados em armazenamento/backup/sincronização com texto; proteção de tinta, conflitos, lixeira, versões e integridade passaram.
- Interface real em Edge headless: dez ícones e cores, nomes dos tipos, 19 opções, criação e abertura de todos os modelos, modo claro/escuro e diálogo em 850 px passaram.
- Editor JS: texto, salvar/repetir sem duplicação, mover/cancelar, desfazer/refazer e fechar após salvar passaram; busca/leitura e contornos japoneses preservados.
- PDFs de erros gerados no Android e Windows foram renderizados e inspecionados.

Precisão/latência física com S Pen e sincronização em dois dispositivos reais ainda dependem do usuário. Modelos são de preenchimento manual, sem correção, gabarito ou lembretes.
