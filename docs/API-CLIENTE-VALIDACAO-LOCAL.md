# Validação local da API Cliente

> Registro histórico da validação de 31/10/2026. Os bloqueios descritos abaixo refletem a API daquela execução e não devem ser interpretados como pendências confirmadas da API atual. O app atual já lê e salva `avatarUrl` no perfil e usa o contrato de histórico de pontos. Consulte o guia de execução e realize nova validação no ambiente de destino.

Data da validação: 31/10/2026
Ambiente: `http://127.0.0.1:8081` (`mottainai-local-api-cliente-1`)

## Escopo

Foram comparadas as rotas publicadas em `/v3/api-docs` com os serviços Retrofit
utilizados pelo app Cliente. Os testes não criaram nem alteraram perfis, ofertas,
pontos ou geocercas.

## Testes executados

| Área | Chamada | Resultado |
|---|---|---|
| Diagnóstico de erros | `Test-ApiErrors.ps1` | Aprovado: token de redefinição inválido retorna `400`; perfil Firebase sem token retorna `401`; `/error` retorna `401`. |
| Validação Firebase | `Test-FirebaseValidation.ps1` | Aprovado: JWT propositalmente inválido foi rejeitado com `401` em 1,62 s. |
| Login legado inválido | `POST /api/v1/customers/auth/login` | `401` em 1,41 s. |
| Redefinição inválida | `POST /api/v1/customers/auth/password-reset` | `400` em 1,07 s. |
| Recuperação com e-mail inválido | `POST /api/v1/customers/auth/password-recovery` | `400` em 7,95 s. |
| Rotas protegidas sem token | Perfil (`GET`/`POST`/`PUT`), saldo, lista e detalhe de lojas, lista e detalhe de promoções, geocercas (`GET`/`POST`/`DELETE`) e perfil legado | Todas retornaram `401`; nenhuma operação foi executada. |
| Documentação | `GET /v3/api-docs` | Disponível (`200`). |

As chamadas autenticadas ainda precisam de uma execução local de
`diagnostics/Test-ClientLogin.ps1`, com a senha inserida diretamente no PowerShell.
Não registrar nem enviar token ou senha por chat, log ou documento.

## Bloqueios para o app Cliente

### 1. Histórico de pontos não existe na API publicada

O app chama:

```text
GET /api/v1/client/loyalty/transactions?from=YYYY-MM-DD&to=YYYY-MM-DD
```

Essa rota não aparece em `/v3/api-docs`. A API publica somente
`GET /api/v1/client/loyalty/balance`.

Impacto: a tela de saldo pode funcionar, mas o histórico de movimentações de
pontos não tem contrato de backend e falhará quando houver sessão válida.

**Solicitação para a API:** publicar `GET /api/v1/client/loyalty/transactions`,
protegida por `FirebaseBearer`, com filtros obrigatórios `from` e `to`, janela
máxima de seis meses e retorno pertencente ao usuário Firebase autenticado.

### 2. Resposta de perfil não traz dados que o app precisa exibir

`GET /api/v1/client/auth/profile` retorna apenas `id`, `birthYear`, `active`,
`marketingConsent`, `createdAt` e `updatedAt`.

Não há `fullName`, `phone` nem endereço. O e-mail pode continuar vindo do Firebase,
mas nome, telefone e endereço não podem ser recuperados em outro aparelho.

**Solicitação para a API:** devolver os dados próprios do cliente. Foto de perfil
não faz parte desse contrato.

### 3. Foto de perfil não deve depender da API Cliente nem do SQL

O app envia a imagem diretamente ao Cloudinary. Não haverá coluna de foto, arquivo
binário, rota exclusiva nem credencial do Cloudinary na API Cliente/SQL.

**Decisão do MVP:** o Android deve manter apenas a referência pública necessária para
exibir a imagem e usar o Cloudinary como origem. O `API Secret` nunca vai ao Android.

### 4. Catálogo usa fonte externa de imagens por decisão do MVP

`CustomerCatalogPromotionResponse` e seus `Item` publicados não fornecem
`imageUrl`. Isso é esperado: o banco atual não recebe imagens de produto.

**Decisão do MVP:** o Android resolve a imagem com uma API externa, aplica cache e
placeholder e não pede alteração de tabela ou rota à API Cliente.

### 5. Geocercas usam autenticação incompatível com Firebase

As rotas publicadas são:

```text
GET    /api/v1/customers/{customerId}/geofences
POST   /api/v1/customers/{customerId}/geofences
DELETE /api/v1/customers/{customerId}/geofences/{geofenceId}
```

Elas usam cliente/token legado, mas o app Cliente atual autentica as funcionalidades
novas exclusivamente por Firebase.

