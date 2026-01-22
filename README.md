Jogo Pedra, Papel e Tesoura (3 jogadores) - Java Sockets

Integrantes:
- Italo Cardoso da Silva
- Luan Rodrigues de Miranda
- Miguel Florentino de lima
- Laura Barbosa Clémente
- Keylla Larrisa de Miranda Porfirio

Descrição do jogo:
- Jogo de Pedra, Papel e Tesoura para 3 jogadores usando sockets TCP.
- O servidor controla toda a lógica do jogo (ordem de jogadas, validação, resultados, rematch) e mantém as conexões abertas entre rodadas.

Como compilar:
```bash
cd /home/italo/fazendo
javac servidor.java Client1.java Client2.java Client3.java
```

Como executar:
1) Inicie o servidor:
```bash
java servidor
```
2) Em três terminais separados, conecte 3 clientes:
```bash
java Client1
java Client2
java Client3
```

Comportamento interativo dos clientes:
- Ao executar um cliente no **terminal**, o cliente perguntará:

  Conectar ao servidor <host>:<port>? (S/N):

  Responda `S` ou `SIM` para conectar ou `N`/`NAO` para cancelar.

- Após os 3 jogadores se conectarem, o servidor pede que cada jogador confirme o início (`PEDIR_CONFIRMACAO`).
- Quando todos confirmarem, a partida inicia.
- Em cada rodada, o servidor aceita jogadas (`JOGADA R|P|S`) e controla a ordem das jogadas (cada jogador será notificado com `SUA_VEZ`, mas pode enviar a jogada a qualquer momento).
- O servidor processa as jogadas na ordem de chegada e garante que cada jogador só tenha uma jogada por rodada.
- Após todas as jogadas, o servidor calcula e informa o resultado a todos (`VENCEU` / `PERDEU` / `EMPATE`) e envia o estado das jogadas (`ESTADO P1:JOGOU P2:JOGOU P3:JOGOU`).
- Após a rodada, o servidor pergunta se desejam jogar novamente (`PERGUNTA_REMATCH`). Se todos aceitarem, inicia nova rodada usando as mesmas conexões.
- Se algum jogador escolher não jogar novamente, a conexão será encerrada e o servidor finaliza.

Observações:
- O servidor utiliza uma fila global para processar as mensagens na ordem de chegada, permitindo lidar com jogadas enviadas simultaneamente.
- A ordem de jogadas é mostrada no console a cada jogada e o servidor envia o estado parcial (`ESTADO`) sempre que uma nova jogada é aceita.
- O jogo suporta múltiplas rodadas sem precisar reconectar os clientes.
