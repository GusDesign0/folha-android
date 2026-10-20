# Folha 0.6.0 — validação

## Executado

- Compilação Android release com Gradle 8.13/AGP 8.13.2, Identity Services 22.0.0, redução R8 e assinatura original preservada.
- 21 cenários de tinta e 20 de gestos/formas/régua em Java.
- Integração Android 16 x86_64, incluindo atualização sobre biblioteca da 0.1.0, pressão, palma, desfazer/refazer, salvamento atômico, PDF original com rotação/CropBox/links, laço/texto/imagens/backup, miniaturas limitadas, rotação e atualização GitHub com download/checksum reais.
- Sincronização Java e C#: transferência, PDF reutilizado sem reenvio após editar, preservação de duas edições offline em conflito, repetição sem duplicações, proteção de caderno aberto, backups restaurados como cópia e validação de caminhos.
- Ida e volta com os manifestos reais do protocolo: Java → Windows C# → Java, preservando PDF, Unicode, coordenadas e pressão dos traços.
- Módulo real do editor Windows executado com adaptador DOM/Canvas em teste: caneta, pressão, desfazer/refazer persistidos, cancelamento de borracha, recuperação de falha de salvamento sem duplicar traços, troca de página e espera das confirmações de gravação ao fechar.
- Função real de exportação Windows: três páginas originais, rotação, CropBox, links e escrita em caminhos preservados; PDFs renderizados para inspeção.
- Prévia Windows aberta no WebView2 real; biblioteca/editor carregados. Login Google e primeira sincronização concluídos no PC pelo usuário.

## Limites da validação

O emulador de integração não possui Google Play Services. O login Google e a transferência com o tablet Samsung físico precisam ser autorizados e conferidos no aparelho pelo usuário. Os testes de ida e volta entre linguagens usam transporte simulado com manifestos reais; não representam um teste de dois aparelhos físicos conectados ao Google simultaneamente.

A medição recente do desenho no emulador, com 250 traços de 40 pontos, ficou em aproximadamente 2,2 ms de CPU na mediana e 2,8 ms no percentil 95. Isso não mede a latência da S Pen física nem garante taxa de quadros.

Não foi feito teste em iPad, macOS, Linux, Windows ARM ou com PDF protegido por senha. O Windows ainda é uma prévia com parte das ferramentas do Android. A sincronização funciona enquanto o app está aberto e preserva conflitos em cópias; não é colaboração simultânea.

Os instaladores não contêm a biblioteca do usuário, tokens de conta, senha Google ou chave de assinatura. A configuração OAuth de cliente instalado é distribuída separadamente da autorização pessoal, que permanece protegida no perfil local.

## Validação 0.6.1

- O erro real de abertura foi reproduzido no WebView2 instalado (155). A origem virtual da interface não disparava WebResourceRequested para /data. O acesso aos arquivos passou para origem HTTPS separada, interceptada localmente, com caminhos permitidos e CORS limitado à interface. Comportamento documentado pela Microsoft: https://learn.microsoft.com/en-us/microsoft-edge/webview2/how-to/webresourcerequested
- Teste invisível com perfil separado: PDFs locais de 111, 192 e 23 páginas, primeira e última página renderizadas. Esses documentos pessoais não acompanham o código. PdfQa.cs recebe uma lista de caminhos para reprodução local.
- Editor real executado em adaptador DOM/Canvas: mouse/caneta só navegam; criar/editar/excluir texto; undo/redo; retry sem duplicação; salvar ao fechar; confirmação/lixeira/restaurar. Escrita do tablet preservada.
- C# e Java: exclusão e restauração sincronizadas, repetição idempotente e PDF sem reenvio. Teste Java → C# → Java passou.
- Exportação PDF: três páginas com rotação, corte e links preservados, escrita em caminhos.
- APK 0.6.1 instalado sobre a versão anterior no emulador: biblioteca antiga preservada; gestos, rotação, ferramentas, PDFs e atualização passaram. Exclusão/restauração via salvamento assíncrono de metadados preservou a escrita. CPU draw p50 1,7 ms / p95 2,2 ms em página com 250 traços (não representa latência física da S Pen).
- A conta no tablet físico e dois aparelhos físicos ao mesmo tempo ainda precisam de conferência do usuário.
