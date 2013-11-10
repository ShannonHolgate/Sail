/*
 * Copyright (c) 2013. Shannon Holgate.
 *
 * Sail - A personal fund management web application.
 *
 * Makes use of the Play web framework for scala, MongoDB and the salat-Play plugin
 */

package helpers

import com.typesafe.plugin._
import org.apache.commons.mail.EmailException
import java.util.Date
import play.api.mvc.{AnyContent, Request}
import java.text.SimpleDateFormat
import play.api.Play.current
import play.i18n.Messages
import play.api.Play

/**
 * Mail helper class to assist the Controllers
 */
trait Mailer {
  /** The Sail email address */
  val adminEmail: String = Messages.get("adminEmailAddress")

  /**
   * Creates and sends an email to the newly created user.
   * Makes use of the register email template and the Mailer plugin
   *
   * @param email String holding the Recipient email address
   * @param name  String name of the Recipient
   *
   * @see         views.email.register
   */
  def sendRegisteredEmail(email:String, name:String, request: Request[AnyContent]) = {
    try {
      val mail = use[MailerPlugin].email
      mail.setSubject(Messages.get("registerSubject"))
      mail.setRecipient(name + " <"+ email +">",email)
      mail.setFrom(Messages.get("sailAdminName") + "<"+adminEmail+">")
      mail.sendHtml(views.html.email.register.render(name,request).toString())
    } catch {
      case eme: EmailException => eme.printStackTrace
    }
  }

  /**
   * Creates and sends an email to the user holding the new reset password key
   * and expiry date.
   * Makes use of the reset html template and the Mailer plugin
   *
   * @param email   String the email address of the user
   * @param name    String the name of the user
   * @param key     String the unique password reset key
   * @param expire  Date the expiry date of the reset key
   * @param request Request[AnyContent] the current request
   *
   * @see           views.html.reset
   */
  def sendResetEmail(email:String, name:String, key:String, expire:Date, request:Request[AnyContent]) = {
    val dateFormatter = new SimpleDateFormat(Messages.get("dateFormat"))
    try {
      val mail = use[MailerPlugin].email
      mail.setSubject(Messages.get("resetSubject"))
      mail.setRecipient(name + " <"+ email +">",email)
      mail.setFrom(Messages.get("sailAdminName") + "<"+adminEmail+">")
      mail.sendHtml(views.html.email.reset.render(name,key,request,dateFormatter.format(expire)).toString())
    } catch {
      case eme: EmailException => eme.printStackTrace
    }
  }
}
