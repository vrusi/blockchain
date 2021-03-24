import java.util.ArrayList;
import java.util.Set;
import java.util.stream.Collectors;

public class CompliantNode implements Node {

    private double p_graph;
    private double p_byzantine;
    private double p_txDistribution;
    private int numRounds;
    private boolean[] followees;
    private ArrayList<Boolean> flaggedNodes;

    private Set<Transaction> pendingTransactions;

    public CompliantNode(double p_graph, double p_byzantine, double p_txDistribution, int numRounds) {
        this.p_graph = p_graph;
        this.p_byzantine = p_byzantine;
        this.p_txDistribution = p_txDistribution;
        this.numRounds = numRounds;
        this.flaggedNodes = new ArrayList<>();
    }


    public void setFollowees(boolean[] followees) {
        this.followees = followees;

        for (int i = 0; i < followees.length; i++) {
            this.flaggedNodes.add(false);
        }
    }

    public void setPendingTransaction(Set<Transaction> pendingTransactions) {
        this.pendingTransactions = pendingTransactions;
    }

    public Set<Transaction> sendToFollowers() {
        return this.pendingTransactions;
    }

    private boolean isFollowee(int node_index) {
        return this.followees[node_index];
    }

    private boolean isFlagged(int node_index) {
        return this.flaggedNodes.get(node_index);
    }

    private void setFlag(int node_index, boolean value) {
        this.flaggedNodes.set(node_index, value);
    }


    public void receiveFromFollowees(Set<Candidate> candidates) {
        Set<Integer> senders = candidates.stream().map(candidate -> candidate.sender).collect(Collectors.toSet());

        for (int node_index = 0; node_index < followees.length; node_index++) {
            if (!isFollowee(node_index)) {
                continue;
            } else if (!senders.contains(node_index)) {
                setFlag(node_index, true);
            }
        }

        for (Candidate candidate : candidates) {
            if (!isFlagged(candidate.sender)) {
                this.pendingTransactions.add(candidate.tx);
            }
        }
    }
}
