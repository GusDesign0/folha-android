# Desenvolvimento

O aplicativo usa Java 8 e APIs nativas do Android, mínimo SDK 29 e alvo SDK 36. A compilação usa javac, aapt2, d8, zipalign e apksigner diretamente.

## Componentes

- MainActivity: biblioteca, controles, importações, atualizações e filas de trabalho.
- NotebookView: entrada de caneta, gestos, cache de tinta e borracha.
- Ink: modelo independente de Android, curvas, pressão, corte e histórico.
- SelectionEditor: laço, transformação visual, copiar/colar e edição de seleção.
- Documents: JSON, metadados leves e gravação atômica.
- MediaFiles: normalização e decodificação de imagens com orçamento de pixels.
- Backups: pacote portátil .folha e restauração com validação de entradas.
- PdfFiles: renderização nativa e exportação com conteúdo original preservado.
- PageArt: desenho de papel, tinta, texto e imagens.
- UpdateChecker: consulta manual da última versão do GitHub.

## Ferramentas e dependências

`setup.ps1` baixa as ferramentas em `../toolchain`. `fetch-deps.ps1` obtém PdfBox-Android 2.0.27.0 e Bouncy Castle 1.72 do Maven Central, verifica SHA-256, extrai classes e recursos. Eles são usados no processamento de arquivos; a escrita não depende desse processamento.

As bibliotecas e os recursos obtidos durante a compilação não são versionados. Licenças e avisos acompanham o APK em `assets/licenses` e `assets/THIRD_PARTY_NOTICES.txt`.

A chave local fica em `../toolchain/private/folha.p12`. Não deve ser publicada ou incluída no backup dos cadernos. Preserve-a para assinar futuras atualizações do mesmo pacote.

## Publicar uma atualização

1. Incremente versionCode/versionName no manifesto e atualize os rótulos/documentação.
2. Compile e execute testes adequados à mudança.
3. Coloque o APK assinado em dist/Folha.apk, o ZIP em dist/Folha.zip e atualize dist/SHA256SUMS.txt e version.json.
4. Publique o código no GitHub e crie uma release com a tag correspondente.
5. O workflow Publicar instaladores valida os checksums e anexa os arquivos à release. Ele também pode ser executado manualmente para uma tag existente.

O workflow reutiliza os arquivos já assinados; nenhuma chave privada de assinatura é enviada ao GitHub. Os links releases/latest/download/Folha.apk e Folha.zip permanecem fixos nas atualizações.

Veja VALIDACAO.md para os testes já executados e FORMATO.md para o arquivo portátil e a evolução para Windows/sincronização.
