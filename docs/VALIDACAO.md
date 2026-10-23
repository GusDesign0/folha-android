# Validação 0.8.3

23/10/2026. Testes em perfis sintéticos e navegador/emulador sem janelas do usuário.

- Android release/R8 e lintRelease: 0 erros, 34 avisos anteriores. Atualização sobre a instalação antiga preservou cadernos.
- Nova aba nativa: dois materiais locais; conferência SHA-256 e contagem de 238/53 páginas; criação, reabertura sem duplicar, preservação de tinta/metadados, restauração da lixeira, renderização, PDF anotado e backup editável passaram.
- Regressão completa Android: S Pen/pressão, palma, cancelamento, desfazer/refazer, gestos, rotação, salvar/reabrir, papéis japoneses, kana, temas, reconhecimento japonês real offline e atualizador passaram. Página com 250 traços no emulador: CPU p50 1,726 ms e p95 2,686 ms; não mede latência física da S Pen.
- C#: catálogo offline, SHA-256, reutilização de cópia, restauração/progresso, deduplicação ao restaurar backup e ID inexistente passaram; regressão de persistência, sincronização, conflitos, histórico e texto imutável em relação à tinta passou.
- Interface real em Edge headless: capas, claro/escuro e janela 880×760; os dois PDFs abertos pelo PDF.js; anotação digitada preservada ao reabrir, dois materiais sem duplicatas, navegação e ausência de erros JavaScript passaram.
- As 291 páginas dos PDFs foram renderizadas e inspecionadas; 500 pares de vocabulário por PDF e 75 textos/leituras/traduções da apostila preservados após a revisão visual.

Os PDFs originais são imutáveis. O catálogo só lê metadados/capas quando aberto, e prepara a cópia fora da thread de entrada. Não há consulta ao catálogo em MOVE da caneta. Sincronização física PC/tablet e conforto no Samsung ainda dependem do teste do usuário.
