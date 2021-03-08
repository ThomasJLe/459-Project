import java.util.*;

public class TxHandler {

	private UTXOPool uPool;

	public TxHandler(UTXOPool utxoPool) {
		this.uPool = new UTXOPool(utxoPool);
	}

	public boolean isValidTx(Transaction tx) {

		double outValue = 0.0;
		double inValue = 0.0;
		int i = 0;
		HashSet<UTXO> utSeen = new HashSet<UTXO>();

		for (Transaction.Input in : tx.getInputs()) {
			// Creating an object of UTXO to manipulate
			UTXO ut = new UTXO(in.prevTxHash, in.outputIndex);

			// (1) Return true if all outputs claimed by tx are in the current UTXO pool
			if (!this.uPool.contains(ut)) {
				return false;
			}

			// Add to the input total
			inValue += uPool.getTxOutput(ut).value;

			// (2) Return true if the signatures on each input of tx are valid
			if (!uPool.getTxOutput(ut).address.verifySignature(tx.getRawDataToSign(i), in.signature))
				return false;

			// (3) Return true if no UTXO is claimed multiple times by tx
			if (utSeen.contains(ut))
				return false;

			// Add the UTXO to the Hash set
			utSeen.add(ut);

			// Increment the index
			i++;
		}

		// all of tx's output values are non-negative
		// For-loop to go through Transaction outputs
		for (Transaction.Output out : tx.getOutputs()) {
			// (4) Return true if all of tx's output values are non-negative
			if (out.value < 0.0)
				return false;

			// Add to the output total
			outValue += out.value;
		}

		// (5) Return true if the sum of tx's input values is greater than or equal to
		// the sum of its output values
		if (inValue < outValue)
			return false;

		// Else, returns true for all statements
		return true;
	}

	/*
	 * Handles each epoch by receiving an unordered array of proposed transactions,
	 * checking each transaction for correctness, returning a mutually valid array
	 * of accepted transactions, and updating the current UTXO pool as appropriate.
	 */
	public Transaction[] handleTxs(Transaction[] possibleTxs) {

		HashSet<Transaction> TransXs = new HashSet<Transaction>(Arrays.asList(possibleTxs));
		int transCount = 0;
		ArrayList<Transaction> valid = new ArrayList<Transaction>();

		do {
			transCount = TransXs.size();
			HashSet<Transaction> forRemove = new HashSet<Transaction>();
			for (Transaction Tx : TransXs) {

				// (1) Return only valid transactions
				if (!isValidTx(Tx)) {
					continue;
				}

				valid.add(Tx);

				// (3) Update uxtoPool
				for (Transaction.Input in : Tx.getInputs()) {
					UTXO Nutxo = new UTXO(in.prevTxHash, in.outputIndex);
					this.uPool.removeUTXO(Nutxo);
				}

				byte[] Hash = Tx.getHash();
				int i = 0;

				// (2) One transaction's inputs may depend on the output of another transaction
				// in the same epoch
				for (Transaction.Output output : Tx.getOutputs()) {
					UTXO Nutxo = new UTXO(Hash, i);
					i++;
					this.uPool.addUTXO(Nutxo, output);
				}
				forRemove.add(Tx);

			}

			for (Transaction Tx : forRemove) {
				TransXs.remove(Tx);
			}

		} while (transCount != TransXs.size() && transCount != 0);

		// (4) Return mutually valid transaction set of maximal size
		return valid.toArray(new Transaction[valid.size()]);
	}

}