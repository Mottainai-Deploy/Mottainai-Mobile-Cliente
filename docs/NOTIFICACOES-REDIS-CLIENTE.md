# Avisos de ofertas no Cliente

## Experiência do cliente

Em **Perfil → Avisos de ofertas**, um popup permite ativar ou desativar o recebimento. Cancelar preserva a configuração atual. Durante o envio, a interface exibe carregamento e bloqueia envios duplicados; após confirmação, fecha e informa o resultado. A permissão de notificações do Android é solicitada quando necessária.

Não existe uma caixa de entrada ou página de configurações adicional. Ao tocar em uma notificação individual, o app abre o detalhe da oferta e confirma a leitura daquele aviso. Um agrupamento abre Ofertas. A Activity antiga permanece apenas para encaminhar notificações já entregues.

## Integração

1. O usuário entra pelo Firebase e possui perfil na API Cliente.
2. O Android obtém acesso de curta duração à API Redis por uma rota autenticada da API Cliente.
3. A preferência, o raio de 5 ou 10 km e a posição recente são sincronizados com a API Cliente.
4. A API Cliente seleciona e publica os avisos; o Android consulta o Redis e apresenta notificações nativas.
5. A leitura é registrada ao abrir um aviso individual.

O aplicativo não concede pontos, escolhe destinatários nem publica campanhas. Limite de três produtos diferentes por loja/dia, prioridade dos 20% com mais pontos próximos e espera de 30 minutos para os demais são regras do servidor. A implantação precisa de SQL compatível, permissões, produtor habilitado e conexão entre API Cliente e Redis.

## Configuração local

Configurar em `local.properties` (ignorado pelo Git):

```properties
MOTTAINAI_NOTIFICATIONS_BASE_URL=http://127.0.0.1:8083/
MOTTAINAI_NOTIFICATIONS_TOKEN_PATH=api/v1/client/notifications/access-token
```

A URL deve ser acessível pelo aparelho. Para desenvolvimento por USB, encaminhar também a porta da API Cliente e manter o cabo conectado:

```powershell
adb reverse tcp:8081 tcp:8081
adb reverse tcp:8083 tcp:8083
```

Confirmar o caminho de emissão de acesso no contrato da API implantada. Não colocar segredos Redis, chaves administrativas ou credenciais do servidor no APK. HTTP local é somente uma configuração de depuração; distribuição exige HTTPS acessível ao dispositivo.

## Contratos e segurança

O cliente Redis consulta `GET v1/notifications` e confirma leitura em `POST v1/notifications/{id}/read`. O campo `createdAt` recebido em segundos é normalizado para apresentação. Metadados da oferta e expiração são validados antes da navegação.

O repositório confere a identidade ao concluir operações assíncronas. Preferências e avisos auxiliares são separados por conta. A posição enviada deve ser recente; a aplicação não salva as coordenadas nesse armazenamento auxiliar. Ao sair, tarefas da sessão anterior são canceladas.

## Limites de execução

Com o app aberto, há consulta periódica. Em segundo plano, JobScheduler usa intervalo mínimo de 15 minutos, sujeito a bateria, rede e políticas do fabricante. Localização em segundo plano exige autorização específica. Não há garantia de aviso instantâneo, execução após parada forçada ou funcionamento sem internet. Esta integração usa Redis; Firebase permanece responsável pela autenticação, sem exigir FCM para esse fluxo.

## Teste manual

1. Entrar, permitir localização e abrir Perfil → Avisos de ofertas.
2. Cancelar e conferir que nada mudou; depois ativar e aguardar confirmação.
3. Gerar uma promoção elegível pelos serviços configurados e aguardar o aviso nativo.
4. Tocar e conferir a oferta correta; testar também uma oferta indisponível.
5. Desativar, trocar de conta e verificar isolamento.
6. Repetir em segundo plano e com restrições de bateria do dispositivo de entrega.
