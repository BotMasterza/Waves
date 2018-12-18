package com.wavesplatform.it.sync.matcherNode

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory.parseString
import com.wavesplatform.features.BlockchainFeatures
import com.wavesplatform.it.api.SyncHttpApi._
import com.wavesplatform.it.api.SyncMatcherHttpApi._
import com.wavesplatform.it.matcher.MatcherSuiteBase
import com.wavesplatform.it.sync._
import com.wavesplatform.it.sync.matcher.config.MatcherPriceAssetConfig._
import com.wavesplatform.it.util._
import com.wavesplatform.transaction.assets.exchange.OrderType._
import org.scalatest._

class SmartsTestSuite extends MatcherSuiteBase with GivenWhenThen {

  import SmartsTestSuite._
  override protected def nodeConfigs: Seq[Config] = Configs.map(configWithPreActivatedFeatures().withFallback(_))

  matcherNode.waitForTransaction(matcherNode.signedIssue(createSignedIssueRequest(IssueUsdTx)).id)

  val (amount, price) = (1000000000L, 1000L)

  "test" in {
    val usdBuyOrder = matcherNode.placeOrder(aliceNode.privateKey, wavesUsdPair, BUY, amount, price, matcherFee, version = 1)
    matcherNode.waitOrderStatus(wavesUsdPair, usdBuyOrder.message.id, "Accepted")

    val usdSellOrder = matcherNode.placeOrder(bobNode.privateKey, wavesUsdPair, SELL, amount, price, matcherFee, version = 2)
    usdSellOrder.status shouldBe "OrderAccepted"

    matcherNode.waitOrderStatus(wavesUsdPair, usdSellOrder.message.id, "Filled")
//    assertBadRequestAndMessage(matcherNode.placeOrder(bobNode.privateKey, wavesUsdPair, SELL, dec8, dec2, matcherNodeFee, version = 1),
//                               "Trading on scripted account isn't allowed yet")

    matcherNode.waitOrderInBlockchain(usdSellOrder.message.id)

//    info(s"matcherNodeHeight: ${matcherNode.height}, aliceNodeHeight: ${aliceNode.height}")
//    info(s"mB: ${matcherNode.blockSeq(1, matcherNode.height)}")
//    info(s"aB: ${aliceNode.blockSeq(1, aliceNode.height)}")
//
    val txInfo = aliceNode.waitOrderInBlockchain(usdSellOrder.message.id)
//    info(s"${aliceNode.height}: $txInfo")
  }

}

object SmartsTestSuite {
  def configWithPreActivatedFeatures(): Config = {
    parseString(s"""
                   |waves {
                   |  blockchain.custom.functionality {
                   |    pre-activated-features = { ${BlockchainFeatures.SmartAssets.id} = 0, ${BlockchainFeatures.SmartAccountTrading.id} = 0 }
                   |  }
                   |  miner.quorum = 1
                   |}
    """.stripMargin)
  }
}
