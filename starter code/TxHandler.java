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
			
			// all outputs claimed by tx are in the current UTXO pool 
		for(Transaction.Input in : tx.getInputs()) {
			UTXO ut = new UTXO(in.prevTxHash, in.outputIndex);
			if (!this.uPool.contains(ut)) { 
				return false;
			}

			double preVal = uPool.getTxOutput(ut).value;
			inValue += preVal;

				// no UTXO is claimed multiple times by tx
			if (utSeen.contains(ut)) { 
				return false;
			}
			utSeen.add(ut);

				// the signatures on each input of tx are valid
			if(!uPool.getTxOutput(ut).address.verifySignature(tx.getRawDataToSign(i), in.signature)) { 
				return false;
			}
			i++;
		}

			// all of tx�s output values are non-negative
		for (Transaction.Output out : tx.getOutputs()) {
			if (out.value < 0.0) { 
				return false;
			}
			outValue += out.value;
		}

			// the sum of tx�s input values is greater than or equal to the sum of its output values
		if (inValue < outValue) { 
			return false;
		}
			// all were true
		return true;
	}

	/* Handles each epoch by receiving an unordered array of proposed 
	 * transactions, checking each transaction for correctness, 
	 * returning a mutually valid array of accepted transactions, 
	 * and updating the current UTXO pool as appropriate.
	 */
	public Transaction[] handleTxs(Transaction[] possibleTxs) {
		// (1) Return only valid transactions
		// (2) One transaction's inputs may depend on the output of another
		// transaction in the same epoch
		// (3) Update uxtoPool
		// (4) Return mutally valid transaction set of maximal size

		HashSet<Transaction> TransXs = new HashSet<Transaction>(Arrays.asList(possibleTxs));
		int transCount = 0;
		ArrayList<Transaction> valid = new ArrayList<Transaction>();

		do {
			transCount = TransXs.size();
			HashSet<Transaction> forRemove = new HashSet<Transaction>();
			for (Transaction Tx : TransXs) {
				if(!isValidTx(Tx)) {
					continue;
				}

				valid.add(Tx);
				
				for(Transaction.Input in : Tx.getInputs()) {
					UTXO Nutxo = new UTXO(in.prevTxHash, in.outputIndex);
					this.uPool.removeUTXO(Nutxo);
				}

				byte[] Hash = Tx.getHash();
				int i = 0;
				
				for (Transaction.Output output : Tx.getOutputs()) {
					UTXO Nutxo = new UTXO(Hash, i);
					i++;
					this.uPool.addUTXO(Nutxo,output);
				}				
				forRemove.add(Tx);

			}

			for (Transaction Tx : forRemove){
				TransXs.remove(Tx);
			}


		} while (transCount != TransXs.size()  && transCount != 0);
		return valid.toArray(new Transaction[valid.size()]);
	}
}