import java.util.ArrayList;

public class BlockChain {
    public static final int CUT_OFF_AGE = 12;

    private ArrayList<Node> blocks = new ArrayList<>();
    private Node maxHeightNode;
    private TransactionPool transactionPool;

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
            if (node == null || node.block == null)
                return null;

            if (node.block.getHash() == prevBlockHash) {
                return node;
            }
        }
        return null;
    }

    private UTXOPool getUTXOPoolFromTx(Transaction tx) {
        if (tx == null)
            return null;

        UTXOPool utxoPool = new UTXOPool();
        int output_index = 0;
        for (Transaction.Output output : tx.getOutputs()) {
            if (output == null)
                continue;

            UTXO utxo = new UTXO(tx.getHash(), output_index++);

            if (utxo == null)
                continue;

            utxoPool.addUTXO(utxo, output);
        }
        return utxoPool;
    }

    public BlockChain(Block genesisBlock) {
        UTXOPool genesisUtxoPool = this.getUTXOPoolFromTx(genesisBlock.getCoinbase());
        Node genesisNode = new Node(genesisBlock, null, genesisUtxoPool);

        this.transactionPool = new TransactionPool();
        this.transactionPool.addTransaction(genesisBlock.getCoinbase());
        this.maxHeightNode = genesisNode;
    }

    public Block getMaxHeightBlock() {
        return this.maxHeightNode.getBlock();
    }

    public UTXOPool getMaxHeightUTXOPool() {
        return this.maxHeightNode.getUtxoPool();
    }

    public TransactionPool getTransactionPool() {
        return this.transactionPool;
    }

    private boolean isTxValid(Transaction tx) {
        UTXOPool utxoPool = getUTXOPoolFromTx(tx);
        if (utxoPool == null)
            return false;

        TxHandler txHandler = new TxHandler(utxoPool);

        if (txHandler == null)
            return false;

        return txHandler.isValidTx(tx);
    }

    private boolean isTransactionPoolEmpty() {
        return ((this.transactionPool == null) || (this.transactionPool.getTransactions() != null && this.getTransactionPool().getTransactions().isEmpty()));
    }

    private boolean areTransactionsValid(Block block) {
        for (Transaction tx : block.getTransactions()) {
            if (tx == null) {
                return false;
            }

            if (!this.isTxValid(tx))
                return false;
        }
        return true;
    }

    private boolean isBlockNull(Block block) {
        return block == null || block.getHash() == null || block.getRawBlock() == null;
    }

    private boolean hasParent(Block block) {
        return block.getPrevBlockHash() == null;
    }

    private boolean isUnderCutOffAge() {
        Integer maxHeight = this.maxHeightNode.getHeight();
        return maxHeight + 1 > maxHeight - CUT_OFF_AGE;
    }

    private boolean isBlockValid(Block block) {
        return !isTransactionPoolEmpty() && areTransactionsValid(block) && !isBlockNull(block) && hasParent(block) && isUnderCutOffAge();
    }


    public boolean addBlock(Block block) {
        System.out.println("in addBlock(Block)");
        if (!isBlockValid(block))
            return false;


        Node parent = getParentNode(block.getPrevBlockHash());

        if (parent == null) {
            return false;
        }

        UTXOPool parentUtxoPool = new UTXOPool(parent.getUtxoPool());
        UTXOPool childUtxoPool = new UTXOPool();
        for (Transaction tx : block.getTransactions()) {
            if (tx == null)
                continue;

            for (Transaction.Input input : tx.getInputs()) {
                if (input == null)
                    continue;

                UTXO utxoToRemove = new UTXO(input.prevTxHash, input.outputIndex);

                if (utxoToRemove == null)
                    continue;

                parentUtxoPool.removeUTXO(utxoToRemove);
            }

            int output_index = 0;
            for (Transaction.Output output : tx.getOutputs()) {
                UTXO utxoToAdd = new UTXO(tx.getHash(), output_index++);

                if (utxoToAdd == null || output == null)
                    continue;

                childUtxoPool.addUTXO(utxoToAdd, output);
            }

            this.transactionPool.removeTransaction(tx.getHash());
        }

        Node newNode = new Node(block, parent, childUtxoPool);

        if (newNode == null)
            return false;

        this.blocks.add(newNode);
        this.maxHeightNode = newNode;
        parent.addChild(newNode);
        return true;
    }


    public void addTransaction(Transaction tx) {
        if (this.isTxValid(tx)) {
            this.transactionPool.addTransaction(tx);
        }
    }
}