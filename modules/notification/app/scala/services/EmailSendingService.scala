package scala.services

import javax.inject.{Inject, Singleton}
import javax.mail.internet.{InternetAddress, MimeMessage}
import javax.mail._

import model.CommonProps._
import model.b2c.{Flat, Subscription}
import play.api.Configuration
import play.api.i18n._


@Singleton
class EmailSendingService @Inject()(config: Configuration,
                                    messagesApi: MessagesApi) {

  val props = new java.util.Properties()
  props.put(EmailSender.SmtpPropStartTls, "true")
  props.put(EmailSender.SmtpPropSmtpAuth, "true")
  props.put(EmailSender.SmtpPropSmtpHost, config.get[String](EmailSender.SmtpHost))
  props.put(EmailSender.SmtpPropSmtpPort, config.get[String](EmailSender.SmtpPort))
  val session = Session.getInstance(props,
    new javax.mail.Authenticator() {
      override protected def getPasswordAuthentication(): javax.mail.PasswordAuthentication = {
        new PasswordAuthentication(config.get[String](EmailSender.SmtpUsername),
          config.get[String](EmailSender.SmtpPassword))
      }
    });


  def sendEmail(flat: Flat, subscription: Subscription) = {
    val localizedMessages:Messages = MessagesImpl(Lang(subscription.language),messagesApi)
    val message = new MimeMessage(session)
    message.setText(views.html.Application.newflat
      .render(flat,localizedMessages)
      .body,"utf-8", "html")
    message.setFrom(new InternetAddress(config.get[String](EmailSender.SentFrom)))
    message.setSubject(flat.address.getOrElse(EmptyProp)+
      ", "+flat.district.getOrElse(EmptyProp)+", "+flat.city.getOrElse(EmptyProp)+", "
      + flat.price.getOrElse(EmptyProp)+ " EUR")
    val address:Address = new InternetAddress(subscription.subscriber)
    message.setRecipients(Message.RecipientType.TO,Array(address))
    Transport.send(message)
  }

}

object EmailSender {
  val SmtpHost = "smtp.host"
  val SmtpPort = "smtp.port"
  val SmtpUsername = "smtp.username"
  val SmtpPassword = "smtp.password"
  val SendToList = "smtp.sendto"
  val SentFrom = "smtp.sentfrom"
  val SmtpPropStartTls = "mail.smtp.starttls.enable"
  val SmtpPropSmtpAuth = "mail.smtp.auth"
  val SmtpPropSmtpHost = "mail.smtp.host"
  val SmtpPropSmtpPort = "mail.smtp.port"
}
