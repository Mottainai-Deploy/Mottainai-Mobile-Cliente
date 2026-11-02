# Operacional — ajustes e validações para entrega mobile

Revisão em 01/11/2026 do [código publicado no commit 006c691](https://github.com/Mottainai-One/Mottainai-Mobile-Operacional/tree/006c691d5836d09db2ca3799b936cd6758e8baf1). Este documento descreve trabalho para a equipe responsável pelo Operacional. Não altera código, regras Firebase ou dados do ambiente.

## 1. Prioridade alta: alinhar a consulta de equipe à autorização

### Evidência

- [UserRepository.listTeam](https://github.com/Mottainai-One/Mottainai-Mobile-Operacional/blob/006c691d5836d09db2ca3799b936cd6758e8baf1/app/src/main/java/com/mottainai/operacional/repository/UserRepository.java#L30) consulta `users` com `whereEqualTo("storeID", storeId)`.
- [firestore.rules](https://github.com/Mottainai-One/Mottainai-Mobile-Operacional/blob/006c691d5836d09db2ca3799b936cd6758e8baf1/firestore.rules#L7) permite leitura em `users/{userId}` somente quando `request.auth.uid == userId`.
- A consulta é utilizada por `TeamFragment` e `HomeFragment`.

### Problema e limites da constatação

Se as regras implantadas forem iguais às versionadas, a consulta por loja não garante que só o documento do usuário autenticado será retornado. Ela pode ser negada integralmente. Isso é uma inferência da comparação de código; a falha não foi reproduzida no ambiente nesta revisão. [Regras Firestore não filtram resultados](https://firebase.google.com/docs/firestore/security/rules-query#rules_are_not_filters).

### O que a equipe precisa fazer

1. Confirmar quais regras estão implantadas e reproduzir a listagem com gerente/dono.
2. Escolher uma fonte autorizada para a equipe: endpoint da API SQL, se houver contrato correspondente, ou coleção Firestore específica com campos e regras adequados para listar membros da própria loja.
3. Manter a leitura do perfil individual separada da listagem. Não liberar a coleção inteira de usuários nem incluir campos pessoais desnecessários.
4. Alinhar `storeID`/`storeKey`, tipo do identificador e aliases de perfil entre token, consulta e regras.
5. Verificar se faltam índices para a consulta escolhida.

### Critérios de aceite

- Gerente/dono visualiza somente os membros permitidos da própria loja.
- Trocar o identificador da loja não permite consultar outra unidade.
- Perfil não autorizado recebe erro de acesso, não dados de outra loja.
- O app diferencia lista vazia de consulta recusada e permite recuperar o erro.
- Há teste de autorização no emulador Firebase ou na API escolhida, antes de publicar regras.

Responsabilidade: mobile Operacional + responsável pela API/Firebase. Não publicar regras em produção sem revisão.

## 2. Prioridade alta: comprovar persistência Firebase com dados de negócio

Essa demanda já possui implementação; o trabalho principal é validar e registrar a evidência.

- [PosApprovalsFragment: leitura em tempo real](https://github.com/Mottainai-One/Mottainai-Mobile-Operacional/blob/006c691d5836d09db2ca3799b936cd6758e8baf1/app/src/main/java/com/mottainai/operacional/fragments/PosApprovalsFragment.java#L85).
- [Gravação da decisão](https://github.com/Mottainai-One/Mottainai-Mobile-Operacional/blob/006c691d5836d09db2ca3799b936cd6758e8baf1/app/src/main/java/com/mottainai/operacional/fragments/PosApprovalsFragment.java#L152).
- [Regras das aprovações](https://github.com/Mottainai-One/Mottainai-Mobile-Operacional/blob/006c691d5836d09db2ca3799b936cd6758e8baf1/firestore.rules#L115).

### Roteiro de teste

1. No ambiente de demonstração, produzir uma solicitação válida do PDV. O mobile não cria esses documentos: as regras bloqueiam criação pelo app.
2. Entrar no Operacional com gerente/dono da mesma loja.
3. Conferir a solicitação pendente e aprovar ou rejeitar.
4. Mostrar que `status`, `decisionUid` e `decisionAt` foram persistidos no documento Firestore.
5. Reiniciar o app e confirmar que o estado continua consistente.
6. Tentar repetir a decisão e tentar acessar a solicitação com conta de outra loja. As regras devem impedir as operações indevidas.

Não executar cancelamento, estorno ou devolução de venda real para gerar evidência acadêmica. O registro da decisão no Firestore também não prova, sozinho, que o PDV aplicou a operação financeira; essa parte exige teste da integração correspondente.

Firebase Authentication e SharedPreferences não devem ser usados como a única evidência dessa demanda. Não é necessário acrescentar SQLite apenas para cumprir a alternativa “Firebase ou SQLite”, se o professor aceitar esta implementação Firestore.

## 3. Prioridade alta: validar push FCM de ponta a ponta

### O que já existe

- [Registro do dispositivo no Firestore](https://github.com/Mottainai-One/Mottainai-Mobile-Operacional/blob/006c691d5836d09db2ca3799b936cd6758e8baf1/app/src/main/java/com/mottainai/operacional/repository/NotificationRepository.java#L53).
- [Recepção de mensagens FCM](https://github.com/Mottainai-One/Mottainai-Mobile-Operacional/blob/006c691d5836d09db2ca3799b936cd6758e8baf1/app/src/main/java/com/mottainai/operacional/notifications/MottainaiFirebaseMessagingService.java).
- [Notificação nativa, deduplicação e destino](https://github.com/Mottainai-One/Mottainai-Mobile-Operacional/blob/006c691d5836d09db2ca3799b936cd6758e8baf1/app/src/main/java/com/mottainai/operacional/notifications/NotificationRouter.java#L85).
- [Teste instrumentado do roteador](https://github.com/Mottainai-One/Mottainai-Mobile-Operacional/blob/006c691d5836d09db2ca3799b936cd6758e8baf1/app/src/androidTest/java/com/mottainai/operacional/DeviceNotificationSmokeTest.java).

### O que falta comprovar

O teste cria um `RemoteMessage` local. Não comprova o envio por um servidor e a entrega pelo FCM.

1. Conferir login, claims de loja/perfil e gravação de `posNotificationDevices` no ambiente de teste.
2. Confirmar que a configuração Firebase do app corresponde ao projeto utilizado pelo emissor. Não copiar credenciais administrativas para o Android.
3. Enviar uma mensagem real pelo serviço responsável e verificar os campos esperados pelo roteador (`notificationType`, `notificationId`, título, corpo e identificador da entidade, quando aplicável).
4. Validar recebimento com o app aberto e em segundo plano, permissão concedida e preferências habilitadas.
5. Tocar na notificação e conferir a entidade e a autorização de acesso, não apenas a abertura da tela.
6. Verificar deduplicação, troca de usuário e limpeza do registro do dispositivo ao sair.
7. Não confundir app removido da lista de recentes com aplicativo explicitamente forçado a parar nas configurações do Android.

Responsabilidade: mobile Operacional + serviço emissor das notificações. Preparar screenshot ou vídeo do push real.

## 4. Prioridade alta: apresentar celular e tablet com evidência visual

### O que já existe

- [Dimensões para tablet](https://github.com/Mottainai-One/Mottainai-Mobile-Operacional/blob/006c691d5836d09db2ca3799b936cd6758e8baf1/app/src/main/res/values-sw580dp/dimens.xml).
- [Dimensões para largura mínima de 600 dp](https://github.com/Mottainai-One/Mottainai-Mobile-Operacional/blob/006c691d5836d09db2ca3799b936cd6758e8baf1/app/src/main/res/values-sw600dp/dimens.xml).
- [Ajustes para tablet em paisagem](https://github.com/Mottainai-One/Mottainai-Mobile-Operacional/blob/006c691d5836d09db2ca3799b936cd6758e8baf1/app/src/main/res/values-sw580dp-land/dimens.xml).

### Matriz mínima

| Área | Validar |
|---|---|
| Login | Campos e botão acessíveis com teclado aberto; sem cortes em paisagem. |
| Início e navegação | Cartões, atalhos e barra de navegação visíveis e utilizáveis. |
| Produtos e inventário | Listas, formulários e ações alcançáveis; texto ampliado sem sobreposição. |
| Scanner | Prévia na orientação correta, permissão negada e retorno ao fluxo. |
| Chat | Compositor acima do teclado e dos botões; histórico preservado ao girar. |
| Perfil e aprovações | Dados e ações legíveis em celular e tablet; nenhuma ação crítica fora da área alcançável. |

Testar retrato, paisagem e recriação da Activity. Corrigir os problemas encontrados e guardar capturas identificadas com dispositivo, orientação e versão do app. A existência de arquivos `sw600dp` não substitui essa validação.

## 5. Chatbot operacional: confirmar execução com o contrato atual

- [ChatAiFragment](https://github.com/Mottainai-One/Mottainai-Mobile-Operacional/blob/006c691d5836d09db2ca3799b936cd6758e8baf1/app/src/main/java/com/mottainai/operacional/fragments/ChatAiFragment.java).
- [AiApiService](https://github.com/Mottainai-One/Mottainai-Mobile-Operacional/blob/006c691d5836d09db2ca3799b936cd6758e8baf1/app/src/main/java/com/mottainai/operacional/network/AiApiService.java).
- [Teste instrumentado do chat](https://github.com/Mottainai-One/Mottainai-Mobile-Operacional/blob/006c691d5836d09db2ca3799b936cd6758e8baf1/app/src/androidTest/java/com/mottainai/operacional/DeviceChatSmokeTest.java).

Validar token operacional aceito pela IA, pergunta relacionada à loja, resposta real, histórico após sair e voltar, falha de rede e tentativa novamente. Conferir isolamento por usuário/loja e garantir que a conversa anterior não apareça ao trocar de conta. Não declarar o teste aprovado apenas porque o arquivo existe.

## 6. Controle de acesso: testar interface e serviço

[RoleHelper](https://github.com/Mottainai-One/Mottainai-Mobile-Operacional/blob/006c691d5836d09db2ca3799b936cd6758e8baf1/app/src/main/java/com/mottainai/operacional/utils/RoleHelper.java) centraliza parte das permissões visuais. Isso não substitui autorização na API ou nas regras Firebase.

Validar estoquista, gerente e dono; perfil ausente/desconhecido; token expirado; acesso direto a uma tela restrita; tentativa de usar identificador de outra loja. Definir a matriz de permissões e conferir que o backend recusa o que a interface não permite. Revisar especialmente permissões permissivas quando o perfil está ausente, como o retorno de `canViewSuggestions(null)`.

## 7. Pendência funcional adicional: convite de equipe

[InviteMemberFragment](https://github.com/Mottainai-One/Mottainai-Mobile-Operacional/blob/006c691d5836d09db2ca3799b936cd6758e8baf1/app/src/main/java/com/mottainai/operacional/fragments/InviteMemberFragment.java#L20) identifica o fluxo de convite como pendente de endpoint. Não declarar essa funcionalidade completa sem integrar um contrato real.

Se fizer parte do escopo da entrega, alinhar com a pessoa da API a criação do vínculo e a autenticação Firebase. Não criar senhas ou acesso operacional apenas por inserir um funcionário no banco, sem o fluxo de autenticação definido pelo projeto. Esse item é adicional à lista acadêmica de nove demandas.

## 8. Ambiente e material para os professores

- Revisar [QA-ENVIRONMENT.md](https://github.com/Mottainai-One/Mottainai-Mobile-Operacional/blob/006c691d5836d09db2ca3799b936cd6758e8baf1/docs/QA-ENVIRONMENT.md), pois a configuração documentada usa portas de uma stack QA específica, diferentes das portas locais de outras stacks.
- Definir uma stack e registrar URLs, portas e versão do APK efetivamente demonstrado. Em aparelho por USB, conferir o encaminhamento das portas configuradas; para execução remota, usar serviços acessíveis ao aparelho.
- Disponibilizar contas de teste por perfil em ambiente autorizado, sem compartilhar chaves administrativas, tokens ou arquivos de ambiente na entrega.
- Atualizar os links para o commit final após as correções. Publicar a mesma versão que será instalada na apresentação.
- Confirmar com o professor se as demandas podem ser distribuídas entre Cliente e Operacional. Não adicionar todas as tecnologias a ambos os apps sem essa necessidade.

## Divisão recomendada das nove demandas

| Demanda | Aplicativo que oferece a evidência principal |
|---|---|
| Controle de acesso | Ambos; Operacional demonstra perfis de funcionário. |
| Hardware | Operacional: câmera e leitura de código. |
| API | Ambos; Operacional também consulta Open Food Facts. |
| Persistência Firebase/SQLite | Operacional: decisões de solicitações do PDV em Firestore. |
| Chatbot operacional | Operacional. |
| Avatar | Cliente: foto no Cloudinary e URL vinculada ao perfil. |
| Notificações | Operacional: FCM e notificações nativas. |
| Celular e tablet | Ambos, com evidência visual. |
| GPS | Cliente: aquisição da localização do aparelho. |

Essas evidências não confirmam offline completo, sincronização de operações pendentes ou notificações por proximidade no Cliente. São requisitos distintos e não devem ser anunciados como concluídos sem implementação e testes correspondentes.
