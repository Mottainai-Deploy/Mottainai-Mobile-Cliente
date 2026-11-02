# Guia de execução do Mottainai Cliente

Este documento explica o que deve estar configurado para o aplicativo Cliente funcionar de ponta a ponta: autenticação, catálogo, perfil, foto, mapas e assistente Lô.

## 1. Visão geral

O aplicativo Android não hospeda dados. Ele usa Firebase somente para autenticação e consulta os serviços abaixo para os dados e funcionalidades.

| Serviço | Responsabilidade | Porta local |
| --- | --- | --- |
| API Cliente SQL | Perfil Firebase, lojas, catálogo, promoções, pontos e geocercas | `8081` |
| API Operacional SQL | Operação do parceiro/PdV e dados operacionais | `8080` |
| API NoSQL | Conversas, histórico e dados não relacionais de apoio | `8082` |
| API IA | Assistente Lô | `8000` |
| PostgreSQL | Banco dos dados SQL | interna no Docker / cloud |
| MongoDB | Banco de conversas e apoio da IA/NoSQL | `27018` no computador |
| Redis | Cache e limite de tráfego da IA | `6379` no computador |

Para o uso normal do Cliente, a API Cliente é obrigatória. Para o chat funcionar, também são obrigatórias a API IA, API NoSQL, MongoDB e Redis. A API Operacional é necessária para os fluxos operacionais e para a consistência dos dados compartilhados, embora o Cliente consuma prioritariamente a API Cliente.

## 2. Pré-requisitos no computador de desenvolvimento

Instale e deixe disponíveis:

- Android Studio e Android SDK Platform Tools (`adb`);
- Docker Desktop aberto, usando containers Linux;
- JDK compatível com o Android Gradle Plugin (preferencialmente JDK 17);
- acesso à internet: Firebase, Cloudinary, Google Maps, ViaCEP, Open Food Facts e o provedor da IA precisam de rede;
- cabo USB com Depuração USB autorizada, se o teste for em celular físico.

Os diretórios usados neste computador são:

```text
C:\Users\isabellagimenes-ieg\StudioProjects\Mottainai-Mobile-Cliente
C:\Users\isabellagimenes-ieg\OneDrive - Instituto J&F\Documentos\Mottainai-Codex\local-stack
```

## 3. Variáveis privadas

Nunca envie chaves, senhas, `google-services.json` ou arquivos `.env` para o Git, chat público ou PR.

Crie um arquivo privado chamado `cloud.env` dentro de `local-stack`. Ele deve estar ignorado pelo Git e conter as credenciais recebidas da equipe de banco:

```env
DB_URL=jdbc:postgresql://<host-cloud>:<porta>/<banco>?sslmode=require
DB_USERNAME=<usuario-da-api>
DB_PASSWORD=<senha-da-api>
IA_POSTGRES_DSN=postgresql+asyncpg://<usuario>:<senha>@<host-cloud>:<porta>/<banco>?ssl=require
```

Use os valores reais entregues pela equipe. Não substitua o banco cloud por valores fictícios.

## 4. Como ligar as APIs para teste local

Abra o PowerShell em `local-stack` e execute:

```powershell
docker --context desktop-linux compose --env-file cloud.env -f compose.yaml -f compose.cloud.yaml up -d
```

Confira o estado:

```powershell
docker --context desktop-linux ps
```

Os containers esperados são:

```text
mottainai-local-api-cliente-1
mottainai-local-api-operacional-1
mottainai-local-api-nosql-1
mottainai-local-ia-1
mottainai-local-db-1
mottainai-windows-mongo
mottainai-windows-redis
```

Valide as APIs pelo computador:

```powershell
Invoke-WebRequest http://127.0.0.1:8081/v3/api-docs
Invoke-WebRequest http://127.0.0.1:8082/v3/api-docs
Invoke-WebRequest http://127.0.0.1:8000/livez
```

Resultados esperados:

- API Cliente: `200`;
- API NoSQL: `200`;
- IA `/livez`: `200`;
- API Operacional pode devolver `401` em rotas protegidas; isso prova que está de pé, mas exige autenticação.

Para desligar os serviços:

```powershell
docker --context desktop-linux compose --env-file cloud.env -f compose.yaml -f compose.cloud.yaml down
```

Para ver falhas do assistente:

```powershell
docker --context desktop-linux logs --tail 150 mottainai-local-ia-1
```

## 5. Conectar o celular físico às APIs locais

