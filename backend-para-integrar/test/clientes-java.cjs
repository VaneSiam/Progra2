const express=require('express');const assert=require('node:assert/strict');const {spawn}=require('node:child_process');const path=require('node:path');
const app=express();app.use(express.json());let creados=0,actualizados=0;
app.post('/api/cliente/create',(q,r)=>{creados++;assert.equal(q.body.telefono,'12345678');r.status(201).json(q.body);});
app.put('/api/cliente/update/1',(q,r)=>{actualizados++;assert.equal(q.body.correo,'cliente@example.invalid');r.json(q.body);});
const server=app.listen(0,'127.0.0.1',()=>{
 const hijo=spawn(path.join(process.env.JAVA_HOME,'bin/java'),['-Dapi.url=http://127.0.0.1:'+server.address().port+'/api','-cp',process.argv[2],'PruebaClientesServicio'],{stdio:'inherit'});
 hijo.on('exit',code=>{try{assert.equal(code,0);assert.equal(creados,1);assert.equal(actualizados,1);console.log('PASS HTTP clientes: cero escrituras inválidas y una creación/edición válidas.');}catch(e){console.error(e);process.exitCode=1;}finally{server.close();}});
});
