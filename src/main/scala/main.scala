import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives.{complete, extractDataBytes, onComplete, path, post}
import akka.pattern.after
import akka.stream.scaladsl.{Flow}
import akka.util.ByteString

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt


object main extends App {

  implicit val system: ActorSystem = ActorSystem("BackPressureExample")
  implicit val executionContext = system.dispatcher
  val flow = Flow[ByteString].log("error logging").mapAsync(1)(data =>{
    println("executing long operation")
   after(2.seconds, system.scheduler)(Future.successful(data))
  })

  val route =
    (post & path("test")) {
        extractDataBytes { bytes =>
          println("uploading")
          val stream = bytes.via(flow).run()
          onComplete(stream) { ioResult =>
            complete("Finished what ever I was doing: " + ioResult)
          }
      }
    }

  val bindingFuture  =  Http().newServerAt("192.168.1.175", 8080).bind(route)
}