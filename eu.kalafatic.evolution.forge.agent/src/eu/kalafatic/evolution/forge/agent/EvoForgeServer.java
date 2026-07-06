package eu.kalafatic.evolution.forge.agent;

import fi.iki.elonen.NanoHTTPD;

public class EvoForgeServer extends NanoHTTPD {
    private String status = "Idle";
    private int progress = 0;
    public EvoForgeServer(int port) { super(port); }
    public void updateProgress(String status, int progress) { this.status = status; this.progress = progress; }
    @Override
    public Response serve(IHTTPSession session) {
        String html = "<html><body><h1>Evo Forge Agent</h1><p>Status: " + status + "</p><p>Progress: " + progress + "%</p></body></html>";
        return newFixedLengthResponse(html);
    }
}
