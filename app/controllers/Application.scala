package controllers

import com.mongodb.casbah.Imports.{ObjectId, MongoDBObject}

import java.io.File

import models.Contact

import play.api._
import play.api.mvc._
import play.api.data.Form
import play.api.data.Forms._

import play.api.libs.json._
import play.api.libs.json.Json._
import play.api.libs.json.Json.{toJson => TJ}

object Application extends Controller {

  val contactForm = Form(
    mapping(
      "fullname" -> nonEmptyText,
      "mobile" -> nonEmptyText,
      "email" -> optional(email)
    )((fullName, mobile, email) => Contact(fullName, mobile, email, None))
    ((contact: Contact) => Some((contact.fullName, contact.phone, contact.email)))

    verifying("Invalid phone number", contact => {
      val phoneMatcher = "^(234|\\+234|0)[0-9]{10}".r.pattern
      phoneMatcher.matcher(contact.phone).matches
    })
  )

  def index = Action { implicit request =>
    Ok(views.html.index())
  }

  def all = Action {
    val contacts = Contact.all.reverse

    val jsonContacts = TJ(
      Map(
        "contacts" -> contacts.map { contact =>
          Map(
            "id" -> TJ(contact._id.toString),
            "fullname" -> TJ(contact.fullName),
            "phone" -> TJ(contact.phone),
            "email" -> TJ(contact.email.getOrElse(null)),
            "imageUrl" -> TJ(contact.imageName.getOrElse(null))
          )
        }
      )
    )

    Ok(jsonContacts).as("application/json")
  }

  def add = Action(parse.multipartFormData) { implicit request =>

    // attempt using a usual play form to parse the datapart of the request.
    contactForm.bindFromRequest.fold(
      errorForm => BadRequest,
      contact => {
        // check if a contact with the phone number specified exists.
        val exists = Contact.findOne(MongoDBObject("phone" -> contact.phone)).isDefined

        if(exists) {
          BadRequest(
            TJ(Map("error" -> "That contact already exists."))
          )
        } else {

          // retrieve and save the uploaded photo.
          val photoName = request.body.file("photo").map { photo =>
            val filename = generateFilename + fileExtension(photo.filename)
            photo.ref.moveTo(new File("/data/contacts/photos", filename))

            filename
          }

          // add the contact to the database.
          Contact.create(
            contact.copy(
              imageName = photoName
            )
          )

          // prepare a json representation of the contact
          val jsonContact = TJ(
            Map(
              "id" -> TJ(contact._id.toString),
              "fullname" -> TJ(contact.fullName),
              "phone" -> TJ(contact.phone),
              "email" -> TJ(contact.email.getOrElse(null)),
              "imageUrl" -> TJ(photoName.getOrElse(null))
            )
          )

          // render the just created contact as json
          Ok(jsonContact).as("application/json")
        }
      }
    )
  }

  def edit(id: String) = Action(parse.multipartFormData) { implicit request =>

    contactForm.bindFromRequest.fold(
      errorForm => BadRequest,
      contact => {

        Contact.findOneById(new ObjectId(id)).map { _contact =>

          val updatedContact = request.body.file("photo").map { photo =>
            if (_contact.imageName.isDefined) {
              new File("/data/contacts/photos", _contact.imageName.get).delete
            }

            val filename = generateFilename + fileExtension(photo.filename)
            photo.ref.moveTo(new File("/data/contacts/photos", filename))

            contact.copy(imageName = Option(filename))
          }.getOrElse {
            contact.copy(imageName = _contact.imageName)
          }

          Contact.edit(
            updatedContact.copy(_id = new ObjectId(id))
          )

          val jsonContact = TJ(
            Map(
              "id" -> TJ(id),
              "fullname" -> TJ(updatedContact.fullName),
              "phone" -> TJ(updatedContact.phone),
              "email" -> TJ(updatedContact.email.getOrElse(null)),
              "imageUrl" -> TJ(updatedContact.imageName.getOrElse(null))
            )
          )
          Ok(jsonContact).as("application/json")
        }.getOrElse(NotFound)
      }
    )
  }

  def delete(id: String) = Action { implicit request =>
    Contact.findOneById(new ObjectId(id)).map { contact =>
      if(contact.imageName.isDefined) {
        new File("/data/contacts/photos/", contact.imageName.get).delete
      }

      Contact.delete(contact)
    }

    Ok("contact deleted").as("text/plain")
  }

  def contact_photo(id: String, imgName: String) = Action {
    Contact.findOneById(new ObjectId(id)).map { contact =>
      contact.imageName.map { imageName =>
        Ok.sendFile(new File("/data/contacts/photos", imageName), true).as("image/png")
      }.getOrElse(NotFound)
    }.getOrElse(NotFound)
  }

  // Generate random file names for contact photos
  private def generateFilename = BigInt.probablePrime(128, scala.util.Random).toString

  // Retrieve file extension
  private def fileExtension(filename: String) = filename.substring(filename.lastIndexOf("."))
}