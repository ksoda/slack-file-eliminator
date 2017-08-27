package slack

import slack.api.BlockingSlackApiClient
import akka.actor.ActorSystem
import slack.models.{SlackFile, User}
import com.github.nscala_time.time.Imports._

object Eliminator extends App {
  Client.run(
    args(0),
    if (args.size > 1) Some(args(1)) else None,
    12
  )
}

object Client extends TimeUtil {
  def run(token: String, userName: Option[String], months: Int): Unit = {
    val c = new Client(token)
    val user = c.findUser(userName)
    println(user.map(_.name).getOrElse("NA"))
    val files = c.files(user, monthsAgo(months))
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
  def findUser(userName: Option[String]): Option[User] =
    userName.flatMap(n => client.listUsers().find(_.name == n))

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
  def monthsAgo(m: Int): Long = ((DateTime.now - m.months).getMillis()) / 1000
}
