# Validação 0.7.1

Executada em 23/10/2026. Testes usam dados sintéticos ou PDFs locais apenas para leitura, sem publicar documentos do usuário.

- Android: build release/R8/lint e assinatura original verificados; 21 cenários de tinta, 20 de gestos/régua/formas e 9 de papéis passaram.
- Integração em Android API 36: atualização sobre cadernos antigos, pressão, cancelamento de traço/palma, dedos, salvar/reabrir, PDF, backup, seleção, imagens/texto, rotação e miniaturas passaram.
- Aparência clara/escura/sistema mantém a mesma instância de NotebookView, zoom, objetos e histórico de desfazer/refazer. Preferência salva localmente. Mudanças reais de modo noturno do emulador atualizam a interface sem recriar o editor. Capturas claras/escuras conferidas.
- CPU de desenho em página com 250 traços foi 1,8 ms de mediana e 2,3 ms no p95 nesta execução em emulador. Não equivale à latência física da S Pen.
- Editor JavaScript real: trocar tema não modifica pixels dos canvases nem emite operações de arquivo; texto, desfazer/refazer, retry, mover/cancelar, fechar e lixeira/restauração passaram.
- Chromium/Edge em processo headless isolado: biblioteca/editor/diálogo claros e escuros, persistência após recarregar, troca de aparência do sistema, seleção em lote e menu por arquivo. Cabeçalho/rodapé cabem em 850 px; ajuste à largura acompanha a janela e preserva zoom manual.
- C# mantém desenho/imagens imutáveis, salva e recupera versões como cópias, conserva PDF e propaga lixeira/restauração; conflitos, idempotência e proteção do documento aberto passaram.
- WebView2 instalado: primeira e última páginas de PDFs de 111, 192 e 23 páginas renderizaram. Origem privada folha-data.local preservada.
- Exportação PDF real: três páginas originais, rotação/crop/links e caminhos da tinta preservados. Busca/sumário e leitura sob demanda continuam com cancelamento e limite de telas.

Aparência não entra no documento/sync e não inverte PDFs, papéis ou cores exportadas. Nenhuma biblioteca de animação ou renderização foi adicionada ao app; Playwright é dependência apenas dos testes de desenvolvimento.

Limites anteriores mantidos: teste de latência física da S Pen e de conta/sincronização em dois aparelhos físicos depende do usuário; histórico local, sem OCR, sem composição vertical automática de texto digitado. Exportação de texto no Windows usa Helvetica/WinAnsi.

