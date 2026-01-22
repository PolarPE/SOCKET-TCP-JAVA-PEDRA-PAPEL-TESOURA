import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class servidor {
    private static final int PORT = 12345;
    private static final int PLAYERS_REQUIRED = 3;

    public static void main(String[] args) throws IOException {
        new servidor().start();
    }

    
    private final BlockingQueue<Message> globalQueue = new LinkedBlockingQueue<>();

    private void start() throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Servidor iniciado na porta " + PORT);
        System.out.println("Aguardando " + PLAYERS_REQUIRED + " jogadores...");

        List<Player> players = new ArrayList<>();
        for (int i = 0; i < PLAYERS_REQUIRED; i++) {
            Socket s = serverSocket.accept();
            Player p = new Player(s, i + 1);
            players.add(p);
            p.sendMessage("TELA " + p.id);
            p.sendMessage("MENSAGEM Bem-vindo, jogador " + p.id);
            broadcast(players, "MENSAGEM Jogador " + p.id + " entrou.");
            System.out.println("Jogador " + p.id + " conectado: " + p.getRemote());
        }

     
        boolean running = true;
        int round = 1;
        while (running) {
            
            broadcast(players, "PEDIR_CONFIRMACAO");
            boolean allConfirmed = waitForAllConfirm(players);
            if (!allConfirmed) {
                System.out.println("Nem todos confirmaram. Encerrando servidor.");
                broadcast(players, "MENSAGEM Nem todos confirmaram. Encerrando servidor.");
                break;
            }

            broadcast(players, "MENSAGEM Iniciando partida (rodada " + round + ")");

        
            Map<Integer, String> moves = new ConcurrentHashMap<>();

            broadcast(players, "MENSAGEM Envie sua jogada (JOGADA <R|P|S>). Você pode enviar mesmo fora da sua vez.");
            
            for (Player p : players) {
                p.sendMessage("SUA_VEZ");
                for (Player other : players) if (other != p) other.sendMessage("ESPERE Jogador " + p.id + " está jogando");

                
                while (moves.size() < players.size()) {
                    Message m;
                    try { m = globalQueue.take(); } catch (InterruptedException ie) { continue; }
                    if (m == null) continue;
                    String txt = m.text.trim();
                    if (txt.equalsIgnoreCase("SAIR") || txt.equalsIgnoreCase("NAO")) {
                       
                        Player quitting = findPlayerById(players, m.playerId);
                        if (quitting != null) {
                            broadcast(players, "MENSAGEM Jogador " + quitting.id + " saiu. Encerrando partida.");
                            running = false; break;
                        }
                    }

                    
                    String sym = null; String upper = txt.toUpperCase();
                    if (upper.startsWith("JOGADA ")) sym = txt.substring(7).trim();
                    else if (upper.startsWith("CHOICE ")) sym = txt.substring(7).trim();
                    else if (upper.startsWith("JOGAR ")) sym = txt.substring(6).trim();

                    if (sym != null && !sym.isEmpty()) {
                        sym = sym.toUpperCase();
                        if (sym.equals("ROCK") || sym.equals("R")) sym = "R";
                        else if (sym.equals("PAPER") || sym.equals("P")) sym = "P";
                        else if (sym.equals("SCISSORS") || sym.equals("S") || sym.equals("SCISSORS") || sym.equals("T")) sym = "S";
                        else { Player sender = findPlayerById(players, m.playerId); if (sender != null) sender.sendMessage("MENSAGEM Jogada inválida: use R, P ou S"); continue; }

                        
                        if (!moves.containsKey(m.playerId)) {
                            moves.put(m.playerId, sym);
                            broadcast(players, "MENSAGEM Jogador " + m.playerId + " já fez sua jogada.");
                            
                            sendEstado(players, moves);
                        } else {
                            Player sender = findPlayerById(players, m.playerId); if (sender != null) sender.sendMessage("MENSAGEM Você já jogou nesta rodada.");
                        }
                    }

                    if (!running) break;
                }

                if (!running) break;
            }

            if (!running) break;

           
            if (moves.size() < players.size()) { broadcast(players, "MENSAGEM Partida cancelada (jogador saiu). Encerrando."); break; }

            
            Set<Integer> winners = evaluateWinners(moves);

            if (winners.isEmpty()) { broadcast(players, "EMPATE"); }
            else {
                for (Player p : players) { if (winners.contains(p.id)) p.sendMessage("VENCEU"); else p.sendMessage("PERDEU"); }
            }

            
            broadcast(players, "PERGUNTA_REMATCH");
            boolean allRematch = waitForAllRematch(players);
            if (!allRematch) { broadcast(players, "MENSAGEM Nem todos aceitaram jogar novamente. Encerrando servidor."); running = false; break; }

            round++;
        }

        
        for (Player p : players) { try { p.sendMessage("FIM"); p.close(); } catch (IOException e) {} }
        serverSocket.close(); System.out.println("Servidor encerrado.");
    }

    private static void broadcast(List<Player> players, String msg) { for (Player p : players) p.sendMessage(msg); }

    private boolean waitForAllConfirm(List<Player> players) {
        Set<Integer> confirmed = new HashSet<>();
        while (confirmed.size() < players.size()) {
            try {
                Message m = globalQueue.take();
                if (m.text == null) continue;
                String t = m.text.trim().toUpperCase();
                if (t.equals("CONFIRMA") || t.equals("SIM") || t.equals("S")) { confirmed.add(m.playerId); Player p = findPlayerById(players, m.playerId); if (p != null) p.sendMessage("MENSAGEM Confirmado. Aguardando outros..."); }
                else if (t.equals("SAIR") || t.equals("NAO") || t.equals("N")) return false;
                else { Player p = findPlayerById(players, m.playerId); if (p != null) p.sendMessage("MENSAGEM Envie CONFIRMA para iniciar ou SAIR para cancelar."); }
            } catch (InterruptedException e) { return false; }
        }
        return true;
    }

    private boolean waitForAllRematch(List<Player> players) {
        Set<Integer> agreed = new HashSet<>();
        while (agreed.size() < players.size()) {
            try {
                Message m = globalQueue.take(); if (m.text == null) continue;
                String t = m.text.trim().toUpperCase(); if (t.startsWith("REMATCH ")) t = t.substring(8).trim();
                if (t.equals("SIM") || t.equals("S") || t.equals("YES")) { agreed.add(m.playerId); Player p = findPlayerById(players, m.playerId); if (p != null) p.sendMessage("MENSAGEM Você aceitou jogar novamente. Aguardando outros..."); }
                else if (t.equals("NAO") || t.equals("N") || t.equals("NO") || t.equals("SAIR")) return false;
                else { Player p = findPlayerById(players, m.playerId); if (p != null) p.sendMessage("MENSAGEM Envie REMATCH SIM ou REMATCH NAO."); }
            } catch (InterruptedException e) { return false; }
        }
        return true;
    }

    private static Player findPlayerById(List<Player> players, int id) { for (Player p : players) if (p.id == id) return p; return null; }

    private static void sendEstado(List<Player> players, Map<Integer, String> moves) { 
        StringBuilder sb = new StringBuilder(); 
        for (Player p : players) {
            if (sb.length() > 0) sb.append(" ");
            if (moves.containsKey(p.id)) {
                sb.append("P").append(p.id).append(": JOGOU");
            } else {
                sb.append("P").append(p.id).append(":");
            }
        }
        for (Player p : players) p.sendMessage("ESTADO " + sb.toString()); 
    }

    private static Set<Integer> evaluateWinners(Map<Integer, String> moves) {
        Map<String, List<Integer>> byChoice = new HashMap<>();
        byChoice.put("R", new ArrayList<>());
        byChoice.put("P", new ArrayList<>());
        byChoice.put("S", new ArrayList<>());
        for (Map.Entry<Integer, String> e : moves.entrySet()) byChoice.get(e.getValue()).add(e.getKey());
        boolean r = !byChoice.get("R").isEmpty(); boolean p = !byChoice.get("P").isEmpty(); boolean s = !byChoice.get("S").isEmpty();
        Set<Integer> winners = new HashSet<>();
        if ((r && p && s) || (r && !p && !s) || (!r && p && !s) || (!r && !p && s)) return winners;
        if (r && p) winners.addAll(byChoice.get("P")); else if (r && s) winners.addAll(byChoice.get("R")); else if (p && s) winners.addAll(byChoice.get("S"));
        return winners;
    }

    static class Message { final int playerId; final String text; final long ts; Message(int playerId, String text) { this.playerId = playerId; this.text = text; this.ts = System.currentTimeMillis(); } }

    class Player {
        final Socket socket; final BufferedReader in; final PrintWriter out; final int id;
        Player(Socket s, int id) throws IOException {
            this.socket = s; this.id = id; this.in = new BufferedReader(new InputStreamReader(s.getInputStream())); this.out = new PrintWriter(s.getOutputStream(), true);
            Thread t = new Thread(() -> { try { String line; while ((line = in.readLine()) != null) globalQueue.offer(new Message(this.id, line)); } catch (IOException e) {} }); t.setDaemon(true); t.start();
        }
        void sendMessage(String m) { out.println(m); }
        String getRemote() { return socket.getRemoteSocketAddress().toString(); }
        void close() throws IOException { socket.close(); }
    }
}