**Solicitação para a API:** criar equivalentes sob `/api/v1/client/`, protegidas por
`FirebaseBearer`, inferindo o cliente pelo token e sem aceitar `customerId` na URL.

### 6. Notificações próximas não têm contrato de entrega

Para avisos em segundo plano faltam contratos para registrar/remover token FCM,
salvar preferência de avisos e respeitar o limite de até três produtos diferentes por
loja/dia. Geocerca sozinha não entrega notificação.

**Solicitação para a API:** definir contrato de dispositivos/notificações com token
FCM, consentimento, raio de 5 ou 10 km, controle de frequência e prioridade por
pontos. O disparo deve ocorrer no backend.

### 7. Assistente: stack local disponível

O app cria o cliente de conversa quando `MOTTAINAI_ASSISTANT_BASE_URL` está
preenchida. O stack local inclui a API NoSQL em `http://127.0.0.1:8082` e o serviço
de IA em `http://127.0.0.1:8000`.

Validações locais: `GET /livez` e `GET /readyz` da IA retornaram `200`; o
`POST /chat` com corpo válido e sem token retornou `401`, confirmando que a rota está
protegida. Uma chamada real do app Cliente validou o token Firebase no Google, mas
recebeu `403`: a IA ainda tenta resolver qualquer usuário Firebase como funcionário
operacional.

**Alteração necessária na API IA:** em `app/security/auth.py`, o fluxo de token
Firebase chama incondicionalmente `resolve_firebase_identity`. Essa função em
`app/database/identity.py` exige registros ativos em `app_user`, `employee`,
`employee_role`, `retail_store` e `company`. Um cliente não possui esse vínculo por
definição. A IA deve criar um fluxo específico de cliente, por exemplo
`resolve_firebase_customer_identity`, que busque o perfil do Cliente pelo UID Firebase
no contrato/banco correto e construa um contexto com papel `CLIENTE`, sem inicializar
o contexto RLS de funcionário. O chat deverá limitar ferramentas e dados conforme o
papel de cliente.

A primeira inicialização pode levar cerca de dois minutos para carregar o modelo de
embeddings em CPU.

Para um celular físico, manter o encaminhamento USB ativo antes de abrir o app:

```powershell
adb reverse tcp:8000 tcp:8000
```

Impacto: o atalho da Lô abre a tela, mas envio, histórico e sessões não podem ser
testados neste ambiente.

**Solicitação para a infraestrutura/IA:** informar uma URL de desenvolvimento do
assistente, contratos de `POST /chat`, `GET /chat/history/{sessionId}` e
`GET /chat/sessions`, além do método de autenticação Firebase aceito por esse serviço.

## Ajustes de contrato recomendados

| Rota | Ajuste solicitado |
|---|---|
| `POST /api/v1/client/auth/profile` | Retornar `201 Created` ao criar o perfil, com resposta completa. Hoje a documentação declara `200`. |
| `GET /api/v1/client/auth/profile` | Adicionar nome, telefone e endereço; manter e-mail no Firebase. |
| `GET /api/v1/client/loyalty/transactions` | Implementar e documentar; já é chamado pelo app. |
| Geocercas | Criar versões com `FirebaseBearer`, sem `customerId` exposto. |
| Notificações | Criar contrato de dispositivo FCM e regras de frequência/prioridade. |
| Assistente | Validar com token Firebase de cliente e confirmar o vínculo desse UID com o perfil/identidade esperada pela IA. |

## Compatibilidades confirmadas

- Firebase é exigido corretamente nas rotas novas de perfil, pontos e catálogo.
- Catálogo suporta `page`, `size`, `query`, coordenadas e `radiusKm`; o limite
  do app, 5 ou 10 km, está dentro do máximo publicado de 500 km.
- Ofertas retornam itens com nome, preço original, preço promocional e quantidade.
- Datas de promoção são `date-time` ISO-8601; o app deve formatá-las antes de
  exibir, sem mostrar valor técnico bruto ao cliente.
- ViaCEP respondeu `200` para a consulta externa de CEP usada no cadastro.

## Observação do ambiente local

- A API Cliente responde às rotas de catálogo e perfil; sem token Firebase elas
  retornam `401`, comportamento esperado. Para o APK de depuração alcançar
  `http://127.0.0.1:8081`, o Manifest de debug libera HTTP local e o celular físico
  precisa de `adb reverse tcp:8081 tcp:8081` a cada reconexão USB.
- A API Operacional respondeu `503` em `/actuator/health` porque a conexão SMTP local
  foi recusada. Ela não é consumida pelo catálogo do app Cliente, mas o ambiente
  Operacional precisa de SMTP configurado ou de health de e-mail desabilitado no perfil
  local para ficar saudável.
