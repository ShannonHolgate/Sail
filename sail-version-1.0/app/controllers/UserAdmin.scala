/*
 * Copyright (c) 2014. Shannon Holgate.
 *
 * Sail - A personal fund management web application.
 *
 * Makes use of the Play web framework for scala, MongoDB and the salat-Play plugin
 */

package controllers

import play.api.mvc.{Security, Results, Controller}
import helpers.Mailer
import play.api.data.Form
import play.api.data.Forms._
import play.i18n.Messages
import models.User
import views.html
import org.joda.time.DateTime

/**
 * Controller for the User Admin flow.
 * Holds the form and logic to change the User's password, email and setup a new fund
 */
object UserAdmin extends Controller with Secured with Mailer{

  /** change password form verifying the email address and that the new passwords match */
  val changePasswordForm= Form(
    tuple(
      "oldpassword" -> text,
      "password" -> text,
      "confirm" -> text
    ) verifying(Messages.get("error.password.nomatch"),result => result match {
      case (oldPassword,password,confirm) => {
        password.equals(confirm)
      }
    })
  )

  /** change email address form, veryifying that the email address does not exists */
  val changeEmailForm= Form(
    tuple(
      "password" -> text,
      "email" -> text,
      "confirm" -> text
    ) verifying(Messages.get("error.email.nomatch"),result => result match {
      case (password,email,confirm) => {
        email.equals(confirm)
    }}) verifying (Messages.get("error.user.exists"), result => result._3 match {
      case (email) => User.findByEmail(email).isEmpty
    }) verifying(Messages.get("error.email.invalid"),result => result match {
      case (password,email,confirm) => email.contains('@') && confirm.contains('@')
    })
  )

  /** New Fund form */
  val newFundForm= Form(
    tuple(
      "email" -> text,
      "password" -> text,
      "answer" -> text
    ) verifying (Messages.get("error.login.invalid"),result => result match {
      case (email,password,answer) => User.authenticate(email, password).isDefined
    }) verifying(Messages.get("error.question.incorrect"),result => result._3 match {
      case (answer) => {
        answer.trim.equalsIgnoreCase(Messages.get("view.admin.answer.text")) ||
          answer.trim.equalsIgnoreCase(Messages.get("view.admin.answer.number"))
      }
    })
  )

  /** Change Name form */
  val changeNameForm= Form(
    tuple(
      "email" -> text,
      "newname" -> text,
      "password" -> text
    ) verifying (Messages.get("error.login.invalid"),result => result match {
      case (email,newName,password) => User.authenticate(email, password).isDefined
    }) verifying(Messages.get("error.name.length",3.toString),result => result._2 match {
      case (name) => name.length.>(3)
    })
  )

  /**
   * Routes to the User Admin view
   * Ensures the user is already logged in
   *
   * @return  Result rendering the User Admin view
   */
  def index = withUser{
    user => implicit request => {
      /** Render the User Admin View */
      Ok(html.admin(changePasswordForm,changeEmailForm,newFundForm,changeNameForm,user)).flashing(request.flash)
    }
  }

  /**
   * Changes the password for a user
   * Updates the password in the MongoDB
   *
   * @return      Result redirecting the user back to the admin page with success message
   *              or refreshes the admin page with the form error
   * @see         User#changePassword(user:ObjectId, email:String, password:String)
   */
  def changePassword = withUser{
    user => implicit request => {
      changePasswordForm.bindFromRequest.fold(
        formWithErrors => {
          BadRequest(html.admin(formWithErrors,changeEmailForm,newFundForm,changeNameForm,user))
        },
        changePassword => {
          if (User.authenticate(user.email, changePassword._1).isDefined) {
            val resetResult = User.changePassword(user.id, changePassword._1,changePassword._3)
            if (null == resetResult) Redirect(routes.UserAdmin.index).flashing(configValues.resetSuccess -> user.name)
            else {
              /** There was an error changing the password */
              val formWithErrors = changePasswordForm.fill(changePassword).withGlobalError(resetResult)
              BadRequest(html.admin(formWithErrors,changeEmailForm,newFundForm,changeNameForm,user))
            }
          }
          else {
            /** The email and password do not match */
            val formWithErrors = changePasswordForm.fill(changePassword).withGlobalError(
              Messages.get("error.password.incorrect"))
            BadRequest(html.admin(formWithErrors,changeEmailForm,newFundForm,changeNameForm,user))
          }
        }
      )
    }
  }

