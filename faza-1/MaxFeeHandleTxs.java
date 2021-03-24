import java.util.ArrayList;

public class MaxFeeHandleTxs {

    public UTXOPool ledger;

    /**
     * Vytvorí verejný ledger, ktorého aktuálny UTXOPool (zbierka nevyčerpaných
     * transakčných výstupov) je {@code utxoPool}. Malo by to vytvoriť obchrannú
     * kópiu utxoPool pomocou konštruktora UTXOPool (UTXOPool uPool).
     */
    public MaxFeeHandleTxs(UTXOPool utxoPool) {
        this.ledger = new UTXOPool(utxoPool);
    }

    /**
     * @return true, ak (1) sú všetky výstupy nárokované {@code tx} v aktuálnom UTXO
     *         pool, (2) podpisy na každom vstupe {@code tx} sú platné, (3) žiadne
     *         UTXO nie je nárokované viackrát, (4) všetky výstupné hodnoty
     *         {@code tx}s sú nezáporné a (5) súčet vstupných hodnôt {@code tx}s je
     *         väčší alebo rovný súčtu jej výstupných hodnôt; a false inak.
     */
    public boolean txIsValid(Transaction tx) {
        // na kontrolu (3) (spent transaction outputs)
        ArrayList<UTXO> stxos = new ArrayList<>();

        // na kontrolu (5)
        double inputSum = 0;
        double outputSum = 0;

        int input_index = 0;

        // (1) vsetky vystupy narokovane mnou su v aktualnom UTXO poole, cize
        // pre kazdy moj input pozri ci utxo z predoslej transakcie
        // je v mojom utxo poole
        for (Transaction.Input input : tx.getInputs()) {
            UTXO utxoPrev = new UTXO(input.prevTxHash, input.outputIndex);

            if (!ledger.contains(utxoPrev)) {
                System.out.println("(1) failed");
                return false;
            }

            System.out.println("(1) passed");

            // (2) podpisy na každom vstupe transakcie sú platné, cize
            // pre vsetky outputy napojene na moje inputy skontroluj podpisy
            Transaction.Output output = this.ledger.getTxOutput(utxoPrev);
            if (!Crypto.verifySignature(output.address, tx.getRawDataToSign(input_index++), input.signature)) {
                System.out.println("(2) failed");
                return false;
            }

            System.out.println("(2) passed");

            // (3) žiadne UTXO nie je nárokované viackrát,
            if (stxos.contains(utxoPrev)) {
                System.out.println("(3) failed");
                return false;
            }

            stxos.add(utxoPrev);
            System.out.println("(3) passed");

            // (5)
            inputSum += output.value;
        }

        // (4) všetky výstupné hodnoty {@code tx}s sú nezáporné
        for (Transaction.Output output : tx.getOutputs()) {
            if (output.value < 0) {
                System.out.println("(4) failed");
                return false;
            }

            System.out.println("(4) passed");

            // (5)
            outputSum += output.value;

            // (5) súčet vstupných hodnôt {@code tx}s je väčší alebo rovný súčtu jej
            // výstupných hodnôt
            if (outputSum > inputSum) {
                System.out.println("(5) failed");
                return false;
            }

            System.out.println("(5) passed");

        }

        System.out.println("transaction is valid");
        return true;
    }

    // vracia poplatok za transakciu vyratanu zo suctu inputov - suctu outputov
    private double getTxFee(Transaction tx) {
        double inputSum = 0;
        for (Transaction.Input input : tx.getInputs()) {
            UTXO utxoPrev = new UTXO(input.prevTxHash, input.outputIndex);
            Transaction.Output output = this.ledger.getTxOutput(utxoPrev);
            if (output == null || output.value < 0)
                continue;
            else
                inputSum += output.value;
        }

        double outputSum = 0;
        for (Transaction.Output output : tx.getOutputs()) {
            if (output == null || output.value < 0)
                continue;
            else
                outputSum += output.value;
        }

        return inputSum - outputSum;
    }

    // vyhľadá množinu transakcií s maximálnymi celkovými poplatkami za transakcie
    // –tj.maximalizuje súčet všetkých transakcií v množine
    // (súčet vstupných hodnôt - súčet výstupných hodnôt).
    public Transaction[] txHandler(Transaction[] possibleTxs) {
        ArrayList<Transaction> maxFeeTxs = new ArrayList<>();

        // najdem maximalny poplatok
        double maxFee = 0;
        for (Transaction tx : possibleTxs) {
            if (!isValidTx(tx)) {
                continue;
            }

            double txFee = getTxFee(tx);
            if (maxFee < txFee) {
                maxFee = txFee;
            }
        }

        // pridaj vsecky transakcie co maju ten max poplatok do arrayu
        for (Transaction tx : possibleTxs) {
            if (isValidTx(tx) && (getTxFee(tx) == maxFee)) {
                maxFeeTxs.add(tx);
            }
        }

        return maxFeeTxs.toArray(new Transaction[0]);
    }

}
