package slack

import slack.api.BlockingSlackApiClient
import akka.actor.ActorSystem
import slack.models.{SlackFile, User}
import com.github.nscala_time.time.Imports._

object Eliminator extends App {
  val token: String = args(0)
  val userName: Option[String] = if (args.size > 1) Some(args(1)) else None
  Client.run(token, userName)
}

object Client extends TimeUtil {
  def run(token: String, userName: Option[String]): Unit = {
    val c = new Client(token)
    val files = c.files(c.findUser(userName), tsTo)
    println(files.map(_.name).mkString("\n"))
    println(c.deleteFiles(files))
    println("Done\n")
    c.finish()
  }
}

class Client(token: String) {
  implicit val system = ActorSystem("slack")
  val client = BlockingSlackApiClient(token)
  def finish() = system.terminate()
  def findUser(userName: Option[String]): Option[User] = {
    val r = userName.flatMap(n =>
      client.listUsers().find(_.name == n)
    )
    println(r.map(_.name).getOrElse("NA"))
    r
  }
  def files(user: Option[User], tsTo: Long): Seq[SlackFile] = user match {
    case Some(_) => client.listFiles(user.map(_.id), None, Some(tsTo.toString)).files
    case None => Seq[SlackFile]()
  }
  def deleteFiles(files: Seq[SlackFile]): Map[String, Boolean] =
    files.map(_.id).toSet.foldLeft(Map[String, Boolean]()) { (a, i) =>
      a + (i -> client.deleteFile(i))
    }
}

trait TimeUtil {
  val m = 0
  val tsTo: Long = ((DateTime.now - m.months).getMillis()) / 1000
}