  /**
   * Changes the email address for a user
   *
   * @return      Result redirecting the user back to the admin page with success message
   *              or refreshes the admin page with the form error
   * @see         User#changeEmail(user:ObjectId, email:String)
   */
  def changeEmail = withUser{
    user => implicit request => {
      changeEmailForm.bindFromRequest.fold(
        formWithErrors => {
          BadRequest(html.admin(changePasswordForm,formWithErrors,newFundForm,changeNameForm,user))
        },
        changeEmail => {
          if (User.authenticate(user.email, changeEmail._1).isDefined) {
            val resetResult = User.changeEmail(user.id,changeEmail._3)
            if (null == resetResult) Redirect(routes.UserAdmin.index).flashing(configValues.genericSuccess ->
              Messages.get("view.admin.emailchanged",user.name,changeEmail._3)).withSession(
              Security.username -> changeEmail._3, configValues.timeoutSession -> DateTime.now().toString())
            else {
              /** There was an error changing the email address */
              val formWithErrors = changeEmailForm.fill(changeEmail).withGlobalError(resetResult)
              BadRequest(html.admin(changePasswordForm,formWithErrors,newFundForm,changeNameForm,user))
            }
          }
          else {
            /** The username and password do not match */
            val formWithErrors = changeEmailForm.fill(changeEmail).withGlobalError(Messages.get("error.password.incorrect"))
            BadRequest(html.admin(changePasswordForm,formWithErrors,newFundForm,changeNameForm,user))
          }
        }
      )
    }
  }

  /**
   * Deletes the entire fund for the user
   *
   * @return      Result redirecting the user back to the admin page with success message
   *              or refreshes the admin page with the form error
   * @see         User#newFund(user:ObjectId)
   */
  def newFund = withUser{
    user => implicit request => {
      newFundForm.bindFromRequest.fold(
        formWithErrors => {
          BadRequest(html.admin(changePasswordForm,changeEmailForm,formWithErrors,changeNameForm,user))
        },
        newFund => {
          val resetResult = User.newFund(user.id)
          if (resetResult) Redirect(routes.UserAdmin.index).flashing(configValues.genericSuccess ->
            Messages.get("view.admin.fundreset"))
          else {
            /** There was an error setting up the new fund */
            val formWithErrors = newFundForm.fill(newFund).withGlobalError(Messages.get("error.newfund.failed"))
            BadRequest(html.admin(changePasswordForm,changeEmailForm,formWithErrors,changeNameForm,user))
          }
        }
      )
    }
  }

  /**
   * Changes the Users name
   *
   * @return      Result redirecting the user back to the admin page with success message
   *              or refreshes the admin page with the form error
   * @see         User#changeName(user:ObjectId, name:String)
   */
  def changeName = withUser{
    user => implicit request => {
      changeNameForm.bindFromRequest.fold(
        formWithErrors => {
          BadRequest(html.admin(changePasswordForm,changeEmailForm,newFundForm,formWithErrors,user))
        },
        changeName => {
          val resetResult = User.changeName(user.id,changeName._2)
          if (null == resetResult) Redirect(routes.UserAdmin.index).flashing(configValues.genericSuccess ->
            Messages.get("view.admin.namechanged", changeName._2))
          else {
            /** There was an error changing the name */
            val formWithErrors = changeNameForm.fill(changeName).withGlobalError(Messages.get("error.name.failed"))
            BadRequest(html.admin(changePasswordForm,changeEmailForm,newFundForm,formWithErrors,user))
          }
        }
      )
    }
  }

  /**
   * Requests a password reset for the users email address
   * Updates the user object on the MongoDB to hold a new unique password
   * reset key then refreshes the page with confirmation in flash scope
   *
   * @return    Result refreshing the pages with confirmation or flashing an error
   * @see       User#requestReset(email:String)
   */
  def requestReset = withUser{
    user => implicit request => {
      val (key,expire,name) = User.requestReset(user.email)
      if (key.isEmpty || expire.isEmpty || name.isEmpty) {
        /** In the very strange event of an ObjectId not being distinct, throw an error */
        BadRequest(html.admin(changePasswordForm,changeEmailForm,newFundForm,changeNameForm,user)).flashing(
          configValues.genericError -> Messages.get("error.user.none"))
      }
      else {
        /** Send the reset email to the user */
        sendResetEmail(user.email,name.get,key.get,expire.get, request)
        Redirect(routes.UserAdmin.index).flashing(configValues.resetRequest -> user.email)
      }
    }
  }
}
