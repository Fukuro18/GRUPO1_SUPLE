package ec.edu.uce.appproductosfinal.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

object EmailService {
    // Configura ESTO con tu correo y contraseña de aplicación generada por Google
    private const val DIRECCION_REMITENTE = "lossininternetapp@gmail.com"
    private const val PASSWORD_APLICACION = "zwzdrbugwunbmbqv" // Las 16 letras, todo junto sin espacios

    suspend fun enviarCodigoCorreo(destinatario: String, codigo: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val properties = Properties().apply {
                    put("mail.smtp.host", "smtp.gmail.com")
                    put("mail.smtp.port", "587")
                    put("mail.smtp.auth", "true")
                    put("mail.smtp.starttls.enable", "true")
                }

                val session = Session.getInstance(properties, object : Authenticator() {
                    override fun getPasswordAuthentication(): PasswordAuthentication {
                        return PasswordAuthentication(DIRECCION_REMITENTE, PASSWORD_APLICACION)
                    }
                })

                val message = MimeMessage(session).apply {
                    setFrom(InternetAddress(DIRECCION_REMITENTE))
                    setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinatario))
                    subject = "Tu Código de Acceso a la App"
                    setText("Hola!\n\nTu código de seguridad de 6 dígitos para ingresar a la aplicación es: $codigo\n\nEste código es válido por 5 minutos.")
                }

                Transport.send(message)
                println("Correo enviado exitosamente a $destinatario con código $codigo")
                true
            } catch (e: Exception) {
                e.printStackTrace()
                println("Error al enviar el correo: ${e.message}")
                false
            }
        }
    }
}
