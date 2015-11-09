package scorex.api.http

import play.api.libs.json.Json
import scorex.transaction.BlockChain
import scorex.transaction.state.wallet.Wallet
import spray.routing.HttpService._


case class BlocksApiRoute(implicit blockchain: BlockChain, wallet: Wallet)
  extends ApiRoute with CommonTransactionApiFunctions {

  override lazy val route =
    pathPrefix("blocks") {
      path("signature" / Segment) { case encodedSignature =>
        get {
          complete(withBlock(encodedSignature)(_.json).toString())
        }
      } ~ path("first") {
        get {
          complete(blockchain.blockAt(1).get.json.toString())
        }
      } ~ path("last") {
        get {
          complete(blockchain.lastBlock.json.toString())
        }
      } ~ path("at" / IntNumber) { case height =>
        get {
          val res = blockchain
            .blockAt(height)
            .map(_.json.toString())
            .getOrElse(Json.obj("status" -> "error", "details" -> "No block for this height").toString())
          complete(res)
        }
      } ~ path("height") {
        get {
          complete(Json.obj("height" -> blockchain.height()).toString())
        }
      } ~ path("height" / Segment) { case encodedSignature =>
        get {
          complete {
            withBlock(encodedSignature) { block =>
              Json.obj("height" -> blockchain.heightOf(block))
            }.toString()
          }
        }
      } ~ path("child" / Segment) { case encodedSignature =>
        get {
          complete(withBlock(encodedSignature) { block =>
            blockchain.children(block).head.json
          }.toString())
        }
      } ~ path("address" / Segment) { case address =>
        get {
          complete(withPrivateKeyAccount(address) { account =>
            Json.arr(blockchain.generatedBy(account).map(_.json))
          }.toString())
        }
      }
    }
}
