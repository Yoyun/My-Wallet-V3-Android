package piuk.blockchain.android.data.payments;

import info.blockchain.api.data.UnspentOutputs;
import info.blockchain.wallet.payment.SpendableUnspentOutputs;

import org.apache.commons.lang3.tuple.Pair;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.crypto.BIP38PrivateKey;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.List;

import io.reactivex.Observable;
import piuk.blockchain.android.data.rxjava.RxBus;
import piuk.blockchain.android.data.rxjava.RxPinning;
import piuk.blockchain.android.data.rxjava.RxUtil;

public class SendDataManager {

    private PaymentService paymentService;
    private RxPinning rxPinning;

    public SendDataManager(PaymentService paymentService, RxBus rxBus) {
        this.paymentService = paymentService;
        rxPinning = new RxPinning(rxBus);
    }

    /**
     * Submits a Bitcoin payment to a specified BTC address and returns the transaction hash if
     * successful
     *
     * @param unspentOutputBundle UTXO object
     * @param keys                A List of elliptic curve keys
     * @param toAddress           The address to send the funds to
     * @param changeAddress       A change address
     * @param bigIntFee           The specified fee amount
     * @param bigIntAmount        The actual transaction amount
     * @return An {@link Observable<String>} where the String is the transaction hash
     */
    public Observable<String> submitBtcPayment(SpendableUnspentOutputs unspentOutputBundle,
                                               List<ECKey> keys,
                                               String toAddress,
                                               String changeAddress,
                                               BigInteger bigIntFee,
                                               BigInteger bigIntAmount) {

        return rxPinning.call(() -> paymentService.submitPayment(
                unspentOutputBundle,
                keys,
                toAddress,
                changeAddress,
                bigIntFee,
                bigIntAmount))
                .compose(RxUtil.applySchedulersToObservable());
    }

    /**
     * Submits a Bitcoin Cash payment to a specified BCH address and returns the transaction hash if
     * successful
     *
     * @param unspentOutputBundle UTXO object
     * @param keys                A List of elliptic curve keys
     * @param toAddress           The address to send the funds to
     * @param changeAddress       A change address
     * @param bigIntFee           The specified fee amount
     * @param bigIntAmount        The actual transaction amount
     * @return An {@link Observable<String>} where the String is the transaction hash
     */
    public Observable<String> submitBchPayment(SpendableUnspentOutputs unspentOutputBundle,
                                               List<ECKey> keys,
                                               String toAddress,
                                               String changeAddress,
                                               BigInteger bigIntFee,
                                               BigInteger bigIntAmount) {

        return rxPinning.call(() -> paymentService.submitBchPayment(
                unspentOutputBundle,
                keys,
                toAddress,
                changeAddress,
                bigIntFee,
                bigIntAmount))
                .compose(RxUtil.applySchedulersToObservable());
    }

    /**
     * Returns an Elliptic Curve Key from a BIP38 private key.
     *
     * @param password          The password for the BIP-38 encrypted key
     * @param scanData          A private key in Base-58
     * @param networkParameters The current Network Parameters
     * @return An {@link ECKey}
     */
    public Observable<ECKey> getEcKeyFromBip38(String password, String scanData, NetworkParameters networkParameters) {
        return Observable.fromCallable(() -> {
            BIP38PrivateKey bip38 = BIP38PrivateKey.fromBase58(networkParameters, scanData);
            return bip38.decrypt(password);
        }).compose(RxUtil.applySchedulersToObservable());
    }

    /**
     * Returns an {@link UnspentOutputs} object containing all the unspent outputs for a given
     * Bitcoin address.
     *
     * @param address The Bitcoin address you wish to query, as a String
     * @return An {@link Observable<UnspentOutputs>}
     */
    public Observable<UnspentOutputs> getUnspentOutputs(String address) {
        return rxPinning.call(() -> paymentService.getUnspentOutputs(address))
                .compose(RxUtil.applySchedulersToObservable());
    }


    /**
     * Returns an {@link UnspentOutputs} object containing all the unspent outputs for a given
     * Bitcoin Cash address. Please note that this method only accepts a valid Base58 (ie Legacy)
     * BCH address. BECH32 is not accepted by the endpoint.
     *
     * @param address The Bitcoin Cash address you wish to query, as a Base58 address String
     * @return An {@link Observable<UnspentOutputs>}
     */
    public Observable<UnspentOutputs> getUnspentBchOutputs(String address) {
        return rxPinning.call(() -> paymentService.getUnspentBchOutputs(address))
                .compose(RxUtil.applySchedulersToObservable());
    }

    /**
     * Returns a {@link SpendableUnspentOutputs} object from a given {@link UnspentOutputs} object,
     * given the payment amount and the current fee per kB. This method selects the minimum number
     * of inputs necessary to allow a successful payment by selecting from the largest inputs
     * first.
     *
     * @param unspentCoins  The addresses' {@link UnspentOutputs}
     * @param paymentAmount The amount you wish to send, as a {@link BigInteger}
     * @param feePerKb      The current fee per kB, as a {@link BigInteger}
     * @return An {@link SpendableUnspentOutputs} object, which wraps a list of spendable outputs
     * for the given inputs
     */
    public SpendableUnspentOutputs getSpendableCoins(UnspentOutputs unspentCoins,
                                                     BigInteger paymentAmount,
                                                     BigInteger feePerKb)
            throws UnsupportedEncodingException {
        return paymentService.getSpendableCoins(unspentCoins, paymentAmount, feePerKb);
    }

    /**
     * Calculates the total amount of bitcoin that can be swept from an {@link UnspentOutputs}
     * object and returns the amount that can be recovered, along with the fee (in absolute terms)
     * necessary to sweep those coins.
     *
     * @param unspentCoins An {@link UnspentOutputs} object that you wish to sweep
     * @param feePerKb     The current fee per kB on the network
     * @return A {@link Pair} object, where left = the sweepable amount as a {@link BigInteger},
     * right = the absolute fee needed to sweep those coins, also as a {@link BigInteger}
     */
    public Pair<BigInteger, BigInteger> getMaximumAvailable(UnspentOutputs unspentCoins,
                                                            BigInteger feePerKb) {
        return paymentService.getMaximumAvailable(unspentCoins, feePerKb);
    }

    /**
     * Returns true if the {@code absoluteFee} is adequate for the number of inputs/outputs in the
     * transaction.
     *
     * @param inputs      The number of inputs
     * @param outputs     The number of outputs
     * @param absoluteFee The absolute fee as a {@link BigInteger}
     * @return True if the fee is adequate, false if not
     */
    public boolean isAdequateFee(int inputs, int outputs, BigInteger absoluteFee) {
        return paymentService.isAdequateFee(inputs, outputs, absoluteFee);
    }

    /**
     * Returns the estimated size of the transaction in kB.
     *
     * @param inputs  The number of inputs
     * @param outputs The number of outputs
     * @return The estimated size of the transaction in kB
     */
    public int estimateSize(int inputs, int outputs) {
        return paymentService.estimateSize(inputs, outputs);
    }

    /**
     * Returns an estimated absolute fee in satoshis (as a {@link BigInteger} for a given number of
     * inputs and outputs.
     *
     * @param inputs   The number of inputs
     * @param outputs  The number of outputs
     * @param feePerKb The current fee per kB om the network
     * @return A {@link BigInteger} representing the absolute fee
     */
    public BigInteger estimatedFee(int inputs, int outputs, BigInteger feePerKb) {
        return paymentService.estimateFee(inputs, outputs, feePerKb);
    }

}
