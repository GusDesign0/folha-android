# Folha 0.3.0 - validação e arquitetura

A interface de escrita usa Canvas e APIs nativas do Android. PdfBox-Android é carregado apenas nas operações de exportação, em segundo plano.

## Verificação executada

- 21 cenários de lógica: curvas, pressão, desfazer/refazer, corte parcial e transformações imutáveis.
- Integração em Android 16 x86_64: pressão de caneta, palma cancelada, dedos sem tinta, persistência e atualização sobre versões anteriores.
- Laço, mover, redimensionar, recolorir e copiar/colar entre páginas do mesmo caderno.
- Importação real do fluxo de imagem, normalização, texto/imagem/traço no JSON e backup editável com restauração em cópia.
- Rejeição de caminhos maliciosos em ZIP e preservação dos traços quando metadados e conteúdo são salvos em sequência.
- Exportação de PDF misto e preservação do texto e links originais em três páginas: rotação 0, rotação 90 e rotação 270 com CropBox deslocado.
- Atualizador: consulta e download reais do GitHub, tamanho e SHA-256, rejeição de versão antiga, assinatura diferente, arquivo truncado/corrompido e cancelamento. URI privada do APK aceita somente leitura e não expõe os cadernos.
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

## Atualizador 0.4.0

A consulta, o download em blocos de 32 KB, o hash e a leitura do pacote usam uma fila exclusiva com prioridade de segundo plano. O progresso visual é limitado a cerca de 7 atualizações por segundo. Não há serviço em segundo plano nem verificações periódicas. Requer confirmação de instalação pelo Android e, na primeira vez, permissão para instalar apps pelo Folha. A versão 0.3.0 precisa receber esta atualização pelo APK; as próximas podem usar o botão. O aparelho pode impor políticas adicionais de instalação.

## Atualização 0.5.0

- 21 cenários de tinta existentes e 20 novos cenários de gesto, formas e geometria da régua passaram.
- Integração Android 16: toque com dois dedos desfaz uma edição e aceita refazer; pinça saindo/voltando, terceiro dedo, cancelamento e palma não desfazem. Escrita opcional com o dedo é cancelada antes de reconhecer o atalho.
- Linha e elipse ajustadas por espera real de 0,6 s na S Pen sintética; régua girada mantém o traço alinhado.
- Mover, duplicar e excluir páginas de caderno preservam ordem, objetos e marcadores; duplicação cria IDs independentes. JSON e backup editável preservam esses dados. A ordem dos PDFs não é alterada.
- Miniatura real de PDF com rotação/CropBox e miniaturas de caderno têm dimensão máxima de 256 px. Grade recicla células, limita cache a 10 miniaturas e fila a 12 pedidos. Imagens de células visíveis são liberadas ao fechar o painel; a atividade também solta a referência ao snapshot do caderno.
- Reconhecimento é executado uma vez após segurar, limitado a 8192 pontos. O gesto de toque mantém apenas estado numérico, sem criar listas por amostra.
- A régua e o reconhecimento produzem traços comuns, selecionáveis e exportáveis como caminhos; não mudam o formato de tinta.
- O script de testes agora falha explicitamente em crash ou ausência de PASS e usa um dispositivo ADB nomeado.

Não foram testados Apple Pencil/iPadOS nem a S Pen física. iPadOS, Windows, conta e sincronização continuam como plataformas/etapas futuras.

## Rotação no tablet

Testada rotação física simulada do emulador de paisagem para retrato e de volta, com o editor e o painel de miniaturas abertos. Mantém a mesma página, tinta, instância do editor, desfazer/refazer e zoom. A régua continua em coordenadas da página, e o enquadramento é limitado ao novo tamanho da área visível. O painel recalcula o tamanho e suas colunas. O teste restaura as configurações de rotação do emulador e não altera dispositivos físicos. O comportamento respeita a rotação automática/bloqueio do Android.
