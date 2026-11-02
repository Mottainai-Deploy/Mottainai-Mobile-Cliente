# Validação da integração de avisos

Data: 02/11/2026. Escopo: Android Cliente integrado às APIs Cliente e Redis.

## Evidências obtidas

- `assembleDebug` e `testDebugUnitTest` concluídos no código do aplicativo publicado: **111 testes, zero falhas e zero ignorados**.
- Notificação nativa recebida em aparelho físico e confirmada pela usuária.
- Popup de avisos aberto pelo perfil e cancelamento sem alteração conferidos em aparelho.
- Testes de contrato cobrem o cliente Redis, preferências e seleção do destino da notificação.
- A publicação preserva o CI da base e exclui configurações locais, credenciais e arquivos gerados.

## Comportamento entregue

O perfil abre um popup Material para ativar/desativar avisos. Há estado de carregamento durante a sincronização, confirmação e tratamento de falhas. Avisos individuais abrem a oferta e registram sua leitura; agrupamentos abrem o catálogo. O encaminhamento antigo é mantido para notificações já presentes no Android.

## Homologação restante por ambiente

A execução prolongada em segundo plano, as políticas de bateria e a apresentação em tablet exigem validação no aparelho de destino. Os testes Android não comprovam todas as combinações de quota e prioridade do servidor. Conferir SQL compatível, permissões e produtor habilitado antes de demonstrar o envio automático.

Os resultados de CI devem ser consultados nas PRs da publicação; este registro não substitui seus resultados nem declara aprovação de uma execução ainda pendente.

Consulte [configuração e contratos](NOTIFICACOES-REDIS-CLIENTE.md).
