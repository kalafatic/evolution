package eu.kalafatic.evolution.supervisor.model;

import java.util.List;

public class DarwinProposalResponse {
    private String protocolVersion = "1.0.0";
    private List<Proposal> proposals;

    public static class Proposal {
        private String id;
        private String strategy;
        private String diff;
        private double score;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getStrategy() { return strategy; }
        public void setStrategy(String strategy) { this.strategy = strategy; }
        public String getDiff() { return diff; }
        public void setDiff(String diff) { this.diff = diff; }
        public double getScore() { return score; }
        public void setScore(double score) { this.score = score; }
    }

    public String getProtocolVersion() { return protocolVersion; }
    public void setProtocolVersion(String protocolVersion) { this.protocolVersion = protocolVersion; }

    public List<Proposal> getProposals() { return proposals; }
    public void setProposals(List<Proposal> proposals) { this.proposals = proposals; }
}
