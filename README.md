# Folha — Android e Windows

Cadernos e anotações em PDF, com foco em tablets Samsung/S Pen e continuidade no computador.

**[Android: baixar APK](https://github.com/GusDesign0/folha-android/releases/latest/download/Folha.apk)** · **[Windows: baixar ZIP](https://github.com/GusDesign0/folha-android/releases/latest/download/Folha-Windows.zip)** · [ZIP para envio ao tablet](https://github.com/GusDesign0/folha-android/releases/latest/download/Folha.zip) · [Versões](https://github.com/GusDesign0/folha-android/releases)

## Começar

No tablet, abra o APK pelo **Meus Arquivos** e escolha **Atualizar** por cima da versão anterior. Não desinstale o app, para preservar sua biblioteca. Requer Android 10 ou superior, com alvo Android 16. É um APK independente, fora da Play Store.

No Windows, extraia **todo o ZIP** para uma pasta e abra **Folha.exe**. Feche uma prévia anterior antes de abrir este pacote. Requer Windows 10/11 x64, .NET Framework 4.8 e Edge WebView2 Runtime.

No tablet, use **Conta e sincronização → Conectar Google**, com a mesma conta conectada no PC. Feche o caderno antes de usar **Sincronizar agora** e trocar de aparelho. A sincronização automática ocorre enquanto o app está aberto; o trabalho offline continua disponível.

**Prévia 0.6.0:** login habilitado para a conta de teste cadastrada no projeto Google. Edições simultâneas são preservadas em cópias de conflito. A sincronização real no tablet precisa da autorização do usuário e Google Play Services. [Como funciona, privacidade e limitações](docs/SINCRONIZACAO.md).

## O que há no app

- Android: S Pen com pressão, rejeição de palma, três tipos de caneta, marca-texto, suavização, borracha parcial ou de traço inteiro, laço, texto e imagens.
- Dois dedos para desfazer, formas ao segurar a ponta, régua móvel/giratória e rotação retrato/paisagem.
- Miniaturas e marcadores; mover, duplicar e excluir páginas de cadernos. PDFs mantêm sua ordem original.
- Biblioteca com pastas, salvamento local e backup editável `.folha`.
- Exportação de PDF preservando o original e acrescentando escrita em caminhos.
- Windows: biblioteca, PDF, canetas/pressão, marca-texto, borracha de traço inteiro, texto, desfazer/refazer, páginas, zoom, backups, exportação e sincronização. Imagens vindas do tablet são exibidas e preservadas. Ferramentas avançadas do Android ainda não têm todas suas interfaces no Windows.

O Android mantém **Atualizar app**, que busca a versão mais recente no GitHub, verifica tamanho/SHA-256/assinatura e abre a confirmação de instalação do sistema. A verificação só ocorre quando solicitada. O pacote Windows é atualizado baixando o novo ZIP; os cadernos ficam no perfil do Windows, fora da pasta do executável.

## Escrita e sincronização

A escrita e o salvamento funcionam localmente, sem aguardar o Google. PDF/imagens são enviados separadamente das revisões de anotações e reutilizados por SHA-256. Uma edição remota não substitui um caderno aberto. Guarde backups independentes dos arquivos importantes.

Os testes incluem atualização sobre cadernos da 0.1.0, gestos, PDF com rotação/corte/links, desenho com pressão, conflitos e ida e volta do protocolo Java → C# → Java. [Validação e limites](docs/VALIDACAO.md).

## Código

[Compilar Android e Windows](docs/BUILD.md) · [Formato `.folha`](docs/FORMATO.md) · [Protocolo de sincronização](docs/PROTOCOLO.md).

Chaves de assinatura e autorizações pessoais não acompanham o código. Dependências e suas licenças estão descritas nos avisos incluídos em cada aplicativo. Esta versão Windows não é uma versão para macOS, Linux ou iPadOS.
