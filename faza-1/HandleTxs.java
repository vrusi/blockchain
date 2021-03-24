import java.util.ArrayList;
public class HandleTxs {

    public UTXOPool ledger;
    
    /**
     * Vytvorí verejný ledger, ktorého aktuálny UTXOPool (zbierka nevyčerpaných
     * transakčných výstupov) je {@code utxoPool}. Malo by to vytvoriť obchrannú kópiu
     * utxoPool pomocou konštruktora UTXOPool (UTXOPool uPool).
     */
    public HandleTxs(UTXOPool utxoPool) {
      this.ledger = new UTXOPool(utxoPool);
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

        
        // (1) vsetky vystupy narokovane mnou musia byt v aktualnom UTXO poole, cize
        // pre kazdy moj input pozri ci utxo z predoslej transakcie je v mojom utxo poole
        int input_index = 0;
        for (Transaction.Input input : tx.getInputs()) {
            UTXO utxoPrev = new UTXO(input.prevTxHash, input.outputIndex);

            // (1)
            if (!ledger.contains(utxoPrev)) {
                System.out.println("(1) failed");
                return false;
            }

            System.out.println("(1) passed");

            // (2) podpisy na každom vstupe transakcie sú platné, cize
            // pre vsetky outputy napojene na moje inputy skontroluj podpisy
            Transaction.Output output = this.ledger.getTxOutput(utxoPrev);
            bool isVerified = Crypto.verifySignature(output.address, tx.getRawDataToSign(input_index++), input.signature);
            if (!isVerified) {
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
            
            // (5) súčet vstupných hodnôt {@code tx}s je väčší alebo rovný súčtu jej výstupných hodnôt
            if (outputSum > inputSum) {
                System.out.println("(5) failed");
                return false;
            }

            System.out.println("(5) passed");

        }

        System.out.println("transaction is valid");
        return true;
    }
   
    /**
     * Spracováva každú epochu prijímaním neusporiadaného radu navrhovaných
     * transakcií, kontroluje správnosť každej transakcie, vracia pole vzájomne
     * platných prijatých transakcií a aktualizuje aktuálny UTXO pool podľa potreby.
     */
    public Transaction[] txHandler(Transaction[] possibleTxs) {
        ArrayList<Transaction> txsValid = new ArrayList<>();
        
        for (Transaction tx : possibleTxs) {
            // kontroluje správnosť každej transakcie
            if (!txIsValid(tx)) {
                continue;
            }
            
            txsValid.add(tx);

            // aktualizuje aktuálny UTXO pool podľa potreby
            // z inputov transakcie povytahuje UTXOs a zmaze ich z aktualneho UTXO poolu
            for (Transaction.Input input : tx.getInputs()) {
                UTXO utxoToRemove = new UTXO(input.prevTxHash, input.outputIndex);
                this.ledger.removeUTXO(utxoToRemove);
            }

            // vytvori nove UTXOs z outputov transakcie a popridava ich do aktualneho UTXO poolu
            int output_index = 0;
            for (Transaction.Output output : tx.getOutputs()) {
                UTXO utxoToAdd = new UTXO(tx.getHash(), output_index++);
                this.ledger.addUTXO(utxoToAdd, output);
            }
        }

        // vracia pole vzájomne platných prijatých transakcií
        return txsValid.toArray(new Transaction[0]);
    }
}
