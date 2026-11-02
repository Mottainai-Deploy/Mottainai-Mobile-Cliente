# App Cliente: migração do layout

Atualização de 26/10/2026: as [correções indicadas nas capturas do aparelho](phone-corrections.md) prevalecem sobre os estados do protótipo descritos nesta migração. O fluxo normal usa estados vazios para dados que dependem da API, mapa real e painéis sobre a tela anterior; as fixtures do Figma ficam restritas à comparação visual em debug.

Referência: [App Cliente / Ready for Dev](https://www.figma.com/design/DFHemxAp7isLLKf33YQve5/Mottainai---2%C2%B0-Ano?node-id=2005-3809).

O repositório foi clonado em `C:\Users\arthurvale-ieg\Downloads\inter.avi\mobile cliente`.
As alterações são entregues pela branch `feat/app-cliente-figma`, com revisão antes da integração à `main`.

## Cobertura e referências

O inventário inicial continha 104 telas por tema. A revisão dos destinos do protótipo acrescentou 11 telas e painéis necessários aos fluxos. As 115 telas e painéis têm XML nativo claro e escuro (230 variantes), incluindo apresentação, cadastro, conta, ofertas, lojas, fidelidade, conversas, avisos e foto do perfil.

Agora foram obtidos os 230 contextos originais pelo Figma MCP e as 230 imagens de referência. A geração usa exclusivamente o contexto próprio de cada variante. Não há mais telas completadas por aproximação do navegador nem temas derivados do contexto claro. `figma-context-gaps.json` está vazio.

A revisão integral compara cada tela do Figma com uma renderização nativa e registra também o final dos conteúdos que precisam de rolagem. A galeria local é produzida por `tools/design-import/review_visuals.py`. As imagens e os contextos de referência ficam no cache ignorado `.design-cache/visual-review`, não dentro do aplicativo.

Isso permite revisar cobertura e diferenças concretas; não estabelece igualdade pixel a pixel em qualquer aparelho, densidade, fonte ou versão do Android.

## Estrutura Android

O aplicativo continua nativo em Java/XML, com um único identificador e temas claro/escuro persistentes. Os fragments principais compartilham comportamento e mantêm seus próprios layouts XML. As demais telas usam `DesignScreenFragment` e um XML por tela.

Os conteúdos usam rolagem e altura adaptável. Linhas distribuem a largura por pesos; a navegação inferior divide os cinco destinos igualmente. O contêiner principal considera barras do sistema, recortes de tela, teclado e largura máxima de 720 dp. Coordenadas proporcionais ficam restritas à composição de ilustrações e às anotações do mapa.

A marca, as fotos e as ilustrações são arquivos locais exportados do Figma, renderizados com AndroidSVG. A fonte DM Sans e sua licença estão incluídas. Não há URLs temporárias do Figma em uso durante a execução.

## Interações e integrações

O primeiro acesso apresenta as três telas de introdução; pular ou concluir salva essa preferência. Os vínculos de navegação obtidos do protótipo estão em `assets/design_navigation.json`. A troca de tema mantém a tela atual. Campos de formulário são editáveis, com validação básica de obrigatoriedade e formato de e-mail. No fluxo normal, busca, ofertas e pontos usam estados sem dados enquanto a API não está pronta. O chat informa que a Lô ainda não está conectada, identifica as mensagens com “Você” e “Lô” e mantém “Minhas conversas” no cabeçalho. Dados editados, região e consentimento são mantidos no aparelho. A foto usa a galeria ou a câmera do Android, permite conferência e é salva localmente, sem envio a um servidor. Grupos decorativos não exibem mais um diálogo genérico ao toque.

O projeto original usa repositórios mock. A API do cliente ainda não está pronta, conforme confirmação do usuário. Não há autenticação remota, Google OAuth, envio real de e-mail, sincronização de conta ou serviço de IA. A atualização de 26/10 acrescenta mapa real e solicitação de permissões nativas; isso não implementa notificações de ofertas/geofencing. As telas de sucesso referentes a serviços de conta continuam sendo estados do protótipo, não comprovantes de operações remotas. O login é uma sessão local.

Estados do protótipo não devem ser confundidos com operações reais concluídas no servidor.

## Geração e manutenção

`tools/design-import/parse.mjs` interpreta o código de referência obtido de `get_design_context`; `generate.py` produz os recursos XML nativos; `download.py` baixa os arquivos fornecidos pelo Figma com curl. O cache de referência e node_modules ficam ignorados pelo Git. Os recursos gerados ficam versionáveis e podem ser editados normalmente.

Para repetir a importação, é necessário recuperar os contextos do Figma no cache; os scripts dependem desses dados para reconstruir os recursos. A compilação normal do Android não depende desse cache. Ao atualizar o design, repetir a geração, revisar as capturas e atualizar o inventário de referências.

## Verificação

Os pesos e o eixo óptico 14 da fonte variável são aplicados pela API do TextView, compatível com Android 8 (API 26), evitando atributos XML ignorados nas versões mínimas.

O teste instrumentado `DesignLayoutTest` mede os layouts a 320 e 600 dp nos dois temas, com fonte em 130%, verifica limites horizontais dos componentes de interface e captura 13 telas representativas por tema (26 imagens), incluindo endereço, confirmação de conta, menu de conversas e câmera. As capturas verificam também se a janela ativa pertence ao aplicativo, evitando registrar sobreposições de outros serviços do emulador. O teste `DesignVisualReviewTest` percorre também as 230 variantes na Activity real, salva os desenhos das Views nativas e registra a geometria dos textos e o fim das rolagens. As capturas integrais das Views não incluem a interface de outros aplicativos ou as barras desenhadas pelo sistema. Esses testes não substituem testes de acessibilidade ou em aparelhos físicos.

Comandos usados:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$env:ANDROID_HOME = 'C:\Users\arthurvale-ieg\AppData\Local\Android\Sdk'
$env:ANDROID_SERIAL = 'emulator-5554'
$env:JAVA_TOOL_OPTIONS = '-Djava.io.tmpdir=C:\tmp\mottainai-java -Djdk.net.unixdomain.tmpdir=C:\tmp\mottainai-java'
.\gradlew.bat --no-daemon assembleDebug connectedDebugAndroidTest lintDebug
```

Neste computador, o diretório temporário curto foi necessário para evitar `Unable to establish loopback connection` no Java. Não é uma configuração obrigatória do aplicativo.

## Capturas do Android

Exemplos renderizados no emulador: [login claro](previews/login-light.png), [login escuro](previews/login-dark.png), [Início claro](previews/home-light.png), [Início escuro](previews/home-dark.png), [apresentação clara](previews/onboarding-light.png), [apresentação escura](previews/onboarding-dark.png), [lojas no tema escuro](previews/stores-dark.png), [perfil escuro](previews/profile-dark.png) e [prévia ilustrativa da câmera](previews/camera-preview.png).

A revisão integral encontrou e corrigiu a altura dos campos de busca e mensagem, herança de alinhamento e peso dos textos, cantos individuais dos balões, larguras de esqueletos de carregamento, posição do botão sobre a foto, ilustração ausente da abertura, composição dos fundos de diálogos e contraste da localização no tema escuro. As capturas iniciais de câmera e localização mostram estados do protótipo; no fluxo normal atualizado, a foto usa a câmera/galeria do Android e o mapa usa localização autorizada, conforme o relatório de correções.

O Figma deixa o fundo de algumas telas de localização sem preenchimento, inclusive no tema escuro. Nessas telas o aplicativo aplica o canvas do tema para manter o texto legível. As barras do sistema usam os elementos reais do Android.

A validação desta rodada está em [visual-review.md](visual-review.md). Os testes automatizados foram executados no emulador; a instalação no aparelho físico é verificada separadamente.
