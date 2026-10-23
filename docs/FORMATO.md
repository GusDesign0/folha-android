# Formato portátil Folha (.folha)

Um arquivo .folha é um ZIP com:

- document.json: schema version 2, IDs UUID de caderno e objetos, revisão, páginas e objetos.
- source.pdf: PDF original, quando o caderno foi importado de um PDF.
- assets/<UUID>.png ou .jpg: imagens referenciadas pelos objetos.

As coordenadas são pontos da página, com origem no canto superior esquerdo da página exibida. Cadernos novos usam 595 x 842 pontos. Em PDFs, a geometria exibida considera CropBox e rotação. Não há caminhos absolutos ou dados próprios de um aparelho no formato.

Cada objeto tem kind (ink, text ou image), id e propriedades próprias. Tinta preserva pontos de entrada (x, y, pressão), largura, cor, marca-texto, tipo de caneta, suavização e sensibilidade. Texto preserva Unicode e caixa; imagens referenciam caminhos relativos. Mover ou redimensionar preserva o ID; cópias recebem um novo ID.

O backup é editável. Restaurar um ID já existente cria uma cópia, preservando originId. Versões desconhecidas do formato são recusadas para evitar uma restauração incompleta.

## Próxima etapa: tablet e Windows com a mesma conta

A sincronização de conta e o aplicativo Windows ainda não estão implementados. O plano é reutilizar este formato nos dois, mantendo um cache local para escrita offline e uma fila de alterações em segundo plano.

O serviço futuro deverá manter uma revisão-base por dispositivo, enviar somente alterações e assets novos, detectar alterações simultâneas e preservar cópias conflitantes sem sobrescrever silenciosamente o trabalho. Antes dessa etapa serão definidos o login, o armazenamento, a política de conflitos e os testes entre aparelhos.

Requisitos permanentes: não colocar operações de rede no caminho da caneta; não bloquear a escrita ao ficar offline; manter texto, imagens, traços e PDF original portáteis.

## Campos opcionais da versão 0.5

`bookmarks` contém índices de página marcados, iniciando em zero. Mover, duplicar e excluir páginas de cadernos atualiza o mapa `pages`, `count`, `lastPage` e os marcadores na mesma gravação de JSON. PDFs continuam com a sequência original. O schema permanece 2: tinta de formas/régua usa os mesmos pontos e tipos de caneta. Versões antigas podem ler a tinta e a organização de cadernos, mas ignoram os marcadores. Não há conta ou sincronização implementadas.

## Lixeira (0.6.1)

O campo opcional `deleted: true` mantém o caderno e seus arquivos na lixeira. Ausência do campo significa biblioteca ativa. A restauração remove o campo. Escrita e arquivos originais são preservados. `pdfPending: true` é usado apenas no Windows para importações ainda não abertas; estas não são enviadas ao Drive até a contagem de páginas ser validada.

## Campos 0.7.0

Papéis novos: tategaki, genko200, genko400, kanji e kana. Genko400 usa 842x595 pontos; demais, 595x842. Metadados opcionais: favorite, cover (sage/sand/blue/rose/lavender), lastOpened. Texto pode ter align (left/center/right). _localHash existe apenas no metadata.json local e não é sincronizado. Cada aparelho mantém até 12 snapshots JSON locais; recuperar cria outro id e preserva os arquivos originais.
