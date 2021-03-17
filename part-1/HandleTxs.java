import java.util.ArrayList;
public class HandleTxs {

    private UTXOPool utxoPoolCurrent;
    
    /**
     * Vytvorí verejný ledger, ktorého aktuálny UTXOPool (zbierka nevyčerpaných
     * transakčných výstupov) je {@code utxoPool}. Malo by to vytvoriť obchrannú kópiu
     * utxoPool pomocou konštruktora UTXOPool (UTXOPool uPool).
     */
    public HandleTxs(UTXOPool utxoPool) {
      utxoPoolCurrent = new UTXOPool(utxoPool);
    }

    /**
     * @return true, ak 
     * (1) sú všetky výstupy nárokované {@code tx} v aktuálnom UTXO pool, 
     * (2) podpisy na každom vstupe {@code tx} sú platné, 
     * (3) žiadne UTXO nie je nárokované viackrát, 
     * (4) všetky výstupné hodnoty {@code tx}s sú nezáporné a 
     * (5) súčet vstupných hodnôt {@code tx}s je väčší alebo rovný súčtu jej
     *     výstupných hodnôt; a false inak.
     */
    public boolean txIsValid(Transaction tx) {
        // na kontrolu (3) (spent transaction outputs)
        ArrayList<UTXO> stxos = new ArrayList<>();

        // na kontrolu (5)
        double inputSum = 0;
        double outputSum = 0;

        // (1) vsetky vystupy narokovane mnou su v aktualnom UTXO poole, cize
        // pre kazdy moj input pozri ci utxo z predoslej transakcie
        // je v mojom utxo poole
        for (int input_index = 0; input_index < tx.numInputs(); input_index++) {
            Transaction.Input input = tx.getInput(input_index);

            UTXO utxoPrev = new UTXO(input.prevTxHash, input.outputIndex);
            if (utxoPoolCurrent.contains(utxoPrev)) {
                return false;
            }

            // (2) podpisy na každom vstupe transakcie sú platné, cize
            // pre vsetky outputy napojene na moje inputy skontroluj podpisy
            Transaction.Output output = utxoPoolCurrent.getTxOutput(utxoPrev);
            if (!Crypto.verifySignature(output.address,
                                        tx.getRawDataToSign(input_index),
                                        input.signature)) {
                return false;
            }

            // (3) žiadne UTXO nie je nárokované viackrát, 
            if (stxos.contains(utxoPrev)) {
                return false;
            } else {
                stxos.add(utxoPrev);
            }

            // (5)
            inputSum += output.value;
        }

        // (4) všetky výstupné hodnoty {@code tx}s sú nezáporné
        for (Transaction.Output output : tx.getOutputs()) {
            if (output.value < 0) {
                return false;
            } else {    
                // (5)
                outputSum += output.value;
            }

            // (5) súčet vstupných hodnôt {@code tx}s je väčší alebo rovný súčtu jej výstupných hodnôt
            if (outputSum > inputSum) {
                return false;
            }
        }

        return true;
    }

    /**
     * Spracováva každú epochu prijímaním neusporiadaného radu navrhovaných
     * transakcií, kontroluje správnosť každej transakcie, vracia pole vzájomne 
     * platných prijatých transakcií a aktualizuje aktuálny UTXO pool podľa potreby.
     */
    public Transaction[] txHandler(Transaction[] possibleTxs)
    {
        ArrayList<Transaction> txsValid = new ArrayList<>();
        for (Transaction tx : possibleTxs) {
            // kontroluje správnosť každej transakcie
            if (txIsValid(tx)) {
                txsValid.add(tx);
            }

            // aktualizuje aktuálny UTXO pool podľa potreby
            // zmaze vsetky povodne utxo z aktualneho utxo poolu
            for (Transaction.Input input : tx.getInputs()) {
                UTXO utxoPrev = new UTXO(input.prevTxHash, input.outputIndex);
                utxoPoolCurrent.removeUTXO(utxoPrev);
            }

            // prida nove utxo pre vsetky outputy transakcie
            for (int output_index = 0 ; output_index < tx.numOutputs(); output_index++) {
                UTXO utxo = new UTXO(tx.getHash(), output_index);
                utxoPoolCurrent.addUTXO(utxo, tx.getOutput(output_index));
            }

        }

        //  vracia pole vzájomne platných prijatých transakcií
        return txsValid.toArray(new Transaction[0]);
    }
}
