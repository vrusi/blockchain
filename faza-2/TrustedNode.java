import java.util.ArrayList;
import java.util.Set;
import java.util.stream.Collectors;

/* TrustedNode označuje uzol, ktorý dodržuje pravidlá (nie je byzantský) */
public class TrustedNode implements Node {

    private double p_graph;
    private double p_byzantine;
    private double p_txDistribution;
    private int numRounds;
    private boolean[] followees;
    private ArrayList<Boolean> flaggedNodes;

    private Set<Transaction> pendingTransactions;

    public TrustedNode(double p_graph, double p_byzantine, double p_txDistribution, int numRounds) {
        this.p_graph = p_graph;
        this.p_byzantine = p_byzantine;
        this.p_txDistribution = p_txDistribution;
        this.numRounds = numRounds;
        this.flaggedNodes = new ArrayList<>();
    }

    /**
     * {@code followees [i]} je pravda iba ak tento uzol nasleduje uzol {@code i}
     */
    public void followeesSet(boolean[] followees) {
        this.followees = followees;

        for (int i = 0; i < this.followees.length; i++) {
            this.flaggedNodes.add(false);
        }
    }

    /** inicializovať návrhový zoznam transakcií */
    public void pendingTransactionSet(Set<Transaction> pendingTransactions) {
        this.pendingTransactions = pendingTransactions;
    }

    /**
     * @return návrhy, ktoré pošlem mojim nasledovníkom. Pamätajme: Po finálovom
     *         kole sa správanie {@code getProposals} zmení a malo by vrátiť
     *         transakcie, pri ktorých bol dosiahnutý konsenzus.
     */
    public Set<Transaction> followersSend() {
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

    /** príjmi kandidátov z iných uzlov
     * treba tu ocheckovat ci mi neposiela skodlivy uzol, ktory moze:
     *  (1) byť funkčne mŕtvy a nikdy v skutočnosti nevysielať žiadne transakcie,
     *  (2) neustále vysielat svoju vlastnú skupinu transakcií a nikdy neprijímat transakcie, ktoré sú mu dané,
     *  (3) meniť správanie medzi kolami, aby sa zabránilo detekcii.
     */
    public void followeesReceive(Set<Candidate> candidates) {
        // skodlivy uzol moze byt followee ktory mi neposle transakcie,
        // pripadne mi ich posle ale neprijme odo mna
        // takze treba pozriet:
        // 1. ci sender prijima transakcie - co vsak nemam ako zistit,
        // 2. a ci kazdy koho followujem je medzi sendermi
        Set<Integer> senders = candidates.stream().map(candidate -> candidate.sender).collect(Collectors.toSet());

        for (int node_index = 0; node_index < this.followees.length; node_index++) {
            // ak ja nenasledujem uzol tak nic s nim nerobim
            if (!isFollowee(node_index)) {
                continue;
            // ak uzol nasledujem ale nic mi neposlal, tak ho oznacim za nedoveryhodny
            } else if (!senders.contains(node_index)) {
                setFlag(node_index, true);
            }
        }

        // ked sender neni pofiderny tak si pridam kandidatsku tranzakciu do pending txs
        for (Candidate candidate : candidates) {
            if (!isFlagged(candidate.sender)) {
                this.pendingTransactions.add(candidate.tx);
            }
        }
    }
}
