# Correções das imagens do aparelho — 26/10/2026

As marcações enviadas pelo usuário substituem o comportamento correspondente do protótipo. O layout continua nativo Java/XML, nos temas claro e escuro.

## Pontos corrigidos

- Imagem 1: validação em texto abaixo do campo, sem popup cobrindo a senha; botão de mostrar/ocultar senha e campos de acesso sem preenchimentos fictícios.
- Imagem 2: botão de saída com vermelho e texto branco legível nos dois temas.
- Imagem 3: o fluxo normal solicita a permissão de notificações do Android e não abre a tela que simulava essa permissão.
- Imagem 4: foto circular, seletor de tema com estado selecionado acessível e botões com altura mínima consistente.
- Imagem 5: o cartão de cálculo fictício é substituído por estado sem dados, pois a API ainda não está pronta.
- Imagem 6: checkbox nativo marcado, com seleção persistente.
- Imagem 7: controle independente para mostrar/ocultar cada senha, mantendo o cursor.
- Imagens 8–11, 13 e 18: painéis abrem por cima da tela existente, com escurecimento transparente. Não substituem a tela por uma página cinza/branca.
- Imagem 9: largura e altura iguais na foto; recorte circular de imagens horizontais/verticais. Novas fotos importadas respeitam a orientação EXIF.
- Imagem 12: valores dos pontos alinhados na borda direita e centralizados na linha. No uso normal, histórico sem dados não publica os registros fictícios do Figma.
- Imagem 13: alterar o raio mantém as opções de 5, 10, 15 e 20 km, sem reescrever a opção de 5 km com o raio salvo.
- Imagem 14: mapa interativo real do OpenStreetMap, centralização na localização autorizada do aparelho e ação para abrir no Maps. Sem marcador inventado de loja parceira.
- Imagem 15: mensagens com largura pelo conteúdo, alinhamento à direita/esquerda e cantos próprios. Ao enviar, a apresentação e sugestões iniciais deixam o espaço da conversa.
- Ajuste posterior do chat: “Minhas conversas” fica no canto direito do cabeçalho, com área mínima de toque de 48 dp. Cada mensagem mostra seu autor: “Você” à direita e “Lô” à esquerda, também nos temas claro/escuro e com fonte ampliada.
- Imagem 16: histórico lateral com a conversa anterior visível sob o escurecimento da área externa.
- Imagens 17 e 19: ofertas, sugestões comerciais, preços, saldo e identidade fictícios removidos do fluxo normal. Estados vazios permanecem até a integração.

Os XMLs com dados originais do Figma continuam disponíveis para comparação visual em testes debug com o argumento `design_preview`. O fluxo normal não ativa essa opção. Release ignora o argumento.

## Integrações e limites

O usuário confirmou que a API do cliente ainda não está pronta. Não foram inventados contratos ou respostas remotas. Autenticação, ofertas, lojas parceiras, saldo, economia, histórico e respostas da Lô ainda precisam dos serviços correspondentes. A conversa informa que a Lô ainda não está conectada. A conta e a foto salvas no aparelho continuam locais.

O mapa usa a [incorporação do OpenStreetMap](https://www.openstreetmap.org/export), com atribuição do provedor. Sem permissão de localização, mostra uma visão geral sem simular a posição do usuário; a centralização exige autorização. A abertura externa usa o [intent geográfico do Android](https://developer.android.com/guide/components/intents-common#Maps). A solicitação de notificações segue a [permissão nativa do Android 13 ou superior](https://developer.android.com/develop/ui/compose/notifications/notification-permission); concedê-la não implementa o serviço de entrega ou geofencing.

## Validação

`ClientCorrectionsTest` cobre os problemas das capturas na interface real. `DesignFlowTest` mantém a verificação das transições do protótipo. `DesignLayoutTest` mede os 115 layouts a 320/600 dp, em ambos os temas e fonte em 130%.

As capturas da rodada ficam no cache ignorado `.design-cache/phone-corrections`: 16 telas e painéis em cada tema, totalizando 32 imagens da janela do Android. As primeiras imagens cobertas por um alerta de ANR do System UI do emulador foram descartadas; a revisão foi repetida depois de reiniciar esse serviço. O teste de captura passa a exigir que a janela do aplicativo tenha foco.

A rodada completa passou em 19 testes, sem falhas. Depois dos ajustes finais de mapa e estados vazios, quatro testes focados também passaram, incluindo a permissão nativa de notificações, abertura de mapa e novas capturas. São 20 testes distintos entre as execuções. As 460 combinações de layout passaram; `assembleDebug`, `lintDebug` e `git diff --check` concluíram sem erros. O lint mantém 1448 avisos que não foram tratados integralmente nesta mudança.

Na tentativa de repetir a suíte integral antes da publicação, o último registro foi de 20 dos 23 testes concluídos, sem falhas até esse ponto. O processo e o emulador foram interrompidos antes do relatório final; essa execução não é considerada uma suíte completa aprovada.

## Instalação

O APK final foi instalado por USB no aparelho modelo 2311DRK48G com `adb install -r`, preservando os dados. O comando retornou `Success`. O SHA-256 lido do APK instalado no aparelho corresponde à compilação: `28aab763048d8baaf8e5a011d876bca9f58980a70641879ebc6e82daaedf2dcb`.

Após o ajuste posterior do chat, três testes focados passaram, incluindo a verificação a 320 dp com fonte em 130% nos dois temas. Essa versão também foi instalada por USB, com `Success`; seu SHA-256 é `c1e8d96027bd6596364ae6f5823b19ba52cda309da4db52cd0301127bc900280` e corresponde ao APK instalado.

Os testes automatizados foram executados no emulador. A instalação física não substitui a validação dos serviços remotos, que continuam indisponíveis enquanto a API não estiver pronta. A entrega para revisão usa a branch `feat/app-cliente-figma`.
