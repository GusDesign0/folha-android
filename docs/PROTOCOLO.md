# Protocolo de sincronização Folha 1

Transporte: Google Drive API v3, `spaces=appDataFolder`. Todos os clientes OAuth devem pertencer ao mesmo projeto Cloud. Arquivos têm `appProperties.folha=1` e não são compartilhados com outros usuários.

## Blobs imutáveis

`kind=blob`, `sha=<SHA-256 hexadecimal>`. O conteúdo é o PDF original ou uma imagem. A busca pelo hash permite reutilizar um envio existente. Um envio interrompido não altera um blob anterior; possíveis duplicatas idênticas são inofensivas.

## Revisões imutáveis

`kind=rev`, `doc=<UUID>`, `rev=<UUID>`, `parent=<UUID ou vazio>`, `time=<milissegundos UTC com 16 dígitos>`. Conteúdo JSON:

```
{
  "version": 1,
  "revision": "UUID",
  "parent": "UUID ou vazio",
  "document": "JSON do caderno schema 2 como string UTF-8",
  "sha": "SHA-256 dos bytes UTF-8 da string document",
  "assets": {
    "source.pdf": { "file": "ID Drive", "sha": "SHA-256", "size": 1234 }
  }
}
```

Imagens usam chaves `assets/<UUID>.png|jpg|jpeg`. São rejeitados caminhos absolutos/traversais, documentos inválidos, revisões de outro caderno, arquivos acima do limite e hashes divergentes. O manifesto é publicado depois dos blobs. Os downloads são preparados em uma pasta temporária antes de substituir o documento local, salvo de forma atômica.

O estado local é separado pelo identificador Google `sub`. Guarda a revisão de base e uma impressão digital local do conteúdo, ignorando `revision`, `updated` e `lastPage`. A impressão digital não é comparada entre plataformas; o SHA-256 de transporte verifica os bytes do manifesto. Isso evita depender de representações idênticas de ponto flutuante em Java e C#.

## Conflitos

Uma revisão que não é pai de outra revisão conhecida é uma ponta do grafo. Antes de baixar, edições locais são publicadas com sua própria revisão de base. Havendo várias pontas, ordenam-se por `time` e `rev` e a última fica no caderno principal. Cada outra ponta recebe uma cópia de conflito com ID determinístico:

`SHA-256("folha-conflict:" + doc + ":" + rev)`, usando os primeiros 32 dígitos como UUID no padrão 8-4-4-4-12.

A cópia preserva o conteúdo daquela ponta e recebe `originId` do caderno original. Seu ID estável evita cópias repetidas; revisões e blobs anteriores não são apagados. Relógios diferentes podem mudar qual ponta será principal, mas as outras edições permanecem nas cópias. Não há edição colaborativa simultânea nem mesclagem automática de traços.

A instalação local é recusada se o caderno estiver aberto, houver salvamentos pendentes ou o conteúdo local tiver mudado desde a verificação. A atualização fica para outra sincronização, depois de fechar o editor. Esta versão não propaga exclusão de cadernos nem coleta o histórico remoto.

## Exclusão reversível (0.6.1)

A lixeira é uma revisão do mesmo documento com `deleted: true`; usa a cadeia de revisões existente, sem apagar blobs. Restauração publica nova revisão sem a marca. Todos os documentos, inclusive os da lixeira, participam do cálculo de alterações. A interface filtra a marca, evitando reaparecimento após sincronizar. Atualize ambos os dispositivos para preservar a marca. Edições offline concorrentes seguem a política de cópias de conflito; não há limpeza automática nem remoção definitiva.
