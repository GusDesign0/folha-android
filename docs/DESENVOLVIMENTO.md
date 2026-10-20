# Folha para Android

Versão independente de um app de cadernos e anotações em PDF, pensado para tablet Samsung com S Pen. Interface em português, sem conta, sem anúncios e sem conexão com a internet.

## Instalar no tablet

1. Transfira `Folha-0.2.0.apk` para o tablet, por exemplo usando um cabo USB ou seu armazenamento de arquivos.
2. No tablet, abra o APK no aplicativo **Meus Arquivos**.
3. Se o Android solicitar, autorize a instalação a partir desse aplicativo e toque em **Instalar**.
4. Abra **Folha**, crie um caderno ou toque em **Importar PDF**.

É uma versão inicial para instalação direta, fora da Play Store. Requer Android 10 ou superior; compilada com SDK 36 e direcionada ao Android 16. O APK usa código Java sem bibliotecas nativas e atende ARM64 e x86_64.

## O que funciona

- Biblioteca de cadernos, nomes e pastas. Para mudar nome e pasta, mantenha um caderno pressionado na biblioteca ou toque no título no editor.
- Papéis pautado, quadriculado, pontilhado e em branco, com adição de páginas.
- Importação de PDF usando o seletor de arquivos do Android; o original é copiado para o armazenamento privado do app.
- Caneta com pressão suavizada, curvas arredondadas, contorno contínuo, marca-texto, cores e ajuste de espessura.
- Borracha de traços completos, desfazer e refazer na página aberta.
- Escrita exclusiva pela caneta por padrão; os dedos movem e ampliam a página. A escrita com o dedo pode ser ativada nas opções.
- Eventos cancelados descartam o traço em andamento. O cancelamento de um ponteiro da palma preserva o ponteiro ativo da caneta.
- Botão lateral da S Pen funciona como borracha enquanto estiver pressionado ao iniciar o contato, quando o Android reporta esse botão.
- Ajuste da página tocando na porcentagem do zoom, navegação por página e salto pelo número.
- Salvamento automático local com gravação atômica.
- Exportação de todas as páginas com anotações para um PDF no local escolhido. A escrita, o marca-texto e o papel dos cadernos são caminhos no PDF, sem rasterizar as anotações.
- Layout adaptável a retrato, paisagem e janelas redimensionáveis.

## Limites desta versão

A escrita real, a latência, a pressão e a interação da palma com a S Pen devem ser validadas no tablet físico. Os testes no emulador usam eventos sintéticos de caneta e dedos. A versão 0.2.0 foi instalada sobre a 0.1.0, e a preservação dos cadernos foi verificada. O cache de tinta acompanha a área visível no zoom, em até duas vezes a resolução da tela, com limite de memória.

As anotações permanecem editáveis no caderno dentro do app. O PDF exportado é uma cópia visual: as páginas dos PDFs importados são rasterizadas em até 2000 pixels na maior dimensão, com os traços desenhados sobre elas como caminhos. As anotações podem ser abertas como formas em um editor que importe caminhos de PDF; isso não as converte em texto editável ou nos tipos nativos de anotação de cada editor. Texto selecionável, pesquisa, links, formulários e estrutura do PDF original não são preservados nessa cópia. O arquivo original importado permanece guardado internamente.

PDFs protegidos por senha, OCR, reconhecimento de escrita, lasso, imagens, caixas de texto e sincronização ainda não estão implementados. O desfazer/refazer é limitado à página atualmente aberta e reinicia ao trocar de página.

Os cadernos ficam no armazenamento privado do app. Desinstalar ou limpar os dados remove os cadernos. Exporte os PDFs que deseja guardar; a exportação não é um backup editável dos cadernos. PDFs muito extensos podem exigir bastante memória durante a exportação.

## Código e compilação no Windows

Este projeto usa as ferramentas oficiais do Android SDK diretamente, sem dependências de interface e sem precisar do Android Studio ou Gradle.

```powershell
.\setup.ps1
```

O script baixa JDK, Android SDK 36 e Build Tools 36.0.0 para uma pasta `toolchain` ao lado do projeto, valida os checksums e executa a compilação. Os URLs e checksums estão em `toolchain.json`. São cerca de 330 MB de downloads. Leia as licenças dos fornecedores das ferramentas antes de distribuí-las.

Se as ferramentas já estiverem nessa pasta:

```powershell
.\build.ps1
```

O resultado é `Folha.apk`. A chave de assinatura de desenvolvimento fica em `toolchain/private/folha.p12`. Preserve a mesma chave para criar atualizações instaláveis sobre essa versão. A chave usada no APK entregue permanece no ambiente de criação e não acompanha o código-fonte. Uma compilação com outra chave não atualiza o APK entregue diretamente.

Para publicação na Play Store, configure uma chave de distribuição própria e um processo de release.

## Estrutura

- `src/.../MainActivity.java`: biblioteca, organização, editor e seletores de arquivos.
- `src/.../NotebookView.java`: escrita, pressão, gestos e cancelamento de ponteiros.
- `src/.../Ink.java`: modelo dos traços, detecção da borracha e histórico.
- `src/.../Documents.java`: formato JSON e persistência atômica.
- `src/.../PdfFiles.java`: leitura e exportação com APIs nativas do Android.
- `src/.../PageArt.java`: desenho do papel e dos traços.
- `tests/InkTest.java`: cenários da lógica de escrita, executados na compilação.
- `tests/FolhaInstrumentation.java`: testes de integração no Android.

## Referências técnicas

- [Entrada de caneta e cancelamento de palma — Android Developers](https://developer.android.com/develop/ui/views/touch-and-input/stylus-input/advanced-stylus-features)
- [Mudanças do Android 16 e navegação de volta](https://developer.android.com/about/versions/16/behavior-changes-16)
- [PdfRenderer](https://developer.android.com/reference/android/graphics/pdf/PdfRenderer)
- [Android SDK](https://developer.android.com/studio)

Folha é um projeto independente e não tem afiliação com Goodnotes ou Samsung.

