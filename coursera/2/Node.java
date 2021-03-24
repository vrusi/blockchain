import java.util.ArrayList;
import java.util.Set;

public interface Node {

    // POZNÁMKA: Node je rozhranie a nemá konštruktor.
    // Vaša trieda CompliantNode.java však vyžaduje 4-argumentový
    // konštruktor, ako je definované v Simulation.java
    // Tento konštruktor dáva vášmu uzlu informácie o simulácii 
    // vrátane počtu kôl, pre ktoré bude bežať.

    /**
     * {@code followees [i]} je pravda iba ak tento uzol nasleduje uzol {@code i}
     */
    void setFollowees(boolean[] followees);

    /** inicializovať návrhový zoznam transakcií */
    void setPendingTransaction(Set<Transaction> pendingTransactions);

    /**
     * @return návrhy, ktoré pošlem mojim nasledovníkom. Pamätajme: Po finálovom
     *         kole sa správanie {@code getProposals} zmení a malo by vrátiť
     *         transakcie, pri ktorých bol dosiahnutý konsenzus.
     */
    Set<Transaction> sendToFollowers();

    /** príjmy kandidátov z iných uzlov */
    void receiveFromFollowees(Set<Candidate> candidates);
}