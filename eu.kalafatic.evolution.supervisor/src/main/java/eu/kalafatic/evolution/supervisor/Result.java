package eu.kalafatic.evolution.supervisor;

public class Result {
    private String status;
    private double score;

    public Result() {}

    public Result(String status, double score) {
        this.status = status;
        this.score = score;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }

    @Override
    public String toString() {
        return "Result{status='" + status + "', score=" + score + "}";
    }
}
