package slack

import akka.actor.ActorSystem
import slack.api.BlockingSlackApiClient

import slack.models.{ SlackFile, User, Channel }
import slack.api.HistoryChunk

import com.github.nscala_time.time.Imports._

object Eliminator extends App {
  args(0) match {
    case "file" => Client.run(args(1), if (args.size > 1) Some(args(2)) else None, 12)
    case "message" => MessageClient.run(args(1), args(2), args(3))
  }
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

  //  Copied
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

object MessageClient {
  def run(token: String, channelName: String, userName: String): Unit = {
    val c = new MessageClient(token)
    val userId = c.findUser(userName).map(_.id).get
    val channelId = c.findChannel(channelName).map(_.id).get
    val tss = c.timestampsByUser(channelId, userId)
    println(tss)
    tss.foreach(ts =>
      try {
        println(ts)
        c.deleteChat(channelId, ts)
      }
      catch {
        case e:slack.api.ApiError => println(e)
      }
    )
    println("Done\n")
    c.finish()
  }
}

class MessageClient(token: String) {
  implicit val system = ActorSystem("slack")
  val client = BlockingSlackApiClient(token)
  def finish() = system.terminate()

  def findChannel(channel: String): Option[Channel] =
    client.listChannels(1).find(_.name == channel)

  def messages(channelId: String): Seq[Map[String, String]] = {
    var history = client.getChannelHistory(channelId)
    var msg = selectedMessages(history)
    while(history.has_more && msg.size < 1000) {
      val ts = msg.last("ts")
      history = client.getChannelHistory(channelId, Some(ts))
      msg = selectedMessages(history) ++ msg
    }
    msg
  }

  def deleteChat(channelId: String, ts: String): Unit = {
    Thread.sleep(1000)
    client.deleteChat(channelId, ts)
  }

  def selectedMessages(history: HistoryChunk): Seq[Map[String, String]] =
    history.messages.map(jv => Map(
      "user" -> (jv \ "user").asOpt[String].getOrElse(""),
      "ts"   -> (jv \ "ts").as[String]))

  def timestampsByUser(channelId: String, userId: String): Seq[String] =
    messages(channelId).filter(_("user") == userId).map(_("ts"))

  // Copy
  def findUser(userName: String): Option[User] =
    client.listUsers().find(_.name == userName)
}
