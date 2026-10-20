# Conta e sincronização — prévia 0.6.0

O Folha funciona offline. Conectar uma conta Google é opcional e autoriza somente a área privada de dados do próprio Folha no Google Drive (`drive.appdata`), além do identificador e e-mail da conta. O app não pede acesso geral aos seus arquivos do Drive. Os dados privados não aparecem como uma pasta comum no site do Drive.

## Usar no tablet e no Windows

1. Atualize o Android instalando o APK por cima da versão anterior. Não desinstale o Folha.
2. No Windows, extraia o ZIP inteiro para uma pasta e abra `Folha.exe`. Feche uma prévia anterior antes de abrir a nova. Requer Windows 10/11 de 64 bits, .NET Framework 4.8 e Microsoft Edge WebView2 Runtime. O runtime normalmente acompanha o Edge; se faltar, obtenha-o no site oficial da Microsoft.
3. No tablet, abra **Conta e sincronização → Conectar Google**. Use a mesma conta que conectou no PC. Nesta prévia, o login está habilitado para a conta de teste cadastrada no projeto Google.
4. Feche o caderno e use **Sincronizar agora** antes de trocar de aparelho. A biblioteca receberá os arquivos; abra o mesmo caderno no outro dispositivo.

A sincronização automática funciona enquanto o app está aberto, depois de um período sem novas edições. Pode levar cerca de um minuto; o botão manual permite antecipá-la. Uma atualização remota não substitui um caderno aberto: feche-o para recebê-la. Alterações simultâneas são conservadas em cadernos com o sufixo **conflito**. Escolha depois qual conteúdo deseja continuar editando.

O PDF original e as imagens são identificados pelo SHA-256 e enviados uma vez. Novas revisões transferem o JSON das anotações e referências aos arquivos. A escrita e o salvamento locais não aguardam a rede. As revisões remotas são preservadas; esta prévia ainda não faz limpeza automática do histórico nem propaga exclusão de cadernos.

Os dados ocupam a cota da sua conta Google. Sem internet, sem permissão ou sem espaço no Drive, as edições continuam locais e o app informa a falha. Mantenha backups `.folha` de arquivos importantes: sincronização não substitui uma cópia independente.

## Conta e privacidade

O Windows abre o navegador padrão para o login, usa PKCE e guarda a autorização protegida pelo usuário do Windows (DPAPI). O Android usa os serviços oficiais do Google Play e mantém o token de acesso apenas em memória. O Folha não recebe sua senha Google.

**Desconectar** remove a conexão local; os cadernos permanecem no aparelho. Para revogar também a autorização no Google, use [Conexões da Conta Google](https://myaccount.google.com/connections). Uma biblioteca já vinculada não é enviada automaticamente para outra conta Google.

O projeto OAuth está em modo de teste. O Google pode pedir nova autorização periodicamente. Não há servidor Folha intermediando seus arquivos. A criptografia de transporte e o armazenamento na nuvem são fornecidos pelo Google; não há criptografia ponta a ponta própria do Folha.

## Escopo da primeira versão Windows

Biblioteca local, PDFs, escrita com mouse/caneta e pressão, tipos de caneta, marca-texto, borracha de traço inteiro, texto, desfazer/refazer, páginas de cadernos, zoom, backup editável e exportação em PDF com caminhos. Imagens e textos vindos do tablet são preservados e exibidos. Laço, régua, criação de imagens, miniaturas e formas automáticas continuam disponíveis no Android; ainda não têm interface equivalente no Windows.

O formato portátil continua sendo `.folha`, versão 2. A versão Windows atual não é para macOS, Linux ou iPadOS.
