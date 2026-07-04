package eu.kalafatic.evolution.forge.agent;

import fi.iki.elonen.NanoHTTPD;
import java.io.IOException;
import java.util.Map;

public class EvoForgeServer extends NanoHTTPD {
    private String status = "Idle";
    private int progress = 0;

    public EvoForgeServer(int port) {
        super(port);
    }

    public void updateProgress(String status, int progress) {
        this.status = status;
        this.progress = progress;
    }

    @Override
    public Response serve(IHTTPSession session) {
        String html = "<html><head><title>Evo Forge Agent Dashboard</title>" +
                      "<style>body{font-family:sans-serif;margin:40px;background:#f4f4f9;}" +
                      ".container{background:white;padding:20px;border-radius:8px;box-shadow:0 2px 4px rgba(0,0,0,0.1);}" +
                      ".progress-bar{width:100%;background:#ddd;border-radius:4px;overflow:hidden;}" +
                      ".progress-fill{height:20px;background:#4caf50;width:" + progress + "%;transition:width 0.3s;}" +
                      "</style></head><body>" +
                      "<div class='container'><h1>Evo Forge Agent</h1>" +
                      "<p>Status: <strong>" + status + "</strong></p>" +
                      "<div class='progress-bar'><div class='progress-fill'></div></div>" +
                      "<p>Progress: " + progress + "%</p>" +
                      "</div></body></html>";
        return newFixedLengthResponse(html);
    }
}
