package example


object Main extends App with Client {
  val token: String = args(0)
  val uname: Option[String] = if (args.size > 1) Some(args(1)) else None
  runClient(token, uname)
}

trait Client extends TimeUtil {

  import slack.api.BlockingSlackApiClient
  import akka.actor.ActorSystem
  import slack.models.User

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

    println("#DEB")
    files.foreach(println)
    println

    val fids = files.map(_.id).toSet
    fids.foreach { i =>
      val r = client.deleteFile(i)
      println(s"${if(r) "v" else "x"}: $fids")
    }

    println("Done\n")

    system.terminate()
  }
}

trait TimeUtil {

  import com.github.nscala_time.time.Imports._

  val m = 12
  val tsTo = ((DateTime.now - m.months).getMillis()) / 1000
}
