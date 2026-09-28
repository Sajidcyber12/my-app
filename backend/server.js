import express from 'express';
import admin from 'firebase-admin';

// Configure GOOGLE_APPLICATION_CREDENTIALS on your server before starting.
admin.initializeApp();
const app = express(); app.use(express.json());
app.get('/health', (_,res)=>res.json({ok:true,service:'crypto-signal-ai'}));
app.post('/notify', async (req,res)=>{
  const {token,title='Crypto Signal AI',body='New signal available',data={}}=req.body;
  if(!token) return res.status(400).json({error:'token required'});
  const id=await admin.messaging().send({token,notification:{title,body},data});
  res.json({messageId:id});
});
app.listen(process.env.PORT||8080,()=>console.log('Crypto Signal AI backend running'));
