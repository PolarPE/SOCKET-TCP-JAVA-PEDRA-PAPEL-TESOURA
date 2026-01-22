import java.io.*;
import java.net.*;

public class Client1 {
	private static final String DEFAULT_HOST = "localhost";
	private static final int DEFAULT_PORT = 12345;

	public static void main(String[] args) {
		String host = args.length > 0 ? args[0] : DEFAULT_HOST;
		int port = args.length > 1 ? Integer.parseInt(args[1]) : DEFAULT_PORT;
		if (System.console() != null) {
			System.out.print("Conectar ao servidor " + host + ":" + port + "? (S/N): ");
			try {
				BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
				String resp = br.readLine();
				if (resp != null) {
					resp = resp.trim().toUpperCase();
					if (resp.equals("S") || resp.equals("SIM")) runClient(host, port);
					else System.out.println("Conexão cancelada.");
				} else {
					System.out.println("Entrada nula. Conexão cancelada.");
				}
			} catch (IOException e) {
				System.err.println("Erro lendo entrada: " + e.getMessage());
			}
		} else {
			runClient(host, port);
		}
	}

	private static void runClient(String host, int port) {
		try (Socket socket = new Socket(host, port);
			 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
			 BufferedReader console = new BufferedReader(new InputStreamReader(System.in))) {


			Thread inputThread = new Thread(() -> {
				try {
					String uline;
					while ((uline = console.readLine()) != null) {
						String t = uline.trim();
						if (t.isEmpty()) continue;
						String up = t.toUpperCase();
						String toSend;
						if (up.equals("R") || up.equals("ROCK")) toSend = "JOGADA R";
						else if (up.equals("P") || up.equals("PAPER")) toSend = "JOGADA P";
						else if (up.equals("S") || up.equals("SCISSORS") || up.equals("SCISSORS") || up.equals("T")) toSend = "JOGADA S";
					else toSend = t;

						out.println(toSend);
					}
				} catch (IOException e) {
				}
			});
			inputThread.setDaemon(true);
			inputThread.start();

			System.out.println("Conectado ao servidor " + host + ":" + port);
			String line;
			while ((line = in.readLine()) != null) {
				if (line.startsWith("TELA ")) {
					System.out.println(line);
				} else if (line.startsWith("MENSAGEM ")) {
					System.out.println(line.substring(8));
				} else if (line.equals("PEDIR_CONFIRMACAO")) {
					System.out.println("Confirmar início da partida? (S/N): (digite S para confirmar)");
				} else if (line.equals("SUA_VEZ")) {
					System.out.println("Sua vez: escolha ROCK, PAPER ou SCISSORS (R/P/S): (pode digitar a qualquer momento)");
				} else if (line.startsWith("ESTADO ") || line.startsWith("CHOICES ")) {
					System.out.println(line.substring(line.indexOf(' ') + 1));
				} else if (line.equals("VENCEU")) {
					System.out.println("Você venceu!\n");
				} else if (line.equals("PERDEU")) {
					System.out.println("Você perdeu.\n");
				} else if (line.equals("EMPATE")) {
					System.out.println("Empate!\n");
				} else if (line.equals("PERGUNTA_REMATCH")) {
					System.out.println("Deseja jogar novamente? (S/N): (digite S para aceitar)");
				} else if (line.equals("FIM")) {
					System.out.println("Servidor finalizou. Conexão encerrada.");
					break;
				} else {
					System.out.println(line);
				}
			}

		} catch (IOException e) {
			System.err.println("Erro de conexão: " + e.getMessage());
		}
	}
}

