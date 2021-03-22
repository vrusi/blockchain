import java.util.ArrayList;

// Blockchain by mal na uspokojenie funkcií udržiavať iba obmedzené množstvo uzlov
// Nemali by ste mať všetky bloky pridané do blockchainu v pamäti  
// pretože by to spôsobilo pretečenie pamäte.

public class BlockChain {
    public static final int CUT_OFF_AGE = 12;

    private ArrayList<Node> blocks = new ArrayList<>();
    private Node maxHeightNode;
    private TransactionPool transactionPool;

    // nech mozem mat stromovu strukturu
    private class Node {
        private Block block;
        private Node parentNode;
        private ArrayList<Node> children;
        private Integer height;
        private UTXOPool utxoPool;

        public Node(Block block, Node parentNode, UTXOPool utxoPool) {
            this.block = block;
            this.parentNode = parentNode;
            this.children = new ArrayList<>();
            this.height = parentNode != null ? parentNode.height + 1 : 1;
            this.utxoPool = utxoPool;
        }

        public Block getBlock() {
            return block;
        }

        public Integer getHeight() {
            return height;
        }

        public UTXOPool getUtxoPool() {
            return utxoPool;
        }

        public Node getParentNode() {
            return parentNode;
        }

        public ArrayList<Node> getChildren() {
            return children;
        }

        public void addChild(Node child) {
            this.children.add(child);
        }
    }

    public Node getParentNode(byte[] prevBlockHash) {
        for (Node node : this.blocks) {
            if (node.block.getHash() == prevBlockHash) {
                return node;
            }
        }
        return null;
    }

    private UTXOPool getUTXOPoolFromTx(Transaction tx) {
        UTXOPool utxoPool = new UTXOPool();
        int output_index = 0;
        for (Transaction.Output output : tx.getOutputs()) {
            UTXO utxo = new UTXO(tx.getHash(), output_index++);
            utxoPool.addUTXO(utxo, output);
        }
        return utxoPool;
    }

    /**
     * vytvor prázdny blockchain iba s Genesis blokom. Predpokladajme, že
     * {@code genesisBlock} je platný blok
     */
    public BlockChain(Block genesisBlock) {
        UTXOPool genesisUtxo = this.getUTXOPoolFromTx(genesisBlock.getCoinbase());
        Node genesisNode = new Node(genesisBlock, null, genesisUtxo);

        this.transactionPool = new TransactionPool();
        this.maxHeightNode = genesisNode;
    }

    /**
     * Získaj maximum height blok
     */
    public Block getMaxHeightBlock() {
        return this.maxHeightNode.getBlock();
    }

    /**
     * Získaj UTXOPool na ťaženie noveho bloku na vrchu max height bloku
     */
    public UTXOPool getMaxHeightUTXOPool() {
        return this.maxHeightNode.getUtxoPool();
    }

    /**
     * Získaj pool transakcií na vyťaženie nového bloku
     */
    public TransactionPool getTransactionPool() {
        return this.transactionPool;
    }

    public boolean isTxValid(Transaction tx) {
        UTXOPool utxoPool = getUTXOPoolFromTx(tx);
        HandleTxs txHandler = new HandleTxs(utxoPool);
        return txHandler.txIsValid(tx);
    }

    private boolean isTransactionPoolEmpty() {
        return this.transactionPool.getTransactions().isEmpty();
    }

    private boolean areTransactionsValid(Block block) {
        for (Transaction tx : block.getTransactions()) {
            if (!this.isTxValid(tx))
                return false;
        }
        return true;
    }

    private boolean isBlockNull(Block block) {
        return block.getHash() == null || block.getRawBlock() == null;
    }

    private boolean hasParent(Block block) {
        return block.getPrevBlockHash() == null;
    }

    private boolean isUnderCutOffAge() {
        Integer maxHeight = this.maxHeightNode.getHeight();
        return maxHeight + 1 > maxHeight - CUT_OFF_AGE;
    }

    // blok nie je valid ak nema transakcie,
    // ak niektora transakcia nie je platna,
    // ak je block null
    // ak nema parenta
    // ak presahuje vysku blockchainu
    private boolean isBlockValid(Block block) {
        return !isTransactionPoolEmpty() && areTransactionsValid(block) && !isBlockNull(block) && hasParent(block) && isUnderCutOffAge();
    }

    /**
     * Pridaj {@code block} do blockchainu, ak je platný. Kvôli platnosti by mali
     * byť všetky transakcie platné a blok by mal byť na
     * {@code height > (maxHeight - CUT_OFF_AGE)}.
     * <p>
     * Môžete napríklad vyskúšať vytvoriť nový blok nad blokom Genesis (výška bloku
     * 2), ak height blockchainu je {@code <=
     * CUT_OFF_AGE + 1}. Len čo {@code height > CUT_OFF_AGE + 1}, nemôžete vytvoriť
     * nový blok vo výške 2.
     *
     * @return true, ak je blok úspešne pridaný
     */
    public boolean blockAdd(Block block) {
        // najprv check ci je valid
        if (!isBlockValid(block))
            return false;

        // block je valid, teraz ho mozem zaclenit do blockchainu

        // updatnem utxopool parenta a transaction pool blockchainu
        Node parent = getParentNode(block.getPrevBlockHash());

        // pre istotu este jeden check ci sa nasiel parent
        if (parent == null) {
            return false;
        }

        UTXOPool parentUtxoPool = new UTXOPool(parent.getUtxoPool());
        UTXOPool childUtxoPool = new UTXOPool();
        for (Transaction tx : block.getTransactions()) {

            // zmazem spent outputy z parentovho utxopoolu
            for (Transaction.Input input : tx.getInputs()) {
                UTXO utxoToRemove = new UTXO(input.prevTxHash, input.outputIndex);
                parentUtxoPool.removeUTXO(utxoToRemove);
            }

            // pridam outputy do utxo poolu childa
            int output_index = 0;
            for (Transaction.Output output : tx.getOutputs()) {
                UTXO utxoToAdd = new UTXO(tx.getHash(), output_index++);
                childUtxoPool.addUTXO(utxoToAdd, output);
            }

            // transakcie ktore su v bloku zmazem z transaction poolu
            this.transactionPool.removeTransaction(tx.getHash());
        }

        // pridam blok do blockchainu
        Node newNode = new Node(block, parent, childUtxoPool);
        this.blocks.add(newNode);
        this.maxHeightNode = newNode;
        parent.addChild(newNode);
        return true;
    }

    /**
     * Pridaj transakciu do transakčného poolu
     */
    public void transactionAdd(Transaction tx) {
        if (this.isTxValid(tx)) {
            this.transactionPool.addTransaction(tx);
        }
    }
}