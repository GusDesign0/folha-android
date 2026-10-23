# Auxílio de kana e kanji

Abra um caderno no tablet e toque em **あ 漢**. A preferência é local e começa desligada.

| Modo | Comportamento |
| --- | --- |
| Desligado | Escrita normal. Cancela os trabalhos pendentes. |
| Traços ajustados | Suavização geométrica, compensação de inclinação leve e alinhamento de segmentos quase retos, preservando curvas, ganchos e pressão. Centraliza nas quadrículas japonesas. Não reconhece o significado. |
| Regular | Reconhece um caractere japonês de cada vez e troca por uma forma regular. Oferece alternativas e Original. |

Os dois modos trabalham após uma pausa de 1,8 s. Kanji, Kana e Genkō yōshi têm separação por quadrícula; nos demais papéis e em PDFs, deixe espaço entre caracteres. O modo Regular interpreta escrita japonesa, e não comandos em romaji como mizu.

Regular usa [ML Kit Digital Ink Recognition](https://developers.google.com/ml-kit/vision/digital-ink-recognition), com modelo `ja` baixado no primeiro uso (~20 MB). Após o download, reconhece no aparelho, sem internet. O app inicia o download ao escolher Regular; o diálogo permite tentar novamente. Traços ajustados não usa esse modelo.

A tinta original só é substituída depois de verificar se o caderno, a página e os traços continuam iguais. Desfazer/refazer preserva a operação. Voltar a escrever no mesmo caractere recém-processado reabre seus traços originais; o buffer recente é limitado. Resultados atrasados não alteram outra página ou um documento fechado. Desligar não reverte caracteres que já foram concluídos: use Original/Desfazer.

Regular salva contornos preenchidos, preservados no PC e em PDF sem fonte japonesa instalada. O campo semântico `text` permite busca no caderno; o PDF recebe caminhos, sem camada de OCR. O laço move, redimensiona e recolore esses objetos; a borracha remove um caractere regular inteiro.

Atualize Android e Windows para 0.8.0 antes de usar Regular em arquivos compartilhados. Versões anteriores não conhecem esses contornos. A exportação de texto japonês digitado no Windows ainda depende de suporte de fontes; isso é separado dos contornos gerados no tablet.

O trabalho de geometria e do reconhecedor usa um executor serial de prioridade baixa. Não há reconhecimento no tratamento de movimento da caneta; a fila tem no máximo 32 grupos, 64 traços/16 mil pontos por grupo. Resultados sem um único caractere japonês preservam a escrita; uma interpretação errada ainda pode aparecer entre as alternativas. Precisão e latência no tablet físico ainda precisam de avaliação pelo usuário.
