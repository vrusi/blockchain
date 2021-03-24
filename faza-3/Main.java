/*
 * Main test code for Cousera cryptocurrency assignment3
 * Based on code by Sven Mentl and Pietro Brunetti
 *
 * Copyright:
 * - Sven Mentl
 * - Pietro Brunetti
 * - Bruce Arden
 * - Tero Keski-Valkama
 */

import java.math.BigInteger;
import java.security.*;


public class Main {

    private static void print(Block block) {
        System.out.println("\n");
        System.out.println("block .......... " + block);
        System.out.println("hash ........... " + block.getHash());
        System.out.println("prevhash ....... " + block.getPrevBlockHash());
        System.out.println("coinbase ....... " + block.getCoinbase());
        System.out.println("coinbase inputs " + block.getCoinbase().getInputs().size());
        System.out.println("coinbase outputs " + block.getCoinbase().getOutputs().size());
        System.out.println("coinbase output value " + block.getCoinbase().getOutput(0).value);
        System.out.println("rawblock ....... " + block.getRawBlock());
        System.out.println("transactions ... " + block.getTransactions());
    }

    public static void main(String[] args) throws NoSuchProviderException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {

        /*
         * Generate key pairs, for Scrooge, Alice & Bob
         */
        KeyPair pk_scrooge = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        KeyPair pk_alice   = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        KeyPair pk_bob     = KeyPairGenerator.getInstance("RSA").generateKeyPair();

        /*
         * Create Genesis block: No TXs, but 1 Coinbase
         */
        Block genesis = new Block(null, pk_scrooge.getPublic());
        genesis.finalize();

        print(genesis);
        BlockChain bc = new BlockChain(genesis);
        BlockHandler bh = new BlockHandler(bc);

        /*
         * Create block1 from alice with tx scrooge -> alice
         */
        Block block1 = new Block(genesis.getHash(), pk_alice.getPublic());
        // new TX: scrooge pays 25 coins to alice
        Tx tx1 = new Tx();

        // the genesis block has a value of 25
        tx1.addInput(genesis.getCoinbase().getHash(), 0);

        tx1.addOutput(5, pk_alice.getPublic());
        tx1.addOutput(10, pk_alice.getPublic());
        tx1.addOutput(10, pk_alice.getPublic());

        // There is only one (at position 0) Transaction.Input in tx2
        // and it contains the coin from Scrooge, therefore I have to sign with the private key from Scrooge
        tx1.signTx(pk_scrooge.getPrivate(), 0);

        block1.transactionAdd(tx1);
        block1.finalize();
        bc.transactionAdd(tx1);
        UTXOPool utxoPool = new UTXOPool();
        HandleTxs handler = new HandleTxs(utxoPool);

        System.out.println("transaction pool: " + bc.getTransactionPool().getTransactions().size());


        print(block1);

        System.out.println("Block1 Added ok: " + bh.blockProcess(block1));

        /*
         * Create alternative block, block2 from scrooge with tx scrooge -> scrooge
         */
        Block block2 = new Block(genesis.getHash(), pk_scrooge.getPublic());

        // new TX: scrooge pays 25 coins to scrooge
        Tx tx2 = new Tx();
        tx2.addInput(genesis.getCoinbase().getHash(), 0);  //25
        tx2.addOutput(5, pk_scrooge.getPublic());
        tx2.addOutput(10, pk_scrooge.getPublic());
        tx2.addOutput(10, pk_scrooge.getPublic());
        tx2.signTx(pk_scrooge.getPrivate(), 0);

        block2.transactionAdd(tx2);
        block2.finalize();

        System.out.println("Block2 Added ok: " + bh.blockProcess(block2));

        /*
         * Create new block3 chained to block1 with tx alice -> bob
         */
        Block block3 = new Block(block1.getHash(), pk_scrooge.getPublic());

        // new TX: alice pays 15 coins to bob
        Tx tx3 = new Tx();
        tx3.addOutput(20, pk_bob.getPublic());
        tx3.addInput(tx1.getHash(), 1);	// 10 coins
        tx3.signTx(pk_alice.getPrivate(), 0);
        tx3.addInput(tx1.getHash(), 2);	// 10 coins
        tx3.signTx(pk_alice.getPrivate(), 1);

        block3.transactionAdd(tx3);
        block3.finalize();

        System.out.println("Block3 Added ok: " + bh.blockProcess(block3));

        /*
         * Create new block4 chained to block3 with tx bob -> bob
         */
        Block block4 = new Block(block3.getHash(), pk_scrooge.getPublic());

        // new TX: bob splits 15 coins to bob
        Tx tx4 = new Tx();
        tx4.addOutput(10, pk_bob.getPublic());
        tx4.addOutput(5, pk_bob.getPublic());
        tx4.addInput(tx3.getHash(), 0);	// 15 coins
        tx4.signTx(pk_bob.getPrivate(), 0);

        block4.transactionAdd(tx4);
        block4.finalize();

        System.out.println("Block4 Added ok: " + bh.blockProcess(block4));

        /*
         * Create new block5 chained to block4 with tx alice -> bob
         */
        Block block5 = new Block(block4.getHash(), pk_alice.getPublic());

        // new TX: alice pays 5+25 coins to bob
        Tx tx5 = new Tx();
        tx5.addOutput(25, pk_bob.getPublic());
        tx5.addInput(tx1.getHash(), 0);	// 5 coins
        tx5.signTx(pk_alice.getPrivate(), 0);
        tx5.addInput(block1.getCoinbase().getHash(), 0); // 25 coins
        tx5.signTx(pk_alice.getPrivate(), 1);

        block5.transactionAdd(tx5);
        block5.finalize();
        System.out.println("Block5 Added ok: " + bh.blockProcess(block5));

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
            this.addSignature(sig.sign(),input);
            // Note that this method is incorrectly named, and should not in fact override the Java
            // object finalize garbage collection related method.
            this.finalize();
        }
    }
}