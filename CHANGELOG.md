# Changelog

## Unreleased

- Classes, construtores, métodos e callbacks Java documentados em português, incluindo testes.
- Guia de execução com APIs locais, configuração de serviços, encaminhamento USB e requisitos de publicação HTTPS.

- Ícone oficial do Cliente exportado do Figma pelo Icon Kitchen, com recursos adaptativos e cinco densidades Android.

- Login e confirmação de cadastro com carregamento explícito; removidos acesso de demonstração e ação de trocar conta no cadastro.
- Perfil salva nome, telefone e URL da foto vinculada à conta; confirmação da foto retorna ao perfil atualizado.
- Busca por posição atual e seleção de raio de 5 ou 10 km; troca de distância preserva a tela atual.
- Catálogo com imagens externas, apresentação revisada de preços e datas e busca dentro da tela de ofertas.
- Layouts Android identificados por nomes descritivos em inglês e mapeamento de importação versionado.

### Histórico das etapas anteriores

As entradas abaixo registram etapas anteriores; as regras atuais acima prevalecem quando houver diferenças.

- Ajustada a tela de login, removida a ação separada de sincronização e adicionadas abertura para usuários sem sessão e transições entre telas.

- Corrigida a dimensão do Google Maps na tela de lojas e a interação de toque dentro da página.

- Login Google usa o fluxo explícito do botão, com cancelamento/erros visíveis, limite de espera e bloqueio de cliques repetidos; texto centralizado nos temas claro e escuro.

- Interface principal substituída pelo frontend completo da PR #25, com navegação Figma nativa e temas claro/escuro, conectando Firebase, perfil SQL, catálogo, imagens, Google Maps, pontos e assistente aos serviços existentes.
- Protótipo visual isolado em debug; login real não abre sessão fictícia e formulários de cadastro mantêm dados somente em memória.

- Fotos ilustrativas do Open Food Facts por nome do produto na Home, Catálogo e detalhe, sem alterar API/banco; busca assíncrona, cache, limite de consultas e créditos com link para a fonte.
- Demonstração inclui oferta fictícia de Nutella para testar a imagem externa com internet.

- Integração visual do Cliente reaproveita marca e DM Sans do frontend, com estilos compartilhados e cores para modo claro/escuro.
- Home usa cards compactos em RecyclerView e acesso ao catálogo; Home e Catálogo compartilham adapter e conteúdo dos cards, preservando Firebase, API e Google Maps.
- Formatador nativo de telefone carrega metadados fora da thread principal e aplica a formatação na thread da view, no cadastro e na edição do perfil.

- Cadastro com máscaras de CPF/CEP/nascimento e telefone brasileiro formatado/validado; payload mantém dígitos e data ISO. Edição de telefone reutiliza a validação.
- Conclusão do cadastro explica o e-mail vinculado e permite trocar de conta, descartando o formulário anterior sem criar acesso anônimo.
- Sincronização consulta o perfil antes de criar, recupera perfil próprio após conflito e não repete POST em ciclo. Mensagens distinguem conexão, timeout, HTTPS e resposta incompatível.
- Login/cadastro mostram a etapa em andamento; chamadas de perfil usam conexão de 5 s, leitura de 10 s e limite total de 20 s, sem repetição automática após falha de conexão. Catálogo/IA mantêm seus limites.

- Assistente integrado aos contratos existentes de envio, histórico e conversas, com URL própria configurável, vínculo ao UID e erros sem respostas fictícias para conta real. Homologação da autorização consumidor na IA ainda é necessária.
- Impacto apresenta saldo de fidelidade da API, compartilhamento do saldo e status oficial opcional; removidas métricas ambientais e conversões inventadas.
- Fotos das ofertas usam referência HTTPS opcional publicada pela API, sem autenticação no CDN e sem cache offline; ausência/falha mantém placeholder.
- Perfil oferece ajuda sobre o fluxo real e escolha de raio de lojas nesta sessão; diálogos usam o tema Material do app.

- Localização em primeiro plano centralizada: leitura ao entrar/retornar ao app e atualização manual, compartilhada entre Home e Lojas, com timeout, cancelamento e descarte de respostas antigas; demonstração não exige GPS.
- Cadastro valida os dígitos verificadores do CPF antes do envio, sem substituir verificação de titularidade ou validação do servidor.
- Entrada nos serviços reais exige sessão, permissão de localização e localização do aparelho ativada; nova tela permite autorizar, abrir configurações e sair, sem solicitar background nesta etapa. Detalhe e assistente revalidam o acesso.
- Home e Lojas compartilham posição/raio e a consulta de lojas em memória; Home orienta obter posição antes do mapa e não substitui por lojas globais.
- Lojas permitem escolher 5/10/15/20 km (padrão 5), reiniciam a paginação ao trocar o raio e não consultam a lista geral sem localização; preferência ainda é estado de tela, não cadastro persistido.
- Detalhe da oferta e lojas abrem a unidade no Google Maps oficial, com alternativa por URL web; sem coordenadas, exigem endereço completo e não pesquisam apenas o nome da rede.
- Firebase restrito à autenticação: cadastro e edição não gravam nome no perfil Firebase; nome protegido indisponível usa apresentação neutra.
- Sessão exige confirmação HTTP e perfil SQL explicitamente ativo; perfis incompletos ou inativos não liberam o acesso.
- Dados fictícios de impacto e respostas locais do assistente ficam restritos ao modo demonstração; contas reais dependem dos serviços configurados.
- Formulário de perfil mantém a edição aberta em caso de falha, com confirmação para sair da conta.
- Home mostra nome e saldo reais, com atualização e recuperação independentes por seção; edição de perfil reflete na saudação.
- Histórico de pontos preserva dados após falhas parciais e usa limites mensais sem sobreposição na precisão do banco.
- Busca opcional de lojas em até 5 km, com permissões de localização contextuais e retorno à lista geral.
- Catálogo e lojas preservam páginas carregadas e permitem abrir ofertas filtradas por loja.
- Pins e lista compartilham detalhe da loja e ação de rota; mapa usa a atribuição nativa do Google Maps.
- Espaçamento de ofertas na Home e no catálogo e tema verde nos componentes de formulário.
- Login por senha e Google retomam o cadastro quando falta o perfil SQL.
- Nova tentativa de sincronização e recuperação de senha pela tela de entrada.
- Cadastro em etapas com confirmação de senha e recuperação após falha de sincronização.
- Associação local do perfil ao Firebase UID e proteção contra respostas de outra sessão.
