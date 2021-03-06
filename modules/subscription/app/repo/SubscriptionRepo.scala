package repo

import java.util
import javax.inject.{Inject, Singleton}

import com.mongodb.client.model.{Aggregates, Filters}
import com.mongodb.client.model.Filters.{or, _}
import configuration.MongoConnection
import model.CommonProps
import model.b2c.{Flat, Subscription}
import org.bson.Document
import org.bson.types.ObjectId
import play.shaded.ahc.io.netty.util.internal.StringUtil
import repo.helpers.SubscriptionRepoHelper._

import scala.collection.JavaConverters._

@Singleton
class SubscriptionRepo @Inject()(connection: MongoConnection) {

  private val subscriptionCollection = connection.getCollection(SubscriptionRepo.CollName)

  def createSubscription(subscription: Subscription) = {
    subscriptionCollection.insertOne(createSubscriptionDocument(subscription))
  }

  def getSubscriptionById(subscriptionId: String): Option[Subscription] = {
    if (StringUtil.isNullOrEmpty(subscriptionId) || !subscriptionId.matches(CommonProps.HexadecimalRegexp)
      || subscriptionId.size != 24) {
      return None
    }
    val params = new util.HashMap[String, Object]()
    params.put("_id", new ObjectId(subscriptionId))
    params.put("itemType", "subscription")
    val documents = subscriptionCollection.find(new Document(params))
    if (documents.iterator.hasNext) {
      return Some(createSubscriptionObject(documents.iterator.next))
    }
    None
  }

  def deleteSubscriptionById(subscriptionId: String): Long = {
    if (StringUtil.isNullOrEmpty(subscriptionId) || !subscriptionId.matches(CommonProps.HexadecimalRegexp)
      || subscriptionId.size != 24) {
      throw new IllegalArgumentException("Cannot delete subscription. Subscription id is empty or not valid")
    }
    val params = new util.HashMap[String, Object]()
    params.put("_id", new ObjectId(subscriptionId))
    val result = subscriptionCollection.deleteOne(new Document(params))
    result.getDeletedCount
  }

  def findAllSubscriptionsForEmail(email: String): List[Subscription] = {
    if (StringUtil.isNullOrEmpty(email) || !email.matches(CommonProps.EmailRegexp)) {
      return List[Subscription]()
    }
    val params = new util.HashMap[String, Object]()
    params.put("subscriber", email)
    val documents = subscriptionCollection.find(new Document(params)).asScala.toList
    return documents.map(document => createSubscriptionObject(document))
  }

  def findAllSubscribersForFlat(flat: Flat): List[Subscription] = {
    val query = and(
      or(lte("priceRange.from", flat.price.get), Filters.eq("priceRange.from", null)),
      or(gte("priceRange.to", flat.price.get), Filters.eq("priceRange.to", null)),
      or(lte("sizeRange.from", flat.size.get), Filters.eq("sizeRange.from", null)),
      or(gte("sizeRange.to", flat.size.get), Filters.eq("sizeRange.to", null)),
      or(lte("floorRange.from", flat.floor.get), Filters.eq("floorRange.from", null)),
      or(gte("floorRange.to", flat.floor.get), Filters.eq("floorRange.to", null)),
      or(Filters.eq("parameters.cities", flat.city.get), Filters.eq("parameters.cities", null)),
      or(Filters.eq("parameters.districts", flat.district.get), Filters.eq("parameters.districts", null)),
      or(Filters.eq("parameters.actions", flat.action.get), Filters.eq("parameters.actions", null)),
      Filters.eq("itemType", "subscription")
    )
    val documents = subscriptionCollection.aggregate(util.Arrays.asList(
      Aggregates.`match`(query),
      new Document("$sort",new Document("subscriber",1)),
      new Document("$group",new Document("_id","$subscriber")
        .append("subscriber",new Document("$first","$subscriber"))
        .append("itemType",new Document("$first","$itemType"))
        .append("priceRange",new Document("$first","$priceRange"))
        .append("sizeRange",new Document("$first","$sizeRange"))
        .append("floorRange",new Document("$first","$floorRange"))
        .append("parameters",new Document("$first","$parameters"))
        .append("language",new Document("$first","$language"))
      )
      )
    ).asScala.toList
    return documents.map(document => createSubscriptionObject(document))
  }
}

object SubscriptionRepo {
  val CollName = "flats"
}
