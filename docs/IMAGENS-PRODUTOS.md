# Imagens externas do catalogo Cliente

## Implementacao atual

O mobile consulta o Open Food Facts diretamente pelo nome do unico produto da promocao. Nao adiciona coluna de imagem, nao altera a API e nao envia token Firebase, CPF, localizacao ou dados do cliente ao fornecedor de imagens.

1. Se a API fornecer uma URL HTTPS valida de imagem, ela tem prioridade.
2. Sem URL, uma promocao de um unico produto usa o nome desse produto na pesquisa publica.
3. A resposta e filtrada por nome e origem HTTPS `images.openfoodfacts.org`.
4. A foto encontrada e **ilustrativa**: busca por nome nao confirma marca, tamanho, embalagem ou o produto exato. O aviso aparece junto da foto com credito e link para a pagina de origem.
5. Falha, timeout, produto sem foto ou promocao com varios produtos sem capa: placeholder. Fotos nao impedem carregar precos, lojas ou navegar.

Home, Catalogo e detalhe usam `ProductImages`. A consulta usa cliente HTTP independente de `ApiClient`, sem interceptador Firebase. Nao existe pesquisa enquanto o usuario digita: somente nomes de produtos recebidos nas ofertas visiveis.

As consultas sao deduplicadas, processadas em fila com pelo menos 7 segundos entre consultas, e pausadas por pelo menos 60 segundos em HTTP 429/503. A fila aceita ate 32 nomes diferentes. Cache em memoria de ate 128 nomes: 6 horas para resultados e 1 minuto para falhas. Glide reaproveita as imagens em cache. As fotos podem aparecer gradualmente; esse cache nao implementa modo offline de negocio.

O retorno da consulta antiga `/cgi/search.pl` e usado porque a busca textual nao esta no contrato v3 de produtos. A integracao esta isolada em `OpenFoodFactsImages`, para troca do endpoint sem alterar as telas.

## Testar no celular pelo USB

1. Desbloqueie o celular, ative depuracao USB e aceite a autorizacao do computador.
2. Confira `adb devices`: precisa aparecer `device`, e nao `unauthorized`.
3. Execute `adb reverse tcp:8081 tcp:8081` com o cabo conectado. Esse encaminhamento pode precisar ser repetido depois de reconectar/reiniciar.
4. A configuracao local deste projeto usa `MOTTAINAI_API_BASE_URL=http://127.0.0.1:8081/` em `local.properties`, ignorado pelo Git.
5. Rode o app pelo Android Studio. Para testar somente as fotos, na tela de entrada toque em **Explorar demonstracao** e abra o catalogo. A primeira oferta e **Nutella - teste de imagem externa**; oferta/preco/loja sao ficticios, mas a foto vem da internet.
6. Abra o detalhe: confirme o aviso de imagem ilustrativa e toque nos creditos para conferir a fonte. Volte ao catalogo e role a lista: uma foto nao deve aparecer em outro card reciclado.
7. Sem internet ou em produto sem resultado, o placeholder permanece e as outras funcoes continuam disponiveis.

Para testar o catalogo autenticado, use login e perfil SQL validos. O modo demonstracao nao testa autenticao, banco ou a API Cliente.

Para emulador, substitua a URL local por `http://10.0.2.2:8081/`; nao use essa URL no celular fisico. A precedencia e: parametro `-P` explicito do Gradle, propriedade em `local.properties`, propriedade Gradle geral.

## Carga local de dados

O ambiente Docker esta em `local-stack` no workspace Mottainai-Codex. `DataLoad.ps1` cria uma empresa/loja marcadas **DEMO LOCAL**, dois produtos e duas promocoes aprovadas/vigentes, sem clientes reais, sem fotos no banco e sem alterar schema/politicas. Executar novamente nao duplica os registros e renova as ofertas por sete dias.

```powershell
.\DataLoad.ps1
# Opcional: posicionar a loja ficticia perto da localizacao usada no teste
.\DataLoad.ps1 -Latitude -23.5631 -Longitude -46.6544
```

O endereco e ficticio; as coordenadas padrao sao de Sao Paulo. A busca de lojas aplica o raio do usuario: para lojas proximas, use coordenadas de teste adequadas. Nao realizar compras ou navegar ate a loja ficticia.

## Pendencia encontrada no SQL compartilhado

As tabelas `promotion` e `promotion_item` possuem RLS habilitada. As politicas presentes atendem apenas ao grupo `mottainai_api`. As views `api_client_catalog_promotions` e `api_client_catalog_promotion_items` pertencem a `mottainai_client_catalog_owner`, que nao possui politicas SELECT correspondentes. Por isso o catalogo retorna zero promocoes mesmo com dados aprovados, ativos, vigentes e disponiveis.

Isso e independente das imagens. Nao deve ser resolvido usando superusuario na API, desligando RLS ou dando o papel Operacional ao Cliente.

As duas politicas SELECT de `local-stack/catalog-read-policies.proposed.sql` foram aplicadas somente no PostgreSQL Docker local em 30/10/2026, apos autorizacao de Isabella. O usuario `client_login` passou a enxergar as duas ofertas aprovadas; o teste de rascunhos confirmou que continuam ocultos, com rollback dos dados de teste e RLS preservado. O responsavel pelo banco/API deve incorporar uma correcao revisada ao instalador oficial. Nenhuma coluna de imagem ou alteracao de endpoint e necessaria para a busca ilustrativa implementada no mobile.

## Referencias

- https://openfoodfacts.github.io/openfoodfacts-server/api/
- https://openfoodfacts.github.io/openfoodfacts-server/api/how-to-download-images/
- https://world.openfoodfacts.org/terms-of-use

## Verificacao em 30/10/2026

- `:app:assembleDebug :app:testDebugUnitTest`: aprovados; 88 testes, zero falhas/erros.
- Busca publica real por Nutella: 5 resultados em 4,3 s; imagem HTTP 200.
- BuildConfig confirma `http://127.0.0.1:8081/` para USB.
- Dataload executado duas vezes: mesmos dois IDs, sem duplicacao.
- Proposta das duas politicas SQL testada com `SET LOCAL ROLE client_login`: duas ofertas visiveis; transacao revertida, sem persistir mudanca de seguranca.
- Celular conectado aparece `unauthorized`: ainda nao foi possivel instalar, abrir ou validar visualmente o APK no aparelho.
- Nenhuma PR aberta ou publicada nesta entrega.

- Correcao das politicas aplicada no banco local apos confirmacao: leitura das ofertas aprovadas e exclusao de rascunhos validadas com `client_login`; sem SUPERUSER/BYPASSRLS para as APIs. O instalador SQL original e repositorios de API nao foram alterados.
