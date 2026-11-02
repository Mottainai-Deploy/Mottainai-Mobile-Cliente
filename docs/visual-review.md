# Revisão do App Cliente — 25/10/2026

Foram comparadas as 115 telas e painéis nos dois temas: 230 referências originais do Figma e 230 renderizações das Views nativas na Activity real. Conteúdos com rolagem também tiveram o fim capturado. A galeria de comparação fica em `.design-cache/visual-review/sheets-final/index.html`, gerada por `tools/design-import/review_visuals.py final`.

## Correções

- Remoção do diálogo genérico de demonstração e dos cliques em grupos decorativos.
- Botões do login com conteúdo centralizado, largura adaptável e texto sem altura fixa que cause corte.
- Campos reais de busca e mensagem, incluindo o campo aninhado nas respostas da Lô.
- Cinco destinos de navegação com larguras iguais, sem espaçadores tratados como destinos.
- Alturas dos formulários, rolagem, teclado, margens, fontes, cantos dos balões, sobreposição da foto e ilustrações originais.
- Peso e eixo óptico 14 da fonte DM Sans conforme o Figma.
- Retornos, confirmação de saída, seleção de consentimento e recuperação dos estados de erro.
- Galeria e câmera do Android com conferência antes de salvar a foto localmente.

## Evidências

A execução integral passou em 9 testes instrumentados: sete testes de fluxo, um de geometria e um de renderização integral. Os fluxos cobrem introdução/login/saída, cadastro, perfil, busca/conversa, consentimento, persistência de tema, foto e recuperação de erros. Os resultados integrais estão no cache `full-flow-tests-passed.xml`.

O teste de geometria mede 115 layouts em 320 e 600 dp, nos dois temas e com fonte em 130%: 460 combinações. O relatório de texto das 230 renderizações não detectou corte de texto. Isso não comprova todos os gestos, aparelhos e versões do Android possíveis.

Após os ajustes finais do recorte circular da Lô e do espaçamento da captura de foto, o teste de geometria passou novamente e oito variantes afetadas foram renderizadas de novo, nos dois temas. A primeira seleção desse teste visual continha um identificador digitado incorretamente; corrigida a seleção, a execução passou. As novas capturas foram incorporadas à galeria integral.

`assembleDebug` e `lintDebug` concluíram sem erros. O lint registra 1455 avisos; eles não estão resolvidos integralmente. As renderizações nativas não incluem os ícones desenhados pelas barras do sistema.

## Limites

O projeto original usa dados mock. Login, cadastro, alterações de conta, ofertas, pontos e respostas da Lô continuam locais. Google OAuth, e-mails, sincronização, localização real e notificações precisam de integração com os serviços correspondentes. Os estados de sucesso do protótipo não comprovam operações no servidor.

Algumas telas de localização do Figma não têm preenchimento de fundo no tema escuro. O aplicativo aplica o fundo escuro para manter a leitura. Quebras de linha e medidas tipográficas ainda podem variar entre Android e Figma; a revisão não afirma igualdade pixel a pixel.

Os testes automatizados rodam somente no emulador. A instalação por USB no aparelho é feita com atualização do APK, preservando os dados existentes.

## Instalação por USB

O APK final foi atualizado com `adb install -r` no aparelho conectado, modelo 2311DRK48G. O comando retornou `Success`. O SHA-256 do APK instalado foi lido no aparelho e corresponde ao arquivo compilado: `365912cbe6acd5c1e6924c4229e43fda4e525c5e637fa343af2e303ea5d9ff05`.
