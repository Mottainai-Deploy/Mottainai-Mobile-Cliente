# Entrega do Mobile Cliente — 01/11/2026

## Revisão em etapas

As branches são encadeadas para revisar cada diferença sem repetir a entrega anterior:

1. `fix/client-mvp-integration`, base `main`: frontend integrado e ajustes de login/cadastro, perfil, fotos, catálogo, lojas, localização, pontos e conversa; nomes descritivos dos layouts em inglês. Reúne o estado funcional em um único commit sobre a main atual.
2. `feat/client-app-icon`, base `fix/client-mvp-integration`: pacote Android do ícone do Figma exportado pelo Icon Kitchen.
3. `docs/client-source-documentation`, base `feat/client-app-icon`: documentação em português das 916 declarações nos 122 arquivos Java, incluindo classes, interfaces, enumerações, construtores, métodos e callbacks.
4. `docs/client-running-guide`, base `docs/client-source-documentation`: guia de execução, registros de validação e atualização das orientações históricas.

Revisar e integrar nessa ordem. Após integrar uma etapa, conferir a base e o diff da próxima, principalmente se a integração usar squash.

## Validação desta entrega

- Sintaxe Java e XML verificada por analisadores, sem erros identificados.
- Referências Java a layouts verificadas, sem layouts ausentes.
- Cobertura dos comentários verificada nas 916 declarações Java.
- Comparação dos tokens Java confirma que o commit de comentários preserva as instruções do código.
- `git diff --check` executado.
- Compilação, lint e testes Gradle tentados, mas a execução local falhou antes da compilação com `java.io.IOException: Unable to establish loopback connection`. Portanto, build, lint e testes automatizados não estão aprovados nesta validação. Conferir o CI e testar no Android Studio antes de integrar.

## Configuração privada

Não são publicados `.idea`, `local.properties`, `google-services.json`, credenciais Firebase de servidor ou arquivos de ambiente. URLs e configurações do ambiente devem seguir [o guia de execução](GUIA-EXECUCAO-APP-CLIENTE.md).

O Docker e o banco são ambientes externos ao repositório Android: esta entrega não publica alterações desses serviços. A indisponibilidade da API e a perda do encaminhamento USB precisam ser diagnosticadas no ambiente, não resolvidas por dados fictícios no app.
