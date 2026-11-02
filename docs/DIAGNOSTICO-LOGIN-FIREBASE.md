# Timeout de sincronizacao do perfil Firebase — 30/10/2026

## Evidencia

O Firebase autentica a conta. A consulta `GET /api/v1/client/auth/profile` chega a API Cliente, mas pode ultrapassar o timeout do Android antes de consultar o SQL.

O log do container registrou `AuthenticationServiceException`, causada por `SocketTimeoutException: Read timed out`, ao buscar:

`https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com`

O ponto de configuracao e `SecurityConfig.firebaseJwtDecoder()` na API Cliente: `NimbusJwtDecoder.withJwkSetUri(...)`. Nao e uma falha de senha, nem comprovacao de que falta cadastro no SQL. Sem concluir a verificacao do JWT, a requisicao nao chega a consulta de perfil.

No Android, `ApiClient` usa leitura de 10 s e limite total de 20 s para perfil. `ProfileSyncError` apresenta a mensagem de timeout observada na tela. Aumentar apenas esse limite nao resolve a indisponibilidade do Google nem o tratamento no servidor.

## Verificacao local

- Windows e curl dentro do container conseguiram consultar as chaves publicas por HTTPS.
- O Java conseguiu consultar o endpoint depois de reiniciar o ambiente. Apos outro timeout, a configuracao local do container Cliente foi ajustada para IPv4/TLS 1.2: a API reiniciada rejeitou a assinatura em 9,02 s a frio. Esse resultado ainda esta proximo do limite do mobile e nao substitui a correcao definitiva de disponibilidade no backend. Certificados e assinatura JWT continuam obrigatorios.
- O teste HTTP real da API usa um JWT sintetico com identificador de chave publica atual e **assinatura invalida**. Nao usa senha, token ou dados de um cliente.
- Primeira validacao: assinatura rejeitada corretamente com HTTP 401 em 13,72 s, acima dos 10 s de leitura do Android.
- Tentativas seguintes: HTTP 401 em 0,28 s e 0,11 s, com cache preparado.
- Isso comprova carregamento das chaves e rejeicao da assinatura invalida. Nao comprova, sozinho, login com uma conta Firebase real ou leitura/criacao de perfil SQL.

## Preparacao temporaria do ambiente local

Com Docker Desktop aberto e imagens construidas, executar no PowerShell 7:

```powershell
.\Start-Local.ps1
```

O script sobe os containers existentes, aguarda o endpoint protegido responder e exercita a validacao de assinatura para preparar as chaves antes do login real. Nao muda codigo das APIs, permissoes, banco ou requisitos de autenticacao.

Para celular fisico pelo USB, usar `http://127.0.0.1:8081/` no app e encaminhar `adb reverse tcp:8081 tcp:8081`. `Keep-ClientUsb.ps1 -Serial <serial> -Watch` pode recuperar o encaminhamento quando o transporte USB reinicia. Essa e uma falha distinta do timeout ao consultar o Google.

Para testar uma conta real sem o app/USB, executar `diagnostics/Test-ClientLogin.ps1` no PowerShell 7. Ele pede email/senha localmente com senha oculta, autentica via REST no Firebase e consulta o perfil usando o ID token somente em memoria. Nao salva nem mostra senha, token ou dados pessoais do perfil. Esse teste com conta real ainda depende da entrada local das credenciais pelo usuario.

## Demanda para a responsavel pela API Cliente

Revisar o carregamento inicial/cache das chaves publicas, atualizacao durante a rotacao de chaves e limites de conexao/leitura do cliente HTTP do decoder. A consulta de perfil deve terminar dentro do orcamento de tempo do mobile ou retornar indisponibilidade de forma rapida e identificavel.

Indisponibilidade do provedor de chaves deve ser diferenciada de token invalido, sem registrar o token e sem pedir que o cliente crie outra conta. Manter assinatura, emissor, audiencia e validade temporal obrigatorios; nao usar bypass ou sessao mockada para contas reais.

Aceitacao:

1. Reiniciar a API sem cache e medir a primeira consulta autenticada.
2. Token Firebase valido e perfil existente: HTTP 200 com o proprio perfil.
3. Token Firebase valido sem perfil: HTTP 404 tratado pelo app como conclusao de cadastro.
4. Assinatura, emissor ou audiencia invalidos: HTTP 401 sem acesso a dados.
5. Google indisponivel/lento: erro de disponibilidade controlado dentro do limite acordado, sem erro generico tardio ou falsa invalidacao da conta.
6. Repetir com rotacao das chaves e com cache aquecido; nao depender de preparo manual para uso normal.

