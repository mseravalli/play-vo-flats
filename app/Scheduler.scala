import javax.inject.{Inject, Singleton}

import actors.ProcessingActor
import akka.actor.{ActorSystem, Props}
import play.api.Configuration
import play.api.libs.ws.WSClient
import repo.FlatRepo

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
  * Created by oginskis on 12/03/2017.
  */
@Singleton
class Scheduler @Inject()(actorSystem: ActorSystem,
  configuration: Configuration, flatRepo: FlatRepo, wsClient: WSClient) {
  val processingActor = actorSystem.actorOf((Props(classOf[ProcessingActor],flatRepo,wsClient,configuration)))
  actorSystem.scheduler.schedule(0 seconds, configuration.underlying.getInt(Scheduler.FlatCheckSchedule) seconds,
    processingActor, ProcessingActor.Process)
  actorSystem.scheduler.schedule(0 seconds, configuration.underlying.getInt(Scheduler.EXPIRATION_KICK_OFF_SCHEDULE)
    seconds,
    processingActor, ProcessingActor.Expire)
}

object Scheduler {
  val FlatCheckSchedule = "ss.lv.flatCheckSchedule"
  val EXPIRATION_KICK_OFF_SCHEDULE = "flat.expire.kickOffSchedule"
}
