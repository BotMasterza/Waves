package com.wavesplatform.it.sync.matcher

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

  import BlacklistedTradingTestSuite._
  override protected def nodeConfigs: Seq[Config] = Configs.map(configWithBlacklisted().withFallback(_))

  private def matcher = dockerNodes().head
  private def alice   = dockerNodes()(1)
  private def bob     = dockerNodes()(2)

  matcher.waitForTransaction(matcher.signedIssue(createSignedIssueRequest(IssueUsdTx)).id)

  val (amount, price) = (1000000000L, 1000L)

  "test" in {
    val usdBuyOrder = matcher.placeOrder(alice.privateKey, wavesUsdPair, BUY, amount, price, matcherFee, version = 1)
    matcher.waitOrderStatus(wavesUsdPair, usdBuyOrder.message.id, "Accepted")

    val usdSellOrder = matcher.placeOrder(bob.privateKey, wavesUsdPair, SELL, amount, price, matcherFee, version = 2)
    usdSellOrder.status shouldBe "OrderAccepted"
    Thread.sleep(60000)
    matcher.orderStatus(usdSellOrder.message.id, wavesUsdPair).status shouldBe "Filled"
//    assertBadRequestAndMessage(matcher.placeOrder(bob.privateKey, wavesUsdPair, SELL, dec8, dec2, matcherFee, version = 1),
//                               "Trading on scripted account isn't allowed yet")

    matcher.waitOrderInBlockchain(usdSellOrder.message.id)

    info(s"matcherHeight: ${matcher.height}, aliceHeight: ${alice.height}")
    info(s"mB: ${matcher.blockSeq(1, matcher.height)}")
    info(s"aB: ${alice.blockSeq(1, alice.height)}")

    val txInfo = alice.waitOrderInBlockchain(usdSellOrder.message.id)
    info(s"${alice.height}: $txInfo")
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
