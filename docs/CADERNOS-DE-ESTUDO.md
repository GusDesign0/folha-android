# Cadernos de estudo

Escolha em Novo caderno → Tipo de caderno no tablet ou PC. Cada página do caderno usa o mesmo modelo. O modelo pode ser exportado para PDF junto com sua escrita.

| Modelo | Campos |
|---|---|
| Cornell | Perguntas/pistas, anotações, resumo |
| Erros | Questão, minha resposta, correção, motivo, nova tentativa, revisões |
| Exercícios | Enunciado/dados, resolução, resposta/conferência, dúvidas |
| Vocabulário | Quatro registros por página: palavra, leitura, significado, frase |
| Estudo de kanji | Leituras, significado, quadrícula grande, treino com guias, palavras compostas |
| Gramática | Estrutura, uso, exemplos/traduções, comparação, meu exemplo |
| Perguntas e respostas | Duas colunas; cubra uma ao revisar |
| Resumo | Ideia central, conceitos, exemplos/conexões, dúvidas |
| Planejamento | Metas semanais, sete dias, atividade/horário, próximas revisões |
| Tradução | Original e tradução lado a lado, vocabulário, observações |

**Preenchimento manual:** o app não detecta erros, compara gabarito ou agenda notificações. Recortes de materiais podem ser inseridos como imagem com as ferramentas existentes do tablet; não há ação automática de enviar uma questão do PDF a outro caderno. No Windows a edição é de texto; tinta e imagens do tablet são preservadas.

Ícone, cor e rótulo derivam do campo paper já sincronizado; não alteram sua capa personalizada. Os papéis anteriores também recebem identificação, sem migrar ou alterar o conteúdo dos documentos. Atualize ambos os aplicativos.

## Definições e desempenho

A fonte editável é papers.json. node generate-papers.cjs gera StudyPaper.java e desktop/web/paper-data.mjs. Os arquivos gerados acompanham o código; não é necessário Node no app. No Android, linhas e títulos viram caminhos em cache LRU limitado a 12 modelos. Nenhuma análise é adicionada ao evento MOVE. No PC o fundo é desenhado separadamente da camada de edição. PDF contém linhas e títulos, sem imagem de fundo.
