# Hiragana e katakana

Crie em Novo caderno → Tipo de caderno. Cada um tem 27 páginas e identidade própria na biblioteca. As tabelas/currículo seguem os sons básicos e combinações; o material é uma composição original do Folha, conferida com a [tabela da Japan Foundation](https://www.erin.jpf.go.jp/en/extra/syllabary/). Não copia suas ilustrações ou páginas.

| Páginas | Conteúdo |
|---|---|
| 1 | Consulta: 46 kana básicos |
| 2–11 | Básicos por grupos |
| 12 | Consulta: 25 sons com dakuten/handakuten |
| 13–17 | Prática dos sons com marcas |
| 18 | Consulta: 33 combinações |
| 19–25 | Prática das combinações |
| 26 | Kana pequenos e sinais |
| 27 | Formas parecidas: hiragana sa/ki/me/nu/ne; katakana shi/tsu/so/n |

Cada linha de treino tem modelo, duas cópias para contornar, quatro espaços para repetir e dois para tentar de memória. Cubra o modelo ao revisar. As datas são manuais. O sinal de prolongamento é incluído no katakana. Romaji segue leituras usuais; ji(di)/zu(du) distinguem as séries de digitação. Partículas ha/he/wo no hiragana podem ser lidas wa/e/o. Não inclui toda a coleção moderna de combinações estrangeiras em katakana.

**Limites:** não há notas automáticas, reconhecimento de acerto ou ordem dos traços numerada. Regular é por caractere; para treinar pares de kana, prefira Desligado ou Traços ajustados. O fundo não é apagado pela borracha.

## Repetição e organização

+ Página cria outra folha com o fundo da página atual; você começa sem a tinta anterior. Duplicar pelas miniaturas do tablet copia também suas anotações. Ao mover, duplicar e apagar, o modelo segue a página. Backup, Drive e PDF preservam os fundos; atualize os dois clientes.

## Implementação e licença

O campo opcional pagePapers mapeia posição para modelo. O campo paper continua sendo o tipo do caderno. Documentos sem o mapa usam o papel global, preservando compatibilidade de leitura com seus cadernos antigos. Clientes anteriores não compreendem o novo mapa: atualize ambos antes de editar estes cadernos.

As linhas/títulos/modelos são dados locais de papers.json. Android reutiliza cinco caminhos de fundo num cache LRU de 12 páginas, sem análise no evento MOVE. O PC desenha em camada separada; PDF grava os modelos como caminhos.

Os 151 caracteres de referência foram derivados de [Klee One Regular](https://github.com/google/fonts/tree/main/ofl/kleeone), Copyright 2020 The Klee Project Authors, sob SIL OFL 1.1. A licença acompanha Android e Windows em licenses/klee-one.txt. O TTF completo não acompanha os instaladores. generate-kana.py com FontTools 4.60.1 recria os contornos a partir do TTF com SHA-256 fixado; generate-papers.cjs gera constantes Java/JS a partir dos JSONs.
