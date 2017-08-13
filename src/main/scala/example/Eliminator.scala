package example

import slack.api.BlockingSlackApiClient
import akka.actor.ActorSystem
import slack.models.User
import com.github.nscala_time.time.Imports._

object Main extends Client with App {
  println(s"#args ${args.size}")
  val token: String = args(0)
  val userName: Option[String] = if (args.size > 1) Some(args(1)) else None
  runClient(token, userName)
}

trait Client extends TimeUtil {
  implicit val system = ActorSystem("slack")

  def runClient(token: String, uname: Option[String]): Unit = {
    val client = BlockingSlackApiClient(token)
    val user: Option[User] = uname.flatMap(n =>
      client.listUsers().find(_.name == n)
    )
    println(user.map(_.name).getOrElse("NA"))

    val uid = user.map(_.id)
    val files = uname match {
      case Some(_) => client.listFiles(uid, None, Some(tsTo.toString)).files
      case None => Seq()
    }

    files.foreach(println)

    val fids = files.map(_.id).toSet
    fids.foreach { i =>
      val r = client.deleteFile(i)
      println(s"${if (r) "v" else "x"}: $fids")
    }

    println("Done\n")

    system.terminate()
  }
}

trait TimeUtil {
  val m = 12
  val tsTo = ((DateTime.now - m.months).getMillis()) / 1000
}
