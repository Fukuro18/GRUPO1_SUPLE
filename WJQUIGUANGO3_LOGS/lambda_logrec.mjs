import { S3Client, PutObjectCommand } from "@aws-sdk/client-s3";
import { DynamoDBClient } from "@aws-sdk/client-dynamodb";
import { DynamoDBDocumentClient, PutCommand, GetCommand, ScanCommand, DeleteCommand } from "@aws-sdk/lib-dynamodb";

const s3Client = new S3Client({});
const ddbClient = new DynamoDBClient({});
const dynamo = DynamoDBDocumentClient.from(ddbClient);

export const handler = async (event) => {
    let body;
    let statusCode = 200;
    const headers = { 
        "Content-Type": "application/json",
        "Access-Control-Allow-Origin": "*"
    };
    const path = event.path || event.resource || "";

    try {
        // ===================== LOGREC =====================
        if (path.includes("log")) {
            if (event.httpMethod === "POST") {
                const logItem = JSON.parse(event.body);
                await dynamo.send(new PutCommand({
                    TableName: "LogRec",
                    Item: {
                        id: Date.now().toString(),       // ID único basado en timestamp
                        accion: logItem.accion,          // "Ingresar", "Creación", "Actualización", "Eliminación"
                        usuario: logItem.usuario,
                        fecha: new Date().toISOString()  // Fecha ISO 8601
                    }
                }));
                body = { message: "Log registrado" };
            }
        }
        // ===================== PRODUCTOS =====================
        else if (path.includes("producto")) {
            switch (event.httpMethod) {
                case "DELETE":
                    await dynamo.send(new DeleteCommand({ TableName: "Productos", Key: { id: parseInt(event.queryStringParameters.id) } }));
                    body = "Producto eliminado";
                    break;
                case "GET":
                    const data = await dynamo.send(new ScanCommand({ TableName: "Productos" }));
                    body = data.Items;
                    break;
                case "POST":
                    const item = JSON.parse(event.body);
                    if (item.imageBase64) {
                        const buffer = Buffer.from(item.imageBase64, 'base64');
                        const fileName = `fotos/${item.id}.jpg`;
                        const bucketName = 'mis-fotos-productos-quiguango';
                        await s3Client.send(new PutObjectCommand({ Bucket: bucketName, Key: fileName, Body: buffer, ContentType: 'image/jpeg' }));
                        item.imageUri = `https://${bucketName}.s3.amazonaws.com/${fileName}`;
                        delete item.imageBase64;
                    }
                    await dynamo.send(new PutCommand({ TableName: "Productos", Item: item }));

                    // Llamada al servicio web mailinsertrec
                    try {
                        const urlProfesor = 'https://URL_DEL_SERVICIO_MAILINSERTREC'; // <-- RECUERDA PONER LA URL REAL
                        await fetch(urlProfesor, {
                            method: 'POST',
                            headers: { 'Content-Type': 'application/json' },
                            body: JSON.stringify({ 
                                email_grupo: "lossininternetapp@gmail.com",
                                producto_descripcion: item.descripcion,
                                producto_costo: item.costo
                            })
                        });
                    } catch(e) { console.log("Error llamando a mailinsertrec:", e); }

                    body = { message: "Producto sincronizado", url: item.imageUri };
                    break;
            }
        } 
        // ===================== USUARIOS =====================
        else if (path.includes("usuario")) {
            switch (event.httpMethod) {
                case "GET":
                    const nombre = event.queryStringParameters.nombre;
                    const res = await dynamo.send(new GetCommand({ TableName: "Usuarios", Key: { nombre } }));
                    body = res.Item || null;
                    break;
                case "POST":
                    const user = JSON.parse(event.body);
                    await dynamo.send(new PutCommand({ TableName: "Usuarios", Item: user }));
                    body = { message: "Usuario sincronizado" };
                    break;
            }
        }
        // ===================== AUTH =====================
        else if (path.includes("auth")) {
            if (event.httpMethod === "POST") {
                const reqBody = JSON.parse(event.body);
                const nombreUser = reqBody.email || reqBody.nombre;
                const passwordUser = reqBody.password || reqBody.clave;
                
                const res = await dynamo.send(new GetCommand({
                    TableName: "Usuarios",
                    Key: { nombre: nombreUser }
                }));

                if (res.Item) {
                    if (res.Item.password === passwordUser || res.Item.clave === passwordUser) {
                        body = { message: "Login exitoso", success: true };
                    } else {
                        statusCode = 401;
                        body = { message: "Contraseña incorrecta", success: false };
                    }
                } else {
                    const newUser = { 
                        nombre: nombreUser, 
                        password: passwordUser
                    };
                    await dynamo.send(new PutCommand({ TableName: "Usuarios", Item: newUser }));
                    body = { message: "Usuario nuevo registrado y logueado exitosamente", success: true };
                }
            }
        }
    } catch (err) {
        statusCode = 400;
        body = err.message;
    }

    return { statusCode, body: JSON.stringify(body), headers };
};
