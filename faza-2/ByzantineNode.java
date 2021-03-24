import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

public class ByzantineNode implements Node {

    public ByzantineNode(double p_graph, double p_byzantine, double p_txDistribution, int numRounds) {
    }

    public void followeesSet(boolean[] followees) {
        return;
    }

    public void pendingTransactionSet(Set<Transaction> pendingTransactions) {
        return;
    }

    public Set<Transaction> followersSend() {
        return new HashSet<Transaction>();
    }

    public void followeesReceive(Set<Candidate> candidates) {
        return;
    }
}
