/*
* Main pre zadanie 1 na predmete DMBLOCK
*/

import java.math.BigInteger;
import java.security.*;

public class Main {

    public static void main(String[] args)
            throws NoSuchProviderException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {

        /*
         * Vygeneruje náhodné páry kľúčov pre Alicu a Boba
         */
        KeyPair pk_bob = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        KeyPair pk_alice = KeyPairGenerator.getInstance("RSA").generateKeyPair();

        /*
         * Vytvor root transakciu:
         *
         * Generovanie root transakcie tx z ničoho, takže Bob vlastní mincu s hodnotou
         * 10 Z ničoho chcem povedať, že táto tx nebude overená, len ju potrebujem, aby
         * som mal správnu transakciu. Výstup, ktorý potom môžem vložiť do UTXOPool,
         * ktorý bude posunutý HandleTxs.
         */
        Tx tx = new Tx();
        tx.addOutput(10, pk_bob.getPublic());

        // Táto hodnota nemá žiadny význam, ale tx.getRawDataToSign(0) k nej pristúpi v
        // prevTxHash;
        byte[] initialHash = BigInteger.valueOf(0).toByteArray();
        tx.addInput(initialHash, 0);

        tx.signTx(pk_bob.getPrivate(), 0);

        /*
         * Nastav UTXOPool
         */
        // Výstup root transakcie je inicializačný unspent výstup.
        UTXOPool utxoPool = new UTXOPool();
        UTXO utxo = new UTXO(tx.getHash(), 0);
        utxoPool.addUTXO(utxo, tx.getOutput(0));

        /*
         * Vytvor test transakciu
         */
        Tx tx2 = new Tx();

        // Transaction.Output z tx na pozícii 0 má hodnotu 10
        tx2.addInput(tx.getHash(), 0);

        // Rozdelím coin hodnoty 10 na 3 coiny a všetky pošlem pre jednoduchosť 
        // na rovnakú adresu
        // (Alice)
        tx2.addOutput(5, pk_alice.getPublic());
        tx2.addOutput(3, pk_alice.getPublic());
        tx2.addOutput(2, pk_alice.getPublic());
         // Všimnite si, že v reálnom svete by sa pre hodnoty používali typy s fixed-point, 
         // nie double.
         // Doubles predstavujú floating-point chyby zaokrúhľovania. Tento typ by mal byť pre
         // príklad BigInteger a praocvať s najmenšími zlomkami mincí (satoshi v Bitcoine).

         // Existuje iba jeden (na pozícii 0) Transaction.Input v tx2
         // a obsahuje mincu od Boba, preto musím túto tx podpísať s
         // privátnym kľúčom Boba
        tx2.signTx(pk_bob.getPrivate(), 0);

        /*
         * Spusti test
         */
        // Pamätajte, že utxoPool obsahuje jeden neminutý Transaction.Output, 
        // ktorým je minca od Boba.
        HandleTxs handleTxs = new HandleTxs(utxoPool);
        System.out.println("handleTxs.txIsValid(tx2) returns: " + handleTxs.txIsValid(tx2));
        System.out.println("handleTxs.txHandler(new Transaction[]{tx2}) returns: "
                + handleTxs.txHandler(new Transaction[] { tx2 }).length + " transaction(s)");
    }

    public static class Tx extends Transaction {
        public void signTx(PrivateKey sk, int input) throws SignatureException {
            Signature sig = null;
            try {
                sig = Signature.getInstance("SHA256withRSA");
                sig.initSign(sk);
                sig.update(this.getRawDataToSign(input));
            } catch (NoSuchAlgorithmException | InvalidKeyException e) {
                throw new RuntimeException(e);
            }
            this.addSignature(sig.sign(), input);
            // Poznámka: táto funkcia je nesprávne pomenovaná a v skutočnosti
            // by nemala overridovať Java objekt finalize garbage kolektor metódu.
            this.finalize();
        }
    }
}