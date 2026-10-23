Folha 0.8.0 - auxílio de kana e kanji

No tablet, toque em あ 漢 na barra da caneta: Desligado / Traços ajustados / Regular.

- Traços ajustados suaviza e endireita segmentos quase retos, preservando curvas, ganchos e pressão. Não reconhece o significado do kanji.
- Regular reconhece um kana ou kanji após uma pausa de 1,8 s. Oferece alternativas e Original; dois dedos desfazem.
- Primeiro uso de Regular baixa o modelo japonês (~20 MB). Reconhecimento offline, em executor serial de prioridade baixa, com fila limitada e resultados verificados antes de alterar a página.
- Prefira papéis Kanji, Kana ou Genkō yōshi; fora de quadrículas, escreva um caractere isolado por vez.
- Regular salva contornos: abre no PC, sincroniza e exporta como caminhos PDF, sem fonte japonesa instalada no Windows.
- APK público ARM64 para o Samsung; x86_64 gerado separadamente só para testes.

Atualize tablet e PC. No tablet, não desinstale. No PC, feche a versão anterior e extraia o ZIP inteiro.

Testes: modelo real reconheceu 水 com Wi-Fi/dados desligados; modos ao vivo, Original, undo/redo, cancelamento; Java/C# ida e volta com contornos; PDF nativo e Windows com vazados preservados. Android API 36, renderização WebView2 e regressões de biblioteca/temas passaram. A precisão com sua caligrafia e latência física ainda precisam de teste no tablet.
