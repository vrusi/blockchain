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
        // na kontrolu (3)
        ArrayList<UTXO> UTXOsSpent = new ArrayList<>();

        // na kontrolu (5)
        double inputSum = 0;
        double outputSum = 0;

        // (1) vsetky vystupy narokovane mnou su v aktualnom UTXO poole, cize
        // pre kazdy moj input pozri ci utxo z predoslej transakcie
        // je v mojom utxo poole
        for (int input_index = 0; input_index < tx.numInputs(); input_index++) {
            Transaction.Input input = tx.getInput(input_index);

            UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
            if (utxoPoolCurrent.contains(utxo)) {
                return false;
            }

            // (2) podpisy na každom vstupe transakcie sú platné, cize
            // pre vsetky outputy napojene na moje inputy skontroluj podpisy
            Transaction.Output output = utxoPoolCurrent.getTxOutput(utxo);
            if (!Crypto.verifySignature(output.address,
                                        tx.getRawDataToSign(input_index),
                                        input.signature)) {
                return false;
            }

            // (3) žiadne UTXO nie je nárokované viackrát, 
            if (UTXOsSpent.contains(utxo)) {
                return false;
            } else {
                UTXOsSpent.add(utxo);
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
        return null;
    }
}
