<div align="center">

# Mottainai Mobile Cliente

**Aplicativo Android do consumidor final: vitrine de ofertas de mercados parceiros e fidelidade no ecossistema Mottainai**

*Ofertas, lojas, pontos e assistente, com autenticação Firebase e clientes HTTP para os serviços configurados; a homologação depende das integrações disponíveis.*

<br/>

[![Android](https://img.shields.io/badge/Android-minSdk%2026%20·%20targetSdk%2036-3DDC84?style=for-the-badge&logo=android&logoColor=white)](app/build.gradle.kts)
[![Java](https://img.shields.io/badge/Java-11%20(source%2Ftarget)-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](app/build.gradle.kts)
[![Firebase](https://img.shields.io/badge/Firebase-Auth-FFCA28?style=for-the-badge&logo=firebase&logoColor=black)](gradle/libs.versions.toml)
[![Google Maps](https://img.shields.io/badge/Google%20Maps-SDK%2020.0.0-4285F4?style=for-the-badge&logo=googlemaps&logoColor=white)](gradle/libs.versions.toml)

[![AGP](https://img.shields.io/badge/AGP-9.0.1-3DDC84?style=flat-square&logo=android&logoColor=white)](gradle/libs.versions.toml)
[![Gradle](https://img.shields.io/badge/Gradle-9.1.0%20(wrapper)-02303A?style=flat-square&logo=gradle&logoColor=white)](gradle/wrapper/gradle-wrapper.properties)
[![Retrofit](https://img.shields.io/badge/Retrofit-2.11.0-48B983?style=flat-square&logo=square&logoColor=white)](gradle/libs.versions.toml)
[![OkHttp](https://img.shields.io/badge/OkHttp-4.12.0-3E4348?style=flat-square&logo=square&logoColor=white)](gradle/libs.versions.toml)
[![GitHub Actions](https://img.shields.io/badge/CI-GitHub%20Actions-2088FF?style=flat-square&logo=githubactions&logoColor=white)](.github/workflows/ci.yml)

<br/>

[Funcionalidades](#funcionalidades) • [Arquitetura](#arquitetura-e-integrações) • [Execução](#configuração-e-execução) • [Entrega](#testes-e-entrega)

</div>

---

## Visão geral

Aplicativo Android do cliente Mottainai, desenvolvido em Java e XML. O cliente consulta ofertas de mercados parceiros próximos, encontra as lojas no mapa, acompanha seus pontos e conversa com a assistente Lô.

O aplicativo funciona como vitrine: a compra é realizada no mercado. O acesso aos serviços exige uma conta autenticada e a localização necessária para os recursos próximos.

## Funcionalidades

| Área | Funcionamento |
|---|---|
| Conta | Cadastro em etapas, login por e-mail/senha ou Google e recuperação de senha. |
| Cadastro | Validação de CPF, telefone, nascimento e endereço, com consulta de CEP pelo ViaCEP. |
| Início | Região atual, lojas, ofertas e saldo recebidos dos serviços configurados. |
| Ofertas | Busca, filtro por loja, paginação e detalhes com preços e validade. |
| Lojas e mapa | Posição atual do aparelho, lojas com coordenadas e raio de **5 ou 10 km**. |
| Como chegar | Abertura do Google Maps com a localização da loja da oferta. |
| Pontos | Consulta de saldo e histórico; o Android não credita pontos por conta própria. |
| Perfil | Edição de nome e telefone, foto pela câmera ou galeria, upload ao Cloudinary e referência vinculada ao perfil. |
| Segurança da conta | Alteração de senha pelo fluxo Firebase e saída da conta. Alteração de e-mail não faz parte do fluxo disponível. |
| Assistente Lô | Envio de perguntas, respostas, histórico e sessões de conversa pela API de IA. |
| Apresentação | Tema claro/escuro e componentes com adaptação ao tamanho da tela. |

### Avisos de ofertas

O cliente ativa ou desativa os avisos em **Perfil → Avisos de ofertas**, por um popup com confirmação e carregamento. As notificações aparecem no Android; tocar em uma oferta abre seu detalhe. Preferências e recebimentos são separados por conta.

Firebase é utilizado somente para autenticação. A API Cliente fornece o acesso aos avisos Redis e sincroniza preferência, raio e posição recente. O fluxo automático exige banco compatível, produtor habilitado e serviços acessíveis ao aparelho. A consulta em segundo plano está sujeita às restrições do Android; não há garantia de entrega instantânea.

Consulte o [guia de notificações](docs/NOTIFICACOES-REDIS-CLIENTE.md) e o [registro de validação](docs/notification-integration-validation.md).

## Arquitetura e integrações

| Componente | Responsabilidade |
|---|---|
| Activities, Fragments e XML | Navegação e apresentação das telas nativas. |
| ViewModels e repositórios | Estado de carregamento, consultas, tentativas e atualização da interface. |
| Retrofit e OkHttp | Comunicação autenticada com os serviços externos. |
| Firebase Authentication | Identidade por e-mail/senha ou Google e recuperação de acesso. |
| API SQL Cliente | Perfil, catálogo, fidelidade e preferências de avisos. |
| API Redis | Consulta de avisos e confirmação de leitura. |
| Google Maps e localização Android | Mapa, coordenadas atuais e abertura da rota para a loja. |
| Cloudinary | Arquivo da foto de perfil; a API mantém sua referência no perfil. |
| Fonte externa de imagens | Imagens dos produtos, com cache e placeholder. |
| Mottainai IA e NoSQL | Respostas e persistência das conversas nos serviços correspondentes. |

Os dados de negócio do Cliente são persistidos pelos serviços e pelo banco SQL. O app não acessa o banco diretamente e não implementa operação offline de negócio. Preferências auxiliares no aparelho não substituem essa persistência.

Repositórios relacionados: [API Cliente](https://github.com/Mottainai-One/Mottainai-Api-SQL-Cliente), [IA](https://github.com/Mottainai-One/Mottainai-IA), [NoSQL](https://github.com/Mottainai-One/Mottainai-Api-NoSQL) e [Redis](https://github.com/Mottainai-One/Mottainai-Redis-Api).

## Tecnologias

Android nativo, Java 11 para o código-fonte, XML, AndroidX, Material Components, ViewModel/LiveData, Retrofit, OkHttp, Glide, Firebase Authentication, Google Identity e Google Maps. As versões são definidas em [libs.versions.toml](gradle/libs.versions.toml) e [app/build.gradle.kts](app/build.gradle.kts).

## Configuração e execução

1. Abrir o projeto no Android Studio e sincronizar o Gradle.
2. Configurar o app Firebase com o pacote `com.mottainai.cliente`, os provedores usados e a impressão digital do certificado de teste.
3. Colocar o arquivo privado em `app/google-services.json` e configurar as propriedades locais das URLs, Maps e Cloudinary conforme o [guia de execução](docs/GUIA-EXECUCAO-APP-CLIENTE.md).
4. Disponibilizar as APIs e o banco compatível. O telefone precisa conseguir alcançar os endereços configurados.
5. Em testes com URLs locais por cabo, manter o encaminhamento USB das portas usadas pelo aplicativo. Um endereço de desenvolvimento no computador não se torna público ao instalar o APK.
6. Executar no dispositivo e entrar com uma conta autorizada.

Não versionar `google-services.json`, `local.properties`, senhas, tokens ou credenciais administrativas. O segredo da API do Cloudinary não vai para o Android.

## Testes e entrega

No PowerShell, a partir da raiz:

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
```

Em Linux/macOS, usar `./gradlew` com as mesmas tarefas. Os testes instrumentados exigem um dispositivo/emulador configurado e são executados separadamente com `connectedDebugAndroidTest`.

O [CI](.github/workflows/ci.yml) verifica build, testes, lint, formatação e convenção dos commits recebidos. O validador preserva as mensagens automáticas de merge fora dessa verificação e possui testes próprios. CI aprovado não substitui os testes em aparelho com as APIs reais.

- [Demandas da disciplina e links por funcionalidade](docs/ENTREGA-DEMANDAS-MOBILE.md).
- [Documentação funcional e técnica do Cliente](docs/MOBILE-CLIENTE.md).
- [Guia de execução e configuração](docs/GUIA-EXECUCAO-APP-CLIENTE.md).
- [Imagens dos produtos](docs/IMAGENS-PRODUTOS.md).
- [Registro das versões e validação da entrega](docs/ENTREGA-MOBILE-CLIENTE.md).

A evidência acadêmica de persistência Firebase/SQLite está mapeada ao Operacional, com decisões de aprovação gravadas no Firestore. O Cliente mantém a arquitetura de autenticação Firebase e persistência de negócio pela API SQL. A demonstração conjunta dos dois apps e a homologação em tablet estão detalhadas no mapa de entrega.