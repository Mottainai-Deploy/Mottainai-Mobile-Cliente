# Frontend completo da PR #25

Esta integração usa os layouts nativos da PR #25 como interface principal do app: apresentação, login, cadastro, Home, ofertas, detalhes, lojas, pontos, perfil, configurações e Lô, com versões clara e escura. Substitui a adaptação parcial da PR #20.

## Código e navegação

- MainActivity hospeda os layouts registrados em assets/design_screens.json.
- Os layouts usam nomes descritivos em inglês com prefixos `fragment_` e `dialog_`; `tools/design-import/layout-names.json` mantém os nomes estáveis entre importações. Os IDs das telas continuam os do Figma para preservar a navegação.
- DesignScreenFragment cuida da apresentação, links entre telas e histórico.
- ClientRuntime conecta os componentes do Figma aos ViewModels e repositórios já existentes.
- Os cards de ofertas e lojas são extraídos dos próprios layouts da PR por tools/bind_figma_templates.py; não são os cards antigos.
- Os nomes das Activities de login, cadastro, acesso à localização e detalhes permanecem como entradas compatíveis, exibindo o novo frontend.

Fluxo de dados: tela do Figma → ClientRuntime → ViewModel → Repository → Firebase Auth / API.

## Comportamento real

| Área | Integração |
| --- | --- |
| Login | E-mail/senha e Google pelo Firebase; sincronização do perfil SQL antes de liberar conteúdo |
| Cadastro | Dados pessoais → endereço/ViaCEP → revisão → criação/vínculo do perfil na API |
| Validação | Máscaras e validação de CPF, telefone, data, CEP, senha, confirmação e UF |
| Sessão | SessionManager verifica a identidade Firebase e o ID do perfil SQL; o protótipo não concede acesso |
| Ofertas | CatalogViewModel, busca, paginação, filtro por loja e ID real na abertura dos detalhes |
| Imagens | Fallback ilustrativo Open Food Facts, independente da autenticação, com atribuição |
| Lojas | Posição autorizada do aparelho, raio 5/10/15/20 km e marcadores de parceiros no Google Maps SDK |
| Rotas | Abre o Google Maps oficial para coordenadas/endereço da loja selecionada |
| Pontos | Saldo e histórico reais; sem criar pontos ou métricas ambientais no Android |
| Perfil | Leitura/edição suportada pelo contrato da API; informações ausentes não são inventadas |
| Lô | AssistantViewModel e serviço configurado; indisponibilidade explícita quando não há URL |

Os formulários de cadastro usam um ViewModel em memória para manter as etapas. CPF, telefone, endereço e senha não são gravados em SharedPreferences. O Firebase mantém a autenticação; a API mantém os dados de negócio.

## Limites preservados

- A prévia de todas as páginas do Figma continua disponível apenas em debug, pelo extra design_preview. Não representa autenticação.
- O botão Explorar demonstração existe apenas em debug e identifica os dados como fictícios.
- Foto no Cloudinary, sincronização de alteração de e-mail e entrega de avisos em segundo plano continuam dependendo das integrações pendentes. A interface não confirma uma gravação remota inexistente.
- A redefinição de senha usa as instruções enviadas pelo Firebase ao e-mail da conta.
- Categorias, medidas ambientais, horários e distâncias não fornecidos pelo contrato não são inventados. O endpoint atual de promoções permite busca e filtro por loja, mas não um feed geográfico global: a garantia de ordenação/proximidade de todo o feed depende desse contrato na API.
- A API de perfil atual não devolve todos os dados pessoais; isso limita o preenchimento inicial e a edição isolada do consentimento de comunicação.
- Login ponta a ponta exige Firebase configurado e API Cliente acessível. As configurações locais privadas não entram no Git.

## Validação

Compilar o app, testes unitários e APK instrumentado. Executar UiIntegrationTest para verificar os cards nos dois temas, proteção das rotas sem sessão, cadastro e navegação da demonstração. Os testes visuais do frontend usam a prévia explícita.

A PR deve ser revisada antes de aprovação/merge. Esta integração não autoriza nem realiza merge na main.
