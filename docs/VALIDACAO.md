# Validação 0.8.0

Executada em 23/10/2026. Dados sintéticos ou PDFs locais somente para leitura; nenhum documento pessoal foi publicado.

- Android release/R8/lint: 21 cenários de tinta, 20 de gestos/formas/régua e 9 de papéis, mais cenários de ajuste japonês, pressão, curvas/ganchos, quadrículas, contornos, transformação e borracha passaram.
- Integração API 36: atualização de dados antigos, pressão, palma/cancelamento, dedos, salvar/reabrir, imagens/texto, PDF/backup, laço, rotação, miniaturas, temas, histórico e atualização passaram.
- Modelo real `ja` do ML Kit baixou no emulador AOSP e reconheceu os quatro traços de 水. O teste desligou Wi-Fi/dados móveis e confirmou reconhecimento e conversão Regular, reutilização do modelo, alternativas e Original. Não foi um reconhecedor simulado.
- Pipeline após pausa: Traços ajustados substituiu objetos mantendo os originais no histórico; Regular criou contorno de 水; desfazer recuperou os quatro traços e refazer recuperou o contorno. Desligar cancelou trabalho pendente.
- Glyphs nativos de 水, 木 e 日 são diferentes, preservam vazados, cópia, JSON, laço/transformação e PDF. PDFs Android e Windows foram renderizados e conferidos lado a lado; Windows não depende de fontes CJK instaladas.
- C#: validação de contornos, armazenamento, backup e preservação dos caracteres através da interface de texto passaram, incluindo fixture gerada no Android.
- Interoperabilidade Java -> C# -> Java: contornos japoneses, metadados, pressão e PDF original preservados. Conflitos, idempotência, documento aberto, lixeira e restauração continuam verificados.
- Editor JS, exportação do PDF original com rotação/crop/links, busca/sumário e leitura limitada/cancelável passaram. Renderização de contornos manteve vazados e limites; exportação independente de fonte passou.
- WebView2 real: primeira/última página de PDFs de 111, 192 e 23 páginas renderizaram. Dados privados seguem em folha-data.local.
- Chromium/Edge headless isolado: aparência clara/escura, pixels da página preservados, preferência persistente, sistema e layout de 850 px passaram.
- APK público ARM64; variante x86_64 com o mesmo Java/recursos testada no emulador. Assinatura original, alinhamento ZIP e ELF de 16 KB conferidos. O modelo é baixado separadamente.
- CPU de desenho em página com 250 traços: em uma execução mediana 1,7 ms e p95 2,1 ms. Execuções anteriores variaram com a carga do host, até cerca de 5 ms no p95. Não mede a latência física da S Pen.

O ajuste geométrico não reconhece o significado do kanji. Regular é por caractere e pode interpretar outro símbolo: alternativas/Original/Desfazer permitem corrigir. Ainda precisam de teste no tablet físico a precisão com a caligrafia do usuário e a latência da S Pen. Atualize ambos os apps para preservar contornos. OCR e composição vertical de texto digitado não estão incluídos; japonês digitado no Windows continua limitado pelas fontes da exportação de texto.