Nenhuma alteracao foi publicada no repositorio da API nesta investigacao.

## Perfil sem cadastro mascarado como HTTP 403

O banco local nao tinha perfis de cliente. `JdbcClientApiOperations.bootstrapFirebase()` retorna `ResponseStatusException(404)` quando nao encontra a identidade Firebase. O processamento interno desse erro passa por `/error`, que era bloqueado pela regra generica da cadeia de seguranca. Assim, erros originais podiam aparecer como 401/403.

Reproducao sem conta real: POST publico de validacao de token de recuperacao com `{"token":"invalid"}` retornava 401, embora o servico rejeite esse formato com 400. A funcao SQL de contexto do cliente executou normalmente e retornou NULL para uma identidade inexistente; nao houve falta de permissao SQL nesse teste.

Foi configurado **apenas no container local** `SPRING_SECURITY_FILTER_DISPATCHER_TYPES=REQUEST,ASYNC,FORWARD,INCLUDE`. Isso exclui somente o processamento interno ERROR do filtro, preservando as requisicoes normais e os outros tipos de despacho. Depois do ajuste: token malformado 400, perfil sem token 401, acesso direto a `/error` 401 e assinatura JWT invalida 401. O teste de uma conta Firebase valida sem perfil deve retornar 404 para o app abrir a conclusao do cadastro.

Para a correcao no codigo oficial da API, revisar as cadeias de seguranca e permitir explicitamente o despacho `DispatcherType.ERROR`, sem liberar as rotas protegidas, conforme https://docs.spring.io/spring-security/reference/7.0/servlet/authorization/authorize-http-requests.html. Testar 404 de perfil inexistente, 400 de validacao e 500 de indisponibilidade, preservando seus status e mantendo a autenticacao de acesso normal.

## Falha ao salvar: ano de nascimento no mapper JDBC

O POST de criacao chegou ao servico e a funcao SQL, mas falhou ao montar a resposta: `ClassCastException: Integer cannot be cast to Short`, em `JdbcClientApiOperations.customer()`, linha 496 da base analisada. O mapper e compartilhado pela criacao, leitura e atualizacao do perfil. A excecao reverte a transacao; o banco continuou com zero clientes na verificacao.

O driver PostgreSQL do proprio container retorna `Integer` ao executar `getObject("birth_year")`, mesmo quando a coluna SQL e SMALLINT. O cast direto para `Short` falha. A correcao minima e:

```java
// Antes
(Short) rs.getObject("birth_year")
// Depois
rs.getObject("birth_year", Short.class)
```

O teste isolado `JdbcSmallintProbe.java` usou apenas constantes SQL, sem dados de clientes: confirmou o retorno Integer, a conversao tipada de 2000 e a preservacao de NULL. O patch reproduzivel esta em `diagnostics/client-birth-year.patch`.

Isabella autorizou explicitamente aplicar essa correcao **somente na copia local da API Cliente e no container local**, sem publicar codigo nem abrir PR. A alteracao esta na branch local `fix/customer-profile-birth-year`, com tres testes de regressao em `JdbcCustomerMapperTest` (INTEGER, SMALLINT e NULL). A equipe da API deve incorporar a correcao revisada ao repositorio oficial.

Validacao concluida: 5 testes Maven aprovados (3 do mapper e 2 existentes), sem falhas/erros. A imagem local `sha256:5482f789bb9d524f39d334cfd2f3e0b486842e010191e24d296abde08724dd20` foi confirmada em execucao no container Cliente. Apos reiniciar: erros de validacao preservam HTTP 400, acesso sem token continua 401 e assinatura invalida foi rejeitada com 401. A primeira preparacao das chaves sofreu novamente falha transitoria de conexao com o Google; a repeticao concluiu em 5,48 s. A instabilidade externa segue como demanda separada do backend.

O banco ainda tinha zero perfis na verificacao posterior ao deploy. O salvamento real precisa ser repetido pelo usuario no app; nao foi criada conta/perfil ficticio para simular sucesso. Nenhum segredo, token, dado pessoal ou schema foi alterado pela correcao do mapper.
