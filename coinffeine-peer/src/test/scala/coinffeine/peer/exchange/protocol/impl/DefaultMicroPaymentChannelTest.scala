package coinffeine.peer.exchange.protocol.impl

import coinffeine.model.currency.Implicits._
import coinffeine.model.exchange.SampleExchange

class DefaultMicroPaymentChannelTest extends ExchangeTest {

  "A micropayment channel" should "generate valid signatures for each step" in new Channels {
    for ((buyer, seller) <- buyerChannels.zip(sellerChannels)) {
      buyer.currentStep should be (seller.currentStep)
      withClue(buyer.currentStep) {
        buyer.validateCurrentTransactionSignatures(seller.signCurrentTransaction) should be ('success)
      }
    }
  }

  for (step <- 1 to SampleExchange.IntermediateSteps) {
    it should s"split the exchanged amount and destroy deposits as fees in the step $step" in
      new Channels {
        val currentBuyerChannel = buyerChannels(step - 1)
        val currentSellerChannel = sellerChannels(step - 1)
        val tx = currentBuyerChannel.closingTransaction(currentSellerChannel.signCurrentTransaction)
        sendToBlockChain(tx.get)
        (balance(buyerWallet) + balance(sellerWallet)) should be (10.BTC)
        balance(buyerWallet) should be (1.BTC * step)
      }
  }

  it should "send exchanged amount to the buyer and deposits to depositors in the last step" in
    new Channels {
      val lastBuyerChannel = buyerChannels.last
      val lastSellerChannel = sellerChannels.last
      val tx = lastBuyerChannel.closingTransaction(lastSellerChannel.signCurrentTransaction)
      sendToBlockChain(tx.get)
      balance(buyerWallet) should be (12.BTC)
      balance(sellerWallet) should be (1.BTC)
    }
}
