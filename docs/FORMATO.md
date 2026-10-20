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
