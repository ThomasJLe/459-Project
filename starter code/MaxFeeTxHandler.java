import java.util.*;

public class MaxFeeTxHandler {

	private UTXOPool uPool;

	public MaxFeeTxHandler(UTXOPool utxoPool) {
		this.uPool = new UTXOPool(utxoPool);
	}

	public boolean isValidTx(Transaction tx) {
		// Initializing variables
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
		// For loop to go through Transaction outputs
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

	private double taxFees(Transaction tx) {
		// Initializing variables
		double inputSum = 0;
		double outputSum = 0;

		// Looping through the transaction inputs
		for (Transaction.Input in : tx.getInputs()) {
			// Creating an object of UTXO to manipulate
			UTXO utxo = new UTXO(in.prevTxHash, in.outputIndex);
			if (!uPool.contains(utxo) || !isValidTx(tx))
				continue;
			Transaction.Output txOutput = uPool.getTxOutput(utxo);
			inputSum += txOutput.value;
		}
		// Looping through transaction outputs to sum them up
		for (Transaction.Output out : tx.getOutputs()) {
			outputSum += out.value;
		}

		// Returns the tx fee
		return inputSum - outputSum;
	}

	// ​This method finds a set of transactions with maximum total transaction fees
	public Transaction[] handleTxs(Transaction[] possibleTxs) {
		// Creating a set that sorts the fees
		Set<Transaction> sortByFees = new TreeSet<>((tx1, tx2) -> {
			double tax1 = taxFees(tx1);
			double tax2 = taxFees(tx2);
			return Double.valueOf(tax2).compareTo(tax1);
		});

		// Adding the fees and possible transactions
		Collections.addAll(sortByFees, possibleTxs);

		Set<Transaction> acceptedTax = new HashSet<>();
		for (Transaction tx : sortByFees) {
			// If transaction is valid, add it to the set
			if (isValidTx(tx)) {

				acceptedTax.add(tx);

				// Looping through the Transaction inputs to remove the UTXO
				for (Transaction.Input in : tx.getInputs()) {
					UTXO utxo = new UTXO(in.prevTxHash, in.outputIndex);
					uPool.removeUTXO(utxo);
				}

				// Looping through the number of outputs and adding to uPool
				for (int i = 0; i < tx.numOutputs(); i++) {
					Transaction.Output out = tx.getOutput(i);
					UTXO utxo = new UTXO(tx.getHash(), i);
					uPool.addUTXO(utxo, out);
				}
			}
		}
		// Creating an array of type Transaction adding the valid transactions
		Transaction[] validTxArray = new Transaction[acceptedTax.size()];

		// Returning the accepted transactions
		return acceptedTax.toArray(validTxArray);
	}
}