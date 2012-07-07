package models

import com.novus.salat._
import com.novus.salat.dao.SalatDAO
import com.novus.salat.global._
import com.mongodb.casbah.Imports._

case class Contact(
  fullName: String,
  phone: String,
  email: Option[String],
  imageName: Option[String],
  _id: ObjectId = new ObjectId
)

object Contact extends SalatDAO[Contact, ObjectId](
  collection = MongoConnection()("fontacts")("contacts")) {

  /*
   * Retrieve all contacts
   */
  def all: Seq[Contact] = Contact.find(MongoDBObject()).toSeq

  /*
   * create a new contact
   */
  def create(contact: Contact): Option[ObjectId] = Contact.insert(contact)

  /*
   * update a contact
   */
  def edit(contact: Contact) {
    Contact.update(MongoDBObject("_id" -> contact._id),
      MongoDBObject(
        "fullName" -> contact.fullName,
        "phone" -> contact.phone,
        "email" -> contact.email,
        "imageName" -> contact.imageName
      )
    )
  }

  /*
   * delete an existing contact
   */
  def delete(contact: Contact) { Contact.removeById(contact._id) }
}