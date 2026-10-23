# Conteúdos do Folha

A partir da versão 0.8.3, Android e Windows têm a aba **Conteúdos**, com os materiais já incluídos na instalação. Não precisa baixar PDFs separadamente, conectar uma conta nem acessar a internet para estudar.

| Material | Páginas | Conteúdo |
|---|---:|---|
| Kanji para a vida diária | 238 | 250 kanji, 25 lições, 500 exemplos, exercícios e gabaritos |
| Kanji · caderno guiado | 53 | Cinco kanji por folha, leituras, progressão dos traços e repetição |

## Como estudar

1. Abra **Conteúdos** na biblioteca.
2. Toque em **Adicionar e abrir** no material desejado.
3. Sua cópia aparece na biblioteca, na pasta **Conteúdos · Japonês**. No tablet, anote com a caneta; no Windows, leia e digite.
4. O botão passa a **Abrir na biblioteca**. Reabrir não cria outra cópia e não substitui suas anotações.
5. Se a cópia estiver na lixeira, **Restaurar e abrir** recupera o progresso existente.

Os PDFs originais do catálogo ficam separados das suas cópias. Apagar a cópia não apaga o material oferecido. PDFs que você já importou manualmente continuam independentes, com seus nomes e anotações.

As cópias da biblioteca usam o fluxo normal de backup `.folha`, PDF anotado e Google Drive. Para continuar em outro aparelho, feche o caderno, sincronize e abra a cópia recebida na biblioteca. Edições simultâneas seguem a política de cópias de conflito do Folha. A primeira utilização de uma edição usa o mesmo UUID no tablet e PC; uma cópia recebida ou restaurada é reutilizada pelo catálogo.

## Arquivos versionados

O catálogo fonte é [`assets/content/catalog.json`](../assets/content/catalog.json). As capas PNG e os PDFs ficam na mesma pasta. Os materiais são locais: esta versão atualiza o catálogo ao atualizar o aplicativo, sem consultar servidores durante a escrita.

Cada entrada inclui `id`, `documentId` (UUID da edição), `edition`, `title`, `description`, `category`, `pages`, `file`, `cover`, `bytes`, `sha256` e `license`. Os nomes dos arquivos são simples, sem subpastas. Android e Windows recusam entradas inválidas e conferem tamanho e SHA-256 antes de criar a cópia. Android confere também a contagem nativa de páginas; no PC o PDF.js confere a contagem ao abrir.

Para adicionar um material:

1. Coloque o PDF autorizado e uma capa PNG pequena na pasta.
2. Adicione uma entrada com UUID novo, contagem de páginas, tamanho e SHA-256 corretos.
3. Confira abertura, legibilidade, adição, reabertura, anotações, lixeira e backup nos dois clientes.
4. Publique junto com a próxima versão do app. O build Windows copia os originais para `content/` e somente as capas para `web/content/`; Android lê os assets diretamente.

Para uma **nova edição de um PDF**, use **novo `documentId` e `id`**. Nunca reutilize a identidade de uma edição com páginas alteradas: as coordenadas das anotações pertencem àquele PDF. As cópias existentes não são atualizadas nem reimpressas pelo catálogo.

## Autoria e licenças

Currículo, textos, exercícios, traduções em português e diagramação são materiais originais do Folha. Não reproduzem o livro comercial PRACTICAL KANJI da AJALT/ASK. A seleção temática e a sequência são independentes.

- Material adaptado: **CC BY-SA 4.0**, com créditos completos no fim de cada PDF.
- Leituras/contagens: KANJIDIC2; pares palavra/leitura conferidos no JMdict, EDRDG, CC BY-SA 4.0.
- Diagramas: KanjiVG, Ulrich Apel e colaboradores, CC BY-SA 3.0, adaptados para sequência cumulativa.
- Japonês: Klee One, SIL OFL 1.1. As fontes de exibição estão incorporadas aos PDFs; não é preciso instalá-las.

Fontes: [EDRDG](https://www.edrdg.org/edrdg/licence.html), [KanjiVG](https://kanjivg.tagaini.net/), [Klee One](https://github.com/google/fonts/tree/main/ofl/kleeone), [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/).

O catálogo não avalia a escrita automaticamente nem inclui áudio. Os PDFs são renderizados por página; o catálogo e a cópia não participam dos eventos da S Pen.
