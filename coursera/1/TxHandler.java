import java.util.ArrayList;

public class TxHandler {

    public UTXOPool ledger;

    public TxHandler(UTXOPool utxoPool) {
        this.ledger = new UTXOPool(utxoPool);
    }

    public boolean isValidTx(Transaction tx) {

        ArrayList<UTXO> stxos = new ArrayList<>();

        double inputSum = 0;
        double outputSum = 0;

        int input_index = 0;
        for (Transaction.Input input : tx.getInputs()) {
            UTXO utxoPrev = new UTXO(input.prevTxHash, input.outputIndex);

            if (!ledger.contains(utxoPrev)) {
                System.out.println("(1) failed");
                return false;
            }

            System.out.println("(1) passed");

            Transaction.Output output = ledger.getTxOutput(utxoPrev);
            if (!Crypto.verifySignature(output.address, tx.getRawDataToSign(input_index++), input.signature)) {
                System.out.println("(2) failed");
                return false;
            }

            System.out.println("(2) passed");

            if (stxos.contains(utxoPrev)) {
                System.out.println("(3) failed");
                return false;
            }

            stxos.add(utxoPrev);
            System.out.println("(3) passed");

            inputSum += output.value;
        }

        for (Transaction.Output output : tx.getOutputs()) {
            if (output.value < 0) {
                System.out.println("(4) failed");
                return false;
            }

            System.out.println("(4) passed");

            outputSum += output.value;

            if (outputSum > inputSum) {
                System.out.println("(5) failed");
                return false;
            }

            System.out.println("(5) passed");

        }

        System.out.println("transaction is valid");
        return true;
    }

    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        ArrayList<Transaction> txsValid = new ArrayList<>();

        for (Transaction tx : possibleTxs) {

            if (!isValidTx(tx)) {
                continue;
            }

            txsValid.add(tx);

            for (Transaction.Input input : tx.getInputs()) {
                UTXO utxoPrev = new UTXO(input.prevTxHash, input.outputIndex);
                ledger.removeUTXO(utxoPrev);
            }

            int output_index = 0;
            for (Transaction.Output output : tx.getOutputs()) {
                UTXO utxoToAdd = new UTXO(tx.getHash(), output_index++);
                this.ledger.addUTXO(utxoToAdd, output);
            }
        }

        return txsValid.toArray(new Transaction[0]);
    }
}
