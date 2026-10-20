# Folha 0.3.0 - validação e arquitetura

A interface de escrita usa Canvas e APIs nativas do Android. PdfBox-Android é carregado apenas nas operações de exportação, em segundo plano.

## Verificação executada

- 21 cenários de lógica: curvas, pressão, desfazer/refazer, corte parcial e transformações imutáveis.
- Integração em Android 16 x86_64: pressão de caneta, palma cancelada, dedos sem tinta, persistência e atualização sobre versões anteriores.
- Laço, mover, redimensionar, recolorir e copiar/colar entre páginas do mesmo caderno.
- Importação real do fluxo de imagem, normalização, texto/imagem/traço no JSON e backup editável com restauração em cópia.
- Rejeição de caminhos maliciosos em ZIP e preservação dos traços quando metadados e conteúdo são salvos em sequência.
- Exportação de PDF misto e preservação do texto e links originais em três páginas: rotação 0, rotação 90 e rotação 270 com CropBox deslocado.
- Consulta manual das versões publicadas no GitHub.
- Cena com 250 traços de 40 pontos cada: CPU de desenho p50 = 1,882 ms; p95 = 2,491 ms em 100 amostras no emulador. Este número não mede latência da S Pen física, taxa de quadros ou garantia de desempenho em outro aparelho.

A resposta física da S Pen não foi testada.

## Medidas para manter a escrita leve

- Caneta não faz rede, leitura de arquivo, exportação ou compactação.
- Cache de tinta acompanha apenas a área visível, em até 2x a resolução, com limite aproximado de 8 milhões de pixels e fallback de menor memória.
- Ao terminar um traço, a tinta é acrescentada ao cache existente. A página inteira não é redesenhada a cada novo traço.
- Geometria pronta dos traços é reutilizada. O desenho durante a escrita trabalha com o traço ativo.
- O laço desenha uma transformação visual durante o arraste; pontos são substituídos uma única vez ao confirmar o gesto.
- Biblioteca mantém metadados leves; pontos são carregados ao abrir o caderno.
- Salvamento serializa JSON em uma fila de segundo plano e reúne gravações pendentes do mesmo caderno.
- Exportação/backup e renderização têm filas separadas, com prioridade de segundo plano.
- Imagens são normalizadas até 2048 px na importação. A decodificação da página possui orçamento de pixels; miniaturas de outras páginas não ficam permanentemente carregadas.
- A exportação de PDFs usa arquivo temporário e memória mista limitada para o parser. Não rasteriza o documento original.

## Limites atuais

- Copiar imagens entre cadernos diferentes ainda não é suportado; o clipboard funciona entre páginas do mesmo caderno.
- Desfazer/refazer reinicia ao trocar de página e mantém até 50 estados.
- Imagens importadas são normalizadas até 2048 px; não substituem um fluxo de fotografia em resolução original.
- PDFs com senha não são suportados.
- Conta, sincronização, Windows e OCR são etapas futuras.
