# Validação 0.7.0

Executada no workspace em 23/10/2026. Os documentos reais usados em leituras de regressão permanecem locais e não acompanham o código.

- Android: 21 cenários de tinta, 20 de gestos/formas/régua e 9 de papéis passaram. Assinatura do APK mantida; instalação sobre a versão anterior e cadernos antigos preservados no emulador API 36.
- Integração Android: pressão, dedos, cancelamento de palma/traço, salvar/abrir, laço, imagens/texto, exportação, gestos, rotação, miniaturas, atualização e backup passaram.
- Cinco papéis japoneses exportaram com as mesmas dimensões do editor. Genko 400 preservou 842 x 595 pontos; demais modelos, 595 x 842.
- Versões: histórico localizado, limitado a 12 e recuperado como cópia com papel, capa, favorito, PDF/traços preservados. Salvamento assíncrono de metadados e lixeira preservaram a tinta.
- Favoritos restauraram parâmetros da caneta; foco pôde ser ativado e desativado; leitura contínua encerrou seu worker/cache ao fechar. Sumário resolveu o destino da página.
- CPU de desenho no emulador, página com 250 traços: mediana 2,7 ms, p95 3,3 ms na última execução (houve variação com a carga do host). Isso não mede latência real da S Pen e não garante o mesmo resultado em hardware físico.
- C#: texto mantém tinta/imagens, tentativas de desenho são recusadas, histórico e recuperação não sobrescrevem originais, lixeira/restauração sincronizam e PDFs são reutilizados. Conflitos e proteção do documento aberto passaram.
- Java -> C# -> Java: papel genko400, capa, favorito, metadados, pressão e PDF original preservados; abrir um arquivo não criou revisão remota apenas por recentes.
- Editor JavaScript real em adaptador DOM/Canvas: texto, desfazer/refazer, retry, mover e cancelar arraste, salvamento ao fechar, lixeira/restaurar passaram.
- Navegador de estudo: busca com espaços/caracteres japoneses e variantes de largura, notas digitadas, sumário com referências e destinos nomeados, cancelamento e limite de cinco telas em leitura contínua passaram em adaptador.
- WebView2 instalado: PDFs locais de 111, 192 e 23 páginas abriram e renderizaram primeira/última página. Origem de dados privada segue separada da origem estática, preservando a correção de 0.6.1.
- Paridade Java/JavaScript para os nove papéis verificada numericamente. O PDF dos cinco modelos contém caminhos, com páginas corretamente dimensionadas; renderizado com Poppler e conferido visualmente.

Limites: conta no tablet físico, latência física da caneta e sincronização em dois aparelhos físicos ainda precisam de conferência do usuário. Histórico é local; não há OCR nem composição vertical automática de texto digitado. Texto exportado no Windows usa Helvetica/WinAnsi. A leitura contínua volta ao editor por página para anotar. Novos campos/papéis exigem atualizar os dois aplicativos.
