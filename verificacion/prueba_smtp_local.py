"""Uso: python prueba_smtp_local.py <java> <classpath> <truststore> <cert.pem> <key.pem> <pdf>
Servidor SMTP solo en loopback. No usa Gmail ni secretos reales.
"""
import base64,email,email.policy,hmac,pathlib,socketserver,ssl,subprocess,sys,threading
java,classpath,truststore,cert,key,pdf=sys.argv[1:]
contexto=ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER);contexto.load_cert_chain(cert,key)
mensajes=[];autenticaciones=[]
class SMTP(socketserver.BaseRequestHandler):
 def handle(self):
  conn=self.request;entrada=conn.makefile('rb');seguro=False;autorizado=False
  def decir(linea):conn.sendall(linea.encode()+b'\r\n')
  decir('220 localhost SMTP de pruebas')
  try:
   while True:
    linea=entrada.readline()
    if not linea:return
    texto=linea.decode().strip();comando=texto.split(' ',1)[0].upper()
    if comando in ('EHLO','HELO'):
     decir('250-localhost')
     if not seguro:decir('250-STARTTLS')
     if seguro:decir('250-AUTH LOGIN PLAIN')
     decir('250 SIZE 10000000')
    elif comando=='STARTTLS':
     decir('220 Iniciar TLS');conn=contexto.wrap_socket(conn,server_side=True);entrada=conn.makefile('rb');seguro=True
    elif comando=='AUTH':
     assert seguro,'No debe autenticar sin cifrar'
     partes=texto.split(' ')
     if partes[1].upper()=='PLAIN':
      if len(partes)<3:decir('334 ');contenido=entrada.readline().strip()
      else:contenido=partes[2].encode()
      datos=base64.b64decode(contenido).split(b'\0');usuario,clave=datos[-2:]
     else:
      decir('334 VXNlcm5hbWU6');usuario=base64.b64decode(entrada.readline().strip())
      decir('334 UGFzc3dvcmQ6');clave=base64.b64decode(entrada.readline().strip())
     autorizado=hmac.compare_digest(usuario,b'sender@example.invalid') and hmac.compare_digest(clave,b'aaaabbbbccccdddd')
     autenticaciones.append(autorizado);decir('235 Aceptado' if autorizado else '535 Credenciales incorrectas')
    elif comando in ('MAIL','RCPT'):decir('250 OK' if autorizado else '530 Autenticación requerida')
    elif comando=='DATA':
     assert autorizado and seguro;decir('354 Enviar mensaje');contenido=[]
     while True:
      parte=entrada.readline()
      if parte in (b'.\r\n',b'.\n'):break
      if not parte:return
      contenido.append(parte[1:] if parte.startswith(b'..') else parte)
     mensajes.append(b''.join(contenido));decir('250 Aceptado solo en prueba local')
    elif comando=='QUIT':decir('221 Hasta luego');return
    else:decir('250 OK')
  finally:conn.close()
with socketserver.ThreadingTCPServer(('127.0.0.1',0),SMTP) as servidor:
 hilo=threading.Thread(target=servidor.serve_forever,daemon=True);hilo.start()
 try:
  resultado=subprocess.run([java,'-Djavax.net.ssl.trustStore='+truststore,'-Djavax.net.ssl.trustStorePassword=test-only','-cp',classpath,'PruebaSMTP',str(servidor.server_address[1]),pdf])
  assert resultado.returncode==0
 finally:servidor.shutdown();hilo.join()
assert len(mensajes)==1,len(mensajes)
m=email.message_from_bytes(mensajes[0],policy=email.policy.default)
adjuntos=list(m.iter_attachments());assert len(adjuntos)==1
assert adjuntos[0].get_content_type()=='application/pdf'
assert adjuntos[0].get_payload(decode=True)==pathlib.Path(pdf).read_bytes()
assert autenticaciones.count(True)>=2 and autenticaciones.count(False)>=1
print('PASS captura SMTP real local: un único mensaje con el PDF intacto; comprobar conexión no envió correo.')
