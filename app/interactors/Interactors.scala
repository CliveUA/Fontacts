package interactors

import java.io.File

import play.api._
import play.api.mvc._
import play.api.libs._

// An interactor communicates the functionalities of a Contact
trait ContactInteractor {

  // Generates random filenames for contact photos
  def generateFilename = BigInt.probablePrime(128, scala.util.Random).toString

  // Retrieves the extension of a file
  def fileExtension(filename: String) = filename.substring(filename.lastIndexOf("."))

  // A regular expression that matches a valid phone number
  val phoneMatcher = "^(234|\\+234|0)[0-9]{10}".r.pattern

  // Determines if a string is a valid phone number
  def isPhoneNumber(phone: String) = phoneMatcher.matcher(phone).matches

  // retrieve and save the contact photo.
  def uploadPhoto(photo: MultipartFormData.FilePart[Files.TemporaryFile]): String = {
    val filename = generateFilename + fileExtension(photo.filename)
      photo.ref.moveTo(new File("/data/contacts/photos", filename))

      filename
  }
}