O endereço `127.0.0.1` dentro do celular é o próprio celular, não o computador. Por isso, com USB, é obrigatório criar o encaminhamento ADB depois de conectar/reconectar o cabo:

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb devices
& $adb reverse tcp:8081 tcp:8081
& $adb reverse tcp:8080 tcp:8080
& $adb reverse tcp:8082 tcp:8082
& $adb reverse tcp:8000 tcp:8000
& $adb reverse --list
```

O resultado deve listar as quatro portas. Se o aplicativo exibir “sem conexão”, primeiro confira Docker, depois execute novamente os comandos acima e reabra o app.

## 6. Configuração do app Android

No arquivo privado `local.properties` do projeto Android, use as URLs locais abaixo para teste por cabo:

```properties
MOTTAINAI_API_BASE_URL=http://127.0.0.1:8081/
MOTTAINAI_ASSISTANT_BASE_URL=http://127.0.0.1:8000/
MOTTAINAI_CLOUDINARY_CLOUD_NAME=<cloud-name>
MOTTAINAI_CLOUDINARY_UPLOAD_PRESET=<unsigned-upload-preset>
```

Também são necessários:

- `app/google-services.json`, obtido no Firebase para o pacote `com.mottainai.cliente`;
- chave do Google Maps configurada no ambiente/Gradle (`MOTTAINAI_MAPS_API_KEY`);
- Firebase Authentication com E-mail/Senha e Google ativados;
- SHA-1 e SHA-256 do certificado de debug/release cadastrados no Firebase/Google Cloud para login Google;
- Cloudinary com preset **unsigned** específico para fotos de perfil.

Compile e instale pelo Android Studio ou, quando o Gradle estiver saudável:

```powershell
.\gradlew.bat :app:installDebug
```

## 7. Emulador Android

No emulador, não use `127.0.0.1`. Configure no `local.properties`:

```properties
MOTTAINAI_API_BASE_URL=http://10.0.2.2:8081/
MOTTAINAI_ASSISTANT_BASE_URL=http://10.0.2.2:8000/
```

Recompile o aplicativo após alterar as URLs.

## 8. Uso em qualquer celular, sem cabo e sem computador

Para funcionar em qualquer dispositivo, os containers locais não bastam. As APIs precisam estar publicadas em um servidor acessível pela internet, com HTTPS.

Arquitetura recomendada:

```text
Celular Android
  ├── Firebase Authentication
  ├── https://api-cliente.<dominio>/
  ├── https://api-ia.<dominio>/
  ├── Google Maps / localização
  └── Cloudinary (upload de avatar)

APIs publicadas
  ├── API Cliente -> PostgreSQL cloud
  ├── API Operacional -> PostgreSQL cloud
  ├── API NoSQL -> MongoDB
  └── API IA -> PostgreSQL + MongoDB + Redis + provedor de IA
```

Depois do deploy, troque as URLs antes de gerar o APK de distribuição:

```properties
MOTTAINAI_API_BASE_URL=https://api-cliente.seu-dominio.com/
MOTTAINAI_ASSISTANT_BASE_URL=https://api-ia.seu-dominio.com/
```

Não use IP local, `10.0.2.2` ou `127.0.0.1` em APKs distribuídos. Não use HTTP em produção: Android bloqueia tráfego sem TLS por padrão e dados de cliente não devem trafegar sem HTTPS.

## 9. Checklist de funcionamento por funcionalidade

### Cadastro e login

- Firebase Authentication ativo;
- `google-services.json` correspondente ao projeto correto;
- API Cliente publicada/rodando;
- perfil Firebase criado com CPF único;
- rota `GET/POST/PUT /api/v1/client/auth/profile` acessível;
- token Firebase enviado pelo app em `Authorization: Bearer <token>`.

### Catálogo, lojas, pontos e localização

- API Cliente rodando;
- banco contém lojas ativas, promoções válidas e itens de promoção;
- políticas/permissões do banco permitem que o papel da API Cliente leia os dados publicados;
- localização do dispositivo ativada e permissão concedida;
- Google Maps SDK habilitado e chave restrita ao pacote/assinatura corretos.

### Foto de perfil

- Cloudinary com `cloud name` e preset unsigned configurados no app;
- API Cliente aceita e retorna `avatarUrl` HTTPS no perfil;
- URL do avatar é persistida para o perfil Firebase correto;
- ao trocar de conta, o app consulta a URL do novo perfil; a foto anterior não deve ser reutilizada.

### Assistente Lô

- API IA, API NoSQL, MongoDB e Redis em execução;
- conexão PostgreSQL da IA configurada;
- credenciais/configuração do provedor de IA disponíveis no ambiente da IA;
- token Firebase válido;
- endpoints `/chat`, `/chat/history/{sessionId}` e `/chat/sessions` respondendo;
- acesso de rede entre IA, Mongo, Redis, PostgreSQL e provedor de IA.

### Notificações de ofertas

Para notificações reais em segundo plano, ainda é necessário configurar e publicar o envio por FCM no backend. O aplicativo pode pedir permissões, mas a API/serviço responsável precisa decidir quando enviar avisos, respeitando raio, lojas parceiras, limite diário e prioridade por fidelidade.

## 10. Diagnóstico rápido

| Sintoma | Verificação e ação |
| --- | --- |
| “Sem conexão” no catálogo | Docker aberto, API Cliente em `8081`, ADB reverse refeito, app reiniciado. |
| Login Firebase funciona, mas perfil falha | API Cliente indisponível, token Firebase inválido ou perfil ainda não criado. Consulte logs da API Cliente. |
| Chat não responde | Verifique `http://127.0.0.1:8000/livez`, logs da IA, Mongo/Redis e encaminhamento `tcp:8000`. |
| Login Google falha | Confira SHA-1/SHA-256, pacote Android, `google-services.json` e provedor Google no Firebase. |
| Mapa vazio | Confira chave do Maps, billing/API habilitada, localização ativada e coordenadas das lojas. |
| Avatar não atualiza | Confira Cloudinary, retorno `avatarUrl` da API Cliente e a sessão Firebase atual. |

## 11. Segurança antes de publicar

- Não colocar senhas, chaves de IA, DSNs ou chaves privadas no APK ou Git;
- restringir chaves Google pelo package name e SHA-1/SHA-256;
- manter upload preset do Cloudinary limitado a imagens, tamanho e pasta esperada;
- manter APIs atrás de HTTPS;
- validar Firebase ID token no backend, nunca confiar em UID recebido no corpo da requisição;
- usar usuários de banco sem superusuário e sem `BYPASSRLS`;
- manter CORS limitado aos clientes necessários quando houver front web;
- monitorar erros e consumo de IA/Cloudinary/Maps.
