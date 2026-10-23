# Validação 0.8.2

23/10/2026; dados sintéticos, sem perfil pessoal ou janelas do usuário.

- Release Android/R8 e lintRelease: 0 erros, 34 avisos. Assinatura original e alinhamento 16 KB preservados; ARM64 público e x86_64 no emulador API 36.
- Core: tinta, gestos, formas/régua, 21 modelos públicos, auxílio japonês, 54 lições e 151 contornos passaram.
- JS: todos os 54 layouts dentro da página, 151 contornos portáteis, títulos compatíveis com PDF, 27 páginas exportadas por alfabeto. Os dez modelos anteriores mantêm geometria e títulos em Java/JS.
- Integração nativa: cada sequência com 27 páginas, caneta/desfazer/refazer, salvar/reabrir, backup, fundos por página, mover/duplicar/apagar junto da tinta, miniaturas e PDF renderizado passaram.
- Interoperabilidade Java → C# → Java com fixture nativa preservou as 27 lições e tinta com pressão. C#: inicialização, cópia/backup/sincronização e bloqueio da alteração de fundos existentes passaram.
- Interface real em Edge headless: 21 opções, criação com sequência, mudança do fundo ao avançar, restauração da última página, + Página repetindo a lição atual e identidade na biblioteca passaram.
- Editor JS, busca/leitura e contornos japoneses passaram; regressão completa do Android incluiu palma, rotação, temas, histórico, auxílio japonês real offline e atualizador.
- PDF do Android e do Windows da mesma lição foi renderizado e inspecionado. Modelos japoneses são caminhos e não dependem de fonte CJK instalada no PC.
- CPU de fundos em cache (bitmap 595×842, emulador): mediana ~1,2–2,0 ms e p95 ~1,4–2,6 ms nos quatro casos medidos. A regressão com 250 traços ficou ~1,7 ms de mediana e ~2,4–3,2 ms no p95. Não mede latência física da S Pen.

Precisão, conforto e latência no Samsung físico ainda dependem do teste do usuário. Não há avaliação automática de escrita, ordem dos traços ou áudio.
