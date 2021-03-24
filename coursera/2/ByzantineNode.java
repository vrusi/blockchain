import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

public class ByzantineNode implements Node {

    public ByzantineNode(double p_graph, double p_byzantine, double p_txDistribution, int numRounds) {
    }

    public void setFollowees(boolean[] followees) {
        return;
    }

    public void setPendingTransaction(Set<Transaction> pendingTransactions) {
        return;
    }

    public Set<Transaction> sendToFollowers() {
        return new HashSet<Transaction>();
    }

    public void receiveFromFollowees(Set<Candidate> candidates) {
        return;
    }
}
