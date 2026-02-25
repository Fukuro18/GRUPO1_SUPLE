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
                    subject = "Código de Verificación - Aplicación de Productos"

                    // Contenido del mensaje en formato HTML para un aspecto más profesional
                    setContent(
                        """
                        <div style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                            <h2 style="color: #007BFF;">Código de Verificación</h2>
                            <p>Estimado/a usuario/a,</p>
                            <p>Gracias por utilizar nuestra aplicación. A continuación, encontrará su código de verificación de 6 dígitos:</p>
                            <h1 style="color: #007BFF; text-align: center; margin: 20px 0;">$codigo</h1>
                            <p>Este código es válido durante los próximos <strong>5 minutos</strong>. Por favor, no comparta este código con nadie.</p>
                            <p>Si no ha solicitado este código, por favor ignore este mensaje o póngase en contacto con nuestro equipo de soporte.</p>
                            <hr style="border: none; border-top: 1px solid #ccc; margin: 20px 0;">
                            <p style="font-size: 0.9em; color: #777;">Atentamente,<br>El equipo de la Aplicación de Productos</p>
                        </div>
                    """.trimIndent(),
                        "text/html; charset=utf-8"
                    )
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

    suspend fun enviarCorreoNuevoProducto(destinatario: String, descripcion: String, costo: Double, imageUri: String? = null): Boolean {
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

                // Bloque de imagen HTML (solo si hay una URL de imagen en S3)
                val imagenHtml = if (!imageUri.isNullOrEmpty() && imageUri.startsWith("http")) {
                    """
                    <p><strong>Imagen del Producto:</strong></p>
                    <p><a href="$imageUri">$imageUri</a></p>
                    <img src="$imageUri" alt="Foto del producto" style="max-width:300px; border-radius:8px; margin-top:8px;" />
                    """.trimIndent()
                } else {
                    "<p><em>Sin imagen disponible</em></p>"
                }

                val message = MimeMessage(session).apply {
                    setFrom(InternetAddress(DIRECCION_REMITENTE))
                    setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinatario))
                    subject = "Nuevo Producto Registrado - NAVALLE"

                    // Contenido HTML con la info del producto
                    setContent(
                        """
                        <div style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                            <h2 style="color: #4CAF50;">Nuevo Producto Ingresado</h2>
                            <p>Estimado equipo,</p>
                            <p>Se ha registrado exitosamente un nuevo producto con la siguiente información:</p>
                            <ul>
                                <li><strong>Descripción:</strong> $descripcion</li>
                                <li><strong>Costo:</strong> $$costo</li>
                            </ul>
                            $imagenHtml
                            <p>Este es un mensaje automático del sistema.</p>
                        </div>
                    """.trimIndent(),
                        "text/html; charset=utf-8"
                    )
                }

                Transport.send(message)
                println("Correo de nuevo producto enviado exitosamente a $destinatario")
                true
            } catch (e: Exception) {
                e.printStackTrace()
                println("Error al enviar el correo del producto: ${e.message}")
                false
            }
        }
    }
}
