
import java.security.PublicKey;

public class BlockHandler {
    private BlockChain blockChain;

    /** Predpokladajme, že blockchain má Genesis blok */
    public BlockHandler(BlockChain blockChain) {
        this.blockChain = blockChain;
    }

    /**
     * pridaj {@code block} do blockchainu ak je platný.
     * 
     * @return true ak je blok platný a bol pridaný, inak false
     */
    public boolean blockProcess(Block block) {
        if (block == null)
            return false;
        return blockChain.addBlock(block);
    }

    /** vytvor nový {@code block} nad max height {@code block} */
    public Block blockCreate(PublicKey myAddress) {
        Block parent = blockChain.getMaxHeightBlock();
        byte[] parentHash = parent.getHash();
        Block current = new Block(parentHash, myAddress);
        UTXOPool uPool = blockChain.getMaxHeightUTXOPool();
        TransactionPool txPool = blockChain.getTransactionPool();
        TxHandler handler = new TxHandler(uPool);
        Transaction[] txs = txPool.getTransactions().toArray(new Transaction[0]);
        Transaction[] rTxs = handler.handleTxs(txs);
        for (int i = 0; i < rTxs.length; i++)
            current.transactionAdd(rTxs[i]);

        current.finalize();
        if (blockChain.addBlock(current))
            return current;
        else
            return null;
    }

    /** spracuj {@code Transaction} */
    public void txProcess(Transaction tx) {
        blockChain.addTransaction(tx);
    }
}
